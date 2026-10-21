package areahint.approval;

import areahint.commandui.CommandUiScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

// 历史内容按实际可用高度分页，长意见和多轮打回不会被截断。
public final class ApprovalHistoryScreen extends CommandUiScreen {
    private final List<OrderedText> lines = new ArrayList<>();
    private int rows;
    private int pages;
    private final boolean showChanges;
    private int contentPage;

    public ApprovalHistoryScreen(Screen parent) { this(parent, false); }

    public ApprovalHistoryScreen(Screen parent, boolean showChanges) {
        super(showChanges ? "approval.ui.changes" : "approval.ui.detail", parent);
        this.showChanges = showChanges;
    }

    @Override
    protected void init() {
        var session = ApprovalClientManager.getSession();
        if (session == null) return;
        int textWidth = Math.min(520, width - 36);
        lines.clear();
        if (showChanges) {
            var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            // 完整展示提交前后内容，描述、维度配置及非几何字段也能在审批前核对。
            for (ApprovalRequest.Change change : session.request.changes) {
                append(change.path == null ? "" : change.path, textWidth);
                append(t("approval.ui.before"), textWidth);
                append(gson.toJson(change.before), textWidth);
                append(t("approval.ui.after"), textWidth);
                append(gson.toJson(change.after), textWidth);
                lines.add(OrderedText.EMPTY);
            }
        } else {
            for (ApprovalRequest.Review review : session.request.reviews) {
                String heading = I18nManager.translate("approval.ui.review", review.reviewerName, ApprovalClientManager.statusName(review.result));
                append(heading, textWidth);
                String comment = review.comment == null || review.comment.isBlank() ? t("approval.ui.no_comment") : review.comment;
                append(comment, textWidth);
                lines.add(OrderedText.EMPTY);
            }
        }
        if (lines.isEmpty()) lines.add(Text.literal(t("approval.ui.no_reviews")).asOrderedText());
        rows = Math.max(1, (height - 82) / 12);
        pages = Math.max(1, (lines.size() + rows - 1) / rows);
        setPage(Math.min(getPage(), pages - 1));
        addDrawableChild(ButtonWidget.builder(Text.literal(t("approval.ui.previous")), button -> {
            setPage(Math.max(0, getPage() - 1));
        }).dimensions(width / 2 - 140, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(t("approval.ui.back")), button -> close())
            .dimensions(width / 2 - 45, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(t("approval.ui.next")), button -> {
            setPage(Math.min(pages - 1, getPage() + 1));
        }).dimensions(width / 2 + 50, height - FOOTER_Y_OFFSET, 90, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        var session = ApprovalClientManager.getSession();
        if (session == null) return;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(I18nManager.translate("approval.ui.page", getPage() + 1, pages)),
            width / 2, 34, BRIGHT_YELLOW);
        int first = getPage() * rows;
        int x = (width - Math.min(520, width - 36)) / 2;
        for (int i = first; i < Math.min(first + rows, lines.size()); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), x, 50 + (i - first) * 12, 0xFFFFFF);
        }
    }

    private void append(String value, int textWidth) {
        for (String line : value.split("\\R", -1)) lines.addAll(textRenderer.wrapLines(Text.literal(line.isEmpty() ? " " : line), textWidth));
    }

    private int getPage() {
        var session = ApprovalClientManager.getSession();
        return showChanges || session == null ? contentPage : session.historyPage;
    }

    private void setPage(int page) {
        var session = ApprovalClientManager.getSession();
        if (showChanges || session == null) contentPage = page; else session.historyPage = page;
    }
}
