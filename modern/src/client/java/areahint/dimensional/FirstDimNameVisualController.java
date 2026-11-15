package areahint.dimensional;

import areahint.commandui.CommandUiActions;
import areahint.commandui.WizardTextInputScreen;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * firstdimname 图形流程控制器，只把首次维度域名输入转换为现有指令。
 */
public final class FirstDimNameVisualController {
    private FirstDimNameVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        openNameInput(parent, null);
    }

    private static void openNameInput(Screen parent, String errorKey) {
        setScreen(new WizardTextInputScreen(parent,
            "commandui.firstdimname.title",
            List.of(new WizardTextInputScreen.FieldSpec("commandui.firstdimname.label",
                "commandui.firstdimname.placeholder", "", 50)),
            "commandui.firstdimname.prompt",
            "commandui.firstdimname.detail",
            errorKey,
            values -> {
                String name = values.isEmpty() ? "" : values.get(0).trim();
                if (name.isEmpty()) {
                    openNameInput(parent, "commandui.common.error.empty");
                    return;
                }
                CommandUiActions.runCommandAndClose(parent, "areahint firstdimname " + name);
            },
            null));
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }
}
