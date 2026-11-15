package areahint.dimensional;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * dimensionalityname 图形流程控制器，只把 Screen 输入转换为现有维度命名指令。
 */
public final class DimensionalityNameVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private DimensionalityNameVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint dimensionalityname");
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
            "commandui.dimensionalityname.title",
            "commandui.dimensionalityname.prompt",
            dimensionItems(),
            dimensionId -> CommandUiActions.runCommand(
                "areahint dimensionalityname select " + CommandUiData.quote(dimensionId)),
            DimensionalityNameVisualController::cancel));
    }

    public static void showNameInputScreen(String dimensionId, String currentName) {
        showNameInputScreen(dimensionId, currentName, null);
    }

    private static void showNameInputScreen(String dimensionId, String currentName, String errorKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.dimensionalityname.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.dimensionalityname.name.label",
                "commandui.dimensionalityname.name.placeholder", currentName, 50)),
            "commandui.dimensionalityname.name.prompt",
            I18nManager.translate("commandui.dimensionalityname.name.detail", dimensionId),
            errorKey,
            values -> {
                String name = values.isEmpty() ? "" : values.get(0).trim();
                if (name.isEmpty()) {
                    showNameInputScreen(dimensionId, currentName, "commandui.common.error.empty");
                    return;
                }
                CommandUiActions.runCommand("areahint dimensionalityname name " + name);
            },
            DimensionalityNameVisualController::cancel));
    }

    public static void showConfirmScreen(String dimensionId, String oldName, String newName) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.dimensionalityname.title",
            I18nManager.translate("commandui.dimensionalityname.confirm", dimensionalName(dimensionId), newName),
            List.of(
                I18nManager.translate("commandui.dimension.item.detail", dimensionId),
                I18nManager.translate("gui.message.name") + oldName
            ),
            "commandui.button.confirm",
            () -> {
                CommandUiActions.runCommand("areahint dimensionalityname confirm");
                clear();
            },
            DimensionalityNameVisualController::cancel));
    }

    public static void cancel() {
        CommandUiActions.runCommand("areahint dimensionalityname cancel");
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

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
