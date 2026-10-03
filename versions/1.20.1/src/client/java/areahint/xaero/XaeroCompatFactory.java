package areahint.xaero;

/**
 * Xaero 装配工厂（1.20.1 节点）：同名工厂每个版本目录一份，节点只挂载自己那一份。
 * 形态与 NetworkCompatFactory 一致，见 docs/plans/multiversion/12-目录契约.md 规则 5。
 */
public final class XaeroCompatFactory {
    private XaeroCompatFactory() {
    }

    public static XaeroBridgeHost create() {
        return new XaeroCompatImpl1201();
    }
}
