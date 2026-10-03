package areahint.network;

import net.minecraft.network.PacketByteBuf;

/**
 * 1.20.1–1.20.4 代际的写入器：直接转发到 PacketByteBuf，编码方式与原代码逐字一致。
 * 本类位于版本目录，只有 1.20.4 节点会挂载它。
 */
public final class PacketByteBufWriter implements NetworkWriter {
    private final PacketByteBuf buf;

    public PacketByteBufWriter(PacketByteBuf buf) {
        this.buf = buf;
    }

    @Override
    public NetworkWriter writeString(String value) {
        buf.writeString(value);
        return this;
    }

    @Override
    public NetworkWriter writeBoolean(boolean value) {
        buf.writeBoolean(value);
        return this;
    }

    @Override
    public NetworkWriter writeInt(int value) {
        buf.writeInt(value);
        return this;
    }

    @Override
    public NetworkWriter writeLong(long value) {
        buf.writeLong(value);
        return this;
    }

    @Override
    public NetworkWriter writeDouble(double value) {
        buf.writeDouble(value);
        return this;
    }
}
