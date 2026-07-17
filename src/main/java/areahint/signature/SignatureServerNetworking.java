package areahint.signature;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import areahint.util.AreaPermissionUtil;
import areahint.world.WorldFolderManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

/**
 * Signature服务端网络处理器。
 *
 * <p>客户端只提交“操作、域名、维度、目标玩家名”，服务端重新读取世界文件中的完整域名数据，
 * 再执行权限检查和写入。这样可以避免客户端伪造完整域名JSON修改其他字段。</p>
 */
public class SignatureServerNetworking {

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(
            Packets.SIGNATURE_AREA_CHANNEL,
            (server, player, handler, buf, responseSender) -> {
                try {
                    String operation = buf.readString(32767);
                    String areaName = buf.readString(32767);
                    String dimension = buf.readString(32767);
                    String targetPlayerName = buf.readString(32767);
                    server.execute(() -> handleRequest(player, operation, areaName, dimension, targetPlayerName));
                } catch (Exception e) {
                    sendResponse(player, false, key("signature.server.error.parse"), lit(e.getMessage()));
                }
            }
        );
    }

    /**
     * 处理添加/删除扩展签名请求。
     */
    private static void handleRequest(ServerPlayerEntity player, String operation, String areaName,
                                      String dimension, String targetPlayerName) {
        try {
            String normalizedOperation = cleanText(operation);
            if (!"add".equals(normalizedOperation) && !"delete".equals(normalizedOperation)) {
                sendResponse(player, false, key("signature.server.error.unknown_operation"), lit(String.valueOf(operation)));
                return;
            }

            String cleanedAreaName = cleanText(areaName);
            if (cleanedAreaName == null) {
                sendResponse(player, false, key("signature.server.error.empty_area"));
                return;
            }

            String target = cleanText(targetPlayerName);
            if (target == null) {
                sendResponse(player, false, key("signature.server.error.empty_player"));
                return;
            }

            if (!AreaManagementCapabilityService.isCurrentDimension(player, dimension)) {
                sendResponse(player, false, key("signature.server.error.unknown_dimension"), lit(String.valueOf(dimension)));
                return;
            }

            String dimType = convertDimensionIdToType(dimension);
            String fileName = Packets.getFileNameForDimension(dimType);
            if (fileName == null) {
                sendResponse(player, false, key("signature.server.error.unknown_dimension"), lit(String.valueOf(dimension)));
                return;
            }

            Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            AreaData storedArea = AreaPermissionUtil.findByName(areas, cleanedAreaName);
            if (storedArea == null) {
                sendResponse(player, false, key("signature.server.error.area_not_found"), lit(cleanedAreaName));
                return;
            }

            String capability = "add".equals(normalizedOperation)
                ? AreaManagementCapabilityService.ADD_SIGNATURE
                : AreaManagementCapabilityService.DELETE_SIGNATURE;
            if (!AreaManagementCapabilityService.canPerform(player, capability, storedArea, areas)) {
                sendResponse(player, false, key("signature.server.error.no_permission"));
                return;
            }

            if ("add".equals(normalizedOperation)) {
                if (storedArea.hasSignature(target)) {
                    sendResponse(player, false, key("signature.server.error.exists"), lit(target));
                    return;
                }
                storedArea.addSignature(target);
            } else {
                if (!hasExtensionSignature(storedArea.getSignatures(), target)) {
                    sendResponse(player, false, key("signature.server.error.not_exists"), lit(target));
                    return;
                }
                storedArea.removeSignature(target);
            }

            if (!FileManager.writeAreaData(areaFile, areas)) {
                sendResponse(player, false, key("signature.server.error.save"));
                return;
            }

            ServerNetworking.sendAllAreaDataToAll();
            sendResponse(player, true,
                key("add".equals(normalizedOperation) ? "signature.server.success.add" : "signature.server.success.delete"),
                lit(target));
        } catch (Exception e) {
            sendResponse(player, false, key("signature.server.error.process"), lit(e.getMessage()));
        }
    }

    private static String convertDimensionIdToType(String dimension) {
        if (dimension == null) {
            return null;
        }

        String normalizedDimension = dimension.trim().toLowerCase(Locale.ROOT);
        if (normalizedDimension.isEmpty()) {
            return null;
        }

        // 客户端可能传完整ID（minecraft:the_end）或内部路径（the_end），服务端统一取 path 精确匹配。
        int colonIndex = normalizedDimension.lastIndexOf(':');
        String dimensionPath = colonIndex >= 0 ? normalizedDimension.substring(colonIndex + 1) : normalizedDimension;
        return Packets.convertDimensionPathToType(dimensionPath);
    }

    private static boolean hasExtensionSignature(List<String> signatures, String target) {
        String cleanedTarget = cleanText(target);
        if (cleanedTarget == null || signatures == null) {
            return false;
        }
        for (String signature : signatures) {
            if (cleanedTarget.equals(cleanText(signature))) {
                return true;
            }
        }
        return false;
    }

    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static void sendResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            ServerPlayNetworking.send(player, Packets.SIGNATURE_AREA_RESPONSE_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
