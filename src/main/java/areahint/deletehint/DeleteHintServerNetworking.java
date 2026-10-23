package areahint.deletehint;

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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.List;

import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

/**
 * DeleteHint服务端网络处理器
 */
public class DeleteHintServerNetworking {

    public static void registerServerReceivers() {
        NetworkCompat.get().registerServerReceiver(
            Channel.DELETEHINT_AREA_CHANNEL,
            (server, player, buf) -> {
                try {
                    String jsonStr = buf.readString(32767);
                    String dimension = buf.readString(32767);
                    server.execute(() -> handleRequest(player, jsonStr, dimension));
                } catch (Exception e) {
                    sendResponse(player, false, key("addhint.error.general_2"), lit(e.getMessage()));
                }
            }
        );
    }

    private static void handleRequest(ServerPlayerEntity player, String jsonStr, String dimension) {
        try {
            if (!AreaManagementCapabilityService.isCurrentDimension(player, dimension)) {
                sendResponse(player, false, key("addhint.error.dimension"), lit(String.valueOf(dimension)));
                return;
            }

            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
            AreaData updatedArea = AreaDataConverter.fromJsonObject(json);

            if (updatedArea == null || updatedArea.getName() == null) {
                sendResponse(player, false, key("addhint.error.area_2"));
                return;
            }

            if (updatedArea.getVertices() == null || updatedArea.getVertices().size() < 3) {
                sendResponse(player, false, key("deletehint.message.vertex_4"));
                return;
            }

            // 获取维度文件
            String dimType = convertDimensionIdToType(dimension);
            String fileName = Packets.getFileNameForDimension(dimType);
            if (fileName == null) {
                sendResponse(player, false, key("addhint.error.dimension"), lit(dimension));
                return;
            }

            // 读取目标维度中的已存域名，并基于已存数据验证权限
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            AreaData existingArea = AreaPermissionUtil.findByName(areas, updatedArea.getName());

            if (existingArea == null) {
                sendResponse(player, false, key("addhint.message.area_3"), lit(updatedArea.getName()));
                return;
            }

            if (!validatePermission(player, existingArea, areas)) {
                sendResponse(player, false, key("addhint.message.area.modify.permission"));
                return;
            }

            // 顶点编辑不授予游戏模式权限，提交审批和直接保存都使用服务器当前的原值。
            updatedArea.setGamemode(existingArea.getGamemode());
            // 删除顶点后仍保留服务端确认的城市化状态。
            updatedArea.setType(existingArea.getType());

            boolean found = false;
            for (int i = 0; i < areas.size(); i++) {
                if (areas.get(i).getName().equals(updatedArea.getName())) {
                    areas.set(i, updatedArea);
                    found = true;
                    break;
                }
            }

            if (!found) {
                sendResponse(player, false, key("addhint.message.area_3"), lit(updatedArea.getName()));
                return;
            }

            // 删除顶点的候选结果先进入审批队列，避免未审批的修改同步到其他玩家。
            if (ApprovalManager.submitAreasIfRequired(player, "deletehint", areaFile, areas)) {
                sendResponse(player, true, key("approval.message.submitted"));
                return;
            }

            if (!FileManager.writeAreaData(areaFile, areas)) {
                sendResponse(player, false, key("addhint.error.save"));
                return;
            }

            // 重新分发给所有玩家
            ServerNetworking.sendAllAreaDataToAll();
            sendResponse(player, true, key("addhint.message.area_2"), lit(updatedArea.getName()), key("deletehint.success.vertex.delete"));

        } catch (Exception e) {
            sendResponse(player, false, key("addhint.error.general"), lit(e.getMessage()));
        }
    }

    private static boolean validatePermission(ServerPlayerEntity player, AreaData existingArea, List<AreaData> areas) {
        return AreaManagementCapabilityService.canPerform(
            player, AreaManagementCapabilityService.DELETE_HINT, existingArea, areas);
    }

    private static String convertDimensionIdToType(String dim) {
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
            NetworkCompat.get().sendToPlayer(player, Channel.DELETEHINT_AREA_RESPONSE_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
