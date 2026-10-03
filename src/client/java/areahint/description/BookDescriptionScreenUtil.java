package areahint.description;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用原版书本界面展示域名描述。
 */
public final class BookDescriptionScreenUtil {
    private static final int PAGE_CHARACTER_LIMIT = 240;

    private BookDescriptionScreenUtil() {
    }

    public static void openDescriptionBook(String title, String author, String description) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        // 书目内容的构建方式各版本不同（1.20.4 及以前是 NBT，1.20.5 起是数据组件），整块交给版本侧实现
        String safeTitle = sanitizeBookField(title, I18nManager.translate("description.book.default.title"));
        String safeAuthor = sanitizeBookField(author, "Areas Hint");
        String fallbackDescription = I18nManager.translate("description.book.no_description");
        List<String> pageTexts = paginate(description == null || description.isEmpty() ? fallbackDescription : description);
        ItemStack book = BookStackFactory.createBook(safeTitle, safeAuthor, pageTexts);

        client.setScreen(new DescriptionBookScreen(BookScreen.Contents.create(book)));
    }

    static boolean isDescriptionBookScreen(Screen screen) {
        return screen instanceof DescriptionBookScreen;
    }

    private static final class DescriptionBookScreen extends BookScreen {
        private DescriptionBookScreen(Contents contents) {
            super(contents);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (DescriptionKeyHandler.shouldCloseOnBoundKey(keyCode, scanCode)) {
                DescriptionKeyHandler.consumeCloseDescriptionBookKey(keyCode, scanCode);
                close();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (DescriptionKeyHandler.shouldCloseOnBoundMouse(button)) {
                DescriptionKeyHandler.consumeCloseDescriptionBookMouse(button);
                close();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private static String sanitizeBookField(String value, String fallback) {
        String clean = value == null || value.trim().isEmpty() ? fallback : value.trim();
        return clean.length() > 32 ? clean.substring(0, 32) : clean;
    }

    private static List<String> paginate(String text) {
        List<String> pages = new ArrayList<>();
        String[] lines = text.split("\\R", -1);
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.length() > PAGE_CHARACTER_LIMIT) {
                if (!current.isEmpty()) {
                    pages.add(current.toString());
                    current.setLength(0);
                }
                int index = 0;
                while (index < line.length()) {
                    int end = Math.min(index + PAGE_CHARACTER_LIMIT, line.length());
                    pages.add(line.substring(index, end));
                    index = end;
                }
                continue;
            }

            int additionalLength = line.length() + (current.isEmpty() ? 0 : 1);
            if (!current.isEmpty() && current.length() + additionalLength > PAGE_CHARACTER_LIMIT) {
                pages.add(current.toString());
                current.setLength(0);
            }
            if (!current.isEmpty()) {
                current.append('\n');
            }
            current.append(line);
        }

        if (!current.isEmpty() || pages.isEmpty()) {
            pages.add(current.toString());
        }
        return pages;
    }
}
