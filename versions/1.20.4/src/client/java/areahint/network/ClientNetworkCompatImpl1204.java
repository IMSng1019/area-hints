package areahint.network;

import areahint.Areashint;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * 1.20.1–1.20.4 代际的客户端网络实现：Identifier + PacketByteBuf 形态。
 * 复用主源集里同一版本目录的 PacketByteBufWriter / PacketByteBufReader，保证编解码完全一致。
 */
public final class ClientNetworkCompatImpl1204 implements ClientNetworkCompat {
    /** 通道名 → Identifier，命名空间沿用 Areashint.MOD_ID */
    private static Identifier id(Channel channel) {
        return new Identifier(Areashint.MOD_ID, channel.path());
    }

    @Override
    public void sendToServer(Channel channel, Consumer<NetworkWriter> payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.accept(new PacketByteBufWriter(buf));
        ClientPlayNetworking.send(id(channel), buf);
    }

    @Override
    public void registerClientReceiver(Channel channel, ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver(id(channel), (client, handler, buf, responseSender) ->
                receiver.handle(client, new PacketByteBufReader(buf)));
    }

    @Override
    public boolean canSend(Channel channel) {
        return ClientPlayNetworking.canSend(id(channel));
    }
}
