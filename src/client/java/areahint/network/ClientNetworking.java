package areahint.network;

import areahint.Areashint;
import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.render.VulkanModCompat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端网络处理类
 * 处理客户端接收服务端数据
 */
public class ClientNetworking {
    /**
     * 初始化客户端网络处理
     */
    public static void init() {
        AreashintClient.LOGGER.info("初始化客户端网络处理");
        
        // 注册区域数据接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier(Packets.S2C_AREA_DATA),
                ClientNetworking::handleAreaData
        );
        
        // 注册客户端命令接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier(Packets.S2C_CLIENT_COMMAND),
                ClientNetworking::handleClientCommand
        );
        
        // 注册调试命令接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
                new Identifier(Packets.S2C_DEBUG_COMMAND),
                ClientNetworking::handleDebugCommand
        );
        
        // 注册recolor响应接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
                Packets.S2C_RECOLOR_RESPONSE,
                ClientNetworking::handleRecolorResponse
        );

        // 注册rename响应接收处理器（由 RenameNetworking 处理）
        areahint.rename.RenameNetworking.registerClientReceivers();

        // 注册Teleport响应接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
                Packets.S2C_TELEPORT_RESPONSE,
                ClientNetworking::handleTeleportResponse
        );

        // 注册SetHigh相关的网络处理器
        ClientPlayNetworking.registerGlobalReceiver(
                Packets.S2C_SETHIGH_AREA_LIST,
                ClientNetworking::handleSetHighAreaList
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
                Packets.S2C_SETHIGH_AREA_SELECTION,
                ClientNetworking::handleSetHighAreaSelection
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
                Packets.S2C_SETHIGH_RESPONSE,
                ClientNetworking::handleSetHighResponse
        );

        // 连接服务器时先自动适配模组语言，再同步给服务端
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register(
            (handler, sender, client) -> {
                adaptLanguageOnJoinIfUnlocked();
                sendLanguageToServer();
            }
        );
    }

    /**
     * 在进入世界时自动适配模组语言（未上锁时）
     */
    private static void adaptLanguageOnJoinIfUnlocked() {
        if (ClientConfig.isLanguageLocked()) {
            AreashintClient.LOGGER.info("语言已锁定，跳过进入世界时的自动语言适配");
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        String minecraftLanguage = client.options.language;
        String appliedLanguage = I18nManager.loadLanguage(minecraftLanguage);
        ClientConfig.setLanguage(appliedLanguage);
        AreashintClient.LOGGER.info("进入世界时自动适配模组语言: {} -> {}", minecraftLanguage, appliedLanguage);
    }

    /** 将客户端模组语言同步给服务端 */
    public static void sendLanguageToServer() {
        try {
            if (ClientPlayNetworking.canSend(Packets.C2S_LANGUAGE_SYNC)) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(I18nManager.getCurrentLanguage());
                ClientPlayNetworking.send(Packets.C2S_LANGUAGE_SYNC, buf);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("Failed to sync language: " + e.getMessage());
        }
    }
    
    /**
     * 处理接收到的区域数据
     * @param client Minecraft客户端实例
     * @param handler 网络处理器
     * @param buf 数据包缓冲区
     * @param responseSender 响应发送器
     */
    private static void handleAreaData(MinecraftClient client, 
                                      ClientPlayNetworkHandler handler,
                                      PacketByteBuf buf, 
                                      PacketSender responseSender) {
        // 读取维度名称和文件内容
        String dimensionName = buf.readString();
        String fileContent = buf.readString();
        
        // 确保在主线程中处理
        client.execute(() -> {
            try {
                // 确定文件名
                String fileName = Packets.getFileNameForDimension(dimensionName);
                if (fileName == null) {
                    AreashintClient.LOGGER.warn("接收到未知维度的区域数据: " + dimensionName);
                    return;
                }
                
                // 获取文件路径
                Path filePath = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
                AreashintClient.LOGGER.info("[调试] 客户端保存区域数据到文件: " + filePath.toAbsolutePath());
                
                // 确保目录存在
                FileManager.checkFolderExist();
                
                // 写入文件
                Files.writeString(filePath, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
                AreashintClient.LOGGER.info("已接收并保存 " + dimensionName + " 的区域数据");
                AreashintClient.LOGGER.info("[调试] 区域数据内容长度: " + fileContent.length() + " 字节");
                if (fileContent.length() < 100) {
                    AreashintClient.LOGGER.info("[调试] 区域数据内容预览: " + fileContent);
                } else {
                    AreashintClient.LOGGER.info("[调试] 区域数据内容预览: " + fileContent.substring(0, 100) + "...");
                }
                
                // 如果当前在该维度中，则重新加载区域数据
                if (client.world != null && 
                        dimensionName.equals(Packets.convertDimensionPathToType(client.world.getDimensionKey().getValue().getPath()))) {
                    AreashintClient.LOGGER.info("[调试] 重新加载当前维度的区域数据: " + fileName);
                    AreashintClient.getAreaDetector().loadAreaData(fileName);
                    areahint.boundviz.BoundVizManager.getInstance().reload();
                }
                
            } catch (IOException e) {
                AreashintClient.LOGGER.error("保存接收到的区域数据时出错: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理接收到的客户端命令
     * @param client Minecraft客户端实例
     * @param handler 网络处理器
     * @param buf 数据包缓冲区
     * @param responseSender 响应发送器
     */
    private static void handleClientCommand(MinecraftClient client, 
                                          ClientPlayNetworkHandler handler,
                                          PacketByteBuf buf, 
                                          PacketSender responseSender) {
        // 读取命令
        String command = buf.readString();
        
        // 确保在主线程中处理
        client.execute(() -> {
            try {
                AreashintClient.LOGGER.info("接收到服务端命令: " + command);
                
                // 解析命令（只在第一个冒号处分割，保留参数）
                int firstColonIndex = command.indexOf(":");
                if (firstColonIndex == -1) {
                    return;
                }
                
                String type = command.substring(0, firstColonIndex);
                String action = command.substring(firstColonIndex + 1);
                
                // 根据命令类型处理
                if (type.equals("areahint")) {
                    // 处理reload命令
                    if (action.equals("reload")) {
                        AreashintClient.reload();
                    } 
                    // 处理frequency命令
                    else if (action.startsWith("frequency")) {
                        if (action.equals("frequency_info")) {
                            displayFrequencyInfo(client);
                        } else {
                            try {
                                String[] frequencyParts = action.split(" ");
                                if (frequencyParts.length >= 2) {
                                    double value = Double.parseDouble(frequencyParts[1]);
                                    ClientConfig.setFrequency(value);
                                    AreashintClient.reload();
                                }
                            } catch (NumberFormatException e) {
                                AreashintClient.LOGGER.error("解析频率值时出错: " + e.getMessage());
                            }
                        }
                    }
                    // 处理hintrender命令
                    else if (action.startsWith("hintrender")) {
                        if (action.equals("hintrender_info")) {
                            displayHintRenderInfo(client);
                        } else {
                            handleHintRenderCommand(client, action);
                        }
                    }
                    // 处理titlestyle命令
                    else if (action.startsWith("titlestyle")) {
                        handleTitleStyleCommand(action);
                    }
                    // 处理titlesize命令
                    else if (action.startsWith("titlesize")) {
                        handleTitleSizeCommand(action);
                    }
                    // 处理副字幕命令
                    else if (action.startsWith("addsubtitle")
                            || action.startsWith("deletesubtitle")
                            || action.startsWith("replacesubtitlecolor")
                            || action.startsWith("replacesubtitlesize")) {
                        handleSubtitleCommand(action);
                    }
                    // 处理EasyAdd命令
                    else if (action.startsWith("easyadd")) {
                        handleEasyAddCommand(action);
                    }
                    // 处理ExpandArea命令
                    else if (action.startsWith("expandarea")) {
                        handleEasyAddCommand(action);
                    }
                    // 处理ShrinkArea命令
                    else if (action.startsWith("shrinkarea")) {
                        handleEasyAddCommand(action);
                    }
                    // 处理DivideArea命令
                    else if (action.startsWith("dividearea")) {
                        handleDivideAreaCommand(action);
                    }
                    // 处理AddHint命令
                    else if (action.startsWith("addhint")) {
                        handleEasyAddCommand(action);
                    }
                    // 处理DeleteHint命令
                    else if (action.startsWith("deletehint")) {
                        handleEasyAddCommand(action);
                    }
                    // 处理Recolor命令
                    else if (action.startsWith("recolor")) {
                        handleRecolorCommand(action);
                    }
                    // 处理DimName命令
                    else if (action.startsWith("dimname")) {
                        handleDimNameCommand(action);
                    }
                    // 处理DimColor命令
                    else if (action.startsWith("dimcolor")) {
                        handleDimColorCommand(action);
                    }
                    // 处理Rename命令
                    else if (action.startsWith("rename")) {
                        handleRenameCommand(action);
                    }
                    // 处理Description命令，必须在delete泛匹配前处理
                    else if (action.startsWith("adddescription")
                            || action.startsWith("replacedescription")
                            || action.startsWith("deletedescription")
                            || action.startsWith("adddimensionalitydescription")
                            || action.startsWith("replacedimensionalitydescription")
                            || action.startsWith("deletedimensionalitydescription")) {
                        areahint.description.DescriptionManager.getInstance().handleClientCommand(action);
                    }
                    // 处理Signature命令，必须在delete泛匹配前处理
                    else if (action.startsWith("addsignature") || action.startsWith("deletesignature")) {
                        handleSignatureCommand(action);
                    }
                    // 处理Delete命令
                    else if (action.startsWith("delete")) {
                        handleDeleteCommand(action);
                    }
                    // 处理ReplaceButton命令
                    else if (action.startsWith("replacebutton")) {
                        handleReplaceButtonCommand(action);
                    }
                    // 处理Language命令
                    else if (action.startsWith("language")) {
                        handleLanguageCommand(action);
                    }
                    // 处理BoundViz命令
                    else if (action.startsWith("boundviz")) {
                        handleBoundVizCommand(action);
                    }
                    // 处理模组开关命令
                    else if (action.equals("on") || action.equals("off")) {
                        areahint.command.ModToggleCommand.handleToggleCommand(action);
                    }
                    // 处理Teleport命令
                    else if (action.startsWith("tcp") || action.startsWith("udp") || action.startsWith("settp")) {
                        areahint.teleport.TeleportClientManager.getInstance().handleClientCommand(action);
                    }
                    // 处理SetHigh命令
                    else if (action.startsWith("sethigh")) {
                        handleSetHighCommand(action);
                    }
                }
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理客户端命令时出错: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理EasyAdd命令
     * @param action 命令动作
     */
    private static void handleEasyAddCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理EasyAdd命令: " + action);
            areahint.easyadd.EasyAddManager manager = areahint.easyadd.EasyAddManager.getInstance();
            
            if (action.equals("easyadd_start")) {
                AreashintClient.LOGGER.info("执行easyadd_start");
                manager.startEasyAdd();
            } else if (action.equals("expandarea_start")) {
                AreashintClient.LOGGER.info("执行expandarea_start");
                System.out.println("DEBUG: 开始执行 expandarea_start");
                areahint.expandarea.ExpandAreaManager.getInstance().startExpandArea();
                System.out.println("DEBUG: expandarea_start 执行完成");
            } else if (action.startsWith("expandarea_select:")) {
                String areaName = action.substring("expandarea_select:".length());
                AreashintClient.LOGGER.info("执行expandarea_select: " + areaName);
                areahint.expandarea.ExpandAreaManager.getInstance().selectAreaByName(areaName);
            } else if (action.equals("expandarea_continue")) {
                AreashintClient.LOGGER.info("执行expandarea_continue");
                areahint.expandarea.ExpandAreaManager.getInstance().continueRecording();
            } else if (action.equals("expandarea_save")) {
                AreashintClient.LOGGER.info("执行expandarea_save");
                areahint.expandarea.ExpandAreaManager.getInstance().finishAndSave();
            } else if (action.equals("expandarea_cancel")) {
                AreashintClient.LOGGER.info("执行expandarea_cancel");
                areahint.expandarea.ExpandAreaManager.getInstance().cancel();
            } else if (action.equals("shrinkarea_start")) {
                AreashintClient.LOGGER.info("执行shrinkarea_start");
                areahint.shrinkarea.ShrinkAreaManager.getInstance().start();
            } else if (action.startsWith("shrinkarea_select:")) {
                String areaName = action.substring("shrinkarea_select:".length());
                AreashintClient.LOGGER.info("执行shrinkarea_select: " + areaName);
                areahint.shrinkarea.ShrinkAreaManager.getInstance().selectAreaByName(areaName);
            } else if (action.equals("shrinkarea_continue")) {
                AreashintClient.LOGGER.info("执行shrinkarea_continue");
                areahint.shrinkarea.ShrinkAreaManager.getInstance().continueRecording();
            } else if (action.equals("shrinkarea_save")) {
                AreashintClient.LOGGER.info("执行shrinkarea_save");
                areahint.shrinkarea.ShrinkAreaManager.getInstance().finishAndSave();
            } else if (action.equals("shrinkarea_cancel")) {
                AreashintClient.LOGGER.info("执行shrinkarea_cancel");
                areahint.shrinkarea.ShrinkAreaManager.getInstance().stop();
            } else if (action.equals("addhint_start")) {
                areahint.addhint.AddHintManager.getInstance().start();
            } else if (action.startsWith("addhint_select:")) {
                String areaName = action.substring("addhint_select:".length());
                areahint.addhint.AddHintManager.getInstance().selectArea(areaName);
            } else if (action.equals("addhint_submit")) {
                areahint.addhint.AddHintManager.getInstance().submit();
            } else if (action.equals("addhint_cancel")) {
                areahint.addhint.AddHintManager.getInstance().cancel();
            } else if (action.equals("deletehint_start")) {
                areahint.deletehint.DeleteHintManager.getInstance().start();
            } else if (action.startsWith("deletehint_select:")) {
                String areaName = action.substring("deletehint_select:".length());
                areahint.deletehint.DeleteHintManager.getInstance().selectArea(areaName);
            } else if (action.startsWith("deletehint_toggle:")) {
                int index = Integer.parseInt(action.substring("deletehint_toggle:".length()));
                areahint.deletehint.DeleteHintManager.getInstance().toggleVertex(index);
            } else if (action.equals("deletehint_submit")) {
                areahint.deletehint.DeleteHintManager.getInstance().submit();
            } else if (action.equals("deletehint_cancel")) {
                areahint.deletehint.DeleteHintManager.getInstance().cancel();
            } else if (action.equals("easyadd_cancel")) {
                AreashintClient.LOGGER.info("执行easyadd_cancel");
                manager.cancelEasyAdd();
            } else if (action.startsWith("easyadd_level:")) {
                String levelStr = action.substring("easyadd_level:".length());
                int level = Integer.parseInt(levelStr);
                AreashintClient.LOGGER.info("执行easyadd_level: " + level);
                manager.handleLevelInput(level);
            } else if (action.startsWith("easyadd_base:")) {
                String baseName = action.substring("easyadd_base:".length());
                AreashintClient.LOGGER.info("执行easyadd_base: " + baseName);
                manager.handleBaseSelection(baseName);
            } else if (action.equals("easyadd_continue")) {
                AreashintClient.LOGGER.info("执行easyadd_continue");
                // 继续记录的逻辑在按键处理中，这里不需要做特殊处理
            } else if (action.equals("easyadd_finish")) {
                AreashintClient.LOGGER.info("执行easyadd_finish");
                manager.finishPointRecording();
                    } else if (action.equals("easyadd_save")) {
            AreashintClient.LOGGER.info("执行easyadd_save");
            manager.confirmSave();
        } else if (action.equals("easyadd_altitude_auto")) {
            AreashintClient.LOGGER.info("执行easyadd_altitude_auto");
            areahint.easyadd.EasyAddAltitudeManager.handleAltitudeTypeSelection(
                areahint.easyadd.EasyAddAltitudeManager.AltitudeType.AUTOMATIC);
        } else if (action.equals("easyadd_altitude_custom")) {
            AreashintClient.LOGGER.info("执行easyadd_altitude_custom");
            areahint.easyadd.EasyAddAltitudeManager.handleAltitudeTypeSelection(
                areahint.easyadd.EasyAddAltitudeManager.AltitudeType.CUSTOM);
        } else if (action.equals("easyadd_altitude_unlimited")) {
            AreashintClient.LOGGER.info("执行easyadd_altitude_unlimited");
            areahint.easyadd.EasyAddAltitudeManager.handleAltitudeTypeSelection(
                areahint.easyadd.EasyAddAltitudeManager.AltitudeType.UNLIMITED);
        } else if (action.startsWith("easyadd_color:")) {
            String colorHex = action.substring("easyadd_color:".length());
            try {
                AreashintClient.LOGGER.info("执行easyadd_color: " + colorHex);
                manager.handleColorSelection(colorHex);
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理颜色选择时出错: " + e.getMessage());
            }
        } else {
            AreashintClient.LOGGER.warn("未知的EasyAdd命令: " + action);
        }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理EasyAdd命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理Recolor命令
     * @param action 命令动作
     */
    private static void handleRecolorCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Recolor命令: " + action);
            areahint.recolor.RecolorManager manager = areahint.recolor.RecolorManager.getInstance();

            if (action.startsWith("recolor_select:")) {
                String areaName = action.substring("recolor_select:".length());
                AreashintClient.LOGGER.info("执行recolor_select: " + areaName);
                manager.handleAreaSelection(areaName);
            } else if (action.startsWith("recolor_color:")) {
                String colorValue = action.substring("recolor_color:".length());
                AreashintClient.LOGGER.info("执行recolor_color: " + colorValue);
                manager.handleColorSelection(colorValue);
            } else if (action.equals("recolor_confirm")) {
                AreashintClient.LOGGER.info("执行recolor_confirm");
                manager.confirmChange();
            } else if (action.equals("recolor_cancel")) {
                AreashintClient.LOGGER.info("执行recolor_cancel");
                manager.cancelRecolor();
            } else {
                AreashintClient.LOGGER.warn("未知的Recolor命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Recolor命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理DimName命令
     */
    private static void handleDimNameCommand(String action) {
        try {
            areahint.dimensional.DimensionalNameUIManager mgr = areahint.dimensional.DimensionalNameUIManager.getInstance();
            if (action.equals("dimname_start")) {
                mgr.startDimName();
            } else if (action.startsWith("dimname_select:")) {
                mgr.handleDimNameSelect(action.substring("dimname_select:".length()));
            } else if (action.startsWith("dimname_name:")) {
                mgr.handleDimNameInput(action.substring("dimname_name:".length()));
            } else if (action.equals("dimname_confirm")) {
                mgr.confirmDimName();
            } else if (action.equals("dimname_cancel")) {
                mgr.cancel();
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理DimName命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理DimColor命令
     */
    private static void handleDimColorCommand(String action) {
        try {
            areahint.dimensional.DimensionalNameUIManager mgr = areahint.dimensional.DimensionalNameUIManager.getInstance();
            if (action.equals("dimcolor_start")) {
                mgr.startDimColor();
            } else if (action.startsWith("dimcolor_select:")) {
                mgr.handleDimColorSelect(action.substring("dimcolor_select:".length()));
            } else if (action.startsWith("dimcolor_color:")) {
                mgr.handleDimColorInput(action.substring("dimcolor_color:".length()));
            } else if (action.equals("dimcolor_confirm")) {
                mgr.confirmDimColor();
            } else if (action.equals("dimcolor_cancel")) {
                mgr.cancel();
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理DimColor命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理Rename命令
     * @param action 命令动作
     */
    private static void handleRenameCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Rename命令: " + action);
            areahint.rename.RenameManager manager = areahint.rename.RenameManager.getInstance();

            if (action.startsWith("rename_select:")) {
                String areaName = action.substring("rename_select:".length());
                AreashintClient.LOGGER.info("执行rename_select: " + areaName);
                manager.handleAreaSelection(areaName);
            } else if (action.equals("rename_confirm")) {
                AreashintClient.LOGGER.info("执行rename_confirm");
                manager.confirmRename();
            } else if (action.equals("rename_cancel")) {
                AreashintClient.LOGGER.info("执行rename_cancel");
                manager.cancelRename();
            } else {
                AreashintClient.LOGGER.warn("未知的Rename命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Rename命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理TitleStyle命令
     * @param action 命令动作
     */
    private static void handleTitleStyleCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理TitleStyle命令: " + action);
            areahint.titlestyle.TitleStyleManager manager = areahint.titlestyle.TitleStyleManager.getInstance();

            if (action.equals("titlestyle_start")) {
                AreashintClient.LOGGER.info("执行titlestyle_start");
                manager.startTitleStyleSelection();
            } else if (action.startsWith("titlestyle_select:")) {
                String style = action.substring("titlestyle_select:".length());
                AreashintClient.LOGGER.info("执行titlestyle_select: " + style);
                manager.handleStyleSelection(style);
            } else if (action.equals("titlestyle_cancel")) {
                AreashintClient.LOGGER.info("执行titlestyle_cancel");
                manager.cancelTitleStyle();
            } else {
                AreashintClient.LOGGER.warn("未知的TitleStyle命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理TitleStyle命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理TitleSize命令
     * @param action 命令动作
     */
    private static void handleTitleSizeCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理TitleSize命令: " + action);
            areahint.titlesize.TitleSizeManager manager = areahint.titlesize.TitleSizeManager.getInstance();

            if (action.equals("titlesize_start")) {
                AreashintClient.LOGGER.info("执行titlesize_start");
                manager.startTitleSizeSelection();
            } else if (action.startsWith("titlesize_select:")) {
                String size = action.substring("titlesize_select:".length());
                AreashintClient.LOGGER.info("执行titlesize_select: " + size);
                manager.handleSizeSelection(size);
            } else if (action.equals("titlesize_cancel")) {
                AreashintClient.LOGGER.info("执行titlesize_cancel");
                manager.cancelTitleSize();
            } else {
                AreashintClient.LOGGER.warn("未知的TitleSize命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理TitleSize命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理副字幕命令。
     * 服务端命令只负责权限和目标玩家，具体交互状态由客户端 SubtitleManager 保存。
     */
    private static void handleSubtitleCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Subtitle命令: " + action);
            areahint.subtitle.SubtitleManager manager = areahint.subtitle.SubtitleManager.getInstance();

            if (action.startsWith("addsubtitle_select:")) {
                manager.handleAddAreaSelection(action.substring("addsubtitle_select:".length()));
            } else if (action.startsWith("addsubtitle_color:")) {
                manager.handleAddColorSelection(action.substring("addsubtitle_color:".length()));
            } else if (action.equals("addsubtitle_confirm")) {
                manager.confirmAddSubtitle();
            } else if (action.equals("addsubtitle_cancel")) {
                manager.cancel();
            } else if (action.startsWith("deletesubtitle_select:")) {
                manager.handleDeleteAreaSelection(action.substring("deletesubtitle_select:".length()));
            } else if (action.equals("deletesubtitle_confirm")) {
                manager.confirmDeleteSubtitle();
            } else if (action.equals("deletesubtitle_cancel")) {
                manager.cancel();
            } else if (action.startsWith("replacesubtitlecolor_select:")) {
                manager.handleColorAreaSelection(action.substring("replacesubtitlecolor_select:".length()));
            } else if (action.startsWith("replacesubtitlecolor_color:")) {
                manager.handleColorSelection(action.substring("replacesubtitlecolor_color:".length()));
            } else if (action.equals("replacesubtitlecolor_confirm")) {
                manager.confirmReplaceSubtitleColor();
            } else if (action.equals("replacesubtitlecolor_cancel")) {
                manager.cancel();
            } else if (action.equals("replacesubtitlesize_start")) {
                manager.startSubtitleSizeSelection();
            } else if (action.startsWith("replacesubtitlesize_select:")) {
                manager.handleSubtitleSizeSelection(action.substring("replacesubtitlesize_select:".length()));
            } else if (action.equals("replacesubtitlesize_cancel")) {
                manager.cancel();
            } else {
                AreashintClient.LOGGER.warn("未知的Subtitle命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Subtitle命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理Delete命令
     * @param action 命令动作
     */
    private static void handleDivideAreaCommand(String action) {
        try {
            areahint.dividearea.DivideAreaManager mgr = areahint.dividearea.DivideAreaManager.getInstance();
            if (action.equals("dividearea_start")) {
                mgr.start();
            } else if (action.startsWith("dividearea_select:")) {
                mgr.selectAreaByName(action.substring("dividearea_select:".length()));
            } else if (action.equals("dividearea_continue")) {
                mgr.continueRecording();
            } else if (action.equals("dividearea_save")) {
                mgr.finishAndSave();
            } else if (action.startsWith("dividearea_name:")) {
                mgr.handleNameInput(action.substring("dividearea_name:".length()));
            } else if (action.startsWith("dividearea_level:")) {
                mgr.handleLevelInput(Integer.parseInt(action.substring("dividearea_level:".length())));
            } else if (action.startsWith("dividearea_base:")) {
                mgr.handleBaseInput(action.substring("dividearea_base:".length()));
            } else if (action.startsWith("dividearea_color:")) {
                mgr.handleColorInput(action.substring("dividearea_color:".length()));
            } else if (action.equals("dividearea_cancel")) {
                mgr.cancel();
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理DivideArea命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理SetHigh命令
     */
    private static void handleSetHighCommand(String action) {
        try {
            if (action.equals("sethigh_start")) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.area.altitude.modify")), false);
                }
            } else if (action.startsWith("sethigh_custom:")) {
                String areaName = action.substring("sethigh_custom:".length());
                areahint.command.SetHighClientCommand.startCustomHeightInput(areaName);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理SetHigh命令时出错: " + e.getMessage(), e);
        }
    }

    private static void handleSignatureCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Signature命令: " + action);
            areahint.signature.SignatureManager manager = areahint.signature.SignatureManager.getInstance();

            if (action.equals("addsignature_start")) {
                manager.startAdd();
            } else if (action.startsWith("addsignature_select:")) {
                manager.selectArea(action.substring("addsignature_select:".length()));
            } else if (action.startsWith("addsignature_name:")) {
                manager.setPlayerName(action.substring("addsignature_name:".length()));
            } else if (action.equals("addsignature_confirm")) {
                manager.confirm();
            } else if (action.equals("addsignature_cancel")) {
                manager.cancel();
            } else if (action.equals("deletesignature_start")) {
                manager.startDelete();
            } else if (action.startsWith("deletesignature_select:")) {
                manager.selectArea(action.substring("deletesignature_select:".length()));
            } else if (action.startsWith("deletesignature_name:")) {
                manager.setPlayerName(action.substring("deletesignature_name:".length()));
            } else if (action.equals("deletesignature_confirm")) {
                manager.confirm();
            } else if (action.equals("deletesignature_confirm2")) {
                manager.confirmDeleteFinal();
            } else if (action.equals("deletesignature_cancel")) {
                manager.cancel();
            } else {
                AreashintClient.LOGGER.warn("未知的Signature命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Signature命令时出错: " + e.getMessage(), e);
        }
    }

    private static void handleDeleteCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Delete命令: " + action);
            areahint.delete.DeleteManager manager = areahint.delete.DeleteManager.getInstance();

            if (action.equals("delete_start")) {
                AreashintClient.LOGGER.info("执行delete_start");
                manager.startDelete();
            } else if (action.startsWith("delete_select:")) {
                String areaName = action.substring("delete_select:".length());
                AreashintClient.LOGGER.info("执行delete_select: " + areaName);
                manager.handleAreaSelection(areaName);
            } else if (action.equals("delete_confirm")) {
                AreashintClient.LOGGER.info("执行delete_confirm");
                manager.confirmDelete();
            } else if (action.equals("delete_cancel")) {
                AreashintClient.LOGGER.info("执行delete_cancel");
                manager.cancelDelete();
            } else {
                AreashintClient.LOGGER.warn("未知的Delete命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Delete命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理ReplaceButton命令
     * @param action 命令动作
     */
    private static void handleReplaceButtonCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理ReplaceButton命令: " + action);
            areahint.replacebutton.ReplaceButtonManager manager = areahint.replacebutton.ReplaceButtonManager.getInstance();

            if (action.equals("replacebutton_start")) {
                AreashintClient.LOGGER.info("执行replacebutton_start");
                manager.startReplaceButton();
            } else if (action.equals("replacebutton_confirm")) {
                AreashintClient.LOGGER.info("执行replacebutton_confirm");
                manager.confirmNewKey();
            } else if (action.equals("replacebutton_cancel")) {
                AreashintClient.LOGGER.info("执行replacebutton_cancel");
                manager.cancel();
            } else {
                AreashintClient.LOGGER.warn("未知的ReplaceButton命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理ReplaceButton命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理Language命令
     * @param action 命令动作
     */
    private static void handleLanguageCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理Language命令: " + action);
            areahint.language.LanguageManager manager = areahint.language.LanguageManager.getInstance();

            if (action.equals("language_start")) {
                manager.startLanguageSelection();
            } else if (action.startsWith("language_select:")) {
                String langCode = action.substring("language_select:".length());
                manager.handleLanguageSelection(langCode);
            } else if (action.equals("language_cancel")) {
                manager.cancelLanguageSelection();
            } else {
                AreashintClient.LOGGER.warn("未知的Language命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理Language命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 处理BoundViz命令
     * @param action 命令动作
     */
    private static void handleBoundVizCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理BoundViz命令: " + action);
            areahint.boundviz.BoundVizManager manager = areahint.boundviz.BoundVizManager.getInstance();

            if (action.equals("boundviz_toggle")) {
                AreashintClient.LOGGER.info("执行boundviz_toggle");
                manager.toggle();

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    if (manager.isEnabled()) {
                        client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.boundary.visualization_2")), false);
                    } else {
                        client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.boundary.visualization")), false);
                    }
                }
            } else {
                AreashintClient.LOGGER.warn("未知的BoundViz命令: " + action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理BoundViz命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 显示当前频率信息
     * @param client Minecraft客户端实例
     */
    private static void displayFrequencyInfo(MinecraftClient client) {
        if (client.player != null) {
            double frequency = ClientConfig.getFrequency();
            client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.general_50") + areahint.data.ConfigData.formatFrequency(frequency) + "§a Hz"));
        }
    }
    
    /**
     * 处理提示文字渲染方式切换命令。
     */
    private static void handleHintRenderCommand(MinecraftClient client, String action) {
        String[] renderParts = action.split(" ");
        if (renderParts.length < 2) {
            return;
        }

        String targetMode = ConfigData.normalizeRenderMode(renderParts[1]);
        String currentMode = ClientConfig.getHintRender();

        if (currentMode.equals(targetMode)) {
            sendHintRenderMessage(client, "§a当前提示文字渲染方式为: §6" + currentMode + "§a，无需重复设置");
            return;
        }

        boolean currentIsVulkan = "Vulkan".equals(currentMode);
        boolean targetIsVulkan = "Vulkan".equals(targetMode);

        if (targetIsVulkan && !VulkanModCompat.isLoaded()) {
            sendHintRenderMessage(client, "§c未检测到 VulkanMod，无法切换到 §6Vulkan§c 渲染");
            return;
        }

        if ((currentIsVulkan && !targetIsVulkan) || (!currentIsVulkan && targetIsVulkan)) {
            sendHintRenderMessage(client, "§cVulkan 与 OpenGL/CPU 之间禁止直接切换");
            return;
        }

        ClientConfig.setHintRender(targetMode);
        AreashintClient.reload();
        sendHintRenderMessage(client, "§a提示文字渲染方式已设置为: §6" + targetMode);
    }

    private static void sendHintRenderMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.of(message), false);
        }
    }

    /**
     * 显示当前渲染方式信息
     * @param client Minecraft客户端实例
     */
    private static void displayHintRenderInfo(MinecraftClient client) {
        if (client.player != null) {
            String renderMode = ClientConfig.getHintRender();
            client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.general_49") + renderMode));
        }
    }
    
    /**
     * 显示当前域名标题样式信息
     * @param client Minecraft客户端实例
     */
    private static void displayTitleStyleInfo(MinecraftClient client) {
        if (client.player != null) {
            String style = ClientConfig.getTitleStyle();
            client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.general_48") + style));
        }
    }
    
    /**
     * 处理接收到的调试命令
     * @param client Minecraft客户端实例
     * @param handler 网络处理器
     * @param buf 数据包缓冲区
     * @param responseSender 响应发送器
     */
    private static void handleDebugCommand(MinecraftClient client, 
                                          ClientPlayNetworkHandler handler,
                                          PacketByteBuf buf, 
                                          PacketSender responseSender) {
        // 读取调试状态
        boolean enabled = buf.readBoolean();
        
        // 确保在主线程中处理
        client.execute(() -> {
            try {
                AreashintClient.LOGGER.info("接收到服务端调试命令: " + (enabled ? "启用" : "禁用"));
                
                // 设置调试状态
                if (enabled) {
                    areahint.debug.ClientDebugManager.enableDebug();
                } else {
                    areahint.debug.ClientDebugManager.disableDebug();
                }
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理调试命令时出错: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理recolor响应
     */
    private static void handleRecolorResponse(MinecraftClient client, ClientPlayNetworkHandler handler, 
                                            PacketByteBuf buf, PacketSender responseSender) {
        try {
            String action = buf.readString();
            
            if ("recolor_list".equals(action)) {
                // 处理域名列表响应
                String dimension = buf.readString();
                int count = buf.readInt();
                
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.message.area.list")), false);
                        
                        for (int i = 0; i < count; i++) {
                            try {
                                String areaName = buf.readString();
                                String currentColor = buf.readString();
                                int level = buf.readInt();
                                String baseName = buf.readString();
                                
                                client.player.sendMessage(net.minecraft.text.Text.of(
                                    String.format(I18nManager.translate("message.message.color.level"),
                                        i + 1, areaName, level, currentColor)
                                ), false);
                            } catch (Exception e) {
                                AreashintClient.LOGGER.error("读取域名信息时出错", e);
                            }
                        }
                        
                        client.player.sendMessage(net.minecraft.text.Text.of(
                            I18nManager.translate("message.message.color_2")
                        ), false);
                    }
                });
                
            } else if ("recolor_interactive".equals(action)) {
                // 处理交互式recolor界面
                String dimension = buf.readString();
                int count = buf.readInt();

                // 读取域名列表
                java.util.List<areahint.data.AreaData> areas = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) {
                    try {
                        String areaName = buf.readString();
                        String currentColor = buf.readString();
                        int level = buf.readInt();
                        String baseName = buf.readString();

                        // 创建简化的AreaData对象（只包含必要信息）
                        areahint.data.AreaData area = new areahint.data.AreaData();
                        area.setName(areaName);
                        area.setColor(currentColor);
                        area.setLevel(level);
                        area.setBaseName(baseName.isEmpty() ? null : baseName);

                        areas.add(area);
                    } catch (Exception e) {
                        AreashintClient.LOGGER.error("读取域名信息时出错", e);
                    }
                }

                client.execute(() -> {
                    if (client.player != null) {
                        // 启动交互式Recolor流程
                        areahint.recolor.RecolorManager.getInstance().startRecolor(areas, dimension);
                    }
                });
            } else if ("recolor_response".equals(action)) {
                // 处理修改颜色响应
                boolean success = buf.readBoolean();
                String message = buf.readString();
                
                client.execute(() -> {
                    if (client.player != null) {
                        if (success) {
                            client.player.sendMessage(net.minecraft.text.Text.of("§a" + message), false);
                        } else {
                            client.player.sendMessage(net.minecraft.text.Text.of("§c" + message), false);
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理recolor响应时出错: " + e.getMessage());
        }
    }
    
    /**
     * 处理rename响应
     * 注意：此方法已被 RenameNetworking 接管，保留此方法仅为兼容性
     */
    private static void handleRenameResponse(MinecraftClient client, ClientPlayNetworkHandler handler,
                                           PacketByteBuf buf, PacketSender responseSender) {
        // 此方法的功能已移至 areahint.rename.RenameNetworking
        // 保留此方法仅为避免编译错误
        AreashintClient.LOGGER.warn("handleRenameResponse 被调用，但功能已移至 RenameNetworking");
    }

    /**
     * 显示交互式recolor界面
     */
    private static void showInteractiveRecolorScreen(int count, PacketByteBuf buf) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.title.area.color.modify")), false);
        client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.prompt.area.color.modify")), false);
        client.player.sendMessage(net.minecraft.text.Text.of(""), false);
        
        for (int i = 0; i < count; i++) {
            try {
                String areaName = buf.readString();
                String currentColor = buf.readString();
                int level = buf.readInt();
                String baseName = buf.readString();
                
                // 创建域名选择按钮
                net.minecraft.text.MutableText areaButton = net.minecraft.text.Text.literal(
                    String.format(I18nManager.translate("message.button.color.level"), areaName, level, currentColor)
                ).setStyle(net.minecraft.text.Style.EMPTY
                    .withClickEvent(new net.minecraft.text.ClickEvent(
                        net.minecraft.text.ClickEvent.Action.RUN_COMMAND, 
                        "/areahint recolor " + areaName))
                    .withHoverEvent(new net.minecraft.text.HoverEvent(
                        net.minecraft.text.HoverEvent.Action.SHOW_TEXT, 
                        net.minecraft.text.Text.of(I18nManager.translate("addhint.prompt.general") + areaName + I18nManager.translate("message.message.color.modify")))));
                
                client.player.sendMessage(areaButton, false);
                
            } catch (Exception e) {
                AreashintClient.LOGGER.error("读取域名信息时出错", e);
            }
        }
        
        client.player.sendMessage(net.minecraft.text.Text.of(""), false);
        client.player.sendMessage(net.minecraft.text.Text.of(
            I18nManager.translate("message.message.color_2")
        ), false);
        client.player.sendMessage(net.minecraft.text.Text.of(
            I18nManager.translate("message.message.general_45")
        ), false);
    }
    
    /**
     * 处理SetHigh域名列表
     */
    private static void handleSetHighAreaList(MinecraftClient client, 
                                            ClientPlayNetworkHandler handler,
                                            PacketByteBuf buf, 
                                            PacketSender responseSender) {
        client.execute(() -> {
            try {
                // 读取域名列表数据
                List<String> areaNames = new ArrayList<>();
                List<areahint.command.SetHighVisualController.AreaEntry> visualEntries = new ArrayList<>();
                
                // 读取服务端发送的数据格式
                String commandType = buf.readString(); // "sethigh_area_list"
                String dimensionType = buf.readString(); // 维度类型
                int areaCount = buf.readInt(); // 域名数量
                
                AreashintClient.LOGGER.info("接收到SetHigh域名列表: 命令类型={}, 维度={}, 域名数量={}", 
                    commandType, dimensionType, areaCount);
                
                // 读取每个域名的信息
                for (int i = 0; i < areaCount; i++) {
                    String areaName = buf.readString();
                    areaNames.add(areaName);
                    
                    boolean hasAltitude = buf.readBoolean();
                    Double maxHeight = null;
                    Double minHeight = null;
                    
                    if (hasAltitude) {
                        boolean hasMax = buf.readBoolean();
                        maxHeight = hasMax ? buf.readDouble() : null;
                        
                        boolean hasMin = buf.readBoolean();
                        minHeight = hasMin ? buf.readDouble() : null;
                    }
                    visualEntries.add(new areahint.command.SetHighVisualController.AreaEntry(
                        areaName, hasAltitude, maxHeight, minHeight));
                }
                
                AreashintClient.LOGGER.info("成功解析域名列表: {}", areaNames);
                
                // 图形入口只接管界面流程，普通 /areahint sethigh 继续使用原聊天按钮。
                if (areahint.command.SetHighVisualController.consumeVisualStartRequest()) {
                    areahint.command.SetHighVisualController.showAreaSelection(visualEntries);
                } else {
                    areahint.command.SetHighClientCommand.handleAreaList(areaNames, dimensionType);
                }
                
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理SetHigh域名列表时出错: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 处理SetHigh域名选择
     */
    private static void handleSetHighAreaSelection(MinecraftClient client, 
                                                 ClientPlayNetworkHandler handler,
                                                 PacketByteBuf buf, 
                                                 PacketSender responseSender) {
        client.execute(() -> {
            try {
                // 读取指定域名的数据
                String areaName = buf.readString();
                
                // 读取当前高度信息
                boolean hasAltitude = buf.readBoolean();
                Double maxHeight = null;
                Double minHeight = null;
                
                if (hasAltitude) {
                    boolean hasMax = buf.readBoolean();
                    if (hasMax) {
                        maxHeight = buf.readDouble();
                    }
                    boolean hasMin = buf.readBoolean();
                    if (hasMin) {
                        minHeight = buf.readDouble();
                    }
                }
                
                AreashintClient.LOGGER.info("接收到SetHigh域名选择: 域名={}, 有高度限制={}, 最大高度={}, 最小高度={}", 
                    areaName, hasAltitude, maxHeight, minHeight);
                
                // 图形流程中收到指定域名数据时直接进入 Screen 模式选择。
                if (areahint.command.SetHighVisualController.isVisualFlowActive()) {
                    areahint.command.SetHighVisualController.showModeSelection(
                        new areahint.command.SetHighVisualController.AreaEntry(areaName, hasAltitude, maxHeight, minHeight));
                } else {
                    areahint.command.SetHighClientCommand.handleAreaSelection(areaName, hasAltitude, maxHeight, minHeight);
                }
                
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理SetHigh域名选择时出错: " + e.getMessage(), e);
            }
        });
    }
    
    private static void handleTeleportResponse(MinecraftClient client,
                                               ClientPlayNetworkHandler handler,
                                               PacketByteBuf buf,
                                               PacketSender responseSender) {
        boolean success = buf.readBoolean();
        String message = buf.readString();
        client.execute(() -> areahint.teleport.TeleportClientManager.getInstance().handleServerResponse(success, message));
    }

    /**
     * 处理SetHigh响应
     */
    private static void handleSetHighResponse(MinecraftClient client,
                                            ClientPlayNetworkHandler handler,
                                            PacketByteBuf buf, 
                                            PacketSender responseSender) {
        client.execute(() -> {
            try {
                boolean success = buf.readBoolean();
                net.minecraft.text.MutableText message = TranslatableMessage.read(buf);
                areahint.command.SetHighClientCommand.handleServerResponse(success, message);
            } catch (Exception e) {
                AreashintClient.LOGGER.error("处理SetHigh响应时出错: " + e.getMessage());
            }
        });
    }
    
    public static void sendTeleportRequest(String mode, String areaName, String teleportFormat) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(mode);
            buf.writeString(areaName);
            buf.writeString(teleportFormat);
            ClientPlayNetworking.send(Packets.C2S_TELEPORT_REQUEST, buf);
        } catch (Exception e) {
            AreashintClient.LOGGER.error("发送传送请求时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 发送SetHigh高度设置请求到服务器
     * @param areaName 域名名称
     * @param hasAltitude 是否有高度限制
     * @param maxHeight 最大高度
     * @param minHeight 最小高度
     */
    public static void sendSetHighRequest(String areaName, boolean hasAltitude, 
                                        Double maxHeight, Double minHeight) {
        try {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(areaName);
            buf.writeBoolean(hasAltitude);
            
            if (hasAltitude) {
                buf.writeBoolean(maxHeight != null);
                if (maxHeight != null) {
                    buf.writeDouble(maxHeight);
                }
                buf.writeBoolean(minHeight != null);
                if (minHeight != null) {
                    buf.writeDouble(minHeight);
                }
            }
            
            ClientPlayNetworking.send(Packets.C2S_SETHIGH_REQUEST, buf);
            
            AreashintClient.LOGGER.info("发送SetHigh请求: 域名={}, 有高度限制={}, 最大高度={}, 最小高度={}", 
                areaName, hasAltitude, maxHeight, minHeight);
                
        } catch (Exception e) {
            AreashintClient.LOGGER.error("发送SetHigh请求时发生错误: " + e.getMessage(), e);
        }
    }
}
