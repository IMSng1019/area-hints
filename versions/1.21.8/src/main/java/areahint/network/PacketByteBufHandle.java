package areahint.network;

import net.minecraft.network.PacketByteBuf;

/**
 * 1.20.1–1.20.4 代际的缓冲区句柄：所有读写直接转发给 PacketByteBuf，编码方式与原代码逐字一致。
 * 本类位于版本目录，只有 1.20.4 节点会挂载它。
 */
public final class PacketByteBufHandle implements NetworkBuf {
    private final PacketByteBuf buf;

    public PacketByteBufHandle(PacketByteBuf buf) {
        this.buf = buf;
    }

    /** 取出底层缓冲区，供发送时使用 */
    public PacketByteBuf unwrap() {
        return buf;
    }

    @Override
    public NetworkBuf writeString(String value) {
        buf.writeString(value);
        return this;
    }

    @Override
    public NetworkBuf writeBoolean(boolean value) {
        buf.writeBoolean(value);
        return this;
    }

    @Override
    public NetworkBuf writeInt(int value) {
        buf.writeInt(value);
        return this;
    }

    @Override
    public NetworkBuf writeLong(long value) {
        buf.writeLong(value);
        return this;
    }

    @Override
    public NetworkBuf writeDouble(double value) {
        buf.writeDouble(value);
        return this;
    }

    @Override
    public String readString() {
        return buf.readString();
    }

    @Override
    public String readString(int maxLength) {
        return buf.readString(maxLength);
    }

    @Override
    public boolean readBoolean() {
        return buf.readBoolean();
    }

    @Override
    public int readInt() {
        return buf.readInt();
    }

    @Override
    public long readLong() {
        return buf.readLong();
    }

    @Override
    public double readDouble() {
        return buf.readDouble();
    }

    @Override
    public boolean isReadable() {
        return buf.isReadable();
    }
}
