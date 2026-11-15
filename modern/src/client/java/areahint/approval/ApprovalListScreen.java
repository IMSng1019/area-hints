package areahint.approval;

import areahint.commandui.CommandUiScreen;
import areahint.gui.CompatEntryListWidget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// 申请与回复共用滚动列表，每项进入独立详情，避免审批按钮挤在长列表中。
public final class ApprovalListScreen extends CommandUiScreen {
    private RequestList list;
    private int displayedVersion = -1;

    public ApprovalListScreen(Screen parent) {
        super(ApprovalClientManager.isOperator() ? "approval.ui.requests" : "approval.ui.replies", parent);
    }

    @Override
    protected void init() {
        list = new RequestList(minecraft, width, 50, height - 34);
        addRenderableWidget(list);
        rebuild();
        int x = width / 2 - 94;
        addRenderableWidget(Button.builder(Component.literal(t("approval.ui.refresh")), button -> ApprovalClientManager.refresh())
            .bounds(x, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal(t("approval.ui.back")), button -> onClose())
            .bounds(x + 98, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
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
    public void onClose() {
        ApprovalClientManager.endSession();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, Component.literal(t(ApprovalClientManager.getVisibleRequests().isEmpty()
            ? "approval.ui.empty" : "approval.ui.select")), width / 2, 34, 0xFFAAAAAA);
    }

    private final class RequestList extends CompatEntryListWidget<RequestList.Entry> {
        RequestList(Minecraft client, int width, int top, int bottom) {
            super(client, width, bottom - top, top, 36);
            /*setRenderBackground(false);
            */
        }

        void replace(List<ApprovalRequest> requests) {
            clearEntries();
            for (ApprovalRequest request : requests) addEntry(new areahint.approval.ApprovalListScreen.RequestList.Entry(request));
        }

        @Override
        public int getRowWidth() { return Math.min(520, ApprovalListScreen.this.width - 36); }

        @Override
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return width - 6;
        }

        private final class Entry extends ContainerObjectSelectionList.Entry<areahint.approval.ApprovalListScreen.RequestList.Entry> {
            private final ApprovalRequest request;
            Entry(ApprovalRequest request) { this.request = request; }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                String name = request.previewAreas == null || request.previewAreas.isEmpty()
                    ? request.dimensionId : request.previewAreas.get(0).getName();
                String title = request.applicantName + " · " + ApprovalClientManager.operationName(request.operation);
                String detail = name + " · " + ApprovalClientManager.statusName(request.status);
                drawTrimmed(context, Component.literal(title), x + 4, y + 5, entryWidth - 8, hovered ? BRIGHT_YELLOW : 0xFFFFFF);
                drawTrimmed(context, Component.literal(detail), x + 4, y + 19, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                if (button != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
                ApprovalClientManager.openRequest(request.id, ApprovalListScreen.this);
                return true;
            }

            @Override
            public List<? extends GuiEventListener> children() { return List.of(); }
            @Override
            public List<? extends NarratableEntry> narratables() { return List.of(); }
        }
    }
}
