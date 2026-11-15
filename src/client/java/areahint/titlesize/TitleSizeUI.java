package areahint.titlesize;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * TitleSize用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class TitleSizeUI {

    /**
     * 显示大小选择界面
     * @param currentSize 当前大小
     */
    public static void showSizeSelectionScreen(String currentSize) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.general")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_8") + getSizeDisplayName(currentSize)), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.general_3")), false);
        client.player.sendMessage(Text.of(""), false);

        // 第一行按钮：极大、大、较大、中
        MutableText row1 = Text.empty()
            .append(createSizeButton(I18nManager.translate("message.message.general_193"), "extra_large", "§d"))
            .append(Text.of("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_104"), "large", "§b"))
            .append(Text.of("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_225"), "medium_large", "§a"))
            .append(Text.of("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_58"), "medium", "§e"));

        // 第二行按钮：较小、小、极小
        MutableText row2 = Text.empty()
            .append(createSizeButton(I18nManager.translate("message.message.general_226"), "medium_small", "§6"))
            .append(Text.of("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_111"), "small", "§c"))
            .append(Text.of("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_194"), "extra_small", "§4"));

        // 取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlesize cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_4"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(row1, false);
        client.player.sendMessage(row2, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(cancelButton, false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.general")), false);
    }

    /**
     * 创建大小选择按钮
     */
    private static MutableText createSizeButton(String displayName, String sizeValue, String colorCode) {
        return Text.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint titlesize select " + sizeValue))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("gui.message.general_2")))));
    }

    /**
     * 获取大小的显示名称
     */
    private static String getSizeDisplayName(String size) {
        switch (size) {
            case "extra_large":
                return I18nManager.translate("message.message.general_193");
            case "large":
                return I18nManager.translate("message.message.general_104");
            case "medium_large":
                return I18nManager.translate("message.message.general_225");
            case "medium":
                return I18nManager.translate("message.message.general_58");
            case "medium_small":
                return I18nManager.translate("message.message.general_226");
            case "small":
                return I18nManager.translate("message.message.general_111");
            case "extra_small":
                return I18nManager.translate("message.message.general_194");
            default:
                return size;
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
