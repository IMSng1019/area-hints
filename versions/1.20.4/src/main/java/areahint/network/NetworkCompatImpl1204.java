package areahint.network;

import areahint.Areashint;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 1.20.1–1.20.4 代际的服务端网络实现：Identifier + PacketByteBuf 形态。
 * 只做「建缓冲区 → 交给调用方写字段 → 发送」三件事，不做任何编解码，保证线上字节与改造前一致。
 */
public final class NetworkCompatImpl1204 implements NetworkCompat {
    /** 通道名 → Identifier，命名空间沿用 Areashint.MOD_ID */
    private static Identifier id(Channel channel) {
        return new Identifier(Areashint.MOD_ID, channel.path());
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, Channel channel, Consumer<NetworkWriter> payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.accept(new PacketByteBufWriter(buf));
        ServerPlayNetworking.send(player, id(channel), buf);
    }

    @Override
    public void registerServerReceiver(Channel channel, ServerReceiver receiver) {
        ServerPlayNetworking.registerGlobalReceiver(id(channel), (server, player, handler, buf, responseSender) ->
                receiver.handle(server, player, new PacketByteBufReader(buf)));
    }
}
