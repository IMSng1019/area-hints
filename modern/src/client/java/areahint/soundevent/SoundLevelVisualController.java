package areahint.soundevent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * soundlevel 指令图形流程控制器，负责从指令面板打开独立音量编辑界面。
 */
public final class SoundLevelVisualController {
    private SoundLevelVisualController() {
    }

    /**
     * 从指令可视化面板打开音量编辑界面，所有修改在确认前仅保存在界面草稿中。
     * @param parent 完成或取消后需要返回的指令面板
     */
    public static void openFromCommandUi(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, new SoundLevelScreen(parent));
        }
    }
}
