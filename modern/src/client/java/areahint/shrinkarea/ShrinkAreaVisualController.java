package areahint.shrinkarea;

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
 * shrinkarea 图形流程控制器，只负责选择域名并转入现有 ShrinkAreaManager 录点流程。
 */
public final class ShrinkAreaVisualController {
    private ShrinkAreaVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        ShrinkAreaManager manager = ShrinkAreaManager.getInstance();
        List<AreaData> areas = manager.beginVisualSelection();
        if (areas.isEmpty()) {
            showInfo(parent, "commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.shrinkarea.title",
            "commandui.shrinkarea.prompt",
            CommandUiData.areaItems(areas),
            area -> {
                closeToGame();
                CommandVisualController.beginVisualRecordMode("shrinkarea");
                manager.selectAreaByName(area.getName());
            },
            () -> {
                manager.stop();
                CommandVisualController.clearVisualRecordMode();
            }));
    }

    private static void showInfo(Screen parent, String messageKey) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.shrinkarea.title",
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
