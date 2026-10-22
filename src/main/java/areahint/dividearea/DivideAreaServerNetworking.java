package areahint.dividearea;

import areahint.data.AreaData;
import areahint.approval.ApprovalManager;
import areahint.file.FileManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import areahint.util.AreaDataConverter;
import areahint.util.AreaPermissionUtil;
import com.google.gson.JsonParser;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.List;

import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

public class DivideAreaServerNetworking {

    public static void registerServerNetworking() {
        NetworkCompat.get().registerServerReceiver(
            Channel.DIVIDE_AREA_CHANNEL,
            (server, player, buf) -> {
                try {
                    String area1Json = buf.readString(32767);
                    String area2Json = buf.readString(32767);
                    String originalName = buf.readString(32767);
                    String dimension = buf.readString(32767);
                    server.execute(() -> handleDivideRequest(player, area1Json, area2Json, originalName, dimension));
                } catch (Exception e) {
                    System.err.println("处理分割域名请求时发生错误: " + e.getMessage());
                    sendResponse(player, false, key("dividearea.error.internal"));
                }
            }
        );
    }

    private static void handleDivideRequest(ServerPlayerEntity player, String a1Json, String a2Json, String origName, String dimension) {
        try {
            if (!AreaManagementCapabilityService.isCurrentDimension(player, dimension)) {
                sendResponse(player, false, key("dividearea.error.dimension"));
                return;
            }

            AreaData area1 = AreaDataConverter.fromJsonObject(JsonParser.parseString(a1Json).getAsJsonObject());
            AreaData area2 = AreaDataConverter.fromJsonObject(JsonParser.parseString(a2Json).getAsJsonObject());
            String dimType = convertDimId(dimension);

            List<AreaData> storedAreas = readAreas(dimType);
            AreaData originalArea = AreaPermissionUtil.findByName(storedAreas, origName);
            if (!AreaManagementCapabilityService.canPerform(
                    player, AreaManagementCapabilityService.DIVIDE_AREA, originalArea, storedAreas)) {
                sendResponse(player, false, key("dividearea.error.permission"));
                return;
            }

            // 两个分割结果继承服务器原域名的模式，客户端不得通过分割创建任意游戏模式的域名。
            area1.setGamemode(originalArea.getGamemode());
            area2.setGamemode(originalArea.getGamemode());
            // 分割涉及删除原域名并新增两个域名，必须作为同一份候选结果审批。
            if (ApprovalManager.requiresApproval(player)) {
                String fileName = Packets.getFileNameForDimension(dimType);
                if (fileName == null) {
                    sendResponse(player, false, key("dividearea.error.dimension"));
                    return;
                }
                Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
                List<AreaData> proposed = FileManager.readAreaData(areaFile);
                proposed.removeIf(area -> area.getName().equals(origName));
                proposed.add(area1);
                proposed.add(area2);
                if (ApprovalManager.submitAreasIfRequired(player, "dividearea", areaFile, proposed)) {
                    sendResponse(player, true, key("approval.message.submitted"));
                    return;
                }
            }

            if (!saveDividedAreas(area1, area2, origName, dimension)) {
                sendResponse(player, false, key("dividearea.error.save"));
                return;
            }
            ServerNetworking.sendAllAreaDataToAll();
            sendResponse(player, true,
                key("dividearea.success.divide_prefix"), lit(origName),
                key("dividearea.success.divide_mid1"), lit(area1.getName()),
                key("dividearea.success.divide_mid2"), lit(area2.getName()),
                key("dividearea.success.divide_suffix"));
        } catch (Exception e) {
            System.err.println("处理分割域名请求失败: " + e.getMessage());
            sendResponse(player, false, key("dividearea.error.process"), lit(e.getMessage()));
        }
    }

    private static boolean saveDividedAreas(AreaData area1, AreaData area2, String origName, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(convertDimId(dimension));
            if (fileName == null) return false;
            Path path = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> existing = FileManager.readAreaData(path);
            existing.removeIf(area -> area.getName().equals(origName));
            existing.add(area1);
            existing.add(area2);
            return FileManager.writeAreaData(path, existing);
        } catch (Exception e) {
            System.err.println("保存分割域名失败: " + e.getMessage());
            return false;
        }
    }

    private static List<AreaData> readAreas(String dimType) {
        try {
            String fileName = Packets.getFileNameForDimension(dimType);
            if (fileName == null) return null;
            Path path = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            return FileManager.readAreaData(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String convertDimId(String dim) {
        if (dim == null) return null;
        if (dim.contains("overworld")) return Packets.DIMENSION_OVERWORLD;
        if (dim.contains("nether")) return Packets.DIMENSION_NETHER;
        if (dim.contains("end")) return Packets.DIMENSION_END;
        return null;
    }

    private static void sendResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            NetworkBuf buf = NetworkCompat.get().newBuffer();
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            NetworkCompat.get().sendToPlayer(player, Channel.DIVIDE_AREA_RESPONSE_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
