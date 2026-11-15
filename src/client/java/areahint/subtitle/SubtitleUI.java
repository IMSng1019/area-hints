package areahint.subtitle;

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
 * 副字幕聊天交互界面。
 * <p>
 * 与现有 Delete/Recolor/TitleSize 一样使用聊天按钮，让玩家在不打开额外屏幕的情况下完成编辑。
 * <p>
 * 其中 addsubtitle 的正文输入阶段额外支持直接在聊天框里输入文本，
 * 以贴近 EasyAdd 那种“进入流程后直接输入内容”的交互习惯。
 */
public class SubtitleUI {
    public enum AreaSelectionMode {
        ADD,
        DELETE,
        COLOR
    }

    public static void showAreaSelectionScreen(List<AreaData> areas, AreaSelectionMode mode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(getAreaSelectionTitle(mode)), false);
        client.player.sendMessage(Text.literal(getAreaSelectionPrompt(mode)), false);
        client.player.sendMessage(Text.of(""), false);

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String commandPrefix = getCommandPrefix(mode);
            String subtitlePreview = area.hasSubtitle() ? area.getSubtitle().replace("\n", " / ") : tr("subtitle.ui.none");
            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Text.literal(tr("subtitle.ui.area.hover", area.getName(), area.getLevel(), subtitlePreview))))
                    .withColor(Formatting.GOLD));

            client.player.sendMessage(areaButton, false);
        }

        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(createCancelButton(getCommandPrefix(mode)), false);
    }

    public static void showSubtitleInputScreen(AreaData area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.add.title")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        if (area.hasSubtitle()) {
            client.player.sendMessage(Text.literal(tr("subtitle.ui.field.current_subtitle", area.getSubtitle().replace("\n", " / "))), false);
        }
        client.player.sendMessage(Text.literal(tr("subtitle.ui.add.input")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.add.newline_hint")), false);
        client.player.sendMessage(createCancelButton("addsubtitle"), false);
    }

    public static void showAddColorSelectionScreen(AreaData area, String subtitle) {
        showColorSelectionScreen(area, "addsubtitle", subtitle);
    }

    public static void showAddConfirmScreen(AreaData area, String subtitle, String color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.add.confirm.title")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.subtitle", subtitle.replace("\n", " / "))), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.new_color", color)), false);

        MutableText confirmButton = Text.literal(tr("subtitle.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addsubtitle confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.save_subtitle"))))
                .withColor(Formatting.GREEN));

        client.player.sendMessage(buttonRow(confirmButton, createCancelButton("addsubtitle")), false);
    }

    public static void showDeleteConfirmScreen(AreaData area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.delete.confirm.title")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.current_subtitle", area.getSubtitle().replace("\n", " / "))), false);

        MutableText confirmButton = Text.literal(tr("subtitle.ui.button.delete_subtitle"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint deletesubtitle confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.delete_subtitle"))))
                .withColor(Formatting.RED)
                .withBold(true));

        client.player.sendMessage(buttonRow(confirmButton, createCancelButton("deletesubtitle")), false);
    }

    public static void showColorSelectionScreen(AreaData area) {
        showColorSelectionScreen(area, "replacesubtitlecolor", null);
    }

    private static void showColorSelectionScreen(AreaData area, String commandPrefix, String pendingSubtitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.color.title")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        if (pendingSubtitle != null) {
            client.player.sendMessage(Text.literal(tr("subtitle.ui.field.subtitle", pendingSubtitle.replace("\n", " / "))), false);
        }
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.current_color", area.getSubtitleColor())), false);
        client.player.sendMessage(Text.of(""), false);

        client.player.sendMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.white", "#FFFFFF", "§f"), colorButton(commandPrefix, "subtitle.ui.color.gray", "#808080", "§7"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_gray", "#555555", "§8"), colorButton(commandPrefix, "subtitle.ui.color.black", "#000000", "§0")), false);
        client.player.sendMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.dark_red", "#AA0000", "§4"), colorButton(commandPrefix, "subtitle.ui.color.red", "#FF5555", "§c"),
            colorButton(commandPrefix, "subtitle.ui.color.pink", "#FF55FF", "§d"), colorButton(commandPrefix, "subtitle.ui.color.gold", "#FFAA00", "§6")), false);
        client.player.sendMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.yellow", "#FFFF55", "§e"), colorButton(commandPrefix, "subtitle.ui.color.green", "#55FF55", "§a"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_green", "#00AA00", "§2"), colorButton(commandPrefix, "subtitle.ui.color.aqua", "#55FFFF", "§b")), false);
        client.player.sendMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.dark_aqua", "#00AAAA", "§3"), colorButton(commandPrefix, "subtitle.ui.color.blue", "#5555FF", "§9"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_blue", "#0000AA", "§1"), colorButton(commandPrefix, "subtitle.ui.color.purple", "#AA00AA", "§5")), false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.flash_bw_all", "FLASH_BW_ALL", "§7"), colorButton(commandPrefix, "subtitle.ui.color.flash_rainbow_all", "FLASH_RAINBOW_ALL", "§b"),
            colorButton(commandPrefix, "subtitle.ui.color.flash_bw_char", "FLASH_BW_CHAR", "§8"), colorButton(commandPrefix, "subtitle.ui.color.flash_rainbow_char", "FLASH_RAINBOW_CHAR", "§d")), false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(createCancelButton(commandPrefix), false);
    }

    public static void showColorConfirmScreen(AreaData area, String newColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.color.confirm.title")), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.old_color", area.getSubtitleColor())), false);
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.new_color", newColor)), false);

        MutableText confirmButton = Text.literal(tr("subtitle.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacesubtitlecolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.save_color"))))
                .withColor(Formatting.GREEN));

        client.player.sendMessage(buttonRow(confirmButton, createCancelButton("replacesubtitlecolor")), false);
    }

    public static void showSubtitleSizeSelectionScreen(String currentSize) {
        showSubtitleSizeSelectionScreen("replacesubtitlesize", currentSize, null, null, null);
    }

    private static void showSubtitleSizeSelectionScreen(String commandPrefix, String currentSize,
                                                        AreaData area, String pendingSubtitle, String pendingColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(tr("subtitle.ui.size.title")), false);
        if (area != null) {
            client.player.sendMessage(Text.literal(tr("subtitle.ui.field.area", area.getName())), false);
        }
        if (pendingSubtitle != null) {
            client.player.sendMessage(Text.literal(tr("subtitle.ui.field.subtitle", pendingSubtitle.replace("\n", " / "))), false);
        }
        if (pendingColor != null) {
            client.player.sendMessage(Text.literal(tr("subtitle.ui.field.new_color", pendingColor)), false);
        }
        client.player.sendMessage(Text.literal(tr("subtitle.ui.field.current_size", getSizeDisplayName(currentSize))), false);
        client.player.sendMessage(Text.of(""), false);

        MutableText row0 = Text.empty()
            .append(sizeButton(commandPrefix, "auto", "§f"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "extra_large", "§d"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "large", "§b"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "medium_large", "§a"));

        MutableText row1 = Text.empty()
            .append(sizeButton(commandPrefix, "medium", "§e"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "medium_small", "§6"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "small", "§c"))
            .append(Text.of("  "))
            .append(sizeButton(commandPrefix, "extra_small", "§4"));

        client.player.sendMessage(row0, false);
        client.player.sendMessage(row1, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(createCancelButton(commandPrefix), false);
    }

    public static String getSizeDisplayName(String size) {
        switch (size) {
            case "auto":
                return tr("subtitle.ui.size.auto");
            case "extra_large":
                return tr("subtitle.ui.size.extra_large");
            case "large":
                return tr("subtitle.ui.size.large");
            case "medium_large":
                return tr("subtitle.ui.size.medium_large");
            case "medium":
                return tr("subtitle.ui.size.medium");
            case "medium_small":
                return tr("subtitle.ui.size.medium_small");
            case "small":
                return tr("subtitle.ui.size.small");
            case "extra_small":
                return tr("subtitle.ui.size.extra_small");
            default:
                return size;
        }
    }

    private static MutableText colorRow(MutableText first, MutableText second, MutableText third, MutableText fourth) {
        return Text.empty()
            .append(first).append(Text.of("  "))
            .append(second).append(Text.of("  "))
            .append(third).append(Text.of("  "))
            .append(fourth);
    }

    private static MutableText colorButton(String commandPrefix, String displayNameKey, String colorValue, String colorCode) {
        String displayName = tr(displayNameKey);
        return Text.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " color " + colorValue))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.select_color", displayName)))));
    }

    private static MutableText sizeButton(String commandPrefix, String sizeValue, String colorCode) {
        String displayName = getSizeDisplayName(sizeValue);
        String subCommand = "replacesubtitlesize".equals(commandPrefix) ? "select" : "size";
        return Text.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " " + subCommand + " " + sizeValue))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.select_size", displayName)))));
    }

    private static MutableText createCancelButton(String commandPrefix) {
        return Text.literal(tr("subtitle.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.literal(tr("subtitle.ui.hover.cancel"))))
                .withColor(Formatting.RED));
    }

    private static MutableText buttonRow(MutableText first, MutableText second) {
        return Text.empty().append(first).append(Text.of("  ")).append(second);
    }

    private static String getCommandPrefix(AreaSelectionMode mode) {
        switch (mode) {
            case DELETE:
                return "deletesubtitle";
            case COLOR:
                return "replacesubtitlecolor";
            case ADD:
            default:
                return "addsubtitle";
        }
    }

    private static String getAreaSelectionTitle(AreaSelectionMode mode) {
        switch (mode) {
            case DELETE:
                return tr("subtitle.ui.select.title.delete");
            case COLOR:
                return tr("subtitle.ui.select.title.color");
            case ADD:
            default:
                return tr("subtitle.ui.select.title.add");
        }
    }

    private static String getAreaSelectionPrompt(AreaSelectionMode mode) {
        switch (mode) {
            case DELETE:
                return tr("subtitle.ui.select.prompt.delete");
            case COLOR:
                return tr("subtitle.ui.select.prompt.color");
            case ADD:
            default:
                return tr("subtitle.ui.select.prompt.add");
        }
    }

    private static String tr(String key, Object... args) {
        return I18nManager.translate(key, args);
    }
}
