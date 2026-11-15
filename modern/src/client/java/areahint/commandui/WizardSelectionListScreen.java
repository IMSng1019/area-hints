package areahint.commandui;

import areahint.gui.CompatEntryListWidget;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        this.list = new SelectionListWidget(this.minecraft, this.width, this.height, 52, this.height - 32);
        this.addRenderableWidget(this.list);
        for (SelectionItem<T> item : this.items) {
            this.list.addItem(item);
        }

        T launchTarget = CommandVisualLaunchContext.findMatchingValue(this.launchOperationId, this.items);
        if (launchTarget != null && this.minecraft != null) {
            this.minecraft.execute(() -> {
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
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelSelection())
            .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void onClose() {
        if (!this.returnToParentOnCancel) {
            super.onClose();
            return;
        }
        cancelFlow();
        if (this.minecraft != null) {
            areahint.util.ClientScreenCompat.setScreen(this.minecraft, this.parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            context.centeredText(this.font, Component.literal(t(this.promptKey)), this.width / 2, 34, 0xFFFFFFFF);
        }
    }

    private void cancelSelection() {
        if (this.returnToParentOnCancel) {
            onClose();
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

    private class SelectionListWidget extends CompatEntryListWidget<SelectionListWidget.Entry> {
        SelectionListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 30);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addItem(SelectionItem<T> item) {
            this.addEntry(new Entry(item));
        }

        @Override
        public int getRowWidth() {
            return Math.min(520, WizardSelectionListScreen.this.width - 36);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            return this.width - 6;
        }

        private class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            private final SelectionItem<T> item;

            private Entry(SelectionItem<T> item) {
                this.item = item;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                int titleColor = hovered ? BRIGHT_YELLOW : 0xFFFFFF;
                WizardSelectionListScreen.this.drawTrimmed(context, Component.literal(this.item.title()),
                    x + 4, y + 5, entryWidth - 8, titleColor);
                WizardSelectionListScreen.this.drawTrimmed(context, Component.literal(this.item.detail()),
                    x + 4, y + 17, entryWidth - 8, 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
                double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
            /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
            */
                if (button == 0) {
                    WizardSelectionListScreen.this.markFlowHandled();
                    CommandVisualLaunchContext.consume();
                    WizardSelectionListScreen.this.selectAction.accept(this.item.value());
                    return true;
                }
                return false;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }
    }
}
