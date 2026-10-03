package areahint.network;

/**
 * 1.20.4 节点的网络实现工厂：机制 ② 的交接点。
 * 每个版本目录各放一份同名工厂，编译期每个节点只看得见自己那一份，因此同名也不会冲突。
 */
public final class NetworkCompatFactory {
    private NetworkCompatFactory() {
    }

    /** 由 Areashint.onInitialize() 调用 */
    public static NetworkCompat create() {
        return new NetworkCompatImpl1204();
    }
}
