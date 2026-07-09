package areahint.commandui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

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
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.use_original")),
            button -> CommandUiActions.runCommandAndClose(this, handler.defaultCommand()))
            .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.back")), button -> close())
            .dimensions(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
            .dimensions(x + (buttonWidth + 4) * 2, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(520, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 54;
        drawTrimmed(context, handler.displayName(), x, y, contentWidth, 0xFFFFFF);
        drawTrimmed(context, handler.description(), x, y + 18, contentWidth, 0xAAAAAA);
        drawTrimmed(context, Text.literal(t("commandui.placeholder.not_implemented")), x, y + 42, contentWidth, BRIGHT_YELLOW);
        String command = handler.defaultCommand();
        drawTrimmed(context, Text.literal(t("commandui.placeholder.command") + (command == null ? "" : "/" + command)),
            x, y + 60, contentWidth, 0xAAAAAA);
    }
}
