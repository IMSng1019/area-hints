package areahint.deletehint;

import areahint.commandui.CommandUiData;
import areahint.commandui.DeleteHintVertexScreen;
import areahint.commandui.WizardConfirmScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * deletehint 图形流程控制器，只负责选择域名并进入现有顶点删除流程。
 */
public final class DeleteHintVisualController {
    private DeleteHintVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        DeleteHintManager manager = DeleteHintManager.getInstance();
        List<AreaData> areas = manager.beginVisualSelection();
        if (areas.isEmpty()) {
            showInfo(parent, "commandui.common.no_areas");
            return;
        }

        setScreen(new WizardSelectionListScreen<>(parent,
            "commandui.deletehint.title",
            "commandui.deletehint.prompt",
            CommandUiData.areaItems(areas),
            area -> {
                if (manager.selectAreaForVisual(area.getName())) {
                    setScreen(new DeleteHintVertexScreen(parent, manager));
                }
            },
            manager::cancel));
    }

    private static void showInfo(Screen parent, String messageKey) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.deletehint.title",
            I18nManager.translate(messageKey),
            List.of(),
            "commandui.button.close",
            null,
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
