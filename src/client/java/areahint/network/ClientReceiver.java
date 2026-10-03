package areahint.network;

import net.minecraft.client.MinecraftClient;

/** 客户端接收器：只拿到解出来的字段读取器，不接触任何版本的缓冲区类型 */
@FunctionalInterface
public interface ClientReceiver {
    void handle(MinecraftClient client, NetworkReader reader);
}
