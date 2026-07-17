package areahint.description;

import areahint.Areashint;
import areahint.command.DimensionalNameCommands;
import areahint.data.AreaData;
import areahint.dimensional.DimensionalNameManager;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.teleport.ServerAreaGeometry;
import areahint.util.AreaPermissionUtil;
import areahint.world.WorldFolderManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 服务端域名描述网络处理。
 */
public final class DescriptionServerNetworking {
    public static final int MAX_DESCRIPTION_LENGTH = 32767;
    private static final String TARGET_AREA = "area";
    private static final String TARGET_DIMENSION = "dimension";
    private static final String OPERATION_ADD = "add";
    private static final String OPERATION_DELETE = "delete";

    private DescriptionServerNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_QUERY,
            (server, player, handler, buf, responseSender) -> {
                String targetType = buf.readString();
                String dimensionType = buf.readString();
                String targetName = buf.readString();
                server.execute(() -> handleQuery(player, targetType, dimensionType, targetName));
            });

        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_CURRENT_QUERY,
            (server, player, handler, buf, responseSender) -> server.execute(() -> handleCurrentAreaQuery(player)));

        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_LIST_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                String operation = buf.readString();
                String targetType = buf.readString();
                String dimensionType = buf.readString();
                String query = buf.readString();
                server.execute(() -> handleListRequest(player, operation, targetType, dimensionType, query));
            });

        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_EDIT_QUERY,
            (server, player, handler, buf, responseSender) -> {
                String targetType = buf.readString();
                String dimensionType = buf.readString();
                String targetName = buf.readString();
                server.execute(() -> handleEditQuery(player, targetType, dimensionType, targetName));
            });

        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_WRITE,
            (server, player, handler, buf, responseSender) -> {
                String targetType = buf.readString();
                String dimensionType = buf.readString();
                String targetName = buf.readString();
                String description = buf.readString(MAX_DESCRIPTION_LENGTH);
                server.execute(() -> handleWrite(player, targetType, dimensionType, targetName, description));
            });

        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DESCRIPTION_DELETE,
            (server, player, handler, buf, responseSender) -> {
                String targetType = buf.readString();
                String dimensionType = buf.readString();
                String targetName = buf.readString();
                server.execute(() -> handleDelete(player, targetType, dimensionType, targetName));
            });
    }

    private static void handleCurrentAreaQuery(ServerPlayerEntity player) {
        try {
            String dimensionType = getPlayerDimensionType(player);
            if (dimensionType == null || dimensionType.isBlank()) {
                sendQueryResponse(player, defaultTitle(player), null);
                return;
            }

            AreaData area = findCurrentArea(readAreas(dimensionType), player.getX(), player.getY(), player.getZ());
            if (area != null) {
                String surfaceName = getSurfaceName(area);
                Path areaFile = DescriptionFileManager.getAreaDescriptionFile(dimensionType, surfaceName);
                DescriptionData areaDescription = DescriptionFileManager.readDescription(areaFile);
                // 玩家位于普通域名内时只查询该域名，描述为空也不再回退到维度域名描述。
                sendQueryResponse(player, surfaceName, areaDescription);
                return;
            }

            String dimensionId = getPlayerDimensionId(player);
            String displayName = DimensionalNameManager.getDimensionalName(dimensionId);
            Path dimensionFile = DescriptionFileManager.getDimensionalDescriptionFile(displayName);
            DescriptionData dimensionDescription = DescriptionFileManager.readDescription(dimensionFile);
            if (hasUsableDescription(dimensionDescription)) {
                sendQueryResponse(player, displayName, dimensionDescription);
                return;
            }

            if (area != null) {
                sendQueryResponse(player, getSurfaceName(area), null);
            } else {
                sendQueryResponse(player, displayName, null);
            }
        } catch (Exception e) {
            Areashint.LOGGER.error("查询当前域名描述失败", e);
            sendQueryResponse(player, defaultTitle(player), null);
        }
    }

    private static void handleQuery(ServerPlayerEntity player, String targetType, String ignoredDimensionType, String targetName) {
        try {
            if (TARGET_DIMENSION.equals(targetType)) {
                String dimensionId = clean(targetName);
                if (dimensionId.isBlank()) {
                    dimensionId = getPlayerDimensionId(player);
                }
                String displayName = DimensionalNameManager.getDimensionalName(dimensionId);
                Path file = DescriptionFileManager.getDimensionalDescriptionFile(displayName);
                DescriptionData data = DescriptionFileManager.readDescription(file);
                sendQueryResponse(player, displayName, data);
                return;
            }

            String dimensionType = getPlayerDimensionType(player);
            if (dimensionType == null) {
                dimensionType = clean(ignoredDimensionType);
            }
            if (dimensionType.isBlank()) {
                sendQueryResponse(player, translate(player, "description.server.query.unsupported_area"), null);
                return;
            }

            AreaContext context = findAreaContext(dimensionType, targetName);
            if (context == null) {
                sendQueryResponse(player, clean(targetName), null);
                return;
            }

            Path file = DescriptionFileManager.getAreaDescriptionFile(dimensionType, context.surfaceName());
            DescriptionData data = DescriptionFileManager.readDescription(file);
            sendQueryResponse(player, context.surfaceName(), data);
        } catch (Exception e) {
            Areashint.LOGGER.error("查询域名描述失败", e);
            sendQueryResponse(player, clean(targetName), null);
        }
    }

    private static void handleEditQuery(ServerPlayerEntity player, String targetType, String ignoredDimensionType, String targetName) {
        try {
            if (TARGET_DIMENSION.equals(targetType)) {
                sendDimensionalEditDescription(player, targetName);
            } else {
                sendAreaEditDescription(player, ignoredDimensionType, targetName);
            }
        } catch (Exception e) {
            Areashint.LOGGER.error("读取待编辑域名描述失败", e);
            sendEditResponse(player, clean(targetType), clean(ignoredDimensionType), clean(targetName), false,
                translate(player, "description.server.error.area_not_found", clean(targetName)), clean(targetName), "");
        }
    }

    private static void sendAreaEditDescription(ServerPlayerEntity player, String ignoredDimensionType, String areaName) {
        String dimensionType = getPlayerDimensionType(player);
        if (dimensionType == null) {
            dimensionType = clean(ignoredDimensionType);
        }
        if (dimensionType.isBlank()) {
            sendEditResponse(player, TARGET_AREA, dimensionType, clean(areaName), false,
                translate(player, "description.server.error.unsupported_area"), clean(areaName), "");
            return;
        }

        AreaContext context = findAreaContext(dimensionType, areaName);
        if (context == null) {
            sendEditResponse(player, TARGET_AREA, dimensionType, clean(areaName), false,
                translate(player, "description.server.error.area_not_found", clean(areaName)), clean(areaName), "");
            return;
        }
        if (!AreaManagementCapabilityService.canPerform(player,
            AreaManagementCapabilityService.REPLACE_DESCRIPTION, context.area(), context.allAreas())) {
            sendEditResponse(player, TARGET_AREA, dimensionType, context.area().getName(), false,
                translate(player, "description.server.error.no_modify_permission"), context.surfaceName(), "");
            return;
        }

        Path file = DescriptionFileManager.getAreaDescriptionFile(dimensionType, context.surfaceName());
        if (hasSurfaceNameConflict(file, context.surfaceName())) {
            sendEditResponse(player, TARGET_AREA, dimensionType, context.area().getName(), false,
                translate(player, "description.server.error.area_filename_conflict"), context.surfaceName(), "");
            return;
        }

        DescriptionData data = DescriptionFileManager.readDescription(file);
        sendEditResponse(player, TARGET_AREA, dimensionType, context.area().getName(), true, "",
            context.surfaceName(), hasUsableDescription(data) ? data.getDescription() : "");
    }

    private static void sendDimensionalEditDescription(ServerPlayerEntity player, String dimensionId) {
        if (!PermissionService.hasCommandPermission(player, PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION, 2)) {
            sendEditResponse(player, TARGET_DIMENSION, "", clean(dimensionId), false,
                translate(player, "description.server.error.no_dimension_modify_permission"), clean(dimensionId), "");
            return;
        }

        String cleanDimensionId = clean(dimensionId);
        String displayName = DimensionalNameManager.getDimensionalName(cleanDimensionId);
        Path file = DescriptionFileManager.getDimensionalDescriptionFile(displayName);
        if (hasSurfaceNameConflict(file, displayName)) {
            sendEditResponse(player, TARGET_DIMENSION, "", cleanDimensionId, false,
                translate(player, "description.server.error.dimension_filename_conflict"), displayName, "");
            return;
        }

        DescriptionData data = DescriptionFileManager.readDescription(file);
        sendEditResponse(player, TARGET_DIMENSION, "", cleanDimensionId, true, "",
            displayName, hasUsableDescription(data) ? data.getDescription() : "");
    }

    private static void handleListRequest(ServerPlayerEntity player, String operation, String targetType, String ignoredDimensionType, String query) {
        if (TARGET_DIMENSION.equals(targetType)) {
            sendDimensionalList(player, operation, query);
        } else {
            sendAreaList(player, operation, query);
        }
    }

    private static void sendAreaList(ServerPlayerEntity player, String operation, String query) {
        String dimensionType = getPlayerDimensionType(player);
        List<ListEntry> entries = new ArrayList<>();
        if (dimensionType != null) {
            List<AreaData> areas = readAreas(dimensionType);
            String normalizedQuery = normalizeQuery(query);
            String capability = OPERATION_DELETE.equals(operation)
                ? AreaManagementCapabilityService.DELETE_DESCRIPTION
                : AreaManagementCapabilityService.ADD_DESCRIPTION;

            for (AreaData area : areas) {
                if (area == null || area.getName() == null) {
                    continue;
                }
                String surfaceName = getSurfaceName(area);
                if (!matches(normalizedQuery, area.getName(), surfaceName, area.getBaseName())) {
                    continue;
                }
                if (AreaManagementCapabilityService.canPerform(player, capability, area, areas)) {
                    entries.add(new ListEntry(area.getName(), surfaceName, area.getLevel(), area.getBaseName(), area.getSignature(), dimensionType));
                }
            }
            entries.sort(Comparator.comparing(ListEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        }
        sendListResponse(player, operation, TARGET_AREA, dimensionType, entries);
    }

    private static void sendDimensionalList(ServerPlayerEntity player, String operation, String query) {
        String permissionNode = OPERATION_DELETE.equals(operation)
            ? PermissionNodes.DELETE_DIMENSIONALITY_DESCRIPTION
            : PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION;
        if (!PermissionService.hasCommandPermission(player, permissionNode, 2)) {
            sendListResponse(player, operation, TARGET_DIMENSION, "", List.of());
            return;
        }

        DimensionalNameCommands.syncServerDimensions(player.getServer());
        String normalizedQuery = normalizeQuery(query);
        List<ListEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : DimensionalNameManager.getAllDimensionalNames().entrySet()) {
            String dimensionId = entry.getKey();
            String displayName = entry.getValue();
            if (matches(normalizedQuery, dimensionId, displayName)) {
                entries.add(new ListEntry(dimensionId, displayName, 0, null, null, dimensionId));
            }
        }
        entries.sort(Comparator.comparing(ListEntry::displayName, String.CASE_INSENSITIVE_ORDER));
        sendListResponse(player, operation, TARGET_DIMENSION, "", entries);
    }

    private static void handleWrite(ServerPlayerEntity player, String targetType, String ignoredDimensionType, String targetName, String description) {
        if (description == null || description.trim().isEmpty()) {
            sendMutationResponse(player, false, translate(player, "description.server.error.empty"));
            return;
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            sendMutationResponse(player, false, translate(player, "description.server.error.too_long"));
            return;
        }

        if (TARGET_DIMENSION.equals(targetType)) {
            writeDimensionalDescription(player, targetName, description);
        } else {
            writeAreaDescription(player, targetName, description);
        }
    }

    private static void writeAreaDescription(ServerPlayerEntity player, String areaName, String description) {
        String dimensionType = getPlayerDimensionType(player);
        if (dimensionType == null) {
            sendMutationResponse(player, false, translate(player, "description.server.error.unsupported_area"));
            return;
        }

        AreaContext context = findAreaContext(dimensionType, areaName);
        if (context == null) {
            sendMutationResponse(player, false, translate(player, "description.server.error.area_not_found", clean(areaName)));
            return;
        }
        if (!AreaManagementCapabilityService.canPerform(player,
            AreaManagementCapabilityService.ADD_DESCRIPTION, context.area(), context.allAreas())) {
            sendMutationResponse(player, false, translate(player, "description.server.error.no_modify_permission"));
            return;
        }

        Path file = DescriptionFileManager.getAreaDescriptionFile(dimensionType, context.surfaceName());
        if (hasSurfaceNameConflict(file, context.surfaceName())) {
            sendMutationResponse(player, false, translate(player, "description.server.error.area_filename_conflict"));
            return;
        }

        DescriptionData data = createDescriptionData(TARGET_AREA, context.area().getName(), context.surfaceName(), dimensionType, description, player);
        sendMutationResponse(player, DescriptionFileManager.writeDescription(file, data),
            translate(player, "description.server.success.area_saved", context.surfaceName()));
    }

    private static void writeDimensionalDescription(ServerPlayerEntity player, String dimensionId, String description) {
        if (!PermissionService.hasCommandPermission(player, PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION, 2)) {
            sendMutationResponse(player, false, translate(player, "description.server.error.no_dimension_modify_permission"));
            return;
        }

        String cleanDimensionId = clean(dimensionId);
        String displayName = DimensionalNameManager.getDimensionalName(cleanDimensionId);
        Path file = DescriptionFileManager.getDimensionalDescriptionFile(displayName);
        if (hasSurfaceNameConflict(file, displayName)) {
            sendMutationResponse(player, false, translate(player, "description.server.error.dimension_filename_conflict"));
            return;
        }

        DescriptionData data = createDescriptionData(TARGET_DIMENSION, cleanDimensionId, displayName, cleanDimensionId, description, player);
        sendMutationResponse(player, DescriptionFileManager.writeDescription(file, data),
            translate(player, "description.server.success.dimension_saved", displayName));
    }

    private static void handleDelete(ServerPlayerEntity player, String targetType, String ignoredDimensionType, String targetName) {
        if (TARGET_DIMENSION.equals(targetType)) {
            deleteDimensionalDescription(player, targetName);
        } else {
            deleteAreaDescription(player, targetName);
        }
    }

    private static void deleteAreaDescription(ServerPlayerEntity player, String areaName) {
        String dimensionType = getPlayerDimensionType(player);
        if (dimensionType == null) {
            sendMutationResponse(player, false, translate(player, "description.server.error.unsupported_area"));
            return;
        }

        AreaContext context = findAreaContext(dimensionType, areaName);
        if (context == null) {
            sendMutationResponse(player, false, translate(player, "description.server.error.area_not_found", clean(areaName)));
            return;
        }
        if (!AreaManagementCapabilityService.canPerform(player,
            AreaManagementCapabilityService.DELETE_DESCRIPTION, context.area(), context.allAreas())) {
            sendMutationResponse(player, false, translate(player, "description.server.error.no_delete_permission"));
            return;
        }

        Path file = DescriptionFileManager.getAreaDescriptionFile(dimensionType, context.surfaceName());
        boolean existed = Files.exists(file);
        boolean success = DescriptionFileManager.deleteDescription(file);
        sendMutationResponse(player, success, existed
            ? translate(player, "description.server.success.area_deleted", context.surfaceName())
            : translate(player, "description.server.message.no_description_or_deleted"));
    }

    private static void deleteDimensionalDescription(ServerPlayerEntity player, String dimensionId) {
        if (!PermissionService.hasCommandPermission(player, PermissionNodes.DELETE_DIMENSIONALITY_DESCRIPTION, 2)) {
            sendMutationResponse(player, false, translate(player, "description.server.error.no_dimension_delete_permission"));
            return;
        }

        String cleanDimensionId = clean(dimensionId);
        String displayName = DimensionalNameManager.getDimensionalName(cleanDimensionId);
        Path file = DescriptionFileManager.getDimensionalDescriptionFile(displayName);
        boolean existed = Files.exists(file);
        boolean success = DescriptionFileManager.deleteDescription(file);
        sendMutationResponse(player, success, existed
            ? translate(player, "description.server.success.dimension_deleted", displayName)
            : translate(player, "description.server.message.no_description_or_deleted"));
    }

    private static DescriptionData createDescriptionData(String targetType, String targetName, String surfaceName,
                                                         String dimension, String description, ServerPlayerEntity player) {
        DescriptionData data = new DescriptionData();
        data.setSchemaVersion(1);
        data.setTargetType(targetType);
        data.setTargetName(targetName);
        data.setSurfaceName(surfaceName);
        data.setDimension(dimension);
        data.setDescription(description);
        data.setAuthor(player.getGameProfile().getName());
        data.setUpdatedAt(System.currentTimeMillis());
        return data;
    }

    private static AreaData findCurrentArea(List<AreaData> areas, double x, double y, double z) {
        if (areas == null || areas.isEmpty()) {
            return null;
        }

        Map<Integer, List<AreaData>> areasByLevel = new HashMap<>();
        for (AreaData area : areas) {
            if (area != null && ServerAreaGeometry.isWithinAltitude(area, y)) {
                areasByLevel.computeIfAbsent(area.getLevel(), key -> new ArrayList<>()).add(area);
            }
        }
        if (areasByLevel.isEmpty()) {
            return null;
        }

        List<Integer> levels = new ArrayList<>(areasByLevel.keySet());
        Collections.sort(levels);

        AreaData currentArea = null;
        List<AreaData> levelOneAreas = sortAreasByDistance(areasByLevel.getOrDefault(1, Collections.emptyList()), x, z);
        for (AreaData area : levelOneAreas) {
            if (ServerAreaGeometry.contains(area, x, y, z)) {
                currentArea = area;
                break;
            }
        }
        if (currentArea == null) {
            return null;
        }

        String baseName = currentArea.getName();
        for (Integer level : levels) {
            if (level <= currentArea.getLevel()) {
                continue;
            }

            List<AreaData> childAreas = new ArrayList<>();
            for (AreaData area : areasByLevel.getOrDefault(level, Collections.emptyList())) {
                if (baseName != null && baseName.equals(area.getBaseName())) {
                    childAreas.add(area);
                }
            }

            boolean foundChild = false;
            for (AreaData area : sortAreasByDistance(childAreas, x, z)) {
                if (ServerAreaGeometry.contains(area, x, y, z)) {
                    currentArea = area;
                    baseName = area.getName();
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                break;
            }
        }

        return currentArea;
    }

    private static List<AreaData> sortAreasByDistance(List<AreaData> areas, double x, double z) {
        if (areas == null || areas.isEmpty()) {
            return Collections.emptyList();
        }
        List<AreaData> result = new ArrayList<>(areas);
        result.sort(Comparator.comparingDouble(area -> distanceToArea(area, x, z)));
        return result;
    }

    private static double distanceToArea(AreaData area, double x, double z) {
        List<AreaData.Vertex> vertices = area == null ? null : area.getVertices();
        if (vertices == null || vertices.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double centerX = 0;
        double centerZ = 0;
        for (AreaData.Vertex vertex : vertices) {
            centerX += vertex.getX();
            centerZ += vertex.getZ();
        }
        centerX /= vertices.size();
        centerZ /= vertices.size();

        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz;
    }

    private static AreaContext findAreaContext(String dimensionType, String areaName) {
        List<AreaData> areas = readAreas(dimensionType);
        String cleanAreaName = stripQuotes(areaName);
        AreaData area = AreaPermissionUtil.findByName(areas, cleanAreaName);
        if (area == null) {
            for (AreaData candidate : areas) {
                if (candidate != null && getSurfaceName(candidate).equals(cleanAreaName)) {
                    area = candidate;
                    break;
                }
            }
        }
        return area == null ? null : new AreaContext(area, areas, getSurfaceName(area));
    }

    private static List<AreaData> readAreas(String dimensionType) {
        String fileName = Packets.getFileNameForDimension(dimensionType);
        if (fileName == null) {
            return new ArrayList<>();
        }
        return FileManager.readAreaData(WorldFolderManager.getWorldDimensionFile(fileName));
    }

    private static String getPlayerDimensionType(ServerPlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return null;
        }
        return Packets.convertDimensionPathToType(player.getWorld().getRegistryKey().getValue().getPath());
    }

    private static String getPlayerDimensionId(ServerPlayerEntity player) {
        if (player == null || player.getWorld() == null) {
            return "";
        }
        return player.getWorld().getRegistryKey().getValue().toString();
    }

    private static String getSurfaceName(AreaData area) {
        String surfaceName = area.getSurfacename();
        return surfaceName == null || surfaceName.trim().isEmpty() ? area.getName() : surfaceName;
    }

    private static boolean hasSurfaceNameConflict(Path file, String surfaceName) {
        if (!Files.exists(file)) {
            return false;
        }
        DescriptionData existing = DescriptionFileManager.readDescription(file);
        return existing != null
            && existing.getSurfaceName() != null
            && !existing.getSurfaceName().equals(surfaceName);
    }

    private static boolean hasUsableDescription(DescriptionData data) {
        return data != null
            && data.getDescription() != null
            && !data.getDescription().trim().isEmpty();
    }

    private static void sendQueryResponse(ServerPlayerEntity player, String title, DescriptionData data) {
        String description = data == null || data.getDescription() == null || data.getDescription().trim().isEmpty()
            ? translate(player, "description.book.no_description")
            : clamp(data.getDescription());
        String author = data == null || data.getAuthor() == null || data.getAuthor().trim().isEmpty()
            ? "Areas Hint"
            : data.getAuthor();

        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(clamp(title == null || title.isBlank() ? defaultTitle(player) : title));
        buffer.writeString(clamp(author));
        buffer.writeString(description);
        ServerPlayNetworking.send(player, Packets.S2C_DESCRIPTION_QUERY_RESPONSE, buffer);
    }

    private static void sendEditResponse(ServerPlayerEntity player, String targetType, String dimensionType, String targetName,
                                         boolean success, String message, String title, String description) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(clean(targetType));
        buffer.writeString(clean(dimensionType));
        buffer.writeString(clean(targetName));
        buffer.writeBoolean(success);
        buffer.writeString(clamp(message));
        buffer.writeString(clamp(title));
        buffer.writeString(clamp(description));
        ServerPlayNetworking.send(player, Packets.S2C_DESCRIPTION_EDIT_RESPONSE, buffer);
    }

    private static String defaultTitle(ServerPlayerEntity player) {
        return translate(player, "description.book.default.title");
    }

    private static String translate(ServerPlayerEntity player, String key, Object... args) {
        return ServerI18nManager.translateForPlayer(player.getUuid(), key, args);
    }

    private static void sendListResponse(ServerPlayerEntity player, String operation, String targetType, String dimensionType, List<ListEntry> entries) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeString(clean(operation));
        buffer.writeString(clean(targetType));
        buffer.writeString(clean(dimensionType));
        buffer.writeInt(entries.size());
        for (ListEntry entry : entries) {
            buffer.writeString(clean(entry.id()));
            buffer.writeString(clean(entry.displayName()));
            buffer.writeInt(entry.level());
            buffer.writeString(clean(entry.baseName()));
            buffer.writeString(clean(entry.signature()));
            buffer.writeString(clean(entry.dimension()));
        }
        ServerPlayNetworking.send(player, Packets.S2C_DESCRIPTION_AREA_LIST, buffer);
    }

    private static void sendMutationResponse(ServerPlayerEntity player, boolean success, String message) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(success);
        buffer.writeString(clamp(message));
        ServerPlayNetworking.send(player, Packets.S2C_DESCRIPTION_MUTATION_RESPONSE, buffer);
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String normalizedQuery, String... values) {
        if (normalizedQuery == null || normalizedQuery.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }

    private static String clamp(String value) {
        String cleanValue = clean(value);
        return cleanValue.length() > MAX_DESCRIPTION_LENGTH ? cleanValue.substring(0, MAX_DESCRIPTION_LENGTH) : cleanValue;
    }

    private static String stripQuotes(String value) {
        String cleaned = clean(value).trim();
        if (cleaned.length() >= 2 && cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        }
        return cleaned;
    }

    private record AreaContext(AreaData area, List<AreaData> allAreas, String surfaceName) {
    }

    private record ListEntry(String id, String displayName, int level, String baseName, String signature, String dimension) {
    }
}
