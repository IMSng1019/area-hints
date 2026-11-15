package areahint.network;

/**
 * 网络字段读取器：与 NetworkWriter 一一对应，只覆盖现有协议真正用到的五种类型。
 * 接收器只拿得到它，不会接触任何版本的缓冲区类型。
 */
public interface NetworkReader {
    String readString();

    /** 带长度上限的读取：现有协议里有 33 处显式写了 32767，保留原样以免改写调用点 */
    String readString(int maxLength);

    boolean readBoolean();

    int readInt();

    long readLong();

    double readDouble();

    /** 是否还有可读字节：SubtitleCommand 用它兼容旧客户端少发字段的情况 */
    boolean isReadable();
}
