package areahint.handshake;

import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.concurrent.CompletableFuture;

public final class ClientModHandshake {
    private ClientModHandshake() { }

    public static void register() {
        ClientLoginNetworking.registerGlobalReceiver(ModHandshake.CHANNEL,
                (client, handler, query, listener) -> {
                    try {
                        int protocol = query.readVarInt();
                        query.readString(ModHandshake.MAX_VERSION_LENGTH);
                        if (protocol != ModHandshake.PROTOCOL_VERSION || query.isReadable()) {
                            return CompletableFuture.completedFuture(null);
                        }
                    } catch (RuntimeException malformed) {
                        return CompletableFuture.completedFuture(null);
                    }
                    PacketByteBuf response = PacketByteBufs.create();
                    response.writeVarInt(ModHandshake.PROTOCOL_VERSION);
                    response.writeString(ModHandshake.localVersion(), ModHandshake.MAX_VERSION_LENGTH);
                    // 仅报告真实模组版本，是否允许登录始终由服务端判断。
                    return CompletableFuture.completedFuture(response);
                });
    }
}
