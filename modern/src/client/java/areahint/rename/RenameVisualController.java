package areahint.rename;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.List;

/**
 * rename 图形流程控制器，只把 Screen 输入转交给现有 RenameManager 状态机。
 */
public final class RenameVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private RenameVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint rename");
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
            showInfo("message.error.area.dimension.rename_2");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.rename.title",
            "commandui.rename.prompt",
            areaItems(areas),
            area -> RenameManager.getInstance().handleAreaSelection(area.getName()),
            RenameManager.getInstance()::cancelRename));
    }

    public static void showNewNameInput(String currentName, String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.rename.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.rename.name.label",
                "commandui.rename.name.placeholder", currentName == null ? "" : currentName, 80)),
            "commandui.rename.name.prompt",
            format("commandui.rename.name.detail", nullText(currentName)),
            errorTextOrKey,
            values -> RenameManager.getInstance().handleNewNameInput(values.isEmpty() ? "" : values.get(0)),
            RenameManager.getInstance()::cancelRename));
    }

    public static void showSurfaceNameInput(String currentSurfaceName) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.rename.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.rename.surface.label",
                "commandui.rename.surface.placeholder", currentSurfaceName == null ? "" : currentSurfaceName, 80)),
            "commandui.rename.surface.prompt",
            "commandui.rename.surface.detail",
            null,
            values -> RenameManager.getInstance().handleSurfaceNameInput(values.isEmpty() ? "" : values.get(0)),
            RenameManager.getInstance()::cancelRename));
    }

    public static void showConfirmScreen(String oldName, String newName, String newSurfaceName) {
        String surfaceText = newSurfaceName == null || newSurfaceName.trim().isEmpty()
            ? I18nManager.translate("commandui.common.none")
            : newSurfaceName.trim();
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.rename.title",
            format("commandui.rename.confirm", oldName, newName),
            List.of(format("commandui.rename.confirm.surface", surfaceText)),
            "commandui.button.confirm",
            RenameManager.getInstance()::confirmRename,
            RenameManager.getInstance()::cancelRename));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.rename.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            RenameVisualController::clear,
            RenameVisualController::clear));
    }

    private static List<WizardSelectionListScreen.SelectionItem<AreaData>> areaItems(List<AreaData> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String detail = I18nManager.translate("commandui.common.area.name", nullText(area.getName())) + "  "
                + I18nManager.translate("commandui.common.area.signature", nullText(area.getSignature()));
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, displayName, detail));
        }
        return items;
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
