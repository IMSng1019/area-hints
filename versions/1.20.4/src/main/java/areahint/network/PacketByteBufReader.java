package areahint.network;

import net.minecraft.network.PacketByteBuf;

/**
 * 1.20.1–1.20.4 代际的读取器：直接转发到 PacketByteBuf，解码方式与原代码逐字一致。
 * 本类位于版本目录，只有 1.20.4 节点会挂载它。
 */
public final class PacketByteBufReader implements NetworkReader {
    private final PacketByteBuf buf;

    public PacketByteBufReader(PacketByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public String readString() {
        return buf.readString();
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
}
