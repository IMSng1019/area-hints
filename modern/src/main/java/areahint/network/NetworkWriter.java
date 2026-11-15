package areahint.network;

/**
 * 网络字段写入器：只覆盖现有协议真正用到的五种类型，不预留任何未来类型。
 * 各版本的实现负责把它落到该版本的缓冲区上，字段顺序由调用方决定，保证线上字节不变。
 */
public interface NetworkWriter {
    NetworkWriter writeString(String value);

    NetworkWriter writeBoolean(boolean value);

    NetworkWriter writeInt(int value);

    NetworkWriter writeLong(long value);

    NetworkWriter writeDouble(double value);
}
