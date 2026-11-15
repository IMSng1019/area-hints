package areahint.debug;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandUiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * debug 图形流程控制器，只负责把按钮选择转换为现有 /areahint debug 子命令。
 */
public final class DebugVisualController {
    private DebugVisualController() {
    }

    public static void openFromCommandUi(Screen parent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new DebugScreen(parent));
        }
    }

    private static final class DebugScreen extends CommandUiScreen {
        private DebugScreen(Screen parent) {
            super("commandui.debug.title", parent);
        }

        @Override
        protected void init() {
            int buttonWidth = Math.min(Math.max(180, longestActionWidth() + 28), Math.max(120, this.width - 40));
            int x = (this.width - buttonWidth) / 2;
            int y = 82;
            addActionButton("commandui.debug.toggle", "areahint debug", x, y, buttonWidth);
            addActionButton("commandui.debug.on", "areahint debug on", x, y + 24, buttonWidth);
            addActionButton("commandui.debug.off", "areahint debug off", x, y + 48, buttonWidth);
            addActionButton("commandui.debug.status", "areahint debug status", x, y + 72, buttonWidth);

            int footerY = this.height - FOOTER_Y_OFFSET;
            int footerWidth = 90;
            int footerX = (this.width - (footerWidth * 2 + 4)) / 2;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.back")), button -> close())
                .dimensions(footerX, footerY, footerWidth, BUTTON_HEIGHT).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
                .dimensions(footerX + footerWidth + 4, footerY, footerWidth, BUTTON_HEIGHT).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            drawTrimmed(context, Text.literal(t("commandui.debug.detail")), 20, 36, this.width - 40, 0xAAAAAA);
            boolean enabled = ClientDebugManager.isDebugEnabled();
            String status = enabled ? t("commandui.debug.on") : t("commandui.debug.off");
            int statusColor = enabled ? 0xFF55FF55 : 0xFFFF5555;
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(t("commandui.debug.status") + ": " + status), this.width / 2, 56, statusColor);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("/areahint debug"),
                this.width / 2, 184, 0xFFFFFF55);
        }

        private void addActionButton(String labelKey, String command, int x, int y, int width) {
            // 继续走原指令，权限、服务端状态和聊天反馈都由现有命令链处理。
            this.addDrawableChild(ButtonWidget.builder(Text.literal(t(labelKey)),
                button -> CommandUiActions.runCommandAndClose(this, command))
                .dimensions(x, y, width, BUTTON_HEIGHT).build());
        }

        private int longestActionWidth() {
            int width = 0;
            width = Math.max(width, this.textRenderer.getWidth(t("commandui.debug.toggle")));
            width = Math.max(width, this.textRenderer.getWidth(t("commandui.debug.on")));
            width = Math.max(width, this.textRenderer.getWidth(t("commandui.debug.off")));
            return Math.max(width, this.textRenderer.getWidth(t("commandui.debug.status")));
        }
    }
}
