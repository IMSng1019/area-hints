package areahint.vizedit;

import areahint.i18n.I18nManager;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

// 保存前确认完整字段和后代名单；离开草稿时的三个选项不会提前清除编辑状态。
final class VizEditDialog extends Screen {
    private final VizEditSession session;
    private final boolean unsaved;
    private final JsonObject fields;
    private final boolean draftOnly;
    private final Runnable continuation;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private int offset;

    private VizEditDialog(VizEditSession session, boolean unsaved, JsonObject fields, boolean draftOnly, Runnable continuation) {
        super(Component.literal(t(unsaved ? "vizedit.ui.unsaved" : "vizedit.ui.review")));
        this.session = session; this.unsaved = unsaved; this.fields = fields;
        this.draftOnly = draftOnly; this.continuation = continuation;
    }
    static VizEditDialog unsaved(VizEditSession session, Runnable continuation) {
        return new VizEditDialog(session, true, null, false, continuation);
    }
    static VizEditDialog review(VizEditSession session, JsonObject fields, boolean draftOnly, Runnable continuation) {
        return new VizEditDialog(session, false, fields, draftOnly, continuation);
    }
    @Override protected void init() {
        lines.clear();
        int contentWidth = Math.min(620, width - 32), left = (width - contentWidth) / 2;
        if (unsaved) {
            append(session.draft.name, contentWidth);
            int w = (contentWidth - 12) / 3;
            Button save = button("vizedit.ui.save", left, height - 32, w,
                () -> VizEditClientManager.review(false, continuation));
            save.active = !session.invalid;
            button("vizedit.ui.discard", left + w + 6, height - 32, w, () -> {
                if (session.originalDraft != null) session.draft = session.originalDraft.copy();
                if (session.approvalId == null) session.operation = "update";
                session.history.clear(); continuation.run();
            });
            button("vizedit.ui.continue", left + 2 * (w + 6), height - 32, w, VizEditClientManager::show);
        } else {
            append(t("vizedit.ui.operation." + session.operation) + ": " + (session.target == null ? session.draft.name : session.target), contentWidth);
            if (!"delete".equals(session.operation)) append(t("vizedit.ui.save_normalized"), contentWidth);
            List<String> affected = session.affectedDescendants().stream().map(area -> area.getName()).toList();
            append(I18nManager.translate("vizedit.ui.affected", affected.size()), contentWidth);
            for (String name : affected) append("• " + name, contentWidth);
            var gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
            append(t("vizedit.ui.before"), contentWidth);
            append(session.originalArea == null ? "∅" : gson.toJson(session.originalArea), contentWidth);
            append(t("vizedit.ui.after"), contentWidth);
            append("delete".equals(session.operation) ? "∅" : gson.toJson(fields), contentWidth);
            int w = (contentWidth - 8) / 2;
            button("vizedit.ui.confirm", left, height - 32, w, () -> VizEditClientManager.submit(fields, draftOnly, continuation));
            button("vizedit.ui.continue", left + w + 8, height - 32, w, this::onClose);
        }
    }
    private Button button(String key, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(t(key)), ignored -> action.run()).bounds(x, y, w, 20).build());
    }
    private void append(String text, int width) {
        for (String line : text.split("\\R", -1)) lines.addAll(font.split(Component.literal(line.isEmpty() ? " " : line), width));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFA151B24);
        context.centeredText(font, title, width / 2, 13, 0xFFFFFFFF);
        int visible = Math.max(1, (height - 82) / 11);
        offset = Math.max(0, Math.min(offset, Math.max(0, lines.size() - visible)));
        int x = (width - Math.min(620, width - 32)) / 2;
        for (int i = offset; i < Math.min(offset + visible, lines.size()); i++) context.text(font, lines.get(i), x, 39 + (i - offset) * 11, 0xFFCFD8E4);
        if (lines.size() > visible) context.text(font, Component.literal("↕ " + (offset + 1) + "/" + lines.size()), width - 76, 28, 0xFF91ABC5);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
    @Override public void onClose() {
        if (!unsaved && "delete".equals(session.operation) && session.approvalId == null) session.operation = "update";
        VizEditClientManager.show();
    }
    @Override public boolean isPauseScreen() { return false; }
    private boolean scroll(double amount) { offset = Math.max(0, offset - (int) Math.signum(amount) * 4); return true; }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { return scroll(vertical); }
    /*@Override public boolean mouseScrolled(double x, double y, double vertical) { return scroll(vertical); }*/
    private static String t(String key) { return I18nManager.translate(key); }
}
