package areahint.dividearea;

import areahint.commandui.CommandUiData;
import areahint.commandui.CommandVisualController;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import areahint.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * dividearea 图形流程控制器，只负责把 Screen 输入转交给现有 DivideAreaManager。
 */
public final class DivideAreaVisualController {
    private static Screen parentScreen;

    private DivideAreaVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        parentScreen = parent;
        DivideAreaManager manager = DivideAreaManager.getInstance();
        List<AreaData> areas = manager.beginVisualSelection();
        if (areas.isEmpty()) {
            showInfo(parent, "commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.dividearea.title",
            "commandui.dividearea.prompt",
            CommandUiData.areaItems(areas),
            area -> {
                closeToGame();
                CommandVisualController.beginVisualRecordMode("dividearea");
                manager.selectAreaByName(area.getName());
            },
            DivideAreaVisualController::cancelFlow));
    }

    public static void openConfig(Screen parent) {
        Screen screenParent = parent != null ? parent : parentScreen;
        DivideAreaManager manager = DivideAreaManager.getInstance();
        switch (manager.getState()) {
            case AREA1_NAME -> openName(screenParent, manager, 1, null);
            case AREA1_SURFACE_NAME -> openSurface(screenParent, manager, 1);
            case AREA1_LEVEL -> openLevel(screenParent, manager, 1);
            case AREA1_BASE -> openBase(screenParent, manager, 1);
            case AREA1_COLOR -> openColor(screenParent, manager, 1);
            case AREA2_NAME -> openName(screenParent, manager, 2, null);
            case AREA2_SURFACE_NAME -> openSurface(screenParent, manager, 2);
            case AREA2_LEVEL -> openLevel(screenParent, manager, 2);
            case AREA2_BASE -> openBase(screenParent, manager, 2);
            case AREA2_COLOR -> openColor(screenParent, manager, 2);
            default -> clearAndClose();
        }
    }

    private static void openName(Screen parent, DivideAreaManager manager, int areaNumber, String errorKey) {
        setScreen(new WizardTextInputScreen(parent,
            "commandui.dividearea.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.dividearea.name.label",
                "commandui.dividearea.name.placeholder", "", 80)),
            I18nManager.translate("commandui.dividearea.name.prompt", areaNumber),
            "commandui.dividearea.name.detail",
            errorKey,
            values -> {
                String name = values.isEmpty() ? "" : values.get(0).trim();
                if (name.isEmpty()) {
                    openName(parent, manager, areaNumber, "commandui.common.error.empty");
                    return;
                }
                if (!manager.handleNameInputForVisual(name)) {
                    openName(parent, manager, areaNumber, "commandui.dividearea.name.error.duplicate");
                    return;
                }
                openSurface(parent, manager, areaNumber);
            },
            DivideAreaVisualController::cancelFlow));
    }

    private static void openSurface(Screen parent, DivideAreaManager manager, int areaNumber) {
        setScreen(new WizardTextInputScreen(parent,
            "commandui.dividearea.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.dividearea.surface.label",
                "commandui.dividearea.surface.placeholder", "", 80)),
            I18nManager.translate("commandui.dividearea.surface.prompt", areaNumber),
            "commandui.dividearea.surface.detail",
            null,
            values -> {
                manager.handleSurfaceInput(values.isEmpty() ? "" : values.get(0));
                openLevel(parent, manager, areaNumber);
            },
            DivideAreaVisualController::cancelFlow));
    }

    private static void openLevel(Screen parent, DivideAreaManager manager, int areaNumber) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            option("commandui.easyadd.level.1", () -> {
                manager.handleLevelInputForVisual(1);
                openConfig(parent);
            }),
            option("commandui.easyadd.level.2", () -> {
                manager.handleLevelInputForVisual(2);
                openConfig(parent);
            }),
            option("commandui.easyadd.level.3", () -> {
                manager.handleLevelInputForVisual(3);
                openConfig(parent);
            })
        );
        setScreen(new WizardOptionScreen(parent,
            "commandui.dividearea.title",
            I18nManager.translate("commandui.dividearea.level.prompt", areaNumber),
            "commandui.dividearea.level.detail",
            options,
            DivideAreaVisualController::cancelFlow));
    }

    private static void openBase(Screen parent, DivideAreaManager manager, int areaNumber) {
        List<AreaData> areas = CommandUiData.loadCurrentDimensionAreas();
        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        items.add(new WizardSelectionListScreen.SelectionItem<>("none",
            I18nManager.translate("commandui.common.none"),
            I18nManager.translate("commandui.dividearea.base.none.detail")));

        int targetLevel = manager.getCurrentBaseTargetLevel();
        for (AreaData area : areas) {
            if (targetLevel > 0 && area.getLevel() != targetLevel) {
                continue;
            }
            items.add(new WizardSelectionListScreen.SelectionItem<>(area.getName(),
                AreaDataConverter.getDisplayName(area),
                ClientAreaText.areaDetail(area)));
        }

        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.dividearea.title",
            I18nManager.translate("commandui.dividearea.base.prompt", areaNumber),
            items,
            baseName -> {
                manager.handleBaseInputForVisual(baseName);
                openConfig(parent);
            },
            DivideAreaVisualController::cancelFlow));
    }

    private static void openColor(Screen parent, DivideAreaManager manager, int areaNumber) {
        setScreen(new WizardOptionScreen(parent,
            "commandui.dividearea.title",
            I18nManager.translate("commandui.dividearea.color.prompt", areaNumber),
            "commandui.color.detail",
            CommandUiData.colorOptions(color -> {
                manager.handleColorInputForVisual(color);
                openConfig(parent);
            }, () -> openCustomColor(parent, manager, areaNumber, null)),
            DivideAreaVisualController::cancelFlow));
    }

    private static void openCustomColor(Screen parent, DivideAreaManager manager, int areaNumber, String errorKey) {
        setScreen(new WizardTextInputScreen(parent,
            "commandui.dividearea.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.color.custom.label",
                "commandui.color.custom.placeholder", "#FFFFFF", 16)),
            "commandui.color.custom.prompt",
            "commandui.color.custom.detail",
            errorKey,
            values -> {
                String color = normalizeStrictColor(values.isEmpty() ? "" : values.get(0));
                if (color == null) {
                    openCustomColor(parent, manager, areaNumber, "commandui.color.error.invalid");
                    return;
                }
                manager.handleColorInputForVisual(color);
                openConfig(parent);
            },
            DivideAreaVisualController::cancelFlow));
    }

    private static void showInfo(Screen parent, String messageKey) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.dividearea.title",
            I18nManager.translate(messageKey),
            List.of(),
            "commandui.button.close",
            DivideAreaVisualController::clear,
            DivideAreaVisualController::clear));
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, Runnable action) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, action);
    }

    private static void cancelFlow() {
        DivideAreaManager.getInstance().cancel();
        clear();
    }

    private static void clearAndClose() {
        clear();
        closeToGame();
    }

    private static void clear() {
        parentScreen = null;
        CommandVisualController.clearVisualRecordMode();
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    private static String normalizeStrictColor(String colorInput) {
        if (colorInput == null || colorInput.trim().isEmpty()) {
            return null;
        }
        String trimmed = colorInput.trim();
        if (ColorUtil.isFlashColor(trimmed)) {
            return trimmed;
        }
        String namedColor = ColorUtil.getColorHex(trimmed);
        if (namedColor != null) {
            return namedColor;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized.matches("^#[0-9A-F]{6}$") ? normalized : null;
    }
}
