package areahint.commandui;

import areahint.gui.AreasHintConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 设置入口桥接，保证主面板和注册表都打开同一个现有配置界面。
 */
final class AreasHintSettingsBridge {
    private AreasHintSettingsBridge() {
    }

    static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new AreasHintConfigScreen(parent));
    }
}
