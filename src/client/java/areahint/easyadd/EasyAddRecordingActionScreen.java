package areahint.easyadd;

import areahint.commandui.CommandUiScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * EasyAdd录点阶段的长按操作面板，避免玩家手动输入完成或取消指令。
 */
public class EasyAddRecordingActionScreen extends CommandUiScreen {
    private boolean handled;

    public EasyAddRecordingActionScreen(Screen parent) {
        super("commandui.easyadd.record.panel.title", parent);
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = Math.min(this.height - FOOTER_Y_OFFSET, this.height / 2 + 22);

        ButtonWidget finishButton = ButtonWidget.builder(Text.literal(t("commandui.button.finish")), button -> {
            this.handled = true;
            EasyAddManager.getInstance().finishPointRecording();
        }).dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build();
        finishButton.active = EasyAddManager.getInstance().getRecordedPoints().size() >= 3;
        this.addDrawableChild(finishButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> {
            this.handled = true;
            EasyAddManager.getInstance().cancelEasyAdd();
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }).dimensions(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    protected void onDiscard() {
        if (!this.handled
                && EasyAddManager.getInstance().getCurrentState() == EasyAddManager.EasyAddState.RECORDING_POINTS) {
            EasyAddVisualController.showRecordingHud();
        }
    }

    @Override
    public void close() {
        onDiscard();
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(300, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = Math.max(34, this.height / 2 - 28);
        int count = EasyAddManager.getInstance().getRecordedPoints().size();
        drawTrimmed(context, Text.literal(t("commandui.easyadd.record.panel.prompt")),
            x, y, contentWidth, 0xFFFFFF);
        drawTrimmed(context, Text.literal(I18nManager.translate("commandui.easyadd.record.count", count)),
            x, y + 14, contentWidth, BRIGHT_YELLOW);
        drawTrimmed(context, Text.literal(t(count >= 3
                ? "commandui.easyadd.record.panel.ready"
                : "commandui.easyadd.record.panel.need_more")),
            x, y + 28, contentWidth, count >= 3 ? BRIGHT_GREEN : 0xFFAA55);
    }
}
