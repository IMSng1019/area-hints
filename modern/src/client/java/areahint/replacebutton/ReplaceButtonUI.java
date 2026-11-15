package areahint.replacebutton;

import areahint.util.TextEventCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import areahint.i18n.I18nManager;

/**
 * ReplaceButton用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class ReplaceButtonUI {

    /**
     * 显示等待按键界面
     */
    public static void showWaitingForKeyScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.record.key")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.key_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.hint.confirm")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_5")));

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.key"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String keyName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.confirm.key")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.key") + keyName));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.vertex.record")));

        // 显示确认和取消按钮
        MutableComponent confirmButton = Component.literal(I18nManager.translate("gui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.confirm.key"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.key"))))
                .withColor(ChatFormatting.RED));

        MutableComponent buttonRow = Component.empty()
            .append(confirmButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);

        client.player.sendSystemMessage(buttonRow);
    }

    /**
     * 显示错误消息
     */
    public static void showError(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.error.general_2") + message));
        }
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty("§a" + message));
        }
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty("§7" + message));
        }
    }
}
