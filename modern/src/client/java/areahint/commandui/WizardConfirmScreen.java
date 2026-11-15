package areahint.commandui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 通用确认页，用于危险操作、一次性命令和最终提交。
 */
public class WizardConfirmScreen extends CommandWizardScreen {
    private final String prompt;
    private final List<String> details;
    private final String confirmKey;
    private final Runnable confirmAction;

    public WizardConfirmScreen(Screen parent, String titleKey, String prompt, List<String> details,
                               String confirmKey, Runnable confirmAction, Runnable cancelAction) {
        super(titleKey, parent, cancelAction);
        this.prompt = prompt;
        this.details = details == null ? List.of() : details;
        this.confirmKey = confirmKey == null ? "commandui.button.confirm" : confirmKey;
        this.confirmAction = confirmAction;
    }

    @Override
    protected void init() {
        int buttonWidth = 92;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addRenderableWidget(Button.builder(Component.literal(t(this.confirmKey)), button -> {
            markFlowHandled();
            if (this.minecraft != null) {
                areahint.util.ClientScreenCompat.setScreen(this.minecraft, null);
            }
            if (this.confirmAction != null) {
                this.confirmAction.run();
            }
        }).bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(430, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 42;
        if (this.prompt != null && !this.prompt.isBlank()) {
            drawTrimmed(context, Component.literal(this.prompt), x, y, contentWidth, 0xFFFFFF);
            y += 18;
        }
        for (String detail : this.details) {
            drawTrimmed(context, Component.literal(detail), x, y, contentWidth, 0xAAAAAA);
            y += 13;
        }
    }
}
