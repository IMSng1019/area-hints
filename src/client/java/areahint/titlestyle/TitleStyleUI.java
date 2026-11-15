package areahint.titlestyle;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * TitleStyle用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class TitleStyleUI {

    /**
     * 显示样式选择界面
     * @param currentStyle 当前样式
     */
    public static void showStyleSelectionScreen(String currentStyle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.general_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_9") + getStyleDisplayName(currentStyle)), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.general_4")), false);
        client.player.sendMessage(Text.of(""), false);

        // 创建样式选择按钮
        MutableText fullButton = Text.literal(I18nManager.translate("gui.button.general_3"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select full"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.message.area_6"))))
                .withColor(Formatting.AQUA));

        MutableText simpleButton = Text.literal(I18nManager.translate("gui.button.general_5"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select simple"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.message.area_5"))))
                .withColor(Formatting.YELLOW));

        MutableText mixedButton = Text.literal(I18nManager.translate("gui.button.general_4"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select mixed"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.message.area_7"))))
                .withColor(Formatting.LIGHT_PURPLE));

        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_6"))))
                .withColor(Formatting.RED));

        // 组合按钮显示
        MutableText buttonRow = Text.empty()
            .append(fullButton)
            .append(Text.of("  "))
            .append(simpleButton)
            .append(Text.of("  "))
            .append(mixedButton)
            .append(Text.of("  "))
            .append(cancelButton);

        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.general_2")), false);
    }

    /**
     * 获取样式的显示名称
     */
    private static String getStyleDisplayName(String style) {
        switch (style) {
            case "full":
                return I18nManager.translate("message.message.general_106");
            case "simple":
                return I18nManager.translate("message.message.general_215");
            case "mixed":
                return I18nManager.translate("message.message.general_209");
            default:
                return style;
        }
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
