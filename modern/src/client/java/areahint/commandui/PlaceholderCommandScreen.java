package areahint.commandui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 未接入专属图形流程的指令占位页。
 */
public class PlaceholderCommandScreen extends CommandUiScreen {
    private final CommandVisualHandler handler;

    public PlaceholderCommandScreen(Screen parent, CommandVisualHandler handler) {
        super("commandui.placeholder.title", parent);
        this.handler = handler;
    }

    @Override
    protected void init() {
        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 112;
        int totalWidth = buttonWidth * 3 + 8;
        int x = (this.width - totalWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.use_original")),
            button -> CommandUiActions.runCommandAndClose(this, handler.defaultCommand()))
            .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.back")), button -> onClose())
            .bounds(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
            .bounds(x + (buttonWidth + 4) * 2, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(520, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 54;
        drawTrimmed(context, handler.displayName(), x, y, contentWidth, 0xFFFFFF);
        drawTrimmed(context, handler.description(), x, y + 18, contentWidth, 0xAAAAAA);
        drawTrimmed(context, Component.literal(t("commandui.placeholder.not_implemented")), x, y + 42, contentWidth, BRIGHT_YELLOW);
        String command = handler.defaultCommand();
        drawTrimmed(context, Component.literal(t("commandui.placeholder.command") + (command == null ? "" : "/" + command)),
            x, y + 60, contentWidth, 0xAAAAAA);
    }
}
