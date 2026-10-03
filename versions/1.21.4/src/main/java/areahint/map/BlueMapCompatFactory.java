package areahint.map;

// 阶段 6 第三方隔离：与 1.21.1 同代际（Xaero 25.3.2 / 世界地图 1.39.x，BlueMap 2.7.8），逐字复制。
// 复制理由：机制 ② 要求每个活跃节点各挂一份实现；本文件与 1.21.1 版本目录的对应文件保持一致。
/**
 * BlueMap 集成工厂（1.21.1 节点）：同名工厂每个版本目录一份，节点只挂载自己那一份。
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
