package areahint.recolor;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Recolor用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class RecolorUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectionScreen(List<AreaData> areas) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("message.title.area.color.modify")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("message.prompt.area.color.modify")), false);
        client.player.sendMessage(Text.of(""), false);

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String currentColor = area.getColor() != null ? area.getColor() : "#FFFFFF";

            // 创建域名选择按钮
            MutableText areaButton = Text.literal(
                String.format(I18nManager.translate("message.button.color.level"), displayName, area.getLevel(), currentColor)
            ).setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint recolor select \"" + area.getName() + "\""))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("message.message.color.modify"))))
                .withColor(Formatting.GOLD));

            client.player.sendMessage(areaButton, false);
        }

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen(String areaName, String currentColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.color")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.name_2") + areaName), false);
        client.player.sendMessage(Text.of(I18nManager.translate("message.message.color_3") + currentColor), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.color")), false);
        client.player.sendMessage(Text.of(""), false);

        // 第一行颜色按钮
        MutableText row1 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));

        // 第二行颜色按钮
        MutableText row2 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_27"), "#FF5555", "§c"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));

        // 第三行颜色按钮
        MutableText row3 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));

        // 第四行颜色按钮
        MutableText row4 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));

        // 闪烁效果按钮行
        MutableText row5 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        // 取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(row1, false);
        client.player.sendMessage(row2, false);
        client.player.sendMessage(row3, false);
        client.player.sendMessage(row4, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_4")), false);
        client.player.sendMessage(row5, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String areaName, String oldColor, String newColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.color.confirm.modify")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.color.confirm")), false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.name_2") + areaName), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.color_3") + oldColor), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.color_6") + newColor), false);
        client.player.sendMessage(Text.of(""), false);

        // 显示确认和取消按钮
        MutableText confirmButton = Text.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.color.confirm.modify"))))
                .withColor(Formatting.GREEN));

        MutableText cancelButton = Text.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint recolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(Formatting.RED));

        MutableText buttonRow = Text.empty()
            .append(confirmButton)
            .append(Text.of("  "))
            .append(cancelButton);

        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.confirm")), false);
    }

    /**
     * 创建颜色选择按钮
     */
    private static MutableText createColorButton(String colorName, String colorValue, String minecraftColor) {
        return Text.literal(minecraftColor + "[" + colorName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint recolor color " + colorValue))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("addhint.prompt.general") + colorName + I18nManager.translate("gui.message.color")))));
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
}
