package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

// 橡皮只栅格化新增线段，整数笔迹累计后再从落笔时的域名扣除，避免反复裁剪小数斜边。
public final class EraserStroke {
    private final Area original;
    private final List<Vertex> points = new ArrayList<>();
    private Area erased = new Area();
    private int width;
    private Area preview;
    private VizEditGeometry.GeometryException failure;

    public EraserStroke(Area original) {
        this.original = original;
    }

    public Area sample(Vertex point, int currentWidth) {
        Vertex previous = points.isEmpty() ? null : points.get(points.size() - 1);
        boolean accepted = previous == null
            || Math.abs(point.getX() - previous.getX()) + Math.abs(point.getZ() - previous.getZ()) >= 0.5;
        if (accepted) points.add(point);
        try {
            if (previous == null || width != currentWidth) {
                // 改宽时整笔重算；保留越界后采集的点，调窄到有效范围即可恢复。
                width = currentWidth;
                erased = VizEditGeometry.stroke(points, width);
                failure = null;
            } else {
                if (failure != null) throw failure;
                if (!accepted || (Math.floor(point.getX()) == Math.floor(previous.getX())
                        && Math.floor(point.getZ()) == Math.floor(previous.getZ()))) return preview;
                erased.add(VizEditGeometry.stroke(List.of(previous, point), width));
            }
            preview = VizEditGeometry.subtract(original, erased);
            return preview;
        } catch (VizEditGeometry.GeometryException e) {
            failure = e;
            throw e;
        }
    }
}
