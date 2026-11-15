package areahint.description;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

/**
 * 版本侧的成书构建（机制 ② 的实现侧，1.20.5+）：书本内容改用数据组件 WrittenBookContentComponent。
 * 旧的 ItemStack.setNbt 与无参 toJsonString 在 1.20.5 已被移除，所以这一块必须整块按版本实现。
 */
public final class BookStackFactory {
    private BookStackFactory() {
    }

    public static ItemStack createBook(String title, String author, List<String> pageTexts) {
        List<RawFilteredPair<Text>> pages = new ArrayList<>();
        for (String page : pageTexts) {
            pages.add(new RawFilteredPair<>(Text.literal(page), Optional.empty()));
        }
        WrittenBookContentComponent content = new WrittenBookContentComponent(
                new RawFilteredPair<>(title, Optional.empty()), author, 0, pages, true);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        return book;
    }
}
