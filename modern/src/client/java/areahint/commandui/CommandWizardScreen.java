package areahint.commandui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 指令向导界面基类，统一把 Esc、绑定键和取消按钮视为丢弃当前流程。
 */
public abstract class CommandWizardScreen extends CommandUiScreen {
    private final Runnable cancelAction;
    private boolean cancelHandled;
    private boolean returnToParentOnCancel;

    protected CommandWizardScreen(String titleKey, Screen parent, Runnable cancelAction) {
        super(titleKey, parent);
        this.cancelAction = cancelAction;
    }

    protected final void cancelAndCloseToGame() {
        cancelFlow();
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, this.returnToParentOnCancel ? this.parent : null);
        }
    }

    // 嵌入编辑器时取消只返回属性页，独立指令向导仍沿用关闭到游戏的默认行为。
    public final void setReturnToParentOnCancel(boolean returnToParentOnCancel) {
        this.returnToParentOnCancel = returnToParentOnCancel;
    }

    @Override
    protected boolean closesOnBoundKey() {
        // 嵌入属性页的文本框允许正常输入绑定字符，避免默认 X 键把未确认内容关闭。
        return !this.returnToParentOnCancel || !(this instanceof WizardTextInputScreen || this instanceof WizardLongTextInputScreen);
    }

    @Override
    protected void closeToGameFromBoundKey() {
        if (!closesOnBoundKey()) return;
        if (!this.returnToParentOnCancel) { super.closeToGameFromBoundKey(); return; }
        areahint.keyhandler.UnifiedKeyHandler.suppressRecordKeyUntilRelease();
        cancelAndCloseToGame();
    }

    protected final void cancelFlow() {
        if (this.cancelHandled) {
            return;
        }
        this.cancelHandled = true;
        CommandVisualLaunchContext.clear();
        if (this.cancelAction != null) {
            this.cancelAction.run();
        }
    }

    protected final void markFlowHandled() {
        this.cancelHandled = true;
    }

    @Override
    protected void onDiscard() {
        cancelFlow();
    }

    @Override
    public void onClose() {
        cancelAndCloseToGame();
    }
}
