package areahint.network;

import areahint.Areashint;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 1.20.1–1.20.4 代际的服务端网络实现：Identifier + PacketByteBuf 形态。
 * 只做「建缓冲区 / 发送 / 注册」三件事，不碰字段编解码，保证线上字节与改造前一致。
 */
public final class NetworkCompatImpl1204 implements NetworkCompat {
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
    public void sendToPlayer(ServerPlayerEntity player, Channel channel, NetworkBuf buf) {
        ServerPlayNetworking.send(player, id(channel), ((PacketByteBufHandle) buf).unwrap());
    }

    @Override
    public void registerServerReceiver(Channel channel, ServerReceiver receiver) {
        ServerPlayNetworking.registerGlobalReceiver(id(channel), (server, player, handler, packetBuf, responseSender) ->
                receiver.handle(server, player, new PacketByteBufHandle(packetBuf)));
    }
}
