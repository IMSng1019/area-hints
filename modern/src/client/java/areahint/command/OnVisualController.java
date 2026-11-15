package areahint.command;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardConfirmScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * on 指令图形流程控制器，只确认后执行现有 /areahint on。
 */
public final class OnVisualController {
    private OnVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.on.detail"));
        details.add(I18nManager.translate(ClientConfig.isEnabled()
            ? "commandui.on.current.enabled"
            : "commandui.on.current.disabled"));

        setScreen(new WizardConfirmScreen(parent,
            "commandui.on.title",
            I18nManager.translate("commandui.on.prompt"),
            details,
            "commandui.on.confirm",
            () -> CommandUiActions.runCommand("areahint on"),
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
