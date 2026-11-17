package areahint.network;

/** 1.20.6 节点的客户端网络实现工厂（1.20.5+ 代际） */
public final class ClientNetworkCompatFactory {
    private ClientNetworkCompatFactory() {
    }

    /** 由 AreashintClient.onInitializeClient() 调用 */
    public static ClientNetworkCompat create() {
        return new ClientNetworkCompatImpl1206();
    }
}
