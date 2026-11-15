package areahint.expandarea;

import areahint.commandui.CommandUiData;
import areahint.commandui.CommandVisualController;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * expandarea 图形流程控制器，只负责把域名选择转交给现有 ExpandAreaManager 录点流程。
 */
public final class ExpandAreaVisualController {
    private ExpandAreaVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        ExpandAreaManager manager = ExpandAreaManager.getInstance();
        List<AreaData> areas = manager.beginVisualSelection();
        if (areas.isEmpty()) {
            showInfo(parent, "commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.expandarea.title",
            "commandui.expandarea.prompt",
            CommandUiData.areaItems(areas),
            area -> {
                closeToGame();
                CommandVisualController.beginVisualRecordMode("expandarea");
                manager.selectAreaByName(area.getName());
            },
            () -> {
                manager.cancel();
                CommandVisualController.clearVisualRecordMode();
            }));
    }

    private static void showInfo(Screen parent, String messageKey) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.expandarea.title",
            I18nManager.translate(messageKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }
}
