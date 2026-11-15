package areahint.command;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 模组开关命令处理器
 * 处理客户端的 /areahint on 和 /areahint off 命令
 */
public class ModToggleCommand {
    
    /**
     * 处理模组开关命令
     * @param action 命令参数（"on" 或 "off"）
     */
    public static void handleToggleCommand(String action) {
        Minecraft client = Minecraft.getInstance();
        
        if (action == null || action.trim().isEmpty()) {
            // 如果没有参数，显示当前状态
            showCurrentStatus(client);
            return;
        }
        
        String command = action.trim().toLowerCase();
        
        switch (command) {
            case "on":
                enableMod(client);
                break;
            case "off":
                disableMod(client);
                break;
            default:
                showUsage(client);
                break;
        }
    }
    
    /**
     * 启用模组
     * @param client Minecraft客户端实例
     */
    private static void enableMod(Minecraft client) {
        ClientConfig.setEnabled(true);
        
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("command.button.area")));
        }

        // 开启后立即按当前位置恢复一次域名检测和显示，避免必须等待下一次移动或检测周期。
        AreashintClient.forceRedetectCurrentArea();
        
        AreashintClient.LOGGER.info("模组已通过命令启用");
    }
    
    /**
     * 禁用模组
     * @param client Minecraft客户端实例
     */
    private static void disableMod(Minecraft client) {
        ClientConfig.setEnabled(false);
        AreashintClient.clearAreaDisplayState();
        
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("command.error.area")));
        }
        
        AreashintClient.LOGGER.info("模组已通过命令禁用");
    }
    
    /**
     * 显示当前模组状态
     * @param client Minecraft客户端实例
     */
    private static void showCurrentStatus(Minecraft client) {
        boolean enabled = ClientConfig.isEnabled();
        String status = enabled ? I18nManager.translate("command.message.general_12") : I18nManager.translate("command.error.general_11");
        
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(String.format(I18nManager.translate("command.button.general"), status)));
        }
    }
    
    /**
     * 显示使用方法
     * @param client Minecraft客户端实例
     */
    private static void showUsage(Minecraft client) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("command.button.general_2")));
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("command.usage.on")));
            client.player.sendSystemMessage(Component.literal(I18nManager.translate("command.usage.off")));
        }
    }
} 
