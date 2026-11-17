package areahint.network;

import areahint.Areashint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.util.Identifier;

/**
 * 1.20.1–1.20.4 代际的客户端网络实现：Identifier + PacketByteBuf 形态。
 * 复用主源集同一版本目录的 PacketByteBufHandle，保证编解码完全一致。
 */
public final class ClientNetworkCompatImpl1204 implements ClientNetworkCompat {
    /** 通道名 → Identifier，命名空间沿用 Areashint.MOD_ID */
    private static Identifier id(Channel channel) {
        // namespace 为 null 表示用模组自身命名空间；少数通道历史上用的是 "areahint:"
        String namespace = channel.namespace() == null ? Areashint.MOD_ID : channel.namespace();
        return new Identifier(namespace, channel.path());
    }

    @Override
    public NetworkBuf newBuffer() {
        return new PacketByteBufHandle(PacketByteBufs.create());
    }

    @Override
    public void sendToServer(Channel channel, NetworkBuf buf) {
        ClientPlayNetworking.send(id(channel), ((PacketByteBufHandle) buf).unwrap());
    }

    @Override
    public void registerClientReceiver(Channel channel, ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id(channel), (client, handler, packetBuf, responseSender) ->
                receiver.handle(client, new PacketByteBufHandle(packetBuf)));
    }

    @Override
    public boolean canSend(Channel channel) {
        return ClientPlayNetworking.canSend(id(channel));
    }
}
