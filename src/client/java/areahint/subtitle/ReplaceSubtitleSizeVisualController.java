package areahint.subtitle;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiData;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * replacesubtitlesize 图形流程控制器，只把 Screen 选择结果交给现有副字幕大小状态机。
 */
public final class ReplaceSubtitleSizeVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private ReplaceSubtitleSizeVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint replacesubtitlesize");
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

    public static void showSizeSelection(String currentSize) {
        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.replacesubtitlesize.title",
            "commandui.subtitle.size.prompt",
            sizeDetail(currentSize),
            CommandUiData.sizeOptions(ReplaceSubtitleSizeVisualController::selectSize,
                true, () -> showCustomSizeScreen(null)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showCustomSizeScreen(String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.replacesubtitlesize.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.replacesubtitlesize.custom.label",
                "commandui.replacesubtitlesize.custom.placeholder", initialCustomScale(), 16)),
            "commandui.replacesubtitlesize.custom.prompt",
            I18nManager.translate("commandui.replacesubtitlesize.custom.detail",
                ConfigData.CUSTOM_SIZE_MIN, ConfigData.CUSTOM_SIZE_MAX),
            errorTextOrKey,
            values -> handleCustomSize(values.isEmpty() ? "" : values.get(0)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacesubtitlesize.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static void selectSize(String size) {
        closeToGame();
        SubtitleManager.getInstance().handleSubtitleSizeSelection(size);
    }

    private static void handleCustomSize(String input) {
        float scale;
        try {
            scale = Float.parseFloat(input.trim());
        } catch (Exception e) {
            showCustomSizeScreen("commandui.replacesubtitlesize.custom.error");
            return;
        }

        if (!Float.isFinite(scale) || scale < ConfigData.CUSTOM_SIZE_MIN || scale > ConfigData.CUSTOM_SIZE_MAX) {
            showCustomSizeScreen("commandui.replacesubtitlesize.custom.error");
            return;
        }
        String customSize = ConfigData.formatCustomSize(scale);
        selectSize(customSize);
    }

    private static String initialCustomScale() {
        String currentSize = ClientConfig.getSubtitleSize();
        if ("auto".equals(currentSize)) {
            return "";
        }

        Float customScale = ConfigData.getCustomSizeScale(currentSize);
        if (customScale != null) {
            return stripCustomPrefix(ConfigData.formatCustomSize(customScale));
        }

        Float presetScale = ConfigData.getPresetSizeScale(currentSize);
        return presetScale == null ? "" : stripCustomPrefix(ConfigData.formatCustomSize(presetScale));
    }

    private static String sizeDetail(String currentSize) {
        return I18nManager.translate("commandui.subtitle.size.detail")
            .replace("%s", sizeDisplayName(currentSize));
    }

    private static String sizeDisplayName(String size) {
        if (size == null || size.trim().isEmpty()) {
            return "";
        }
        if (ConfigData.getCustomSizeScale(size) != null) {
            return I18nManager.translate("commandui.common.size.custom") + " (" + stripCustomPrefix(size) + ")";
        }
        return SubtitleUI.getSizeDisplayName(size);
    }

    private static String stripCustomPrefix(String size) {
        if (size == null) {
            return "";
        }
        return size.startsWith("custom:") ? size.substring("custom:".length()) : size;
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
}
