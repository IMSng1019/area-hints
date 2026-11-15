package areahint.command;

import areahint.Areashint;
import areahint.debug.DebugManager;
import areahint.debug.DebugManager.DebugCategory;
import areahint.i18n.ServerI18nManager;
import areahint.network.ServerNetworking;
import areahint.permission.PermissionNodes;
import areahint.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


/**
 * 调试命令处理类
 * 用于注册和处理调试相关的命令
 */
public class DebugCommand {
    /**
     * 注册调试命令
     * @param dispatcher 命令分发器
     * @param registryAccess 注册表访问
     * @param environment 环境
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, 
                               CommandBuildContext registryAccess, 
                               Commands.CommandSelection environment) {
        
        dispatcher.register(
            Commands.literal("areahint")
                .then(Commands.literal("debug")
                    .requires(source -> PermissionService.hasCommandPermission(source, PermissionNodes.DEBUG, 2))
                    .executes(DebugCommand::toggleDebug)
                    .then(Commands.literal("on")
                        .executes(DebugCommand::enableDebug))
                    .then(Commands.literal("off")
                        .executes(DebugCommand::disableDebug))
                    .then(Commands.literal("status")
                        .executes(DebugCommand::showDebugStatus))
                )
        );
    }
    
    /**
     * 切换调试模式
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int toggleDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendSuccess(() -> Component.translatable("command.message.general_30").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        if (DebugManager.isDebugEnabled(player.getUUID())) {
            return disableDebug(context);
        } else {
            return enableDebug(context);
        }
    }
    
    /**
     * 启用调试模式
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int enableDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendSuccess(() -> Component.translatable("command.message.general_30").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        boolean wasEnabled = DebugManager.enableDebug(player);
        
        // 发送调试命令到客户端
        ServerNetworking.sendDebugCommandToClient(player, true);
        
        // 发送当前配置信息
        if (wasEnabled) {
            DebugManager.sendTranslatableDebugInfo(DebugCategory.CONFIG, "message.message.general_20");
        }
        
        return 1;
    }
    
    /**
     * 禁用调试模式
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int disableDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendSuccess(() -> Component.translatable("command.message.general_30").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        DebugManager.disableDebug(player);
        
        // 发送调试命令到客户端
        ServerNetworking.sendDebugCommandToClient(player, false);
        
        return 1;
    }
    
    /**
     * 显示调试状态
     * @param context 命令上下文
     * @return 命令结果
     */
    private static int showDebugStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        
        if (player == null) {
            source.sendSuccess(() -> Component.translatable("command.message.general_30").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        boolean isEnabled = DebugManager.isDebugEnabled(player.getUUID());
        String status = isEnabled ? ServerI18nManager.translateForPlayer(player.getUUID(), "message.message.general_18") : ServerI18nManager.translateForPlayer(player.getUUID(), "message.message.general_19");
        ChatFormatting color = isEnabled ? ChatFormatting.GREEN : ChatFormatting.RED;

        source.sendSuccess(() -> Component.literal(ServerI18nManager.translateForPlayer(player.getUUID(), "command.hint.general"))
                .append(Component.literal(status).withStyle(color)), false);
        
        return 1;
    }
} 