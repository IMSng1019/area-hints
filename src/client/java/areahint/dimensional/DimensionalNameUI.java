package areahint.dimensional;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * 维度域名交互UI
 */
public class DimensionalNameUI {

    /**
     * 显示维度选择界面
     * @param commandPrefix "dimname" 或 "dimcolor"
     */
    public static void showDimensionSelectionScreen(String commandPrefix) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String titleKey = commandPrefix.equals("dimname") ? "gui.title.area.dimension.modify" : "gui.title.area.color.dimension.modify";
        String cmdBase = commandPrefix.equals("dimname") ? "dimensionalityname" : "dimensionalitycolor";

        client.player.sendMessage(Text.of(I18nManager.translate(titleKey)), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.dimension")), false);
        client.player.sendMessage(Text.of(""), false);

        Map<String, String> names = ClientDimensionalNameManager.getAllDimensionalNames();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String dimId = entry.getKey();
            String dimName = entry.getValue();
            String color = ClientDimensionalNameManager.getDimensionalColor(dimId);
            String colorDisplay = color != null ? color : I18nManager.translate("gui.message.general_10");

            String label;
            if (commandPrefix.equals("dimcolor")) {
                label = String.format(I18nManager.translate("gui.button.color"), dimName, dimId, colorDisplay);
            } else {
                label = String.format("§6[%s] §7(%s)", dimName, dimId);
            }

            MutableText btn = Text.literal(label)
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + cmdBase + " select \"" + dimId + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Text.of(I18nManager.translate("addhint.prompt.general") + dimName)))
                    .withColor(Formatting.GOLD));
            client.player.sendMessage(btn, false);
        }

        client.player.sendMessage(Text.of(""), false);
        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + cmdBase + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    /**
     * 显示名称输入提示界面
     */
    public static void showNameInputScreen(String dimensionId, String currentName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.dimension")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.dimension_2") + dimensionId), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.name_3") + currentName), false);
        client.player.sendMessage(Text.of(""), false);

        // 提供一个可点击的建议命令
        MutableText inputHint = Text.literal(I18nManager.translate("gui.prompt.name_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint dimensionalityname name "))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.prompt.name_3")))));
        client.player.sendMessage(inputHint, false);

        client.player.sendMessage(Text.of(""), false);
        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    /**
     * 显示名称确认界面
     */
    public static void showNameConfirmScreen(String dimensionId, String oldName, String newName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.dimension.confirm")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.dimension.confirm")), false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.dimension_2") + dimensionId), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.name") + oldName), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.name_4") + newName), false);
        client.player.sendMessage(Text.of(""), false);

        MutableText confirmBtn = Text.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.confirm.modify"))))
                .withColor(Formatting.GREEN));

        MutableText cancelBtn = Text.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(Text.empty().append(confirmBtn).append(Text.of("  ")).append(cancelBtn), false);
    }

    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen(String dimensionId, String dimName, String currentColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.color.dimension_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.dimension_2") + dimName + " §7(" + dimensionId + ")"), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.color_4") + currentColor), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.color")), false);
        client.player.sendMessage(Text.of(""), false);

        // 颜色按钮行
        showColorRows(client);

        client.player.sendMessage(Text.of(""), false);
        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    private static void showColorRows(MinecraftClient client) {
        MutableText row1 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));

        MutableText row2 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_27"), "#FF5555", "§c"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));

        MutableText row3 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));

        MutableText row4 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));

        MutableText row5 = Text.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Text.of("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        client.player.sendMessage(row1, false);
        client.player.sendMessage(row2, false);
        client.player.sendMessage(row3, false);
        client.player.sendMessage(row4, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_4")), false);
        client.player.sendMessage(row5, false);

        client.player.sendMessage(Text.of(""), false);
        MutableText customBtn = Text.literal(I18nManager.translate("gui.button.color_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint dimensionalitycolor color #"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.prompt.color_2")))));
        client.player.sendMessage(customBtn, false);
    }

    private static MutableText colorBtn(String name, String value, String mcColor) {
        return Text.literal(mcColor + "[" + name + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor color " + value))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("addhint.prompt.general") + name))));
    }

    /**
     * 显示颜色确认界面
     */
    public static void showColorConfirmScreen(String dimensionId, String dimName,
                                               String oldColor, String newColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.color.dimension")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.color.dimension")), false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.dimension_2") + dimName + " §7(" + dimensionId + ")"), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.color_2") + oldColor), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.color_5") + newColor), false);
        client.player.sendMessage(Text.of(""), false);

        MutableText confirmBtn = Text.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.confirm.modify"))))
                .withColor(Formatting.GREEN));

        MutableText cancelBtn = Text.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(Text.empty().append(confirmBtn).append(Text.of("  ")).append(cancelBtn), false);
    }

    /**
     * 显示首次维度命名提示
     */
    public static void showFirstNamePrompt(String dimensionId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.dimension")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.dimension") + dimensionId), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.dimension.name")), false);
        client.player.sendMessage(Text.of(""), false);

        MutableText inputBtn = Text.literal(I18nManager.translate("gui.prompt.name"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint firstdimname "))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.prompt.dimension.name_2")))));
        client.player.sendMessage(inputBtn, false);

        client.player.sendMessage(Text.of(""), false);
        MutableText skipBtn = Text.literal(I18nManager.translate("gui.button.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint firstdimname_skip"))
                .withHoverEvent(TextEventCompat.showText(
                    Text.of(I18nManager.translate("gui.message.name_5")))));
        client.player.sendMessage(skipBtn, false);
    }
}
