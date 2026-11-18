package areahint.vizedit;

import areahint.commandui.CommandUiData;
import areahint.commandui.CommandWizardScreen;
import areahint.commandui.WizardLongTextInputScreen;
import areahint.commandui.WizardOptionScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.commandui.WizardTextInputScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

// 属性入口复用 easyadd 的分步输入、父域名列表和颜色预设，所有确认只修改本次编辑草稿。
final class VizEditPropertiesScreen extends Screen {
    private final VizEditSession session;
    private final VizEditSession.Draft before;
    private final List<Summary> summaries = new ArrayList<>();
    private boolean checkpointed;

    VizEditPropertiesScreen(VizEditSession session) {
        super(Text.literal(t("vizedit.ui.properties")));
        this.session = session;
        this.before = session.draft.copy();
    }

    @Override protected void init() {
        summaries.clear();
        int content = Math.min(600, width - 24), x = (width - content) / 2;
        int column = (content - 16) / 2, row = Math.max(36, Math.min(48, (height - 88) / 3));
        category("vizedit.properties.names", session.draft.name + " / " + display(session.draft.surface), x, 52, column,
            editable("rename"), () -> showNames(null, session.draft.name, session.draft.surface));
        category("vizedit.properties.hierarchy", session.draft.level + " / " + display(session.draft.parent), x + column + 16, 52, column,
            session.canEditHierarchy(), this::showHierarchy);
        category("vizedit.ui.color", session.draft.color, x, 52 + row, column,
            editable("recolor"), () -> showColor(false));
        category("vizedit.properties.altitude", display(session.draft.min) + " ~ " + display(session.draft.max), x + column + 16, 52 + row, column,
            editable("sethigh"), this::showAltitude);
        category("vizedit.properties.description", display(session.draft.description), x, 52 + row * 2, column,
            canDescriptionText() || editable("deletedescription"), this::showDescription);
        category("vizedit.properties.subtitle", display(session.draft.subtitle) + " / " + session.draft.subtitleColor, x + column + 16, 52 + row * 2, column,
            canSubtitleText() || editable("deletesubtitle") || editable("replacesubtitlecolor"), this::showSubtitle);
        addDrawableChild(ButtonWidget.builder(Text.literal(t("vizedit.ui.back")), ignored -> close())
            .dimensions(width / 2 - 70, height - 27, 140, 20).build());
    }

    private void category(String key, String value, int x, int y, int width, boolean enabled, Runnable action) {
        ButtonWidget button = addDrawableChild(ButtonWidget.builder(Text.literal(t(key)), ignored -> action.run())
            .dimensions(x, y, width, 20).build());
        button.active = enabled;
        summaries.add(new Summary(value.replace("\n", " / "), x, y + 24, width));
    }

    private void showNames(String error, String name, String surface) {
        input("vizedit.properties.names", List.of(
            field("vizedit.ui.name", "commandui.easyadd.name.placeholder", name, 256),
            field("vizedit.ui.surface", "commandui.easyadd.surface.placeholder", surface, 256)), error, values -> {
                String nextName = values.get(0).trim(), nextSurface = values.get(1).trim();
                if (!validName(nextName, false) || !validName(nextSurface, true)) {
                    showNames("vizedit.error.invalid", values.get(0), values.get(1)); return;
                }
                if (session.areas.stream().anyMatch(area -> nextName.equals(area.getName()) && !nextName.equals(session.target))) {
                    showNames("vizedit.error.duplicate", values.get(0), values.get(1)); return;
                }
                apply("rename", () -> {
                    session.draft.name = nextName; session.draft.surface = nextSurface;
                    session.refreshDescriptionTarget();
                });
            });
    }

    private void showHierarchy() {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        for (int level = 1; level <= 3; level++) {
            int selected = level;
            options.add(option("commandui.easyadd.level." + level, () -> showParents(selected)));
        }
        options.add(option("vizedit.properties.custom_level", () -> showCustomLevel(null, session.draft.level)));
        options("vizedit.properties.hierarchy", "commandui.easyadd.level.prompt", options);
    }

    private void showCustomLevel(String error, String value) {
        input("vizedit.properties.custom_level", List.of(field("vizedit.ui.level", "", value, 10)), error, values -> {
            try {
                int level = Integer.parseInt(values.get(0).trim());
                if (level < 1) throw new NumberFormatException();
                showParents(level);
            } catch (NumberFormatException exception) { showCustomLevel("vizedit.error.level", values.get(0)); }
        });
    }

    private void showParents(int level) {
        List<WizardSelectionListScreen.SelectionItem<String>> candidates = new ArrayList<>();
        if (level == 1) {
            candidates.add(new WizardSelectionListScreen.SelectionItem<>("", t("description.ui.none"), t("commandui.easyadd.level.1.detail")));
            candidates.add(new WizardSelectionListScreen.SelectionItem<>(session.dimensionalName, session.dimensionalName, session.dimension));
        } else {
            session.areas.stream().filter(area -> area.getLevel() == level - 1 && !isDescendant(area))
                .sorted(Comparator.comparing(AreaData::getName)).forEach(area -> candidates.add(
                    new WizardSelectionListScreen.SelectionItem<>(area.getName(), area.getName(),
                        I18nManager.translate("commandui.easyadd.base.detail", area.getName(), area.getLevel(), area.getColor()))));
        }
        // 等级与上级共同确认，取消选择时不留下失配的半成品层级。
        nested(new WizardSelectionListScreen<>(this, "vizedit.properties.hierarchy",
            candidates.isEmpty() ? "vizedit.error.parent" : "commandui.easyadd.base.prompt", candidates,
            parent -> {
                if (session.setHierarchy(level, parent)) showHub(); else deny();
            }, null));
    }

    private boolean isDescendant(AreaData area) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (AreaData current = area; current != null && visited.add(current.getName()); current = session.find(current.getBaseName()))
            if (Objects.equals(current.getName(), session.target)) return true;
        return false;
    }

    private void showAltitude() {
        options("vizedit.properties.altitude", "commandui.easyadd.altitude.prompt", List.of(
            option("commandui.easyadd.altitude.custom", () -> showCustomAltitude(null, session.draft.min, session.draft.max)),
            option("commandui.easyadd.altitude.unlimited", () -> apply("sethigh", () -> { session.draft.min = ""; session.draft.max = ""; }))));
    }

    private void showCustomAltitude(String error, String min, String max) {
        input("vizedit.properties.altitude", List.of(
            field("vizedit.ui.min", "commandui.easyadd.altitude.min.placeholder", min, 32),
            field("vizedit.ui.max", "commandui.easyadd.altitude.max.placeholder", max, 32)), error, values -> {
                try {
                    Double low = height(values.get(0)), high = height(values.get(1));
                    if (low != null && high != null && low > high) throw new NumberFormatException();
                    apply("sethigh", () -> { session.draft.min = values.get(0).trim(); session.draft.max = values.get(1).trim(); });
                } catch (NumberFormatException exception) { showCustomAltitude("vizedit.error.altitude", values.get(0), values.get(1)); }
            });
    }

    private void showColor(boolean subtitle) {
        options(subtitle ? "vizedit.properties.subtitle_color" : "vizedit.ui.color", "commandui.easyadd.color.prompt",
            CommandUiData.colorOptions(color -> chooseColor(subtitle, color), () -> showCustomColor(subtitle, null,
                subtitle ? session.draft.subtitleColor : session.draft.color)));
    }

    private void showCustomColor(boolean subtitle, String error, String value) {
        input(subtitle ? "vizedit.properties.subtitle_color" : "vizedit.ui.color", List.of(
            field("commandui.easyadd.color.custom.label", "commandui.easyadd.color.custom.placeholder", value, 32)), error, values -> {
                String color = values.get(0).trim();
                if (!ColorUtil.isValidColor(color)) { showCustomColor(subtitle, "vizedit.error.color", values.get(0)); return; }
                chooseColor(subtitle, color);
            });
    }

    private void chooseColor(boolean subtitle, String color) {
        if (subtitle) {
            if (!canSubtitleColor()) { deny(); return; }
            session.draft.subtitleColor = color;
            showHub();
        } else apply("recolor", () -> session.draft.color = color);
    }

    private void showDescription() {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        if (canDescriptionText()) options.add(option("vizedit.properties.edit_text", () -> showDescriptionText(null, session.draft.description)));
        if (editable("deletedescription") || originalDescription().isBlank()) options.add(option("vizedit.properties.clear_text", () -> {
            if (!(editable("deletedescription") || originalDescription().isBlank() && canDescriptionText())) { deny(); return; }
            session.setDescription(""); showHub();
        }));
        options("vizedit.properties.description", "vizedit.properties.description_hint", options);
    }

    private void showDescriptionText(String error, String value) {
        nested(new WizardLongTextInputScreen(this, "vizedit.properties.description", "vizedit.properties.description_hint",
            "vizedit.properties.detail", value, error, text -> {
                String description = text.trim();
                if (description.isEmpty() ? !(editable("deletedescription") || originalDescription().isBlank() && canDescriptionText()) : !canDescriptionText()) {
                    showDescriptionText("vizedit.client.denied", text); return;
                }
                session.setDescription(description); showHub();
            }, null, 32767));
    }

    private void showSubtitle() {
        List<WizardOptionScreen.OptionSpec> options = new ArrayList<>();
        if (canSubtitleText()) options.add(option("vizedit.properties.edit_text", () -> showSubtitleText(null, session.draft.subtitle)));
        if (!session.draft.subtitle.isBlank() && canSubtitleColor()) options.add(option("vizedit.properties.subtitle_color", () -> showColor(true)));
        if (editable("deletesubtitle") || originalSubtitle().isBlank()) options.add(option("vizedit.properties.clear_text", () -> {
            if (!(editable("deletesubtitle") || originalSubtitle().isBlank() && canSubtitleText())) { deny(); return; }
            session.draft.subtitle = ""; session.draft.subtitleColor = "#FFFFFF"; showHub();
        }));
        options("vizedit.properties.subtitle", "vizedit.properties.subtitle_hint", options);
    }

    private void showSubtitleText(String error, String value) {
        nested(new WizardLongTextInputScreen(this, "vizedit.properties.subtitle", "vizedit.properties.subtitle_hint",
            "vizedit.properties.detail", value, error, text -> {
                String subtitle = text.replace("/n", "\n").replace("\\n", "\n").trim();
                if (subtitle.isEmpty() ? !(editable("deletesubtitle") || originalSubtitle().isBlank() && canSubtitleText()) : !canSubtitleText()) {
                    showSubtitleText("vizedit.client.denied", text); return;
                }
                session.draft.subtitle = subtitle;
                if (subtitle.isEmpty()) session.draft.subtitleColor = "#FFFFFF";
                showHub();
            }, null, 32767));
    }

    private boolean canDescriptionText() { return editable(session.descriptionAction()); }
    private boolean canSubtitleText() { return editable("addsubtitle") || editable("replacesubtitle"); }
    private boolean canSubtitleColor() {
        return editable("replacesubtitlecolor") || originalSubtitle().isBlank() && canSubtitleText()
            || !Objects.equals(session.draft.subtitle, originalSubtitle()) && canSubtitleText();
    }
    private String originalDescription() { return session.storedDescription(); }
    private String originalSubtitle() {
        return session.originalArea == null || session.originalArea.getSubtitle() == null ? "" : session.originalArea.getSubtitle();
    }
    private boolean editable(String action) { return !"delete".equals(session.operation) && session.allows(action); }

    private void apply(String action, Runnable change) {
        if (!editable(action)) { deny(); return; }
        change.run(); showHub();
    }
    private void deny() { VizEditClientManager.report("vizedit.client.denied"); showHub(); }
    private void showHub() { MinecraftClient.getInstance().setScreen(this); }
    private void nested(CommandWizardScreen screen) {
        screen.setReturnToParentOnCancel(true);
        MinecraftClient.getInstance().setScreen(screen);
    }
    private void options(String title, String prompt, List<WizardOptionScreen.OptionSpec> options) {
        nested(new WizardOptionScreen(this, title, prompt, "vizedit.properties.detail", options, null));
    }
    private void input(String title, List<WizardTextInputScreen.FieldSpec> fields, String error, Consumer<List<String>> submit) {
        nested(new WizardTextInputScreen(this, title, fields, "", "vizedit.properties.detail", error, submit, null));
    }
    private static WizardTextInputScreen.FieldSpec field(String label, String placeholder, String value, int maxLength) {
        return new WizardTextInputScreen.FieldSpec(label, placeholder, value, maxLength);
    }
    private static WizardOptionScreen.OptionSpec option(String label, Runnable action) {
        return new WizardOptionScreen.OptionSpec(label, "", -1, action);
    }
    private static boolean validName(String value, boolean optional) {
        return (optional || !value.isBlank()) && value.length() <= 256 && value.codePoints().noneMatch(Character::isISOControl);
    }
    private static Double height(String value) {
        if (value.isBlank()) return null;
        double number = Double.parseDouble(value.trim());
        if (!Double.isFinite(number) || number < -64 || number > 320) throw new NumberFormatException();
        return number;
    }

    @Override public void close() {
        // 更换父引用或等级后旧域名障碍已失效，自然岸线偏移仍可继续使用。
        if (!Objects.equals(session.draft.parent, before.parent) || !Objects.equals(session.draft.level, before.level)) {
            session.draft.boundaryState = session.draft.boundaryState.withoutHardConstraints();
            session.status = "";
        }
        if (!checkpointed && !session.draft.same(before)) { session.history.push(before); checkpointed = true; }
        VizEditClientManager.show();
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0171B22);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 13, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(t("vizedit.properties.prompt")), width / 2, 32, 0xFFC5D3E2);
        for (Summary summary : summaries) context.drawTextWithShadow(textRenderer,
            Text.literal(textRenderer.trimToWidth(summary.text, summary.width)), summary.x, summary.y, 0xFFC5D3E2);
        super.render(context, mouseX, mouseY, delta);
        for (Summary summary : summaries) if (mouseX >= summary.x && mouseX <= summary.x + summary.width && mouseY >= summary.y && mouseY < summary.y + 11)
            context.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(Text.literal(summary.text), Math.min(400, width - 24)), mouseX, mouseY);
    }
    private static String display(String value) { return value == null || value.isBlank() ? t("description.ui.none") : value; }
    private static String t(String key) { return I18nManager.translate(key); }
    private record Summary(String text, int x, int y, int width) { }
}
