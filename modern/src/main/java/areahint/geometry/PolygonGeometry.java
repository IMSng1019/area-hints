package areahint.geometry;

import areahint.data.AreaData;

import java.util.ArrayList;
import java.util.List;

/**
 * 域名多边形的共享几何工具，渲染帧只消费预计算结果。
 */
public final class PolygonGeometry {
    private PolygonGeometry() {
    }

    /**
     * 使用耳切法把简单凹多边形剖分为三角形，返回值保存原顶点索引。
     */
    public static List<int[]> triangulate(List<AreaData.Vertex> polygon) {
        int size = polygon == null ? 0 : polygon.size();
        List<int[]> triangles = new ArrayList<>();
        if (size < 3) {
            return triangles;
        }

        double signedArea = 0.0D;
        for (int i = 0; i < size; i++) {
            AreaData.Vertex current = polygon.get(i);
            AreaData.Vertex next = polygon.get((i + 1) % size);
            signedArea += current.getX() * next.getZ() - next.getX() * current.getZ();
        }
        boolean counterClockwise = signedArea > 0.0D;

        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            remaining.add(i);
        }

        int safety = size * size;
        while (remaining.size() > 2 && safety-- > 0) {
            boolean earFound = false;
            for (int i = 0; i < remaining.size(); i++) {
                int previous = remaining.get((i - 1 + remaining.size()) % remaining.size());
                int current = remaining.get(i);
                int next = remaining.get((i + 1) % remaining.size());
                if (!isEar(polygon, remaining, previous, current, next, counterClockwise)) {
                    continue;
                }
                triangles.add(new int[]{previous, current, next});
                remaining.remove(i);
                earFound = true;
                break;
            }
            if (!earFound) {
                break;
            }
        }
        return triangles;
    }

    /**
     * 使用奇偶规则进行精确命中，边界上的点也视为位于域名内。
     */
    public static boolean contains(List<AreaData.Vertex> polygon, double x, double z) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            AreaData.Vertex first = polygon.get(j);
            AreaData.Vertex second = polygon.get(i);
            if (isPointOnSegment(x, z, first, second)) {
                return true;
            }
            boolean crosses = (second.getZ() > z) != (first.getZ() > z)
                && x < (first.getX() - second.getX()) * (z - second.getZ())
                / (first.getZ() - second.getZ()) + second.getX();
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean isEar(List<AreaData.Vertex> polygon, List<Integer> remaining,
                                 int previous, int current, int next, boolean counterClockwise) {
        AreaData.Vertex a = polygon.get(previous);
        AreaData.Vertex b = polygon.get(current);
        AreaData.Vertex c = polygon.get(next);
        double cross = (b.getX() - a.getX()) * (c.getZ() - a.getZ())
            - (b.getZ() - a.getZ()) * (c.getX() - a.getX());
        if (counterClockwise ? cross <= 0.0D : cross >= 0.0D) {
            return false;
        }
        for (int index : remaining) {
            if (index != previous && index != current && index != next
                && isPointInTriangle(polygon.get(index), a, b, c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPointInTriangle(AreaData.Vertex point, AreaData.Vertex a,
                                             AreaData.Vertex b, AreaData.Vertex c) {
        double first = sign(point, a, b);
        double second = sign(point, b, c);
        double third = sign(point, c, a);
        boolean hasNegative = first < 0.0D || second < 0.0D || third < 0.0D;
        boolean hasPositive = first > 0.0D || second > 0.0D || third > 0.0D;
        return !(hasNegative && hasPositive);
    }

    private static double sign(AreaData.Vertex first, AreaData.Vertex second, AreaData.Vertex third) {
        return (first.getX() - third.getX()) * (second.getZ() - third.getZ())
            - (second.getX() - third.getX()) * (first.getZ() - third.getZ());
    }

    private static boolean isPointOnSegment(double x, double z, AreaData.Vertex first, AreaData.Vertex second) {
        double cross = (x - first.getX()) * (second.getZ() - first.getZ())
            - (z - first.getZ()) * (second.getX() - first.getX());
        if (Math.abs(cross) > 1.0E-7D) {
            return false;
        }
        double dot = (x - first.getX()) * (second.getX() - first.getX())
            + (z - first.getZ()) * (second.getZ() - first.getZ());
        if (dot < 0.0D) {
            return false;
        }
        double lengthSquared = Math.pow(second.getX() - first.getX(), 2.0D)
            + Math.pow(second.getZ() - first.getZ(), 2.0D);
        return dot <= lengthSquared;
    }
}
