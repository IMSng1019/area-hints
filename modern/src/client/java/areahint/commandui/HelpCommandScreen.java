package areahint.commandui;

import areahint.gui.CompatEntryListWidget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

/**
 * 图形化 help 面板，与主命令列表复用同一份指令元数据。
 */
public class HelpCommandScreen extends CommandUiScreen {
    private HelpListWidget list;
    private Component hoveredDescription;
    private boolean urbanVisible;

    public HelpCommandScreen(net.minecraft.client.gui.screens.Screen parent) {
        super("commandui.help.title", parent);
    }

    @Override
    protected void init() {
        this.list = new HelpListWidget(this.minecraft, this.width, this.height, 32, this.height - 32);
        this.addRenderableWidget(this.list);
        rebuildCommandList();

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int x = (this.width - (buttonWidth * 2 + 4)) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.back")), button -> onClose())
            .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
            .bounds(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void tick() {
        super.tick();
        // 与命令列表同步响应服务端命令树变化，避免帮助页继续保留已经隐藏的城市化入口。
        if (this.list != null && this.urbanVisible != areahint.urban.ApplyUrbanAreaVisualController.isAvailable()) {
            double scroll = this.list.getScrollOffset();
            rebuildCommandList();
            this.list.setScrollOffset(scroll);
        }
    }

    private void rebuildCommandList() {
        if (this.list == null) return;
        this.urbanVisible = areahint.urban.ApplyUrbanAreaVisualController.isAvailable();
        this.list.clearCommandEntries();
        for (CommandVisualHandler handler : CommandVisualRegistry.getHandlers()) {
            this.list.addCommand(handler);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.hoveredDescription = null;
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (this.hoveredDescription != null) {
            drawWrappedTooltip(context, this.hoveredDescription, mouseX, mouseY);
        }
    }

    private class HelpListWidget extends CompatEntryListWidget<HelpListWidget.Entry> {
        HelpListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 34);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addCommand(CommandVisualHandler handler) {
            this.addEntry(new areahint.commandui.HelpCommandScreen.HelpListWidget.Entry(handler));
        }

        void clearCommandEntries() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(560, HelpCommandScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private class Entry extends ContainerObjectSelectionList.Entry<areahint.commandui.HelpCommandScreen.HelpListWidget.Entry> {
            private final CommandVisualHandler handler;

            private Entry(CommandVisualHandler handler) {
                this.handler = handler;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                HelpCommandScreen.this.drawTrimmed(context, handler.displayName(), x + 4, y + 5, entryWidth - 8, 0xFFFFFF);
                HelpCommandScreen.this.drawTrimmed(context, Component.literal(displayCommand()), x + 4, y + 18,
                    entryWidth - 8, hovered ? BRIGHT_YELLOW : 0xAAAAAA);
                if (hovered) {
                    HelpCommandScreen.this.hoveredDescription = handler.description();
                }
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                if (button == 0) {
                    handler.open(HelpCommandScreen.this);
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }

            private String displayCommand() {
                String command = this.handler.defaultCommand();
                return command == null || command.isBlank() ? t("commandui.command.settings.name") : "/" + command;
            }
        }
    }
}
