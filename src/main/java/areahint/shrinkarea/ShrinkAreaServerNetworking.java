package areahint.shrinkarea;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import areahint.util.AreaDataConverter;
import areahint.util.AreaPermissionUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

/**
 * 收缩域名服务端网络处理
 * 处理客户端发送的收缩域名请求
 */
public class ShrinkAreaServerNetworking {

    /**
     * 注册服务端网络处理器
     */
    public static void registerServerNetworking() {
        // 注册接收客户端收缩域名请求的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            Packets.SHRINK_AREA_CHANNEL,
            (server, player, handler, buf, responseSender) -> {
                try {
                    String areaJsonString = buf.readString(32767);
                    String dimension = buf.readString(32767);  // 接收维度信息
                    server.execute(() -> handleShrinkAreaRequest(player, areaJsonString, dimension));
                } catch (Exception e) {
                    System.err.println("处理收缩域名请求时发生错误: " + e.getMessage());
                    e.printStackTrace();
                    sendResponse(player, false, key("shrinkarea.server.error.internal"));
                }
            }
        );
    }

    /**
     * 处理收缩域名请求
     */
    private static void handleShrinkAreaRequest(ServerPlayerEntity player, String areaJsonString, String dimension) {
        try {
            if (!AreaManagementCapabilityService.isCurrentDimension(player, dimension)) {
                sendResponse(player, false, key("dividearea.error.dimension"));
                return;
            }

            // 解析接收到的域名数据
            JsonObject areaJson = JsonParser.parseString(areaJsonString).getAsJsonObject();
            AreaData shrunkArea = AreaDataConverter.fromJsonObject(areaJson);

            // 使用客户端指定的目标维度读取已存域名，权限判断只基于服务端已存数据
            String dimensionType = convertDimensionIdToType(dimension);
            String fileName = Packets.getFileNameForDimension(dimensionType);
            if (fileName == null) {
                sendResponse(player, false, key("shrinkarea.server.error.save"));
                return;
            }
            java.nio.file.Path areaPath = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> existingAreas = FileManager.readAreaData(areaPath);
            AreaData existingArea = AreaPermissionUtil.findByName(existingAreas, shrunkArea.getName());
            if (existingArea == null) {
                System.err.println("未找到要收缩的域名: " + shrunkArea.getName());
                sendResponse(player, false, key("shrinkarea.server.error.save"));
                return;
            }

            // 验证权限
            if (!validatePermission(player, existingArea, existingAreas)) {
                sendResponse(player, false, key("shrinkarea.server.error.permission"));
                return;
            }

            // 验证域名数据
            if (!validateAreaData(shrunkArea)) {
                sendResponse(player, false, key("shrinkarea.server.error.validation"));
                return;
            }

            // 保存收缩后的域名
            if (!saveShrunkArea(shrunkArea, areaPath, existingAreas)) {
                sendResponse(player, false, key("shrinkarea.server.error.save"));
                return;
            }

            // 重新分发给所有玩家
            redistributeAreasToAllPlayers(player.getServer());

            // 发送成功响应
            sendResponse(player, true, key("shrinkarea.success.area.shrink_2"), lit(shrunkArea.getName()));

            // 服务端日志
            System.out.println("玩家 " + player.getGameProfile().getName() +
                " 成功收缩域名: " + shrunkArea.getName() + " (维度: " + dimension + ")");

        } catch (Exception e) {
            System.err.println("处理收缩域名请求失败: " + e.getMessage());
            e.printStackTrace();
            sendResponse(player, false, key("shrinkarea.server.error.process"));
        }
    }

    /**
     * 基于服务端已存域名验证玩家权限
     * @param player 玩家
     * @param existingArea 服务端已存域名数据
     * @param existingAreas 目标维度的已存域名列表
     */
    private static boolean validatePermission(ServerPlayerEntity player, AreaData existingArea, List<AreaData> existingAreas) {
        return AreaManagementCapabilityService.canPerform(
            player, AreaManagementCapabilityService.SHRINK_AREA, existingArea, existingAreas);
    }

    /**
     * 验证域名数据
     */
    private static boolean validateAreaData(AreaData area) {
        // 检查基本字段
        if (area.getName() == null || area.getName().trim().isEmpty()) {
            return false;
        }

        if (area.getVertices() == null || area.getVertices().size() < 3) {
            return false;
        }

        if (area.getAllSignatures().isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 保存收缩后的域名
     */
    private static boolean saveShrunkArea(AreaData shrunkArea, java.nio.file.Path areaPath, List<AreaData> existingAreas) {
        try {
            // 查找并更新现有域名
            boolean found = false;
            for (int i = 0; i < existingAreas.size(); i++) {
                AreaData existingArea = existingAreas.get(i);
                if (existingArea.getName().equals(shrunkArea.getName())) {
                    // 更新现有域名
                    existingAreas.set(i, shrunkArea);
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.err.println("未找到要收缩的域名: " + shrunkArea.getName());
                return false;
            }

            // 保存更新后的域名列表
            return FileManager.writeAreaData(areaPath, existingAreas);

        } catch (Exception e) {
            System.err.println("保存收缩域名失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将维度ID转换为Packets期望的维度类型
     * 参考EasyAdd的实现
     */
    private static String convertDimensionIdToType(String dimension) {
        if (dimension == null) return null;
        if (dimension.contains("overworld")) {
            return Packets.DIMENSION_OVERWORLD;
        } else if (dimension.contains("nether")) {
            return Packets.DIMENSION_NETHER;
        } else if (dimension.contains("end")) {
            return Packets.DIMENSION_END;
        }
        return null;
    }

    /**
     * 重新分发域名给所有玩家（相当于执行一次reload指令）
     * 向所有玩家发送所有维度的区域数据
     */
    private static void redistributeAreasToAllPlayers(net.minecraft.server.MinecraftServer server) {
        try {
            // 使用ServerNetworking的方法发送所有维度的数据（相当于reload）
            ServerNetworking.sendAllAreaDataToAll();
        } catch (Exception e) {
            System.err.println("重新分发域名失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            ServerPlayNetworking.send(player, Packets.SHRINK_AREA_RESPONSE_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
