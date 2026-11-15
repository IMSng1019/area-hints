package areahint.util;

import net.minecraft.client.Minecraft;

/** 自绘文本页不含 EditBox，SDL 时代需要显式拥有文本输入；标准控件继续使用原版焦点管理。 */
public final class TextInputCompat {
    private TextInputCompat() { }

    public static void start(Object owner) {
        //? if >=26.3 {
        /*Minecraft.getInstance().textInputManager().startTextInput(owner);*/
        //?}
    }

    public static void stop(Object owner) {
        //? if >=26.3 {
        /*Minecraft.getInstance().textInputManager().stopTextInput(owner);*/
        //?}
    }

    // 参数是 GUI 坐标的左右上下边界，原版管理器负责缩放和像素密度换算。
    public static void area(int left, int top, int right, int bottom) {
        //? if >=26.3 {
        /*Minecraft.getInstance().textInputManager().setTextInputArea(left, top, right, bottom);*/
        //?}
    }
}
