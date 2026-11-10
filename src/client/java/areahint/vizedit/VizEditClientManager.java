package areahint.vizedit;

import areahint.AreashintClient;
import areahint.approval.ApprovalClientManager;
import areahint.approval.ApprovalRequest;
import areahint.i18n.I18nManager;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// 授权、分片、会话及提交确认共用一个入口，命令和按钮均不能绕过服务端权限。
public final class VizEditClientManager {
    private static final int MAX_LENGTH = 8_000_000;
    private static final int CHUNK_LENGTH = 8_000;
    private static final long TIMEOUT = 30_000L;
    private static final Map<String, Transfer> TRANSFERS = new HashMap<>();
    private static VizEditSession session;
    private static ClientPlayNetworkHandler connection;
    private static String worldDimension;
    private static String awaitingId;
    private static String awaitingAction;
    private static long awaitingSince;
    private static boolean busy;
    private static Screen requestedParent;
    private static String selectAfterOpen;
    private static boolean createAfterOpen;
    private static Runnable afterSave;
    private static JsonObject submittedAction;
    private static MapAccess mapAccess;

    private VizEditClientManager() { }

    public static void initialize() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_VIZEDIT_DATA, (client, reader) -> {
            try {
                String id = reader.readString(128);
                int index = reader.readInt(), count = reader.readInt();
                String part = reader.readString(CHUNK_LENGTH);
                ClientPlayNetworkHandler receivedOn = client.getNetworkHandler();
                client.execute(() -> {
                    if (receivedOn != null && receivedOn == client.getNetworkHandler()) receive(id, index, count, part);
                });
            } catch (Exception e) { AreashintClient.LOGGER.warn("读取可视化编辑分片失败", e); }
        });
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_VIZEDIT_RESULT, (client, reader) -> {
            try {
                String value = reader.readString(32767);
                ClientPlayNetworkHandler receivedOn = client.getNetworkHandler();
                client.execute(() -> {
                    if (receivedOn == null || receivedOn != client.getNetworkHandler()) return;
                    try { result(JsonParser.parseString(value).getAsJsonObject()); }
                    catch (Exception e) { report("vizedit.client.invalid_data"); }
                });
            } catch (Exception e) { AreashintClient.LOGGER.warn("读取可视化编辑结果失败", e); }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> invalidate());
    }

    public static VizEditSession session() { return session; }
    public static boolean isBusy() { return busy; }
    public static boolean isDraftLocked() { return submittedAction != null; }
    public static boolean mapAvailable() { return mapAccess != null && mapAccess.available(); }
    public static void installMap(MapAccess access) { mapAccess = access; }

    public static void open() { open(MinecraftClient.getInstance().currentScreen); }
    public static void open(Screen parent) {
        if (session != null && !session.invalid) { show(); return; }
        if (session != null && session.dirty() && !session.readOnly) {
            MinecraftClient.getInstance().setScreen(VizEditDialog.unsaved(session, () -> requestOpen(parent, null, null)));
            return;
        }
        requestOpen(parent, null, null);
    }

    public static void openApproval(ApprovalRequest request, Screen parent) {
        if (session != null && session.dirty()) {
            MinecraftClient.getInstance().setScreen(VizEditDialog.unsaved(session, () -> requestOpen(parent, request.id, null)));
            return;
        }
        requestOpen(parent, request.id, null);
    }

    private static void requestOpen(Screen parent, String approvalId, String selected) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (busy || client.world == null || client.player == null) return;
        if (!ClientNetworkCompat.get().canSend(Channel.C2S_VIZEDIT_ACTION)) { report("vizedit.client.unavailable"); return; }
        JsonObject action = new JsonObject();
        action.addProperty("action", "open");
        String id = UUID.randomUUID().toString();
        action.addProperty("requestId", id);
        if (approvalId != null) action.addProperty("approvalId", approvalId);
        requestedParent = parent;
        selectAfterOpen = selected;
        connection = client.getNetworkHandler();
        worldDimension = client.world.getRegistryKey().getValue().toString();
        if (send(action)) { clearWaiting(); await(id, "open"); }
    }

    // 审批预览使用服务器审批快照，完全不建立写会话，也不改变正式域名数据。
    public static void preview(ApprovalRequest request, Screen parent) {
        if (session != null && session.dirty() && !session.readOnly) {
            MinecraftClient.getInstance().setScreen(VizEditDialog.unsaved(session, () -> previewNow(request, parent)));
            return;
        }
        previewNow(request, parent);
    }

    private static void previewNow(ApprovalRequest request, Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || !request.dimensionId.equals(client.world.getRegistryKey().getValue().toString())) {
            report("vizedit.client.wrong_dimension"); return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("dimensionId", request.dimensionId);
        data.addProperty("dimensionalName", request.dimensionId);
        data.add("areas", VizEditSession.GSON.toJsonTree(request.previewAreas));
        session = new VizEditSession(data, parent, true);
        connection = client.getNetworkHandler();
        worldDimension = request.dimensionId;
        if (!session.areas.isEmpty()) session.select(session.areas.get(0).getName());
        // 删除申请保留原轮廓用于核对，但预览必须以删除色标出，不能伪装成修改后仍存在的域名。
        if (request.vizEdit != null && "delete".equals(request.vizEdit.operation)) session.operation = "delete";
        session.mapMode = mapAvailable();
        centerInitial();
        show();
    }

    private static void receive(String id, int index, int count, String part) {
        if (count < 1 || count > 1024 || index < 0 || index >= count || part.length() > CHUNK_LENGTH) return;
        if (!TRANSFERS.containsKey(id) && TRANSFERS.size() >= 2) return;
        Transfer transfer = TRANSFERS.computeIfAbsent(id, ignored -> new Transfer(count));
        if (transfer.parts.length != count) { TRANSFERS.remove(id); return; }
        if (transfer.parts[index] == null) {
            transfer.parts[index] = part; transfer.received++; transfer.length += part.length();
        } else if (!transfer.parts[index].equals(part)) { TRANSFERS.remove(id); return; }
        if (transfer.length > MAX_LENGTH) { TRANSFERS.remove(id); return; }
        if (transfer.received != count) return;
        TRANSFERS.remove(id);
        try {
            JsonObject data = JsonParser.parseString(String.join("", transfer.parts)).getAsJsonObject();
            if (!"session".equals(VizEditSession.string(data, "kind", ""))
                    || !Objects.equals(awaitingId, VizEditSession.string(data, "requestId", null))) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (connection != client.getNetworkHandler() || client.world == null
                    || !Objects.equals(worldDimension, client.world.getRegistryKey().getValue().toString())
                    || !Objects.equals(worldDimension, VizEditSession.string(data, "dimensionId", null))) return;
            VizEditSession incoming = new VizEditSession(data, requestedParent, false);
            if (incoming.id.isEmpty()) throw new IllegalArgumentException("sessionId");
            session = incoming;
            clearWaiting();
            if (createAfterOpen) session.create();
            else if (selectAfterOpen != null) session.select(selectAfterOpen);
            createAfterOpen = false;
            selectAfterOpen = null;
            session.mapMode = mapAvailable();
            centerInitial();
            show();
        } catch (Exception e) {
            busy = false;
            report("vizedit.client.invalid_data");
            AreashintClient.LOGGER.warn("解析可视化编辑会话失败", e);
        }
    }

    private static void centerInitial() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) session.grid.center(client.player.getX(), client.player.getZ());
        if (session.draft != null && !session.draft.shape.isEmpty()) {
            session.grid.frame(session.draft.shape.getBounds2D(), 170, 64,
                Math.max(80, client.getWindow().getScaledWidth() - 180), Math.max(80, client.getWindow().getScaledHeight() - 96));
        }
    }

    public static void show() {
        if (session == null) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (session.mapMode && mapAvailable()) {
            try { if (mapAccess.open(session)) return; }
            catch (Throwable e) { AreashintClient.LOGGER.warn("Xaero 编辑画布打开失败，回退坐标网格", e); }
            session.status = I18nManager.translate("vizedit.client.map_unavailable");
        }
        session.mapMode = false;
        client.setScreen(new VizEditScreen(session, session.grid, null));
    }

    public static void switchCanvas() {
        if (session == null || busy) return;
        session.mapMode = !session.mapMode && mapAvailable();
        session.focusRequested = session.draft != null;
        show();
    }

    public static void requestClose() {
        if (session == null || busy) return;
        VizEditSession current = session;
        if (current.dirty() && !current.readOnly) {
            MinecraftClient.getInstance().setScreen(VizEditDialog.unsaved(current, () -> close(current.parentScreen)));
        } else close(current.parentScreen);
    }

    private static void close(Screen parent) {
        session = null;
        clearWaiting();
        MinecraftClient.getInstance().setScreen(parent);
    }

    public static void changeTarget(String name) {
        if (session == null || busy || isDraftLocked() || session.approvalId != null && !session.readOnly) return;
        Runnable action = () -> {
            if (session == null) return;
            if (name == null) session.create(); else session.select(name);
            show();
        };
        if (session.dirty()) MinecraftClient.getInstance().setScreen(VizEditDialog.unsaved(session, action));
        else action.run();
    }

    public static void review(boolean draftOnly, Runnable continuation) {
        if (session == null || busy || session.readOnly || session.invalid || session.draft == null) return;
        // 超时后保持已提交草稿不变，重试复用相同请求身份，避免再次创建审批申请。
        if (submittedAction != null) {
            if (send(submittedAction)) await(submittedAction.get("requestId").getAsString(), "submit");
            show(); return;
        }
        try {
            boolean deleting = "delete".equals(session.operation);
            int before = deleting ? 0 : areahint.vizedit.geometry.VizEditGeometry.contours(session.draft.shape).stream().mapToInt(List::size).sum();
            JsonObject fields = session.prepareSave();
            if (!deleting) session.status = I18nManager.translate("vizedit.client.save_normalized", before, fields.getAsJsonArray("vertices").size());
            MinecraftClient.getInstance().setScreen(VizEditDialog.review(session, fields, draftOnly, continuation));
        } catch (areahint.vizedit.geometry.VizEditGeometry.GeometryException e) {
            report(e.errorKey());
        } catch (IllegalArgumentException e) { report("vizedit.client.invalid_fields"); }
    }

    static void submit(JsonObject fields, boolean draftOnly, Runnable continuation) {
        if (session == null || session.invalid || session.readOnly || busy) return;
        JsonObject action = new JsonObject();
        String id = UUID.randomUUID().toString();
        action.addProperty("action", "submit"); action.addProperty("requestId", id);
        action.addProperty("sessionId", session.id); action.addProperty("operation", session.operation);
        action.addProperty("target", session.target); action.add("fields", fields);
        if (session.approvalId != null) action.addProperty("approvalMode", draftOnly || !session.canResubmit ? "draft" : "resubmit");
        if (!send(action)) return;
        submittedAction = action.deepCopy();
        afterSave = continuation;
        await(id, "submit");
        show();
    }

    private static void result(JsonObject result) {
        String requestId = VizEditSession.string(result, "requestId", null);
        if (!Objects.equals(awaitingId, requestId)) return;
        String resultSession = VizEditSession.string(result, "sessionId", "");
        if (session != null && !resultSession.isEmpty() && !Objects.equals(session.id, resultSession)) return;
        boolean success = result.has("success") && result.get("success").getAsBoolean();
        String messageKey = VizEditSession.string(result, "messageKey", null);
        if (messageKey != null) report(messageKey);
        if (!success) {
            busy = false; submittedAction = null; afterSave = null;
            // 过期或并发冲突的基线不可再复用；保留草稿并让下一次入口重新建立授权会话。
            if (session != null && ("vizedit.error.session".equals(messageKey) || "vizedit.error.conflict".equals(messageKey))) session.invalid = true;
            return;
        }
        if (!"submit".equals(awaitingAction) || session == null) { clearWaiting(); return; }
        String outcome = VizEditSession.string(result, "outcome", "");
        if (!List.of("saved", "submitted", "draft", "resubmitted").contains(outcome)) return;
        VizEditSession completed = session;
        Runnable continuation = afterSave;
        completed.originalDraft = completed.draft.copy();
        completed.history.clear();
        // 只有匹配请求和会话的成功响应才结束草稿，失败或超时仍然可以继续编辑。
        completed.operation = "update";
        clearWaiting();
        ApprovalClientManager.refresh();
        if (continuation != null) {
            // 切换目标需要新的服务器基线；保存成功的旧会话不可继续提交。
            completed.invalid = true;
            continuation.run();
            if (session == completed && !"open".equals(awaitingAction)) {
                String selected = completed.target;
                createAfterOpen = "create".equals(completed.operation);
                session = null;
                requestOpen(completed.parentScreen, null, selected);
            }
        } else {
            session = null;
            if (completed.approvalId != null && "draft".equals(outcome)) {
                ApprovalClientManager.afterVisualDraft(completed.approvalId, completed.approvalRevision + 1, completed.parentScreen);
            } else if (completed.approvalId != null && "resubmitted".equals(outcome)) ApprovalClientManager.returnToList();
            else MinecraftClient.getInstance().setScreen(completed.parentScreen);
        }
    }

    private static boolean send(JsonObject action) {
        String value = action.toString();
        if (value.length() > MAX_LENGTH) { report("vizedit.client.too_large"); return false; }
        try {
            if (!ClientNetworkCompat.get().canSend(Channel.C2S_VIZEDIT_ACTION)) { report("vizedit.client.unavailable"); return false; }
            if (value.length() <= CHUNK_LENGTH && value.getBytes(StandardCharsets.UTF_8).length < 30_000) {
                NetworkBuf buffer = ClientNetworkCompat.get().newBuffer(); buffer.writeString(value);
                ClientNetworkCompat.get().sendToServer(Channel.C2S_VIZEDIT_ACTION, buffer);
            } else {
                if (!ClientNetworkCompat.get().canSend(Channel.C2S_VIZEDIT_CHUNK)) { report("vizedit.client.unavailable"); return false; }
                List<String> chunks = new ArrayList<>();
                for (int start = 0; start < value.length();) {
                    int end = Math.min(value.length(), start + CHUNK_LENGTH);
                    if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1)) && Character.isLowSurrogate(value.charAt(end))) end--;
                    chunks.add(value.substring(start, end)); start = end;
                }
                String transferId = action.get("requestId").getAsString();
                for (int i = 0; i < chunks.size(); i++) {
                    NetworkBuf buffer = ClientNetworkCompat.get().newBuffer();
                    buffer.writeString(transferId); buffer.writeInt(i); buffer.writeInt(chunks.size()); buffer.writeString(chunks.get(i));
                    ClientNetworkCompat.get().sendToServer(Channel.C2S_VIZEDIT_CHUNK, buffer);
                }
            }
            return true;
        } catch (Exception e) {
            AreashintClient.LOGGER.warn("发送可视化编辑动作失败", e);
            report("vizedit.client.unavailable"); return false;
        }
    }

    private static void await(String id, String action) {
        awaitingId = id; awaitingAction = action; awaitingSince = System.currentTimeMillis(); busy = true;
    }
    private static void clearWaiting() { awaitingId = null; awaitingAction = null; busy = false; afterSave = null; submittedAction = null; }
    private static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (connection != null && (client.getNetworkHandler() != connection || client.world == null
                || !Objects.equals(worldDimension, client.world.getRegistryKey().getValue().toString()))) invalidate();
        long now = System.currentTimeMillis();
        TRANSFERS.values().removeIf(transfer -> now - transfer.createdAt > TIMEOUT);
        if (busy && now - awaitingSince > TIMEOUT) {
            busy = false;
            report("vizedit.client.timeout");
        }
    }
    private static void invalidate() {
        if (session != null) { session.invalid = true; session.status = I18nManager.translate("vizedit.client.invalid_session"); }
        connection = null; TRANSFERS.clear(); clearWaiting();
    }
    public static void report(String key) {
        String text = I18nManager.translate(key);
        if (session != null) session.status = text;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) client.player.sendMessage(Text.literal(text), false);
    }

    // 兼容实现仅从各版本 Xaero 桥接注册，共享客户端类不引用任何可选模组类型。
    public interface MapAccess {
        boolean available();
        boolean open(VizEditSession session);
    }
    private static final class Transfer {
        final String[] parts;
        final long createdAt = System.currentTimeMillis();
        int received;
        int length;
        Transfer(int count) { parts = new String[count]; }
    }
}
