package areahint.command;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.data.ConfigData;
import areahint.file.FileManager;
import areahint.dimensional.DimensionalNameManager;
import areahint.file.JsonHelper;
import areahint.i18n.ServerI18nManager;
import areahint.network.Packets;
import areahint.network.ServerNetworking;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.subtitle.SubtitleCommand;
import areahint.util.AreaPermissionUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * 统一命令处理类
 * 实现服务端和客户端共享的命令功能
 */
public class ServerCommands {
    /**
     * 注册命令
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register(ServerCommands::registerCommands);
    }
    
    /**
     * 注册所有命令
     * @param dispatcher 命令分发器
     * @param registryAccess 命令注册访问器
     * @param environment 环境
     */
    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, 
                                        CommandRegistryAccess registryAccess, 
                                        CommandManager.RegistrationEnvironment environment) {
        // 注册调试命令
        DebugCommand.register(dispatcher, registryAccess, environment);
        
        // 注册check命令
        CheckCommand.register(dispatcher, registryAccess, environment);
        
        dispatcher.register(literal("areahint")
            // help 命令
            .then(literal("help")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.HELP, 0))
                .executes(ServerCommands::executeHelp))

            // reload 命令
            .then(literal("reload")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.RELOAD, 0))
                .executes(ServerCommands::executeReload))

            // on/off 命令（客户端本地开关，权限等级0，只影响执行命令的玩家）
            .then(literal("on")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ON, 0))
                .executes(context -> executeModToggle(context, "on")))
            .then(literal("off")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.OFF, 0))
                .executes(context -> executeModToggle(context, "off")))
	            
            // dimensionalityname 命令 (交互式维度域名管理)
            .then(literal("dimensionalityname")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DIMENSIONALITY_NAME, 2))
                .executes(DimensionalNameCommands::executeStart)
                .then(literal("select")
                    .then(argument("dimension", StringArgumentType.greedyString())
                        .executes(context -> DimensionalNameCommands.executeSelect(context,
                            StringArgumentType.getString(context, "dimension")))))
                .then(literal("name")
                    .then(argument("newName", StringArgumentType.greedyString())
                        .executes(context -> DimensionalNameCommands.executeName(context,
                            StringArgumentType.getString(context, "newName")))))
                .then(literal("confirm")
                    .executes(DimensionalNameCommands::executeConfirm))
                .then(literal("cancel")
                    .executes(DimensionalNameCommands::executeCancel)))

            // dimensionalitycolor 命令 (交互式维度域名颜色管理)
            .then(literal("dimensionalitycolor")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DIMENSIONALITY_COLOR, 2))
                .executes(DimensionalNameCommands::executeColorStart)
                .then(literal("select")
                    .then(argument("dimension", StringArgumentType.greedyString())
                        .executes(context -> DimensionalNameCommands.executeColorSelect(context,
                            StringArgumentType.getString(context, "dimension")))))
                .then(literal("color")
                    .then(argument("colorValue", StringArgumentType.greedyString())
                        .executes(context -> DimensionalNameCommands.executeColorColor(context,
                            StringArgumentType.getString(context, "colorValue")))))
                .then(literal("confirm")
                    .executes(DimensionalNameCommands::executeColorConfirm))
                .then(literal("cancel")
                    .executes(DimensionalNameCommands::executeColorCancel)))

            // firstdimname 命令默认对玩家开放，但允许 LuckPerms 通过独立节点显式拒绝。
            .then(literal("firstdimname")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.FIRST_DIM_NAME, 0))
                .then(argument("name", StringArgumentType.greedyString())
                    .executes(context -> DimensionalNameCommands.executeFirstDimName(context,
                        StringArgumentType.getString(context, "name")))))
            .then(literal("firstdimname_skip")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.FIRST_DIM_NAME_SKIP, 0))
                .executes(DimensionalNameCommands::executeFirstDimNameSkip))

            // add 命令 (仅服务端)
            .then(literal("add")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD, 2))
                .then(argument("json", StringArgumentType.greedyString())
                    .executes(context -> executeAdd(context, StringArgumentType.getString(context, "json"))))
            )
            
            // delete 命令 (交互式删除)
            .then(literal("delete")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETE, 0))
                .executes(ServerCommands::executeDeleteStart)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .suggests(DELETABLE_AREA_SUGGESTIONS)
                        .executes(context -> executeDeleteSelect(context, StringArgumentType.getString(context, "areaName")))))
                .then(literal("confirm")
                    .executes(ServerCommands::executeDeleteConfirm))
                .then(literal("cancel")
                    .executes(ServerCommands::executeDeleteCancel))
            )
            
            // adddescription 命令 (交互式添加域名描述)
            .then(literal("adddescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD_DESCRIPTION, 0))
                .executes(ServerCommands::executeAddDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "adddescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddescription_select:",
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("text")
                    .executes(context -> executeDescriptionAction(context, "adddescription_text:", ""))
                    .then(argument("description", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddescription_text:",
                            StringArgumentType.getString(context, "description")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "adddescription_confirm", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "adddescription_cancel", null))))

            // replacedescription 命令 (交互式替换域名描述，保持原 adddescription 的空书本写入行为)
            .then(literal("replacedescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD_DESCRIPTION, 0))
                .executes(ServerCommands::executeReplaceDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "replacedescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedescription_select:",
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("text")
                    .executes(context -> executeDescriptionAction(context, "replacedescription_text:", ""))
                    .then(argument("description", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedescription_text:",
                            StringArgumentType.getString(context, "description")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "replacedescription_confirm", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "replacedescription_cancel", null))))

            // deletedescription 命令 (交互式删除域名描述)
            .then(literal("deletedescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETE_DESCRIPTION, 0))
                .executes(ServerCommands::executeDeleteDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "deletedescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "deletedescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "deletedescription_select:",
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "deletedescription_confirm", null)))
                .then(literal("confirm2")
                    .executes(context -> executeDescriptionAction(context, "deletedescription_confirm2", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "deletedescription_cancel", null))))

            // adddimensionalitydescription 命令 (交互式添加维度域名描述)
            .then(literal("adddimensionalitydescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION, 2))
                .executes(ServerCommands::executeAddDimensionalityDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("dimensionId", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_select:",
                            StringArgumentType.getString(context, "dimensionId")))))
                .then(literal("text")
                    .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_text:", ""))
                    .then(argument("description", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_text:",
                            StringArgumentType.getString(context, "description")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_confirm", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "adddimensionalitydescription_cancel", null))))

            // replacedimensionalitydescription 命令 (交互式替换维度域名描述，保持原 adddimensionalitydescription 的空书本写入行为)
            .then(literal("replacedimensionalitydescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD_DIMENSIONALITY_DESCRIPTION, 2))
                .executes(ServerCommands::executeReplaceDimensionalityDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("dimensionId", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_select:",
                            StringArgumentType.getString(context, "dimensionId")))))
                .then(literal("text")
                    .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_text:", ""))
                    .then(argument("description", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_text:",
                            StringArgumentType.getString(context, "description")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_confirm", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "replacedimensionalitydescription_cancel", null))))

            // deletedimensionalitydescription 命令 (交互式删除维度域名描述)
            .then(literal("deletedimensionalitydescription")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETE_DIMENSIONALITY_DESCRIPTION, 2))
                .executes(ServerCommands::executeDeleteDimensionalityDescriptionStart)
                .then(literal("search")
                    .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_search:", ""))
                    .then(argument("query", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_search:",
                            StringArgumentType.getString(context, "query")))))
                .then(literal("select")
                    .then(argument("dimensionId", StringArgumentType.greedyString())
                        .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_select:",
                            StringArgumentType.getString(context, "dimensionId")))))
                .then(literal("confirm")
                    .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_confirm", null)))
                .then(literal("confirm2")
                    .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_confirm2", null)))
                .then(literal("cancel")
                    .executes(context -> executeDescriptionAction(context, "deletedimensionalitydescription_cancel", null))))

            // addsignature 命令 (交互式添加签名)
            .then(literal("addsignature")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADDSIGNATURE, 0))
                .executes(ServerCommands::executeAddSignatureStart)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeAddSignatureSelect(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("name")
                    .then(argument("playerName", StringArgumentType.greedyString())
                        .executes(context -> executeAddSignatureName(context,
                            StringArgumentType.getString(context, "playerName")))))
                .then(literal("confirm")
                    .executes(ServerCommands::executeAddSignatureConfirm))
                .then(literal("cancel")
                    .executes(ServerCommands::executeAddSignatureCancel)))

            // deletesignature 命令 (交互式删除签名)
            .then(literal("deletesignature")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETESIGNATURE, 0))
                .executes(ServerCommands::executeDeleteSignatureStart)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDeleteSignatureSelect(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("name")
                    .then(argument("playerName", StringArgumentType.greedyString())
                        .executes(context -> executeDeleteSignatureName(context,
                            StringArgumentType.getString(context, "playerName")))))
                .then(literal("confirm")
                    .executes(ServerCommands::executeDeleteSignatureConfirm))
                .then(literal("confirm2")
                    .executes(ServerCommands::executeDeleteSignatureConfirm2))
                .then(literal("cancel")
                    .executes(ServerCommands::executeDeleteSignatureCancel)))

            // frequency 命令
            .then(literal("frequency")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.FREQUENCY, 0))
                .then(argument("value", DoubleArgumentType.doubleArg(1, 60))
                    .executes(context -> executeFrequency(context, DoubleArgumentType.getDouble(context, "value"))))
                .executes(ServerCommands::executeFrequencyInfo))
            
            // hintrender 命令
            .then(literal("hintrender")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.HINT_RENDER, 0))
                .then(argument("mode", StringArgumentType.word())
                    .executes(context -> executeHintRender(context, StringArgumentType.getString(context, "mode"))))
                .executes(ServerCommands::executeHintRenderInfo))
            
            // titlestyle 命令（交互式）
            .then(literal("titlestyle")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.TITLE_STYLE, 0))
                .executes(ServerCommands::executeTitleStyleStart)
                .then(literal("select")
                    .then(argument("style", StringArgumentType.word())
                        .executes(context -> executeTitleStyleSelect(context, StringArgumentType.getString(context, "style")))))
                .then(literal("cancel")
                    .executes(ServerCommands::executeTitleStyleCancel)))

            // titlesize 命令（交互式）
            .then(literal("titlesize")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.TITLE_SIZE, 0))
                .executes(ServerCommands::executeTitleSizeStart)
                .then(literal("select")
                    .then(argument("size", StringArgumentType.word())
                        .executes(context -> executeTitleSizeSelect(context, StringArgumentType.getString(context, "size")))))
                .then(literal("cancel")
                    .executes(ServerCommands::executeTitleSizeCancel)))

            // addsubtitle 命令（交互式添加/替换域名副字幕）
            .then(createAddSubtitleCommand("addsubtitle"))

            // replacesubtitle 作为 addsubtitle 的同义指令，完整复用同一套权限、子命令和客户端流程。
            .then(createAddSubtitleCommand("replacesubtitle"))

            // deletesubtitle 命令（交互式删除域名副字幕字段）
            .then(literal("deletesubtitle")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETE_SUBTITLE, 0))
                .executes(SubtitleCommand::executeDeleteSubtitle)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeSubtitleClientCommand(context,
                            "deletesubtitle_select:" + stripQuotes(StringArgumentType.getString(context, "areaName"))))))
                .then(literal("confirm")
                    .executes(context -> executeSubtitleClientCommand(context, "deletesubtitle_confirm")))
                .then(literal("cancel")
                    .executes(context -> executeSubtitleClientCommand(context, "deletesubtitle_cancel"))))

            // replacesubtitlecolor 命令（交互式替换域名副字幕颜色）
            .then(createReplaceSubtitleColorCommand("replacesubtitlecolor"))

            // replacesubtitlesize 命令（修改个人配置中的副字幕大小）
            .then(literal("replacesubtitlesize")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.REPLACE_SUBTITLE_SIZE, 0))
                .executes(context -> executeSubtitleClientCommand(context, "replacesubtitlesize_start"))
                .then(literal("select")
                    .then(argument("size", StringArgumentType.word())
                        .executes(context -> executeSubtitleClientCommand(context,
                            "replacesubtitlesize_select:" + StringArgumentType.getString(context, "size")))))
                .then(literal("cancel")
                    .executes(context -> executeSubtitleClientCommand(context, "replacesubtitlesize_cancel"))))
                
            // easyadd 命令（带多个子命令）
            .then(createEasyAddCommand("easyadd"))

            // addarea 是 easyadd 的完整别名，权限与执行逻辑都复用 EasyAdd。
            .then(createEasyAddCommand("addarea"))
                            
            // expandarea 命令 (域名扩展)
            .then(literal("expandarea")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.EXPANDAREA, 0))
                .executes(ServerCommands::executeExpandAreaStart)
                // 直接指定域名：/areahint expandarea "域名"
                .then(argument("areaName", StringArgumentType.greedyString())
                    .suggests(EXPANDABLE_AREA_SUGGESTIONS)
                    .executes(context -> executeExpandAreaSelect(context, 
                        StringArgumentType.getString(context, "areaName"))))
                // 子命令（保持向后兼容）
                .then(literal("select")
                    .then(argument("selectAreaName", StringArgumentType.greedyString())
                        .suggests(EXPANDABLE_AREA_SUGGESTIONS)
                        .executes(context -> executeExpandAreaSelect(context, 
                            StringArgumentType.getString(context, "selectAreaName")))))
                .then(literal("continue")
                    .executes(ServerCommands::executeExpandAreaContinue))
                .then(literal("save")
                    .executes(ServerCommands::executeExpandAreaSave))
                .then(literal("cancel")
                    .executes(ServerCommands::executeExpandAreaCancel)))
            
            // shrinkarea 命令 (域名收缩)
            .then(literal("shrinkarea")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.SHRINKAREA, 0))
                .executes(ServerCommands::executeShrinkAreaStart)
                // 直接指定域名：/areahint shrinkarea "域名"
                .then(argument("areaName", StringArgumentType.greedyString())
                    .suggests(SHRINKABLE_AREA_SUGGESTIONS)
                    .executes(context -> executeShrinkAreaSelect(context, 
                        StringArgumentType.getString(context, "areaName"))))
                // 子命令（保持向后兼容）
                .then(literal("select")
                    .then(argument("selectAreaName", StringArgumentType.greedyString())
                        .suggests(SHRINKABLE_AREA_SUGGESTIONS)
                        .executes(context -> executeShrinkAreaSelect(context, 
                            StringArgumentType.getString(context, "selectAreaName")))))
                .then(literal("continue")
                    .executes(ServerCommands::executeShrinkAreaContinue))
                .then(literal("save")
                    .executes(ServerCommands::executeShrinkAreaSave))
                .then(literal("cancel")
                    .executes(ServerCommands::executeShrinkAreaCancel)))

            // dividearea 命令 (域名分割)
            .then(literal("dividearea")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DIVIDEAREA, 0))
                .executes(ServerCommands::executeDivideAreaStart)
                .then(literal("select")
                    .then(argument("selectAreaName", StringArgumentType.greedyString())
                        .executes(context -> executeDivideAreaSelect(context,
                            StringArgumentType.getString(context, "selectAreaName")))))
                .then(literal("continue")
                    .executes(ServerCommands::executeDivideAreaContinue))
                .then(literal("save")
                    .executes(ServerCommands::executeDivideAreaSave))
                .then(literal("name")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDivideAreaName(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("level")
                    .then(argument("levelValue", IntegerArgumentType.integer(1, 3))
                        .executes(context -> executeDivideAreaLevel(context,
                            IntegerArgumentType.getInteger(context, "levelValue")))))
                .then(literal("base")
                    .then(argument("baseName", StringArgumentType.greedyString())
                        .executes(context -> executeDivideAreaBase(context,
                            StringArgumentType.getString(context, "baseName")))))
                .then(literal("color")
                    .then(argument("colorValue", StringArgumentType.greedyString())
                        .executes(context -> executeDivideAreaColor(context,
                            StringArgumentType.getString(context, "colorValue")))))
                .then(literal("cancel")
                    .executes(ServerCommands::executeDivideAreaCancel)))

            // recolor 命令
            .then(literal("recolor")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.RECOLOR, 0))
                .executes(RecolorCommand::executeRecolor)
                // /areahint recolor select <域名>
                .then(literal("select")
                    .then(argument("selectAreaName", StringArgumentType.greedyString())
                        .executes(context -> executeRecolorSelect(context,
                            StringArgumentType.getString(context, "selectAreaName")))))
                // /areahint recolor color <颜色>
                .then(literal("color")
                    .then(argument("colorValue", StringArgumentType.greedyString())
                        .executes(context -> executeRecolorColor(context,
                            StringArgumentType.getString(context, "colorValue")))))
                // /areahint recolor confirm
                .then(literal("confirm")
                    .executes(ServerCommands::executeRecolorConfirm))
                // /areahint recolor cancel
                .then(literal("cancel")
                    .executes(ServerCommands::executeRecolorCancel)))
            
            // rename 命令（交互式流程）
            .then(literal("rename")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.RENAME, 0))
                .executes(RenameAreaCommand::executeRename)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> RenameAreaCommand.executeRenameSelect(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("confirm")
                    .executes(RenameAreaCommand::executeRenameConfirm))
                .then(literal("cancel")
                    .executes(RenameAreaCommand::executeRenameCancel)))
                            
            // tcp 命令（中心传送）
            .then(literal("tcp")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.TELEPORT, 0))
                .executes(context -> executeTeleportStart(context, "tcp"))
                .then(argument("areaName", StringArgumentType.greedyString())
                    .suggests(TELEPORT_AREA_SUGGESTIONS)
                    .executes(context -> executeTeleportSelect(context, "tcp", StringArgumentType.getString(context, "areaName")))))

            // udp 命令（随机传送）
            .then(literal("udp")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.TELEPORT, 0))
                .executes(context -> executeTeleportStart(context, "udp"))
                .then(argument("areaName", StringArgumentType.greedyString())
                    .suggests(TELEPORT_AREA_SUGGESTIONS)
                    .executes(context -> executeTeleportSelect(context, "udp", StringArgumentType.getString(context, "areaName")))))

            // settp 命令（设置传送命令头）
            .then(literal("settp")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.SETTP, 0))
                .executes(ServerCommands::executeSetTpStart)
                .then(argument("format", StringArgumentType.greedyString())
                    .executes(context -> executeSetTpSet(context, StringArgumentType.getString(context, "format")))))

            // sethigh 命令
            .then(literal("sethigh")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.SETHIGH, 0))
                .then(literal("custom")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> SetHighCommand.executeSetHighCustom(context, StringArgumentType.getString(context, "areaName")))))
                .then(literal("unlimited")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> SetHighCommand.executeSetHighUnlimited(context, StringArgumentType.getString(context, "areaName")))))
                .then(literal("cancel")
                    .executes(context -> SetHighCommand.executeSetHighCancel(context)))
                .then(argument("areaName", StringArgumentType.greedyString())
                    .suggests(SETHIGH_AREA_SUGGESTIONS)
                    .executes(context -> SetHighCommand.executeSetHighWithArea(context, StringArgumentType.getString(context, "areaName"))))
                .executes(SetHighCommand::executeSetHigh))

            // addhint 命令 (向已有域名添加顶点)
            .then(literal("addhint")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADDHINT, 0))
                .executes(ServerCommands::executeAddHintStart)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeAddHintSelect(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("continue")
                    .executes(ServerCommands::executeAddHintContinue))
                .then(literal("submit")
                    .executes(ServerCommands::executeAddHintSubmit))
                .then(literal("cancel")
                    .executes(ServerCommands::executeAddHintCancel)))

            // deletehint 命令 (从已有域名删除顶点)
            .then(literal("deletehint")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DELETEHINT, 0))
                .executes(ServerCommands::executeDeleteHintStart)
                .then(literal("select")
                    .then(argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeDeleteHintSelect(context,
                            StringArgumentType.getString(context, "areaName")))))
                .then(literal("toggle")
                    .then(argument("index", IntegerArgumentType.integer(0))
                        .executes(context -> executeDeleteHintToggle(context,
                            IntegerArgumentType.getInteger(context, "index")))))
                .then(literal("submit")
                    .executes(ServerCommands::executeDeleteHintSubmit))
                .then(literal("cancel")
                    .executes(ServerCommands::executeDeleteHintCancel)))

            // replacebutton 命令
            .then(literal("replacebutton")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.REPLACE_BUTTON, 0))
                .executes(ServerCommands::executeReplaceButtonStart)
                .then(literal("confirm")
                    .executes(ServerCommands::executeReplaceButtonConfirm))
                .then(literal("cancel")
                    .executes(ServerCommands::executeReplaceButtonCancel)))

            // language 命令（交互式语言选择）
            .then(literal("language")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.LANGUAGE, 0))
                .executes(ServerCommands::executeLanguageStart)
                .then(literal("select")
                    .then(argument("langCode", StringArgumentType.word())
                        .executes(context -> executeLanguageSelect(context, StringArgumentType.getString(context, "langCode")))))
                .then(literal("cancel")
                    .executes(ServerCommands::executeLanguageCancel)))

            // boundviz 命令
            .then(literal("boundviz")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.BOUND_VIZ, 0))
                .executes(ServerCommands::executeBoundViz))

            // serverlanguage 命令（服务端语言，权限等级4）
            .then(literal("serverlanguage")
                .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.SERVER_LANGUAGE, 4))
                .then(argument("langCode", StringArgumentType.word())
                    .executes(context -> executeServerLanguage(context, StringArgumentType.getString(context, "langCode")))))
        );
    }

    /**
     * 创建EasyAdd命令树。
     * <p>
     * easyadd 与 addarea 需要保持完全一致，所以这里集中维护所有子命令；
     * 新增或调整EasyAdd流程时，只需要改这一处，两个指令会同时生效。
     *
     * @param commandName 指令名称，例如 easyadd 或 addarea
     * @return 已绑定EasyAdd执行逻辑的命令节点
     */
    private static LiteralArgumentBuilder<ServerCommandSource> createEasyAddCommand(String commandName) {
        return literal(commandName)
            .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.EASYADD, 0))
            .executes(ServerCommands::executeEasyAddStart)
            .then(literal("cancel")
                .executes(ServerCommands::executeEasyAddCancel))
            .then(literal("level")
                .then(argument("levelValue", IntegerArgumentType.integer(1, 3))
                    .executes(context -> executeEasyAddLevel(context, IntegerArgumentType.getInteger(context, "levelValue")))))
            .then(literal("base")
                .then(argument("baseName", StringArgumentType.greedyString())
                    .executes(context -> executeEasyAddBase(context, StringArgumentType.getString(context, "baseName")))))
            .then(literal("continue")
                .executes(ServerCommands::executeEasyAddContinue))
            .then(literal("finish")
                .executes(ServerCommands::executeEasyAddFinish))
            .then(literal("altitude")
                .then(literal("auto")
                    .executes(ServerCommands::executeEasyAddAltitudeAuto))
                .then(literal("custom")
                    .executes(ServerCommands::executeEasyAddAltitudeCustom))
                .then(literal("unlimited")
                    .executes(ServerCommands::executeEasyAddAltitudeUnlimited)))
            .then(literal("color")
                .then(argument("colorValue", StringArgumentType.greedyString())
                    .executes(context -> executeEasyAddColor(context, StringArgumentType.getString(context, "colorValue")))))
            .then(literal("save")
                .executes(ServerCommands::executeEasyAddSave));
    }

    /**
     * 创建添加/替换副字幕命令树。
     * <p>
     * addsubtitle 与 replacesubtitle 都调用这里，内部继续派发 addsubtitle_* 动作，
     * 确保两个命令入口实际走同一套客户端状态机和服务端写入逻辑。
     */
    private static LiteralArgumentBuilder<ServerCommandSource> createAddSubtitleCommand(String commandName) {
        return literal(commandName)
            .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.ADD_SUBTITLE, 0))
            .executes(SubtitleCommand::executeAddSubtitle)
            .then(literal("select")
                .then(argument("areaName", StringArgumentType.greedyString())
                    .executes(context -> executeSubtitleClientCommand(context,
                        "addsubtitle_select:" + stripQuotes(StringArgumentType.getString(context, "areaName"))))))
            .then(literal("color")
                .then(argument("colorValue", StringArgumentType.word())
                    .executes(context -> executeSubtitleClientCommand(context,
                        "addsubtitle_color:" + StringArgumentType.getString(context, "colorValue")))))
            .then(literal("confirm")
                .executes(context -> executeSubtitleClientCommand(context, "addsubtitle_confirm")))
            .then(literal("cancel")
                .executes(context -> executeSubtitleClientCommand(context, "addsubtitle_cancel")));
    }

    /**
     * 创建副字幕颜色命令树。
     */
    private static LiteralArgumentBuilder<ServerCommandSource> createReplaceSubtitleColorCommand(String commandName) {
        return literal(commandName)
            .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.REPLACE_SUBTITLE_COLOR, 0))
            .executes(SubtitleCommand::executeReplaceSubtitleColor)
            .then(literal("select")
                .then(argument("areaName", StringArgumentType.greedyString())
                    .executes(context -> executeSubtitleClientCommand(context,
                        "replacesubtitlecolor_select:" + stripQuotes(StringArgumentType.getString(context, "areaName"))))))
            .then(literal("color")
                .then(argument("colorValue", StringArgumentType.greedyString())
                    .executes(context -> executeSubtitleClientCommand(context,
                        "replacesubtitlecolor_color:" + StringArgumentType.getString(context, "colorValue")))))
            .then(literal("confirm")
                .executes(context -> executeSubtitleClientCommand(context, "replacesubtitlecolor_confirm")))
            .then(literal("cancel")
                .executes(context -> executeSubtitleClientCommand(context, "replacesubtitlecolor_cancel")));
    }
    
    /**
     * 执行help命令
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendMessage(Text.translatable("command.title.general"));
        source.sendMessage(Text.translatable("help.command.help"));
        source.sendMessage(Text.translatable("command.usage.on"));
        source.sendMessage(Text.translatable("command.usage.off"));
        source.sendMessage(Text.translatable("help.command.reload"));
        source.sendMessage(Text.translatable("help.command.delete"));
        source.sendMessage(Text.translatable("help.command.frequency"));
        source.sendMessage(Text.translatable("help.command.hintrender"));
        source.sendMessage(Text.translatable("help.command.titlestyle"));
        source.sendMessage(Text.translatable("help.command.titlesize"));
        source.sendMessage(Text.translatable("help.command.addsubtitle"));
        source.sendMessage(Text.translatable("help.command.replacesubtitle"));
        source.sendMessage(Text.translatable("help.command.deletesubtitle"));
        source.sendMessage(Text.translatable("help.command.replacesubtitlecolor"));
        source.sendMessage(Text.translatable("help.command.replacesubtitlesize"));
        source.sendMessage(Text.translatable("help.command.add"));
        source.sendMessage(Text.translatable("help.command.easyadd"));
        source.sendMessage(Text.translatable("help.command.addarea"));
        source.sendMessage(Text.translatable("help.command.recolor"));
        source.sendMessage(Text.translatable("help.command.rename"));
        source.sendMessage(Text.translatable("help.command.sethigh"));
        source.sendMessage(Text.translatable("help.command.tcp"));
        source.sendMessage(Text.translatable("help.command.udp"));
        source.sendMessage(Text.translatable("help.command.settp"));
        source.sendMessage(Text.translatable("help.command.replacebutton"));
        source.sendMessage(Text.translatable("help.command.check"));
        source.sendMessage(Text.translatable("help.command.dimensionalityname"));
        source.sendMessage(Text.translatable("help.command.dimensionalitycolor"));
        source.sendMessage(Text.translatable("help.command.expandarea"));
        source.sendMessage(Text.translatable("help.command.shrinkarea"));
        source.sendMessage(Text.translatable("help.command.dividearea"));
        source.sendMessage(Text.translatable("help.command.addhint"));
        source.sendMessage(Text.translatable("help.command.deletehint"));
        source.sendMessage(Text.translatable("help.command.boundviz"));
        source.sendMessage(Text.translatable("help.command.language"));
        source.sendMessage(Text.translatable("help.command.firstdimname"));
        source.sendMessage(Text.translatable("help.command.firstdimname_skip"));
        source.sendMessage(Text.translatable("help.command.debug"));
        source.sendMessage(Text.translatable("help.command.adddescription"));
        source.sendMessage(Text.translatable("help.command.replacedescription"));
        source.sendMessage(Text.translatable("help.command.deletedescription"));
        source.sendMessage(Text.translatable("help.command.adddimensionalitydescription"));
        source.sendMessage(Text.translatable("help.command.replacedimensionalitydescription"));
        source.sendMessage(Text.translatable("help.command.deletedimensionalitydescription"));
        source.sendMessage(Text.translatable("help.command.addsignature"));
        source.sendMessage(Text.translatable("help.command.deletesignature"));
        source.sendMessage(Text.translatable("help.command.serverlanguage"));

        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行reload命令
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // 重新加载区域数据并发送给所有客户端
        ServerNetworking.sendAllAreaDataToAll();
        
        // 重新发送维度域名配置给所有客户端
        try {
            MinecraftServer server = source.getServer();
            areahint.network.DimensionalNameNetworking.sendDimensionalNamesToAllClients(server);
            Areashint.LOGGER.info(ServerI18nManager.translate("command.message.area.dimension_3"));
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.area.dimension_4"), e);
        }
        
        // 通知客户端重载配置
        sendClientCommand(source, "areahint:reload");
        
        source.sendMessage(Text.translatable("command.message.area.dimension"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行add命令
     * @param context 命令上下文
     * @param json JSON字符串
     * @return 执行结果
     */
    private static int executeAdd(CommandContext<ServerCommandSource> context, String json) {
        ServerCommandSource source = context.getSource();
        
        // 解析JSON数据
        AreaData areaData = JsonHelper.fromJsonSingle(json);
        
        if (areaData == null) {
            source.sendMessage(Text.translatable("command.error.general_6"));
            return 0;
        }
        
        // 验证高度数据（如果存在）
        if (areaData.getAltitude() != null) {
            if (!areaData.getAltitude().isValid()) {
                source.sendMessage(Text.translatable("command.error.altitude_8"));
                return 0;
            }
            
            // 检查高度值的合理性（Minecraft世界高度通常在-64到320之间）
            Double minAlt = areaData.getAltitude().getMin();
            Double maxAlt = areaData.getAltitude().getMax();
            
            if (minAlt != null && (minAlt < -64 || minAlt > 320)) {
                source.sendMessage(Text.translatable("command.error.altitude_2", minAlt));
                return 0;
            }
            
            if (maxAlt != null && (maxAlt < -64 || maxAlt > 320)) {
                source.sendMessage(Text.translatable("command.error.altitude", maxAlt));
                return 0;
            }
        }
        
        // 规范化颜色，确保 /add JSON 与 EasyAdd/Recolor 一致写入静态色或 FLASH_* 语义
        areaData.setColor(areahint.util.ColorUtil.normalizeColor(areaData.getColor()));

        if (!areaData.isValid()) {
            source.sendMessage(Text.translatable("command.error.area.level"));
            return 0;
        }

        // 根据维度决定写入哪个文件
        String dimensionId = getDimensionFromSource(source);
        if (dimensionId == null) {
            source.sendMessage(Text.translatable("command.error.dimension"));
            return 0;
        }
        
        String fileName = Packets.getFileNameForDimension(dimensionId);
        if (fileName == null) {
            source.sendMessage(Text.translatable("command.error.dimension_2").append(Text.literal(dimensionId)));
            return 0;
        }
        
        Path filePath = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
        
        // 自动设置创建者签名（如果没有设置的话）
        if (areaData.getSignature() == null) {
            areaData.setSignature(source.getName());
        }
        
        // 添加区域数据
        if (FileManager.addAreaData(filePath, areaData)) {
            source.sendMessage(Text.translatable("command.success.area.add").append(Text.literal(areaData.getName())));
            
            // 通知所有客户端更新
            ServerNetworking.sendAreaDataToAll(dimensionId);
            
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendMessage(Text.translatable("command.error.area.add"));
            return 0;
        }
    }
    
    private static int executeTeleportStart(CommandContext<ServerCommandSource> context, String mode) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:" + mode + "_start");
        return Command.SINGLE_SUCCESS;
    }

    private static int executeTeleportSelect(CommandContext<ServerCommandSource> context, String mode, String areaName) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:" + mode + "_select:" + stripQuotes(areaName));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSetTpStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:settp_start");
        return Command.SINGLE_SUCCESS;
    }

    private static int executeSetTpSet(CommandContext<ServerCommandSource> context, String format) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:settp_set:" + stripQuotes(format));
        return Command.SINGLE_SUCCESS;
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 1) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static int forwardSignatureCommand(CommandContext<ServerCommandSource> context, String command) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, command);
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAddSignatureStart(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:addsignature_start");
    }

    private static int executeAddSignatureSelect(CommandContext<ServerCommandSource> context, String areaName) {
        return forwardSignatureCommand(context, "areahint:addsignature_select:" + areaName);
    }

    private static int executeAddSignatureName(CommandContext<ServerCommandSource> context, String playerName) {
        return forwardSignatureCommand(context, "areahint:addsignature_name:" + playerName);
    }

    private static int executeAddSignatureConfirm(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:addsignature_confirm");
    }

    private static int executeAddSignatureCancel(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:addsignature_cancel");
    }

    private static int executeDeleteSignatureStart(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:deletesignature_start");
    }

    private static int executeDeleteSignatureSelect(CommandContext<ServerCommandSource> context, String areaName) {
        return forwardSignatureCommand(context, "areahint:deletesignature_select:" + areaName);
    }

    private static int executeDeleteSignatureName(CommandContext<ServerCommandSource> context, String playerName) {
        return forwardSignatureCommand(context, "areahint:deletesignature_name:" + playerName);
    }

    private static int executeDeleteSignatureConfirm(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:deletesignature_confirm");
    }

    private static int executeDeleteSignatureConfirm2(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:deletesignature_confirm2");
    }

    private static int executeDeleteSignatureCancel(CommandContext<ServerCommandSource> context) {
        return forwardSignatureCommand(context, "areahint:deletesignature_cancel");
    }

    /**
     * 从命令源获取当前维度
     * @param source 命令源
     * @return 维度ID
     */
    private static String getDimensionFromSource(ServerCommandSource source) {
        try {
            Identifier dimension = source.getWorld().getDimensionKey().getValue();
            return Packets.convertDimensionPathToType(dimension.getPath());
        } catch (Exception e) {
            return null;
        }
    }
    
    
    /**
     * 发送客户端命令
     * @param source 命令源
     * @param command 客户端命令
     */
    private static void sendClientCommand(ServerCommandSource source, String command) {
        try {
            // 向运行该命令的玩家发送客户端命令
            if (source.getPlayer() != null) {
                ServerNetworking.sendCommandToClient(source.getPlayer(), command);
            }
        } catch (Exception e) {
            Areashint.LOGGER.error("发送客户端命令时出错", e);
        }
    }

    /**
     * 转发副字幕客户端动作。
     * 副字幕流程的按钮和文本输入都只作用于执行命令的玩家本人，不支持目标选择器。
     */
    private static int executeSubtitleClientCommand(CommandContext<ServerCommandSource> context, String action) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        sendClientCommand(source, "areahint:" + action);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 执行模组客户端开关命令。
     * 该开关只写入执行命令玩家自己的客户端配置，不改变服务端状态，也不影响其他玩家。
     * @param context 命令上下文
     * @param action 开关动作，取值为on或off
     * @return 执行结果
     */
    private static int executeModToggle(CommandContext<ServerCommandSource> context, String action) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        sendClientCommand(source, "areahint:" + action);
        return Command.SINGLE_SUCCESS;
    }
	    
    private static int executeAddDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "adddescription_start", null);
    }

    private static int executeReplaceDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "replacedescription_start", null);
    }

    private static int executeDeleteDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "deletedescription_start", null);
    }

    private static int executeAddDimensionalityDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "adddimensionalitydescription_start", null);
    }

    private static int executeReplaceDimensionalityDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "replacedimensionalitydescription_start", null);
    }

    private static int executeDeleteDimensionalityDescriptionStart(CommandContext<ServerCommandSource> context) {
        return executeDescriptionAction(context, "deletedimensionalitydescription_start", null);
    }

    private static int executeDescriptionAction(CommandContext<ServerCommandSource> context, String action, String value) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        sendClientCommand(source, "areahint:" + action + (value == null ? "" : value));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 执行frequency命令（显示当前频率）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeFrequencyInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // 向客户端发送命令
        sendClientCommand(source, "areahint:frequency_info");
        
        source.sendMessage(Text.translatable("command.message.general_15"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行frequency命令（设置频率）
     * @param context 命令上下文
     * @param value 频率值
     * @return 执行结果
     */
    private static int executeFrequency(CommandContext<ServerCommandSource> context, double value) {
        ServerCommandSource source = context.getSource();
        String frequencyText = areahint.data.ConfigData.formatFrequency(value);
        
        // 向客户端发送命令
        sendClientCommand(source, "areahint:frequency " + frequencyText);
        
        source.sendMessage(Text.translatable("command.message.general_16").append(Text.literal(frequencyText + " ")).append(Text.translatable("command.message.frequency.unit")));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行hintrender命令（显示当前渲染方式）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeHintRenderInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // 向客户端发送命令
        sendClientCommand(source, "areahint:hintrender_info");
        
        source.sendMessage(Text.translatable("command.message.general_13"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * 执行hintrender命令（设置渲染方式）
     * @param context 命令上下文
     * @param mode 渲染方式
     * @return 执行结果
     */
    private static int executeHintRender(CommandContext<ServerCommandSource> context, String mode) {
        ServerCommandSource source = context.getSource();
        
        String normalizedMode = ConfigData.normalizeRenderMode(mode);
        
        if (ConfigData.isValidRenderMode(normalizedMode)) {
            // 向客户端发送命令
            sendClientCommand(source, "areahint:hintrender " + normalizedMode);

            source.sendMessage(Text.translatable("command.message.hintrender.sent"));

            return Command.SINGLE_SUCCESS;
        } else {
            source.sendMessage(Text.translatable("command.error.general_7").append(Text.literal(mode)).append(Text.translatable("command.error.general")));
            return 0;
        }
    }
    
    /**
     * 执行titlestyle命令（启动交互式样式选择流程）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeTitleStyleStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 检查是否为客户端命令
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        // 发送客户端命令
        try {
            // 通过网络发送到客户端处理
            sendClientCommand(source, "areahint:titlestyle_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.start_4").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行titlestyle select命令（选择样式）
     * @param context 命令上下文
     * @param style 样式
     * @return 执行结果
     */
    private static int executeTitleStyleSelect(CommandContext<ServerCommandSource> context, String style) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:titlestyle_select:" + style);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.general_14").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行titlestyle cancel命令（取消样式选择）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeTitleStyleCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:titlestyle_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel_5").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行titlesize命令（启动交互式大小选择流程）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeTitleSizeStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 检查是否为客户端命令
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        // 发送客户端命令
        try {
            // 通过网络发送到客户端处理
            sendClientCommand(source, "areahint:titlesize_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.start_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行titlesize select命令（选择大小）
     * @param context 命令上下文
     * @param size 大小
     * @return 执行结果
     */
    private static int executeTitleSizeSelect(CommandContext<ServerCommandSource> context, String size) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:titlesize_select:" + size);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.general_13").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行titlesize cancel命令（取消大小选择）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeTitleSizeCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:titlesize_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行delete命令（启动交互式删除流程）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeDeleteStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 检查是否为客户端命令
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        // 发送客户端命令
        try {
            // 通过网络发送到客户端处理
            sendClientCommand(source, "areahint:delete_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.start").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行delete select命令（选择要删除的域名）
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    private static int executeDeleteSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:delete_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行delete confirm命令（确认删除）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeDeleteConfirm(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:delete_confirm");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.confirm.delete").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行delete cancel命令（取消删除）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeDeleteCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        try {
            sendClientCommand(source, "areahint:delete_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel.delete").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    private static boolean canDeleteArea(ServerPlayerEntity player, String playerName, AreaData area) {
        return PermissionService.hasNodeOr(player, PermissionNodes.DELETE,
            () -> player.hasPermissionLevel(2) || AreaPermissionUtil.isSignedBy(area, playerName));
    }

    private static boolean canSetHighArea(ServerPlayerEntity player, String playerName, AreaData area, List<AreaData> areas) {
        return PermissionService.hasNodeOr(player, PermissionNodes.SETHIGH, () -> {
            if (player.hasPermissionLevel(2)) {
                return true;
            }
            if (AreaPermissionUtil.isSignedBy(area, playerName)) {
                return true;
            }
            for (AreaData otherArea : areas) {
                if (area.getName().equals(otherArea.getBaseName())
                    && AreaPermissionUtil.isSignedBy(otherArea, playerName)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static boolean canExpandArea(ServerPlayerEntity player, String playerName, AreaData area, List<AreaData> areas) {
        return PermissionService.hasNodeOr(player, PermissionNodes.EXPANDAREA,
            () -> AreaPermissionUtil.canModifyArea(player, area, areas));
    }

    private static boolean canShrinkArea(ServerPlayerEntity player, String playerName, AreaData area, List<AreaData> areas) {
        return PermissionService.hasNodeOr(player, PermissionNodes.SHRINKAREA, () -> {
            if (player.hasPermissionLevel(2)) {
                return true;
            }
            String baseName = area.getBaseName();
            if (baseName == null) {
                return false;
            }
            return AreaPermissionUtil.isBaseSignedByPlayer(baseName, areas, playerName);
        });
    }

    /**
     * 获取当前玩家可以删除的域名列表
     * @param source 命令源
     * @param dimension 维度
     * @return 可删除的域名列表
     */
    private static List<String> getDeletableAreaNames(ServerCommandSource source, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                return List.of();
            }
            
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return List.of();
            }
            
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                return List.of();
            }

            List<AreaData> areas = FileManager.readAreaData(areaFile);
            String playerName = source.getName();

            return areas.stream()
                .filter(area -> canDeleteArea(player, playerName, area))
                .filter(area -> areas.stream().noneMatch(child -> area.getName().equals(child.getBaseName())))
                .map(AreaData::getName)
                .toList();
        } catch (Exception e) {
            Areashint.LOGGER.error("获取可删除域名列表时发生错误", e);
            return List.of();
        }
    }
    
    /**
     * 可删除域名的建议提供器
     */
    private static final SuggestionProvider<ServerCommandSource> DELETABLE_AREA_SUGGESTIONS = 
        (context, builder) -> {
            ServerCommandSource source = context.getSource();
            String dimension = getDimensionFromSource(source);
            List<String> deletableAreas = getDeletableAreaNames(source, dimension);
            
            // 添加所有可删除的域名到建议列表
            for (String areaName : deletableAreas) {
                if (areaName.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(areaName);
                }
            }
            
            return builder.buildFuture();
        };
    
    // ===== EasyAdd 命令处理方法 =====
    
    /**
     * 处理EasyAdd开始命令（仅客户端）
     */
    private static int executeEasyAddStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // 检查是否为客户端命令
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        // 发送客户端命令
        try {
            // 通过网络发送到客户端处理
            sendClientCommand(source, "areahint:easyadd_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.start_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd取消命令（仅客户端）
     */
    private static int executeEasyAddCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd等级选择命令（仅客户端）
     */
    private static int executeEasyAddLevel(CommandContext<ServerCommandSource> context, int level) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_level:" + level);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.level_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd上级域名选择命令（仅客户端）
     */
    private static int executeEasyAddBase(CommandContext<ServerCommandSource> context, String baseName) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_base:" + baseName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.parent").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd继续记录命令（仅客户端）
     */
    private static int executeEasyAddContinue(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_continue");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.record.continue").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd完成记录命令（仅客户端）
     */
    private static int executeEasyAddFinish(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_finish");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.record.finish").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd保存命令（仅客户端）
     */
    private static int executeEasyAddSave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_save");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("easyadd.error.area.save").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd自动高度选择命令（仅客户端）
     */
    private static int executeEasyAddAltitudeAuto(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_altitude_auto");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.altitude_4").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd自定义高度选择命令（仅客户端）
     */
    private static int executeEasyAddAltitudeCustom(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_altitude_custom");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.altitude_5").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 处理EasyAdd不限制高度选择命令（仅客户端）
     */
    private static int executeEasyAddAltitudeUnlimited(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_altitude_unlimited");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.altitude_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 处理EasyAdd颜色选择命令（仅客户端）
     */
    private static int executeEasyAddColor(CommandContext<ServerCommandSource> context, String color) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:easyadd_color:" + color);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.color_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行expandarea命令
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeExpandAreaStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // 检查是否为玩家执行
            if (source.getPlayer() == null) {
                source.sendMessage(Text.translatable("command.error.general_9"));
                return 0;
            }
            
            // 通过客户端命令通道发送启动命令
            sendClientCommand(source, "areahint:expandarea_start");
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.expand.start").append(Text.literal(e.getMessage())));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 执行expandarea命令（继续）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeExpandAreaContinue(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:expandarea_continue");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.continue.expand").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行expandarea命令（保存）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeExpandAreaSave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:expandarea_save");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.save.expand").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行shrinkarea命令
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeShrinkAreaStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        try {
            // 检查是否为玩家执行
            if (source.getPlayer() == null) {
                source.sendMessage(Text.translatable("command.error.general_9"));
                return 0;
            }
            
            // 通过客户端命令通道发送启动命令
            sendClientCommand(source, "areahint:shrinkarea_start");
            
            return Command.SINGLE_SUCCESS;
            
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.shrink.start").append(Text.literal(e.getMessage())));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 执行shrinkarea命令（继续）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeShrinkAreaContinue(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:shrinkarea_continue");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.continue.shrink").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行shrinkarea命令（保存）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeShrinkAreaSave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:shrinkarea_save");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area.save.shrink").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行expandarea命令（选择域名）
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    private static int executeExpandAreaSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:expandarea_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行expandarea命令（取消）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeExpandAreaCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:expandarea_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel.expand").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    // ===== DivideArea 命令处理 =====

    private static int executeDivideAreaStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (source.getPlayer() == null) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { source.sendMessage(Text.translatable("command.error.area.divide.start").append(Text.literal(e.getMessage()))); return 0; }
    }

    private static int executeDivideAreaSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { source.sendMessage(Text.translatable("command.error.area_2").append(Text.literal(e.getMessage()))); return 0; }
    }

    private static int executeDivideAreaContinue(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_continue");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaSave(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_save");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaName(CommandContext<ServerCommandSource> context, String name) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_name:" + name);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaLevel(CommandContext<ServerCommandSource> context, int level) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_level:" + level);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaBase(CommandContext<ServerCommandSource> context, String baseName) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_base:" + baseName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaColor(CommandContext<ServerCommandSource> context, String color) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_color:" + color);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    private static int executeDivideAreaCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (!source.isExecutedByPlayer()) { source.sendMessage(Text.translatable("command.error.general_9")); return 0; }
            sendClientCommand(source, "areahint:dividearea_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) { return 0; }
    }

    // ===== AddHint 命令处理 =====

    private static int executeAddHintStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (source.getPlayer() == null) {
                source.sendMessage(Text.translatable("command.error.general_9"));
                return 0;
            }
            sendClientCommand(source, "areahint:addhint_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.vertex.add.start").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeAddHintSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:addhint_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeAddHintContinue(CommandContext<ServerCommandSource> context) {
        // 继续记录由按键处理，此处无需操作
        return Command.SINGLE_SUCCESS;
    }

    private static int executeAddHintSubmit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:addhint_submit");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.general_5").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeAddHintCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:addhint_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel_4").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    // ===== DeleteHint 命令方法 =====

    private static int executeDeleteHintStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        try {
            if (source.getPlayer() == null) {
                source.sendMessage(Text.translatable("command.error.general_9"));
                return 0;
            }
            sendClientCommand(source, "areahint:deletehint_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.vertex.delete.start").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeDeleteHintSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:deletehint_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_2").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeDeleteHintToggle(CommandContext<ServerCommandSource> context, int index) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:deletehint_toggle:" + index);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.vertex").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeDeleteHintSubmit(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:deletehint_submit");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.general_5").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeDeleteHintCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:deletehint_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel_4").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 执行shrinkarea命令（选择域名）
     * @param context 命令上下文
     * @param areaName 域名名称
     * @return 执行结果
     */
    private static int executeShrinkAreaSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:shrinkarea_select:" + areaName);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.area_3").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
    
    /**
     * 执行shrinkarea命令（取消）
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int executeShrinkAreaCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        
        try {
            sendClientCommand(source, "areahint:shrinkarea_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel.shrink").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    /**
     * 获取当前玩家可以设置高度的域名列表
     * @param source 命令源
     * @param dimension 维度
     * @return 可设置高度的域名列表
     */
    private static List<String> getSettableAreaNames(ServerCommandSource source, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                return List.of();
            }
            
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return List.of();
            }
            
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                return List.of();
            }

            List<AreaData> areas = FileManager.readAreaData(areaFile);
            String playerName = source.getName();

            return areas.stream()
                .filter(area -> canSetHighArea(player, playerName, area, areas))
                .map(AreaData::getName)
                .toList();
        } catch (Exception e) {
            Areashint.LOGGER.error("获取可设置高度的域名列表时发生错误", e);
            return List.of();
        }
    }

    private static List<String> getTeleportAreaNames(ServerCommandSource source, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                return List.of();
            }

            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return List.of();
            }

            return FileManager.readAreaData(areaFile).stream()
                .filter(AreaData::isValid)
                .map(AreaData::getName)
                .toList();
        } catch (Exception e) {
            Areashint.LOGGER.error("获取可传送域名列表时发生错误", e);
            return List.of();
        }
    }

    private static final SuggestionProvider<ServerCommandSource> TELEPORT_AREA_SUGGESTIONS =
        (context, builder) -> suggestAreaNames(builder, getTeleportAreaNames(context.getSource(), getDimensionFromSource(context.getSource())));

    private static CompletableFuture<Suggestions> suggestAreaNames(SuggestionsBuilder builder, List<String> areaNames) {
        String remaining = builder.getRemaining().toLowerCase();
        for (String areaName : areaNames) {
            if (areaName.toLowerCase().startsWith(remaining)) {
                builder.suggest(areaName);
            }
        }
        return builder.buildFuture();
    }

    /**
     * 可设置高度的域名的建议提供器
     */
    private static final SuggestionProvider<ServerCommandSource> SETHIGH_AREA_SUGGESTIONS = 
        (context, builder) -> {
            ServerCommandSource source = context.getSource();
            String dimension = getDimensionFromSource(source);
            List<String> settableAreas = getSettableAreaNames(source, dimension);
            
            // 添加所有可设置高度的域名到建议列表
            for (String areaName : settableAreas) {
                if (areaName.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(areaName);
                }
            }
            
            return builder.buildFuture();
        };
    
    /**
     * 获取当前玩家可扩展的域名列表
     * @param source 命令源
     * @param dimension 维度名称
     * @return 可扩展的域名名称列表
     */
    private static List<String> getExpandableAreaNames(ServerCommandSource source, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                return List.of();
            }
            
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return List.of();
            }
            
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                return List.of();
            }

            List<AreaData> areas = FileManager.readAreaData(areaFile);
            String playerName = source.getName();

            return areas.stream()
                .filter(area -> canExpandArea(player, playerName, area, areas))
                .map(AreaData::getName)
                .toList();
        } catch (Exception e) {
            Areashint.LOGGER.error("获取可扩展域名列表时发生错误", e);
            return List.of();
        }
    }
    
    /**
     * 可扩展域名的建议提供器
     */
    private static final SuggestionProvider<ServerCommandSource> EXPANDABLE_AREA_SUGGESTIONS = 
        (context, builder) -> {
            ServerCommandSource source = context.getSource();
            String dimension = getDimensionFromSource(source);
            List<String> expandableAreas = getExpandableAreaNames(source, dimension);
            
            // 添加所有可扩展的域名到建议列表
            for (String areaName : expandableAreas) {
                if (areaName.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(areaName);
                }
            }
            
            return builder.buildFuture();
        };
    
    /**
     * 获取当前玩家可收缩的域名列表
     * @param source 命令源
     * @param dimension 维度名称
     * @return 可收缩的域名名称列表
     */
    private static List<String> getShrinkableAreaNames(ServerCommandSource source, String dimension) {
        try {
            String fileName = Packets.getFileNameForDimension(dimension);
            if (fileName == null) {
                return List.of();
            }
            
            Path areaFile = areahint.world.WorldFolderManager.getWorldDimensionFile(fileName);
            if (!areaFile.toFile().exists()) {
                return List.of();
            }
            
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                return List.of();
            }

            List<AreaData> areas = FileManager.readAreaData(areaFile);
            String playerName = source.getName();

            return areas.stream()
                .filter(area -> canShrinkArea(player, playerName, area, areas))
                .map(AreaData::getName)
                .toList();
        } catch (Exception e) {
            Areashint.LOGGER.error("获取可收缩域名列表时发生错误", e);
            return List.of();
        }
    }
    
    /**
     * 可收缩域名的建议提供器
     */
    private static final SuggestionProvider<ServerCommandSource> SHRINKABLE_AREA_SUGGESTIONS = 
        (context, builder) -> {
            ServerCommandSource source = context.getSource();
            String dimension = getDimensionFromSource(source);
            List<String> shrinkableAreas = getShrinkableAreaNames(source, dimension);
            
            // 添加所有可收缩的域名到建议列表
            for (String areaName : shrinkableAreas) {
                if (areaName.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(areaName);
                }
            }

            return builder.buildFuture();
        };

    /**
     * 执行recolor select命令（客户端通过网络调用）
     */
    private static int executeRecolorSelect(CommandContext<ServerCommandSource> context, String areaName) {
        ServerCommandSource source = context.getSource();

        // 移除引号（如果存在）
        if (areaName.startsWith("\"") && areaName.endsWith("\"") && areaName.length() > 1) {
            areaName = areaName.substring(1, areaName.length() - 1);
        }

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:recolor_select:" + areaName);
        }

        return 1;
    }

    /**
     * 执行recolor color命令（客户端通过网络调用）
     */
    private static int executeRecolorColor(CommandContext<ServerCommandSource> context, String colorValue) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:recolor_color:" + colorValue);
        }

        return 1;
    }

    /**
     * 执行recolor confirm命令（客户端通过网络调用）
     */
    private static int executeRecolorConfirm(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:recolor_confirm");
        }

        return 1;
    }

    /**
     * 执行recolor cancel命令（客户端通过网络调用）
     */
    private static int executeRecolorCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:recolor_cancel");
        }

        return 1;
    }

    /**
     * 执行replacebutton命令（开始按键更换流程）
     */
    private static int executeReplaceButtonStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:replacebutton_start");
        }

        return 1;
    }

    /**
     * 执行replacebutton confirm命令（确认新按键）
     */
    private static int executeReplaceButtonConfirm(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:replacebutton_confirm");
        }

        return 1;
    }

    /**
     * 执行replacebutton cancel命令（取消按键更换）
     */
    private static int executeReplaceButtonCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 发送命令到客户端
        if (source.getPlayer() != null) {
            ServerNetworking.sendCommandToClient(source.getPlayer(),
                "areahint:replacebutton_cancel");
        }

        return 1;
    }

    /**
     * 执行boundviz命令（切换边界可视化）
     */
    private static int executeLanguageStart(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:language_start");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.start.language").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeLanguageSelect(CommandContext<ServerCommandSource> context, String langCode) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:language_select:" + langCode);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.language").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeLanguageCancel(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }
        try {
            sendClientCommand(source, "areahint:language_cancel");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.cancel.language").append(Text.literal(e.getMessage())));
            return 0;
        }
    }

    private static int executeServerLanguage(CommandContext<ServerCommandSource> context, String langCode) {
        ServerCommandSource source = context.getSource();
        ServerI18nManager.loadLanguage(langCode);
        source.sendMessage(Text.literal(ServerI18nManager.translate("command.success.serverlanguage", langCode)));
        Areashint.LOGGER.info(ServerI18nManager.translate("command.success.serverlanguage", langCode));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeBoundViz(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // 检查是否为客户端命令
        if (!source.isExecutedByPlayer()) {
            source.sendMessage(Text.translatable("command.error.general_9"));
            return 0;
        }

        // 发送命令到客户端
        try {
            sendClientCommand(source, "areahint:boundviz_toggle");
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendMessage(Text.translatable("command.error.boundary.visualization").append(Text.literal(e.getMessage())));
            return 0;
        }
    }
}
