package areahint.delete;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;

/**
 * delete 图形流程控制器，只把界面选择转换成现有 /areahint delete 子命令。
 */
public final class DeleteVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static boolean visualFlowActive;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private DeleteVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint delete");
    }

    public static boolean consumeVisualStartRequest() {
        boolean requested = startingFromVisualCommand;
        startingFromVisualCommand = false;
        visualFlowActive = requested;
        visualStartTicksRemaining = 0;
        return requested;
    }

    public static boolean isVisualFlowActive() {
        return visualFlowActive;
    }

    public static void clear() {
        startingFromVisualCommand = false;
        visualFlowActive = false;
        visualStartTicksRemaining = 0;
        parentScreen = null;
    }

    public static void showAreaSelection(List<AreaData> areas) {
        if (areas == null || areas.isEmpty()) {
            showInfo("message.message.area.dimension.delete");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.delete.title",
            "commandui.delete.prompt",
            CommandUiData.areaItems(areas),
            area -> CommandUiActions.runCommand("areahint delete select " + CommandUiData.quote(area.getName())),
            () -> {
                clear();
                CommandUiActions.runCommand("areahint delete cancel");
            }));
    }

    public static void showConfirmScreen(AreaData area) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.delete.title",
            I18nManager.translate("commandui.delete.confirm", area == null ? "" : area.getName()),
            area == null ? List.of() : areaDetails(area),
            "commandui.button.delete",
            () -> {
                clear();
                CommandUiActions.runCommand("areahint delete confirm");
            },
            () -> {
                clear();
                CommandUiActions.runCommand("areahint delete cancel");
            }));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.delete.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            DeleteVisualController::clear,
            DeleteVisualController::clear));
    }

    private static List<String> areaDetails(AreaData area) {
        return List.of(
            I18nManager.translate("commandui.common.area.name", nullText(area.getName())),
            I18nManager.translate("commandui.common.area.level", area.getLevel()),
            I18nManager.translate("commandui.common.area.surface", nullText(area.getSurfacename())),
            I18nManager.translate("commandui.common.area.base", nullText(area.getBaseName())),
            I18nManager.translate("commandui.common.area.signature", nullText(area.getSignature()))
        );
    }

    private static String nullText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("commandui.common.none") : value;
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
