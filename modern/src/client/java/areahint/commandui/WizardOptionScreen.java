package areahint.commandui;

import areahint.gui.CompatEntryListWidget;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 通用选项按钮页，用于等级、高度模式、颜色预设等固定选项。
 */
public class WizardOptionScreen extends CommandWizardScreen {
    public record OptionSpec(String labelKey, String detailKey, int swatchColor, Runnable action) {
    }

    private static final int BUTTON_WIDTH = 170;
    private static final int BUTTON_GAP = 8;
    private static final int ROW_HEIGHT = 28;
    private static final int SWATCH_SIZE = 10;
    private static final int LIST_TOP = 62;
    private static final int LIST_BOTTOM_PADDING = 32;

    private final String promptKey;
    private final String detailKey;
    private final List<OptionSpec> options;
    private OptionListWidget optionList;

    public WizardOptionScreen(Screen parent, String titleKey, String promptKey, String detailKey,
                              List<OptionSpec> options, Runnable cancelAction) {
        super(titleKey, parent, cancelAction);
        this.promptKey = promptKey;
        this.detailKey = detailKey;
        this.options = options;
    }

    @Override
    protected void init() {
        int listTop = getListTop();
        int listBottom = Math.max(listTop + ROW_HEIGHT, this.height - LIST_BOTTOM_PADDING);
        this.optionList = new OptionListWidget(this.minecraft, this.width, this.height, listTop, listBottom, getColumnCount());
        this.optionList.addOptions(this.options);
        this.addRenderableWidget(this.optionList);

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int x = (this.width - buttonWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(420, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 14;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
        }
    }

    private int getListTop() {
        return Math.min(LIST_TOP, Math.max(34, this.height - LIST_BOTTOM_PADDING - ROW_HEIGHT));
    }

    private int getColumnCount() {
        if (this.width < 420) {
            return 1;
        }
        int maxColumns = Math.max(1, (this.width - 40 + BUTTON_GAP) / (BUTTON_WIDTH + BUTTON_GAP));
        int optionColumns = Math.max(1, Math.min(this.options.size(), maxColumns));
        if (this.options.size() > 3) {
            return Math.max(2, optionColumns);
        }
        return optionColumns;
    }

    private class OptionListWidget extends CompatEntryListWidget<OptionListWidget.Entry> {
        private final int columns;

        OptionListWidget(Minecraft client, int width, int height, int top, int bottom, int columns) {
            super(client, width, bottom - top, top, ROW_HEIGHT);
            this.columns = Math.max(1, columns);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addOptions(List<OptionSpec> options) {
            for (int i = 0; i < options.size(); i += this.columns) {
                int end = Math.min(i + this.columns, options.size());
                this.addEntry(new areahint.commandui.WizardOptionScreen.OptionListWidget.Entry(options.subList(i, end)));
            }
        }

        @Override
        public int getRowWidth() {
            return this.columns * BUTTON_WIDTH + (this.columns - 1) * BUTTON_GAP;
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            // 滚动条贴近屏幕右边缘，避免长列表看起来偏向内容区内部。
            return this.width - 6;
        }

        private class Entry extends ContainerObjectSelectionList.Entry<areahint.commandui.WizardOptionScreen.OptionListWidget.Entry> {
            private final List<OptionSpec> rowOptions;
            private final List<Button> buttons = new ArrayList<>();

            private Entry(List<OptionSpec> rowOptions) {
                this.rowOptions = List.copyOf(rowOptions);
                for (OptionSpec option : this.rowOptions) {
                    this.buttons.add(Button.builder(Component.literal(t(option.labelKey())), button -> option.action().run())
                        .bounds(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build());
                }
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                int rowWidth = this.rowOptions.size() * BUTTON_WIDTH + (this.rowOptions.size() - 1) * BUTTON_GAP;
                int buttonX = x + Math.max(0, (entryWidth - rowWidth) / 2);
                for (int i = 0; i < this.buttons.size(); i++) {
                    Button button = this.buttons.get(i);
                    OptionSpec option = this.rowOptions.get(i);
                    button.setPosition(buttonX + i * (BUTTON_WIDTH + BUTTON_GAP), y + 4);
                    button.extractRenderState(context, mouseX, mouseY, tickDelta);
                    drawSwatchForButton(context, button, option);
                }
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                if (button != 0) {
                    return false;
                }
                for (Button optionButton : this.buttons) {
                    if (optionButton.mouseClicked(clickEvent, doubled)) {
                    /*if (optionButton.mouseClicked(mouseX, mouseY, button)) {
                    */
                        return true;
                    }
                }
                return false;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return this.buttons;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return this.buttons;
            }

            private void drawSwatchForButton(GuiGraphicsExtractor context, Button button, OptionSpec option) {
                if (option.swatchColor() < 0) {
                    return;
                }
                int swatchX = button.getX() + 6;
                int swatchY = button.getY() + (button.getHeight() - SWATCH_SIZE) / 2;
                context.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | option.swatchColor());
                areahint.util.GuiDrawCompat.drawBorder(context, swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 0xFFFFFFFF);
            }
        }
    }
}
