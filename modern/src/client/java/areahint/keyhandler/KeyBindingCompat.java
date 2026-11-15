package areahint.keyhandler;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** 隔离 1.21.9 的按键类别和输入记录变更，统一绑定键仍按键码匹配而不限制修饰键。 */
public final class KeyBindingCompat {
    // 新版类别只能注册一次，三个功能共享同一个类别与现有本地化名称。
    private static final KeyMapping.Category GENERAL = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("areahint", "general"));

    private KeyBindingCompat() {
    }

    public static KeyMapping create(String translationKey, InputConstants.Type type, int code) {
        return new KeyMapping(translationKey, type, code, GENERAL);
        /*return new KeyBinding(translationKey, type, code, "category.areahint.general");
        */
    }

    // 原接口只有键码与扫描码，新事件的修饰键不参与 KeyBinding 的普通按键匹配。
    public static boolean matchesKey(KeyMapping binding, int keyCode, int scanCode) {
        return binding.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
        /*return binding.matchesKey(keyCode, scanCode);
        */
    }

    // 鼠标绑定只匹配按钮，不依赖点击位置或双击状态。
    public static boolean matchesMouse(KeyMapping binding, int button) {
        return binding.matchesMouse(new net.minecraft.client.input.MouseButtonEvent(0, 0, new net.minecraft.client.input.MouseButtonInfo(button, 0)));
        /*return binding.matchesMouse(button);
        */
    }
}
