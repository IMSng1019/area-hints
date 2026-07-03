package areahint.subtitle;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * replacesubtitle 图形流程控制器，只把 Screen 输入转交给现有副字幕替换状态机。
 */
public final class ReplaceSubtitleVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private ReplaceSubtitleVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint replacesubtitle");
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
            "commandui.replacesubtitle.title",
            "commandui.replacesubtitle.prompt",
            areaItems(areas),
            area -> SubtitleManager.getInstance().handleAddAreaSelection(area.getName()),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showSubtitleTextScreen(AreaData area, String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.replacesubtitle.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.replacesubtitle.text.label",
                "commandui.replacesubtitle.text.placeholder", "", 160)),
            "commandui.replacesubtitle.text.prompt",
            I18nManager.translate("commandui.replacesubtitle.text.detail", areaName(area), subtitlePreview(area)),
            errorTextOrKey,
            values -> SubtitleManager.getInstance().handleSubtitleText(values.isEmpty() ? "" : values.get(0)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showColorSelection(AreaData area, String subtitle) {
        setScreen(new WizardOptionScreen(parentScreen,
            "commandui.replacesubtitle.title",
            "commandui.replacesubtitle.color.prompt",
            I18nManager.translate("commandui.replacesubtitle.color.detail", areaName(area), previewText(subtitle)),
            areahint.commandui.CommandUiData.colorOptions(
                color -> SubtitleManager.getInstance().handleAddColorSelection(color),
                () -> showCustomColorScreen(null)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showCustomColorScreen(String errorTextOrKey) {
        setScreen(new WizardTextInputScreen(parentScreen,
            "commandui.replacesubtitle.color.custom.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.replacesubtitle.color.custom.label",
                "commandui.replacesubtitle.color.custom.placeholder", "#FFFFFF", 32)),
            "commandui.replacesubtitle.color.custom.prompt",
            "commandui.replacesubtitle.color.custom.detail",
            errorTextOrKey,
            values -> SubtitleManager.getInstance().handleAddColorSelection(values.isEmpty() ? "" : values.get(0)),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showConfirmScreen(AreaData area, String subtitle, String color) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.replacesubtitle.confirm.area", areaName(area)));
        details.add(I18nManager.translate("commandui.replacesubtitle.confirm.subtitle", previewText(subtitle)));
        details.add(I18nManager.translate("commandui.replacesubtitle.confirm.color", color == null ? "" : color));

        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacesubtitle.title",
            I18nManager.translate("commandui.replacesubtitle.confirm.prompt"),
            details,
            "commandui.button.confirm",
            SubtitleManager.getInstance()::confirmAddSubtitle,
            SubtitleManager.getInstance()::cancel));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacesubtitle.title",
            I18nManager.translate(messageTextOrKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static List<WizardSelectionListScreen.SelectionItem<AreaData>> areaItems(List<AreaData> areas) {
        List<WizardSelectionListScreen.SelectionItem<AreaData>> items = new ArrayList<>();
        for (AreaData area : areas) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(area,
                AreaDataConverter.getDisplayName(area),
                I18nManager.translate("commandui.replacesubtitle.item.detail",
                    areaName(area), area == null ? "" : area.getLevel(), subtitlePreview(area))));
        }
        return items;
    }

    private static String areaName(AreaData area) {
        return area == null || area.getName() == null ? "" : area.getName();
    }

    private static String subtitlePreview(AreaData area) {
        if (area == null || !area.hasSubtitle()) {
            return I18nManager.translate("subtitle.ui.none");
        }
        return previewText(area.getSubtitle());
    }

    private static String previewText(String value) {
        return value == null || value.trim().isEmpty()
            ? I18nManager.translate("subtitle.ui.none")
            : value.replace("\n", " / ").trim();
    }

    private static void setScreen(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
