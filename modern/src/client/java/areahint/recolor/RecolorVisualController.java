package areahint.recolor;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ColorUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import java.util.Locale;

/**
 * recolor 图形流程控制器，只把 Screen 输入转交给现有 RecolorManager 状态机。
 */
public final class RecolorVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private RecolorVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint recolor");
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

    public static void showAreaSelectionScreen(List<AreaData> areas) {
        if (areas == null || areas.isEmpty()) {
            showInfo("commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.recolor.title",
            "commandui.recolor.prompt",
            CommandUiData.areaItems(areas),
            area -> RecolorManager.getInstance().handleAreaSelection(area.getName()),
            RecolorManager.getInstance()::cancelRecolor));
    }

    public static void showColorSelectionScreen(String areaName, String currentColor) {
        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.recolor.title",
            "commandui.color.prompt",
            "commandui.color.detail",
            CommandUiData.colorOptions(
                color -> RecolorManager.getInstance().handleColorSelection(color),
                () -> showCustomColorScreen(areaName, currentColor, null)),
            RecolorManager.getInstance()::cancelRecolor));
    }

    public static void showCustomColorScreen(String areaName, String currentColor, String errorKey) {
        String initialColor = currentColor == null || ColorUtil.isFlashColor(currentColor) ? "#FFFFFF" : currentColor;
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.recolor.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.color.custom.label",
                "commandui.color.custom.placeholder", initialColor, 32)),
            "commandui.color.custom.prompt",
            "commandui.color.custom.detail",
            errorKey,
            values -> {
                String color = normalizeStrictColor(values.isEmpty() ? "" : values.get(0));
                if (color == null) {
                    showCustomColorScreen(areaName, currentColor, "commandui.color.error.invalid");
                    return;
                }
                RecolorManager.getInstance().handleColorSelection(color);
            },
            RecolorManager.getInstance()::cancelRecolor));
    }

    public static void showConfirmScreen(String areaName, String oldColor, String newColor) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.recolor.title",
            format("commandui.recolor.confirm", areaName, newColor),
            List.of(format("commandui.recolor.old", oldColor)),
            "commandui.button.confirm",
            RecolorManager.getInstance()::confirmChange,
            RecolorManager.getInstance()::cancelRecolor));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.recolor.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            RecolorVisualController::clear,
            RecolorVisualController::clear));
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

    private static String format(String key, Object... args) {
        String template = I18nManager.translate(key);
        try {
            return String.format(template, args);
        } catch (IllegalArgumentException e) {
            for (int i = 0; i < args.length; i++) {
                template = template.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return template;
        }
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
