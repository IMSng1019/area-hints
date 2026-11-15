package areahint.dimensional;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.i18n.I18nManager;
import areahint.util.ColorUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * dimensionalitycolor 图形流程控制器，只把 Screen 输入转换为现有维度颜色指令。
 */
public final class DimensionalityColorVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private DimensionalityColorVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint dimensionalitycolor");
    }

    public static boolean consumeVisualStartRequest() {
        boolean requested = startingFromVisualCommand;
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        return requested;
    }

    public static boolean isVisualFlowActive() {
        return parentScreen != null;
    }

    public static void clear() {
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        parentScreen = null;
    }

    public static void showDimensionSelectionScreen() {
        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.dimensionalitycolor.title",
            "commandui.dimensionalitycolor.prompt",
            dimensionItems(),
            dimensionId -> CommandUiActions.runCommand(
                "areahint dimensionalitycolor select " + CommandUiData.quote(dimensionId)),
            DimensionalityColorVisualController::cancel));
    }

    public static void showColorSelectionScreen(String dimensionId) {
        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.dimensionalitycolor.title",
            "commandui.color.prompt",
            "commandui.color.detail",
            CommandUiData.colorOptions(
                color -> CommandUiActions.runCommand("areahint dimensionalitycolor color " + color),
                () -> showCustomColorScreen(dimensionId, null)),
            DimensionalityColorVisualController::cancel));
    }

    public static void showCustomColorScreen(String dimensionId, String errorKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.dimensionalitycolor.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.color.custom.label",
                "commandui.color.custom.placeholder", dimensionalColor(dimensionId), 16)),
            "commandui.color.custom.prompt",
            "commandui.color.custom.detail",
            errorKey,
            values -> {
                String color = normalizeStrictColor(values.isEmpty() ? "" : values.get(0));
                if (color == null) {
                    showCustomColorScreen(dimensionId, "commandui.color.error.invalid");
                    return;
                }
                CommandUiActions.runCommand("areahint dimensionalitycolor color " + color);
            },
            DimensionalityColorVisualController::cancel));
    }

    public static void showConfirmScreen(String dimensionId, String newColor) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.dimensionalitycolor.title",
            I18nManager.translate("commandui.dimensionalitycolor.confirm", dimensionalName(dimensionId), newColor),
            List.of(
                I18nManager.translate("commandui.dimension.item.detail", dimensionId),
                I18nManager.translate("commandui.dimensionalitycolor.old", dimensionalColor(dimensionId))
            ),
            "commandui.button.confirm",
            () -> {
                CommandUiActions.runCommand("areahint dimensionalitycolor confirm");
                clear();
            },
            DimensionalityColorVisualController::cancel));
    }

    public static void cancel() {
        CommandUiActions.runCommand("areahint dimensionalitycolor cancel");
        clear();
    }

    private static List<WizardSelectionListScreen.SelectionItem<String>> dimensionItems() {
        Set<String> dimensions = new LinkedHashSet<>();
        dimensions.addAll(ClientDimensionalNameManager.getAllDimensionalNames().keySet());
        dimensions.add("minecraft:overworld");
        dimensions.add("minecraft:the_nether");
        dimensions.add("minecraft:the_end");

        List<WizardSelectionListScreen.SelectionItem<String>> items = new ArrayList<>();
        for (String dimensionId : dimensions) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(dimensionId,
                dimensionalName(dimensionId),
                I18nManager.translate("commandui.dimension.item.detail", dimensionId)));
        }
        return items;
    }

    private static String dimensionalName(String dimensionId) {
        String name = ClientDimensionalNameManager.getDimensionalName(dimensionId);
        return name == null || name.isBlank() ? dimensionId : name;
    }

    private static String dimensionalColor(String dimensionId) {
        String color = ClientDimensionalNameManager.getDimensionalColor(dimensionId);
        return color == null || color.isBlank() ? "#FFFFFF" : color;
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

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
