package areahint.commandui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 通用执行页，用于显示将要发送的原始指令。
 */
public class WizardExecutionScreen extends CommandUiScreen {
    private final String command;
    private final String promptKey;

    public WizardExecutionScreen(Screen parent, String titleKey, String promptKey, String command) {
        super(titleKey, parent);
        this.promptKey = promptKey;
        this.command = command;
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.execute")), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            CommandUiActions.runCommand(this.command);
        }).bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> closeToGameFromBoundKey())
            .bounds(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(430, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = Math.max(44, this.height / 2 - 34);
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 18;
        }
        drawTrimmed(context, Component.literal("/" + this.command), x, y, contentWidth, BRIGHT_YELLOW);
    }
}
