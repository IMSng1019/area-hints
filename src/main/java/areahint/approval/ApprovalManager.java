package areahint.approval;

import areahint.Areashint;
import areahint.config.ServerConfig;
import areahint.data.AreaData;
import areahint.data.DimensionalNameData;
import areahint.file.FileManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.DimensionalNameNetworking;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.util.AreaPermissionUtil;
import areahint.world.WorldFolderManager;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// 所有方法由服务端主线程调用，申请文件是唯一的审批事实来源，客户端不能决定是否免审。
public final class ApprovalManager {
    // 显式保存JsonNull，新增和删除文档的before/after以及事务回滚目标不能丢失。
    public static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private static final Map<String, ApprovalRequest> REQUESTS = new LinkedHashMap<>();
    private static String worldKey;
    private static Path requestFolder;

    private ApprovalManager() { }

    public static void initialize(MinecraftServer server) {
        REQUESTS.clear();
        // 存档绝对路径隔离不同世界，申请仍统一保存在全局目录供全部管理员读取。
        String identity = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toAbsolutePath().normalize().toString();
        worldKey = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
        requestFolder = FileManager.getConfigFolder().resolve("approval-requests").resolve(worldKey);
        try {
            Files.createDirectories(requestFolder);
            recoverTransactions();
            try (var files = Files.list(requestFolder)) {
                files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                    try {
                        ApprovalRequest request = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), ApprovalRequest.class);
                        if (request != null && worldKey.equals(request.worldKey) && validId(request.id)) {
                            REQUESTS.put(request.id, normalizeRequest(request));
                        }
                    } catch (Exception e) {
                        Areashint.LOGGER.error("读取审批申请失败，保留原文件: {}", path, e);
                    }
                });
            }
        } catch (IOException e) {
            Areashint.LOGGER.error("初始化审批目录失败", e);
        }
    }

    public static void reset() {
        REQUESTS.clear();
        worldKey = null;
        requestFolder = null;
    }

    public static boolean isOperator(ServerPlayerEntity player) {
        return player != null && player.getServer() != null
            && player.hasPermissionLevel(2)
            && player.getServer().getPlayerManager().isOperator(player.getGameProfile());
    }

    public static boolean requiresApproval(ServerPlayerEntity player) {
        return player != null && ServerConfig.isSubmitApproval() && !isOperator(player);
    }

    // 城市化审批拥有独立开关，不能因为普通提交审批关闭而直接修改正式域名。
    public static boolean requiresApproval(ServerPlayerEntity player, String operation) {
        return player != null && isOperationEnabled(operation) && !isOperator(player);
    }

    private static boolean isOperationEnabled(String operation) {
        return "applyurbanarea".equals(operation) ? ServerConfig.isSubmitUrbanArea() : ServerConfig.isSubmitApproval();
    }

    public static boolean submitAreasIfRequired(ServerPlayerEntity player, String operation,
                                               Path file, List<AreaData> proposed) throws IOException {
        if ("applyurbanarea".equals(operation) && !isOperationEnabled(operation)) {
            throw new IOException("approval.error.disabled");
        }
        if (!requiresApproval(player, operation)) {
            return false;
        }
        JsonArray before = canonicalAreas(readDocument(file));
        JsonArray after = canonicalAreas(GSON.toJsonTree(proposed));
        Map<String, JsonObject> oldAreas = index(before);
        Map<String, JsonObject> newAreas = index(after);
        JsonArray removed = new JsonArray();
        JsonArray added = new JsonArray();
        for (var entry : oldAreas.entrySet()) {
            if (!entry.getValue().equals(newAreas.get(entry.getKey()))) {
                removed.add(entry.getValue().deepCopy());
            }
        }
        for (var entry : newAreas.entrySet()) {
            if (!entry.getValue().equals(oldAreas.get(entry.getKey()))) {
                added.add(entry.getValue().deepCopy());
            }
        }
        if (removed.isEmpty() && added.isEmpty()) {
            throw new IOException("approval.error.no_changes");
        }
        ApprovalRequest request = create(player, operation, player.getWorld().getRegistryKey().getValue().toString());
        request.changes.add(change("areas", file, removed, added));
        request.previewAreas = toAreas(added.isEmpty() ? removed : added);
        saveNew(request);
        return true;
    }

    public static boolean submitDocumentIfRequired(ServerPlayerEntity player, String operation, Path file,
            JsonElement proposed, String dimensionId, List<AreaData> previewAreas) throws IOException {
        if (!requiresApproval(player)) {
            return false;
        }
        JsonElement before = readDocument(file);
        JsonElement after = proposed == null ? JsonNull.INSTANCE : proposed;
        if (before.equals(after)) {
            throw new IOException("approval.error.no_changes");
        }
        ApprovalRequest request = create(player, operation, dimensionId);
        // 维度配置按维度ID保存差异，审批其中一个维度时不锁住或覆盖其他维度。
        if ("dimensional_names.json".equals(file.getFileName().toString())) {
            request.changes.add(collectionChange("dimensions", file,
                canonicalDimensions(before), canonicalDimensions(after), "dimensionId"));
        } else {
            request.changes.add(change("document", file, before, after));
        }
        if (previewAreas != null) {
            request.previewAreas = toAreas(GSON.toJsonTree(previewAreas).getAsJsonArray());
        }
        saveNew(request);
        return true;
    }

    private static ApprovalRequest create(ServerPlayerEntity player, String operation, String dimensionId) throws IOException {
        if (requestFolder == null) {
            throw new IOException("审批目录尚未初始化");
        }
        ApprovalRequest request = new ApprovalRequest();
        request.id = UUID.randomUUID().toString();
        request.worldKey = worldKey;
        request.worldName = WorldFolderManager.getCurrentWorldName();
        request.applicantId = player.getUuidAsString();
        request.applicantName = player.getGameProfile().getName();
        request.operation = operation;
        request.dimensionId = normalizeDimension(dimensionId);
        request.createdAt = request.updatedAt = System.currentTimeMillis();
        return request;
    }

    private static ApprovalRequest.Change change(String kind, Path file, JsonElement before, JsonElement after) throws IOException {
        Path root = WorldFolderManager.getCurrentWorldFolder().toAbsolutePath().normalize();
        Path target = file.toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new IOException("审批目标不属于当前世界目录");
        }
        ApprovalRequest.Change change = new ApprovalRequest.Change();
        change.kind = kind;
        change.path = root.relativize(target).toString().replace('\\', '/');
        change.before = before.deepCopy();
        change.after = after.deepCopy();
        return change;
    }

    private static void saveNew(ApprovalRequest request) throws IOException {
        persist(request);
        REQUESTS.put(request.id, request);
        // 通知失败不会撤销已经持久化的申请，玩家下次查询仍能收到该记录。
        try {
            ApprovalNetworking.broadcast(request, false);
        } catch (RuntimeException e) {
            Areashint.LOGGER.error("申请已保存，但同步审批列表失败", e);
        }
    }

    public static List<ApprovalRequest> visibleRequests(ServerPlayerEntity player) {
        boolean operator = isOperator(player);
        return REQUESTS.values().stream()
            .filter(request -> operator ? "pending".equals(request.status) : player.getUuidAsString().equals(request.applicantId))
            .sorted(Comparator.comparingLong((ApprovalRequest request) -> request.updatedAt).reversed())
            .toList();
    }

    public static ApprovalRequest findVisible(ServerPlayerEntity player, String id) throws ApprovalException {
        // 关闭提交开关后仍允许读取已经收到的回复，开关校验只限制后续修改和审批。
        ApprovalRequest request = REQUESTS.get(id);
        if (request == null || !Objects.equals(worldKey, request.worldKey)) {
            throw error("missing");
        }
        if (!isOperator(player) && !player.getUuidAsString().equals(request.applicantId)) {
            throw error("denied");
        }
        return request;
    }

    private static ApprovalRequest editable(ServerPlayerEntity player, String id, int revision) throws ApprovalException {
        ApprovalRequest stored = findVisible(player, id);
        if (!isOperationEnabled(stored.operation)) {
            throw error("disabled");
        }
        if (stored.revision != revision) {
            throw error("stale");
        }
        boolean allowed = isOperator(player) && "pending".equals(stored.status)
            || player.getUuidAsString().equals(stored.applicantId) && "returned".equals(stored.status);
        if (!allowed) {
            throw error("denied");
        }
        return copy(stored);
    }

    public static void saveDraft(ServerPlayerEntity player, JsonObject action) throws IOException, ApprovalException {
        ApprovalRequest request = editable(player, string(action, "id"), integer(action, "revision"));
        applyDraft(player, request, action.get("areas"));
        update(request);
        ApprovalNetworking.sendResult(player, true, "approval.message.saved", "draft", request.id);
        ApprovalNetworking.broadcast(request, false);
    }

    public static void decide(ServerPlayerEntity player, JsonObject action) throws IOException, ApprovalException {
        if (!isOperator(player)) {
            throw error("denied");
        }
        ApprovalRequest request = editable(player, string(action, "id"), integer(action, "revision"));
        if (!"pending".equals(request.status)) {
            throw error("denied");
        }
        String result = string(action, "result");
        if (!List.of("approved", "rejected", "returned").contains(result)) {
            throw error("invalid");
        }
        String comment = action.has("comment") ? string(action, "comment") : "";
        if (comment.length() > 4000) {
            throw error("invalid");
        }
        applyDraft(player, request, action.get("areas"));
        ApprovalRequest.Review review = new ApprovalRequest.Review();
        review.reviewerId = player.getUuidAsString();
        review.reviewerName = player.getGameProfile().getName();
        review.result = result;
        review.comment = comment;
        review.time = System.currentTimeMillis();
        request.reviews.add(review);
        request.status = result;
        // 批准先验证整个差异再写入，同一申请涉及的域名必须作为整体处理。
        if ("approved".equals(result)) {
            applyApproved(player, request);
        } else {
            update(request);
        }
        ApprovalNetworking.sendResult(player, true, "approval.message.reviewed", "decision", request.id);
        ApprovalNetworking.broadcast(request, true);
    }

    public static void resubmit(ServerPlayerEntity player, JsonObject action) throws IOException, ApprovalException {
        ApprovalRequest request = editable(player, string(action, "id"), integer(action, "revision"));
        if (!player.getUuidAsString().equals(request.applicantId) || !"returned".equals(request.status)) {
            throw error("denied");
        }
        // 历史必须完整上传并与服务端一致，实际保存仍沿用服务端副本而非客户端提供的人名或意见。
        JsonElement history = action.get("history");
        if (history == null || !GSON.toJsonTree(request.reviews).equals(history)) {
            throw error("history");
        }
        validateResubmitPermission(player, request);
        applyDraft(player, request, action.get("areas"));
        rebase(request);
        if ("applyurbanarea".equals(request.operation)) {
            // 重提以最新正式记录为基线，再次核验类型变化，已被其他申请城市化的域名不能重复提交。
            for (ApprovalRequest.Change change : request.changes) validateUrbanChange(change);
        }
        request.status = "pending";
        update(request);
        ApprovalNetworking.sendResult(player, true, "approval.message.resubmitted", "resubmit", request.id);
        ApprovalNetworking.broadcast(request, false);
    }

    // 临时编辑只允许调整已申请域名的顶点，不能借顶点包改写署名、级别、颜色或审批内容。
    private static void applyDraft(ServerPlayerEntity player, ApprovalRequest request, JsonElement incoming)
        throws IOException, ApprovalException {
        if (incoming == null || incoming.isJsonNull()) {
            return;
        }
        if (!incoming.isJsonArray() || incoming.getAsJsonArray().size() != request.previewAreas.size()) {
            throw error("invalid");
        }
        Map<String, JsonObject> drafts;
        try {
            // 与服务端快照共用模型规范化，忽略transient缓存并统一缺省字段与显式null。
            drafts = index(canonicalAreas(incoming));
        } catch (IOException e) {
            throw error("invalid");
        }
        Set<String> editableNames = new HashSet<>();
        for (ApprovalRequest.Change change : request.changes) {
            if ("areas".equals(change.kind)) {
                for (JsonElement item : change.after.getAsJsonArray()) {
                    editableNames.add(item.getAsJsonObject().get("name").getAsString());
                }
            }
        }
        for (int i = 0; i < request.previewAreas.size(); i++) {
            AreaData original = request.previewAreas.get(i);
            original.setSignatures(original.getSignatures());
            original.setType(original.getType());
            JsonObject draft = drafts.get(original.getName());
            if (draft == null) {
                throw error("invalid");
            }
            JsonObject expected = GSON.toJsonTree(original).getAsJsonObject();
            JsonObject attributes = draft.deepCopy();
            expected.remove("vertices");
            expected.remove("second-vertices");
            attributes.remove("vertices");
            attributes.remove("second-vertices");
            if (!expected.equals(attributes)) {
                throw error("invalid");
            }
            if (!editableNames.contains(original.getName())) {
                if (!GSON.toJsonTree(original).equals(draft)) {
                    throw error("invalid");
                }
                continue;
            }
            AreaData updated;
            try {
                updated = GSON.fromJson(draft, AreaData.class);
                // 未编辑顶点时保留原有二级顶点，不能因审批颜色或签名而附带修改几何数据。
                if (!Objects.equals(GSON.toJsonTree(original.getVertices()), draft.get("vertices"))) {
                    validateGeometry(updated);
                    validateDraftPermission(player, request, original, updated);
                } else {
                    updated.setSecondVertices(original.getSecondVertices());
                }
            } catch (RuntimeException e) {
                throw error("invalid");
            }
            request.previewAreas.set(i, updated);
            for (ApprovalRequest.Change change : request.changes) {
                if ("areas".equals(change.kind)) {
                    JsonArray after = change.after.getAsJsonArray();
                    for (int j = 0; j < after.size(); j++) {
                        if (updated.getName().equals(after.get(j).getAsJsonObject().get("name").getAsString())) {
                            after.set(j, GSON.toJsonTree(updated));
                        }
                    }
                }
            }
        }
    }

    private static void validateGeometry(AreaData area) throws ApprovalException {
        if (area == null || area.getVertices() == null || area.getVertices().size() < 3 || area.getVertices().size() > 4096) {
            throw error("invalid");
        }
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (AreaData.Vertex vertex : area.getVertices()) {
            if (vertex == null || !Double.isFinite(vertex.getX()) || !Double.isFinite(vertex.getZ())
                || Math.abs(vertex.getX()) > 30000000 || Math.abs(vertex.getZ()) > 30000000) {
                throw error("invalid");
            }
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minZ = Math.min(minZ, vertex.getZ());
            maxZ = Math.max(maxZ, vertex.getZ());
        }
        if (minX == maxX || minZ == maxZ) {
            throw error("invalid");
        }
        // 二级顶点由服务端根据一级顶点重建，不相信客户端上报的包围范围。
        area.setSecondVertices(List.of(new AreaData.Vertex(minX, maxZ), new AreaData.Vertex(maxX, maxZ),
            new AreaData.Vertex(maxX, minZ), new AreaData.Vertex(minX, minZ)));
    }

    // 顶点包不能绕过特殊命令的权限：移动或重排同时视为删除与添加，管理员也遵守显式拒绝节点。
    private static void validateDraftPermission(ServerPlayerEntity player, ApprovalRequest request,
                                                AreaData before, AreaData after) throws IOException, ApprovalException {
        Map<JsonElement, Integer> oldVertices = vertexCounts(before);
        Map<JsonElement, Integer> newVertices = vertexCounts(after);
        boolean added = newVertices.entrySet().stream().anyMatch(entry -> entry.getValue() > oldVertices.getOrDefault(entry.getKey(), 0));
        boolean removed = oldVertices.entrySet().stream().anyMatch(entry -> entry.getValue() > newVertices.getOrDefault(entry.getKey(), 0));
        if (!added && !removed) {
            added = true;
            removed = true;
        }
        if (added && !PermissionService.hasCommandPermission(player, PermissionNodes.ADDHINT, 0)
            || removed && !PermissionService.hasCommandPermission(player, PermissionNodes.DELETEHINT, 0)) {
            throw error("denied");
        }
        for (ApprovalRequest.Change change : request.changes) {
            if (!"areas".equals(change.kind)) continue;
            List<AreaData> currentAreas = toAreas(canonicalAreas(readDocument(resolve(change.path))));
            AreaData current = AreaPermissionUtil.findByName(currentAreas, before.getName());
            if ("applyurbanarea".equals(request.operation)) {
                // 城市化临时草稿沿用该申请的域名归属，录点命令的显式拒绝仍由上面的节点校验拦截。
                if (current == null || !areahint.urban.ApplyUrbanAreaCommand.canUrbanize(player, current)) {
                    throw error("denied");
                }
                continue;
            }
            if (current != null && (added && !AreaManagementCapabilityService.canPerform(player,
                AreaManagementCapabilityService.ADD_HINT, current, currentAreas)
                || removed && !AreaManagementCapabilityService.canPerform(player,
                    AreaManagementCapabilityService.DELETE_HINT, current, currentAreas))) {
                throw error("denied");
            }
        }
    }

    private static Map<JsonElement, Integer> vertexCounts(AreaData area) {
        Map<JsonElement, Integer> counts = new HashMap<>();
        if (area.getVertices() != null) {
            for (AreaData.Vertex vertex : area.getVertices()) {
                counts.merge(GSON.toJsonTree(vertex), 1, Integer::sum);
            }
        }
        return counts;
    }

    // 打回后再次申请重新使用原功能的权限规则，签名或权限节点已撤销时不能继续沿用旧资格。
    private static void validateResubmitPermission(ServerPlayerEntity player, ApprovalRequest request)
        throws IOException, ApprovalException {
        if ("applyurbanarea".equals(request.operation)) {
            validateUrbanResubmitPermission(player, request);
            return;
        }
        if (isOperator(player)) {
            return;
        }
        String operation = request.operation;
        boolean allowed;
        if ("add".equals(operation)) {
            allowed = PermissionService.hasCommandPermission(player, PermissionNodes.ADD, 2);
        } else if ("easyadd".equals(operation)) {
            allowed = PermissionService.hasNodeOr(player, PermissionNodes.EASYADD, () -> true);
        } else if ("dimensionalityname".equals(operation)) {
            String currentName = areahint.dimensional.DimensionalNameManager.getDimensionalName(request.dimensionId);
            allowed = PermissionService.hasCommandPermission(player, PermissionNodes.DIMENSIONALITY_NAME, 2)
                || request.dimensionId.equals(currentName)
                    && (PermissionService.hasCommandPermission(player, PermissionNodes.FIRST_DIM_NAME, 0)
                        || PermissionService.hasCommandPermission(player, PermissionNodes.FIRST_DIM_NAME_SKIP, 0));
        } else if ("dimensionalitycolor".equals(operation)) {
            allowed = PermissionService.hasCommandPermission(player, PermissionNodes.DIMENSIONALITY_COLOR, 2);
        } else if (operation.endsWith("dimensionalitydescription")) {
            String node = "deletedimensionalitydescription".equals(operation)
                ? PermissionNodes.DELETE_DIMENSIONALITY_DESCRIPTION : PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION;
            allowed = PermissionService.hasCommandPermission(player, node, 2);
        } else {
            List<AreaData> currentAreas = new ArrayList<>();
            String targetName = null;
            for (ApprovalRequest.Change change : request.changes) {
                if (!"areas".equals(change.kind)) continue;
                currentAreas = toAreas(canonicalAreas(readDocument(resolve(change.path))));
                Map<String, JsonObject> before = index(change.before.getAsJsonArray());
                if (before.isEmpty()) continue;
                targetName = before.keySet().iterator().next();
                if ("rename".equals(operation)) {
                    Set<String> oldNames = new HashSet<>(before.keySet());
                    oldNames.removeAll(index(change.after.getAsJsonArray()).keySet());
                    if (oldNames.size() == 1) targetName = oldNames.iterator().next();
                }
                break;
            }
            if (targetName == null && !request.previewAreas.isEmpty()) {
                targetName = request.previewAreas.get(0).getName();
                String dimensionPath = request.dimensionId.substring(request.dimensionId.lastIndexOf(':') + 1);
                String fileName = Packets.getFileNameForDimension(Packets.convertDimensionPathToType(dimensionPath));
                if (fileName != null) {
                    currentAreas = toAreas(canonicalAreas(readDocument(WorldFolderManager.getWorldDimensionFile(fileName))));
                }
            }
            AreaData current = AreaPermissionUtil.findByName(currentAreas, targetName);
            allowed = AreaManagementCapabilityService.canPerform(player, operation, current, currentAreas);
        }
        if (!allowed) {
            throw error("denied");
        }
    }

    private static void validateUrbanResubmitPermission(ServerPlayerEntity player, ApprovalRequest request)
            throws IOException, ApprovalException {
        if (!ServerConfig.isSubmitUrbanArea()) throw error("disabled");
        if (!PermissionService.hasCommandPermission(player, PermissionNodes.APPLY_URBAN_AREA, 0)) throw error("denied");
        if (request.changes.isEmpty()) throw error("invalid");
        for (ApprovalRequest.Change change : request.changes) {
            validateUrbanChange(change);
            List<AreaData> currentAreas = toAreas(canonicalAreas(readDocument(resolve(change.path))));
            for (JsonElement item : change.before.getAsJsonArray()) {
                AreaData current = AreaPermissionUtil.findByName(currentAreas, item.getAsJsonObject().get("name").getAsString());
                if (current == null) throw error("conflict");
                if (!areahint.urban.ApplyUrbanAreaCommand.canUrbanize(player, current)) throw error("denied");
                if (!AreaData.RURAL_TYPE.equals(current.getType())) throw error("conflict");
            }
        }
    }

    private static void applyApproved(ServerPlayerEntity reviewer, ApprovalRequest request) throws IOException, ApprovalException {
        Map<Path, JsonElement> originals = new LinkedHashMap<>();
        Map<Path, JsonElement> replacements = new LinkedHashMap<>();
        for (ApprovalRequest.Change change : request.changes) {
            if ("applyurbanarea".equals(request.operation)) validateUrbanChange(change);
            Path file = resolve(change.path);
            JsonElement current = readDocument(file);
            originals.put(file, current);
            if ("areas".equals(change.kind)) {
                // 审批只批准原操作允许的属性变化，不能把历史申请中的夹带字段转成模式授权。
                validateGamemodeChange(request.operation, change);
                validateAreaTypeChange(request.operation, change);
                JsonArray merged = "applyurbanarea".equals(request.operation)
                    ? mergeUrbanAreas(change, current) : mergeAreas(change, current, request.operation);
                validateRelationships(merged, change);
                replacements.put(file, merged);
            } else if ("dimensions".equals(change.kind)) {
                replacements.put(file, mergeCollection(change, current, canonicalDimensions(current), "dimensionId"));
            } else {
                if (!current.equals(change.before)) {
                    throw error("conflict");
                }
                replacements.put(file, change.after);
            }
        }
        // 正式数据和审批回复使用事务日志关联，重启时依据已持久化的审批状态完成提交或恢复。
        request.revision++;
        request.updatedAt = System.currentTimeMillis();
        ApprovalTransaction transaction = new ApprovalTransaction();
        transaction.request = copy(request);
        Path root = WorldFolderManager.getCurrentWorldFolder().toAbsolutePath().normalize();
        originals.forEach((path, data) -> transaction.before.put(root.relativize(path).toString(), data));
        replacements.forEach((path, data) -> transaction.after.put(root.relativize(path).toString(), data));
        Path journal = requestFolder.resolve(request.id + ".transaction");
        writeDocument(journal, GSON.toJsonTree(transaction));
        try {
            for (var entry : replacements.entrySet()) {
                writeDocument(entry.getKey(), entry.getValue());
            }
            persist(request);
        } catch (IOException e) {
            boolean restored = true;
            for (var entry : originals.entrySet()) {
                try {
                    writeDocument(entry.getKey(), entry.getValue());
                } catch (IOException restoreError) {
                    restored = false;
                    e.addSuppressed(restoreError);
                }
            }
            if (restored) {
                try {
                    Files.deleteIfExists(journal);
                } catch (IOException cleanupError) {
                    e.addSuppressed(cleanupError);
                }
            }
            throw e;
        }
        REQUESTS.put(request.id, request);
        try {
            Files.deleteIfExists(journal);
        } catch (IOException e) {
            Areashint.LOGGER.warn("审批已完成，事务日志将在下次启动时清理: {}", journal, e);
        }
        areahint.dimensional.DimensionalNameManager.loadDimensionalNames();
        if ("applyurbanarea".equals(request.operation) && Areashint.getServer() != null) {
            // 城市化成功后复用完整 reload，令客户端数据、域名缓存及现有模式策略同步刷新。
            areahint.command.ServerCommands.reload(reviewer.getCommandSource());
            return;
        }
        ServerNetworking.sendAllAreaDataToAll();
        MinecraftServer server = Areashint.getServer();
        if (server != null) {
            for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
                DimensionalNameNetworking.sendDimensionalNamesToClient(online);
            }
        }
    }

    private static JsonArray mergeAreas(ApprovalRequest.Change change, JsonElement current, String operation)
            throws IOException, ApprovalException {
        JsonArray merged = mergeCollection(change, current, canonicalAreas(current), "name");
        Map<String, JsonObject> rawAreas = current.isJsonNull() ? Map.of() : index(current.getAsJsonArray());
        Map<String, JsonObject> before = index(change.before.getAsJsonArray());
        Map<String, JsonObject> after = index(change.after.getAsJsonArray());
        Set<String> removedNames = new HashSet<>(before.keySet());
        removedNames.removeAll(after.keySet());
        // 改名与划分产生的新名称仍继承原域名是否显式声明类型，不能把原有显式 rural 丢掉。
        String inheritedName = "dividearea".equals(operation) && before.size() == 1
            ? before.keySet().iterator().next()
            : "rename".equals(operation) && removedNames.size() == 1
                ? removedNames.iterator().next() : null;
        for (JsonElement element : merged) {
            JsonObject area = element.getAsJsonObject();
            String name = area.get("name").getAsString();
            if (!after.containsKey(name) || !AreaData.RURAL_TYPE.equals(typeValue(area))) continue;
            JsonObject original = rawAreas.get(name);
            if (original == null && inheritedName != null) original = rawAreas.get(inheritedName);
            JsonElement explicitType = original == null ? null : original.get("type");
            // 快照为冲突校验补齐的 rural 只存在于审批记录，正式新域名及原本省略类型的域名继续不写 type。
            if (explicitType == null || explicitType.isJsonNull() || explicitType.getAsString().isBlank()) {
                area.remove("type");
            }
        }
        return merged;
    }

    // 城市化先复用完整冲突检查，再从当前原始对象覆盖获批字段，保留模型未识别的自定义属性。
    private static JsonArray mergeUrbanAreas(ApprovalRequest.Change change, JsonElement current)
            throws IOException, ApprovalException {
        JsonArray merged = mergeAreas(change, current, "applyurbanarea");
        Map<String, JsonObject> rawAreas = index(current.getAsJsonArray());
        Map<String, JsonObject> before = index(change.before.getAsJsonArray());
        Map<String, JsonObject> after = index(change.after.getAsJsonArray());
        for (int i = 0; i < merged.size(); i++) {
            String name = merged.get(i).getAsJsonObject().get("name").getAsString();
            JsonObject proposed = after.get(name);
            if (proposed == null) continue;
            JsonObject preserved = rawAreas.get(name).deepCopy();
            preserved.add("type", proposed.get("type").deepCopy());
            for (String key : List.of("vertices", "second-vertices")) {
                // 未实际编辑的几何字段也保留原始 JSON，避免仅城市化时重写顶点中的扩展属性。
                if (!Objects.equals(before.get(name).get(key), proposed.get(key))) {
                    preserved.add(key, proposed.get(key).deepCopy());
                }
            }
            merged.set(i, preserved);
        }
        return merged;
    }

    // 城市化仅能把既有农村域名改为城市，审批内的特殊编辑只额外允许调整一级和二级顶点。
    private static void validateUrbanChange(ApprovalRequest.Change change) throws IOException, ApprovalException {
        if (!"areas".equals(change.kind) || !change.before.isJsonArray() || !change.after.isJsonArray()) {
            throw error("invalid");
        }
        Map<String, JsonObject> before = index(change.before.getAsJsonArray());
        Map<String, JsonObject> after = index(change.after.getAsJsonArray());
        if (before.isEmpty() || !before.keySet().equals(after.keySet())) throw error("invalid");
        for (var entry : before.entrySet()) {
            JsonObject original = entry.getValue();
            JsonObject proposed = after.get(entry.getKey());
            if (!AreaData.RURAL_TYPE.equals(typeValue(original)) || !AreaData.URBAN_TYPE.equals(typeValue(proposed))) {
                throw error("invalid");
            }
            JsonObject expectedAttributes = original.deepCopy();
            JsonObject actualAttributes = proposed.deepCopy();
            for (String key : List.of("type", "vertices", "second-vertices")) {
                expectedAttributes.remove(key);
                actualAttributes.remove(key);
            }
            if (!expectedAttributes.equals(actualAttributes)) throw error("invalid");
        }
    }

    // 普通审批不能夹带城市化，改名和分割仍从原域名继承类型，新建域名默认保持农村类型。
    private static void validateAreaTypeChange(String operation, ApprovalRequest.Change change)
            throws IOException, ApprovalException {
        if ("applyurbanarea".equals(operation)) return;
        Map<String, JsonObject> before = index(change.before.getAsJsonArray());
        Map<String, JsonObject> after = index(change.after.getAsJsonArray());
        Set<String> removedNames = new HashSet<>(before.keySet());
        removedNames.removeAll(after.keySet());
        JsonObject original = "dividearea".equals(operation) && before.size() == 1
            ? before.values().iterator().next()
            : "rename".equals(operation) && removedNames.size() == 1
                ? before.get(removedNames.iterator().next()) : null;
        for (var entry : after.entrySet()) {
            JsonObject prior = before.getOrDefault(entry.getKey(), original);
            String expectedType = prior == null ? AreaData.RURAL_TYPE : typeValue(prior);
            if (!expectedType.equals(typeValue(entry.getValue()))) throw error("invalid");
        }
    }

    private static String typeValue(JsonObject area) {
        return GSON.fromJson(area, AreaData.class).getType();
    }

    private static void validateGamemodeChange(String operation, ApprovalRequest.Change change)
            throws IOException, ApprovalException {
        // 原有 add JSON 指令本身要求管理员权限，继续允许其显式配置游戏模式。
        if ("add".equals(operation)) {
            return;
        }
        Map<String, JsonObject> before = index(change.before.getAsJsonArray());
        Map<String, JsonObject> after = index(change.after.getAsJsonArray());
        Set<String> removedNames = new HashSet<>(before.keySet());
        removedNames.removeAll(after.keySet());
        JsonObject original = "dividearea".equals(operation) && before.size() == 1
            ? before.values().iterator().next()
            : "rename".equals(operation) && removedNames.size() == 1
                ? before.get(removedNames.iterator().next()) : null;
        for (var entry : after.entrySet()) {
            JsonObject prior = before.getOrDefault(entry.getKey(), original);
            JsonElement expectedMode = prior == null ? JsonNull.INSTANCE : gamemodeValue(prior);
            if (!expectedMode.equals(gamemodeValue(entry.getValue()))) {
                throw error("invalid");
            }
        }
    }

    private static JsonElement gamemodeValue(JsonObject area) {
        JsonElement value = area.get("gamemode");
        return value == null ? JsonNull.INSTANCE : value;
    }

    // 按稳定主键检查申请涉及的记录，保留当前文件中所有无关记录及其扩展字段。
    private static JsonArray mergeCollection(ApprovalRequest.Change change, JsonElement current,
                                             JsonArray canonical, String key) throws IOException, ApprovalException {
        JsonArray array = current.isJsonNull() ? new JsonArray() : current.getAsJsonArray();
        Map<String, JsonObject> actual = index(canonical, key);
        Map<String, JsonObject> before = index(change.before.getAsJsonArray(), key);
        Map<String, JsonObject> after = index(change.after.getAsJsonArray(), key);
        for (var entry : before.entrySet()) {
            if (!entry.getValue().equals(actual.get(entry.getKey()))) {
                throw error("conflict");
            }
        }
        for (String name : after.keySet()) {
            if (!before.containsKey(name) && actual.containsKey(name)) {
                throw error("conflict");
            }
        }
        JsonArray result = new JsonArray();
        Set<String> written = new HashSet<>();
        // 未受影响的原始 JSON 原样保留，避免审批覆盖其他申请已批准的域名或扩展字段。
        for (JsonElement item : array) {
            String name = item.getAsJsonObject().get(key).getAsString();
            if (!before.containsKey(name)) {
                result.add(item.deepCopy());
            } else if (after.containsKey(name)) {
                result.add(after.get(name).deepCopy());
                written.add(name);
            }
        }
        for (var entry : after.entrySet()) {
            if (!written.contains(entry.getKey())) {
                result.add(entry.getValue().deepCopy());
            }
        }
        return result;
    }

    private static void validateRelationships(JsonArray merged, ApprovalRequest.Change change) throws IOException, ApprovalException {
        Map<String, JsonObject> all = index(merged);
        Set<String> removed = new HashSet<>(index(change.before.getAsJsonArray()).keySet());
        removed.removeAll(index(change.after.getAsJsonArray()).keySet());
        for (JsonObject area : all.values()) {
            JsonElement parent = area.get("base-name");
            if (parent != null && !parent.isJsonNull() && removed.contains(parent.getAsString())) {
                throw error("conflict");
            }
        }
        for (JsonElement item : change.after.getAsJsonArray()) {
            AreaData area = GSON.fromJson(item, AreaData.class);
            if (!area.isValid()) {
                throw error("invalid");
            }
            if (area.getLevel() > 1) {
                JsonObject parent = all.get(area.getBaseName());
                if (parent == null || parent.get("level").getAsInt() != area.getLevel() - 1) {
                    throw error("conflict");
                }
            }
        }
    }

    // 打回后重新提交以当前正式数据为基线，保留原申请实际修改的字段，其余字段沿用最新值。
    private static void rebase(ApprovalRequest request) throws IOException, ApprovalException {
        for (ApprovalRequest.Change change : request.changes) {
            JsonElement current = readDocument(resolve(change.path));
            if (!"areas".equals(change.kind) && !"dimensions".equals(change.kind)) {
                change.before = current;
                continue;
            }
            String identityKey = "dimensions".equals(change.kind) ? "dimensionId" : "name";
            Map<String, JsonObject> actual = index("dimensions".equals(change.kind)
                ? canonicalDimensions(current) : canonicalAreas(current), identityKey);
            Map<String, JsonObject> before = index(change.before.getAsJsonArray(), identityKey);
            Map<String, JsonObject> after = index(change.after.getAsJsonArray(), identityKey);
            Set<String> removedNames = new HashSet<>(before.keySet());
            removedNames.removeAll(after.keySet());
            Set<String> addedNames = new HashSet<>(after.keySet());
            addedNames.removeAll(before.keySet());
            String renamedFrom = "rename".equals(request.operation) && removedNames.size() == 1 && addedNames.size() == 1
                ? removedNames.iterator().next() : null;
            String dividedFrom = "dividearea".equals(request.operation) && before.size() == 1
                ? before.keySet().iterator().next() : null;
            JsonArray newBefore = new JsonArray();
            JsonArray newAfter = new JsonArray();
            for (String name : before.keySet()) {
                if (!actual.containsKey(name)) {
                    throw error("conflict");
                }
                newBefore.add(actual.get(name).deepCopy());
            }
            for (var entry : after.entrySet()) {
                String name = entry.getKey();
                if (!before.containsKey(name)) {
                    if (actual.containsKey(name)) {
                        throw error("conflict");
                    }
                    // 改名后重新申请仍继承旧域名最新的未修改字段，避免覆盖后来更新的颜色或签名。
                    JsonObject added = renamedFrom == null ? entry.getValue().deepCopy()
                        : rebaseObject(before.get(renamedFrom), entry.getValue(), actual.get(renamedFrom));
                    // 分割申请被打回期间管理员可能更新原域名模式，再提交时两个结果都继承最新值。
                    if (dividedFrom != null) {
                        added.add("gamemode", gamemodeValue(actual.get(dividedFrom)).deepCopy());
                        added.addProperty("type", typeValue(actual.get(dividedFrom)));
                    }
                    newAfter.add(added);
                    continue;
                }
                newAfter.add(rebaseObject(before.get(name), entry.getValue(), actual.get(name)));
            }
            change.before = newBefore;
            change.after = newAfter;
            if ("areas".equals(change.kind)) {
                request.previewAreas = toAreas(newAfter.isEmpty() ? newBefore : newAfter);
            }
        }
    }

    // 仅重放申请真正改动的字段，其他字段从当前正式记录继承。
    private static JsonObject rebaseObject(JsonObject before, JsonObject after, JsonObject current) {
        JsonObject rebased = current.deepCopy();
        Set<String> keys = new HashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            if (!Objects.equals(before.get(key), after.get(key))) {
                if (after.has(key)) {
                    rebased.add(key, after.get(key).deepCopy());
                } else {
                    rebased.remove(key);
                }
            }
        }
        return rebased;
    }

    public static AreaData previewArea(ServerPlayerEntity player, String id, int index, boolean editing) throws ApprovalException {
        ApprovalRequest request = findVisible(player, id);
        if (editing) {
            editable(player, id, request.revision);
        }
        if (index < 0 || index >= request.previewAreas.size()) {
            throw error("preview_unavailable");
        }
        AreaData area = request.previewAreas.get(index);
        if (editing) {
            boolean found = false;
            for (ApprovalRequest.Change change : request.changes) {
                if ("areas".equals(change.kind)) {
                    for (JsonElement item : change.after.getAsJsonArray()) {
                        found |= area.getName().equals(item.getAsJsonObject().get("name").getAsString());
                    }
                }
            }
            if (!found) {
                throw error("preview_unavailable");
            }
        }
        return GSON.fromJson(GSON.toJsonTree(area), AreaData.class);
    }

    private static void update(ApprovalRequest request) throws IOException {
        request.revision++;
        request.updatedAt = System.currentTimeMillis();
        persist(request);
        REQUESTS.put(request.id, request);
    }

    private static void persist(ApprovalRequest request) throws IOException {
        if (requestFolder == null || !validId(request.id)) {
            throw new IOException("审批存储未就绪");
        }
        writeDocument(requestFolder.resolve(request.id + ".json"), GSON.toJsonTree(request));
    }

    // 事务日志保留原文与候选文档，服务端进程中断后也不会出现已批准却没有回复记录的状态。
    private static void recoverTransactions() throws IOException {
        try (var files = Files.list(requestFolder)) {
            List<Path> journals = files.filter(path -> path.getFileName().toString().endsWith(".transaction")).toList();
            for (Path journal : journals) {
                try {
                    ApprovalTransaction transaction = GSON.fromJson(readDocument(journal), ApprovalTransaction.class);
                    if (transaction == null || transaction.request == null || !validId(transaction.request.id)
                        || !Objects.equals(worldKey, transaction.request.worldKey)) {
                        throw new IOException("审批事务日志无效");
                    }
                    JsonElement saved = readDocument(requestFolder.resolve(transaction.request.id + ".json"));
                    ApprovalRequest completed = saved.isJsonNull() ? null : GSON.fromJson(saved, ApprovalRequest.class);
                    boolean committed = completed != null && completed.revision == transaction.request.revision
                        && "approved".equals(completed.status);
                    // 正式文件先于审批记录落盘，已提交日志只需清理，重放会覆盖批准后其他合法修改。
                    if (!committed) {
                        for (var entry : transaction.before.entrySet()) {
                            JsonElement current = readDocument(resolve(entry.getKey()));
                            JsonElement expected = transaction.after.get(entry.getKey());
                            if (!current.equals(entry.getValue()) && !current.equals(expected)) {
                                throw new IOException("审批恢复目标已被其他操作修改，保留现有数据和日志: " + entry.getKey());
                            }
                        }
                        for (var entry : transaction.before.entrySet()) {
                            writeDocument(resolve(entry.getKey()), entry.getValue());
                        }
                    }
                    Files.deleteIfExists(journal);
                } catch (IOException | RuntimeException e) {
                    // 个别恢复记录异常不应阻止其他申请加载，也不应覆盖管理员后来手动修改的数据。
                    Areashint.LOGGER.error("恢复审批事务失败，保留日志等待处理: {}", journal, e);
                }
            }
        }
    }

    private static final class ApprovalTransaction {
        private ApprovalRequest request;
        private Map<String, JsonElement> before = new LinkedHashMap<>();
        private Map<String, JsonElement> after = new LinkedHashMap<>();
    }

    public static void writeDocument(Path file, JsonElement data) throws IOException {
        if (data == null || data.isJsonNull()) {
            Files.deleteIfExists(file);
            return;
        }
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonElement readDocument(Path file) throws IOException {
        if (Files.notExists(file)) {
            return JsonNull.INSTANCE;
        }
        try {
            return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IOException("读取审批目标 JSON 失败", e);
        }
    }

    private static Path resolve(String relative) throws IOException {
        Path root = WorldFolderManager.getCurrentWorldFolder().toAbsolutePath().normalize();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root) || !relative.endsWith(".json")) {
            throw new IOException("审批记录目标路径无效");
        }
        return path;
    }

    private static JsonArray canonicalAreas(JsonElement data) throws IOException {
        if (data == null || data.isJsonNull()) {
            return new JsonArray();
        }
        if (!data.isJsonArray()) {
            throw new IOException("域名文件必须是 JSON 数组");
        }
        JsonArray result = new JsonArray();
        for (JsonElement item : data.getAsJsonArray()) {
            AreaData area = GSON.fromJson(item, AreaData.class);
            if (area == null) {
                throw new IOException("无效的域名数据");
            }
            // getter统一空签名列表，避免客户端展示签名时的清理影响草稿一致性校验。
            area.setSignatures(area.getSignatures());
            // 缺失类型与显式 rural 等价，统一后不会把升级前的域名误判为并发修改。
            area.setType(area.getType());
            result.add(GSON.toJsonTree(area));
        }
        return result;
    }

    private static JsonArray canonicalDimensions(JsonElement data) throws IOException {
        JsonArray result = new JsonArray();
        if (data == null || data.isJsonNull()) {
            return result;
        }
        if (!data.isJsonArray()) {
            throw new IOException("维度域名文件必须是 JSON 数组");
        }
        for (JsonElement item : data.getAsJsonArray()) {
            DimensionalNameData dimension = GSON.fromJson(item, DimensionalNameData.class);
            if (dimension == null || dimension.getDimensionId() == null || dimension.getDisplayName() == null) {
                throw new IOException("无效的维度域名数据");
            }
            result.add(GSON.toJsonTree(dimension));
        }
        return result;
    }

    private static ApprovalRequest.Change collectionChange(String kind, Path file, JsonArray before,
                                                           JsonArray after, String key) throws IOException {
        Map<String, JsonObject> oldItems = index(before, key);
        Map<String, JsonObject> newItems = index(after, key);
        JsonArray removed = new JsonArray();
        JsonArray added = new JsonArray();
        oldItems.forEach((id, item) -> {
            if (!item.equals(newItems.get(id))) removed.add(item.deepCopy());
        });
        newItems.forEach((id, item) -> {
            if (!item.equals(oldItems.get(id))) added.add(item.deepCopy());
        });
        if (removed.isEmpty() && added.isEmpty()) {
            throw new IOException("approval.error.no_changes");
        }
        return change(kind, file, removed, added);
    }

    private static Map<String, JsonObject> index(JsonArray array) throws IOException {
        return index(array, "name");
    }

    private static Map<String, JsonObject> index(JsonArray array, String key) throws IOException {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement item : array) {
            if (!item.isJsonObject() || !item.getAsJsonObject().has(key)
                || !item.getAsJsonObject().get(key).isJsonPrimitive()
                || !item.getAsJsonObject().get(key).getAsJsonPrimitive().isString()) {
                throw new IOException("无效的域名数据");
            }
            JsonObject object = item.getAsJsonObject();
            String name = object.get(key).getAsString();
            if (result.put(name, object) != null) {
                throw new IOException("域名名称重复");
            }
        }
        return result;
    }

    private static List<AreaData> toAreas(JsonArray array) {
        List<AreaData> result = new ArrayList<>();
        for (JsonElement item : array) {
            result.add(GSON.fromJson(item, AreaData.class));
        }
        return result;
    }

    private static ApprovalRequest copy(ApprovalRequest request) {
        return normalizeRequest(GSON.fromJson(GSON.toJsonTree(request), ApprovalRequest.class));
    }

    // 兼容旧记录省略文档空值的情况，后续冲突检查和删除写入统一使用JsonNull。
    private static ApprovalRequest normalizeRequest(ApprovalRequest request) {
        if (request.changes == null) request.changes = new ArrayList<>();
        if (request.reviews == null) request.reviews = new ArrayList<>();
        if (request.previewAreas == null) request.previewAreas = new ArrayList<>();
        for (AreaData area : request.previewAreas) area.setType(area.getType());
        for (ApprovalRequest.Change change : request.changes) {
            if (change.before == null) change.before = JsonNull.INSTANCE;
            if (change.after == null) change.after = JsonNull.INSTANCE;
            if ("areas".equals(change.kind)) {
                // 旧版审批快照缺失的模式和类型补齐默认值，匹配当前模型的规范化结果。
                normalizeAreaSnapshot(change.before);
                normalizeAreaSnapshot(change.after);
            }
        }
        return request;
    }

    private static void normalizeAreaSnapshot(JsonElement snapshot) {
        if (!snapshot.isJsonArray()) {
            return;
        }
        for (JsonElement element : snapshot.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject area = element.getAsJsonObject();
            if (!area.has("gamemode")) area.add("gamemode", JsonNull.INSTANCE);
            // 历史申请缺失或空白的 type 都补为农村类型，与当前模型规范化后的快照保持一致。
            area.addProperty("type", typeValue(area));
        }
    }

    private static boolean validId(String id) {
        try {
            return id != null && UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String normalizeDimension(String value) {
        if (value == null || value.isBlank()) {
            return "minecraft:overworld";
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "overworld" -> "minecraft:overworld";
            case "nether", "the_nether" -> "minecraft:the_nether";
            case "end", "the_end" -> "minecraft:the_end";
            default -> value;
        };
    }

    public static String string(JsonObject object, String key) throws ApprovalException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw error("invalid");
        }
        return value.getAsString();
    }

    private static int integer(JsonObject object, String key) throws ApprovalException {
        try {
            JsonElement value = object.get(key);
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                throw error("invalid");
            }
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw error("invalid");
        }
    }

    private static ApprovalException error(String suffix) {
        return new ApprovalException("approval.error." + suffix);
    }

    public static final class ApprovalException extends Exception {
        public ApprovalException(String key) {
            super(key);
        }
    }
}
