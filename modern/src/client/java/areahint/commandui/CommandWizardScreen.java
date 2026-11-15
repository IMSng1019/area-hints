package areahint.commandui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 指令向导界面基类，统一把 Esc、绑定键和取消按钮视为丢弃当前流程。
 */
public abstract class CommandWizardScreen extends CommandUiScreen {
    private final Runnable cancelAction;
    private boolean cancelHandled;

    protected CommandWizardScreen(String titleKey, Screen parent, Runnable cancelAction) {
        super(titleKey, parent);
        this.cancelAction = cancelAction;
    }

    protected final void cancelAndCloseToGame() {
        cancelFlow();
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
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
