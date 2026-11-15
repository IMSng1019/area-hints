package areahint.titlesize;

import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import areahint.AreashintClient;

/**
 * TitleSize交互式管理器
 * 负责域名标题大小设置的交互流程
 */
public class TitleSizeManager {

    /**
     * TitleSize状态枚举
     */
    public enum TitleSizeState {
        IDLE,           // 空闲状态
        SELECTING_SIZE  // 选择大小状态
    }

    // 单例实例
    private static TitleSizeManager instance;

    // 当前状态
    private TitleSizeState currentState = TitleSizeState.IDLE;

    // 私有构造函数（单例模式）
    private TitleSizeManager() {}

    /**
     * 获取单例实例
     */
    public static TitleSizeManager getInstance() {
        if (instance == null) {
            instance = new TitleSizeManager();
        }
        return instance;
    }

    /**
     * 启动TitleSize交互流程
     */
    public void startTitleSizeSelection() {
        if (currentState != TitleSizeState.IDLE) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_5")));
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            // 设置状态并显示UI
            currentState = TitleSizeState.SELECTING_SIZE;

            // 获取当前大小
            String currentSize = ClientConfig.getTitleSize();

            // 显示选择界面
            TitleSizeUI.showSizeSelectionScreen(currentSize);
        }
    }

    /**
     * 处理大小选择
     * @param size 选择的大小
     */
    public void handleSizeSelection(String size) {
        if (currentState != TitleSizeState.SELECTING_SIZE) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 验证大小有效性
        if (!isValidSize(size)) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_7") + size));
            return;
        }

        // 保存大小设置
        ClientConfig.setTitleSize(size);

        // 显示成功消息
        String sizeDisplay = getSizeDisplayName(size);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.general_46") + sizeDisplay));

        // 执行reload
        AreashintClient.reload();
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.general_51")));

        // 重置状态
        resetState();
    }

    /**
     * 取消TitleSize流程
     */
    public void cancelTitleSize() {
        Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.cancel_4")));
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        currentState = TitleSizeState.IDLE;
    }

    /**
     * 验证大小是否有效
     */
    private boolean isValidSize(String size) {
        return size.equals("extra_large") || size.equals("large") || size.equals("medium_large") ||
               size.equals("medium") || size.equals("medium_small") || size.equals("small") ||
               size.equals("extra_small");
    }

    /**
     * 获取大小的显示名称
     */
    private String getSizeDisplayName(String size) {
        switch (size) {
            case "extra_large":
                return I18nManager.translate("message.message.general_193");
            case "large":
                return I18nManager.translate("message.message.general_104");
            case "medium_large":
                return I18nManager.translate("message.message.general_225");
            case "medium":
                return I18nManager.translate("message.message.general_58");
            case "medium_small":
                return I18nManager.translate("message.message.general_226");
            case "small":
                return I18nManager.translate("message.message.general_111");
            case "extra_small":
                return I18nManager.translate("message.message.general_194");
            default:
                return size;
        }
    }

    // Getters
    public TitleSizeState getCurrentState() {
        return currentState;
    }
}
