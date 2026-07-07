package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

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
    private static final int SCROLLBAR_GAP = 8;

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
        this.optionList = new OptionListWidget(this.client, this.width, this.height, listTop, listBottom, getColumnCount());
        this.optionList.addOptions(this.options);
        this.addDrawableChild(this.optionList);

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int x = (this.width - buttonWidth) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(420, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 14;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
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

    private class OptionListWidget extends ElementListWidget<OptionListWidget.Entry> {
        private final int columns;

        OptionListWidget(MinecraftClient client, int width, int height, int top, int bottom, int columns) {
            super(client, width, bottom - top, top, ROW_HEIGHT);
            this.columns = Math.max(1, columns);
            this.setRenderBackground(false);
        }

        void addOptions(List<OptionSpec> options) {
            for (int i = 0; i < options.size(); i += this.columns) {
                int end = Math.min(i + this.columns, options.size());
                this.addEntry(new Entry(options.subList(i, end)));
            }
        }

        @Override
        public int getRowWidth() {
            return this.columns * BUTTON_WIDTH + (this.columns - 1) * BUTTON_GAP;
        }

        @Override
        protected int getScrollbarPositionX() {
            // 滚动条放在选项区右侧，避免和按钮、页脚取消按钮重叠。
            return Math.min(this.width - 6, this.getRowRight() + SCROLLBAR_GAP);
        }

        private class Entry extends ElementListWidget.Entry<Entry> {
            private final List<OptionSpec> rowOptions;
            private final List<ButtonWidget> buttons = new ArrayList<>();

            private Entry(List<OptionSpec> rowOptions) {
                this.rowOptions = List.copyOf(rowOptions);
                for (OptionSpec option : this.rowOptions) {
                    this.buttons.add(ButtonWidget.builder(Text.literal(t(option.labelKey())), button -> option.action().run())
                        .dimensions(0, 0, BUTTON_WIDTH, BUTTON_HEIGHT).build());
                }
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int rowWidth = this.rowOptions.size() * BUTTON_WIDTH + (this.rowOptions.size() - 1) * BUTTON_GAP;
                int buttonX = x + Math.max(0, (entryWidth - rowWidth) / 2);
                for (int i = 0; i < this.buttons.size(); i++) {
                    ButtonWidget button = this.buttons.get(i);
                    OptionSpec option = this.rowOptions.get(i);
                    button.setPosition(buttonX + i * (BUTTON_WIDTH + BUTTON_GAP), y + 4);
                    button.render(context, mouseX, mouseY, tickDelta);
                    drawSwatchForButton(context, button, option);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) {
                    return false;
                }
                for (ButtonWidget optionButton : this.buttons) {
                    if (optionButton.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public List<? extends Element> children() {
                return this.buttons;
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return this.buttons;
            }

            private void drawSwatchForButton(DrawContext context, ButtonWidget button, OptionSpec option) {
                if (option.swatchColor() < 0) {
                    return;
                }
                int swatchX = button.getX() + 6;
                int swatchY = button.getY() + (button.getHeight() - SWATCH_SIZE) / 2;
                context.fill(swatchX, swatchY, swatchX + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | option.swatchColor());
                context.drawBorder(swatchX, swatchY, SWATCH_SIZE, SWATCH_SIZE, 0xFFFFFFFF);
            }
        }
    }
}
