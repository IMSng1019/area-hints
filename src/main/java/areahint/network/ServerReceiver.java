package areahint.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/** 服务端接收器：只拿到解出来的字段读取器，不接触任何版本的缓冲区类型 */
@FunctionalInterface
public interface ServerReceiver {
    void handle(MinecraftServer server, ServerPlayerEntity player, NetworkReader reader);
}
