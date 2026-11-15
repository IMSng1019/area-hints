package areahint.subtitle;

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
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(getAreaSelectionTitle(mode)));
        client.player.sendSystemMessage(Component.literal(getAreaSelectionPrompt(mode)));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String commandPrefix = getCommandPrefix(mode);
            String subtitlePreview = area.hasSubtitle() ? area.getSubtitle().replace("\n", " / ") : tr("subtitle.ui.none");
            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint " + commandPrefix + " select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.literal(tr("subtitle.ui.area.hover", area.getName(), area.getLevel(), subtitlePreview))))
                    .withColor(ChatFormatting.GOLD));

            client.player.sendSystemMessage(areaButton);
        }

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(createCancelButton(getCommandPrefix(mode)));
    }

    public static void showSubtitleInputScreen(AreaData area) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.add.title")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        if (area.hasSubtitle()) {
            client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.current_subtitle", area.getSubtitle().replace("\n", " / "))));
        }
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.add.input")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.add.newline_hint")));
        client.player.sendSystemMessage(createCancelButton("addsubtitle"));
    }

    public static void showAddColorSelectionScreen(AreaData area, String subtitle) {
        showColorSelectionScreen(area, "addsubtitle", subtitle);
    }

    public static void showAddConfirmScreen(AreaData area, String subtitle, String color) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.add.confirm.title")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.subtitle", subtitle.replace("\n", " / "))));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.new_color", color)));

        MutableComponent confirmButton = Component.literal(tr("subtitle.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint addsubtitle confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.save_subtitle"))))
                .withColor(ChatFormatting.GREEN));

        client.player.sendSystemMessage(buttonRow(confirmButton, createCancelButton("addsubtitle")));
    }

    public static void showDeleteConfirmScreen(AreaData area) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.delete.confirm.title")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.current_subtitle", area.getSubtitle().replace("\n", " / "))));

        MutableComponent confirmButton = Component.literal(tr("subtitle.ui.button.delete_subtitle"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint deletesubtitle confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.delete_subtitle"))))
                .withColor(ChatFormatting.RED)
                .withBold(true));

        client.player.sendSystemMessage(buttonRow(confirmButton, createCancelButton("deletesubtitle")));
    }

    public static void showColorSelectionScreen(AreaData area) {
        showColorSelectionScreen(area, "replacesubtitlecolor", null);
    }

    private static void showColorSelectionScreen(AreaData area, String commandPrefix, String pendingSubtitle) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.color.title")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        if (pendingSubtitle != null) {
            client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.subtitle", pendingSubtitle.replace("\n", " / "))));
        }
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.current_color", area.getSubtitleColor())));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        client.player.sendSystemMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.white", "#FFFFFF", "§f"), colorButton(commandPrefix, "subtitle.ui.color.gray", "#808080", "§7"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_gray", "#555555", "§8"), colorButton(commandPrefix, "subtitle.ui.color.black", "#000000", "§0")));
        client.player.sendSystemMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.dark_red", "#AA0000", "§4"), colorButton(commandPrefix, "subtitle.ui.color.red", "#FF5555", "§c"),
            colorButton(commandPrefix, "subtitle.ui.color.pink", "#FF55FF", "§d"), colorButton(commandPrefix, "subtitle.ui.color.gold", "#FFAA00", "§6")));
        client.player.sendSystemMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.yellow", "#FFFF55", "§e"), colorButton(commandPrefix, "subtitle.ui.color.green", "#55FF55", "§a"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_green", "#00AA00", "§2"), colorButton(commandPrefix, "subtitle.ui.color.aqua", "#55FFFF", "§b")));
        client.player.sendSystemMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.dark_aqua", "#00AAAA", "§3"), colorButton(commandPrefix, "subtitle.ui.color.blue", "#5555FF", "§9"),
            colorButton(commandPrefix, "subtitle.ui.color.dark_blue", "#0000AA", "§1"), colorButton(commandPrefix, "subtitle.ui.color.purple", "#AA00AA", "§5")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(colorRow(
            colorButton(commandPrefix, "subtitle.ui.color.flash_bw_all", "FLASH_BW_ALL", "§7"), colorButton(commandPrefix, "subtitle.ui.color.flash_rainbow_all", "FLASH_RAINBOW_ALL", "§b"),
            colorButton(commandPrefix, "subtitle.ui.color.flash_bw_char", "FLASH_BW_CHAR", "§8"), colorButton(commandPrefix, "subtitle.ui.color.flash_rainbow_char", "FLASH_RAINBOW_CHAR", "§d")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(createCancelButton(commandPrefix));
    }

    public static void showColorConfirmScreen(AreaData area, String newColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.color.confirm.title")));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.old_color", area.getSubtitleColor())));
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.new_color", newColor)));

        MutableComponent confirmButton = Component.literal(tr("subtitle.ui.button.confirm"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint replacesubtitlecolor confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.save_color"))))
                .withColor(ChatFormatting.GREEN));

        client.player.sendSystemMessage(buttonRow(confirmButton, createCancelButton("replacesubtitlecolor")));
    }

    public static void showSubtitleSizeSelectionScreen(String currentSize) {
        showSubtitleSizeSelectionScreen("replacesubtitlesize", currentSize, null, null, null);
    }

    private static void showSubtitleSizeSelectionScreen(String commandPrefix, String currentSize,
                                                        AreaData area, String pendingSubtitle, String pendingColor) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.size.title")));
        if (area != null) {
            client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.area", area.getName())));
        }
        if (pendingSubtitle != null) {
            client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.subtitle", pendingSubtitle.replace("\n", " / "))));
        }
        if (pendingColor != null) {
            client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.new_color", pendingColor)));
        }
        client.player.sendSystemMessage(Component.literal(tr("subtitle.ui.field.current_size", getSizeDisplayName(currentSize))));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        MutableComponent row0 = Component.empty()
            .append(sizeButton(commandPrefix, "auto", "§f"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "extra_large", "§d"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "large", "§b"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "medium_large", "§a"));

        MutableComponent row1 = Component.empty()
            .append(sizeButton(commandPrefix, "medium", "§e"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "medium_small", "§6"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "small", "§c"))
            .append(Component.nullToEmpty("  "))
            .append(sizeButton(commandPrefix, "extra_small", "§4"));

        client.player.sendSystemMessage(row0);
        client.player.sendSystemMessage(row1);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(createCancelButton(commandPrefix));
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

    private static MutableComponent colorRow(MutableComponent first, MutableComponent second, MutableComponent third, MutableComponent fourth) {
        return Component.empty()
            .append(first).append(Component.nullToEmpty("  "))
            .append(second).append(Component.nullToEmpty("  "))
            .append(third).append(Component.nullToEmpty("  "))
            .append(fourth);
    }

    private static MutableComponent colorButton(String commandPrefix, String displayNameKey, String colorValue, String colorCode) {
        String displayName = tr(displayNameKey);
        return Component.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " color " + colorValue))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.select_color", displayName)))));
    }

    private static MutableComponent sizeButton(String commandPrefix, String sizeValue, String colorCode) {
        String displayName = getSizeDisplayName(sizeValue);
        String subCommand = "replacesubtitlesize".equals(commandPrefix) ? "select" : "size";
        return Component.literal(colorCode + "[" + displayName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(
                    "/areahint " + commandPrefix + " " + subCommand + " " + sizeValue))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.select_size", displayName)))));
    }

    private static MutableComponent createCancelButton(String commandPrefix) {
        return Component.literal(tr("subtitle.ui.button.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint " + commandPrefix + " cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.literal(tr("subtitle.ui.hover.cancel"))))
                .withColor(ChatFormatting.RED));
    }

    private static MutableComponent buttonRow(MutableComponent first, MutableComponent second) {
        return Component.empty().append(first).append(Component.nullToEmpty("  ")).append(second);
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
