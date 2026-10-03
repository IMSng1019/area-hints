package areahint.network;

/** 1.20.6 节点的网络实现工厂（1.20.5+ 代际：CustomPayload 形态） */
public final class NetworkCompatFactory {
    private NetworkCompatFactory() {
    }

    /** 由 Areashint.onInitialize() 调用 */
    public static NetworkCompat create() {
        return new NetworkCompatImpl1206();
    }
}
