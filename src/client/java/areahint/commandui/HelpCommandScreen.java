package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 图形化 help 面板，与主命令列表复用同一份指令元数据。
 */
public class HelpCommandScreen extends CommandUiScreen {
    private HelpListWidget list;
    private Text hoveredDescription;

    public HelpCommandScreen(net.minecraft.client.gui.screen.Screen parent) {
        super("commandui.help.title", parent);
    }

    @Override
    protected void init() {
        this.list = new HelpListWidget(this.client, this.width, this.height, 32, this.height - 32);
        this.addDrawableChild(this.list);
        for (CommandVisualHandler handler : CommandVisualRegistry.getHandlers()) {
            this.list.addCommand(handler);
        }

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
        this.hoveredDescription = null;
        super.render(context, mouseX, mouseY, delta);
        if (this.hoveredDescription != null) {
            drawWrappedTooltip(context, this.hoveredDescription, mouseX, mouseY);
        }
    }

    private class HelpListWidget extends ElementListWidget<HelpListWidget.Entry> {
        HelpListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 34);
            this.setRenderBackground(false);
        }

        void addCommand(CommandVisualHandler handler) {
            this.addEntry(new Entry(handler));
        }

        @Override
        public int getRowWidth() {
            return Math.min(560, HelpCommandScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private class Entry extends ElementListWidget.Entry<Entry> {
            private final CommandVisualHandler handler;

            private Entry(CommandVisualHandler handler) {
                this.handler = handler;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                HelpCommandScreen.this.drawTrimmed(context, handler.displayName(), x + 4, y + 5, entryWidth - 8, 0xFFFFFF);
                HelpCommandScreen.this.drawTrimmed(context, Text.literal(displayCommand()), x + 4, y + 18,
                    entryWidth - 8, hovered ? BRIGHT_YELLOW : 0xAAAAAA);
                if (hovered) {
                    HelpCommandScreen.this.hoveredDescription = handler.description();
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    handler.open(HelpCommandScreen.this);
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends Element> children() {
                return List.of();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of();
            }

            private String displayCommand() {
                String command = this.handler.defaultCommand();
                return command == null || command.isBlank() ? t("commandui.command.settings.name") : "/" + command;
            }
        }
    }
}
