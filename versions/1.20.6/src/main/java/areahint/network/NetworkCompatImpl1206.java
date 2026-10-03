package areahint.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 1.20.5+ 代的服务端网络实现：CustomPayload + PayloadTypeRegistry 形态。
 * 每个通道一个 payload 类型（通道 id 与原协议逐字一致），载荷只搬运调用方写好的字节。
 */
public final class NetworkCompatImpl1206 implements NetworkCompat {
    @Override
    public NetworkBuf newBuffer() {
        return new PacketByteBufHandle(PacketByteBufs.create());
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, Channel channel, NetworkBuf buf) {
        AreahintPayloadTypes.ensureRegistered();
        ServerPlayNetworking.send(player, new AreahintPayload(AreahintPayloadTypes.idOf(channel), AreahintPayloadTypes.bytes(buf)));
    }

    @Override
    public void registerServerReceiver(Channel channel, ServerReceiver receiver) {
        AreahintPayloadTypes.ensureRegistered();
        ServerPlayNetworking.registerGlobalReceiver(AreahintPayloadTypes.idOf(channel), (payload, context) ->
                receiver.handle(context.server(), context.player(), AreahintPayloadTypes.reader(payload)));
    }
}
