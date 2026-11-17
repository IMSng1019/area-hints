package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import java.awt.geom.Area;
import java.util.List;

// 主动保存只检查原有精度和拓扑要求，边界笔禁区仅保护绘制过程中的自动优化。
public final class BoundarySaveGuard {
    private BoundarySaveGuard() { }

    public static List<Vertex> prepare(Area shape, Area ignoredForbidden) {
        // 保留原入口参数以兼容草稿调用，主动保存不再因量化后的边界越过辅助限制而拒绝。
        return VizEditGeometry.prepareForSave(shape);
    }

    public static boolean allows(Area candidate, Area before, Area forbidden) {
        if (forbidden == null || forbidden.isEmpty()) return true;
        // 之后使用普通工具明确画入的面积是新的用户意图，不把它误当作优化产生的越界。
        Area added = new Area(candidate);
        if (before != null) added.subtract(before);
        added.intersect(forbidden);
        return added.isEmpty();
    }
}
