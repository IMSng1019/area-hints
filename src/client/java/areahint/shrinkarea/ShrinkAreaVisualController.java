package areahint.shrinkarea;

import areahint.commandui.CommandUiData;
import areahint.commandui.CommandVisualController;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

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
