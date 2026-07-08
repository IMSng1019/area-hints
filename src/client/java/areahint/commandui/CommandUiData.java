package areahint.commandui;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.data.ConfigData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import areahint.util.ColorUtil;
import areahint.world.ClientWorldFolderManager;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 指令图形流程的数据工具，集中处理域名读取、参数转义和通用选项。
 */
public final class CommandUiData {
    private CommandUiData() {
    }

    public static List<AreaData> loadCurrentDimensionAreas() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return List.of();
        }

        try {
            String fileName = getFileNameForDimension(client.world.getRegistryKey().getValue().toString());
            if (fileName == null) {
                return List.of();
            }
            Path path = ClientWorldFolderManager.getWorldDimensionFile(fileName);
            if (!path.toFile().exists()) {
                return List.of();
            }
            return FileManager.readAreaData(path);
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<WizardSelectionListScreen.SelectionItem<AreaData>> areaItems(List<AreaData> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String detail = ClientAreaText.areaDetail(area);
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, displayName, detail));
        }
        return items;
    }

    public static List<AreaData> validAreas(List<AreaData> areas) {
        List<AreaData> validAreas = new ArrayList<>();
        for (AreaData area : areas) {
            if (area != null && area.isValid()) {
                validAreas.add(area);
            }
        }
        return validAreas;
    }

    public static String quote(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    public static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    public static String getFileNameForDimension(String dimensionId) {
        if (dimensionId == null) {
            return null;
        }
        if (dimensionId.contains("overworld")) {
            return Areashint.OVERWORLD_FILE;
        }
        if (dimensionId.contains("nether")) {
            return Areashint.NETHER_FILE;
        }
        if (dimensionId.contains("end")) {
            return Areashint.END_FILE;
        }
        return null;
    }

    public static List<WizardOptionScreen.OptionSpec> colorOptions(java.util.function.Consumer<String> colorAction,
                                                                   Runnable customAction) {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        addColor(options, "commandui.easyadd.color.white", "#FFFFFF", colorAction);
        addColor(options, "commandui.easyadd.color.gray", "#808080", colorAction);
        addColor(options, "commandui.easyadd.color.dark_gray", "#555555", colorAction);
        addColor(options, "commandui.easyadd.color.black", "#000000", colorAction);
        addColor(options, "commandui.easyadd.color.dark_red", "#AA0000", colorAction);
        addColor(options, "commandui.easyadd.color.red", "#FF0000", colorAction);
        addColor(options, "commandui.easyadd.color.pink", "#FF55FF", colorAction);
        addColor(options, "commandui.easyadd.color.gold", "#FFAA00", colorAction);
        addColor(options, "commandui.easyadd.color.yellow", "#FFFF55", colorAction);
        addColor(options, "commandui.easyadd.color.green", "#55FF55", colorAction);
        addColor(options, "commandui.easyadd.color.dark_green", "#00AA00", colorAction);
        addColor(options, "commandui.easyadd.color.aqua", "#55FFFF", colorAction);
        addColor(options, "commandui.easyadd.color.dark_aqua", "#00AAAA", colorAction);
        addColor(options, "commandui.easyadd.color.blue", "#5555FF", colorAction);
        addColor(options, "commandui.easyadd.color.dark_blue", "#0000AA", colorAction);
        addColor(options, "commandui.easyadd.color.purple", "#AA00AA", colorAction);
        addFlash(options, "commandui.easyadd.color.flash_bw_all", ColorUtil.FLASH_BW_ALL, colorAction);
        addFlash(options, "commandui.easyadd.color.flash_rainbow_all", ColorUtil.FLASH_RAINBOW_ALL, colorAction);
        addFlash(options, "commandui.easyadd.color.flash_bw_char", ColorUtil.FLASH_BW_CHAR, colorAction);
        addFlash(options, "commandui.easyadd.color.flash_rainbow_char", ColorUtil.FLASH_RAINBOW_CHAR, colorAction);
        options.add(new WizardOptionScreen.OptionSpec("commandui.easyadd.color.custom",
            "commandui.easyadd.color.custom.detail", -1, customAction));
        return options;
    }

    public static List<WizardOptionScreen.OptionSpec> sizeOptions(java.util.function.Consumer<String> sizeAction,
                                                                  boolean includeAuto,
                                                                  Runnable customAction) {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        if (includeAuto) {
            options.add(new WizardOptionScreen.OptionSpec("commandui.common.size.auto",
                "commandui.common.size.auto.detail", -1, () -> sizeAction.accept("auto")));
        }
        addSize(options, "commandui.common.size.extra_large", "extra_large", sizeAction);
        addSize(options, "commandui.common.size.large", "large", sizeAction);
        addSize(options, "commandui.common.size.medium_large", "medium_large", sizeAction);
        addSize(options, "commandui.common.size.medium", "medium", sizeAction);
        addSize(options, "commandui.common.size.medium_small", "medium_small", sizeAction);
        addSize(options, "commandui.common.size.small", "small", sizeAction);
        addSize(options, "commandui.common.size.extra_small", "extra_small", sizeAction);
        options.add(new WizardOptionScreen.OptionSpec("commandui.common.size.custom",
            "commandui.common.size.custom.detail", -1, customAction));
        return options;
    }

    public static String normalizeCustomSize(String input) {
        try {
            float scale = Float.parseFloat(input.trim());
            return ConfigData.formatCustomSize(scale);
        } catch (Exception e) {
            return null;
        }
    }

    private static void addColor(List<WizardOptionScreen.OptionSpec> options, String labelKey, String color,
                                 java.util.function.Consumer<String> colorAction) {
        int rgb = Integer.parseInt(color.substring(1), 16);
        options.add(new WizardOptionScreen.OptionSpec(labelKey, "", rgb, () -> colorAction.accept(color)));
    }

    private static void addFlash(List<WizardOptionScreen.OptionSpec> options, String labelKey, String color,
                                 java.util.function.Consumer<String> colorAction) {
        options.add(new WizardOptionScreen.OptionSpec(labelKey, "", 0xFFFFFF, () -> colorAction.accept(color)));
    }

    private static void addSize(List<WizardOptionScreen.OptionSpec> options, String labelKey, String size,
                                java.util.function.Consumer<String> sizeAction) {
        options.add(new WizardOptionScreen.OptionSpec(labelKey, "", -1, () -> sizeAction.accept(size)));
    }
}
