package areahint.titlestyle;

import areahint.util.TextEventCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import areahint.i18n.I18nManager;

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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.general_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_9") + getStyleDisplayName(currentStyle)));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.general_4")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 创建样式选择按钮
        MutableComponent fullButton = Component.literal(I18nManager.translate("gui.button.general_3"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select full"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.message.area_6"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent simpleButton = Component.literal(I18nManager.translate("gui.button.general_5"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select simple"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.message.area_5"))))
                .withColor(ChatFormatting.YELLOW));

        MutableComponent mixedButton = Component.literal(I18nManager.translate("gui.button.general_4"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle select mixed"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.message.area_7"))))
                .withColor(ChatFormatting.LIGHT_PURPLE));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint titlestyle cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_6"))))
                .withColor(ChatFormatting.RED));

        // 组合按钮显示
        MutableComponent buttonRow = Component.empty()
            .append(fullButton)
            .append(Component.nullToEmpty("  "))
            .append(simpleButton)
            .append(Component.nullToEmpty("  "))
            .append(mixedButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);

        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.general_2")));
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
