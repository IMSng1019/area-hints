package areahint.recolor;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Recolor用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class RecolorUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectionScreen(List<AreaData> areas) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.title.area.color.modify")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.prompt.area.color.modify")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String currentColor = area.getColor() != null ? area.getColor() : "#FFFFFF";

            // 创建域名选择按钮
            MutableComponent areaButton = Component.literal(
                String.format(I18nManager.translate("message.button.color.level"), displayName, area.getLevel(), currentColor)
            ).setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint recolor select \"" + area.getName() + "\""))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("message.message.color.modify"))))
                .withColor(ChatFormatting.GOLD));

            client.player.sendSystemMessage(areaButton);
        }

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen(String areaName, String currentColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.name_2") + areaName));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.color_3") + currentColor));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 第一行颜色按钮
        MutableComponent row1 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));

        // 第二行颜色按钮
        MutableComponent row2 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_27"), "#FF5555", "§c"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));

        // 第三行颜色按钮
        MutableComponent row3 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));

        // 第四行颜色按钮
        MutableComponent row4 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));

        // 闪烁效果按钮行
        MutableComponent row5 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        // 取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(row1);
        client.player.sendSystemMessage(row2);
        client.player.sendSystemMessage(row3);
        client.player.sendSystemMessage(row4);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_4")));
        client.player.sendSystemMessage(row5);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String areaName, String oldColor, String newColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.color.confirm.modify")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.color.confirm")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.name_2") + areaName));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.color_3") + oldColor));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.color_6") + newColor));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 显示确认和取消按钮
        MutableComponent confirmButton = Component.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.color.confirm.modify"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(ChatFormatting.RED));

        MutableComponent buttonRow = Component.empty()
            .append(confirmButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);

        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.confirm")));
    }

    /**
     * 创建颜色选择按钮
     */
    private static MutableComponent createColorButton(String colorName, String colorValue, String minecraftColor) {
        return Component.literal(minecraftColor + "[" + colorName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint recolor color " + colorValue))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + colorName + I18nManager.translate("gui.message.color")))));
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
}
