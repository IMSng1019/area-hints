package areahint.titlestyle;

import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import areahint.AreashintClient;

/**
 * TitleStyle交互式管理器
 * 负责域名标题样式设置的交互流程
 */
public class TitleStyleManager {

    /**
     * TitleStyle状态枚举
     */
    public enum TitleStyleState {
        IDLE,           // 空闲状态
        SELECTING_STYLE // 选择样式状态
    }

    // 单例实例
    private static TitleStyleManager instance;

    // 当前状态
    private TitleStyleState currentState = TitleStyleState.IDLE;

    // 私有构造函数（单例模式）
    private TitleStyleManager() {}

    /**
     * 获取单例实例
     */
    public static TitleStyleManager getInstance() {
        if (instance == null) {
            instance = new TitleStyleManager();
        }
        return instance;
    }

    /**
     * 启动TitleStyle交互流程
     */
    public void startTitleStyleSelection() {
        if (currentState != TitleStyleState.IDLE) {
            Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_6")));
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            // 设置状态并显示UI
            currentState = TitleStyleState.SELECTING_STYLE;

            // 获取当前样式
            String currentStyle = ClientConfig.getTitleStyle();

            // 显示选择界面
            TitleStyleUI.showStyleSelectionScreen(currentStyle);
        }
    }

    /**
     * 处理样式选择
     * @param style 选择的样式
     */
    public void handleStyleSelection(String style) {
        if (currentState != TitleStyleState.SELECTING_STYLE) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 验证样式有效性
        if (!isValidStyle(style)) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.error.general_8") + style));
            return;
        }

        // 保存样式设置
        ClientConfig.setTitleStyle(style);

        // 显示成功消息
        String styleDisplay = getStyleDisplayName(style);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.general_47") + styleDisplay));

        // 执行reload
        AreashintClient.reload();
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.general_51")));

        // 重置状态
        resetState();
    }

    /**
     * 取消TitleStyle流程
     */
    public void cancelTitleStyle() {
        Minecraft.getInstance().player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("message.message.cancel_5")));
        resetState();
    }

    /**
     * 重置状态
     */
    private void resetState() {
        currentState = TitleStyleState.IDLE;
    }

    /**
     * 验证样式是否有效
     */
    private boolean isValidStyle(String style) {
        return style.equals("full") || style.equals("simple") || style.equals("mixed");
    }

    /**
     * 获取样式的显示名称
     */
    private String getStyleDisplayName(String style) {
        switch (style) {
            case "full":
                return I18nManager.translate("message.message.general_106");
            case "simple":
                return I18nManager.translate("message.message.general_215");
            case "mixed":
                return I18nManager.translate("message.message.general_209");
            default:
                return style;
        }
    }

    // Getters
    public TitleStyleState getCurrentState() {
        return currentState;
    }
}
