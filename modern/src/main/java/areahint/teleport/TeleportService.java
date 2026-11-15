package areahint.teleport;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import areahint.network.Packets;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class TeleportService {
    private static final SafeLandingFinder SAFE_LANDING_FINDER = new SafeLandingFinder();

    private TeleportService() {
    }

    public static boolean isValidTeleportFormat(String teleportFormat) {
        if (teleportFormat == null) {
            return false;
        }
        String normalized = teleportFormat.trim().toLowerCase(Locale.ROOT);
        return "tp".equals(normalized)
                || "minecraft:tp".equals(normalized)
                || "teleport".equals(normalized)
                || "minecraft:teleport".equals(normalized);
    }

    public static void handleTeleportRequest(ServerPlayer player, String mode, String areaName, String teleportFormat) {
        if (player == null) {
            return;
        }

        if (!PermissionService.hasCommandPermission(player, PermissionNodes.TELEPORT, 0)) {
            sendResponse(player, false, translate(player, "teleport.server.error.no_permission"));
            return;
        }

        String normalizedMode = normalizeMode(mode);
        if (normalizedMode == null) {
            sendResponse(player, false, translate(player, "teleport.server.error.unknown_mode", mode));
            return;
        }

        if (areaName == null || areaName.trim().isEmpty()) {
            sendResponse(player, false, translate(player, "teleport.server.error.empty_name"));
            return;
        }

        if (!isValidTeleportFormat(teleportFormat)) {
            sendResponse(player, false, translate(player, "teleport.server.error.invalid_format"));
            return;
        }

        String dimensionType = getPlayerDimensionType(player);
        String fileName = Packets.getFileNameForDimension(dimensionType);
        if (fileName == null) {
            sendResponse(player, false, translate(player, "teleport.server.error.unsupported_dimension"));
            return;
        }

        Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
        List<AreaData> areas = FileManager.readAreaData(areaFile);
        AreaData area = findArea(areas, areaName.trim());
        if (area == null) {
            sendResponse(player, false, translate(player, "teleport.server.error.area_not_found", areaName));
            return;
        }
        if (!AreaManagementCapabilityService.canPerform(player, normalizedMode, area, areas)) {
            sendResponse(player, false, translate(player, "teleport.server.error.no_permission"));
            return;
        }

        // 服务端玩家的世界始终是 ServerWorld，统一入口兼容 1.21.6 移除 getServerWorld 的变更。
        ServerLevel world = areahint.util.ServerPlayerCompat.world(player);
        Optional<Vec3> landing = "tcp".equals(normalizedMode)
                ? SAFE_LANDING_FINDER.findCenterLanding(world, area)
                : SAFE_LANDING_FINDER.findRandomLanding(world, area);

        if (landing.isEmpty()) {
            sendResponse(player, false, translate(player, "teleport.server.error.no_landing", area.getName()));
            return;
        }

        Vec3 position = landing.get();
        if (executeTeleport(player, position)) {
            sendResponse(player, true, translate(player, "teleport.server.success.teleported",
                area.getName(), formatCoordinate(position.x), formatCoordinate(position.y), formatCoordinate(position.z)));
        } else {
            sendResponse(player, false, translate(player, "teleport.server.error.execute_failed"));
        }
    }

    private static boolean executeTeleport(ServerPlayer player, Vec3 position) {
        return executeTeleport(player, areahint.util.ServerPlayerCompat.world(player), position);
    }

    // 审批预览仍使用 TCP 的中心安全落点算法，只把查找目标换成服务端保存的临时域名。
    public static boolean handleApprovalTeleport(ServerPlayer player, String id, int index)
            throws areahint.approval.ApprovalManager.ApprovalException {
        if (!PermissionService.hasCommandPermission(player, PermissionNodes.TELEPORT, 0)) {
            throw new areahint.approval.ApprovalManager.ApprovalException("approval.error.denied");
        }
        var request = areahint.approval.ApprovalManager.findVisible(player, id);
        AreaData area = areahint.approval.ApprovalManager.previewArea(player, id, index, false);
        Identifier dimension = Identifier.tryParse(request.dimensionId);
        ServerLevel world = dimension == null ? null : areahint.util.ServerPlayerCompat.server(player).getLevel(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension));
        if (world == null) {
            throw new areahint.approval.ApprovalManager.ApprovalException("approval.error.preview_unavailable");
        }
        Optional<Vec3> landing = SAFE_LANDING_FINDER.findCenterLanding(world, area);
        if (landing.isEmpty()) {
            sendResponse(player, false, translate(player, "teleport.server.error.no_landing", area.getName()));
            return false;
        }
        Vec3 position = landing.get();
        boolean success = executeTeleport(player, world, position);
        sendResponse(player, success, success
            ? translate(player, "teleport.server.success.teleported", area.getName(), formatCoordinate(position.x),
                formatCoordinate(position.y), formatCoordinate(position.z))
            : translate(player, "teleport.server.error.execute_failed"));
        return success;
    }

    private static boolean executeTeleport(ServerPlayer player, ServerLevel world, Vec3 position) {
        try {
            // 1.21.2 起 ServerPlayerEntity.teleport 多了 Set<PositionFlag> 与末尾布尔参数
            /*player.teleport(world, position.x, position.y, position.z, player.getYaw(), player.getPitch());
            */
            player.teleportTo(world, position.x, position.y, position.z, java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            return areahint.util.ServerPlayerCompat.world(player) == world && player.distanceToSqr(position) < 1.0D;
        } catch (Exception e) {
            Areashint.LOGGER.error("执行域名传送时发生错误", e);
            return false;
        }
    }

    private static AreaData findArea(List<AreaData> areas, String areaName) {
        if (areas == null) {
            return null;
        }
        // 单次遍历：先比对域名，命中后才做较昂贵的 isValid 校验（内含颜色正则），返回结果与原来一致
        for (AreaData area : areas) {
            if (area != null && areaName.equals(area.getName()) && area.isValid()) {
                return area;
            }
        }
        return null;
    }

    private static String normalizeMode(String mode) {
        if ("tcp".equalsIgnoreCase(mode)) {
            return "tcp";
        }
        if ("udp".equalsIgnoreCase(mode)) {
            return "udp";
        }
        return null;
    }

    private static String getPlayerDimensionType(ServerPlayer player) {
        Identifier dimension = areahint.util.ServerPlayerCompat.world(player).dimension().identifier();
        return Packets.convertDimensionPathToType(dimension.getPath());
    }

    public static void writeResponse(NetworkBuf buf, boolean success, String message) {
        buf.writeBoolean(success);
        buf.writeString(message == null ? "" : message);
    }

    private static String translate(ServerPlayer player, String key, Object... args) {
        return ServerI18nManager.translateForPlayer(player.getUUID(), key, args);
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static void sendResponse(ServerPlayer player, boolean success, String message) {
        try {
            NetworkBuf buf = NetworkCompat.get().newBuffer();
            writeResponse(buf, success, message);
            NetworkCompat.get().sendToPlayer(player, Channel.S2C_TELEPORT_RESPONSE, buf);
        } catch (Exception e) {
            Areashint.LOGGER.error("发送传送响应时发生错误", e);
            player.sendSystemMessage(Component.literal((success ? "§a" : "§c") + message));
        }
    }
}
