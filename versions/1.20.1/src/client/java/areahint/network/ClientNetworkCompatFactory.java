package areahint.network;

/**
 * 1.20.4 节点的客户端网络实现工厂：机制 ② 的交接点。
 * 与主源集的 NetworkCompatFactory 同名不同源集，互不影响。
 */
public final class ClientNetworkCompatFactory {
    private ClientNetworkCompatFactory() {
    }

    /** 由 AreashintClient.onInitializeClient() 调用 */
    public static ClientNetworkCompat create() {
        return new ClientNetworkCompatImpl1204();
    }
}
