package areahint.dividearea;

import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import areahint.util.AreaDataConverter;
import areahint.util.AreaPermissionUtil;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.List;

import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

public class DivideAreaServerNetworking {

    public static void registerServerNetworking() {
        ServerPlayNetworking.registerGlobalReceiver(
            Packets.DIVIDE_AREA_CHANNEL,
            (server, player, handler, buf, responseSender) -> {
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

            if (!validatePermission(player, origName, dimType)) {
                sendResponse(player, false, key("dividearea.error.permission"));
                return;
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

    private static boolean validatePermission(ServerPlayerEntity player, String areaName, String dimType) {
        List<AreaData> areas = readAreas(dimType);
        AreaData area = AreaPermissionUtil.findByName(areas, areaName);
        return AreaManagementCapabilityService.canPerform(
            player, AreaManagementCapabilityService.DIVIDE_AREA, area, areas);
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
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            ServerPlayNetworking.send(player, Packets.DIVIDE_AREA_RESPONSE_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
