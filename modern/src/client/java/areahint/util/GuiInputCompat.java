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
        return (modifiers & org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0;
        /*return Screen.hasShiftDown();
        */
    }

    // 编辑器每帧采样指针时没有键盘事件，仍需实时读取左右 Shift，允许拖动中切换约束。
    public static boolean isShiftPressed() {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        /*return Screen.hasShiftDown();
        */
    }

    // 使用原版事件的 Ctrl 判定保留 macOS Command 快捷键语义，旧版仍走原 Screen 方法。
    public static boolean hasControl(int key, int scan, int modifiers) {
        return new net.minecraft.client.input.KeyEvent(key, scan, modifiers).hasControlDown();
        /*return Screen.hasControlDown();
        */
    }

    // 旧回调只产生 char，新回调产生完整码点；新版必须交给 CharInput 校验，不能截断补充平面字符。
    public static boolean isValidChar(int codepoint) {
        return new net.minecraft.client.input.CharacterEvent(codepoint).isAllowedChatCharacter();
        /*return net.minecraft.util.StringHelper.isValidChar((char) codepoint);*/
        /*return net.minecraft.SharedConstants.isValidChar((char) codepoint);
        */
    }
}
