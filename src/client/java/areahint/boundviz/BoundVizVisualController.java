package areahint.boundviz;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * boundviz 图形流程控制器，只把按钮操作转换为现有 /areahint boundviz 指令。
 */
public final class BoundVizVisualController {
    private BoundVizVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new BoundVizScreen(parent));
        }
    }

    private static final class BoundVizScreen extends CommandUiScreen {
        private ButtonWidget toggleButton;

        private BoundVizScreen(Screen parent) {
            super("commandui.boundviz.title", parent);
        }

        @Override
        protected void init() {
            int toggleWidth = Math.min(Math.max(160, this.textRenderer.getWidth(t("commandui.boundviz.toggle")) + 28),
                Math.max(120, this.width - 40));
            this.toggleButton = ButtonWidget.builder(Text.literal(t("commandui.boundviz.toggle")), button -> {
                // 继续走原指令，权限、网络转发和实际开关逻辑都由现有命令链处理。
                CommandUiActions.runCommand("areahint boundviz");
                button.setMessage(Text.literal(t("commandui.boundviz.toggle")));
            }).dimensions((this.width - toggleWidth) / 2, 82, toggleWidth, BUTTON_HEIGHT).build();
            this.addDrawableChild(this.toggleButton);

            int y = this.height - FOOTER_Y_OFFSET;
            int buttonWidth = 90;
            int x = (this.width - (buttonWidth * 2 + 4)) / 2;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.back")), button -> close())
                .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
                .dimensions(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            if (this.toggleButton != null) {
                this.toggleButton.setMessage(Text.literal(t("commandui.boundviz.toggle")));
            }
            boolean enabled = BoundVizManager.getInstance().isEnabled();
            String status = enabled ? t("commandui.boundviz.status.on") : t("commandui.boundviz.status.off");
            int statusColor = enabled ? 0xFF55FF55 : 0xFFAAAAAA;
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(t("commandui.boundviz.status") + status), this.width / 2, 56, statusColor);
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("/areahint boundviz"), this.width / 2, 108, 0xFFFFFF55);
        }
    }
}
