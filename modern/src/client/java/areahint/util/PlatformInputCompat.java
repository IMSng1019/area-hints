package areahint.util;

import areahint.keyhandler.LegacyKeyCodec;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

/** 将原生键盘事件与历史配置分开，集中处理 GLFW 与 SDL 的类型、轮询和持久化边界。 */
public final class PlatformInputCompat {
    private static final int[] SDL_CAPTURE_KEYS = LegacyKeyCodec.supportedLegacyCodes();

    private PlatformInputCompat() { }

    // 新事件第二项为布局对应的逻辑键码，旧事件第二项为扫描码；保留原值用于重建快捷键事件。
    public static int secondaryCode(KeyEvent event) {
        //? if >=26.3 {
        /*return event.keycode();*/
        //?} else {
        return event.scancode();
        //?}
    }

    public static InputConstants.Type keyboardType() {
        //? if >=26.3 {
        /*return InputConstants.Type.KEYBOARD;*/
        //?} else {
        return InputConstants.Type.KEYSYM;
        //?}
    }

    // 未知旧值在 SDL 上保持未绑定，不能把同一个整数误当成另一颗键。
    public static int fromStoredCode(int storedCode) {
        //? if >=26.3 {
        /*int nativeCode = LegacyKeyCodec.toSdl(storedCode);
        return nativeCode == LegacyKeyCodec.UNSUPPORTED ? InputConstants.UNKNOWN.getValue() : nativeCode;*/
        //?} else {
        return storedCode;
        //?}
    }

    public static int toStoredCode(int nativeCode) {
        //? if >=26.3 {
        /*return LegacyKeyCodec.toLegacy(nativeCode);*/
        //?} else {
        return nativeCode;
        //?}
    }

    public static InputConstants.Key storedKey(int storedCode) {
        return keyboardType().getOrCreate(fromStoredCode(storedCode));
    }

    public static boolean isKeyDown(Minecraft client, int nativeCode) {
        //? if >=26.3 {
        /*return nativeCode > 0 && InputConstants.isKeyDown(nativeCode);*/
        //?} else {
        return nativeCode >= 32 && nativeCode <= 348 && InputConstants.isKeyDown(client.getWindow(), nativeCode);
        //?}
    }

    // SDL 只捕获能无损写回历史字段的键；顺序沿用旧键码，从而保留同时按键时的原先优先级。
    public static int firstPressedKey(Minecraft client) {
        //? if >=26.3 {
        /*for (int storedCode : SDL_CAPTURE_KEYS) {
            int nativeCode = LegacyKeyCodec.toSdl(storedCode);
            if (isKeyDown(client, nativeCode)) return nativeCode;
        }*/
        //?} else {
        for (int nativeCode = 32; nativeCode <= 348; nativeCode++) {
            if (isKeyDown(client, nativeCode)) return nativeCode;
        }
        //?}
        return InputConstants.UNKNOWN.getValue();
    }

    // 可打印键使用平台本地名称，特殊键继续走原有中文与其他语言的自定义名称。
    public static String printableKeyName(int nativeCode) {
        //? if >=26.3 {
        /*int storedCode = LegacyKeyCodec.toLegacy(nativeCode);
        return (storedCode >= 32 && storedCode <= 96) || (storedCode >= 320 && storedCode <= 336)
                ? keyboardType().getOrCreate(nativeCode).getDisplayName().getString() : null;*/
        //?} else {
        return org.lwjgl.glfw.GLFW.glfwGetKeyName(nativeCode, 0);
        //?}
    }
}
