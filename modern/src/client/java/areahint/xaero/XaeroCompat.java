package areahint.xaero;

/**
 * Xaero 可选兼容入口：只保留「装配」这一个静态方法，桥接实现由各版本目录提供。
 * 每个版本目录各有一份同名工厂 XaeroCompatFactory，节点只挂载自己那一份。
 */
public final class XaeroCompat {
    private XaeroCompat() {
    }

    /** 装配本节点支持的 Xaero 联动；依赖是否存在、版本是否匹配都在实现里判定 */
    public static void initialize() {
        XaeroCompatFactory.create().initialize();
    }
}
