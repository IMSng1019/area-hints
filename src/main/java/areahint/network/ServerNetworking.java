package areahint.network;

import areahint.Areashint;
import areahint.file.FileManager;
import areahint.map.BlueMapCompat;
import areahint.i18n.ServerI18nManager;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.util.AreaPermissionUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import areahint.network.TranslatableMessage;
import areahint.network.TranslatableMessage.Part;
import static areahint.network.TranslatableMessage.key;
import static areahint.network.TranslatableMessage.lit;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;

/**
 * 服务端网络处理类
 * 处理服务端与客户端之间的通信
 */
public class ServerNetworking {
    /**
     * 初始化网络处理
     */
    public static void init() {
        // 注册网络通道和处理器
        Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_67"));
        
        // 注册网络请求处理器
        registerNetworkHandlers();
        
        // 注册玩家连接事件，当玩家加入服务器时发送区域数据和维度域名数据
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_28") + player.getName().getString() + ServerI18nManager.translate("message.message.area.dimension"));
            
            // 发送所有维度的区域数据
            sendAllAreaDataToClient(player);
            
            // 发送维度域名配置
            areahint.network.DimensionalNameNetworking.sendDimensionalNamesToClient(player);
            
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_123") + player.getName().getString() + ServerI18nManager.translate("message.message.general_10"));
        });

        // 玩家断开连接时清理语言偏好
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerI18nManager.removePlayer(handler.getPlayer().getUuid());
        });
    }
    
    /**
     * 向客户端发送区域数据
     * @param player 目标玩家
     * @param dimensionName 维度名称（overworld、the_nether、the_end）
     */
    public static void sendAreaDataToClient(ServerPlayerEntity player, String dimensionName) {
        try {
            String fileName = Packets.getFileNameForDimension(dimensionName);
            if (fileName == null) {
                Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.dimension.name") + dimensionName);
                return;
            }
            
            // 使用世界文件夹管理器获取文件路径
            Path filePath = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            Areashint.LOGGER.info(ServerI18nManager.translate("message.button.general_15") + filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.general_69") + filePath);
                // 尝试创建空文件
                try {
                    FileManager.createEmptyAreaFile(filePath);
                    Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_117") + filePath);
                } catch (IOException e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.general_12") + e.getMessage());
                }
                return;
            }
            
            // 读取文件内容
            String fileContent = Files.readString(filePath);
            Areashint.LOGGER.info(ServerI18nManager.translate("message.button.general_12") + fileContent.length() + ServerI18nManager.translate("message.message.general_12"));
            if (fileContent.length() < 100) {
                Areashint.LOGGER.info(ServerI18nManager.translate("message.button.general_13") + fileContent);
            } else {
                Areashint.LOGGER.info(ServerI18nManager.translate("message.button.general_13") + fileContent.substring(0, 100) + "...");
            }
            
            // 创建数据包
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeString(dimensionName);
            buffer.writeString(fileContent);
            
            // 发送数据包
            ServerPlayNetworking.send(player, new Identifier(Packets.S2C_AREA_DATA), buffer);
            
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_123") + player.getName().getString() + ServerI18nManager.translate("message.message.general_8") + dimensionName + ServerI18nManager.translate("message.message.general_19"));
            
        } catch (IOException e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.message.general_70") + e.getMessage());
        }
    }
    
    /**
     * 向所有客户端发送区域数据
     * @param dimensionName 维度名称（overworld、the_nether、the_end）
     */
    public static void sendAreaDataToAll(String dimensionName) {
        if (Areashint.getServer() == null) {
            return;
        }
        
        List<ServerPlayerEntity> players = Areashint.getServer().getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
            sendAreaDataToClient(player, dimensionName);
        }

        BlueMapCompat.requestDimensionSync(dimensionName);
    }
    
    /**
     * 向所有客户端发送所有维度的区域数据
     */
    public static void sendAllAreaDataToAll() {
        Areashint.LOGGER.info(ServerI18nManager.translate("message.button.dimension"));
        sendAreaDataToAll(Packets.DIMENSION_OVERWORLD);
        sendAreaDataToAll(Packets.DIMENSION_NETHER);
        sendAreaDataToAll(Packets.DIMENSION_END);
        BlueMapCompat.requestFullSync();
        Areashint.LOGGER.info(ServerI18nManager.translate("message.button.dimension.finish"));
    }
    
    /**
     * 向指定玩家发送所有维度的区域数据
     * @param player 目标玩家
     */
    public static void sendAllAreaDataToClient(ServerPlayerEntity player) {
        sendAreaDataToClient(player, Packets.DIMENSION_OVERWORLD);
        sendAreaDataToClient(player, Packets.DIMENSION_NETHER);
        sendAreaDataToClient(player, Packets.DIMENSION_END);
    }
    
    /**
     * 向客户端发送命令
     * @param player 目标玩家
     * @param command 命令字符串
     */
    public static void sendCommandToClient(ServerPlayerEntity player, String command) {
        try {
            // 创建数据包
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeString(command);
            
            // 发送数据包
            ServerPlayNetworking.send(player, new Identifier(Packets.S2C_CLIENT_COMMAND), buffer);
            
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_123") + player.getName().getString() + ServerI18nManager.translate("message.message.general_9") + command);
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.message.general_71") + e.getMessage());
        }
    }
    
    /**
     * 向所有在线客户端发送命令
     */
    public static void sendCommandToAllClients(MinecraftServer server, String command) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendCommandToClient(player, command);
        }
    }

    /**
     * 向客户端发送调试命令
     * @param player 目标玩家
     * @param enabled 是否启用调试
     */
    public static void sendDebugCommandToClient(ServerPlayerEntity player, boolean enabled) {
        try {
            // 创建数据包
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeBoolean(enabled);
            
            // 发送数据包
            ServerPlayNetworking.send(player, new Identifier(Packets.S2C_DEBUG_COMMAND), buffer);
            
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_123") + player.getName().getString() + ServerI18nManager.translate("message.message.general_11") + (enabled ? ServerI18nManager.translate("message.message.general_74") : ServerI18nManager.translate("message.message.general_213")));
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.message.general_72") + e.getMessage());
        }
    }
    
    /**
     * 向所有在线玩家发送指定维度的区域数据
     * @param dimensionType 维度类型
     */
    public static void sendAreaDataToAllPlayers(String dimensionType) {
        try {
            // 获取所有在线玩家
            List<ServerPlayerEntity> players = Areashint.getServer().getPlayerManager().getPlayerList();
            
            for (ServerPlayerEntity player : players) {
                // 检查玩家是否在指定维度中
                String playerDimension = player.getWorld().getRegistryKey().getValue().toString();
                String playerDimensionType = Packets.convertDimensionPathToType(playerDimension);
                
                if (playerDimensionType != null && playerDimensionType.equals(dimensionType)) {
                    // 发送区域数据到该玩家
                    sendAreaDataToClient(player, dimensionType);
                }
            }
            
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.dimension_6"), dimensionType);
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.error.general_34") + e.getMessage(), e);
        }
    }
    
    /**
     * 注册网络请求处理器
     */
    private static void registerNetworkHandlers() {
        // 注册语言同步处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_LANGUAGE_SYNC,
            (server, player, handler, buf, responseSender) -> {
                String lang = buf.readString();
                server.execute(() -> ServerI18nManager.setPlayerLanguage(player.getUuid(), lang));
            });

        // 注册recolor请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_RECOLOR_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    String areaName = buf.readString();
                    String color = buf.readString();
                    String dimension = buf.readString();

                    server.execute(() -> {
                        areahint.command.RecolorCommand.handleRecolorRequest(player, areaName, color, dimension);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.general_37"), e);
                }
            });

        // 注册Teleport请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_TELEPORT_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String mode = buf.readString();
                    final String areaName = buf.readString();
                    final String teleportFormat = buf.readString();
                    server.execute(() -> areahint.teleport.TeleportService.handleTeleportRequest(player, mode, areaName, teleportFormat));
                } catch (Exception e) {
                    Areashint.LOGGER.error("处理传送请求时发生错误", e);
                }
            });

        // 注册SetHigh请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_SETHIGH_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String areaName = buf.readString();
                    final boolean hasCustomHeight = buf.readBoolean();
                    final Double maxHeight;
                    final Double minHeight;

                    if (hasCustomHeight) {
                        boolean hasMax = buf.readBoolean();
                        maxHeight = hasMax ? buf.readDouble() : null;
                        boolean hasMin = buf.readBoolean();
                        minHeight = hasMin ? buf.readDouble() : null;
                    } else {
                        maxHeight = null;
                        minHeight = null;
                    }

                    server.execute(() -> {
                        areahint.command.SetHighCommand.handleHeightRequest(player, areaName, hasCustomHeight, maxHeight, minHeight);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.general_36"), e);
                }
            });

        // 注册请求可删除域名列表处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_REQUEST_DELETABLE_AREAS,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String dimension = buf.readString();

                    server.execute(() -> {
                        sendDeletableAreasList(player, dimension);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.area.delete.list_5"), e);
                }
            });

        // 注册Delete请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DELETE_AREA,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String areaName = buf.readString();
                    final String dimension = buf.readString();

                    server.execute(() -> {
                        handleDeleteRequest(player, areaName, dimension);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.general_35"), e);
                }
            });

        // 注册维度域名修改请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DIMNAME_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String dimensionId = buf.readString();
                    final String newName = buf.readString();
                    server.execute(() -> {
                        if (!PermissionService.hasCommandPermission(player, PermissionNodes.DIMENSIONALITY_NAME, 2)) {
                            player.sendMessage(net.minecraft.text.Text.translatable("message.error.permission"), false);
                            return;
                        }
                        areahint.command.DimensionalNameCommands.handleDimNameChange(
                            player.getCommandSource(), dimensionId, newName);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.area.dimension.modify_2"), e);
                }
            });

        // 注册维度域名颜色修改请求处理器
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_DIMCOLOR_REQUEST,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String dimensionId = buf.readString();
                    final String newColor = buf.readString();
                    server.execute(() -> {
                        if (!PermissionService.hasCommandPermission(player, PermissionNodes.DIMENSIONALITY_COLOR, 2)) {
                            player.sendMessage(net.minecraft.text.Text.translatable("message.error.permission"), false);
                            return;
                        }
                        areahint.command.DimensionalNameCommands.handleDimColorChange(
                            player.getCommandSource(), dimensionId, newColor);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.area.dimension.modify_2"), e);
                }
            });

        // 注册首次维度命名处理器，同时校验权限节点并限制为尚未命名的维度。
        ServerPlayNetworking.registerGlobalReceiver(Packets.C2S_FIRST_DIMNAME,
            (server, player, handler, buf, responseSender) -> {
                try {
                    final String dimensionId = buf.readString();
                    final String newName = buf.readString();
                    server.execute(() -> {
                        // 网络包可能绕过 Brigadier 指令树，因此写入维度域名前必须再次检查同一权限节点。
                        if (!PermissionService.hasCommandPermission(player, PermissionNodes.FIRST_DIM_NAME, 0)) {
                            player.sendMessage(net.minecraft.text.Text.translatable("message.error.permission"), false);
                            return;
                        }
                        String currentName = areahint.dimensional.DimensionalNameManager.getDimensionalName(dimensionId);
                        // 仅当维度名称等于维度ID时（未被命名）才允许
                        if (!currentName.equals(dimensionId)) {
                            player.sendMessage(net.minecraft.text.Text.translatable("message.error.dimension_2"), false);
                            return;
                        }
                        areahint.command.DimensionalNameCommands.handleDimNameChange(
                            player.getCommandSource(), dimensionId, newName);
                    });
                } catch (Exception e) {
                    Areashint.LOGGER.error(ServerI18nManager.translate("message.error.dimension_4"), e);
                }
            });
    }

    /**
     * 发送可删除域名列表到客户端
     * @param player 请求的玩家
     * @param dimension 维度标识
     */
    private static void sendDeletableAreasList(ServerPlayerEntity player, String dimension) {
        try {
            String playerName = player.getName().getString();
            boolean hasNode = PermissionService.hasCommandPermission(player, PermissionNodes.DELETE, 2);

            Areashint.LOGGER.info(ServerI18nManager.translate("message.prompt.area.delete.list") + playerName + ServerI18nManager.translate("message.message.dimension_2") + dimension + ServerI18nManager.translate("message.message.general_21") + hasNode);

            // 获取文件名
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.dimension_11") + dimension);
                // 发送空列表
                sendEmptyDeletableAreasList(player);
                return;
            }

            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);

            // 读取区域数据
            java.util.List<areahint.data.AreaData> allAreas = FileManager.readAreaData(areaFile);
            java.util.List<areahint.data.AreaData> deletableAreas = new java.util.ArrayList<>();

            // 筛选可删除的域名
            for (areahint.data.AreaData area : allAreas) {
                // 检查权限
                boolean canDelete = PermissionService.hasNodeOr(player, PermissionNodes.DELETE,
                    () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));

                if (canDelete) {
                    // 检查是否有次级域名引用此域名
                    boolean hasChildren = false;
                    for (areahint.data.AreaData childArea : allAreas) {
                        if (area.getName().equals(childArea.getBaseName())) {
                            hasChildren = true;
                            break;
                        }
                    }

                    // 只有没有子域名的才能删除
                    if (!hasChildren) {
                        deletableAreas.add(area);
                    }
                }
            }

            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_175") + deletableAreas.size() + ServerI18nManager.translate("message.message.area.delete_2"));

            // 创建数据包
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeInt(deletableAreas.size());

            for (areahint.data.AreaData area : deletableAreas) {
                String json = areahint.file.JsonHelper.toJsonSingle(area);
                buffer.writeString(json);
            }

            // 发送数据包
            ServerPlayNetworking.send(player, Packets.S2C_DELETABLE_AREAS_LIST, buffer);

            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_123") + playerName + ServerI18nManager.translate("message.message.area.delete.list"));

        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.error.area.delete.list_2"), e);
            sendEmptyDeletableAreasList(player);
        }
    }

    /**
     * 发送空的可删除域名列表
     * @param player 目标玩家
     */
    private static void sendEmptyDeletableAreasList(ServerPlayerEntity player) {
        try {
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeInt(0);
            ServerPlayNetworking.send(player, Packets.S2C_DELETABLE_AREAS_LIST, buffer);
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.error.area.delete.list_3"), e);
        }
    }

    /**
     * 处理删除域名请求
     * @param player 请求的玩家
     * @param areaName 要删除的域名名称
     * @param dimension 维度标识
     */
    private static void handleDeleteRequest(ServerPlayerEntity player, String areaName, String dimension) {
        try {
            String playerName = player.getName().getString();

            Areashint.LOGGER.info(ServerI18nManager.translate("message.prompt.delete_3") + playerName + ServerI18nManager.translate("message.message.area_5") + areaName + ServerI18nManager.translate("message.message.dimension_2") + dimension);

            // 获取文件名
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.dimension_11") + dimension);
                sendDeleteResponse(player, false, key("message.message.dimension_12"), lit(dimension + "）"));
                return;
            }

            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.dimension_16") + fileName);

            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.area_9") + areaFile.toAbsolutePath());

            // 读取区域数据
            java.util.List<areahint.data.AreaData> areas = FileManager.readAreaData(areaFile);
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_224") + areas.size() + ServerI18nManager.translate("message.message.area_3"));

            // 查找要删除的域名
            areahint.data.AreaData targetArea = null;
            for (areahint.data.AreaData area : areas) {
                if (area.getName().equals(areaName)) {
                    targetArea = area;
                    break;
                }
            }

            if (targetArea == null) {
                Areashint.LOGGER.warn(ServerI18nManager.translate("addhint.message.area_3") + areaName);
                sendDeleteResponse(player, false, key("addhint.message.area_3"), lit(areaName));
                return;
            }

            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.area_11") + areaName + ServerI18nManager.translate("message.message.general_3") + targetArea.getSignature());

            // 检查签名权限
            final areahint.data.AreaData finalTargetArea = targetArea;
            String signature = finalTargetArea.getSignature();
            boolean canDelete = PermissionService.hasNodeOr(player, PermissionNodes.DELETE,
                () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(finalTargetArea, playerName));
            if (!canDelete) {
                if (signature == null) {
                    Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.general_28") + playerName + ServerI18nManager.translate("message.message.area.delete") + ServerI18nManager.translate("message.message.area.delete_4"));
                    sendDeleteResponse(player, false, key("message.message.area.delete_7"));
                } else {
                    Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.general_28") + playerName + ServerI18nManager.translate("message.message.area_2") + signature);
                    sendDeleteResponse(player, false, key("message.message.area.delete_6"));
                }
                return;
            }
            if (signature == null) {
                Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_216") + playerName + ServerI18nManager.translate("message.message.area.delete_4"));
            }

            // 检查是否有次级域名引用此域名
            for (areahint.data.AreaData area : areas) {
                if (areaName.equals(area.getBaseName())) {
                    Areashint.LOGGER.warn(ServerI18nManager.translate("message.message.area_8") + areaName + ServerI18nManager.translate("message.message.area_4") + area.getName() + ServerI18nManager.translate("message.message.general_15"));
                    sendDeleteResponse(player, false, key("message.message.area.delete_5"), lit(area.getName()), key("message.message.general_16"));
                    return;
                }
            }

            // 执行删除
            areas.remove(targetArea);
            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.area.list_2") + areaName);

            // 保存文件
            FileManager.writeAreaData(areaFile, areas);

            // 向所有客户端发送更新后的区域数据
            sendAllAreaDataToAll();

            // 发送成功响应
            sendDeleteResponse(player, true, lit(areaName));

            Areashint.LOGGER.info(ServerI18nManager.translate("message.message.general_28") + playerName + ServerI18nManager.translate("message.message.area.delete_3") + areaName);

        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.error.delete_5"), e);
            sendDeleteResponse(player, false, key("message.error.area.delete_3"), lit(e.getMessage()));
        }
    }

    /**
     * 发送删除响应到客户端
     * @param player 目标玩家
     * @param success 是否成功
     * @param message 响应消息
     */
    private static void sendDeleteResponse(ServerPlayerEntity player, boolean success, Part... parts) {
        try {
            PacketByteBuf buffer = PacketByteBufs.create();
            buffer.writeBoolean(success);
            TranslatableMessage.write(buffer, parts);
            ServerPlayNetworking.send(player, Packets.S2C_DELETE_RESPONSE, buffer);
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("message.message.delete_3") + e.getMessage());
        }
    }
}
