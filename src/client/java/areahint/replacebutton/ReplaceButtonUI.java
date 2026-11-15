package areahint.replacebutton;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * ReplaceButton用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class ReplaceButtonUI {

    /**
     * 显示等待按键界面
     */
    public static void showWaitingForKeyScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.record.key")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.key_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.hint.confirm")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_5")), false);

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.key"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String keyName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.confirm.key")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.key") + keyName), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.vertex.record")), false);

        // 显示确认和取消按钮
        MutableText confirmButton = Text.literal(I18nManager.translate("gui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.confirm.key"))))
                .withColor(Formatting.GREEN));

        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacebutton cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.key"))))
                .withColor(Formatting.RED));

        MutableText buttonRow = Text.empty()
            .append(confirmButton)
            .append(Text.of("  "))
            .append(cancelButton);

        client.player.sendMessage(buttonRow, false);
    }

    /**
     * 显示错误消息
     */
    public static void showError(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.error.general_2") + message), false);
        }
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of("§a" + message), false);
        }
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of("§7" + message), false);
        }
    }
}
