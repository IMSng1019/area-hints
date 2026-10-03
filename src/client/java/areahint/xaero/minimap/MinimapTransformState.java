package areahint.xaero.minimap;

/**
 * 小地图画布变换快照：由版本侧的桥接实现从 Xaero 内部状态取出，供共享侧几何计算使用。
 * 本类刻意不引用任何 xaero 类型，因此留在 src/。
 */
public record MinimapTransformState(double ps, double pc, double zoom, int halfViewW, int halfViewH, boolean circle) {
    /** 无效快照：缩放或视口为 0 时使用 */
    static final MinimapTransformState EMPTY = new MinimapTransformState(0.0D, 1.0D, 0.0D, 0, 0, false);

    /** 缩放与视口是否有效 */
    public boolean valid() {
        return Double.isFinite(zoom) && Math.abs(zoom) > 1.0E-6D && halfViewW > 0 && halfViewH > 0;
    }

    /** 屏幕视口对应的世界半径 */
    public double worldRadius() {
        if (!valid()) {
            return 0.0D;
        }
        double screenRadius = circle ? halfViewW : Math.hypot(halfViewW, halfViewH);
        return screenRadius / Math.abs(zoom) + 2.0D;
    }
}
