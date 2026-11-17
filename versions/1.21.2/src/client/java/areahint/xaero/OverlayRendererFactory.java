package areahint.xaero;

/** 1.21.2/1.21.3 的覆盖层工厂，每个构建节点只装配自己版本目录内的实现。 */
public final class OverlayRendererFactory {
    private OverlayRendererFactory() {
    }

    public static OverlayBackend create() {
        return new OverlayRenderer1214();
    }
}
