package areahint.vizedit;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// 属性表单由两个画布共用，输入值直接留在内存草稿中，不向正式域名发出局部保存。
final class VizEditPropertiesScreen extends Screen {
    private final VizEditSession session;
    private final VizEditSession.Draft before;
    private final List<Label> labels = new ArrayList<>();
    private EditBox parentField;
    private boolean checkpointed;

    VizEditPropertiesScreen(VizEditSession session) {
        super(Component.literal(t("vizedit.ui.properties")));
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
        Button nextParent = addRenderableWidget(Button.builder(Component.literal("›"), ignored -> cycleParent())
            .bounds(x + 2 * column - 4, 50 + row, 20, 18).build());
        nextParent.active = session.allows("rename");
        field("vizedit.ui.color", x, 40 + row * 2, column, session.draft.color, session.allows("recolor"), value -> session.draft.color = value);
        field("vizedit.ui.min", x + column + 16, 40 + row * 2, column, session.draft.min, session.allows("sethigh"), value -> session.draft.min = value);
        field("vizedit.ui.max", x, 40 + row * 3, column, session.draft.max, session.allows("sethigh"), value -> session.draft.max = value);
        addRenderableWidget(Button.builder(Component.literal(t("vizedit.ui.back")), ignored -> onClose())
            .bounds(width / 2 - 70, height - 27, 140, 20).build());
    }

    private EditBox field(String key, int x, int y, int width, String value, boolean editable, Consumer<String> change) {
        labels.add(new Label(key, x, y, width));
        EditBox field = new EditBox(font, x, y + 11, width, 18, Component.literal(t(key)));
        field.setMaxLength(256); field.setValue(value); field.setEditable(editable && !"delete".equals(session.operation));
        field.setResponder(change); addRenderableWidget(field); return field;
    }

    private void cycleParent() {
        int level;
        try { level = Integer.parseInt(session.draft.level.trim()); } catch (NumberFormatException e) { return; }
        if (level <= 1) { parentField.setValue(""); return; }
        List<String> candidates = new ArrayList<>();
        for (AreaData area : session.areas) {
            if (area.getLevel() == level - 1 && !Objects.equals(area.getName(), session.target)
                    && !isDescendant(area)) candidates.add(area.getName());
        }
        candidates.sort(String::compareTo);
        if (!candidates.isEmpty()) parentField.setValue(candidates.get((candidates.indexOf(session.draft.parent) + 1) % candidates.size()));
    }

    private boolean isDescendant(AreaData area) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (AreaData current = area; current != null && visited.add(current.getName()); current = session.find(current.getBaseName()))
            if (Objects.equals(current.getName(), session.target)) return true;
        return false;
    }

    @Override public void onClose() {
        if (!checkpointed && !session.draft.same(before)) { session.history.push(before); checkpointed = true; }
        VizEditClientManager.show();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0171B22);
        context.centeredText(font, title, width / 2, 13, 0xFFFFFFFF);
        for (Label label : labels) context.text(font,
            Component.literal(font.plainSubstrByWidth(t(label.key), label.width)), label.x, label.y, 0xFFC5D3E2);
        super.extractRenderState(context, mouseX, mouseY, delta);
        for (Label label : labels) if (mouseX >= label.x && mouseX <= label.x + label.width && mouseY >= label.y && mouseY < label.y + 11)
            context.setTooltipForNextFrame(font, Component.literal(t(label.key)), mouseX, mouseY);
    }
    private static String t(String key) { return I18nManager.translate(key); }
    private record Label(String key, int x, int y, int width) { }
}
