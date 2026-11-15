package areahint.commandui;

import areahint.data.AreaData;
import areahint.gui.CompatEntryListWidget;
import areahint.i18n.I18nManager;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * deletehint 的一级顶点选择页，点击顶点切换删除标记。
 */
public class DeleteHintVertexScreen extends CommandWizardScreen {
    private final areahint.deletehint.DeleteHintManager manager;
    private VertexListWidget list;

    public DeleteHintVertexScreen(Screen parent, areahint.deletehint.DeleteHintManager manager) {
        super("commandui.deletehint.title", parent, manager::cancel);
        this.manager = manager;
    }

    @Override
    protected void init() {
        this.list = new VertexListWidget(this.minecraft, this.width, this.height, 58, this.height - 34);
        this.addRenderableWidget(this.list);
        rebuildList();

        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.delete")), button -> {
            // 未选择顶点时保留当前页面，避免临时审批流程留在活跃状态却失去恢复入口。
            if (this.manager.getMarkedIndices().isEmpty()) return;
            markFlowHandled();
            if (this.minecraft != null) {
                areahint.util.ClientScreenCompat.setScreen(this.minecraft, null);
            }
            this.manager.submit();
        }).bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        AreaData area = this.manager.getSelectedArea();
        int contentWidth = Math.min(460, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        drawTrimmed(context, Component.literal(t("commandui.deletehint.vertex.prompt")), x, y, contentWidth, 0xFFFFFF);
        if (area != null) {
            drawTrimmed(context, Component.literal(I18nManager.translate("commandui.deletehint.vertex.count",
                    this.manager.getMarkedIndices().size(), area.getVertices().size() - this.manager.getMarkedIndices().size())),
                x, y + 14, contentWidth, 0xAAAAAA);
        }
    }

    private void rebuildList() {
        if (this.list == null) {
            return;
        }
        this.list.clearAll();
        AreaData area = this.manager.getSelectedArea();
        if (area == null || area.getVertices() == null) {
            return;
        }
        for (int i = 0; i < area.getVertices().size(); i++) {
            this.list.addVertex(i, area.getVertices().get(i));
        }
    }

    private class VertexListWidget extends CompatEntryListWidget<VertexListWidget.Entry> {
        VertexListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 28);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addVertex(int index, AreaData.Vertex vertex) {
            this.addEntry(new areahint.commandui.DeleteHintVertexScreen.VertexListWidget.Entry(index, vertex));
        }

        void clearAll() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, DeleteHintVertexScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private class Entry extends ContainerObjectSelectionList.Entry<areahint.commandui.DeleteHintVertexScreen.VertexListWidget.Entry> {
            private final int index;
            private final AreaData.Vertex vertex;

            private Entry(int index, AreaData.Vertex vertex) {
                this.index = index;
                this.vertex = vertex;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                Set<Integer> marked = DeleteHintVertexScreen.this.manager.getMarkedIndices();
                boolean selected = marked.contains(this.index);
                int titleColor = selected ? 0xFF5555 : hovered ? BRIGHT_YELLOW : 0xFFFFFF;
                String title = I18nManager.translate("commandui.deletehint.vertex.item",
                    this.index + 1, (int) this.vertex.getX(), (int) this.vertex.getZ());
                String detail = t(selected
                    ? "commandui.deletehint.vertex.marked"
                    : "commandui.deletehint.vertex.unmarked");
                DeleteHintVertexScreen.this.drawTrimmed(context, Component.literal(title), x + 4, y + 4, entryWidth - 8, titleColor);
                DeleteHintVertexScreen.this.drawTrimmed(context, Component.literal(detail), x + 4, y + 16, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                if (button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
                    DeleteHintVertexScreen.this.manager.toggleVertex(this.index);
                    DeleteHintVertexScreen.this.rebuildList();
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
        }
    }
}
