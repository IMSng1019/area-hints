package areahint.titlesize;

import areahint.util.TextEventCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import areahint.i18n.I18nManager;

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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.general")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_8") + getSizeDisplayName(currentSize)));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.general_3")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 第一行按钮：极大、大、较大、中
        MutableComponent row1 = Component.empty()
            .append(createSizeButton(I18nManager.translate("message.message.general_193"), "extra_large", "§d"))
            .append(Component.nullToEmpty("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_104"), "large", "§b"))
            .append(Component.nullToEmpty("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_225"), "medium_large", "§a"))
            .append(Component.nullToEmpty("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_58"), "medium", "§e"));

        // 第二行按钮：较小、小、极小
        MutableComponent row2 = Component.empty()
            .append(createSizeButton(I18nManager.translate("message.message.general_226"), "medium_small", "§6"))
            .append(Component.nullToEmpty("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_111"), "small", "§c"))
            .append(Component.nullToEmpty("  "))
            .append(createSizeButton(I18nManager.translate("message.message.general_194"), "extra_small", "§4"));

        // 取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlesize cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_4"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(row1);
        client.player.sendSystemMessage(row2);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(cancelButton);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.general")));
    }

    /**
     * 创建大小选择按钮
     */
    private static MutableComponent createSizeButton(String displayName, String sizeValue, String colorCode) {
        return Component.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint titlesize select " + sizeValue))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("gui.message.general_2")))));
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
