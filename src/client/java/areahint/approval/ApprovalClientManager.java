package areahint.approval;

import areahint.AreashintClient;
import areahint.boundviz.BoundVizManager;
import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandVisualController;
import areahint.commandui.DeleteHintVertexScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.world.ClientWorldFolderManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// 客户端只持有服务器审批快照和内存草稿，正式域名仍由原有同步协议更新。
public final class ApprovalClientManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_MESSAGE_LENGTH = 32767;
    private static final long REQUEST_TIMEOUT = 20_000L;
    private static final Map<String, Transfer> TRANSFERS = new HashMap<>();
    private static final List<ApprovalRequest> REQUESTS = new ArrayList<>();
    private static ClientPlayNetworkHandler connection;
    private static boolean enabled;
    private static boolean submitApprovalEnabled;
    private static boolean urbanApprovalEnabled;
    private static boolean operator;
    private static int version;
    private static Session session;
    private static String awaitingAction;
    private static String awaitingId;
    private static long awaitingSince;
    private static Runnable afterDraft;
    private static boolean draftAcknowledged;
    private static JsonObject lastSnapshot;

    private ApprovalClientManager() { }

    public static void register() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_APPROVAL_DATA, (client, buf) -> {
            try {
                String transferId = buf.readString(128);
                int index = buf.readInt();
                int count = buf.readInt();
                String chunk = buf.readString(MAX_MESSAGE_LENGTH);
                ClientPlayNetworkHandler receivedOn = client.getNetworkHandler();
                client.execute(() -> {
                    if (acceptConnection(client, receivedOn)) receiveChunk(transferId, index, count, chunk);
                });
            } catch (Exception e) {
                AreashintClient.LOGGER.warn("无法读取审批快照分包", e);
            }
        });
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_APPROVAL_RESULT, (client, buf) -> {
            try {
                String json = buf.readString(MAX_MESSAGE_LENGTH);
                ClientPlayNetworkHandler receivedOn = client.getNetworkHandler();
                client.execute(() -> {
                    if (!acceptConnection(client, receivedOn)) return;
                    try {
                        handleResult(JsonParser.parseString(json).getAsJsonObject());
                    } catch (Exception e) {
                        clearWaiting();
                        message("approval.client.invalid_data");
                        AreashintClient.LOGGER.warn("无法解析审批响应", e);
                    }
                });
            } catch (Exception e) {
                AreashintClient.LOGGER.warn("无法读取审批响应", e);
            }
        });
    }

    // 正式世界目录就绪后才询问审批数据，登录早到的快照在这里补写到正确目录。
    public static void onWorldReady() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        if (connection != null && connection != client.getNetworkHandler()) reset();
        connection = client.getNetworkHandler();
        if (lastSnapshot != null) persistSnapshot(lastSnapshot);
        requestSnapshot("sync");
    }

    private static boolean acceptConnection(MinecraftClient client, ClientPlayNetworkHandler receivedOn) {
        ClientPlayNetworkHandler current = client.getNetworkHandler();
        if (current == null || receivedOn != null && receivedOn != current) return false;
        if (connection != null && connection != current) reset();
        connection = current;
        return true;
    }

    private static void receiveChunk(String id, int index, int count, String chunk) {
        if (count < 1 || count > 512 || index < 0 || index >= count) return;
        if (!TRANSFERS.containsKey(id) && TRANSFERS.size() >= 4) TRANSFERS.clear();
        Transfer transfer = TRANSFERS.computeIfAbsent(id, ignored -> new Transfer(count));
        if (transfer.chunks.length != count) {
            TRANSFERS.remove(id);
            return;
        }
        if (transfer.chunks[index] == null) {
            transfer.chunks[index] = chunk;
            transfer.received++;
            transfer.length += chunk.length();
        }
        if (transfer.length > 8_000_000) {
            TRANSFERS.remove(id);
            return;
        }
        if (transfer.received != count) return;
        TRANSFERS.remove(id);
        try {
            applySnapshot(JsonParser.parseString(String.join("", transfer.chunks)).getAsJsonObject());
        } catch (Exception e) {
            message("approval.client.invalid_data");
            AreashintClient.LOGGER.warn("无法解析审批快照", e);
        }
    }

    private static void applySnapshot(JsonObject snapshot) {
        JsonObject config = snapshot.has("config") ? snapshot.getAsJsonObject("config") : new JsonObject();
        submitApprovalEnabled = !config.has("submit_approval") || config.get("submit_approval").getAsBoolean();
        urbanApprovalEnabled = config.has("submit_urban_area") && config.get("submit_urban_area").getAsBoolean();
        enabled = submitApprovalEnabled || urbanApprovalEnabled;
        operator = snapshot.has("operator") && snapshot.get("operator").getAsBoolean();
        REQUESTS.clear();
        if (snapshot.has("requests")) {
            for (JsonElement value : snapshot.getAsJsonArray("requests")) {
                ApprovalRequest request = GSON.fromJson(value, ApprovalRequest.class);
                if (request != null && request.id != null) REQUESTS.add(request);
            }
        }
        lastSnapshot = snapshot;
        persistSnapshot(snapshot);
        // 其他管理员更新同一申请时不覆盖本地草稿，禁用提交并要求重新打开最新申请。
        if (session != null && awaitingAction == null) {
            ApprovalRequest latest = find(session.request.id);
            session.stale = latest == null || latest.revision != session.request.revision
                || !Objects.equals(latest.status, session.request.status);
        }
        // 关闭审批后保留已打开的回复详情，具体申请的修改按钮单独检查其开关。
        version++;
        completeDraftWhenReady();
    }

    private static void persistSnapshot(JsonObject snapshot) {
        if (!ClientWorldFolderManager.isInitialized()) return;
        try {
            var folder = ClientWorldFolderManager.getCurrentWorldFolder();
            Files.createDirectories(folder);
            JsonObject config = snapshot.has("config") ? snapshot.getAsJsonObject("config").deepCopy() : new JsonObject();
            if (!config.has("submit_approval")) config.addProperty("submit_approval", true);
            if (!config.has("submit_urban_area")) config.addProperty("submit_urban_area", false);
            Files.writeString(folder.resolve("server-config.json"), GSON.toJson(config), StandardCharsets.UTF_8);
            // 回复包含账号标识，避免同一个本地世界目录切换账号后误把旧回复当作当前玩家内容。
            JsonObject replies = new JsonObject();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) replies.addProperty("playerId", client.player.getUuidAsString());
            JsonArray records = new JsonArray();
            if (!operator) {
                for (ApprovalRequest request : REQUESTS) {
                    if (request.reviews != null && !request.reviews.isEmpty()) records.add(GSON.toJsonTree(request));
                }
            }
            replies.add("requests", records);
            Files.writeString(folder.resolve("approval-replies.json"), GSON.toJson(replies), StandardCharsets.UTF_8);
        } catch (Exception e) {
            AreashintClient.LOGGER.error("保存审批配置与回复失败", e);
        }
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean canOpenPanel() { return enabled || !getVisibleRequests().isEmpty(); }
    public static boolean isSessionOperationEnabled() {
        return session != null && ("applyurbanarea".equals(session.request.operation)
            ? urbanApprovalEnabled : submitApprovalEnabled);
    }
    public static boolean isOperator() { return operator; }
    public static int getVersion() { return version; }
    public static boolean isBusy() { return awaitingAction != null; }
    public static Session getSession() { return session; }

    public static List<ApprovalRequest> getVisibleRequests() {
        return REQUESTS.stream().filter(request -> operator ? "pending".equals(request.status)
            : request.reviews != null && !request.reviews.isEmpty()).toList();
    }

    public static void openList(Screen parent) {
        if (!canOpenPanel()) {
            message("approval.client.unavailable");
            return;
        }
        endSession();
        requestSnapshot("list");
        MinecraftClient.getInstance().setScreen(new ApprovalListScreen(parent));
    }

    public static void refresh() { requestSnapshot("list"); }

    public static void openRequest(String id, Screen parent) {
        ApprovalRequest request = find(id);
        if (request == null) return;
        endSession();
        session = new Session(copyRequest(request), parent);
        MinecraftClient.getInstance().setScreen(new ApprovalDetailScreen());
    }

    public static boolean restorePanel() {
        if (session == null || !canOpenPanel()) return false;
        MinecraftClient.getInstance().setScreen(new ApprovalDetailScreen());
        return true;
    }

    public static void returnToList() {
        Screen parent = session == null ? null : session.parent;
        endSession();
        MinecraftClient.getInstance().setScreen(parent instanceof ApprovalListScreen ? parent : new ApprovalListScreen(parent));
    }

    public static void endSession() {
        if (session != null) {
            if (session.editing) {
                areahint.addhint.AddHintManager.getInstance().cancel();
                areahint.deletehint.DeleteHintManager.getInstance().cancel();
                CommandVisualController.clearVisualRecordMode();
            }
            BoundVizManager.getInstance().clearApprovalPreview();
            if (session.previewActive) BoundVizManager.getInstance().setEnabled(session.wasBoundVizEnabled);
        }
        session = null;
        clearWaiting();
    }

    public static void preview(int index) {
        if (session == null || isBusy() || !validIndex(index)) return;
        Session current = session;
        Runnable launch = () -> {
            if (session != current) return;
            enablePreview();
            MinecraftClient.getInstance().setScreen(null);
            CommandUiActions.runCommand("areahint tcp approval " + current.request.id + " " + index);
            message("approval.ui.restore", areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName());
        };
        if (session.modified) saveDraftThen(launch); else launch.run();
    }

    public static void editGeometry(boolean add, int index) {
        if (!canEdit(index) || isBusy()) return;
        Session current = session;
        Runnable launch = () -> {
            if (session != current) return;
            CommandUiActions.runCommand("areahint " + (add ? "addhint" : "deletehint")
                + " approval " + current.request.id + " " + index);
        };
        if (session.modified) saveDraftThen(launch); else launch.run();
    }

    // 特殊指令仍由服务端核验申请归属，客户端只在收到授权动作后复用原有录点界面。
    public static void handleClientCommand(String action) {
        String[] parts = action.split(":", 3);
        if (parts.length != 3) return;
        int index;
        try { index = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return; }
        if (session == null || !session.request.id.equals(parts[1])) {
            ApprovalRequest request = find(parts[1]);
            if (request == null) { refresh(); return; }
            endSession();
            session = new Session(copyRequest(request), new ApprovalListScreen(null));
            session.editMode = !operator;
        }
        if (!canEdit(index)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !client.world.getRegistryKey().getValue().toString().equals(session.request.dimensionId)) {
            message("approval.client.wrong_dimension");
            return;
        }
        Session current = session;
        current.editing = true;
        enablePreview();
        java.util.function.Consumer<AreaData> callback = area -> {
            if (session != current) return;
            current.areas.set(index, copyArea(area));
            // 内容核对页同步显示本次临时几何，保证看到的修改后内容与最终上传草稿一致。
            for (ApprovalRequest.Change change : current.request.changes) {
                if (!"areas".equals(change.kind) || change.after == null || !change.after.isJsonArray()) continue;
                for (JsonElement item : change.after.getAsJsonArray()) {
                    JsonObject value = item.getAsJsonObject();
                    if (value.has("name") && area.getName().equals(value.get("name").getAsString())) {
                        value.add("vertices", GSON.toJsonTree(area.getVertices()));
                        value.add("second-vertices", GSON.toJsonTree(area.getSecondVertices()));
                    }
                }
            }
            current.modified = true;
            current.editing = false;
            updateOverlay();
            message("approval.ui.temporary");
            client.setScreen(new ApprovalDetailScreen());
        };
        if ("approval_addhint".equals(parts[0])) {
            areahint.addhint.AddHintManager.getInstance().beginTemporary(current.areas.get(index), current.request.dimensionId, callback);
            client.setScreen(null);
        } else if ("approval_deletehint".equals(parts[0])) {
            var manager = areahint.deletehint.DeleteHintManager.getInstance();
            manager.beginTemporary(current.areas.get(index), current.request.dimensionId, callback);
            client.setScreen(new DeleteHintVertexScreen(new ApprovalDetailScreen(), manager));
        }
    }

    public static boolean canEdit(int index) {
        if (session == null || session.stale || !isSessionOperationEnabled() || !validIndex(index)) return false;
        if (!(operator && "pending".equals(session.request.status)
                || !operator && "returned".equals(session.request.status) && session.editMode)) return false;
        String name = session.areas.get(index).getName();
        for (ApprovalRequest.Change change : session.request.changes) {
            if (!"areas".equals(change.kind) || change.after == null || !change.after.isJsonArray()) continue;
            for (JsonElement area : change.after.getAsJsonArray()) {
                if (area.isJsonObject() && area.getAsJsonObject().has("name")
                        && name.equals(area.getAsJsonObject().get("name").getAsString())) return true;
            }
        }
        return false;
    }

    public static void decide(String result) {
        if (session == null || session.stale || !isSessionOperationEnabled() || !operator || !"pending".equals(session.request.status)) return;
        JsonObject action = actionForSession("decision");
        action.addProperty("result", result);
        action.addProperty("comment", session.comment);
        sendMutation(action, null);
    }

    public static void resubmit() {
        if (session == null || session.stale || !isSessionOperationEnabled() || operator || !"returned".equals(session.request.status)) return;
        sendMutation(actionForSession("resubmit"), null);
    }

    private static void saveDraftThen(Runnable action) {
        if (session == null || session.stale) { message("approval.client.stale"); return; }
        if (!isSessionOperationEnabled()) { message("approval.error.disabled"); return; }
        sendMutation(actionForSession("draft"), action);
    }

    private static JsonObject actionForSession(String action) {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        json.addProperty("id", session.request.id);
        json.addProperty("revision", session.request.revision);
        if (session.modified) json.add("areas", GSON.toJsonTree(session.areas));
        if ("resubmit".equals(action)) json.add("history", GSON.toJsonTree(session.request.reviews));
        return json;
    }

    private static void sendMutation(JsonObject json, Runnable continuation) {
        if (isBusy() || session == null) return;
        if (!send(json)) return;
        awaitingAction = json.get("action").getAsString();
        awaitingId = session.request.id;
        awaitingSince = System.currentTimeMillis();
        afterDraft = continuation;
    }

    private static void handleResult(JsonObject result) {
        boolean success = result.has("success") && result.get("success").getAsBoolean();
        if (result.has("messageKey")) message(result.get("messageKey").getAsString());
        String id = result.has("id") ? result.get("id").getAsString() : null;
        if (id != null && id.isBlank()) id = null;
        String action = result.has("action") ? result.get("action").getAsString() : awaitingAction;
        if (action != null && action.isBlank()) action = awaitingAction;
        if (awaitingAction == null || id != null && !id.equals(awaitingId)
                || action != null && !action.equals(awaitingAction)) return;
        String completed = awaitingAction;
        String completedId = awaitingId;
        // 结果包只确认草稿已保存，完整新版本通过分包快照到达，两个包的先后顺序都允许。
        if (success && "draft".equals(completed)) {
            draftAcknowledged = true;
            completeDraftWhenReady();
            return;
        }
        clearWaiting();
        if (!success) { refresh(); return; }
        if (session == null || !session.request.id.equals(completedId)) return;
        if (result.has("request") && result.get("request").isJsonObject()) {
            session.request = GSON.fromJson(result.get("request"), ApprovalRequest.class);
            session.areas = copyAreas(session.request.previewAreas);
            session.modified = false;
            session.stale = false;
            updateOverlay();
        }
        if ("decision".equals(completed) || "resubmit".equals(completed)) {
            returnToList();
        }
    }

    private static void completeDraftWhenReady() {
        if (!draftAcknowledged || !"draft".equals(awaitingAction) || session == null) return;
        if (!session.request.id.equals(awaitingId)) { clearWaiting(); return; }
        ApprovalRequest latest = find(awaitingId);
        if (latest == null || latest.revision <= session.request.revision) return;
        Runnable continuation = afterDraft;
        boolean sameStatus = Objects.equals(latest.status, session.request.status);
        session.request = copyRequest(latest);
        session.areas = copyAreas(latest.previewAreas);
        session.modified = false;
        session.stale = !sameStatus;
        clearWaiting();
        updateOverlay();
        if (sameStatus && continuation != null) continuation.run();
    }

    private static void requestSnapshot(String action) {
        JsonObject json = new JsonObject();
        json.addProperty("action", action);
        send(json);
    }

    private static boolean send(JsonObject json) {
        if (!ClientNetworkCompat.get().canSend(Channel.C2S_APPROVAL_ACTION)) return false;
        String value = json.toString();
        if (value.length() > 8_000_000) { message("approval.client.too_large"); return false; }
        try {
            // 旧版C2S自定义包限制的是整个UTF-8字节数，不能仅按Java字符串字符数判断。
            if (value.getBytes(StandardCharsets.UTF_8).length > 32_000) {
                if (!ClientNetworkCompat.get().canSend(Channel.C2S_APPROVAL_CHUNK)) {
                    message("approval.client.too_large");
                    return false;
                }
                List<String> chunks = new ArrayList<>();
                for (int start = 0; start < value.length();) {
                    int end = Math.min(start + 8_000, value.length());
                    // 不在代理对中间切开，避免中文之外的补充字符或表情在UTF-8传输中损坏。
                    if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1))
                            && Character.isLowSurrogate(value.charAt(end))) end--;
                    chunks.add(value.substring(start, end));
                    start = end;
                }
                String transferId = java.util.UUID.randomUUID().toString();
                for (int i = 0; i < chunks.size(); i++) {
                    NetworkBuf part = ClientNetworkCompat.get().newBuffer();
                    part.writeString(transferId);
                    part.writeInt(i);
                    part.writeInt(chunks.size());
                    part.writeString(chunks.get(i));
                    ClientNetworkCompat.get().sendToServer(Channel.C2S_APPROVAL_CHUNK, part);
                }
                return true;
            }
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(value);
            ClientNetworkCompat.get().sendToServer(Channel.C2S_APPROVAL_ACTION, buf);
            return true;
        } catch (Exception e) {
            AreashintClient.LOGGER.warn("发送审批动作失败", e);
            message("approval.client.unavailable");
            return false;
        }
    }

    private static void enablePreview() {
        if (session == null) return;
        if (!session.previewActive) {
            session.wasBoundVizEnabled = BoundVizManager.getInstance().isEnabled();
            session.previewActive = true;
        }
        BoundVizManager.getInstance().setEnabled(true);
        updateOverlay();
    }

    private static void updateOverlay() {
        if (session == null || !session.previewActive) return;
        Set<String> hidden = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Set<String> remaining = new HashSet<>();
        for (ApprovalRequest.Change change : session.request.changes) {
            if (!"areas".equals(change.kind)) continue;
            addNames(hidden, change.before);
            addNames(hidden, change.after);
            addNames(removed, change.before);
            addNames(remaining, change.after);
        }
        removed.removeAll(remaining);
        for (AreaData area : session.areas) hidden.add(area.getName());
        // 删除申请仍保留原几何供 tcp 定位，但修改后预览不能把已经删除的域名边界重新叠加回来。
        List<AreaData> visible = session.areas.stream().filter(area -> !removed.contains(area.getName())).toList();
        BoundVizManager.getInstance().setApprovalPreview(session.request.dimensionId, visible, hidden);
    }

    private static void addNames(Set<String> names, JsonElement data) {
        if (data == null || !data.isJsonArray()) return;
        for (JsonElement element : data.getAsJsonArray()) {
            if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                names.add(element.getAsJsonObject().get("name").getAsString());
            }
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        TRANSFERS.values().removeIf(transfer -> now - transfer.createdAt > REQUEST_TIMEOUT);
        if (awaitingAction != null && now - awaitingSince > REQUEST_TIMEOUT) {
            clearWaiting();
            message("approval.client.timeout");
            refresh();
        }
    }

    public static void reset() {
        endSession();
        clearWaiting();
        TRANSFERS.clear();
        REQUESTS.clear();
        connection = null;
        enabled = false;
        submitApprovalEnabled = false;
        urbanApprovalEnabled = false;
        operator = false;
        lastSnapshot = null;
        version++;
    }

    private static void clearWaiting() {
        awaitingAction = null;
        awaitingId = null;
        afterDraft = null;
        draftAcknowledged = false;
    }

    private static boolean validIndex(int index) { return session != null && index >= 0 && index < session.areas.size(); }
    private static ApprovalRequest find(String id) { return REQUESTS.stream().filter(request -> request.id.equals(id)).findFirst().orElse(null); }
    private static AreaData copyArea(AreaData area) { return GSON.fromJson(GSON.toJson(area), AreaData.class); }
    private static ApprovalRequest copyRequest(ApprovalRequest request) { return GSON.fromJson(GSON.toJson(request), ApprovalRequest.class); }
    private static List<AreaData> copyAreas(List<AreaData> areas) {
        List<AreaData> result = new ArrayList<>();
        if (areas != null) for (AreaData area : areas) if (area != null) result.add(copyArea(area));
        return result;
    }

    public static String operationName(String operation) { return I18nManager.translate("approval.type." + operation); }
    public static String statusName(String status) { return I18nManager.translate("approval.ui.status." + status); }
    private static void message(String key, Object... args) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal(I18nManager.translate(key, args)), false);
    }

    // 页面字段存于会话中，临时关闭、调整窗口大小或顶点编辑返回后仍保留输入和选中位置。
    public static final class Session {
        public ApprovalRequest request;
        public List<AreaData> areas;
        public final Screen parent;
        public String comment = "";
        public int areaIndex;
        public int historyPage;
        public boolean editMode;
        public boolean modified;
        public boolean stale;
        private boolean editing;
        private boolean previewActive;
        private boolean wasBoundVizEnabled;

        private Session(ApprovalRequest request, Screen parent) {
            this.request = request;
            this.areas = copyAreas(request.previewAreas);
            this.parent = parent;
        }
    }

    private static final class Transfer {
        final String[] chunks;
        final long createdAt = System.currentTimeMillis();
        int received;
        int length;
        Transfer(int count) { chunks = new String[count]; }
    }
}
