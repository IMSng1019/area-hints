package areahint.replacebutton;

import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

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

        long window = client.getWindow().handle();

        // 遍历所有可能的按键
        for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            if (GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS) {
                // 获取按键名称
                String keyName = getKeyName(keyCode);

                // 通知管理器
                ReplaceButtonManager.getInstance().handleKeyPress(keyCode, keyName);

                // 只处理第一个按下的键
                break;
            }
        }
    }

    /**
     * 获取按键名称
     */
    public static String getKeyName(int keyCode) {
        String glfwName = GLFW.glfwGetKeyName(keyCode, 0);
        if (glfwName != null && !glfwName.isEmpty()) {
            return glfwName.toUpperCase();
        }

        // 对于特殊键，返回自定义名称
        switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE: return I18nManager.translate("replacebutton.message.general_12");
            case GLFW.GLFW_KEY_ESCAPE: return "ESC";
            case GLFW.GLFW_KEY_ENTER: return I18nManager.translate("replacebutton.message.general_7");
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_BACKSPACE: return I18nManager.translate("replacebutton.message.general_13");
            case GLFW.GLFW_KEY_INSERT: return "Insert";
            case GLFW.GLFW_KEY_DELETE: return "Delete";
            case GLFW.GLFW_KEY_RIGHT: return I18nManager.translate("replacebutton.message.general_6");
            case GLFW.GLFW_KEY_LEFT: return I18nManager.translate("replacebutton.message.general_11");
            case GLFW.GLFW_KEY_DOWN: return I18nManager.translate("replacebutton.message.general_2");
            case GLFW.GLFW_KEY_UP: return I18nManager.translate("replacebutton.message.general");
            case GLFW.GLFW_KEY_PAGE_UP: return "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "Page Down";
            case GLFW.GLFW_KEY_HOME: return "Home";
            case GLFW.GLFW_KEY_END: return "End";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "Scroll Lock";
            case GLFW.GLFW_KEY_NUM_LOCK: return "Num Lock";
            case GLFW.GLFW_KEY_PRINT_SCREEN: return "Print Screen";
            case GLFW.GLFW_KEY_PAUSE: return "Pause";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return I18nManager.translate("replacebutton.message.general_10");
            case GLFW.GLFW_KEY_LEFT_CONTROL: return I18nManager.translate("replacebutton.message.general_9");
            case GLFW.GLFW_KEY_LEFT_ALT: return I18nManager.translate("replacebutton.message.general_8");
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return I18nManager.translate("replacebutton.message.general_5");
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return I18nManager.translate("replacebutton.message.general_4");
            case GLFW.GLFW_KEY_RIGHT_ALT: return I18nManager.translate("replacebutton.message.general_3");
            default: return I18nManager.translate("replacebutton.message.key") + keyCode;
        }
    }
}
