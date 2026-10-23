package areahint.approval;

import areahint.commandui.CommandUiScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

// 每次只显示一个申请的操作，完整历史放在独立分页界面，窄窗口也保留回复输入框。
public final class ApprovalDetailScreen extends CommandUiScreen {
    private final List<ButtonWidget> mutationButtons = new ArrayList<>();
    private ButtonWidget previewButton;
    private ButtonWidget addButton;
    private ButtonWidget deleteButton;
    private TextFieldWidget comment;
    private int contentX;
    private int contentWidth;

    public ApprovalDetailScreen() {
        super(ApprovalClientManager.getSession() != null && ApprovalClientManager.getSession().editMode
            ? "approval.ui.edit" : "approval.ui.detail", null);
    }

    @Override
    protected void init() {
        var session = ApprovalClientManager.getSession();
        if (session == null) return;
        mutationButtons.clear();
        contentWidth = Math.min(520, width - 28);
        contentX = (width - contentWidth) / 2;
        int columns = (contentWidth - 8) / 3;
        int actionsY = height - 52;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
            if (!session.areas.isEmpty()) session.areaIndex = Math.floorMod(session.areaIndex - 1, session.areas.size());
        }).dimensions(contentX, 76, 20, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
            if (!session.areas.isEmpty()) session.areaIndex = (session.areaIndex + 1) % session.areas.size();
        }).dimensions(contentX + contentWidth - 20, 76, 20, BUTTON_HEIGHT).build());
        previewButton = button("approval.ui.preview", contentX, 100, columns, () -> ApprovalClientManager.preview(session.areaIndex));
        addButton = button("approval.ui.addhint", contentX + columns + 4, 100, columns,
            () -> ApprovalClientManager.editGeometry(true, session.areaIndex));
        deleteButton = button("approval.ui.deletehint", contentX + (columns + 4) * 2, 100, columns,
            () -> ApprovalClientManager.editGeometry(false, session.areaIndex));
        int halfWidth = (contentWidth - 4) / 2;
        button("approval.ui.history", contentX, 124, halfWidth, () -> {
            if (client != null) client.setScreen(new ApprovalHistoryScreen(this));
        }).setMessage(Text.literal(I18nManager.translate("approval.ui.history", session.request.reviews.size(), session.request.reviews.size())));
        button("approval.ui.changes", contentX + halfWidth + 4, 124, halfWidth, () -> {
            if (client != null) client.setScreen(new ApprovalHistoryScreen(this, true));
        });

        if (ApprovalClientManager.isOperator()) {
            comment = new TextFieldWidget(textRenderer, contentX, height - 80, contentWidth, BUTTON_HEIGHT, Text.empty());
            comment.setMaxLength(2048);
            comment.setText(session.comment);
            comment.setPlaceholder(Text.literal(t("approval.ui.comment_hint")));
            comment.setChangedListener(value -> session.comment = value);
            addDrawableChild(comment);
            mutationButtons.add(button("approval.ui.approve", contentX, actionsY, columns, () -> ApprovalClientManager.decide("approved")));
            mutationButtons.add(button("approval.ui.reject", contentX + columns + 4, actionsY, columns, () -> ApprovalClientManager.decide("rejected")));
            mutationButtons.add(button("approval.ui.return", contentX + (columns + 4) * 2, actionsY, columns, () -> ApprovalClientManager.decide("returned")));
        } else if ("returned".equals(session.request.status)) {
            if (session.editMode) {
                mutationButtons.add(button("approval.ui.resubmit", contentX, actionsY, contentWidth, ApprovalClientManager::resubmit));
            } else {
                mutationButtons.add(button("approval.ui.modify", contentX, actionsY, contentWidth, () -> {
                    session.editMode = true;
                    if (client != null) client.setScreen(new ApprovalDetailScreen());
                }));
            }
        }
        button("approval.ui.back", width / 2 - 94, height - FOOTER_Y_OFFSET, 90, ApprovalClientManager::returnToList);
        button("approval.ui.close", width / 2 + 4, height - FOOTER_Y_OFFSET, 90, () -> {
            ApprovalClientManager.endSession();
            if (client != null) client.setScreen(null);
        });
        tick();
    }

    private ButtonWidget button(String key, int x, int y, int width, Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(t(key)), ignored -> action.run())
            .dimensions(x, y, width, BUTTON_HEIGHT).build());
    }

    @Override
    public void tick() {
        var session = ApprovalClientManager.getSession();
        if (session == null) { if (client != null) client.setScreen(null); return; }
        boolean available = !ApprovalClientManager.isBusy() && !session.stale;
        boolean editable = available && ApprovalClientManager.isSessionOperationEnabled();
        for (ButtonWidget button : mutationButtons) button.active = editable;
        if (previewButton != null) previewButton.active = available && !session.areas.isEmpty();
        if (addButton != null) addButton.active = available && ApprovalClientManager.canEdit(session.areaIndex);
        if (deleteButton != null) deleteButton.active = available && ApprovalClientManager.canEdit(session.areaIndex);
        if (comment != null) comment.setEditable(editable);
    }

    @Override
    public void close() { ApprovalClientManager.returnToList(); }

    @Override
    protected void onDiscard() {
        // 绑定键只临时收起界面，会话保留以供下一次长按恢复；显式关闭按钮才丢弃草稿。
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 输入意见时绑定键也可能是普通字母，交给随后到达的字符事件，不能关闭审批页面。
        if (comment != null && comment.isFocused()
                && areahint.keyhandler.UnifiedKeyHandler.matchesRecordKey(keyCode, scanCode)) return true;
        if (comment != null && comment.isFocused() && comment.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        var session = ApprovalClientManager.getSession();
        if (session == null) return;
        var request = session.request;
        line(context, "approval.ui.applicant", 30, request.applicantName);
        line(context, "approval.ui.type", 42, ApprovalClientManager.operationName(request.operation));
        line(context, "approval.ui.result", 54, ApprovalClientManager.statusName(request.status));
        String state = ApprovalClientManager.isBusy() ? "approval.ui.busy" : session.stale ? "approval.ui.changed" : null;
        if (state == null) line(context, "approval.ui.dimension", 66, request.dimensionId);
        else drawTrimmed(context, Text.literal(t(state)), contentX, 66, contentWidth, 0xFFAA55);
        String areaLabel = session.areas.isEmpty() ? t("approval.ui.no_preview") : I18nManager.translate("approval.ui.area",
            session.areas.get(Math.min(session.areaIndex, session.areas.size() - 1)).getName(), session.areaIndex + 1, session.areas.size());
        drawTrimmed(context, Text.literal(areaLabel), contentX + 24, 82, contentWidth - 48, BRIGHT_YELLOW);
        if (ApprovalClientManager.isOperator()) {
            drawTrimmed(context, Text.literal(t("approval.ui.comment")), contentX, height - 92, contentWidth, 0xAAAAAA);
        } else if (!request.reviews.isEmpty()) {
            var review = request.reviews.get(request.reviews.size() - 1);
            String reviewText = I18nManager.translate("approval.ui.review", review.reviewerName, ApprovalClientManager.statusName(review.result));
            drawTrimmed(context, Text.literal(reviewText), contentX, 150, contentWidth, 0xAAAAAA);
            drawTrimmed(context, Text.literal(review.comment == null || review.comment.isBlank() ? t("approval.ui.no_comment") : review.comment),
                contentX, 162, contentWidth, 0xFFFFFF);
        }
        // 禁用的编辑按钮提供原因提示，回复详情要先点击修改才进入可编辑草稿。
        if (addButton != null && !addButton.active && addButton.isHovered()
                || deleteButton != null && !deleteButton.active && deleteButton.isHovered()) {
            drawWrappedTooltip(context, Text.literal(t("approval.ui.geometry_unavailable")), mouseX, mouseY);
        }
    }

    private void line(DrawContext context, String key, int y, Object value) {
        drawTrimmed(context, Text.literal(I18nManager.translate(key, value)), contentX, y, contentWidth, 0xFFFFFF);
    }
}
