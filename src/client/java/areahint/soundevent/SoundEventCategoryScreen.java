package areahint.soundevent;

import areahint.commandui.CommandUiScreen;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;

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
        this.list = new CategoryListWidget(this.client, this.width, bottom - LIST_TOP, LIST_TOP);
        this.addDrawableChild(this.list);
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
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("soundevent.button.cancel")),
                        button -> this.context.returnToOrigin())
                .dimensions((this.width - width) / 2, this.height - 26, width, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        String currentText = t("soundevent.screen.current",
                SoundEventManager.getDisplayText(this.context.currentSelection()));
        drawContext.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(this.textRenderer.trimToWidth(currentText, Math.max(40, this.width - 40))),
                this.width / 2, 32, 0xAAAAAA);
    }

    @Override
    public void close() {
        this.context.returnToOrigin();
    }

    private void openCategory(SoundEventCatalog.Category category) {
        if (this.client == null) {
            return;
        }
        if (SoundEventCatalog.CATEGORY_NOTE_BLOCK.equals(category.key())) {
            this.client.setScreen(SoundEventListScreen.forInstruments(this, this.context, category));
        } else {
            this.client.setScreen(SoundEventListScreen.forSelections(this, this.context,
                    t("soundevent.category." + category.key()), category.sounds()));
        }
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }

    private final class CategoryListWidget extends ElementListWidget<CategoryListWidget.Entry> {
        CategoryListWidget(MinecraftClient client, int width, int height, int top) {
            super(client, width, height, top, ROW_HEIGHT);
            this.setRenderBackground(false);
        }

        void addCategory(String label, String tooltip, Runnable action) {
            this.addEntry(new Entry(label, tooltip, action));
        }

        @Override
        public int getRowWidth() {
            return Math.min(360, SoundEventCategoryScreen.this.width - 36);
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
                this.button.setDimensionsAndPosition(entryWidth, BUTTON_HEIGHT, x, y + 3);
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
