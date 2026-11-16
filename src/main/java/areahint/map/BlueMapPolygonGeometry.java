package areahint.map;

import areahint.data.AreaData.Vertex;

import java.util.ArrayList;
import java.util.List;

/** BlueMap 各版本共用的多边形预处理，不依赖地图 API 或 Minecraft 世界实例。 */
public final class BlueMapPolygonGeometry {
    private BlueMapPolygonGeometry() {
    }

    public static Polygon prepare(List<Vertex> vertices) {
        List<Vertex> points = new ArrayList<>();
        for (Vertex vertex : vertices) {
            if (vertex != null) {
                points.add(vertex);
            }
        }

        // 闭合顶点只用于表达边界，不能重复参与 marker 位置计算。
        if (points.size() >= 2) {
            Vertex first = points.get(0);
            Vertex last = points.get(points.size() - 1);
            if (Double.compare(first.getX(), last.getX()) == 0
                    && Double.compare(first.getZ(), last.getZ()) == 0) {
                points.remove(points.size() - 1);
            }
        }
        if (points.size() < 3) {
            throw new IllegalArgumentException("有效多边形顶点不足，至少需要 3 个点");
        }

        // 中心坐标与 Shape 使用同一份有效顶点，保持开放与闭合写法的显示位置一致。
        double centerX = 0;
        double centerZ = 0;
        for (Vertex point : points) {
            centerX += point.getX();
            centerZ += point.getZ();
        }
        return new Polygon(List.copyOf(points), centerX / points.size(), centerZ / points.size());
    }

    public record Polygon(List<Vertex> vertices, double centerX, double centerZ) {
    }
}
