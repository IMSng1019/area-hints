package areahint.replacebutton;

import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * ReplaceButton按键监听器
 * 监听玩家按键输入用于更换记录按键
 */
public class ReplaceButtonKeyListener {

    private static boolean registered = false;

    /**
     * 注册按键监听器
     */
    public static void register() {
        if (registered) {
            return;
        }

        // 注册客户端tick事件监听器
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ReplaceButtonManager manager = ReplaceButtonManager.getInstance();

            // 只在等待按键状态时监听
            if (manager.getCurrentState() == ReplaceButtonManager.ReplaceButtonState.WAITING_FOR_KEY) {
                checkKeyPress(client);
            }
        });

        registered = true;
    }

    /**
     * 检查按键按下
     */
    private static void checkKeyPress(Minecraft client) {
        if (areahint.util.ClientScreenCompat.getScreen(client) != null) {
            // 如果有GUI打开，不处理按键
            return;
        }

        // 平台兼容层返回首个可持久化的原生键，不把 SDL 窗口句柄传给 GLFW。
        int keyCode = areahint.util.PlatformInputCompat.firstPressedKey(client);
        if (keyCode != InputConstants.UNKNOWN.getValue()) {
            ReplaceButtonManager.getInstance().handleKeyPress(keyCode, getKeyName(keyCode));
        }
    }

    /**
     * 获取按键名称
     */
    public static String getKeyName(int keyCode) {
        String platformName = areahint.util.PlatformInputCompat.printableKeyName(keyCode);
        if (platformName != null && !platformName.isEmpty()) {
            return platformName.toUpperCase(java.util.Locale.ROOT);
        }

        // 对于特殊键，返回自定义名称
        switch (keyCode) {
            case InputConstants.KEY_SPACE: return I18nManager.translate("replacebutton.message.general_12");
            case InputConstants.KEY_ESCAPE: return "ESC";
            case InputConstants.KEY_RETURN: return I18nManager.translate("replacebutton.message.general_7");
            case InputConstants.KEY_TAB: return "Tab";
            case InputConstants.KEY_BACKSPACE: return I18nManager.translate("replacebutton.message.general_13");
            case InputConstants.KEY_INSERT: return "Insert";
            case InputConstants.KEY_DELETE: return "Delete";
            case InputConstants.KEY_RIGHT: return I18nManager.translate("replacebutton.message.general_6");
            case InputConstants.KEY_LEFT: return I18nManager.translate("replacebutton.message.general_11");
            case InputConstants.KEY_DOWN: return I18nManager.translate("replacebutton.message.general_2");
            case InputConstants.KEY_UP: return I18nManager.translate("replacebutton.message.general");
            case InputConstants.KEY_PAGEUP: return "Page Up";
            case InputConstants.KEY_PAGEDOWN: return "Page Down";
            case InputConstants.KEY_HOME: return "Home";
            case InputConstants.KEY_END: return "End";
            case InputConstants.KEY_CAPSLOCK: return "Caps Lock";
            case InputConstants.KEY_SCROLLLOCK: return "Scroll Lock";
            case InputConstants.KEY_NUMLOCK: return "Num Lock";
            case InputConstants.KEY_PRINTSCREEN: return "Print Screen";
            case InputConstants.KEY_PAUSE: return "Pause";
            case InputConstants.KEY_LSHIFT: return I18nManager.translate("replacebutton.message.general_10");
            case InputConstants.KEY_LCONTROL: return I18nManager.translate("replacebutton.message.general_9");
            case InputConstants.KEY_LALT: return I18nManager.translate("replacebutton.message.general_8");
            case InputConstants.KEY_RSHIFT: return I18nManager.translate("replacebutton.message.general_5");
            case InputConstants.KEY_RCONTROL: return I18nManager.translate("replacebutton.message.general_4");
            case InputConstants.KEY_RALT: return I18nManager.translate("replacebutton.message.general_3");
            default: return I18nManager.translate("replacebutton.message.key") + keyCode;
        }
    }
}
