package areahint.recolor;

import areahint.AreashintClient;
import areahint.i18n.I18nManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Recolor客户端命令处理器
 */
public class RecolorClientCommand {

    /**
     * 注册客户端命令
     */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("areahint")
            .then(ClientCommands.literal("recolor")
                // /areahint recolor select <域名>
                .then(ClientCommands.literal("select")
                    .then(ClientCommands.argument("areaName", StringArgumentType.greedyString())
                        .executes(context -> executeSelect(context, StringArgumentType.getString(context, "areaName")))))
                // /areahint recolor color <颜色>
                .then(ClientCommands.literal("color")
                    .then(ClientCommands.argument("colorValue", StringArgumentType.greedyString())
                        .executes(context -> executeColor(context, StringArgumentType.getString(context, "colorValue")))))
                // /areahint recolor confirm
                .then(ClientCommands.literal("confirm")
                    .executes(RecolorClientCommand::executeConfirm))
                // /areahint recolor cancel
                .then(ClientCommands.literal("cancel")
                    .executes(RecolorClientCommand::executeCancel))));
    }

    /**
     * 执行域名选择
     */
    private static int executeSelect(CommandContext<FabricClientCommandSource> context, String areaName) {
        try {
            // 移除引号（如果存在）
            if (areaName.startsWith("\"") && areaName.endsWith("\"") && areaName.length() > 1) {
                areaName = areaName.substring(1, areaName.length() - 1);
            }

            RecolorManager.getInstance().handleAreaSelection(areaName);
            return 1;
        } catch (Exception e) {
            AreashintClient.LOGGER.error(I18nManager.translate("command.message.general_24"), e);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.general_3") + e.getMessage()));
            }
            return 0;
        }
    }

    /**
     * 执行颜色选择
     */
    private static int executeColor(CommandContext<FabricClientCommandSource> context, String colorValue) {
        try {
            RecolorManager.getInstance().handleColorSelection(colorValue);
            return 1;
        } catch (Exception e) {
            AreashintClient.LOGGER.error(I18nManager.translate("command.message.general_22"), e);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.general_3") + e.getMessage()));
            }
            return 0;
        }
    }

    /**
     * 执行确认
     */
    private static int executeConfirm(CommandContext<FabricClientCommandSource> context) {
        try {
            RecolorManager.getInstance().confirmChange();
            return 1;
        } catch (Exception e) {
            AreashintClient.LOGGER.error(I18nManager.translate("command.message.general_23"), e);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.general_3") + e.getMessage()));
            }
            return 0;
        }
    }

    /**
     * 执行取消
     */
    private static int executeCancel(CommandContext<FabricClientCommandSource> context) {
        try {
            RecolorManager.getInstance().cancelRecolor();
            return 1;
        } catch (Exception e) {
            AreashintClient.LOGGER.error(I18nManager.translate("command.message.general_21"), e);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.error.general_3") + e.getMessage()));
            }
            return 0;
        }
    }
}
