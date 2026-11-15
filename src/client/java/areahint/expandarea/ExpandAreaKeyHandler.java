package areahint.expandarea;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import areahint.i18n.I18nManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class ExpandAreaKeyHandler {
    private static KeyBinding confirmKey;
    private static boolean wasConfirmPressed = false;
    
    /**
     * 注册按键处理器
     * X键由UnifiedKeyHandler统一处理，这里只处理Enter键
     */
    public static void register() {
        // 注册确认按键 (Enter)
        confirmKey = KeyBindingHelper.registerKeyBinding(areahint.keyhandler.KeyBindingCompat.create(
            "key.areahint.expandarea.confirm",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_ENTER
        ));
        
        // 注册客户端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            
            ExpandAreaManager manager = ExpandAreaManager.getInstance();
            
            // 只有在ExpandArea模式活动时才处理按键
            if (!manager.isActive()) {
                wasConfirmPressed = confirmKey.isPressed();
                return;
            }
            
            // 处理确认按键
            if (confirmKey.isPressed() && !wasConfirmPressed) {
                if (manager.isRecording()) {
                    manager.finishRecording();
                } else {
                    client.player.sendMessage(
                        Text.literal(I18nManager.translate("expandarea.error.area.record.expand"))
                            .formatted(Formatting.RED), 
                        false
                    );
                }
            }
            wasConfirmPressed = confirmKey.isPressed();
        });
    }
}
