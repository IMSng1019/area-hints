package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import java.awt.geom.Area;
import java.util.List;

// 保存仍遵守原有精度和拓扑要求，智能边界只增加一道针对实际新增面积的保护。
public final class BoundarySaveGuard {
    private BoundarySaveGuard() { }

    public static List<Vertex> prepare(Area shape, Area forbidden) {
        return VizEditGeometry.prepareForSave(shape, forbidden);
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
