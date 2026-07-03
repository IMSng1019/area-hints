package areahint.command;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.i18n.I18nManager;
import areahint.network.ClientNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * sethigh 图形流程控制器，只把 Screen 输入转换为现有 SetHigh 网络请求。
 */
public final class SetHighVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static boolean visualFlowActive;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private SetHighVisualController() {
    }

    /**
     * 服务端返回的可设置高度域名条目，保留当前高度用于界面展示。
     */
    public record AreaEntry(String name, boolean hasAltitude, Double maxHeight, Double minHeight) {
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
        CommandUiActions.runCommandAndClose(parent, "areahint sethigh");
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

    public static void showAreaSelection(List<AreaEntry> areas) {
        if (areas == null || areas.isEmpty()) {
            showInfo("commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.sethigh.title",
            "commandui.sethigh.prompt",
            areaItems(areas),
            SetHighVisualController::showModeSelection,
            SetHighVisualController::cancelWithCommand));
    }

    public static void showModeSelection(AreaEntry area) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            option("commandui.sethigh.unlimited", () -> showUnlimitedConfirm(area)),
            option("commandui.sethigh.custom", () -> showCustomAltitude(area, null))
        );

        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.sethigh.title",
            "commandui.sethigh.mode.prompt",
            "commandui.sethigh.mode.detail",
            options,
            SetHighVisualController::cancelWithCommand));
    }

    private static void showUnlimitedConfirm(AreaEntry area) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.sethigh.title",
            format("commandui.sethigh.confirm.unlimited", area.name()),
            List.of(currentAltitudeText(area)),
            "commandui.button.confirm",
            () -> {
                ClientNetworking.sendSetHighRequest(area.name(), false, null, null);
                clear();
            },
            SetHighVisualController::cancelWithCommand));
    }

    private static void showCustomAltitude(AreaEntry area, String errorKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.sethigh.title",
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.sethigh.min.label",
                    "commandui.sethigh.min.placeholder", initialValue(area.minHeight()), 12),
                new WizardTextInputScreen.FieldSpec("commandui.sethigh.max.label",
                    "commandui.sethigh.max.placeholder", initialValue(area.maxHeight()), 12)
            ),
            "commandui.sethigh.custom.prompt",
            "commandui.sethigh.custom.detail",
            errorKey,
            values -> handleCustomAltitude(area, values),
            SetHighVisualController::cancelWithCommand));
    }

    private static void handleCustomAltitude(AreaEntry area, List<String> values) {
        try {
            double minHeight = Double.parseDouble(values.get(0).trim());
            double maxHeight = Double.parseDouble(values.get(1).trim());
            if (maxHeight <= minHeight) {
                showCustomAltitude(area, "commandui.sethigh.error.order");
                return;
            }

            setScreen(new WizardConfirmScreen(parentScreen,
                "commandui.sethigh.title",
                format("commandui.sethigh.confirm.custom", area.name(), minHeight, maxHeight),
                List.of(currentAltitudeText(area)),
                "commandui.button.confirm",
                () -> {
                    ClientNetworking.sendSetHighRequest(area.name(), true, maxHeight, minHeight);
                    clear();
                },
                SetHighVisualController::cancelWithCommand));
        } catch (NumberFormatException e) {
            showCustomAltitude(area, "commandui.sethigh.error.number");
        }
    }

    private static List<WizardSelectionListScreen.SelectionItem<AreaEntry>> areaItems(List<AreaEntry> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaEntry>> items = new ArrayList<>();
        for (AreaEntry area : areas) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, area.name(), currentAltitudeText(area)));
        }
        return items;
    }

    private static String currentAltitudeText(AreaEntry area) {
        if (!area.hasAltitude()) {
            return I18nManager.translate("commandui.sethigh.unlimited");
        }
        return I18nManager.translate("commandui.sethigh.min.label") + ": " + valueText(area.minHeight())
            + " | " + I18nManager.translate("commandui.sethigh.max.label") + ": " + valueText(area.maxHeight());
    }

    private static String valueText(Double value) {
        return value == null ? I18nManager.translate("commandui.common.none") : String.valueOf(value);
    }

    private static String initialValue(Double value) {
        if (value == null) {
            return "";
        }
        if (value.doubleValue() == value.longValue()) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(value);
    }

    private static WizardOptionScreen.OptionSpec option(String labelKey, Runnable action) {
        return new WizardOptionScreen.OptionSpec(labelKey, "", -1, action);
    }

    private static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.sethigh.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            SetHighVisualController::clear,
            SetHighVisualController::clear));
    }

    private static void cancelWithCommand() {
        clear();
        CommandUiActions.runCommand("areahint sethigh cancel");
    }

    private static String format(String key, Object... args) {
        String template = I18nManager.translate(key);
        if (!template.contains("%")) {
            for (int i = 0; i < args.length; i++) {
                template = template.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return template;
        }
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
