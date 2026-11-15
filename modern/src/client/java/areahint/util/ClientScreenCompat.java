package areahint.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** 26.2 将屏幕与 HUD 管理迁入 Gui，统一宿主访问以保留各页面原有的打开、返回和焦点流程。 */
public final class ClientScreenCompat {
    private ClientScreenCompat() {
    }

    public static Screen getScreen(Minecraft client) {
        //? if >=26.2 {
        /*return client.gui.screen();*/
        //?} else {
        return client.screen;
        //?}
    }

    // 交给对应版本原生宿主执行 removed、added、初始化和鼠标切换，不能直接改屏幕字段。
    public static void setScreen(Minecraft client, Screen screen) {
        //? if >=26.2 {
        /*client.gui.setScreen(screen);*/
        //?} else {
        client.setScreen(screen);
        //?}
    }

    // 描述书页光标沿用 HUD tick 计时，避免迁移后改变闪烁节奏。
    public static int getGuiTicks(Minecraft client) {
        //? if >=26.2 {
        /*return client.gui.hud.getGuiTicks();*/
        //?} else {
        return client.gui.getGuiTicks();
        //?}
    }
}
