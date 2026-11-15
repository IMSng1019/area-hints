package areahint.command;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * reload 指令图形流程控制器，只确认后执行现有 /areahint reload。
 */
public final class ReloadVisualController {
    private ReloadVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.reload.detail"));
        details.add(I18nManager.translate("commandui.reload.sync"));
        details.add(I18nManager.translate("commandui.reload.command", "/areahint reload"));

        setScreen(new WizardConfirmScreen(parent,
            "commandui.reload.title",
            I18nManager.translate("commandui.reload.prompt"),
            details,
            "commandui.reload.confirm",
            () -> CommandUiActions.runCommand("areahint reload"),
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }
}
