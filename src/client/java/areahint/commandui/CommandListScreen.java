package areahint.commandui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 命令面板，集中展示全部用户可见指令按钮。
 */
public class CommandListScreen extends CommandUiScreen {
    private static final int SEARCH_WIDTH = 260;
    private static final int SEARCH_Y = 30;
    private static final int LIST_TOP = 58;

    private CommandListWidget list;
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private Text hoveredDescription;

    public CommandListScreen(Screen parent) {
        super("commandui.commands.title", parent);
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(SEARCH_WIDTH, Math.max(120, this.width - 48));
        int searchX = (this.width - searchWidth) / 2;
        this.searchField = new TextFieldWidget(this.textRenderer, searchX, SEARCH_Y, searchWidth, BUTTON_HEIGHT, Text.empty());
        this.searchField.setMaxLength(64);
        this.searchField.setText(this.searchQuery);
        this.searchField.setPlaceholder(Text.literal(t("commandui.search.placeholder")));
        this.searchField.setChangedListener(value -> {
            this.searchQuery = value == null ? "" : value;
            rebuildCommandList();
        });

        this.list = new CommandListWidget(this.client, this.width, this.height, LIST_TOP, this.height - 32);
        this.addDrawableChild(this.list);
        this.addDrawableChild(this.searchField);
        rebuildCommandList();

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int x = (this.width - (buttonWidth * 2 + 4)) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.back")), button -> close())
            .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.close")), button -> closeToGameFromBoundKey())
            .dimensions(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused()
                && this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField != null && this.searchField.isFocused() && this.searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.hoveredDescription = null;
        super.render(context, mouseX, mouseY, delta);
        if (this.hoveredDescription != null) {
            drawWrappedTooltip(context, this.hoveredDescription, mouseX, mouseY);
        }
    }

    private void rebuildCommandList() {
        if (this.list == null) {
            return;
        }

        this.list.clearCommandEntries();
        String normalizedSearch = normalize(this.searchQuery);
        String currentCategory = null;
        int matchedCommands = 0;
        for (CommandVisualHandler handler : CommandVisualRegistry.getHandlers()) {
            if (!matchesSearch(handler, normalizedSearch)) {
                continue;
            }
            if (!handler.categoryKey().equals(currentCategory)) {
                currentCategory = handler.categoryKey();
                this.list.addCategory(currentCategory);
            }
            this.list.addCommand(handler);
            matchedCommands++;
        }

        if (matchedCommands == 0) {
            this.list.addEmptyResult();
        }
    }

    private boolean matchesSearch(CommandVisualHandler handler, String normalizedSearch) {
        if (normalizedSearch.isBlank()) {
            return true;
        }

        List<String> searchTargets = new ArrayList<>();
        searchTargets.add(handler.id());
        searchTargets.add(handler.defaultCommand());
        searchTargets.add("/" + handler.defaultCommand());
        searchTargets.add(handler.displayName().getString());
        searchTargets.add(handler.description().getString());
        searchTargets.add(t(handler.categoryKey()));
        for (String target : searchTargets) {
            if (normalize(target).contains(normalizedSearch)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private class CommandListWidget extends ElementListWidget<CommandListWidget.Entry> {
        CommandListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 34);
            this.setRenderBackground(false);
        }

        void clearCommandEntries() {
            super.clearEntries();
        }

        void addCategory(String categoryKey) {
            this.addEntry(new CategoryEntry(categoryKey));
        }

        void addCommand(CommandVisualHandler handler) {
            this.addEntry(new CommandEntry(handler));
        }

        void addEmptyResult() {
            this.addEntry(new EmptyEntry());
        }

        @Override
        public int getRowWidth() {
            return Math.min(560, CommandListScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private abstract class Entry extends ElementListWidget.Entry<Entry> {
        }

        private class CategoryEntry extends Entry {
            private final String categoryKey;

            private CategoryEntry(String categoryKey) {
                this.categoryKey = categoryKey;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int lineY = y + 17;
                context.fill(x + 4, lineY, x + entryWidth - 4, lineY + 1, 0x66AAAAAA);
                String title = t(this.categoryKey);
                int textWidth = CommandListScreen.this.textRenderer.getWidth(title) + 12;
                context.fill(x + 4, y + 7, x + 8 + textWidth, y + 20, 0x66000000);
                context.drawTextWithShadow(CommandListScreen.this.textRenderer, Text.literal(title), x + 10, y + 10, BRIGHT_YELLOW);
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

        private class CommandEntry extends Entry {
            private static final int BUTTON_WIDTH = 168;
            private final CommandVisualHandler handler;
            private final ButtonWidget commandButton;

            private CommandEntry(CommandVisualHandler handler) {
                this.handler = handler;
                this.commandButton = ButtonWidget.builder(handler.displayName(), button -> handler.open(CommandListScreen.this))
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                if (hovered) {
                    context.fill(x + 2, y + 2, x + entryWidth - 2, y + entryHeight - 2, 0x33222222);
                }

                int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(112, entryWidth / 3));
                this.commandButton.setWidth(buttonWidth);
                this.commandButton.setPosition(x + 6, y + 7);
                this.commandButton.render(context, mouseX, mouseY, tickDelta);

                int detailX = x + buttonWidth + 16;
                if (hovered || this.commandButton.isMouseOver(mouseX, mouseY)) {
                    CommandListScreen.this.hoveredDescription = handler.description();
                }
                CommandListScreen.this.drawTrimmed(context, Text.literal(displayCommand(handler)), detailX, y + 14,
                    entryWidth - buttonWidth - 20, hovered ? BRIGHT_YELLOW : 0xAAAAAA);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return this.commandButton.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return this.commandButton.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public List<? extends Element> children() {
                return List.of(this.commandButton);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                return List.of(this.commandButton);
            }

            private String displayCommand(CommandVisualHandler handler) {
                String command = handler.defaultCommand();
                return command == null || command.isBlank() ? t("commandui.command.settings.name") : "/" + command;
            }
        }

        private class EmptyEntry extends Entry {
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String text = t("commandui.search.empty");
                int textX = x + (entryWidth - CommandListScreen.this.textRenderer.getWidth(text)) / 2;
                context.drawTextWithShadow(CommandListScreen.this.textRenderer, Text.literal(text), textX, y + 12, 0xAAAAAA);
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
