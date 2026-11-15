package areahint.keyhandler;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/** 隔离 1.21.9 的按键类别和输入记录变更，统一绑定键仍按键码匹配而不限制修饰键。 */
public final class KeyBindingCompat {
    // 新版类别只能注册一次，三个功能共享同一个类别与现有本地化名称。
    //? if >=1.21.9 {
    /*private static final KeyBinding.Category GENERAL = KeyBinding.Category.create(net.minecraft.util.Identifier.of("areahint", "general"));*/
    //?}

    private KeyBindingCompat() {
    }

    public static KeyBinding create(String translationKey, InputUtil.Type type, int code) {
        //? if >=1.21.9 {
        /*return new KeyBinding(translationKey, type, code, GENERAL);*/
        //?} else {
        return new KeyBinding(translationKey, type, code, "category.areahint.general");
        //?}
    }

    // 原接口只有键码与扫描码，新事件的修饰键不参与 KeyBinding 的普通按键匹配。
    public static boolean matchesKey(KeyBinding binding, int keyCode, int scanCode) {
        //? if >=1.21.9 {
        /*return binding.matchesKey(new net.minecraft.client.input.KeyInput(keyCode, scanCode, 0));*/
        //?} else {
        return binding.matchesKey(keyCode, scanCode);
        //?}
    }

    // 鼠标绑定只匹配按钮，不依赖点击位置或双击状态。
    public static boolean matchesMouse(KeyBinding binding, int button) {
        //? if >=1.21.9 {
        /*return binding.matchesMouse(new net.minecraft.client.gui.Click(0, 0, new net.minecraft.client.input.MouseInput(button, 0)));*/
        //?} else {
        return binding.matchesMouse(button);
        //?}
    }
}
