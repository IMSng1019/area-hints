package areahint.subtitle;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ColorUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * replacesubtitlecolor 图形流程控制器，只把 Screen 输入交给现有副字幕颜色状态机。
 */
public final class ReplaceSubtitleColorVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private ReplaceSubtitleColorVisualController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!startingFromVisualCommand) {
                return;
            }
            visualStartTicksRemaining--;
            if (visualStartTicksRemaining <= 0) {
                clear();
            }
        });
        registered = true;
    }

    public static void openFromCommandUi(Screen parent) {
        parentScreen = parent;
        startingFromVisualCommand = true;
        visualStartTicksRemaining = VISUAL_START_TIMEOUT_TICKS;
        CommandUiActions.runCommandAndClose(parent, "areahint replacesubtitlecolor");
    }

    public static boolean consumeVisualStartRequest() {
        boolean requested = startingFromVisualCommand;
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        return requested;
    }

    public static void clear() {
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        parentScreen = null;
    }

    public static void showAreaSelection(List<AreaData> areas) {
        if (areas == null || areas.isEmpty()) {
            showInfo("subtitle.manager.error.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.replacesubtitlecolor.title",
            "commandui.replacesubtitlecolor.prompt",
            areaItems(areas),
            area -> SubtitleManager.getInstance().handleColorAreaSelection(area.getName()),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showColorSelection(AreaData area) {
        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.replacesubtitlecolor.title",
            "commandui.replacesubtitlecolor.color.prompt",
            I18nManager.translate("commandui.replacesubtitlecolor.color.detail",
                areaName(area), currentColor(area), subtitlePreview(area)),
            CommandUiData.colorOptions(
                color -> SubtitleManager.getInstance().handleColorSelection(color),
                () -> showCustomColorScreen(area, null)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showCustomColorScreen(AreaData area, String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.replacesubtitlecolor.color.custom.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.replacesubtitlecolor.color.custom.label",
                "commandui.replacesubtitlecolor.color.custom.placeholder", currentColor(area), 32)),
            "commandui.replacesubtitlecolor.color.custom.prompt",
            I18nManager.translate("commandui.replacesubtitlecolor.color.custom.detail", areaName(area)),
            errorTextOrKey,
            values -> handleCustomColorInput(area, values.isEmpty() ? "" : values.get(0)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showConfirmScreen(AreaData area, String color) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.replacesubtitlecolor.confirm.area", areaName(area)));
        details.add(I18nManager.translate("commandui.replacesubtitlecolor.confirm.subtitle", subtitlePreview(area)));
        details.add(I18nManager.translate("commandui.replacesubtitlecolor.confirm.old_color", currentColor(area)));
        details.add(I18nManager.translate("commandui.replacesubtitlecolor.confirm.new_color", color == null ? "" : color));

        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacesubtitlecolor.title",
            I18nManager.translate("commandui.replacesubtitlecolor.confirm.prompt"),
            details,
            "commandui.button.confirm",
            SubtitleManager.getInstance()::confirmReplaceSubtitleColor,
            SubtitleManager.getInstance()::cancel));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacesubtitlecolor.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static void handleCustomColorInput(AreaData area, String colorInput) {
        if (!isValidColorInput(colorInput)) {
            showCustomColorScreen(area, I18nManager.translate("subtitle.manager.error.invalid_color", colorInput));
            return;
        }

        SubtitleManager.getInstance().handleColorSelection(colorInput);
    }

    private static List<WizardSelectionListScreen.SelectionItem<AreaData>> areaItems(List<AreaData> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        for (AreaData area : areas) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(area,
                AreaDataConverter.getDisplayName(area),
                I18nManager.translate("commandui.replacesubtitlecolor.item.detail",
                    areaName(area), area == null ? "" : area.getLevel(), subtitlePreview(area), currentColor(area))));
        }
        return items;
    }

    private static boolean isValidColorInput(String colorInput) {
        if (colorInput == null) {
            return false;
        }

        String trimmed = colorInput.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        return ColorUtil.isFlashColor(trimmed)
            || trimmed.matches("^#?[0-9A-Fa-f]{6}$")
            || ColorUtil.getColorHex(trimmed) != null;
    }

    private static String areaName(AreaData area) {
        return area == null || area.getName() == null ? "" : area.getName();
    }

    private static String currentColor(AreaData area) {
        return area == null ? "#FFFFFF" : area.getSubtitleColor();
    }

    private static String subtitlePreview(AreaData area) {
        if (area == null || !area.hasSubtitle()) {
            return I18nManager.translate("subtitle.ui.none");
        }
        return area.getSubtitle().replace("\n", " / ").trim();
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
