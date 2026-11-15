package areahint.soundevent;

import areahint.commandui.CommandUiScreen;
import areahint.gui.CompatEntryListWidget;
import areahint.i18n.I18nManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        this.list = new SoundListWidget(this.minecraft, this.width, bottom - LIST_TOP, LIST_TOP);
        this.addRenderableWidget(this.list);
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
        this.addRenderableWidget(Button.builder(Component.literal(t("soundevent.button.back")),
                        button -> back())
                .bounds(x, this.height - 26, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("soundevent.button.cancel")),
                        button -> this.context.returnToOrigin())
                .bounds(x + buttonWidth + gap, this.height - 26, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        super.extractRenderState(drawContext, mouseX, mouseY, delta);
        drawContext.centeredText(this.font,
                Component.literal(this.font.plainSubstrByWidth(this.heading, Math.max(40, this.width - 40))),
                this.width / 2, 28, 0xFFFFFF55);
    }

    @Override
    public void onClose() {
        back();
    }

    private void openNotes(SoundEventCatalog.InstrumentGroup instrument) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(forSelections(this, this.context,
                    t("soundevent.screen.instrument", instrument.soundId()), instrument.notes()));
        }
    }

    private void back() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
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

    private final class SoundListWidget extends CompatEntryListWidget<SoundListWidget.Entry> {
        SoundListWidget(Minecraft client, int width, int height, int top) {
            super(client, width, height, top, ROW_HEIGHT);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addItem(String label, String tooltip, Runnable action) {
            this.addEntry(new areahint.soundevent.SoundEventListScreen.SoundListWidget.Entry(label, tooltip, action));
        }

        @Override
        public int getRowWidth() {
            return Math.min(520, SoundEventListScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private final class Entry extends ContainerObjectSelectionList.Entry<areahint.soundevent.SoundEventListScreen.SoundListWidget.Entry> {
            private final Button button;

            private Entry(String label, String tooltip, Runnable action) {
                this.button = Button.builder(Component.literal(label), ignored -> action.run())
                        .bounds(0, 0, 200, BUTTON_HEIGHT).build();
                this.button.setTooltip(Tooltip.create(Component.literal(tooltip)));
            }

            @Override
            public void extractContent(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext drawContext, int index, int y, int x, int entryWidth,
                               int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                // 1.20.1 无 setDimensionsAndPosition，等价拆成 setWidth + setPosition（高度构造时已定）
                this.button.setWidth(entryWidth);
                this.button.setPosition(x, y + 4);
                this.button.extractRenderState(drawContext, mouseX, mouseY, tickDelta);
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                return this.button.mouseClicked(clickEvent, doubled);
                /*return this.button.mouseClicked(mouseX, mouseY, button);
                */
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.button);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(this.button);
            }
        }
    }
}
