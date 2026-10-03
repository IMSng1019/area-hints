package areahint.network;

import java.util.function.Consumer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 服务端网络能力统一入口：只描述「发什么、谁收」，不描述怎么编解码。
 * 实现由各版本目录里的 NetworkCompatFactory 提供，编译期每个节点只挂载自己那一份。
 * 说明：签名里的 ServerPlayerEntity 是 1.20.1–1.21.11 全程稳定的类型，属于刻意的例外；
 * 缓冲区类型（PacketByteBuf / CustomPayload）绝不进入本接口。
 */
public interface NetworkCompat {
    /** 服务端向单个玩家发送：字段由调用方按既有顺序写入 */
    void sendToPlayer(ServerPlayerEntity player, Channel channel, Consumer<NetworkWriter> payload);

    /** 注册服务端接收器 */
    void registerServerReceiver(Channel channel, ServerReceiver receiver);

    /** 由模组入口在初始化时注入实现：全仓库唯一一处交接点 */
    static void install(NetworkCompat impl) {
        Holder.instance = impl;
    }

    /** 取当前代际的实现 */
    static NetworkCompat get() {
        NetworkCompat impl = Holder.instance;
        if (impl == null) {
            throw new IllegalStateException("NetworkCompat 尚未安装：请检查 Areashint.onInitialize() 里的 install 调用");
        }
        return impl;
    }

    /** 静态持有者：接口本身不能有可变静态字段，用嵌套类承载 */
    final class Holder {
        private Holder() {
        }

        static NetworkCompat instance;
    }
}
