package areahint.language;

import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import areahint.AreashintClient;

/**
 * 语言选择交互式管理器
 */
public class LanguageManager {
    public enum State {
        IDLE,
        SELECTING_LANGUAGE
    }

    private static LanguageManager instance;
    private State currentState = State.IDLE;

    private LanguageManager() {}

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    /**
     * 启动语言选择流程
     */
    public void startLanguageSelection() {
        if (currentState != State.IDLE) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.error.language")));
            }
            return;
        }

        currentState = State.SELECTING_LANGUAGE;
        ClientConfig.setLanguageLocked(true);
        LanguageUI.showLanguageSelectionScreen();
    }

    /**
     * 启动图形化语言选择流程，只占用原语言状态机，不重复发送聊天按钮。
     */
    public void startVisualLanguageSelection() {
        if (currentState == State.IDLE) {
            currentState = State.SELECTING_LANGUAGE;
            ClientConfig.setLanguageLocked(true);
        }
    }

    /**
     * 处理语言选择
     * @param langCode 语言代码
     */
    public void handleLanguageSelection(String langCode) {
        if (currentState != State.SELECTING_LANGUAGE) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 先加载语言，并获取最终实际生效的语言代码
        String appliedLanguage = I18nManager.loadLanguage(langCode);

        // 保存实际生效语言
        ClientConfig.setLanguage(appliedLanguage);

        // 同步语言给服务端
        areahint.network.ClientNetworking.sendLanguageToServer();

        String displayName = I18nManager.getLanguageDisplayName(appliedLanguage);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.message.language") + displayName + " (" + appliedLanguage + ")"));

        resetState();
    }

    /**
     * 取消语言选择
     */
    public void cancelLanguageSelection() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("language.prompt.cancel.language")));
        }
        resetState();
    }

    private void resetState() {
        currentState = State.IDLE;
    }

    /**
     * 图形界面被绑定键关闭或返回时丢弃本次可视化选择进度。
     */
    public void discardVisualLanguageSelection() {
        if (currentState == State.SELECTING_LANGUAGE) {
            resetState();
        }
    }

    public State getCurrentState() {
        return currentState;
    }
}
