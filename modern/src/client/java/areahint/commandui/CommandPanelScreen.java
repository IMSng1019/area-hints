package areahint.commandui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 指令可视化主面板，只保留入口按钮，完整指令列表放在命令面板中。
 */
public class CommandPanelScreen extends CommandUiScreen {
    private boolean approvalVisible;
    private boolean approvalOperator;

    public CommandPanelScreen() {
        super("commandui.title", null);
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(180, this.width - 40);
        int x = (this.width - buttonWidth) / 2;
        this.approvalVisible = areahint.approval.ApprovalClientManager.canOpenPanel();
        this.approvalOperator = areahint.approval.ApprovalClientManager.isOperator();
        int startY = Math.max(38, this.height / 2 - (this.approvalVisible ? 70 : 58));
        int gap = 24;

        addEntryButton(x, startY, buttonWidth, "commandui.button.boundviz", "boundviz");
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.commands")),
            button -> areahint.util.ClientScreenCompat.setScreen(Minecraft.getInstance(), new CommandListScreen(this)))
            .bounds(x, startY + gap, buttonWidth, BUTTON_HEIGHT).build());
        addEntryButton(x, startY + gap * 2, buttonWidth, "commandui.button.language", "language");
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.settings")),
            button -> AreasHintSettingsBridge.open(this))
            .bounds(x, startY + gap * 3, buttonWidth, BUTTON_HEIGHT).build());
        addEntryButton(x, startY + gap * 4, buttonWidth, "commandui.button.help", "help");
        if (this.approvalVisible) {
            String label = this.approvalOperator
                ? "approval.ui.requests" : "approval.ui.replies";
            this.addRenderableWidget(Button.builder(Component.literal(t(label)),
                button -> areahint.approval.ApprovalClientManager.openList(this))
                .bounds(x, startY + gap * 5, buttonWidth, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        boolean visible = areahint.approval.ApprovalClientManager.canOpenPanel();
        boolean operator = areahint.approval.ApprovalClientManager.isOperator();
        // 登录快照或身份变化只在入口显隐、申请与回复标签变化时重排，不随每条审批更新重建按钮。
        if (visible != this.approvalVisible || visible && operator != this.approvalOperator) {
            this.rebuildWidgets();
        }
    }

    private void addEntryButton(int x, int y, int width, String textKey, String commandId) {
        this.addRenderableWidget(Button.builder(Component.literal(t(textKey)), button -> openHandler(commandId))
            .bounds(x, y, width, BUTTON_HEIGHT).build());
    }

    private void openHandler(String commandId) {
        CommandVisualHandler handler = CommandVisualRegistry.getById(commandId);
        if (handler != null) {
            handler.open(this);
        }
    }
}
