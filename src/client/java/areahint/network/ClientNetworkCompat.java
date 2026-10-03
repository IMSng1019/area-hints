package areahint.network;

import java.util.function.Consumer;

/**
 * 客户端网络能力统一入口：发送、注册接收器、能否发送。
 * 这三件事全都依赖客户端专属 API，所以只能放在 client 源集，不能进 src/main。
 */
public interface ClientNetworkCompat {
    /** 客户端向服务端发送：字段由调用方按既有顺序写入 */
    void sendToServer(Channel channel, Consumer<NetworkWriter> payload);

    /** 注册客户端接收器 */
    void registerClientReceiver(Channel channel, ClientReceiver receiver);

    /** 该通道现在能否发送（等价于旧的 canSend 语义） */
    boolean canSend(Channel channel);

    /** 由客户端入口在初始化时注入实现：全仓库唯一一处交接点 */
    static void install(ClientNetworkCompat impl) {
        Holder.instance = impl;
    }

    /** 取当前代际的实现 */
    static ClientNetworkCompat get() {
        ClientNetworkCompat impl = Holder.instance;
        if (impl == null) {
            throw new IllegalStateException("ClientNetworkCompat 尚未安装：请检查 AreashintClient.onInitializeClient() 里的 install 调用");
        }
        return impl;
    }

    /** 静态持有者 */
    final class Holder {
        private Holder() {
        }

        static ClientNetworkCompat instance;
    }
}
