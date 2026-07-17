package areahint.management;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.network.Packets;
import areahint.util.AreaPermissionUtil;
import areahint.world.WorldFolderManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.List;

/**
 * 世界地图域名管理能力查询协议。
 */
public final class AreaManagementServerNetworking {
    private AreaManagementServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_AREA_MANAGEMENT_CAPABILITIES,
            (server, player, handler, buf, responseSender) -> {
                long requestId = buf.readLong();
                String dimensionId = buf.readString();
                String areaName = buf.readString();
                server.execute(() -> handleRequest(player, requestId, dimensionId, areaName));
            });
    }

    private static void handleRequest(ServerPlayerEntity player, long requestId, String dimensionId, String areaName) {
        String playerDimension = player.getWorld().getRegistryKey().getValue().toString();
        if (!playerDimension.equals(dimensionId)) {
            sendResponse(player, requestId, dimensionId, areaName, false, List.of());
            return;
        }

        String dimensionType = Packets.convertDimensionPathToType(player.getWorld().getRegistryKey().getValue().getPath());
        String fileName = Packets.getFileNameForDimension(dimensionType);
        if (fileName == null) {
            sendResponse(player, requestId, dimensionId, areaName, false, List.of());
            return;
        }

        try {
            Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            AreaData area = AreaPermissionUtil.findByName(areas, areaName);
            if (area == null) {
                sendResponse(player, requestId, dimensionId, areaName, false, List.of());
                return;
            }
            List<String> allowed = AreaManagementCapabilityService.getAllowedOperations(player, area, areas);
            sendResponse(player, requestId, dimensionId, areaName, true, allowed);
        } catch (Exception e) {
            Areashint.LOGGER.warn("处理域名管理能力查询失败: {} / {}", dimensionId, areaName, e);
            sendResponse(player, requestId, dimensionId, areaName, false, List.of());
        }
    }

    private static void sendResponse(ServerPlayerEntity player, long requestId, String dimensionId,
                                     String areaName, boolean valid, List<String> operations) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(requestId);
        buf.writeString(dimensionId == null ? "" : dimensionId);
        buf.writeString(areaName == null ? "" : areaName);
        buf.writeBoolean(valid);
        buf.writeInt(operations.size());
        for (String operation : operations) {
            buf.writeString(operation);
        }
        ServerPlayNetworking.send(player, Packets.S2C_AREA_MANAGEMENT_CAPABILITIES, buf);
    }
}
