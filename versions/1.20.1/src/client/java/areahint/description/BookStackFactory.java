package areahint.description;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

/**
 * 版本侧的成书构建（机制 ② 的实现侧，1.20.4 及以前）：用 NBT 写入标题/作者/每页文本。
 * 本类位于版本目录，只有对应节点会挂载它；共享 src/ 只保留调用点。
 */
public final class BookStackFactory {
    private BookStackFactory() {
    }

    public static ItemStack createBook(String title, String author, List<String> pageTexts) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("title", title);
        nbt.putString("author", author);
        NbtList pages = new NbtList();
        for (String page : pageTexts) {
            // 1.20.1 上文本序列化入口仍叫 Text.Serializer.toJson（1.20.2 起才改名）
            pages.add(NbtString.of(Text.Serializer.toJson(Text.literal(page))));
        }
        nbt.put("pages", pages);
        book.setNbt(nbt);
        return book;
    }
}
