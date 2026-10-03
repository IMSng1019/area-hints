package areahint.network;

/**
 * 版本中立的缓冲区句柄：既能写也能读，方法名与原 PacketByteBuf 的用法逐字对应。
 * 这样调用点只是换类型，字段顺序与编码方式完全不动，线上字节自然不变。
 */
public interface NetworkBuf extends NetworkWriter, NetworkReader {
}
