package areahint.command;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.ServerI18nManager;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import areahint.util.SurfaceNameHandler;
import areahint.world.WorldFolderManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Check命令处理器
 * 处理 /areahint check 命令，显示联合域名和域名详细信息
 */
public class CheckCommand {
    
    /**
     * 注册check命令
     * @param dispatcher 命令分发器
     * @param registryAccess 注册表访问
     * @param environment 环境
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, 
                               CommandBuildContext registryAccess, 
                               Commands.CommandSelection environment) {
        
        dispatcher.register(
            Commands.literal("areahint")
                .then(Commands.literal("check")
                    .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.CHECK, 0))
                    .executes(CheckCommand::executeCheckAll)
                    .then(Commands.argument("unionName", StringArgumentType.string())
                        .suggests(createUnionNameSuggestionProvider())
                        .executes(CheckCommand::executeCheckUnion)
                    )
                )
        );
    }
    
    /**
     * 执行 /areahint check 命令 - 显示所有联合域名
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int executeCheckAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerPlayer player = source.getPlayerOrException();
            ResourceKey<Level> dimensionType = areahint.util.ServerPlayerCompat.world(player).dimension();
            String dimensionId = dimensionType.identifier().toString();
            
            // 获取当前维度的域名文件
            String fileName = getDimensionFileName(dimensionId);
            Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
            
            if (areaFile == null || !areaFile.toFile().exists()) {
                source.sendSystemMessage(Component.translatable("command.error.area.dimension_2").withStyle(ChatFormatting.RED));
                return 1;
            }

            // 读取域名数据
            List<AreaData> areas = FileManager.readAreaData(areaFile);

            if (areas.isEmpty()) {
                source.sendSystemMessage(Component.translatable("command.error.area.dimension").withStyle(ChatFormatting.RED));
                return 1;
            }
            
            // 收集所有联合域名
            Map<String, List<AreaData>> unionNameGroups = new LinkedHashMap<>();
            
            for (AreaData area : areas) {
                String unionName = area.getSurfacename();
                if (unionName == null || unionName.trim().isEmpty()) {
                    unionName = area.getName(); // 如果没有联合域名，使用实际域名
                }
                
                unionNameGroups.computeIfAbsent(unionName, k -> new ArrayList<>()).add(area);
            }
            
            // 发送标题
            source.sendSystemMessage(Component.literal(""));
            source.sendSystemMessage(Component.translatable("command.title.area.surface.list").withStyle(ChatFormatting.GOLD));
            source.sendSystemMessage(Component.translatable("command.message.area.surface_5").withStyle(ChatFormatting.GRAY));
            source.sendSystemMessage(Component.literal(""));
            
            // 显示每个联合域名
            for (Map.Entry<String, List<AreaData>> entry : unionNameGroups.entrySet()) {
                String unionName = entry.getKey();
                List<AreaData> areasInUnion = entry.getValue();
                
                MutableComponent unionText = Component.literal("§a▶ " + unionName).withStyle(ChatFormatting.GREEN);
                
                // 添加悬停提示
                MutableComponent hoverContent = Component.literal(ServerI18nManager.translateForPlayer(player.getUUID(), "command.message.area.surface_4") + unionName + "\n");
                hoverContent.append(Component.literal(ServerI18nManager.translateForPlayer(player.getUUID(), "command.message.general_8") + areasInUnion.size() + ServerI18nManager.translateForPlayer(player.getUUID(), "command.message.area_3")));

                for (int i = 0; i < areasInUnion.size(); i++) {
                    AreaData area = areasInUnion.get(i);
                    hoverContent.append(Component.literal("§f- " + area.getName()));
                    if (i < areasInUnion.size() - 1) {
                        hoverContent.append(Component.literal("\n"));
                    }
                }

                unionText.withStyle(style -> style
                    // 1.21.5 起 ClickEvent/HoverEvent 由构造器改为具体记录类型（ShowText / RunCommand）
                    /*.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverContent))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint check \"" + unionName + "\""))
                    */
                    .withHoverEvent(new HoverEvent.ShowText(hoverContent))
                    .withClickEvent(new ClickEvent.RunCommand("/areahint check \"" + unionName + "\""))
                );
                
                source.sendSystemMessage(unionText);
            }
            
            source.sendSystemMessage(Component.literal(""));
            source.sendSystemMessage(Component.translatable("command.message.general_9").append(Component.literal(String.valueOf(unionNameGroups.size()))).append(Component.translatable("command.message.area.surface_2")).append(Component.literal(String.valueOf(areas.size()))).append(Component.translatable("command.message.area_2")).withStyle(ChatFormatting.GRAY));
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_21"), e);
            source.sendSystemMessage(Component.translatable("command.error.general_2").append(Component.literal(e.getMessage())).withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    /**
     * 执行 /areahint check <联合域名> 命令 - 显示指定联合域名的详细信息
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int executeCheckUnion(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String unionName = StringArgumentType.getString(context, "unionName");
        
        try {
            ServerPlayer player = source.getPlayerOrException();
            ResourceKey<Level> dimensionType = areahint.util.ServerPlayerCompat.world(player).dimension();
            String dimensionId = dimensionType.identifier().toString();
            
            // 获取当前维度的域名文件
            String fileName = getDimensionFileName(dimensionId);
            Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
            
            if (areaFile == null || !areaFile.toFile().exists()) {
                source.sendSystemMessage(Component.translatable("command.error.area.dimension_2").withStyle(ChatFormatting.RED));
                return 1;
            }

            // 读取域名数据
            List<AreaData> areas = FileManager.readAreaData(areaFile);

            // 查找匹配的域名
            List<AreaData> matchedAreas = new ArrayList<>();

            for (AreaData area : areas) {
                String areaUnionName = area.getSurfacename();
                if (areaUnionName == null || areaUnionName.trim().isEmpty()) {
                    areaUnionName = area.getName();
                }

                if (areaUnionName.equals(unionName)) {
                    matchedAreas.add(area);
                }
            }

            if (matchedAreas.isEmpty()) {
                source.sendSystemMessage(Component.translatable("command.error.area.surface").append(Component.literal(unionName)).withStyle(ChatFormatting.RED));
                return 1;
            }
            
            // 发送详细信息
            source.sendSystemMessage(Component.literal(""));
            source.sendSystemMessage(Component.translatable("command.title.area.surface").withStyle(ChatFormatting.GOLD));
            source.sendSystemMessage(Component.translatable("command.message.area.surface_4").append(Component.literal(unionName)).withStyle(ChatFormatting.GOLD));
            source.sendSystemMessage(Component.translatable("command.message.area_6").append(Component.literal(String.valueOf(matchedAreas.size()))).withStyle(ChatFormatting.GRAY));
            source.sendSystemMessage(Component.literal(""));
            
            // 显示每个域名的详细信息
            for (int i = 0; i < matchedAreas.size(); i++) {
                AreaData area = matchedAreas.get(i);
                
                source.sendSystemMessage(Component.translatable("command.message.area_9").append(Component.literal((i + 1) + ":")).withStyle(ChatFormatting.GREEN));
                source.sendSystemMessage(Component.translatable("command.message.area").append(Component.literal(area.getName())));
                
                if (area.getSurfacename() != null && !area.getSurfacename().trim().isEmpty()) {
                    source.sendSystemMessage(Component.translatable("command.message.area.surface").append(Component.literal(area.getSurfacename())));
                }
                
                source.sendSystemMessage(Component.translatable("command.message.area.level").append(Component.literal(String.valueOf(area.getLevel()))).append(Component.translatable("command.message.general_29")).withStyle(ChatFormatting.YELLOW));
                
                if (area.getBaseName() != null) {
                    source.sendSystemMessage(Component.translatable("command.message.area.parent").append(Component.literal(area.getBaseName())));
                }
                
                if (area.getSignature() != null) {
                    source.sendSystemMessage(Component.translatable("command.message.general").append(Component.literal(area.getSignature())));
                }
                
                if (area.getColor() != null) {
                    source.sendSystemMessage(Component.translatable("command.message.color").append(Component.literal(area.getColor())));
                }
                
                // 显示高度信息
                if (area.getAltitude() != null) {
                    MutableComponent altitudeText = Component.translatable("command.message.altitude");
                    if (area.getAltitude().getMin() != null && area.getAltitude().getMax() != null) {
                        altitudeText.append(Component.literal(String.valueOf(area.getAltitude().getMin()))).append(Component.translatable("command.message.general_4")).append(Component.literal(String.valueOf(area.getAltitude().getMax())));
                    } else if (area.getAltitude().getMin() != null) {
                        altitudeText.append(Component.literal(String.valueOf(area.getAltitude().getMin()))).append(Component.translatable("command.message.general_2"));
                    } else if (area.getAltitude().getMax() != null) {
                        altitudeText.append(Component.literal(String.valueOf(area.getAltitude().getMax()))).append(Component.translatable("command.message.general_3"));
                    } else {
                        altitudeText.append(Component.translatable("command.message.general_25"));
                    }
                    source.sendSystemMessage(altitudeText);
                } else {
                    source.sendSystemMessage(Component.translatable("command.message.altitude_2"));
                }
                
                // 显示顶点信息
                if (area.getVertices() != null && !area.getVertices().isEmpty()) {
                    source.sendSystemMessage(Component.translatable("command.message.vertex").append(Component.literal(String.valueOf(area.getVertices().size()))));
                    
                    // 显示前几个顶点作为示例
                    MutableComponent verticesText = Component.translatable("command.message.vertex.coordinate");
                    int maxShow = Math.min(3, area.getVertices().size());
                    for (int j = 0; j < maxShow; j++) {
                        AreaData.Vertex vertex = area.getVertices().get(j);
                        verticesText.append(Component.literal("(" + vertex.getX() + ", " + vertex.getZ() + ")"));
                        if (j < maxShow - 1) {
                            verticesText.append(Component.literal(", "));
                        }
                    }
                    if (area.getVertices().size() > 3) {
                        verticesText.append(Component.literal("..."));
                    }
                    source.sendSystemMessage(verticesText);
                }
                
                if (i < matchedAreas.size() - 1) {
                    source.sendSystemMessage(Component.literal(""));
                }
            }
            
        } catch (Exception e) {
            Areashint.LOGGER.error(ServerI18nManager.translate("command.error.general_21"), e);
            source.sendSystemMessage(Component.translatable("command.error.general_2").append(Component.literal(e.getMessage())).withStyle(ChatFormatting.RED));
        }

        return 1;
    }

    /**
     * 创建联合域名建议提供器
     * @return 建议提供器
     */
    public static SuggestionProvider<CommandSourceStack> createUnionNameSuggestionProvider() {
        return (context, builder) -> {
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                ResourceKey<Level> dimensionType = areahint.util.ServerPlayerCompat.world(player).dimension();
                String dimensionId = dimensionType.identifier().toString();
                
                // 获取当前维度的域名文件
                String fileName = getDimensionFileName(dimensionId);
                Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
                
                if (areaFile == null || !areaFile.toFile().exists()) {
                    return Suggestions.empty();
                }
                
                // 读取域名数据
                List<AreaData> areas = FileManager.readAreaData(areaFile);
                
                // 收集所有联合域名
                Set<String> unionNames = new LinkedHashSet<>();
                
                for (AreaData area : areas) {
                    String unionName = area.getSurfacename();
                    if (unionName == null || unionName.trim().isEmpty()) {
                        unionName = area.getName();
                    }
                    unionNames.add(unionName);
                }
                
                // 过滤并添加建议
                String input = builder.getRemaining().toLowerCase();
                for (String unionName : unionNames) {
                    if (unionName.toLowerCase().contains(input)) {
                        builder.suggest("\"" + unionName + "\"");
                    }
                }
                
            } catch (Exception e) {
                // 静默处理错误，返回空建议
            }
            
            return builder.buildFuture();
        };
    }
    
    /**
     * 根据维度ID获取文件名
     * @param dimensionId 维度ID
     * @return 文件名
     */
    private static String getDimensionFileName(String dimensionId) {
        switch (dimensionId) {
            case "minecraft:overworld":
                return "overworld.json";
            case "minecraft:the_nether":
                return "the_nether.json";
            case "minecraft:the_end":
                return "the_end.json";
            default:
                return "overworld.json";
        }
    }
} 