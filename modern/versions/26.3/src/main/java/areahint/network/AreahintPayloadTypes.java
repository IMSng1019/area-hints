package areahint.network;

import areahint.Areashint;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 1.20.5+ 代的 PayloadType 注册表：为 Channel.ALL 里的每个通道注册一个 payload 类型。
 * 注册动作在 1.20.4 上不存在，所以这一层只能由版本侧提供（共享 src/ 里不出现任何注册代码）。
 */
public final class AreahintPayloadTypes {
    private static final Map<Channel, CustomPacketPayload.Type<AreahintPayload>> IDS = new HashMap<>();
    private static boolean registered = false;

    private AreahintPayloadTypes() {
    }

    /** 幂等注册：客户端会同时装服务端与客户端实现，重复调用直接返回 */
    public static synchronized void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        for (Channel channel : Channel.ALL) {
            CustomPacketPayload.Type<AreahintPayload> id = new CustomPacketPayload.Type<>(identifierOf(channel));
            IDS.put(channel, id);
            StreamCodec<RegistryFriendlyByteBuf, AreahintPayload> codec = AreahintPayload.codecFor(id);
            PayloadTypeRegistry.serverboundPlay().register(id, codec);
            PayloadTypeRegistry.clientboundPlay().register(id, codec);
        }
    }

    /** 取通道对应的 Id；没注册过就是编程错误，直接抛出而不是静默丢弃 */
    public static CustomPacketPayload.Type<AreahintPayload> idOf(Channel channel) {
        CustomPacketPayload.Type<AreahintPayload> id = IDS.get(channel);
        if (id == null) {
            throw new IllegalStateException("通道未注册：" + channel);
        }
        return id;
    }

    /** 通道名 → Identifier，命名空间规则与 1.20.4 实现保持一致 */
    public static Identifier identifierOf(Channel channel) {
        String namespace = channel.namespace() == null ? Areashint.MOD_ID : channel.namespace();
        // 1.21 起 Identifier 构造器私有化，必须用 Identifier.of
        return Identifier.fromNamespaceAndPath(namespace, channel.path());
    }

    /** 把收到的字节还原成版本中立的缓冲区句柄 */
    public static NetworkBuf reader(AreahintPayload payload) {
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeBytes(payload.data());
        return new PacketByteBufHandle(buf);
    }

    /** 取出句柄里的字节 */
    public static byte[] bytes(NetworkBuf buf) {
        FriendlyByteBuf packet = ((PacketByteBufHandle) buf).unwrap();
        byte[] data = new byte[packet.readableBytes()];
        packet.readBytes(data);
        return data;
    }
}
