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
import net.minecraft.network.chat.Component;

/**
 * 声音分类图形界面，所有分类和“无声音”都使用独立按钮。
 */
public final class SoundEventCategoryScreen extends CommandUiScreen {
    private static final int LIST_TOP = 54;
    private static final int FOOTER_HEIGHT = 32;
    private static final int ROW_HEIGHT = 26;
    private static final int BUTTON_HEIGHT = 20;
    private final SoundEventVisualController.SelectionContext context;
    private CategoryListWidget list;

    SoundEventCategoryScreen(SoundEventVisualController.SelectionContext context) {
        super("commandui.replacesoundevent.title", context.returnScreen());
        this.context = context;
    }

    @Override
    protected void init() {
        int bottom = Math.max(LIST_TOP + ROW_HEIGHT, this.height - FOOTER_HEIGHT);
        this.list = new CategoryListWidget(this.minecraft, this.width, bottom - LIST_TOP, LIST_TOP);
        this.addRenderableWidget(this.list);
        this.list.addCategory(t("soundevent.button.none"), t("soundevent.hover.none"),
                () -> this.context.select(SoundEventSelection.none()));
        for (SoundEventCatalog.Category category : SoundEventCatalog.getCategories()) {
            int count = SoundEventCatalog.CATEGORY_NOTE_BLOCK.equals(category.key())
                    ? category.instruments().size() : category.sounds().size();
            String name = t("soundevent.category." + category.key());
            this.list.addCategory(t("soundevent.button.category", name, count),
                    t("soundevent.hover.category", name), () -> openCategory(category));
        }

        int width = 100;
        this.addRenderableWidget(Button.builder(Component.literal(t("soundevent.button.cancel")),
                        button -> this.context.returnToOrigin())
                .bounds((this.width - width) / 2, this.height - 26, width, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        super.extractRenderState(drawContext, mouseX, mouseY, delta);
        String currentText = t("soundevent.screen.current",
                SoundEventManager.getDisplayText(this.context.currentSelection()));
        drawContext.centeredText(this.font,
                Component.literal(this.font.plainSubstrByWidth(currentText, Math.max(40, this.width - 40))),
                this.width / 2, 32, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.context.returnToOrigin();
    }

    private void openCategory(SoundEventCatalog.Category category) {
        if (this.minecraft == null) {
            return;
        }
        if (SoundEventCatalog.CATEGORY_NOTE_BLOCK.equals(category.key())) {
            areahint.util.ClientScreenCompat.setScreen(this.minecraft, SoundEventListScreen.forInstruments(this, this.context, category));
        } else {
            areahint.util.ClientScreenCompat.setScreen(this.minecraft, SoundEventListScreen.forSelections(this, this.context,
                    t("soundevent.category." + category.key()), category.sounds()));
        }
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }

    private final class CategoryListWidget extends CompatEntryListWidget<CategoryListWidget.Entry> {
        CategoryListWidget(Minecraft client, int width, int height, int top) {
            super(client, width, height, top, ROW_HEIGHT);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addCategory(String label, String tooltip, Runnable action) {
            this.addEntry(new areahint.soundevent.SoundEventCategoryScreen.CategoryListWidget.Entry(label, tooltip, action));
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, SoundEventCategoryScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private final class Entry extends ContainerObjectSelectionList.Entry<areahint.soundevent.SoundEventCategoryScreen.CategoryListWidget.Entry> {
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
                this.button.setPosition(x, y + 3);
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
