package areahint.vizedit;

import areahint.approval.ApprovalManager;
import areahint.approval.ApprovalRequest;
import areahint.data.AreaData;
import areahint.dimensional.DimensionalNameManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.world.WorldFolderManager;
import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// 会话只存在于服务端主线程，客户端只持有随机令牌，不能自行上传或替换比较基线。
public final class VizEditServerService {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60_000L;
    private static final Gson GSON = VizEditChangeService.GSON;

    private VizEditServerService() { }

    private record Session(String id, String dimension, String world, JsonArray baseline,
                           String approvalId, int approvalRevision, long started) { }

    public static void clear(UUID player) { SESSIONS.remove(player); }
    public static void clearAll() { SESSIONS.clear(); }

    public static void close(UUID player, String sessionId) {
        Session session = SESSIONS.get(player);
        if (session != null && Objects.equals(session.id, sessionId)) SESSIONS.remove(player);
    }

    public static boolean canOpen(ServerPlayerEntity player) {
        return PermissionService.hasCommandPermission(player, PermissionNodes.VIZEDIT, 0);
    }

    public static boolean canCreate(ServerPlayerEntity player) {
        return PermissionService.hasNodeOr(player, PermissionNodes.EASYADD, () -> true);
    }

    public static VizEditChangeService.PermissionCheck permissions(ServerPlayerEntity player, JsonArray current) {
        List<AreaData> areas = toAreas(current);
        return (operation, original) -> "easyadd".equals(operation) ? canCreate(player)
            : AreaManagementCapabilityService.canPerform(player, operation, original, areas);
    }

    public static JsonObject open(ServerPlayerEntity player, JsonObject action) throws IOException, ApprovalManager.ApprovalException {
        if (!canOpen(player)) throw VizEditChangeService.error("denied");
        String dimension = dimension(player);
        JsonArray current = readAreas(file(dimension));
        String approvalId = VizEditChangeService.text(action, "approvalId", true);
        ApprovalRequest approval = approvalId == null ? null : ApprovalManager.editableVizEdit(player, approvalId);
        if (approval != null && !dimension.equals(approval.dimensionId)) throw VizEditChangeService.error("dimension");
        Session session = new Session(UUID.randomUUID().toString(), dimension, world(), current.deepCopy(),
            approvalId, approval == null ? 0 : approval.revision, System.currentTimeMillis());
        JsonObject response = new JsonObject();
        response.addProperty("kind", "session");
        response.addProperty("requestId", VizEditChangeService.text(action, "requestId", false));
        response.addProperty("sessionId", session.id);
        response.addProperty("dimensionId", dimension);
        response.addProperty("dimensionalName", DimensionalNameManager.getDimensionalName(dimension));
        response.addProperty("canCreate", canCreate(player));
        response.add("areas", current.deepCopy());
        List<AreaData> areas = toAreas(current);
        JsonObject capabilities = new JsonObject();
        for (AreaData area : areas) {
            capabilities.add(area.getName(), GSON.toJsonTree(AreaManagementCapabilityService.getAllowedOperations(player, area, areas)));
        }
        response.add("capabilities", capabilities);
        if (approval != null) {
            response.addProperty("approvalId", approvalId);
            response.addProperty("approvalRevision", approval.revision);
            response.addProperty("approvalStatus", approval.status);
            response.addProperty("canResubmit", "returned".equals(approval.status) && player.getUuidAsString().equals(approval.applicantId));
            response.addProperty("operation", approval.vizEdit.operation);
            response.addProperty("target", approval.vizEdit.targetName);
            response.add("fields", approval.vizEdit.fields.deepCopy());
        }
        // 超大列表拒绝打开，避免把不能完整传给客户端的基线登记成可提交会话。
        if (response.toString().length() > VizEditNetworking.MAX_MESSAGE_LENGTH) throw VizEditChangeService.error("size");
        SESSIONS.put(player.getUuid(), session);
        return response;
    }

    public static JsonObject submit(ServerPlayerEntity player, JsonObject action) throws IOException, ApprovalManager.ApprovalException {
        if (!canOpen(player)) throw VizEditChangeService.error("denied");
        Session session = SESSIONS.get(player.getUuid());
        String sessionId = VizEditChangeService.text(action, "sessionId", false);
        if (session == null || !session.id.equals(sessionId) || !session.world.equals(world())
                || !session.dimension.equals(dimension(player)) || System.currentTimeMillis() - session.started > SESSION_TIMEOUT) {
            throw VizEditChangeService.error("session");
        }
        String operation = VizEditChangeService.text(action, "operation", false);
        String target = VizEditChangeService.text(action, "target", true);
        JsonElement incoming = action.get("fields");
        if (incoming == null || !incoming.isJsonObject()) throw VizEditChangeService.error("invalid");
        String author = player.getGameProfile().getName();
        if (session.approvalId != null) {
            ApprovalRequest request = ApprovalManager.editableVizEdit(player, session.approvalId);
            if (request.revision != session.approvalRevision) throw VizEditChangeService.error("conflict");
            // 打回后仍只能修改同一目标和原操作，不能把修改申请偷偷改成删除或新建。
            if (!Objects.equals(operation, request.vizEdit.operation) || !Objects.equals(target, request.vizEdit.targetName)) {
                throw VizEditChangeService.error("invalid");
            }
            author = request.applicantName;
        }
        Path file = file(session.dimension);
        JsonArray current = readAreas(file);
        VizEditChangeService.Prepared prepared = VizEditChangeService.prepare(session.baseline, current, operation, target,
            incoming.getAsJsonObject(), session.dimension, DimensionalNameManager.getDimensionalName(session.dimension),
            author, permissions(player, current));
        validateDocumentSize(prepared);
        JsonObject result = new JsonObject();
        result.addProperty("requestId", VizEditChangeService.text(action, "requestId", false));
        result.addProperty("sessionId", session.id);
        result.addProperty("success", true);
        String outcome;
        String approvalId = session.approvalId;
        if (approvalId != null) {
            String mode = VizEditChangeService.text(action, "approvalMode", true);
            boolean resubmit = mode == null || "resubmit".equals(mode);
            if (mode != null && !Set.of("draft", "resubmit").contains(mode)) throw VizEditChangeService.error("invalid");
            ApprovalManager.saveVizEdit(player, approvalId, session.approvalRevision, prepared, resubmit);
            outcome = resubmit ? "resubmitted" : "draft";
        } else if (ApprovalManager.requiresApproval(player, "vizedit")) {
            approvalId = ApprovalManager.submitVizEdit(player, file, prepared);
            outcome = "submitted";
        } else {
            // 一个维度只有一次临时文件原子替换，所有后代与目标一起成功或一起失败。
            ApprovalManager.writeDocument(file, prepared.document());
            outcome = "saved";
            try {
                ServerNetworking.sendAllAreaDataToAll();
            } catch (RuntimeException exception) {
                // 同步失败不能把已经落盘的操作报告为保存失败，客户端仍能主动 reload 读取结果。
                areahint.Areashint.LOGGER.warn("可视化编辑已保存，广播同步失败", exception);
            }
        }
        result.addProperty("outcome", outcome);
        result.addProperty("messageKey", "vizedit.message." + outcome);
        if (approvalId != null) result.addProperty("approvalId", approvalId);
        SESSIONS.remove(player.getUuid());
        return result;
    }

    // 审批通过前从依赖快照再次重建结果，不信任持久化 after 中出现的非开放属性。
    public static VizEditChangeService.Prepared validateApproval(ApprovalRequest request, JsonArray current) {
        VizEditMetadata metadata = request.vizEdit;
        if (metadata == null || metadata.dependencies == null || metadata.fields == null) throw VizEditChangeService.error("invalid");
        var prepared = VizEditChangeService.prepare(metadata.dependencies, current, metadata.operation, metadata.targetName,
            metadata.fields, request.dimensionId, DimensionalNameManager.getDimensionalName(request.dimensionId),
            request.applicantName, (operation, area) -> true);
        // 旧申请若仍含高精度顶点，必须重新预览提交，不能在批准时悄悄改成另一条量化后的边界。
        if (!"delete".equals(metadata.operation) && !Objects.equals(metadata.fields.get("vertices"), prepared.fields().get("vertices")))
            throw VizEditChangeService.error("precision");
        validateDocumentSize(prepared);
        return prepared;
    }

    private static void validateDocumentSize(VizEditChangeService.Prepared prepared) {
        // 按真正落盘的缩进格式计数，避免提交紧凑 JSON 能通过而正式文件超出客户端分片上限。
        if (ApprovalManager.GSON.toJson(prepared.document()).length() > areahint.network.AreaDataChunkNetworking.MAX_LENGTH) {
            throw VizEditChangeService.error("size");
        }
    }

    public static Path file(String dimension) {
        // 现有域名存储仅支持三个标准维度，不能把同名自定义维度映射到主世界文件。
        if (!Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end").contains(dimension)) {
            throw VizEditChangeService.error("dimension");
        }
        String type = dimension.substring(dimension.indexOf(':') + 1);
        String name = Packets.getFileNameForDimension(type);
        if (name == null) throw VizEditChangeService.error("dimension");
        return WorldFolderManager.getWorldDimensionFile(name);
    }

    public static JsonArray readAreas(Path file) throws IOException {
        if (Files.notExists(file)) return new JsonArray();
        try {
            JsonElement document = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (!document.isJsonArray()) throw VizEditChangeService.error("invalid");
            VizEditChangeService.index(document.getAsJsonArray());
            return document.getAsJsonArray();
        } catch (JsonParseException exception) {
            throw new IOException("域名文件 JSON 无效", exception);
        }
    }

    public static List<AreaData> toAreas(JsonArray array) {
        List<AreaData> areas = new ArrayList<>();
        array.forEach(item -> areas.add(GSON.fromJson(item, AreaData.class)));
        return areas;
    }

    private static String dimension(ServerPlayerEntity player) {
        return player.getWorld().getRegistryKey().getValue().toString();
    }

    private static String world() {
        return WorldFolderManager.getCurrentWorldFolder().toAbsolutePath().normalize().toString();
    }
}
