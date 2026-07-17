package areahint.command;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.management.AreaManagementCapabilityService;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 域名重命名命令处理类
 * 实现 /areahint rename 指令功能（交互式流程）
 */
public class RenameAreaCommand {

    /**
     * 执行rename指令（启动交互式流程）
     * @param context 命令上下文
     * @return 执行结果
     */
    public static int executeRename(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
                sendRenameableAreaList(player);
            } else {
                source.sendMessage(Text.translatable("command.error.general_9"));
            }
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_22") + e.getMessage());
            source.sendMessage(Text.translatable("command.error.general_4"));
        }

        return 1;
    }

    /**
     * 处理域名选择命令
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    public static int executeRenameSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            // 发送命令到客户端
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:rename_select:" + areaName);
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 处理确认命令
     * @param context 命令上下文
     * @return 执行结果
     */
    public static int executeRenameConfirm(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            // 发送命令到客户端
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:rename_confirm");
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.confirm.rename").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 处理取消命令
     * @param context 命令上下文
     * @return 执行结果
     */
    public static int executeRenameCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            // 发送命令到客户端
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:rename_cancel");
            return 1;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel.rename").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 发送可重命名域名列表到客户端
     * @param player 玩家
     */
    private static void sendRenameableAreaList(ServerPlayerEntity player) {
        try {
            String playerName = player.getName().getString();
            RegistryKey<World> dimensionType = player.getWorld().getRegistryKey();
            String dimensionId = dimensionType.getValue().toString();

            // 获取维度文件名
            String dimensionPath = dimensionType.getValue().getPath();
            String fileName = Packets.getFileNameForDimension(
                Packets.convertDimensionPathToType(dimensionPath));

            if (fileName == null) {
                sendRenameResponse(player, false, key("command.message.dimension_2"));
                return;
            }

            // 获取域名文件路径
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString("rename_list");
                buf.writeString(dimensionId);
                buf.writeInt(0); // 域名数量
                ServerPlayNetworking.send(player, Packets.S2C_RENAME_RESPONSE, buf);
                return;
            }

            // 读取域名数据
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            List<AreaData> editableAreas = new ArrayList<>();

            // 筛选可编辑的域名
            for (AreaData area : areas) {
                if (canRenameArea(player, area, areas)) {
                    editableAreas.add(area);
                }
            }

            // 发送到客户端
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString("rename_list");
            buf.writeString(dimensionId);
            buf.writeInt(editableAreas.size());

            for (AreaData area : editableAreas) {
                buf.writeString(area.getName());
                buf.writeString(area.getSignature() != null ? area.getSignature() : "");
            }

            ServerPlayNetworking.send(player, Packets.S2C_RENAME_RESPONSE, buf);

        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.message.area.list") + e.getMessage());
            sendRenameResponse(player, false, key("command.error.area_4"));
        }
    }

    /**
     * 处理域名重命名请求
     * @param player 玩家
     * @param oldName 原域名
     * @param newName 新域名
     * @param newSurfaceName 新联合域名（可为null或空字符串）
     * @param dimension 维度ID
     */
    public static void handleRenameRequest(ServerPlayerEntity player, String oldName, String newName,
                                          String newSurfaceName, String dimension) {
        try {
            String playerName = player.getName().getString();

            if (!AreaManagementCapabilityService.isCurrentDimension(player, dimension)) {
                sendRenameResponse(player, false, key("command.message.dimension_3"));
                return;
            }

            // 验证新域名格式
            if (newName == null || newName.trim().isEmpty()) {
                sendRenameResponse(player, false, key("command.message.area_11"));
                return;
            }

            newName = newName.trim();

            // 处理空字符串的联合域名
            if (newSurfaceName != null && newSurfaceName.trim().isEmpty()) {
                newSurfaceName = null;
            }

            // 获取维度文件名
            String dimensionPath = dimension.substring(dimension.lastIndexOf(":") + 1);
            String fileName = Packets.getFileNameForDimension(
                Packets.convertDimensionPathToType(dimensionPath));

            if (fileName == null) {
                sendRenameResponse(player, false, key("command.message.dimension_3"));
                return;
            }

            // 获取域名文件
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                sendRenameResponse(player, false, key("command.message.dimension_5"));
                return;
            }

            // 读取域名数据
            List<AreaData> areas = FileManager.readAreaData(areaFile);
            AreaData targetArea = null;
            boolean newNameExists = false;

            // 查找目标域名和检查新名称是否已存在
            for (AreaData area : areas) {
                if (area.getName().equals(oldName)) {
                    targetArea = area;
                }
                if (area.getName().equals(newName)) {
                    newNameExists = true;
                }
            }

            // 验证
            if (targetArea == null) {
                sendRenameResponse(player, false, key("addhint.message.area_3"), lit(oldName));
                return;
            }

            if (newNameExists) {
                sendRenameResponse(player, false, key("command.message.area_10"), lit(newName + "\""), key("command.prompt.name"));
                return;
            }

            if (!canRenameArea(player, targetArea, areas)) {
                sendRenameResponse(player, false, key("command.message.area.rename.permission"), lit(oldName + "\""));
                return;
            }

            // 执行重命名
            targetArea.setName(newName);

            // 更新联合域名
            if (newSurfaceName != null) {
                targetArea.setSurfacename(newSurfaceName);
            }

            // 更新所有引用该域名的子域名的 base-name
            for (AreaData area : areas) {
                if (oldName.equals(area.getBaseName())) {
                    area.setBaseName(newName);
                }
            }

            // 保存文件
            if (FileManager.writeAreaData(areaFile, areas)) {
                player.sendMessage(Text.translatable("command.success.area.rename"));
                player.sendMessage(Text.translatable("command.success.area.rename.old").append(Text.literal(oldName)));
                player.sendMessage(Text.translatable("command.message.area_5").append(Text.literal(newName)));
                if (newSurfaceName != null) {
                    player.sendMessage(Text.translatable("command.message.area.surface_3").append(Text.literal(newSurfaceName)));
                }
                sendRenameResponse(player, true, key("command.success.area.rename"));

                // 向所有客户端发送更新后的区域数据
                ServerNetworking.sendAllAreaDataToAll();

                Areashint.LOGGER.info(ServerI18nManager.translate("command.message.general_28") + playerName + ServerI18nManager.translate("command.message.area_4") + oldName + "\"" + ServerI18nManager.translate("command.message.rename") + newName + "\"");
            } else {
                sendRenameResponse(player, false, key("command.error.area.save"));
            }

        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.rename_2") + e.getMessage());
            sendRenameResponse(player, false, key("command.error.general_19"), lit(e.getMessage()));
        }
    }

    /**
     * 检查是否可以重命名域名
     * @param area 域名数据
     * @param playerName 玩家名称
     * @param isAdmin 是否为管理员
     * @return 是否可以重命名
     */
    private static boolean canRenameArea(ServerPlayerEntity player, AreaData area, List<AreaData> allAreas) {
        return AreaManagementCapabilityService.canPerform(
            player, AreaManagementCapabilityService.RENAME, area, allAreas);
    }

    /**
     * 发送重命名响应到客户端
     * @param player 玩家
     * @param success 是否成功
     * @param message 消息
     */
    private static void sendRenameResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString("rename_response");
        buf.writeBoolean(success);
        TranslatableMessage.write(buf, parts);
        ServerPlayNetworking.send(player, Packets.S2C_RENAME_RESPONSE, buf);
    }

    /**
     * 注册服务端网络接收器
     */
    public static void registerServerReceivers() {
        // 注册重命名请求接收器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_RENAME_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    String oldName = buf.readString();
                    String newName = buf.readString();
                    String newSurfaceName = buf.readString();
                    String dimension = buf.readString();

                    server.execute(() -> {
                        handleRenameRequest(player, oldName, newName, newSurfaceName, dimension);
                    });

                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("command.error.rename"), e);
                    sendRenameResponse(player, false, key("command.error.general_23"), lit(e.getMessage()));
                }
            });
    }
}
