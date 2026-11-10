package areahint.vizedit;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

// 属性表单由两个画布共用，输入值直接留在内存草稿中，不向正式域名发出局部保存。
final class VizEditPropertiesScreen extends Screen {
    private final VizEditSession session;
    private final VizEditSession.Draft before;
    private final List<Label> labels = new ArrayList<>();
    private TextFieldWidget parentField;
    private boolean checkpointed;

    VizEditPropertiesScreen(VizEditSession session) {
        super(Text.literal(t("vizedit.ui.properties")));
        this.session = session;
        this.before = session.draft.copy();
    }

    @Override protected void init() {
        labels.clear();
        int content = Math.min(600, width - 24), x = (width - content) / 2;
        int column = (content - 16) / 2, row = Math.min(42, Math.max(30, (height - 78) / 4));
        field("vizedit.ui.name", x, 40, column, session.draft.name, session.allows("rename"), value -> session.draft.name = value);
        field("vizedit.ui.surface", x + column + 16, 40, column, session.draft.surface, session.allows("rename"), value -> session.draft.surface = value);
        field("vizedit.ui.level", x, 40 + row, column, session.draft.level, session.allows("rename"), value -> session.draft.level = value);
        parentField = field("vizedit.ui.parent", x + column + 16, 40 + row, column - 23, session.draft.parent, session.allows("rename"), value -> session.draft.parent = value);
        ButtonWidget nextParent = addDrawableChild(ButtonWidget.builder(Text.literal("›"), ignored -> cycleParent())
            .dimensions(x + 2 * column - 4, 50 + row, 20, 18).build());
        nextParent.active = session.allows("rename");
        field("vizedit.ui.color", x, 40 + row * 2, column, session.draft.color, session.allows("recolor"), value -> session.draft.color = value);
        field("vizedit.ui.min", x + column + 16, 40 + row * 2, column, session.draft.min, session.allows("sethigh"), value -> session.draft.min = value);
        field("vizedit.ui.max", x, 40 + row * 3, column, session.draft.max, session.allows("sethigh"), value -> session.draft.max = value);
        addDrawableChild(ButtonWidget.builder(Text.literal(t("vizedit.ui.back")), ignored -> close())
            .dimensions(width / 2 - 70, height - 27, 140, 20).build());
    }

    private TextFieldWidget field(String key, int x, int y, int width, String value, boolean editable, Consumer<String> change) {
        labels.add(new Label(key, x, y, width));
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y + 11, width, 18, Text.literal(t(key)));
        field.setMaxLength(256); field.setText(value); field.setEditable(editable && !"delete".equals(session.operation));
        field.setChangedListener(change); addDrawableChild(field); return field;
    }

    private void cycleParent() {
        int level;
        try { level = Integer.parseInt(session.draft.level.trim()); } catch (NumberFormatException e) { return; }
        if (level <= 1) { parentField.setText(""); return; }
        List<String> candidates = new ArrayList<>();
        for (AreaData area : session.areas) {
            if (area.getLevel() == level - 1 && !Objects.equals(area.getName(), session.target)
                    && !isDescendant(area)) candidates.add(area.getName());
        }
        candidates.sort(String::compareTo);
        if (!candidates.isEmpty()) parentField.setText(candidates.get((candidates.indexOf(session.draft.parent) + 1) % candidates.size()));
    }

    private boolean isDescendant(AreaData area) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (AreaData current = area; current != null && visited.add(current.getName()); current = session.find(current.getBaseName()))
            if (Objects.equals(current.getName(), session.target)) return true;
        return false;
    }

    @Override public void close() {
        if (!checkpointed && !session.draft.same(before)) { session.history.push(before); checkpointed = true; }
        VizEditClientManager.show();
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0171B22);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 13, 0xFFFFFF);
        for (Label label : labels) context.drawTextWithShadow(textRenderer,
            Text.literal(textRenderer.trimToWidth(t(label.key), label.width)), label.x, label.y, 0xC5D3E2);
        super.render(context, mouseX, mouseY, delta);
        for (Label label : labels) if (mouseX >= label.x && mouseX <= label.x + label.width && mouseY >= label.y && mouseY < label.y + 11)
            context.drawTooltip(textRenderer, Text.literal(t(label.key)), mouseX, mouseY);
    }
    private static String t(String key) { return I18nManager.translate(key); }
    private record Label(String key, int x, int y, int width) { }
}
