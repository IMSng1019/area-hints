package areahint.commandui;

import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * 录点类指令的长按操作面板，负责继续、完成和取消当前图形流程。
 */
public class RecordCommandActionScreen extends CommandUiScreen {
    private final RecordFlow flow;
    private boolean handled;

    public RecordCommandActionScreen(Screen parent, RecordFlow flow) {
        super("commandui." + flow.id() + ".title", parent);
        this.flow = flow;
    }

    public static boolean openActive(Screen parent) {
        RecordFlow flow = RecordFlow.active();
        if (flow == null) {
            return false;
        }
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new RecordCommandActionScreen(parent, flow));
            return true;
        }
        return false;
    }

    @Override
    protected void init() {
        int buttonWidth = 82;
        int totalWidth = buttonWidth * 3 + 16;
        int x = (this.width - totalWidth) / 2;
        int y = Math.min(this.height - FOOTER_Y_OFFSET, this.height / 2 + 28);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.continue")), button -> {
            this.handled = true;
            this.flow.continueAction().run();
            if (this.client != null) {
                this.client.setScreen(null);
            }
        }).dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());

        ButtonWidget finishButton = ButtonWidget.builder(Text.literal(t("commandui.button.finish")), button -> {
            this.handled = true;
            if (this.client != null) {
                this.client.setScreen(null);
            }
            this.flow.finishAction().run();
            if ("dividearea".equals(this.flow.id())) {
                areahint.dividearea.DivideAreaVisualController.openConfig(null);
            } else if (!this.flow.isStillRecording()) {
                CommandVisualController.clearVisualRecordMode();
            }
        }).dimensions(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build();
        finishButton.active = this.flow.canFinish();
        this.addDrawableChild(finishButton);

        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> {
            this.handled = true;
            if (this.client != null) {
                this.client.setScreen(null);
            }
            this.flow.cancelAction().run();
            CommandVisualController.clearVisualRecordMode();
        }).dimensions(x + (buttonWidth + 8) * 2, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    protected void onDiscard() {
        if (!this.handled && this.flow.isStillRecording()) {
            this.flow.continueAction().run();
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
        int contentWidth = Math.min(340, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = Math.max(34, this.height / 2 - 34);
        drawTrimmed(context, Text.literal(t("commandui.record.panel.prompt")), x, y, contentWidth, 0xFFFFFF);
        drawTrimmed(context, Text.literal(I18nManager.translate("commandui.record.panel.count", this.flow.recordedCount())),
            x, y + 14, contentWidth, BRIGHT_YELLOW);
        drawTrimmed(context, Text.literal(t(this.flow.canFinish()
                ? "commandui.record.panel.ready"
                : "commandui.record.panel.need_more")),
            x, y + 28, contentWidth, this.flow.canFinish() ? BRIGHT_GREEN : 0xFFAA55);
    }

    public record RecordFlow(String id, int recordedCount, int minFinishCount, Runnable continueAction,
                             Runnable finishAction, Runnable cancelAction, java.util.function.BooleanSupplier recordingSupplier) {
        boolean canFinish() {
            return this.recordedCount >= this.minFinishCount;
        }

        boolean isStillRecording() {
            return this.recordingSupplier.getAsBoolean();
        }

        static RecordFlow active() {
            areahint.addhint.AddHintManager addHint = areahint.addhint.AddHintManager.getInstance();
            if (addHint.isActive() && addHint.isRecording()) {
                return new RecordFlow("addhint", addHint.getNewVertices().size(), 1,
                    () -> {
                    },
                    addHint::submit,
                    addHint::cancel,
                    addHint::isRecording);
            }

            areahint.expandarea.ExpandAreaManager expandArea = areahint.expandarea.ExpandAreaManager.getInstance();
            if (expandArea.isActive() && expandArea.isRecording()) {
                return new RecordFlow("expandarea", expandArea.getNewVertices().size(), 3,
                    expandArea::continueRecording,
                    expandArea::finishAndSave,
                    expandArea::cancel,
                    expandArea::isRecording);
            }

            areahint.shrinkarea.ShrinkAreaManager shrinkArea = areahint.shrinkarea.ShrinkAreaManager.getInstance();
            if (shrinkArea.isActive() && shrinkArea.isRecording()) {
                return new RecordFlow("shrinkarea", shrinkArea.getShrinkVertices().size(), 3,
                    shrinkArea::continueRecording,
                    shrinkArea::finishAndSave,
                    shrinkArea::stop,
                    shrinkArea::isRecording);
            }

            areahint.dividearea.DivideAreaManager divideArea = areahint.dividearea.DivideAreaManager.getInstance();
            if (divideArea.isActive() && divideArea.isRecording()) {
                return new RecordFlow("dividearea", divideArea.getNewVertices().size(), 2,
                    divideArea::continueRecording,
                    divideArea::finishAndSave,
                    divideArea::cancel,
                    divideArea::isRecording);
            }
            return null;
        }
    }
}
