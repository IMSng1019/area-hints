package areahint.easyadd;

import areahint.commandui.CommandUiActions;
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
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * EasyAdd 图形流程控制器，负责把 Manager 状态映射为 Screen 或录点 HUD。
 */
public final class EasyAddVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private EasyAddVisualController() {
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
        openFromCommandUi(parent, "areahint easyadd");
    }

    public static void openFromCommandUi(Screen parent, String command) {
        parentScreen = parent;
        startingFromVisualCommand = true;
        visualStartTicksRemaining = VISUAL_START_TIMEOUT_TICKS;
        CommandUiActions.runCommandAndClose(parent, command);
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

    public static void showNameScreen(String errorKey) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.easyadd.name.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.easyadd.name.label",
                "commandui.easyadd.name.placeholder", "", 80)),
            "commandui.easyadd.name.prompt",
            "commandui.easyadd.name.detail",
            errorKey,
            values -> EasyAddManager.getInstance().handleVisualNameInput(values.get(0)),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showSurfaceNameScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.easyadd.surface.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.easyadd.surface.label",
                "commandui.easyadd.surface.placeholder", "", 80)),
            "commandui.easyadd.surface.prompt",
            "commandui.easyadd.surface.detail",
            null,
            values -> EasyAddManager.getInstance().handleVisualSurfaceNameInput(values.get(0)),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showLevelScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardOptionScreen(parentScreen,
            "commandui.easyadd.level.title",
            "commandui.easyadd.level.prompt",
            "commandui.easyadd.level.detail",
            List.of(
                new WizardOptionScreen.OptionSpec("commandui.easyadd.level.1", "commandui.easyadd.level.1.detail", -1,
                    () -> EasyAddManager.getInstance().handleLevelInput(1)),
                new WizardOptionScreen.OptionSpec("commandui.easyadd.level.2", "commandui.easyadd.level.2.detail", -1,
                    () -> EasyAddManager.getInstance().handleLevelInput(2)),
                new WizardOptionScreen.OptionSpec("commandui.easyadd.level.3", "commandui.easyadd.level.3.detail", -1,
                    () -> EasyAddManager.getInstance().handleLevelInput(3))),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showBaseSelectScreen(List<AreaData> areas) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String detail = I18nManager.translate("commandui.easyadd.base.detail",
                area.getName(), area.getLevel(), area.getColor());
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, displayName, detail));
        }
        client.setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.easyadd.base.title",
            "commandui.easyadd.base.prompt",
            items,
            area -> EasyAddManager.getInstance().handleBaseSelection(area.getName()),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showRecordingHud() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    public static void showAltitudeScreen(List<BlockPos> recordedPoints) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardOptionScreen(parentScreen,
            "commandui.easyadd.altitude.title",
            altitudePrompt(recordedPoints),
            "commandui.easyadd.altitude.detail",
            List.of(
                new WizardOptionScreen.OptionSpec("commandui.easyadd.altitude.auto",
                    "commandui.easyadd.altitude.auto.detail", -1,
                    () -> EasyAddAltitudeManager.handleAltitudeTypeSelection(EasyAddAltitudeManager.AltitudeType.AUTOMATIC)),
                new WizardOptionScreen.OptionSpec("commandui.easyadd.altitude.custom",
                    "commandui.easyadd.altitude.custom.detail", -1,
                    () -> EasyAddAltitudeManager.handleAltitudeTypeSelection(EasyAddAltitudeManager.AltitudeType.CUSTOM)),
                new WizardOptionScreen.OptionSpec("commandui.easyadd.altitude.unlimited",
                    "commandui.easyadd.altitude.unlimited.detail", -1,
                    () -> EasyAddAltitudeManager.handleAltitudeTypeSelection(EasyAddAltitudeManager.AltitudeType.UNLIMITED))),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showCustomAltitudeScreen(String errorKey) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.easyadd.altitude.custom.title",
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.easyadd.altitude.min",
                    "commandui.easyadd.altitude.min.placeholder", "", 12),
                new WizardTextInputScreen.FieldSpec("commandui.easyadd.altitude.max",
                    "commandui.easyadd.altitude.max.placeholder", "", 12)),
            "commandui.easyadd.altitude.custom.prompt",
            "commandui.easyadd.altitude.custom.detail",
            errorKey,
            values -> EasyAddManager.getInstance().handleVisualCustomAltitudeInput(values.get(0), values.get(1)),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showColorScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardOptionScreen(parentScreen,
            "commandui.easyadd.color.title",
            "commandui.easyadd.color.prompt",
            "commandui.easyadd.color.detail",
            colorOptions(),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showCustomColorScreen(String errorKey) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.easyadd.color.custom.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.easyadd.color.custom.label",
                "commandui.easyadd.color.custom.placeholder", "#FFFFFF", 16)),
            "commandui.easyadd.color.custom.prompt",
            "commandui.easyadd.color.custom.detail",
            errorKey,
            values -> EasyAddManager.getInstance().handleVisualCustomColorInput(values.get(0)),
            () -> EasyAddManager.getInstance().cancelEasyAdd()));
    }

    public static void showConfirmScreen(AreaData areaData) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.setScreen(new EasyAddConfirmScreen(parentScreen, areaData));
    }

    private static String altitudePrompt(List<BlockPos> points) {
        if (points == null || points.isEmpty()) {
            return "commandui.easyadd.altitude.prompt";
        }
        int minY = points.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = points.stream().mapToInt(BlockPos::getY).max().orElse(0);
        return I18nManager.translate("commandui.easyadd.altitude.prompt.range", minY, maxY);
    }

    private static List<WizardOptionScreen.OptionSpec> colorOptions() {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        addColor(options, "commandui.easyadd.color.white", "#FFFFFF");
        addColor(options, "commandui.easyadd.color.gray", "#808080");
        addColor(options, "commandui.easyadd.color.dark_gray", "#555555");
        addColor(options, "commandui.easyadd.color.black", "#000000");
        addColor(options, "commandui.easyadd.color.dark_red", "#AA0000");
        addColor(options, "commandui.easyadd.color.red", "#FF0000");
        addColor(options, "commandui.easyadd.color.pink", "#FF55FF");
        addColor(options, "commandui.easyadd.color.gold", "#FFAA00");
        addColor(options, "commandui.easyadd.color.yellow", "#FFFF00");
        addColor(options, "commandui.easyadd.color.green", "#00FF00");
        addColor(options, "commandui.easyadd.color.dark_green", "#00AA00");
        addColor(options, "commandui.easyadd.color.aqua", "#55FFFF");
        addColor(options, "commandui.easyadd.color.dark_aqua", "#00AAAA");
        addColor(options, "commandui.easyadd.color.blue", "#5555FF");
        addColor(options, "commandui.easyadd.color.dark_blue", "#0000AA");
        addColor(options, "commandui.easyadd.color.purple", "#AA00AA");
        addFlash(options, "commandui.easyadd.color.flash_bw_all", ColorUtil.FLASH_BW_ALL);
        addFlash(options, "commandui.easyadd.color.flash_rainbow_all", ColorUtil.FLASH_RAINBOW_ALL);
        addFlash(options, "commandui.easyadd.color.flash_bw_char", ColorUtil.FLASH_BW_CHAR);
        addFlash(options, "commandui.easyadd.color.flash_rainbow_char", ColorUtil.FLASH_RAINBOW_CHAR);
        options.add(new WizardOptionScreen.OptionSpec("commandui.easyadd.color.custom",
            "commandui.easyadd.color.custom.detail", -1, () -> showCustomColorScreen(null)));
        return options;
    }

    private static void addColor(List<WizardOptionScreen.OptionSpec> options, String labelKey, String color) {
        int rgb = Integer.parseInt(color.substring(1), 16);
        options.add(new WizardOptionScreen.OptionSpec(labelKey, "", rgb,
            () -> EasyAddManager.getInstance().handleColorSelection(color)));
    }

    private static void addFlash(List<WizardOptionScreen.OptionSpec> options, String labelKey, String color) {
        options.add(new WizardOptionScreen.OptionSpec(labelKey, "", 0xFFFFFF,
            () -> EasyAddManager.getInstance().handleColorSelection(color)));
    }

}
