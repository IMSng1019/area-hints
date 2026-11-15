package areahint.subtitle;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.List;

/**
 * deletesubtitle 图形流程控制器，只把 Screen 选择结果交给现有副字幕删除状态机。
 */
public final class DeleteSubtitleVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private DeleteSubtitleVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint deletesubtitle");
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
            "commandui.deletesubtitle.title",
            "commandui.deletesubtitle.prompt",
            areaItems(areas),
            area -> SubtitleManager.getInstance().handleDeleteAreaSelection(area.getName()),
            SubtitleManager.getInstance()::cancel));
    }

    public static void showConfirmScreen(AreaData area) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.deletesubtitle.confirm.area", areaName(area)));
        details.add(I18nManager.translate("commandui.deletesubtitle.confirm.subtitle", subtitlePreview(area)));
        details.add(I18nManager.translate("commandui.deletesubtitle.confirm.warning"));

        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.deletesubtitle.title",
            I18nManager.translate("commandui.deletesubtitle.confirm.prompt"),
            details,
            "commandui.button.delete",
            SubtitleManager.getInstance()::confirmDeleteSubtitle,
            SubtitleManager.getInstance()::cancel));
    }

    public static void showInfo(String messageTextOrKey) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.deletesubtitle.title",
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
                I18nManager.translate("commandui.deletesubtitle.item.detail",
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
        return area.getSubtitle().replace("\n", " / ").trim();
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
