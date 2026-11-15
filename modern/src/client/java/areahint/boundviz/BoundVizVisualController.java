package areahint.boundviz;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * boundviz 图形流程控制器，只把按钮操作转换为现有 /areahint boundviz 指令。
 */
public final class BoundVizVisualController {
    private BoundVizVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, new BoundVizScreen(parent));
        }
    }

    private static final class BoundVizScreen extends CommandUiScreen {
        private Button toggleButton;

        private BoundVizScreen(Screen parent) {
            super("commandui.boundviz.title", parent);
        }

        @Override
        protected void init() {
            int toggleWidth = Math.min(Math.max(160, this.font.width(t("commandui.boundviz.toggle")) + 28),
                Math.max(120, this.width - 40));
            this.toggleButton = Button.builder(Component.literal(t("commandui.boundviz.toggle")), button -> {
                // 继续走原指令，权限、网络转发和实际开关逻辑都由现有命令链处理。
                CommandUiActions.runCommand("areahint boundviz");
                button.setMessage(Component.literal(t("commandui.boundviz.toggle")));
            }).bounds((this.width - toggleWidth) / 2, 82, toggleWidth, BUTTON_HEIGHT).build();
            this.addRenderableWidget(this.toggleButton);

            int y = this.height - FOOTER_Y_OFFSET;
            int buttonWidth = 90;
            int x = (this.width - (buttonWidth * 2 + 4)) / 2;
            this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.back")), button -> onClose())
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
            this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
                .bounds(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            if (this.toggleButton != null) {
                this.toggleButton.setMessage(Component.literal(t("commandui.boundviz.toggle")));
            }
            boolean enabled = BoundVizManager.getInstance().isEnabled();
            String status = enabled ? t("commandui.boundviz.status.on") : t("commandui.boundviz.status.off");
            int statusColor = enabled ? 0xFF55FF55 : 0xFFAAAAAA;
            context.centeredText(this.font,
                Component.literal(t("commandui.boundviz.status") + status), this.width / 2, 56, statusColor);
            context.centeredText(this.font,
                Component.literal("/areahint boundviz"), this.width / 2, 108, 0xFFFFFF55);
        }
    }
}
