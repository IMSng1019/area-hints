package areahint.util;

/** 统一自绘文本编辑器的快捷键和字符判定，保留新输入事件的修饰键与完整 Unicode 码点。 */
public final class GuiInputCompat {
    private GuiInputCompat() {
    }

    public static boolean isSelectAll(int key, int scan, int modifiers) {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).isSelectAll();
        /*return Screen.isSelectAll(key);
        */
    }

    public static boolean isCopy(int key, int scan, int modifiers) {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).isCopy();
        /*return Screen.isCopy(key);
        */
    }

    public static boolean isPaste(int key, int scan, int modifiers) {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).isPaste();
        /*return Screen.isPaste(key);
        */
    }

    public static boolean isCut(int key, int scan, int modifiers) {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).isCut();
        /*return Screen.isCut(key);
        */
    }

    // 新版直接读取该次按键事件的 Shift 位，旧版沿用 Screen 对当前键盘状态的判定。
    public static boolean hasShift(int modifiers) {
        return (modifiers & com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT) != 0;
        /*return Screen.hasShiftDown();
        */
    }

    // 编辑器每帧采样指针时没有键盘事件，仍需实时读取左右 Shift，允许拖动中切换约束。
    public static boolean isShiftPressed() {
        return net.minecraft.client.Minecraft.getInstance().hasShiftDown();
        /*return Screen.hasShiftDown();
        */
    }

    // 使用原版事件的 Ctrl 判定保留 macOS Command 快捷键语义，旧版仍走原 Screen 方法。
    public static boolean hasControl(int key, int scan, int modifiers) {
        //? if >=26.3 {
        /*return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).hasControlDownWithQuirk();*/
        //?} else {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).hasControlDown();
        //?}
        /*return Screen.hasControlDown();
        */
    }

    // SDL 将物理键与快捷键字符分开，撤销/重做仍依据键盘布局中的 Z/Y，而不是美式键盘的位置。
    public static boolean isUndoKey(int key, int secondaryCode) {
        //? if >=26.3 {
        /*return secondaryCode == com.mojang.blaze3d.platform.InputConstants.KEYCODE_Z;*/
        //?} else {
        return key == com.mojang.blaze3d.platform.InputConstants.KEY_Z;
        //?}
    }

    public static boolean isRedoKey(int key, int secondaryCode) {
        //? if >=26.3 {
        /*return secondaryCode == com.mojang.blaze3d.platform.InputConstants.KEYCODE_Y;*/
        //?} else {
        return key == com.mojang.blaze3d.platform.InputConstants.KEY_Y;
        //?}
    }

    // 旧回调只产生 char，新回调产生完整码点；新版必须交给 CharInput 校验，不能截断补充平面字符。
    public static boolean isValidChar(int codepoint) {
        return new net.minecraft.client.input.CharacterEvent(codepoint).isAllowedChatCharacter();
        /*return net.minecraft.util.StringHelper.isValidChar((char) codepoint);*/
        /*return net.minecraft.SharedConstants.isValidChar((char) codepoint);
        */
    }
}
