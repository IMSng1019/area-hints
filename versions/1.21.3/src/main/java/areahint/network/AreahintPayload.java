package areahint.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * 1.20.5+ 代的通用载荷：把「通道 + 已编码字节」整体塞进一个 CustomPayload。
 * 每个通道各有自己的 CustomPayload.Id（由 AreahintPayloadTypes 逐个注册），因此通道标识与原协议逐字一致；
 * 载荷内部只搬运调用方写好的字节，不做任何字段解释，保证线上字段顺序与编码方式不变。
 */
public record AreahintPayload(CustomPayload.Id<AreahintPayload> type, byte[] data) implements CustomPayload {
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return type;
    }

    /** 每通道一个 codec：解码时必须知道自己是哪个 Id，否则 getId() 对不上注册表 */
    public static PacketCodec<RegistryByteBuf, AreahintPayload> codecFor(CustomPayload.Id<AreahintPayload> id) {
        return PacketCodec.ofStatic(
                (buf, value) -> buf.writeByteArray(value.data()),
                buf -> new AreahintPayload(id, buf.readByteArray()));
    }
}
