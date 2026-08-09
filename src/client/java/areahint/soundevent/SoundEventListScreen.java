package areahint.soundevent;

import areahint.commandui.CommandUiScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 可滚动的声音或音符盒乐器列表，每个目录项都对应一个真实按钮。
 */
public final class SoundEventListScreen extends CommandUiScreen {
    private static final int LIST_TOP = 48;
    private static final int FOOTER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 28;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen previousScreen;
    private final SoundEventVisualController.SelectionContext context;
    private final String heading;
    private final List<SoundEventSelection> selections;
    private final List<SoundEventCatalog.InstrumentGroup> instruments;
    private SoundListWidget list;

    private SoundEventListScreen(Screen previousScreen,
                                 SoundEventVisualController.SelectionContext context,
                                 String heading,
                                 List<SoundEventSelection> selections,
                                 List<SoundEventCatalog.InstrumentGroup> instruments) {
        super("commandui.replacesoundevent.title", previousScreen);
        this.previousScreen = previousScreen;
        this.context = context;
        this.heading = heading;
        this.selections = selections;
        this.instruments = instruments;
    }

    static SoundEventListScreen forSelections(Screen previousScreen,
                                              SoundEventVisualController.SelectionContext context,
                                              String heading,
                                              List<SoundEventSelection> selections) {
        return new SoundEventListScreen(previousScreen, context, heading,
                List.copyOf(selections), List.of());
    }

    static SoundEventListScreen forInstruments(Screen previousScreen,
                                               SoundEventVisualController.SelectionContext context,
                                               SoundEventCatalog.Category category) {
        return new SoundEventListScreen(previousScreen, context,
                t("soundevent.category.note_block"), List.of(), List.copyOf(category.instruments()));
    }

    @Override
    protected void init() {
        int bottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - FOOTER_HEIGHT);
        this.list = new SoundListWidget(this.client, this.width, bottom - LIST_TOP, LIST_TOP);
        this.addDrawableChild(this.list);
        if (!this.instruments.isEmpty()) {
            for (SoundEventCatalog.InstrumentGroup instrument : this.instruments) {
                this.list.addItem(instrument.soundId(),
                        t("soundevent.hover.instrument", instrument.soundId()),
                        () -> openNotes(instrument));
            }
        } else {
            for (SoundEventSelection selection : this.selections) {
                this.list.addItem(selectionLabel(selection), selectionTooltip(selection),
                        () -> this.context.select(selection));
            }
        }

        int buttonWidth = 90;
        int gap = 8;
        int x = (this.width - buttonWidth * 2 - gap) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("soundevent.button.back")),
                        button -> back())
                .dimensions(x, this.height - 26, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("soundevent.button.cancel")),
                        button -> this.context.returnToOrigin())
                .dimensions(x + buttonWidth + gap, this.height - 26, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(this.textRenderer.trimToWidth(this.heading, Math.max(40, this.width - 40))),
                this.width / 2, 28, 0xFFFF55);
    }

    @Override
    public void close() {
        back();
    }

    private void openNotes(SoundEventCatalog.InstrumentGroup instrument) {
        if (this.client != null) {
            this.client.setScreen(forSelections(this, this.context,
                    t("soundevent.screen.instrument", instrument.soundId()), instrument.notes()));
        }
    }

    private void back() {
        if (this.client != null) {
            this.client.setScreen(this.previousScreen);
        }
    }

    private static String selectionLabel(SoundEventSelection selection) {
        if (selection.isNoteBlock()) {
            return t("soundevent.button.note", selection.note(), formatPitch(selection.pitch()));
        }
        return selection.soundId();
    }

    private static String selectionTooltip(SoundEventSelection selection) {
        if (selection.isNoteBlock()) {
            return t("soundevent.hover.note", selection.soundId(), selection.note(), formatPitch(selection.pitch()));
        }
        return t("soundevent.hover.sound", selection.soundId());
    }

    private static String formatPitch(float pitch) {
        return String.format(java.util.Locale.ROOT, "%.3f", pitch)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }

    private final class SoundListWidget extends ElementListWidget<SoundListWidget.Entry> {
        SoundListWidget(MinecraftClient client, int width, int height, int top) {
            super(client, width, height, top, ROW_HEIGHT);
            this.setRenderBackground(false);
        }

        void addItem(String label, String tooltip, Runnable action) {
            this.addEntry(new Entry(label, tooltip, action));
        }

        @Override
        public int getRowWidth() {
            return Math.min(520, SoundEventListScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private final class Entry extends ElementListWidget.Entry<Entry> {
            private final ButtonWidget button;

            private Entry(String label, String tooltip, Runnable action) {
                this.button = ButtonWidget.builder(Text.literal(label), ignored -> action.run())
                        .dimensions(0, 0, 200, BUTTON_HEIGHT).build();
                this.button.setTooltip(Tooltip.of(Text.literal(tooltip)));
            }

            @Override
            public void render(DrawContext drawContext, int index, int y, int x, int entryWidth,
                               int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.button.setDimensionsAndPosition(entryWidth, BUTTON_HEIGHT, x, y + 4);
                this.button.render(drawContext, mouseX, mouseY, tickDelta);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.button.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(this.button);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(this.button);
            }
        }
    }
}
