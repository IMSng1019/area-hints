package areahint.debug;

import areahint.Areashint;
import areahint.i18n.ServerI18nManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 调试管理器
 * 用于管理调试功能和向玩家发送调试信息
 */
public class DebugManager {
    // 启用调试的玩家UUID集合
    private static final Set<UUID> debugEnabledPlayers = new HashSet<>();
    
    // 是否有任何玩家启用了调试
    private static boolean anyDebugEnabled = false;
    
    /**
     * 为指定玩家启用调试模式
     * @param player 玩家
     * @return 是否成功启用
     */
    public static boolean enableDebug(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        boolean wasEnabled = debugEnabledPlayers.contains(playerUUID);
        
        if (!wasEnabled) {
            debugEnabledPlayers.add(playerUUID);
            anyDebugEnabled = true;
            Areashint.LOGGER.info(ServerI18nManager.translate("debug.message.general"), player.getName().getString());
            sendTranslatableDebugMessage(player, "debug.hint.general", ChatFormatting.GREEN);
        } else {
            sendTranslatableDebugMessage(player, "debug.message.general_5", ChatFormatting.YELLOW);
        }
        
        return !wasEnabled;
    }
    
    /**
     * 为指定玩家禁用调试模式
     * @param player 玩家
     * @return 是否成功禁用
     */
    public static boolean disableDebug(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        boolean wasEnabled = debugEnabledPlayers.remove(playerUUID);
        
        // 更新全局调试状态
        anyDebugEnabled = !debugEnabledPlayers.isEmpty();
        
        if (wasEnabled) {
            Areashint.LOGGER.info(ServerI18nManager.translate("debug.message.general_2"), player.getName().getString());
            sendTranslatableDebugMessage(player, "debug.hint.general_2", ChatFormatting.YELLOW);
        } else {
            sendTranslatableDebugMessage(player, "debug.message.general_6", ChatFormatting.RED);
        }
        
        return wasEnabled;
    }
    
    /**
     * 检查是否有任何玩家启用了调试
     * @return 是否有任何玩家启用了调试
     */
    public static boolean isAnyDebugEnabled() {
        return anyDebugEnabled;
    }
    
    /**
     * 检查指定玩家是否启用了调试
     * @param playerUUID 玩家UUID
     * @return 是否启用了调试
     */
    public static boolean isDebugEnabled(UUID playerUUID) {
        return debugEnabledPlayers.contains(playerUUID);
    }
    
    /**
     * 向启用调试的玩家发送调试信息
     * @param category 调试类别
     * @param message 调试信息
     */
    public static void sendDebugInfo(DebugCategory category, String message) {
        if (!anyDebugEnabled) {
            return; // 如果没有玩家启用调试，直接返回，避免不必要的处理
        }
        
        // 记录到服务端日志
        Areashint.LOGGER.debug("[{}] {}", category.name(), message);
        
        // 遍历所有启用调试的玩家
        for (UUID playerUUID : debugEnabledPlayers) {
            ServerPlayer player = Areashint.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null && player.isAlive()) {
                String formattedMessage = String.format("[%s] %s", category.name(), message);
                sendDebugMessage(player, formattedMessage, category.getFormatting());
            }
        }
    }
    
    /**
     * 向玩家发送带格式的调试消息
     * @param player 玩家
     * @param message 消息
     * @param formatting 格式
     */
    private static void sendDebugMessage(ServerPlayer player, String message, ChatFormatting formatting) {
        Component text = Component.translatable("debug.button.general").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(message).withStyle(formatting));
        player.sendSystemMessage(text);
    }

    private static void sendTranslatableDebugMessage(ServerPlayer player, String key, ChatFormatting formatting) {
        Component text = Component.literal(ServerI18nManager.translateForPlayer(player.getUUID(), "debug.button.general")).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(ServerI18nManager.translateForPlayer(player.getUUID(), key)).withStyle(formatting));
        player.sendSystemMessage(text);
    }

    public static void sendTranslatableDebugInfo(DebugCategory category, String key) {
        if (!anyDebugEnabled) return;
        for (UUID playerUUID : debugEnabledPlayers) {
            ServerPlayer player = Areashint.getServer().getPlayerList().getPlayer(playerUUID);
            if (player != null && player.isAlive()) {
                Component text = Component.literal(ServerI18nManager.translateForPlayer(playerUUID, "debug.button.general")).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("[" + category.name() + "] ").withStyle(category.getFormatting()))
                        .append(Component.literal(ServerI18nManager.translateForPlayer(playerUUID, key)).withStyle(category.getFormatting()));
                player.sendSystemMessage(text);
            }
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