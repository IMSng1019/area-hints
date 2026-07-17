package areahint.command;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.management.AreaManagementCapabilityService;

import areahint.network.Packets;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 域名高度设置命令处理器
 * 处理 /areahint sethigh 命令
 */
public class SetHighCommand {
    
    /**
     * 执行设置高度命令
     * @param context 命令上下文
     * @return 执行结果
     */
    public static int executeSetHigh(CommandContext<ServerCommandSource> context) {
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

        // 获取可修改高度的域名列表
        List<AreaData> editableAreas = getHeightEditableAreas(player, fileName, playerName);

        if (editableAreas.isEmpty()) {
            source.sendMessage(Text.translatable("command.error.area.altitude.dimension"));
            return 0;
        }

        // 启动交互式高度设置流程
        startInteractiveHeightSetting(source, editableAreas);

        return 1;
    }

    /**
     * 启动交互式高度设置流程
     * @param source 命令源
     * @param editableAreas 可编辑的域名列表
     */
    private static void startInteractiveHeightSetting(ServerCommandSource source, List<AreaData> editableAreas) {
        try {
            // 发送域名列表到客户端，启动交互式流程
            List<String> areaNames = editableAreas.stream()
                    .map(AreaData::getName)
                    .toList();
            
            // 获取维度类型
            String dimension = source.getPlayer().getWorld().getRegistryKey().getValue().toString();
            String dimensionType = convertDimensionIdToType(dimension);
            
            // 发送数据包到客户端，格式与客户端期望的一致
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("sethigh_area_list"); // 命令类型
            buf.writeString(dimensionType); // 维度类型
            buf.writeInt(editableAreas.size()); // 域名数量
            
            // 写入每个域名的信息
            for (AreaData area : editableAreas) {
                buf.writeString(area.getName());
                
                AreaData.AltitudeData altitude = area.getAltitude();
                boolean hasAltitude = altitude != null && (altitude.getMax() != null || altitude.getMin() != null);
                buf.writeBoolean(hasAltitude);
                
                if (hasAltitude) {
                    buf.writeBoolean(altitude.getMax() != null);
                    if (altitude.getMax() != null) {
                        buf.writeDouble(altitude.getMax());
                    }
                    buf.writeBoolean(altitude.getMin() != null);
                    if (altitude.getMin() != null) {
                        buf.writeDouble(altitude.getMin());
                    }
                }
            }
            
            ServerPlayNetworking.send(source.getPlayer(), Packets.S2C_SETHIGH_AREA_LIST, buf);
            
            source.sendMessage(Text.translatable("command.prompt.area.altitude.modify"));

        } catch (Exception e) {
            Areashint.LOGGER.error("启动交互式高度设置流程时发生错误", e);
            source.sendMessage(Text.translatable("command.error.altitude.start"));
        }
    }

    /**
     * 执行设置指定域名高度的命令
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    public static int executeSetHighWithArea(CommandContext<ServerCommandSource> context, String areaName) {
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

        // 获取可修改高度的域名列表
        List<AreaData> editableAreas = getHeightEditableAreas(player, fileName, playerName);

        if (editableAreas.isEmpty()) {
            source.sendMessage(Text.translatable("command.error.area.altitude.dimension"));
            return 0;
        }

        // 查找指定的域名
        AreaData targetArea = editableAreas.stream()
                .filter(area -> area.getName().equals(areaName))
                .findFirst()
                .orElse(null);

        if (targetArea == null) {
            source.sendMessage(Text.translatable("addhint.error.area").append(Text.literal(areaName)).append(Text.translatable("command.message.altitude.modify.permission")));
            // 启动交互式流程，让用户选择其他域名
            startInteractiveHeightSetting(source, editableAreas);
            return 0;
        }
        
        // 直接启动指定域名的交互式高度设置流程
        startInteractiveHeightSettingForSpecificArea(source, targetArea);
        
        return 1;
    }
    
    /**
     * 启动指定域名的交互式高度设置流程
     * @param source 命令源
     * @param targetArea 目标域名
     */
    private static void startInteractiveHeightSettingForSpecificArea(ServerCommandSource source, AreaData targetArea) {
        try {
            // 发送指定域名的数据到客户端
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(targetArea.getName());
            
            // 写入当前高度信息
            AreaData.AltitudeData altitude = targetArea.getAltitude();
            boolean hasAltitude = altitude != null && (altitude.getMax() != null || altitude.getMin() != null);
            buf.writeBoolean(hasAltitude);
            
            if (hasAltitude) {
                buf.writeBoolean(altitude.getMax() != null);
                if (altitude.getMax() != null) {
                    buf.writeDouble(altitude.getMax());
                }
                buf.writeBoolean(altitude.getMin() != null);
                if (altitude.getMin() != null) {
                    buf.writeDouble(altitude.getMin());
                }
            }
            
            ServerPlayNetworking.send(source.getPlayer(), Packets.S2C_SETHIGH_AREA_SELECTION, buf);
            
            source.sendMessage(Text.translatable("command.message.area.start").append(Text.literal(targetArea.getName())).append(Text.translatable("command.message.altitude_3")));

        } catch (Exception e) {
            Areashint.LOGGER.error("启动指定域名高度设置流程时发生错误", e);
            source.sendMessage(Text.translatable("command.error.altitude.start"));
        }
    }

    /**
     * 获取可编辑高度的域名列表
     * @param player 玩家
     * @param fileName 文件名
     * @param playerName 玩家名称
     * @return 可编辑的域名列表
     */
    private static List<AreaData> getHeightEditableAreas(ServerPlayerEntity player, String fileName, String playerName) {
        try {
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return new ArrayList<>();
            }
            
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            
            return areas.stream()
                    .filter(area -> canEditAreaHeight(player, area, playerName, areas))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            Areashint.LOGGER.error("读取域名数据时发生错误", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查是否可以编辑域名高度
     * @param player 玩家
     * @param area 域名数据
     * @param playerName 玩家名称
     * @param allAreas 所有域名数据（用于检查basename引用）
     * @return 是否可以编辑
     */
    private static boolean canEditAreaHeight(ServerPlayerEntity player, AreaData area, String playerName, List<AreaData> allAreas) {
        return AreaManagementCapabilityService.canPerform(
            player, AreaManagementCapabilityService.SET_HIGH, area, allAreas);
    }
    
    /**
     * 直接在聊天栏显示可修改高度的域名列表
     * @param source 命令源
     * @param editableAreas 可编辑的域名列表
     * @param isAdmin 是否为管理员
     */
    private static void displayEditableAreasInChat(ServerCommandSource source, List<AreaData> editableAreas, boolean isAdmin) {
        if (editableAreas.isEmpty()) {
            source.sendMessage(Text.translatable("command.error.area.altitude.dimension"));
            return;
        }

        java.util.UUID playerUuid = source.getPlayer() != null ? source.getPlayer().getUuid() : null;
        MutableText message = Text.translatable("command.message.area.altitude.modify");
        for (AreaData area : editableAreas) {
            message.append(Text.literal("§7- §f" + area.getName() + "§7: ")).append(getHeightDisplayText(area.getAltitude(), playerUuid)).append(Text.literal("\n"));
        }
        source.sendMessage(message);
    }
    
    /**
     * 获取高度的显示字符串
     * @param altitude 高度数据
     * @return 显示字符串
     */
    private static MutableText getHeightDisplayText(AreaData.AltitudeData altitude, java.util.UUID playerUuid) {
        if (altitude == null) {
            return playerUuid != null ? Text.literal(ServerI18nManager.translateForPlayer(playerUuid, "command.message.general_10")) : Text.translatable("command.message.general_10");
        }

        MutableText result = Text.empty();
        boolean hasMax = false;
        if (altitude.getMax() != null) {
            String label = playerUuid != null ? ServerI18nManager.translateForPlayer(playerUuid, "command.error.general_8") : ServerI18nManager.translate("command.error.general_8");
            result.append(Text.literal(label + altitude.getMax()));
            hasMax = true;
        }
        if (altitude.getMin() != null) {
            if (hasMax) result.append(Text.literal("§7, "));
            String label = playerUuid != null ? ServerI18nManager.translateForPlayer(playerUuid, "command.message.general_11") : ServerI18nManager.translate("command.message.general_11");
            result.append(Text.literal(label + altitude.getMin()));
            hasMax = true;
        }

        if (!hasMax) {
            return playerUuid != null ? Text.literal(ServerI18nManager.translateForPlayer(playerUuid, "command.message.general_10")) : Text.translatable("command.message.general_10");
        }
        return result;
    }
    
    /**
     * 转换维度ID为类型
     * @param dimensionId 维度ID
     * @return 维度类型
     */
    private static String convertDimensionIdToType(String dimensionId) {
        if (dimensionId.contains("overworld")) {
            return Packets.DIMENSION_OVERWORLD;
        } else if (dimensionId.contains("nether")) {
            return Packets.DIMENSION_NETHER;
        } else if (dimensionId.contains("end")) {
            return Packets.DIMENSION_END;
        }
        return Packets.DIMENSION_OVERWORLD; // 默认返回主世界
    }
    
    /**
     * 处理高度设置请求
     * @param player 玩家
     * @param areaName 域名名称
     * @param hasCustomHeight 是否使用自定义高度
     * @param maxHeight 最大高度
     * @param minHeight 最小高度
     */
    public static void handleHeightRequest(ServerPlayerEntity player, String areaName, 
                                         boolean hasCustomHeight, Double maxHeight, Double minHeight) {
        try {
            String playerName = player.getName().getString();
                
            // 获取玩家当前维度
            String dimension = player.getWorld().getRegistryKey().getValue().toString();
            String dimensionType = convertDimensionIdToType(dimension);
            String fileName = Packets.getFileNameForDimension(dimensionType);
            
            if (fileName == null) {
                sendResponse(player, false, key("command.error.dimension_3"));
                return;
            }
            
            // 获取可修改高度的域名列表
            List<AreaData> editableAreas = getHeightEditableAreas(player, fileName, playerName);
            
            // 查找指定的域名
            AreaData targetArea = editableAreas.stream()
                    .filter(area -> area.getName().equals(areaName))
                    .findFirst()
                    .orElse(null);
            
            if (targetArea == null) {
                sendResponse(player, false, key("addhint.message.area_2"), lit(areaName), key("command.message.altitude.modify.permission"));
                return;
            }
            
            // 验证高度数据
            if (hasCustomHeight) {
                if (maxHeight != null && (maxHeight < -64 || maxHeight > 320)) {
                    sendResponse(player, false, key("command.button.altitude_4"));
                    return;
                }
                if (minHeight != null && (minHeight < -64 || minHeight > 320)) {
                    sendResponse(player, false, key("command.button.altitude_3"));
                    return;
                }
                if (maxHeight != null && minHeight != null && maxHeight < minHeight) {
                    sendResponse(player, false, key("command.message.altitude_13"));
                    return;
                }
            }
            
            // 更新域名高度数据
            AreaData.AltitudeData newAltitude = hasCustomHeight ? 
                    new AreaData.AltitudeData(maxHeight, minHeight) : null;
            
            targetArea.setAltitude(newAltitude);
            
            // 保存到文件
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> allAreas = FileManager.readAreaData(areaFile);
            
            // 更新指定域名
            for (int i = 0; i < allAreas.size(); i++) {
                if (allAreas.get(i).getName().equals(areaName)) {
                    allAreas.set(i, targetArea);
                    break;
                }
            }
            
            FileManager.writeAreaData(areaFile, allAreas);
            
            // 重新分发数据给所有玩家（相当于执行一次reload指令）
            // 使用 sendAllAreaDataToAll() 确保所有玩家都能收到更新，无论他们在哪个维度
            areahint.network.ServerNetworking.sendAllAreaDataToAll();
            
            // 发送成功响应
            if (hasCustomHeight) {
                String maxStr = maxHeight != null ? String.valueOf(maxHeight) : "";
                String minStr = minHeight != null ? String.valueOf(minHeight) : "";
                sendResponse(player, true, key("addhint.message.area_2"), lit(areaName), key("command.message.altitude_4"),
                    maxHeight != null ? lit(maxStr) : key("command.message.general_25"), lit(" ~ "),
                    minHeight != null ? lit(minStr) : key("command.message.general_25"));
            } else {
                sendResponse(player, true, key("addhint.message.area_2"), lit(areaName), key("command.message.altitude_5"));
            }
            
        } catch (Exception e) {
            Areashint.LOGGER.error("处理高度设置请求时发生错误", e);
            sendResponse(player, false, key("command.error.altitude_9"));
        }
    }
    
    /**
     * 执行自定义高度命令
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    public static int executeSetHighCustom(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.translatable("command.error.general_10"));
            return 0;
        }

        // 通过标准命令通道发送到客户端
        areahint.network.ServerNetworking.sendCommandToClient(source.getPlayer(), "areahint:sethigh_custom:" + areaName);
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行不限制高度命令
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    public static int executeSetHighUnlimited(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.translatable("command.error.general_10"));
            return 0;
        }

        // 直接处理不限制高度请求
        handleHeightRequest(source.getPlayer(), areaName, false, null, null);
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行取消高度设置命令
     * @param context 命令上下文
     * @return 执行结果
     */
    public static int executeSetHighCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (source.getPlayer() == null) {
            source.sendMessage(Text.translatable("command.error.general_10"));
            return 0;
        }

        source.sendMessage(Text.translatable("command.error.altitude.cancel"));
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 发送响应到客户端
     * @param player 玩家
     * @param success 是否成功
     * @param message 消息
     */
    private static void sendResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeBoolean(success);
            TranslatableMessage.write(buf, parts);
            ServerPlayNetworking.send(player, Packets.S2C_SETHIGH_RESPONSE, buf);
        } catch (Exception e) {
            Areashint.LOGGER.error("发送响应时发生错误", e);
        }
    }
    
    /**
     * 发送客户端命令
     * @param source 命令源
     * @param command 命令
     * @param args 参数
     */
    private static void sendClientCommand(ServerCommandSource source, String command, String... args) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(command);
            buf.writeInt(args.length);
            for (String arg : args) {
                buf.writeString(arg);
            }
            
            ServerPlayNetworking.send(source.getPlayer(), new Identifier(Packets.S2C_CLIENT_COMMAND), buf);
            
        } catch (Exception e) {
            Areashint.LOGGER.error("发送客户端命令时发生错误", e);
        }
    }
}
