package areahint.command;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.network.TranslatableMessage.Part;
import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;
import areahint.util.AreaPermissionUtil;
import areahint.util.ColorUtil;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.nio.file.Path;
import areahint.i18n.ServerI18nManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 域名重新着色指令处理器
 */
public class RecolorCommand {
    
    /**
     * 执行recolor指令
     * @param context 指令上下文
     * @return 执行结果
     */
    public static int executeRecolor(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.translatable("command.error.general_10"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        String playerName = player.getName().getString();

        // 获取玩家当前维度
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        String dimensionType = convertDimensionIdToType(dimension);
        String fileName = Packets.getFileNameForDimension(dimensionType);

        if (fileName == null) {
            source.sendMessage(Text.translatable("command.error.dimension_3"));
            return 0;
        }

        // 获取可编辑的域名列表
        List<AreaData> editableAreas = getEditableAreas(player, fileName, playerName);

        if (editableAreas.isEmpty()) {
            source.sendMessage(Text.translatable("command.error.area.dimension_3"));
            return 0;
        }

        // 发送域名列表到客户端，包含交互式颜色选择界面
        sendInteractiveRecolorToClient(player, editableAreas, dimensionType);

        source.sendMessage(Text.translatable("command.prompt.area.list"));
        return 1;
    }
    
    /**
     * 执行带参数的recolor指令（直接修改颜色）
     * @param context 指令上下文
     * @param areaName 域名名称
     * @param colorInput 颜色输入
     * @return 执行结果
     */
    public static int executeRecolorChange(CommandContext<ServerCommandSource> context, String areaName, String colorInput) {
        ServerCommandSource source = context.getSource();
        
        if (source.getPlayer() == null) {
            source.sendMessage(Text.translatable("command.error.general_10"));
            return 0;
        }

        ServerPlayerEntity player = source.getPlayer();
        String playerName = player.getName().getString();

        // 标准化颜色输入
        String normalizedColor = areahint.util.ColorUtil.normalizeColor(colorInput);
        if (!areahint.util.ColorUtil.isValidColor(normalizedColor)) {
            source.sendMessage(Text.translatable("command.error.color").append(Text.literal(colorInput)));
            source.sendMessage(Text.translatable("message.message.color"));
            source.sendMessage(Text.translatable("message.message.general_45"));
            return 0;
        }
        
        // 获取玩家当前维度
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        String dimensionType = convertDimensionIdToType(dimension);
        
        // 处理重新着色请求
        handleRecolorRequest(player, areaName, normalizedColor, dimensionType);
        
        return 1;
    }
    
    /**
     * 获取玩家可编辑的域名列表
     */
    private static List<AreaData> getEditableAreas(ServerPlayerEntity player, String fileName, String playerName) {
        List<AreaData> editableAreas = new ArrayList<>();

        try {
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (areaFile == null || !areaFile.toFile().exists()) {
                return editableAreas;
            }

            List<AreaData> allAreas = FileManager.readAreaData(areaFile);

            for (AreaData area : allAreas) {
                if (canEditArea(player, playerName, area)) {
                    editableAreas.add(area);
                }
            }

        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.area.list_2"), e);
        }

        return editableAreas;
    }
    
    /**
     * 发送域名列表到客户端
     */
    private static void sendAreaListToClient(ServerPlayerEntity player, List<AreaData> areas, String dimension) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("recolor_list");
            buf.writeString(dimension);
            buf.writeInt(areas.size());
            
            for (AreaData area : areas) {
                buf.writeString(area.getName());
                buf.writeString(area.getColor());
                buf.writeInt(area.getLevel());
                buf.writeString(area.getBaseName() != null ? area.getBaseName() : "");
                buf.writeString(area.getSignature() != null ? area.getSignature() : "");
            }
            
            ServerPlayNetworking.send(player, Packets.S2C_RECOLOR_RESPONSE, buf);
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.area.list"), e);
        }
    }
    
    /**
     * 处理客户端发送的重新着色请求
     */
    public static void handleRecolorRequest(ServerPlayerEntity player, String areaName, String newColor, String dimension) {
        try {
            String playerName = player.getName().getString();

            // 验证颜色格式
            if (!ColorUtil.isValidColor(newColor)) {
                sendRecolorResponse(player, false, key("command.error.color_3"), lit(newColor));
                return;
            }
            
            // 获取维度文件
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                sendRecolorResponse(player, false, key("addhint.error.dimension"), lit(dimension));
                return;
            }
            
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (areaFile == null || !areaFile.toFile().exists()) {
                sendRecolorResponse(player, false, key("command.message.dimension_5"));
                return;
            }
            
            // 读取和更新域名数据
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            boolean found = false;
            String oldColor = "";
            
            for (AreaData area : areas) {
                if (area.getName().equals(areaName)) {
                    if (!canEditArea(player, playerName, area)) {
                        sendRecolorResponse(player, false, key("command.message.area.modify.permission"), lit(areaName + "\""));
                        return;
                    }
                    
                    oldColor = area.getColor();
                    area.setColor(newColor);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                sendRecolorResponse(player, false, key("addhint.message.area_3"), lit(areaName));
                return;
            }
            
            // 保存文件
            if (FileManager.writeAreaData(areaFile, areas)) {
                sendRecolorResponse(player, true, key("command.message.area_10"), lit(areaName), key("command.message.color_4"), lit(oldColor), lit(" → "), lit(newColor));
                
                // 重新加载并发送给所有客户端
                ServerNetworking.sendAllAreaDataToAll();
                
                Areashint.LOGGER.info(ServerI18nManager.translate("command.message.general_28") + playerName + ServerI18nManager.translate("command.message.area_4") + areaName + "\"" + ServerI18nManager.translate("command.message.color_3") + oldColor + ServerI18nManager.translate("command.message.general_5") + newColor);
                
            } else {
                sendRecolorResponse(player, false, key("command.error.area.save"));
            }
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_20"), e);
            sendRecolorResponse(player, false, key("command.error.general_19"), lit(e.getMessage()));
        }
    }
    
    private static boolean canEditArea(ServerPlayerEntity player, String playerName, AreaData area) {
        return PermissionService.hasNodeOr(player, PermissionNodes.RECOLOR,
            () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
    }

    /**
     * 发送重新着色响应
     */
    private static void sendRecolorResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("recolor_response");
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            ServerPlayNetworking.send(player, Packets.S2C_RECOLOR_RESPONSE, buf);
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_18"), e);
        }
    }
    
    /**
     * 发送交互式recolor界面到客户端
     */
    private static void sendInteractiveRecolorToClient(ServerPlayerEntity player, List<AreaData> areas, String dimension) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("recolor_interactive");
            buf.writeString(dimension);
            buf.writeInt(areas.size());
            
            for (AreaData area : areas) {
                buf.writeString(area.getName());
                buf.writeString(area.getColor());
                buf.writeInt(area.getLevel());
                buf.writeString(area.getBaseName() != null ? area.getBaseName() : "");
                buf.writeString(area.getSignature() != null ? area.getSignature() : "");
            }
            
            ServerPlayNetworking.send(player, Packets.S2C_RECOLOR_RESPONSE, buf);
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_15"), e);
        }
    }
    
    /**
     * 将维度ID转换为Packets期望的维度类型
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
}
