package areahint.description;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * 版本侧的成书构建（机制 ② 的实现侧，1.20.5+）：书本内容改用数据组件 WrittenBookContentComponent。
 * 旧的 ItemStack.setNbt 与无参 toJsonString 在 1.20.5 已被移除，所以这一块必须整块按版本实现。
 */
public final class BookStackFactory {
    private BookStackFactory() {
    }

    public static ItemStack createBook(String title, String author, List<String> pageTexts) {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : pageTexts) {
            pages.add(new Filterable<>(Component.literal(page), Optional.empty()));
        }
        WrittenBookContent content = new WrittenBookContent(
                new Filterable<>(title, Optional.empty()), author, 0, pages, true);
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }
}
