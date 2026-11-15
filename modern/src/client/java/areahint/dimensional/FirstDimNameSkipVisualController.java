package areahint.dimensional;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * firstdimname_skip 图形流程控制器，只确认后调用现有跳过首次维度命名指令。
 */
public final class FirstDimNameSkipVisualController {
    private static final String COMMAND = "areahint firstdimname_skip";

    private FirstDimNameSkipVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        setScreen(new WizardConfirmScreen(parent,
            "commandui.firstdimname_skip.title",
            I18nManager.translate("commandui.firstdimname_skip.prompt"),
            List.of(I18nManager.translate("commandui.firstdimname_skip.detail"), "/" + COMMAND),
            "commandui.button.execute",
            () -> CommandUiActions.runCommand(COMMAND),
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
