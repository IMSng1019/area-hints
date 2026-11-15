package areahint.debug;

import areahint.AreashintClient;
import areahint.i18n.I18nManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 客户端调试管理器
 * 用于客户端调试功能和向玩家发送调试信息
 */
public class ClientDebugManager {
    // 是否启用调试
    private static boolean debugEnabled = false;
    
    /**
     * 启用调试模式
     */
    public static void enableDebug() {
        debugEnabled = true;
        AreashintClient.LOGGER.info("客户端调试模式已启用");
        sendDebugMessage(I18nManager.translate("debug.hint.general"), ChatFormatting.GREEN);
    }
    
    /**
     * 禁用调试模式
     */
    public static void disableDebug() {
        debugEnabled = false;
        AreashintClient.LOGGER.info("客户端调试模式已禁用");
        sendDebugMessage(I18nManager.translate("debug.hint.general_2"), ChatFormatting.YELLOW);
    }
    
    /**
     * 检查是否启用了调试
     * @return 是否启用了调试
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * 发送调试信息
     * @param category 调试类别
     * @param message 调试信息
     */
    public static void sendDebugInfo(DebugCategory category, String message) {
        if (!debugEnabled) {
            return; // 如果没有启用调试，直接返回
        }
        
        // 记录到客户端日志
        AreashintClient.LOGGER.debug("[{}] {}", category.name(), message);
        
        // 发送到屏幕
        String formattedMessage = String.format("[%s] %s", category.name(), message);
        sendDebugMessage(formattedMessage, category.getFormatting());
    }
    
    /**
     * 向玩家发送带格式的调试消息
     * @param message 消息
     * @param formatting 格式
     */
    private static void sendDebugMessage(String message, ChatFormatting formatting) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            Component text = Component.literal(I18nManager.translate("debug.button.general")).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(message).withStyle(formatting));
            client.player.sendSystemMessage(text);
        }
    }
    
    /**
     * 调试类别枚举
     */
    public enum DebugCategory {
        AREA_DETECTION(ChatFormatting.AQUA),
        PLAYER_POSITION(ChatFormatting.GREEN),
        CONFIG(ChatFormatting.YELLOW),
        NETWORK(ChatFormatting.LIGHT_PURPLE),
        RENDER(ChatFormatting.BLUE),
        COMMAND(ChatFormatting.WHITE),
        EASY_ADD(ChatFormatting.GOLD),
        GENERAL(ChatFormatting.GRAY);
        
        private final ChatFormatting formatting;
        
        DebugCategory(ChatFormatting formatting) {
            this.formatting = formatting;
        }
        
        public ChatFormatting getFormatting() {
            return formatting;
        }
    }
} 