package areahint.commandui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * help 图形流程控制器，负责打开复用指令元数据的帮助列表界面。
 */
public final class HelpVisualController {
    private HelpVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, new HelpCommandScreen(parent));
        }
    }
}
