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
 * off 指令图形流程控制器，只确认后执行现有 /areahint off。
 */
public final class OffVisualController {
    private OffVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.off.detail"));
        details.add(I18nManager.translate(ClientConfig.isEnabled()
            ? "commandui.off.current.enabled"
            : "commandui.off.current.disabled"));

        setScreen(new WizardConfirmScreen(parent,
            "commandui.off.title",
            I18nManager.translate("commandui.off.prompt"),
            details,
            "commandui.off.confirm",
            () -> CommandUiActions.runCommand("areahint off"),
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
