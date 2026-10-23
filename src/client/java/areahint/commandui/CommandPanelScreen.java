package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.commands")),
            button -> MinecraftClient.getInstance().setScreen(new CommandListScreen(this)))
            .dimensions(x, startY + gap, buttonWidth, BUTTON_HEIGHT).build());
        addEntryButton(x, startY + gap * 2, buttonWidth, "commandui.button.language", "language");
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.settings")),
            button -> AreasHintSettingsBridge.open(this))
            .dimensions(x, startY + gap * 3, buttonWidth, BUTTON_HEIGHT).build());
        addEntryButton(x, startY + gap * 4, buttonWidth, "commandui.button.help", "help");
        if (this.approvalVisible) {
            String label = this.approvalOperator
                ? "approval.ui.requests" : "approval.ui.replies";
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t(label)),
                button -> areahint.approval.ApprovalClientManager.openList(this))
                .dimensions(x, startY + gap * 5, buttonWidth, BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void tick() {
        super.tick();
        boolean visible = areahint.approval.ApprovalClientManager.canOpenPanel();
        boolean operator = areahint.approval.ApprovalClientManager.isOperator();
        // 登录快照或身份变化只在入口显隐、申请与回复标签变化时重排，不随每条审批更新重建按钮。
        if (visible != this.approvalVisible || visible && operator != this.approvalOperator) {
            this.clearAndInit();
        }
    }

    private void addEntryButton(int x, int y, int width, String textKey, String commandId) {
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t(textKey)), button -> openHandler(commandId))
            .dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    private void openHandler(String commandId) {
        CommandVisualHandler handler = CommandVisualRegistry.getById(commandId);
        if (handler != null) {
            handler.open(this);
        }
    }
}
