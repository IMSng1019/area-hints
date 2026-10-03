package areahint.description;

import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

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

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("title", sanitizeBookField(title, I18nManager.translate("description.book.default.title")));
        nbt.putString("author", sanitizeBookField(author, "Areas Hint"));

        NbtList pages = new NbtList();
        String fallbackDescription = I18nManager.translate("description.book.no_description");
        for (String page : paginate(description == null || description.isEmpty() ? fallbackDescription : description)) {
            // 1.20.2 起文本序列化入口由 Text.Serializer.toJson 改为 Text.Serialization.toJsonString
            //? if >=1.20.2 {
            pages.add(NbtString.of(Text.Serialization.toJsonString(Text.literal(page))));
            //?} else {
            /*pages.add(NbtString.of(Text.Serializer.toJson(Text.literal(page))));*/
            //?}
        }
        nbt.put("pages", pages);
        book.setNbt(nbt);

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
