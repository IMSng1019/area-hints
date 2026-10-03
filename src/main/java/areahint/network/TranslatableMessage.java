package areahint.network;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 网络包翻译消息工具类
 * 服务端写入翻译键/字面文本，客户端读取并构建 Text
 */
public class TranslatableMessage {

    public record Part(boolean isKey, String value) {}

    public static Part key(String key) { return new Part(true, key); }
    public static Part lit(String text) { return new Part(false, text); }

    /** 写入单个翻译键 */
    public static void write(NetworkWriter buf, String key) {
        buf.writeInt(1);
        buf.writeBoolean(true);
        buf.writeString(key);
    }

    /** 写入多个部分（翻译键+字面文本混合） */
    public static void write(NetworkWriter buf, Part... parts) {
        buf.writeInt(parts.length);
        for (Part part : parts) {
            buf.writeBoolean(part.isKey);
            buf.writeString(part.value);
        }
    }

    /** 客户端读取并构建 MutableText */
    public static MutableText read(NetworkReader buf) {
        int count = buf.readInt();
        MutableText result = Text.empty();
        for (int i = 0; i < count; i++) {
            boolean isKey = buf.readBoolean();
            String value = buf.readString(32767);
            result.append(isKey ? Text.translatable(value) : Text.literal(value));
        }
        return result;
    }
}
