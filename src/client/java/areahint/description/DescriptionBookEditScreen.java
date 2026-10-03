package areahint.description;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

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

    private final PlayerEntity player;
    private final Screen previousScreen;
    private final List<String> pages;
    private final SelectionManager selectionManager;
    private int currentPage;
    private PageTurnWidget nextPageButton;
    private PageTurnWidget previousPageButton;
    private boolean submitted;
    private boolean canceled;

    private DescriptionBookEditScreen(PlayerEntity player, List<String> pages, Screen previousScreen) {
        super(Text.literal(I18nManager.translate("description.book.title")));
        this.player = player;
        this.pages = pages;
        this.previousScreen = previousScreen;
        this.selectionManager = new SelectionManager(
            this::getCurrentPageContent,
            this::setCurrentPageContent,
            SelectionManager.makeClipboardGetter(MinecraftClient.getInstance()),
            SelectionManager.makeClipboardSetter(MinecraftClient.getInstance()),
            this::isPageContentValid
        );
    }

    public static void open(String title, String initialDescription) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal(I18nManager.translate("description.book.instruction")), false);
        client.setScreen(new DescriptionBookEditScreen(client.player, splitToBookPages(initialDescription), client.currentScreen));
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int buttonY = 196;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> submit())
            .dimensions(centerX + 2, buttonY, 98, 20)
            .build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close())
            .dimensions(centerX - 100, buttonY, 98, 20)
            .build());

        int bookX = (width - PAGE_BACKGROUND_WIDTH) / 2;
        nextPageButton = addDrawableChild(new PageTurnWidget(bookX + 116, 159, true, button -> openNextPage(), true));
        previousPageButton = addDrawableChild(new PageTurnWidget(bookX + 43, 159, false, button -> openPreviousPage(), true));
        updatePageButtons();
        selectionManager.putCursorAtEnd();
    }

    @Override
    public void removed() {
        if (!submitted && !canceled && DescriptionManager.getInstance().isWaitingForBookInput()) {
            DescriptionManager.getInstance().cancel();
        }
    }

    @Override
    public void close() {
        canceled = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(previousScreen);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 描述指令中的书本是编辑界面，不是“查看描述”的只读界面。
        // 绑定键在这里应继续作为普通按键输入处理，不能触发关闭界面。
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (Screen.isSelectAll(keyCode)) {
            selectionManager.selectAll();
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            selectionManager.copy();
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            selectionManager.paste();
            return true;
        }
        if (Screen.isCut(keyCode)) {
            selectionManager.cut();
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> selectionManager.delete(-1);
            case GLFW.GLFW_KEY_DELETE -> selectionManager.delete(1);
            case GLFW.GLFW_KEY_RIGHT -> selectionManager.moveCursor(1, Screen.hasShiftDown());
            case GLFW.GLFW_KEY_LEFT -> selectionManager.moveCursor(-1, Screen.hasShiftDown());
            case GLFW.GLFW_KEY_HOME -> selectionManager.moveCursorToStart(Screen.hasShiftDown());
            case GLFW.GLFW_KEY_END -> selectionManager.moveCursorToEnd(Screen.hasShiftDown());
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> selectionManager.insert("\n");
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (super.charTyped(chr, modifiers)) {
            return true;
        }
        // 1.20.5 起 SharedConstants.isValidChar 搬到 StringHelper.isValidChar，故按版本分支
        //? if >=1.20.5 {
        /*if (net.minecraft.util.StringHelper.isValidChar(chr)) {*/
        //?} else {
        if (SharedConstants.isValidChar(chr)) {
        //?}
            selectionManager.insert(Character.toString(chr));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 鼠标绑定键同样不能关闭描述编辑界面，避免 adddescription 流程被误取消。
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int bookX = (width - PAGE_BACKGROUND_WIDTH) / 2;
        // 1.21.2 起 drawTexture 需要显式传入 RenderLayer 工厂，纹理尺寸沿用原 7 参重载的 256
        //? if <=1.21.1 {
        context.drawTexture(net.minecraft.client.gui.screen.ingame.BookScreen.BOOK_TEXTURE, bookX, 2, 0, 0, PAGE_BACKGROUND_WIDTH, PAGE_BACKGROUND_HEIGHT);
        //?} else {
        /*context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, net.minecraft.client.gui.screen.ingame.BookScreen.BOOK_TEXTURE, bookX, 2, 0F, 0F, PAGE_BACKGROUND_WIDTH, PAGE_BACKGROUND_HEIGHT, 256, 256);*/
        //?}
        Text pageIndicator = Text.literal((currentPage + 1) + "/" + pages.size());
        context.drawText(textRenderer, pageIndicator, bookX + PAGE_TEXT_X_OFFSET + PAGE_TEXT_WIDTH - textRenderer.getWidth(pageIndicator), 18, 0, false);

        int textX = bookX + PAGE_TEXT_X_OFFSET;
        int textY = PAGE_TEXT_Y_OFFSET;
        String page = getCurrentPageContent();
        // 1.21.2 起 drawTextWrapped 改名为 drawWrappedText 并新增阴影参数（原方法无阴影，传 false）
        //? if <=1.21.1 {
        context.drawTextWrapped(textRenderer, Text.literal(page), textX, textY, MAX_TEXT_WIDTH, 0);
        //?} else {
        /*context.drawWrappedText(textRenderer, Text.literal(page), textX, textY, MAX_TEXT_WIDTH, 0, false);*/
        //?}
        drawCursor(context, textX, textY, page);
    }

    private void submit() {
        Optional<String> description = getDescription();
        if (description.isEmpty()) {
            if (player != null) {
                player.sendMessage(Text.literal(I18nManager.translate("description.book.error.empty")).formatted(Formatting.RED), false);
            }
            return;
        }
        submitted = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(previousScreen);
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
            selectionManager.putCursorAtEnd();
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
        selectionManager.putCursorAtEnd();
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
            && textRenderer.getWrappedLinesHeight(content, MAX_TEXT_WIDTH) <= PAGE_TEXT_HEIGHT;
    }

    private void drawCursor(DrawContext context, int textX, int textY, String page) {
        int cursor = selectionManager.getSelectionEnd();
        if (cursor < 0 || cursor > page.length()) {
            return;
        }
        String beforeCursor = page.substring(0, cursor);
        String[] rawLines = beforeCursor.split("\\n", -1);
        String currentLine = rawLines.length == 0 ? "" : rawLines[rawLines.length - 1];
        int wrappedLineCountBeforeCurrent = 0;
        for (int i = 0; i < rawLines.length - 1; i++) {
            wrappedLineCountBeforeCurrent += Math.max(1, textRenderer.wrapLines(Text.literal(rawLines[i]), MAX_TEXT_WIDTH).size());
        }
        List<?> currentWrappedLines = textRenderer.wrapLines(Text.literal(currentLine), MAX_TEXT_WIDTH);
        int currentWrappedLine = Math.max(0, currentWrappedLines.size() - 1);
        int lineIndex = wrappedLineCountBeforeCurrent + currentWrappedLine;
        int cursorX = textX + Math.min(MAX_TEXT_WIDTH, textRenderer.getWidth(currentLine));
        int cursorY = textY + lineIndex * 9;
        if ((MinecraftClient.getInstance().inGameHud.getTicks() / 6) % 2 == 0) {
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
