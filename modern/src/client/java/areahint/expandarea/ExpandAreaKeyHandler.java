package areahint.expandarea;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import areahint.i18n.I18nManager;
import com.mojang.blaze3d.platform.InputConstants;

public class ExpandAreaKeyHandler {
    private static KeyMapping confirmKey;
    private static boolean wasConfirmPressed = false;
    
    /**
     * 注册按键处理器
     * X键由UnifiedKeyHandler统一处理，这里只处理Enter键
     */
    public static void register() {
        // 注册确认按键 (Enter)
        confirmKey = KeyMappingHelper.registerKeyMapping(areahint.keyhandler.KeyBindingCompat.create(
            "key.areahint.expandarea.confirm",
            areahint.util.PlatformInputCompat.keyboardType(),
            InputConstants.KEY_RETURN
        ));
        
        // 注册客户端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            
            ExpandAreaManager manager = ExpandAreaManager.getInstance();
            
            // 只有在ExpandArea模式活动时才处理按键
            if (!manager.isActive()) {
                wasConfirmPressed = confirmKey.isDown();
                return;
            }
            
            // 处理确认按键
            if (confirmKey.isDown() && !wasConfirmPressed) {
                if (manager.isRecording()) {
                    manager.finishRecording();
                } else {
                    client.player.sendSystemMessage(
                        Component.literal(I18nManager.translate("expandarea.error.area.record.expand"))
                            .withStyle(ChatFormatting.RED));
                }
            }
            wasConfirmPressed = confirmKey.isDown();
        });
    }
}
