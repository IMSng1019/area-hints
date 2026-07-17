package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * 通用滚动选择列表，适合域名、上级域名等长列表。
 */
public class WizardSelectionListScreen<T> extends CommandWizardScreen {
    public record SelectionItem<T>(T value, String title, String detail) {
    }

    private final String promptKey;
    private final String launchOperationId;
    private final List<SelectionItem<T>> items;
    private final Consumer<T> selectAction;
    private final boolean returnToParentOnCancel;
    private SelectionListWidget list;

    public WizardSelectionListScreen(Screen parent, String titleKey, String promptKey,
                                     List<SelectionItem<T>> items, Consumer<T> selectAction,
                                     Runnable cancelAction) {
        this(parent, titleKey, promptKey, items, selectAction, cancelAction, false);
    }

    public WizardSelectionListScreen(Screen parent, String titleKey, String promptKey,
                                     List<SelectionItem<T>> items, Consumer<T> selectAction,
                                     Runnable cancelAction, boolean returnToParentOnCancel) {
        super(titleKey, parent, cancelAction);
        this.promptKey = promptKey;
        this.launchOperationId = operationIdFromTitleKey(titleKey);
        this.items = items;
        this.selectAction = selectAction;
        this.returnToParentOnCancel = returnToParentOnCancel;
    }

    @Override
    protected void init() {
        this.list = new SelectionListWidget(this.client, this.width, this.height, 52, this.height - 32);
        this.addDrawableChild(this.list);
        for (SelectionItem<T> item : this.items) {
            this.list.addItem(item);
        }

        T launchTarget = CommandVisualLaunchContext.findMatchingValue(this.launchOperationId, this.items);
        if (launchTarget != null && this.client != null) {
            this.client.execute(() -> {
                try {
                    markFlowHandled();
                    this.selectAction.accept(launchTarget);
                } finally {
                    CommandVisualLaunchContext.consume();
                }
            });
        }

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int x = (this.width - buttonWidth) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> cancelSelection())
            .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void close() {
        if (!this.returnToParentOnCancel) {
            super.close();
            return;
        }
        cancelFlow();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(t(this.promptKey)), this.width / 2, 34, 0xFFFFFF);
        }
    }

    private void cancelSelection() {
        if (this.returnToParentOnCancel) {
            close();
        } else {
            cancelAndCloseToGame();
        }
    }

    private static String operationIdFromTitleKey(String titleKey) {
        String prefix = "commandui.";
        String suffix = ".title";
        if (titleKey == null || !titleKey.startsWith(prefix) || !titleKey.endsWith(suffix)) {
            return null;
        }
        return titleKey.substring(prefix.length(), titleKey.length() - suffix.length());
    }

    private class SelectionListWidget extends ElementListWidget<SelectionListWidget.Entry> {
        SelectionListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 30);
            this.setRenderBackground(false);
        }

        void addItem(SelectionItem<T> item) {
            this.addEntry(new Entry(item));
        }

        @Override
        public int getRowWidth() {
            return Math.min(520, WizardSelectionListScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private class Entry extends ElementListWidget.Entry<Entry> {
            private final SelectionItem<T> item;

            private Entry(SelectionItem<T> item) {
                this.item = item;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int titleColor = hovered ? BRIGHT_YELLOW : 0xFFFFFF;
                WizardSelectionListScreen.this.drawTrimmed(context, Text.literal(this.item.title()),
                    x + 4, y + 5, entryWidth - 8, titleColor);
                WizardSelectionListScreen.this.drawTrimmed(context, Text.literal(this.item.detail()),
                    x + 4, y + 17, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    WizardSelectionListScreen.this.markFlowHandled();
                    CommandVisualLaunchContext.consume();
                    WizardSelectionListScreen.this.selectAction.accept(this.item.value());
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends Element> children() {
                return List.of();
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of();
            }
        }
    }
}
