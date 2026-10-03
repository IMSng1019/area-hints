package areahint.network;

/**
 * 网络字段读取器：与 NetworkWriter 一一对应，只覆盖现有协议真正用到的五种类型。
 * 接收器只拿得到它，不会接触任何版本的缓冲区类型。
 */
public interface NetworkReader {
    String readString();

    boolean readBoolean();

    int readInt();

    long readLong();

    double readDouble();
}
