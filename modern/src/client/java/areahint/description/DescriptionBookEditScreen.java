package areahint.description;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import areahint.util.GuiInputCompat;

import areahint.i18n.I18nManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 使用原版书本外观录入描述，点击完成后直接保存到描述数据库。
 */
public final class DescriptionBookEditScreen extends Screen {
    private static final int MAX_TEXT_WIDTH = 114;
    private static final int PAGE_TEXT_X_OFFSET = 36;
    private static final int PAGE_TEXT_Y_OFFSET = 32;
    private static final int PAGE_TEXT_WIDTH = 114;
    private static final int PAGE_TEXT_HEIGHT = 128;
    private static final int PAGE_BACKGROUND_WIDTH = 192;
    private static final int PAGE_BACKGROUND_HEIGHT = 192;
    private static final int PAGE_CHARACTER_LIMIT = 1024;

    private final Player player;
    private final Screen previousScreen;
    private final List<String> pages;
    private final TextFieldHelper selectionManager;
    private int currentPage;
    private PageButton nextPageButton;
    private PageButton previousPageButton;
    private boolean submitted;
    private boolean canceled;

    private DescriptionBookEditScreen(Player player, List<String> pages, Screen previousScreen) {
        super(Component.literal(I18nManager.translate("description.book.title")));
        this.player = player;
        this.pages = pages;
        this.previousScreen = previousScreen;
        this.selectionManager = new TextFieldHelper(
            this::getCurrentPageContent,
            this::setCurrentPageContent,
            TextFieldHelper.createClipboardGetter(Minecraft.getInstance()),
            TextFieldHelper.createClipboardSetter(Minecraft.getInstance()),
            this::isPageContentValid
        );
    }

    public static void open(String title, String initialDescription) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(Component.literal(I18nManager.translate("description.book.instruction")));
        areahint.util.ClientScreenCompat.setScreen(client, new DescriptionBookEditScreen(client.player, splitToBookPages(initialDescription), areahint.util.ClientScreenCompat.getScreen(client)));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = 196;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> submit())
            .bounds(centerX + 2, buttonY, 98, 20)
            .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
            .bounds(centerX - 100, buttonY, 98, 20)
            .build());

        int bookX = (width - PAGE_BACKGROUND_WIDTH) / 2;
        nextPageButton = addRenderableWidget(new PageButton(bookX + 116, 159, true, button -> openNextPage(), true));
        previousPageButton = addRenderableWidget(new PageButton(bookX + 43, 159, false, button -> openPreviousPage(), true));
        updatePageButtons();
        selectionManager.setCursorToEnd();
        areahint.util.TextInputCompat.area(bookX + PAGE_TEXT_X_OFFSET, PAGE_TEXT_Y_OFFSET,
            bookX + PAGE_TEXT_X_OFFSET + PAGE_TEXT_WIDTH, PAGE_TEXT_Y_OFFSET + PAGE_TEXT_HEIGHT);
        areahint.util.TextInputCompat.start(this);
    }

    @Override
    public void removed() {
        areahint.util.TextInputCompat.stop(this);
        super.removed();
        if (!submitted && !canceled && DescriptionManager.getInstance().isWaitingForBookInput()) {
            DescriptionManager.getInstance().cancel();
        }
    }

    @Override
    public void onClose() {
        canceled = true;
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, previousScreen);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = areahint.util.PlatformInputCompat.secondaryCode(inputEvent), modifiers = inputEvent.modifiers();
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    */
        // 描述指令中的书本是编辑界面，不是“查看描述”的只读界面。
        // 绑定键在这里应继续作为普通按键输入处理，不能触发关闭界面。
        if (super.keyPressed(inputEvent)) {
        /*if (super.keyPressed(keyCode, scanCode, modifiers)) {
        */
            return true;
        }
        if (GuiInputCompat.isSelectAll(keyCode, scanCode, modifiers)) {
            selectionManager.selectAll();
            return true;
        }
        if (GuiInputCompat.isCopy(keyCode, scanCode, modifiers)) {
            selectionManager.copy();
            return true;
        }
        if (GuiInputCompat.isPaste(keyCode, scanCode, modifiers)) {
            selectionManager.paste();
            return true;
        }
        if (GuiInputCompat.isCut(keyCode, scanCode, modifiers)) {
            selectionManager.cut();
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_BACKSPACE -> selectionManager.removeCharsFromCursor(-1);
            case InputConstants.KEY_DELETE -> selectionManager.removeCharsFromCursor(1);
            case InputConstants.KEY_RIGHT -> selectionManager.moveByChars(1, GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_LEFT -> selectionManager.moveByChars(-1, GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_HOME -> selectionManager.setCursorToStart(GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_END -> selectionManager.setCursorToEnd(GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> selectionManager.insertText("\n");
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent inputEvent) {
        int chr = inputEvent.codepoint();
    /*public boolean charTyped(char chr, int modifiers) {
    */
        if (super.charTyped(inputEvent)) {
        /*if (super.charTyped(chr, modifiers)) {
        */
            return true;
        }
        if (GuiInputCompat.isValidChar(chr)) {
            selectionManager.insertText(Character.toString(chr));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
        double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
    /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
    */
        // 鼠标绑定键同样不能关闭描述编辑界面，避免 adddescription 流程被误取消。
        return super.mouseClicked(clickEvent, doubled);
        /*return super.mouseClicked(mouseX, mouseY, button);
        */
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int bookX = (width - PAGE_BACKGROUND_WIDTH) / 2;
        // 1.21.2 起需 RenderLayer 工厂，1.21.6 起改用 GUI 管线；各代纹理尺寸均保留为 256。
        /*context.drawTexture(net.minecraft.client.gui.screen.ingame.BookScreen.BOOK_TEXTURE, bookX, 2, 0, 0, PAGE_BACKGROUND_WIDTH, PAGE_BACKGROUND_HEIGHT);
        */
        /*context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, net.minecraft.client.gui.screen.ingame.BookScreen.BOOK_TEXTURE, bookX, 2, 0F, 0F, PAGE_BACKGROUND_WIDTH, PAGE_BACKGROUND_HEIGHT, 256, 256);*/
        context.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.screens.inventory.BookViewScreen.BOOK_LOCATION, bookX, 2, 0F, 0F, PAGE_BACKGROUND_WIDTH, PAGE_BACKGROUND_HEIGHT, 256, 256);
        Component pageIndicator = Component.literal((currentPage + 1) + "/" + pages.size());
        context.text(font, pageIndicator, bookX + PAGE_TEXT_X_OFFSET + PAGE_TEXT_WIDTH - font.width(pageIndicator), 18, 0xFF000000, false);

        int textX = bookX + PAGE_TEXT_X_OFFSET;
        int textY = PAGE_TEXT_Y_OFFSET;
        areahint.util.TextInputCompat.area(textX, textY, textX + PAGE_TEXT_WIDTH, textY + PAGE_TEXT_HEIGHT);
        String page = getCurrentPageContent();
        // 1.21.2 起 drawTextWrapped 改名为 drawWrappedText 并新增阴影参数（原方法无阴影，传 false）
        /*context.drawTextWrapped(textRenderer, Text.literal(page), textX, textY, MAX_TEXT_WIDTH, 0xFF000000);
        */
        context.textWithWordWrap(font, Component.literal(page), textX, textY, MAX_TEXT_WIDTH, 0xFF000000, false);
        drawCursor(context, textX, textY, page);
    }

    private void submit() {
        Optional<String> description = getDescription();
        if (description.isEmpty()) {
            if (player != null) {
                player.sendSystemMessage(Component.literal(I18nManager.translate("description.book.error.empty")).withStyle(ChatFormatting.RED));
            }
            return;
        }
        submitted = true;
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, previousScreen);
        }
        DescriptionManager.getInstance().receiveBookDescription(description.get());
    }

    private Optional<String> getDescription() {
        List<String> cleanedPages = new ArrayList<>();
        for (String page : pages) {
            if (!page.trim().isEmpty() || !cleanedPages.isEmpty()) {
                cleanedPages.add(page);
            }
        }
        while (!cleanedPages.isEmpty() && cleanedPages.get(cleanedPages.size() - 1).trim().isEmpty()) {
            cleanedPages.remove(cleanedPages.size() - 1);
        }
        String description = String.join("\n\n", cleanedPages).trim();
        return description.isEmpty() ? Optional.empty() : Optional.of(description);
    }

    private void openPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            selectionManager.setCursorToEnd();
            updatePageButtons();
        }
    }

    private void openNextPage() {
        if (currentPage < pages.size() - 1) {
            currentPage++;
        } else {
            pages.add("");
            currentPage++;
        }
        selectionManager.setCursorToEnd();
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPageButton != null) {
            previousPageButton.visible = currentPage > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = true;
        }
    }

    private String getCurrentPageContent() {
        if (currentPage < 0 || currentPage >= pages.size()) {
            return "";
        }
        return pages.get(currentPage);
    }

    private void setCurrentPageContent(String content) {
        if (currentPage < 0 || currentPage >= pages.size()) {
            return;
        }
        pages.set(currentPage, content);
    }

    private boolean isPageContentValid(String content) {
        return content != null
            && content.length() <= PAGE_CHARACTER_LIMIT
            && font.wordWrapHeight(Component.literal(content), MAX_TEXT_WIDTH) <= PAGE_TEXT_HEIGHT;
            /*&& textRenderer.getWrappedLinesHeight(content, MAX_TEXT_WIDTH) <= PAGE_TEXT_HEIGHT;
            */
    }

    private void drawCursor(GuiGraphicsExtractor context, int textX, int textY, String page) {
        int cursor = selectionManager.getSelectionPos();
        if (cursor < 0 || cursor > page.length()) {
            return;
        }
        String beforeCursor = page.substring(0, cursor);
        String[] rawLines = beforeCursor.split("\\n", -1);
        String currentLine = rawLines.length == 0 ? "" : rawLines[rawLines.length - 1];
        int wrappedLineCountBeforeCurrent = 0;
        for (int i = 0; i < rawLines.length - 1; i++) {
            wrappedLineCountBeforeCurrent += Math.max(1, font.split(Component.literal(rawLines[i]), MAX_TEXT_WIDTH).size());
        }
        List<?> currentWrappedLines = font.split(Component.literal(currentLine), MAX_TEXT_WIDTH);
        int currentWrappedLine = Math.max(0, currentWrappedLines.size() - 1);
        int lineIndex = wrappedLineCountBeforeCurrent + currentWrappedLine;
        int cursorX = textX + Math.min(MAX_TEXT_WIDTH, font.width(currentLine));
        int cursorY = textY + lineIndex * 9;
        if ((areahint.util.ClientScreenCompat.getGuiTicks(Minecraft.getInstance()) / 6) % 2 == 0) {
            context.fill(cursorX, cursorY, cursorX + 1, cursorY + 9, 0xFF000000);
        }
    }

    private static List<String> splitToBookPages(String description) {
        List<String> pages = new ArrayList<>();
        String clean = description == null ? "" : description;
        if (clean.isEmpty()) {
            pages.add("");
            return pages;
        }
        String[] split = clean.split("\\n\\n", -1);
        for (String page : split) {
            pages.add(page);
        }
        if (pages.isEmpty()) {
            pages.add("");
        }
        return pages;
    }
}
