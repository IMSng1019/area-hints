package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// 保存或简化失败后只定位现有几何中的问题，不修补边界、不修改草稿或猜测不存在的错误坐标。
public final class GeometryIssueLocator {
    private GeometryIssueLocator() { }

    // 界面可以调整自己的取景框，但不能改动定位结果中保存的原始边界范围。
    public record Issue(Rectangle2D.Double bounds, double x, double z) {
        public Issue { bounds = (Rectangle2D.Double) bounds.clone(); }
        @Override public Rectangle2D.Double bounds() { return (Rectangle2D.Double) bounds.clone(); }
    }

    public static Issue locate(Area shape, String errorKey) {
        if (shape == null || errorKey == null) return null;
        if (!errorKey.equals("vizedit.error.geometry_holes") && !errorKey.equals("vizedit.error.geometry_disconnected")
                && !errorKey.equals("vizedit.error.geometry_self_intersection")) return null;
        List<List<Vertex>> contours;
        try { contours = VizEditGeometry.contours(shape); }
        catch (VizEditGeometry.GeometryException ignored) { return null; }
        if (errorKey.equals("vizedit.error.geometry_self_intersection")) return intersection(contours);
        List<Ring> rings = new ArrayList<>();
        for (List<Vertex> contour : contours) {
            List<Vertex> points = VizEditGeometry.simplify(contour);
            if (points.size() < 3) continue;
            // 与保存校验使用同一份原轮廓判号，面积相消但非退化的环保留为未分类问题，不能被重新判成洞或丢弃。
            double area = VizEditGeometry.signedArea(contour);
            int direction = area == 0 ? 0 : area > 0 ? 1 : -1;
            rings.add(new Ring(points, direction, Math.abs(area)));
        }
        // 面积仅用于稳定选择最大的主区域和问题区域，真实小洞或小岛没有任何最小面积门槛。
        if (errorKey.equals("vizedit.error.geometry_holes")) {
            Ring hole = largest(rings, 1, null);
            return hole == null ? null : region(hole.points);
        }
        // 保存校验优先报告真实洞，单纯有洞不能因为调用方传入不连通键就被标成额外岛屿。
        if (rings.size() < 2 || largest(rings, 1, null) != null) return null;
        Ring mainland = largest(rings, -1, null);
        if (mainland == null) mainland = largest(rings, 0, null);
        Ring island = largest(rings, -1, mainland);
        if (island == null) island = largest(rings, 0, mainland);
        return island == null ? null : region(island.points);
    }

    private record Ring(List<Vertex> points, int direction, double area) { }

    private static Ring largest(List<Ring> rings, int direction, Ring excluded) {
        Ring result = null;
        for (Ring ring : rings) if (ring != excluded && ring.direction == direction
                && (result == null || ring.area > result.area)) result = ring;
        return result;
    }

    private static Issue region(List<Vertex> points) {
        double minX = Double.POSITIVE_INFINITY, minZ = minX, maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;
        for (Vertex point : points) {
            minX = Math.min(minX, point.getX()); maxX = Math.max(maxX, point.getX());
            minZ = Math.min(minZ, point.getZ()); maxZ = Math.max(maxZ, point.getZ());
        }
        double x = minX + (maxX - minX) / 2, z = minZ + (maxZ - minZ) / 2;
        // 凹形轮廓的包围盒中心可能落在外部，此时将标记放在真实顶点上，取景范围仍保留整个问题轮廓。
        if (points.size() >= 3 && !VizEditGeometry.fromVertices(points).contains(x, z)) {
            x = points.get(0).getX(); z = points.get(0).getZ();
        }
        return new Issue(new Rectangle2D.Double(minX, minZ, maxX - minX, maxZ - minZ), x, z);
    }

    private static Issue point(double x, double z) { return new Issue(new Rectangle2D.Double(x, z, 0, 0), x, z); }

    private static Issue intersection(List<List<Vertex>> contours) {
        List<Edge> edges = new ArrayList<>();
        for (int ringId = 0; ringId < contours.size(); ringId++) {
            List<Vertex> ring = VizEditGeometry.simplify(contours.get(ringId));
            if (ring.size() < 3) continue;
            for (int i = 0; i < ring.size(); i++) {
                Vertex a = ring.get(i), b = ring.get((i + 1) % ring.size()), c = ring.get((i + 2) % ring.size());
                // 正常共线中点已经删除，剩余相邻共线边是折返，定位到其实际重叠部分。
                if (VizEditGeometry.orientation(a, b, c) == 0) return overlap(a, b, b, c);
                edges.add(new Edge(a, b, edges.size(), ringId, i, ring.size()));
            }
        }
        if (edges.isEmpty()) return null;
        EdgeTree tree = new EdgeTree(edges.toArray(Edge[]::new), 0, edges.size());
        for (Edge edge : edges) {
            Issue issue = tree.find(edge);
            if (issue != null) return issue;
        }
        return null;
    }

    private static boolean onSegment(Vertex point, Vertex a, Vertex b) {
        return point.getX() >= Math.min(a.getX(), b.getX()) && point.getX() <= Math.max(a.getX(), b.getX())
            && point.getZ() >= Math.min(a.getZ(), b.getZ()) && point.getZ() <= Math.max(a.getZ(), b.getZ())
            && VizEditGeometry.orientation(a, b, point) == 0;
    }

    private static Issue overlap(Vertex a, Vertex b, Vertex c, Vertex d) {
        List<Vertex> shared = new ArrayList<>(4);
        if (onSegment(a, c, d)) shared.add(a);
        if (onSegment(b, c, d)) shared.add(b);
        if (onSegment(c, a, b)) shared.add(c);
        if (onSegment(d, a, b)) shared.add(d);
        return shared.isEmpty() ? null : region(shared);
    }

    private static Issue crossing(Edge first, Edge second) {
        Vertex a = first.a, b = first.b, c = second.a, d = second.b;
        Issue shared = overlap(a, b, c, d);
        if (shared != null) return shared;
        // 只对已确定相交的非共线边计算交点，十进制除法防止近乎平行时的叉积相消。
        BigDecimal ax = decimal(a.getX()), az = decimal(a.getZ());
        BigDecimal rx = decimal(b.getX()).subtract(ax), rz = decimal(b.getZ()).subtract(az);
        BigDecimal sx = decimal(d.getX()).subtract(decimal(c.getX())), sz = decimal(d.getZ()).subtract(decimal(c.getZ()));
        BigDecimal denominator = rx.multiply(sz).subtract(rz.multiply(sx));
        if (denominator.signum() == 0) return null;
        BigDecimal t = decimal(c.getX()).subtract(ax).multiply(sz)
            .subtract(decimal(c.getZ()).subtract(az).multiply(sx)).divide(denominator, MathContext.DECIMAL128);
        return point(ax.add(rx.multiply(t)).doubleValue(), az.add(rz.multiply(t)).doubleValue());
    }

    private static BigDecimal decimal(double value) { return BigDecimal.valueOf(value); }

    private static final class Edge {
        final Vertex a, b;
        final int id, ringId, index, count;
        final double minX, minZ, maxX, maxZ;
        Edge(Vertex a, Vertex b, int id, int ringId, int index, int count) {
            this.a = a; this.b = b; this.id = id; this.ringId = ringId; this.index = index; this.count = count;
            minX = Math.min(a.getX(), b.getX()); maxX = Math.max(a.getX(), b.getX());
            minZ = Math.min(a.getZ(), b.getZ()); maxZ = Math.max(a.getZ(), b.getZ());
        }
        boolean adjacent(Edge other) {
            int difference = Math.abs(index - other.index);
            return ringId == other.ringId && (difference == 1 || difference == count - 1);
        }
    }

    // 错误定位也使用包围盒树筛选边对，大轮廓不会无条件执行全部顶点的两两比较。
    private static final class EdgeTree {
        final Edge[] edges;
        final int from, to;
        final double minX, minZ, maxX, maxZ;
        final EdgeTree left, right;

        EdgeTree(Edge[] edges, int from, int to) {
            this.edges = edges; this.from = from; this.to = to;
            double x0 = Double.POSITIVE_INFINITY, z0 = x0, x1 = Double.NEGATIVE_INFINITY, z1 = x1;
            for (int i = from; i < to; i++) {
                x0 = Math.min(x0, edges[i].minX); x1 = Math.max(x1, edges[i].maxX);
                z0 = Math.min(z0, edges[i].minZ); z1 = Math.max(z1, edges[i].maxZ);
            }
            minX = x0; minZ = z0; maxX = x1; maxZ = z1;
            if (to - from <= 8) { left = right = null; return; }
            boolean horizontal = maxX - minX >= maxZ - minZ;
            Arrays.sort(edges, from, to, Comparator.comparingDouble(edge -> horizontal ? edge.minX + edge.maxX : edge.minZ + edge.maxZ));
            int middle = (from + to) / 2;
            left = new EdgeTree(edges, from, middle); right = new EdgeTree(edges, middle, to);
        }

        Issue find(Edge edge) {
            if (edge.maxX < minX || edge.minX > maxX || edge.maxZ < minZ || edge.minZ > maxZ) return null;
            if (left != null) {
                Issue result = left.find(edge);
                return result != null ? result : right.find(edge);
            }
            for (int i = from; i < to; i++) {
                Edge other = edges[i];
                if (other.id <= edge.id || edge.adjacent(other) || edge.maxX < other.minX || edge.minX > other.maxX
                        || edge.maxZ < other.minZ || edge.minZ > other.maxZ) continue;
                if (VizEditGeometry.intersects(edge.a, edge.b, other.a, other.b)) {
                    Issue issue = crossing(edge, other);
                    if (issue != null) return issue;
                }
            }
            return null;
        }
    }
}
