package areahint.dimensional;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String titleKey = commandPrefix.equals("dimname") ? "gui.title.area.dimension.modify" : "gui.title.area.color.dimension.modify";
        String cmdBase = commandPrefix.equals("dimname") ? "dimensionalityname" : "dimensionalitycolor";

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate(titleKey)));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.dimension")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

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

            MutableComponent btn = Component.literal(label)
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + cmdBase + " select \"" + dimId + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + dimName)))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(btn);
        }

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + cmdBase + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    /**
     * 显示名称输入提示界面
     */
    public static void showNameInputScreen(String dimensionId, String currentName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.dimension")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.dimension_2") + dimensionId));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.name_3") + currentName));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 提供一个可点击的建议命令
        MutableComponent inputHint = Component.literal(I18nManager.translate("gui.prompt.name_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint dimensionalityname name "))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.prompt.name_3")))));
        client.player.sendSystemMessage(inputHint);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    /**
     * 显示名称确认界面
     */
    public static void showNameConfirmScreen(String dimensionId, String oldName, String newName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.dimension.confirm")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.dimension.confirm")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.dimension_2") + dimensionId));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.name") + oldName));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.name_4") + newName));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        MutableComponent confirmBtn = Component.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.confirm.modify"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalityname cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.nullToEmpty("  ")).append(cancelBtn));
    }

    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen(String dimensionId, String dimName, String currentColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.color.dimension_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.dimension_2") + dimName + " §7(" + dimensionId + ")"));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.color_4") + currentColor));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 颜色按钮行
        showColorRows(client);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_5"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    private static void showColorRows(Minecraft client) {
        MutableComponent row1 = Component.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));

        MutableComponent row2 = Component.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_27"), "#FF5555", "§c"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));

        MutableComponent row3 = Component.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));

        MutableComponent row4 = Component.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));

        MutableComponent row5 = Component.empty()
            .append(colorBtn(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(colorBtn(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        client.player.sendSystemMessage(row1);
        client.player.sendSystemMessage(row2);
        client.player.sendSystemMessage(row3);
        client.player.sendSystemMessage(row4);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_4")));
        client.player.sendSystemMessage(row5);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent customBtn = Component.literal(I18nManager.translate("gui.button.color_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint dimensionalitycolor color #"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.prompt.color_2")))));
        client.player.sendSystemMessage(customBtn);
    }

    private static MutableComponent colorBtn(String name, String value, String mcColor) {
        return Component.literal(mcColor + "[" + name + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor color " + value))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + name))));
    }

    /**
     * 显示颜色确认界面
     */
    public static void showColorConfirmScreen(String dimensionId, String dimName,
                                               String oldColor, String newColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.color.dimension")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.color.dimension")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.dimension_2") + dimName + " §7(" + dimensionId + ")"));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.color_2") + oldColor));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.color_5") + newColor));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        MutableComponent confirmBtn = Component.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.confirm.modify"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint dimensionalitycolor cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.modify"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.nullToEmpty("  ")).append(cancelBtn));
    }

    /**
     * 显示首次维度命名提示
     */
    public static void showFirstNamePrompt(String dimensionId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.dimension")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.dimension") + dimensionId));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.dimension.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        MutableComponent inputBtn = Component.literal(I18nManager.translate("gui.prompt.name"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.suggestCommand(
                    "/areahint firstdimname "))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.prompt.dimension.name_2")))));
        client.player.sendSystemMessage(inputBtn);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent skipBtn = Component.literal(I18nManager.translate("gui.button.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint firstdimname_skip"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.message.name_5")))));
        client.player.sendSystemMessage(skipBtn);
    }
}
