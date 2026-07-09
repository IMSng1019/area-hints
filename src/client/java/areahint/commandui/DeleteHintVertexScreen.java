package areahint.commandui;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Set;

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
        this.list = new VertexListWidget(this.client, this.width, this.height, 58, this.height - 34);
        this.addDrawableChild(this.list);
        rebuildList();

        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.delete")), button -> {
            markFlowHandled();
            if (this.client != null) {
                this.client.setScreen(null);
            }
            this.manager.submit();
        }).dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .dimensions(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        AreaData area = this.manager.getSelectedArea();
        int contentWidth = Math.min(460, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        drawTrimmed(context, Text.literal(t("commandui.deletehint.vertex.prompt")), x, y, contentWidth, 0xFFFFFF);
        if (area != null) {
            drawTrimmed(context, Text.literal(I18nManager.translate("commandui.deletehint.vertex.count",
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

    private class VertexListWidget extends ElementListWidget<VertexListWidget.Entry> {
        VertexListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 28);
            this.setRenderBackground(false);
        }

        void addVertex(int index, AreaData.Vertex vertex) {
            this.addEntry(new Entry(index, vertex));
        }

        void clearAll() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, DeleteHintVertexScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private class Entry extends ElementListWidget.Entry<Entry> {
            private final int index;
            private final AreaData.Vertex vertex;

            private Entry(int index, AreaData.Vertex vertex) {
                this.index = index;
                this.vertex = vertex;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                Set<Integer> marked = DeleteHintVertexScreen.this.manager.getMarkedIndices();
                boolean selected = marked.contains(this.index);
                int titleColor = selected ? 0xFF5555 : hovered ? BRIGHT_YELLOW : 0xFFFFFF;
                String title = I18nManager.translate("commandui.deletehint.vertex.item",
                    this.index + 1, (int) this.vertex.getX(), (int) this.vertex.getZ());
                String detail = t(selected
                    ? "commandui.deletehint.vertex.marked"
                    : "commandui.deletehint.vertex.unmarked");
                DeleteHintVertexScreen.this.drawTrimmed(context, Text.literal(title), x + 4, y + 4, entryWidth - 8, titleColor);
                DeleteHintVertexScreen.this.drawTrimmed(context, Text.literal(detail), x + 4, y + 16, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    DeleteHintVertexScreen.this.manager.toggleVertex(this.index);
                    DeleteHintVertexScreen.this.rebuildList();
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
        }
    }
}
