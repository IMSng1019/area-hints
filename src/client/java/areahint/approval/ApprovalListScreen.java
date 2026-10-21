package areahint.approval;

import areahint.commandui.CommandUiScreen;
import areahint.gui.CompatEntryListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

// 申请与回复共用滚动列表，每项进入独立详情，避免审批按钮挤在长列表中。
public final class ApprovalListScreen extends CommandUiScreen {
    private RequestList list;
    private int displayedVersion = -1;

    public ApprovalListScreen(Screen parent) {
        super(ApprovalClientManager.isOperator() ? "approval.ui.requests" : "approval.ui.replies", parent);
    }

    @Override
    protected void init() {
        list = new RequestList(client, width, 50, height - 34);
        addDrawableChild(list);
        rebuild();
        int x = width / 2 - 94;
        addDrawableChild(ButtonWidget.builder(Text.literal(t("approval.ui.refresh")), button -> ApprovalClientManager.refresh())
            .dimensions(x, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(t("approval.ui.back")), button -> close())
            .dimensions(x + 98, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
    }

    @Override
    public void tick() {
        if (displayedVersion != ApprovalClientManager.getVersion()) rebuild();
    }

    private void rebuild() {
        if (list == null) return;
        displayedVersion = ApprovalClientManager.getVersion();
        list.replace(ApprovalClientManager.getVisibleRequests());
    }

    @Override
    protected void onDiscard() { ApprovalClientManager.endSession(); }

    @Override
    public void close() {
        ApprovalClientManager.endSession();
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(t(ApprovalClientManager.getVisibleRequests().isEmpty()
            ? "approval.ui.empty" : "approval.ui.select")), width / 2, 34, 0xAAAAAA);
    }

    private final class RequestList extends CompatEntryListWidget<RequestList.Entry> {
        RequestList(MinecraftClient client, int width, int top, int bottom) {
            super(client, width, bottom - top, top, 36);
            //? if <=1.20.4 {
            setRenderBackground(false);
            //?}
        }

        void replace(List<ApprovalRequest> requests) {
            clearEntries();
            for (ApprovalRequest request : requests) addEntry(new Entry(request));
        }

        @Override
        public int getRowWidth() { return Math.min(520, ApprovalListScreen.this.width - 36); }

        @Override
        //? if <=1.20.4 {
        protected int getScrollbarPositionX() {
        //?} else {
        /*protected int getScrollbarX() {*/
        //?}
            return width - 6;
        }

        private final class Entry extends ElementListWidget.Entry<Entry> {
            private final ApprovalRequest request;
            Entry(ApprovalRequest request) { this.request = request; }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String name = request.previewAreas == null || request.previewAreas.isEmpty()
                    ? request.dimensionId : request.previewAreas.get(0).getName();
                String title = request.applicantName + " · " + ApprovalClientManager.operationName(request.operation);
                String detail = name + " · " + ApprovalClientManager.statusName(request.status);
                drawTrimmed(context, Text.literal(title), x + 4, y + 5, entryWidth - 8, hovered ? BRIGHT_YELLOW : 0xFFFFFF);
                drawTrimmed(context, Text.literal(detail), x + 4, y + 19, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) return false;
                ApprovalClientManager.openRequest(request.id, ApprovalListScreen.this);
                return true;
            }

            @Override
            public List<? extends Element> children() { return List.of(); }
            @Override
            public List<? extends Selectable> selectableChildren() { return List.of(); }
        }
    }
}
