package areahint.replacebutton;

import com.mojang.blaze3d.platform.InputConstants;

import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;

/**
 * ReplaceButton管理器
 * 管理按键更换流程的状态和逻辑
 */
public class ReplaceButtonManager {

    // 单例实例
    private static ReplaceButtonManager instance;

    // 状态枚举
    public enum ReplaceButtonState {
        IDLE,              // 空闲状态
        WAITING_FOR_KEY,   // 等待玩家按键
        CONFIRMING         // 等待确认
    }

    // 当前状态
    private ReplaceButtonState currentState = ReplaceButtonState.IDLE;

    // 临时存储的新按键代码
    private int pendingKeyCode = -1;

    // 临时存储的新按键名称
    private String pendingKeyName = "";

    // 是否由图形化指令入口启动，用于在 Screen 和聊天按钮之间切换展示。
    private boolean visualFlowActive = false;

    /**
     * 私有构造方法（单例模式）
     */
    private ReplaceButtonManager() {
    }

    /**
     * 获取单例实例
     */
    public static ReplaceButtonManager getInstance() {
        if (instance == null) {
            instance = new ReplaceButtonManager();
        }
        return instance;
    }

    /**
     * 开始按键更换流程
     */
    public void startReplaceButton() {
        boolean visualRequested = ReplaceButtonVisualController.consumeVisualStartRequest();
        visualFlowActive = visualRequested;
        currentState = ReplaceButtonState.WAITING_FOR_KEY;
        showWaitingForKey(null);
    }

    /**
     * 处理玩家按下的键
     * @param keyCode 当前平台原生键码，写入配置前转换回历史编号
     * @param keyName 按键名称
     */
    public void handleKeyPress(int keyCode, String keyName) {
        if (currentState != ReplaceButtonState.WAITING_FOR_KEY) {
            return;
        }

        // 忽略某些特殊键
        int storedCode = areahint.util.PlatformInputCompat.toStoredCode(keyCode);
        if (isInvalidKey(keyCode) || storedCode <= 0) {
            showInvalidKeyError();
            return;
        }

        // 保存临时按键信息
        pendingKeyCode = storedCode;
        pendingKeyName = keyName;

        // 切换到确认状态
        currentState = ReplaceButtonState.CONFIRMING;
        showConfirmScreen(keyName);
    }

    /**
     * 确认使用新按键
     */
    public void confirmNewKey() {
        if (currentState != ReplaceButtonState.CONFIRMING) {
            return;
        }

        // 保存新按键到配置
        ClientConfig.setRecordKey(pendingKeyCode);

        // 通知 UnifiedKeyHandler 重新注册按键
        areahint.keyhandler.UnifiedKeyHandler.reregisterKey();

        // 显示成功消息
        ReplaceButtonUI.showSuccess(I18nManager.translate("replacebutton.message.record.key") + pendingKeyName);

        // 重置状态
        reset();
    }

    /**
     * 取消按键更换
     */
    public void cancel() {
        ReplaceButtonUI.showInfo(I18nManager.translate("replacebutton.message.cancel.key"));
        reset();
    }

    /**
     * 重置状态
     */
    public void reset() {
        currentState = ReplaceButtonState.IDLE;
        pendingKeyCode = -1;
        pendingKeyName = "";
        visualFlowActive = false;
        ReplaceButtonVisualController.clear();
    }

    /**
     * 获取当前状态
     */
    public ReplaceButtonState getCurrentState() {
        return currentState;
    }

    /**
     * 检查是否处于活跃状态
     */
    public boolean isActive() {
        return currentState != ReplaceButtonState.IDLE;
    }

    /**
     * 检查按键是否无效（不能用作记录键）
     */
    private boolean isInvalidKey(int keyCode) {
        // 不允许使用的按键
        return keyCode == InputConstants.KEY_ESCAPE ||      // ESC键
               keyCode == InputConstants.KEY_RETURN ||       // 回车键
               keyCode == InputConstants.KEY_TAB ||         // Tab键
               keyCode == InputConstants.KEY_LSHIFT ||  // Shift键
               keyCode == InputConstants.KEY_RSHIFT ||
               keyCode == InputConstants.KEY_LCONTROL || // Ctrl键
               keyCode == InputConstants.KEY_RCONTROL ||
               keyCode == InputConstants.KEY_LALT ||    // Alt键
               keyCode == InputConstants.KEY_RALT ||
               keyCode == InputConstants.UNKNOWN.getValue();       // 未知键
    }

    /**
     * 显示等待按键界面，图形流程使用 Screen，聊天流程保持原交互。
     */
    private void showWaitingForKey(String errorText) {
        if (visualFlowActive) {
            ReplaceButtonVisualController.showWaitingForKeyScreen(errorText);
        } else {
            ReplaceButtonUI.showWaitingForKeyScreen();
        }
    }

    /**
     * 显示非法按键提示，图形流程把错误留在当前 Screen。
     */
    private void showInvalidKeyError() {
        String errorText = I18nManager.translate("replacebutton.message.record.key_2");
        if (visualFlowActive) {
            ReplaceButtonVisualController.showWaitingForKeyScreen(errorText);
        } else {
            ReplaceButtonUI.showError(errorText);
        }
    }

    /**
     * 显示确认界面，后续确认仍走原有 replacebutton confirm 子命令。
     */
    private void showConfirmScreen(String keyName) {
        if (visualFlowActive) {
            ReplaceButtonVisualController.showConfirmScreen(keyName);
        } else {
            ReplaceButtonUI.showConfirmScreen(keyName);
        }
    }
}
