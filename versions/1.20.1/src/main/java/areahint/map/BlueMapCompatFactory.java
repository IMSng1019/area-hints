package areahint.map;

/**
 * BlueMap 集成工厂（1.20.1 节点）：同名工厂每个版本目录一份，节点只挂载自己那一份。
 * 形态与 NetworkCompatFactory 一致，见 docs/plans/multiversion/12-目录契约.md 规则 5。
 */
public final class BlueMapCompatFactory {
    private BlueMapCompatFactory() {
    }

    /** 返回本节点的集成实现；调用方必须先用 FabricLoader 判断 BlueMap 是否安装 */
    public static BlueMapBridge create() {
        return new BlueMapIntegration();
    }
}
