package areahint.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

/** 1.20.5+ 代的客户端网络实现：CustomPayload 形态，与主源集实现共用 PayloadType 注册表 */
public final class ClientNetworkCompatImpl1206 implements ClientNetworkCompat {
    @Override
    public NetworkBuf newBuffer() {
        return new PacketByteBufHandle(PacketByteBufs.create());
    }

    @Override
    public void sendToServer(Channel channel, NetworkBuf buf) {
        AreahintPayloadTypes.ensureRegistered();
        ClientPlayNetworking.send(new AreahintPayload(AreahintPayloadTypes.idOf(channel), AreahintPayloadTypes.bytes(buf)));
    }

    @Override
    public void registerClientReceiver(Channel channel, ClientReceiver receiver) {
        AreahintPayloadTypes.ensureRegistered();
        ClientPlayNetworking.registerGlobalReceiver(AreahintPayloadTypes.idOf(channel), (payload, context) ->
                receiver.handle(context.client(), AreahintPayloadTypes.reader(payload)));
    }

    @Override
    public boolean canSend(Channel channel) {
        AreahintPayloadTypes.ensureRegistered();
        return ClientPlayNetworking.canSend(AreahintPayloadTypes.idOf(channel));
    }
}
