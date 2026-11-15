package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 改编自 Province core/FenceFaces.java（MIT）；交点切边后按有向半边找面，不替开放路径添加闭合边。
final class FenceFaces {
    private static final int MAX_SEGMENTS = 4096;
    private static final int MAX_SPLIT_SEGMENTS = 16_384;
    private FenceFaces() { }

    // 精确节点身份不使用参考实现的近邻吸附，真正存在的小缺口必须保持开放。
    private record Point(double x, double z) {
        Point { if (x == 0) x = 0; if (z == 0) z = 0; }
        Vertex vertex() { return new Vertex(x, z); }
    }
    private record Edge(Point from, Point to) { }
    private record Cut(double position, Point point) { }

    static Area faceAt(List<double[]> source, double x, double z) {
        if (source == null || source.size() < 3) return null;
        if (source.size() > MAX_SEGMENTS) throw VizEditGeometry.error("geometry_limit");
        List<Edge> segments = new ArrayList<>();
        for (double[] edge : source) {
            if (edge == null || edge.length != 4) throw VizEditGeometry.error("geometry_coordinates");
            Point a = new Point(edge[0], edge[1]), b = new Point(edge[2], edge[3]);
            VizEditGeometry.checkCoordinate(a.vertex()); VizEditGeometry.checkCoordinate(b.vertex());
            if (!a.equals(b)) segments.add(new Edge(a, b));
        }
        List<Edge> edges = split(segments);
        Map<Point, Integer> ids = new HashMap<>();
        List<Point> nodes = new ArrayList<>();
        List<int[]> arcs = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Edge edge : edges) {
            int a = node(ids, nodes, edge.from), b = node(ids, nodes, edge.to);
            if (a == b) continue;
            long key = ((long) Math.min(a, b) << 32) | (Math.max(a, b) & 0xffffffffL);
            if (seen.add(key)) arcs.add(new int[]{a, b});
        }
        return smallestFace(nodes, arcs, x, z);
    }

    private static List<Edge> split(List<Edge> edges) {
        List<List<Cut>> cuts = new ArrayList<>();
        for (Edge edge : edges) cuts.add(new ArrayList<>(List.of(new Cut(0, edge.from), new Cut(1, edge.to))));
        int count = edges.size() * 2;
        for (int i = 0; i < edges.size(); i++) {
            Edge a = edges.get(i);
            for (int j = i + 1; j < edges.size(); j++) {
                Edge b = edges.get(j);
                if (!boxesIntersect(a, b)) continue;
                double ax = a.to.x - a.from.x, az = a.to.z - a.from.z;
                double bx = b.to.x - b.from.x, bz = b.to.z - b.from.z;
                double denominator = ax * bz - az * bx;
                if (denominator == 0) {
                    // 重叠共线边也在已有端点处切开，否则相同围栏被重复画过后会丢失面。
                    count += endpointCut(a, b.from, cuts.get(i)) + endpointCut(a, b.to, cuts.get(i));
                    count += endpointCut(b, a.from, cuts.get(j)) + endpointCut(b, a.to, cuts.get(j));
                } else {
                    double dx = b.from.x - a.from.x, dz = b.from.z - a.from.z;
                    double t = (dx * bz - dz * bx) / denominator, u = (dx * az - dz * ax) / denominator;
                    if (t < 0 || t > 1 || u < 0 || u > 1) continue;
                    Point hit = t == 0 ? a.from : t == 1 ? a.to : u == 0 ? b.from : u == 1 ? b.to
                        : new Point(a.from.x + ax * t, a.from.z + az * t);
                    if (t > 0 && t < 1) { cuts.get(i).add(new Cut(t, hit)); count++; }
                    if (u > 0 && u < 1) { cuts.get(j).add(new Cut(u, hit)); count++; }
                }
                if (count > MAX_SPLIT_SEGMENTS * 2) throw VizEditGeometry.error("geometry_limit");
            }
        }
        List<Edge> result = new ArrayList<>();
        for (List<Cut> list : cuts) {
            list.sort(Comparator.comparingDouble(Cut::position));
            for (int i = 0; i + 1 < list.size(); i++) {
                Point a = list.get(i).point, b = list.get(i + 1).point;
                if (!a.equals(b)) result.add(new Edge(a, b));
                if (result.size() > MAX_SPLIT_SEGMENTS) throw VizEditGeometry.error("geometry_limit");
            }
        }
        return result;
    }

    private static boolean boxesIntersect(Edge a, Edge b) {
        return Math.max(a.from.x, a.to.x) >= Math.min(b.from.x, b.to.x)
            && Math.max(b.from.x, b.to.x) >= Math.min(a.from.x, a.to.x)
            && Math.max(a.from.z, a.to.z) >= Math.min(b.from.z, b.to.z)
            && Math.max(b.from.z, b.to.z) >= Math.min(a.from.z, a.to.z);
    }

    private static int endpointCut(Edge edge, Point point, List<Cut> cuts) {
        double dx = edge.to.x - edge.from.x, dz = edge.to.z - edge.from.z;
        if ((point.x - edge.from.x) * dz != (point.z - edge.from.z) * dx) return 0;
        double t = Math.abs(dx) >= Math.abs(dz) ? (point.x - edge.from.x) / dx : (point.z - edge.from.z) / dz;
        if (t <= 0 || t >= 1) return 0;
        cuts.add(new Cut(t, point));
        return 1;
    }

    private static int node(Map<Point, Integer> ids, List<Point> nodes, Point point) {
        Integer existing = ids.get(point);
        if (existing != null) return existing;
        int id = nodes.size(); nodes.add(point); ids.put(point, id); return id;
    }

    private static Area smallestFace(List<Point> nodes, List<int[]> arcs, double x, double z) {
        List<List<Integer>> outgoing = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) outgoing.add(new ArrayList<>());
        List<int[]> directed = new ArrayList<>();
        for (int[] arc : arcs) {
            outgoing.get(arc[0]).add(directed.size()); directed.add(new int[]{arc[0], arc[1]});
            outgoing.get(arc[1]).add(directed.size()); directed.add(new int[]{arc[1], arc[0]});
        }
        // 每个节点只排序一次，下一半边取反向边顺时针的邻边，避免高连接度节点造成平方开销。
        for (List<Integer> list : outgoing) list.sort(Comparator.comparingDouble(index -> {
            int[] arc = directed.get(index); Point from = nodes.get(arc[0]), to = nodes.get(arc[1]);
            return Math.atan2(to.z - from.z, to.x - from.x);
        }));
        int[] next = new int[directed.size()];
        for (List<Integer> list : outgoing) {
            for (int i = 0; i < list.size(); i++) next[list.get(i) ^ 1] = list.get((i + 1) % list.size());
        }
        boolean[] used = new boolean[directed.size()];
        List<Area> bounded = new ArrayList<>();
        Area best = null;
        double bestMeasure = Double.POSITIVE_INFINITY;
        for (int start = 0; start < directed.size(); start++) {
            if (used[start]) continue;
            List<Vertex> loop = new ArrayList<>();
            int current = start;
            while (!used[current]) {
                used[current] = true; loop.add(nodes.get(directed.get(current)[0]).vertex()); current = next[current];
            }
            if (current != start || loop.size() < 3) continue;
            double signed = VizEditGeometry.signedArea(loop);
            // 顺时针轮廓表示有界面；无界外侧环和往返的开放边都不得作为魔棒选区。
            if (signed >= 0) continue;
            Area shape = VizEditGeometry.fromVertices(loop);
            if (shape.isEmpty()) continue;
            bounded.add(shape);
            double measure = -signed;
            if (shape.contains(x, z) && measure < bestMeasure) { best = shape; bestMeasure = measure; }
        }
        if (best == null) return null;
        Area result = new Area(best);
        // 嵌套但不相接的围栏保留内环，保存时仍须通过无洞校验，不能越过内侧实际边界。
        for (Area inner : bounded) {
            if (inner == best || inner.contains(x, z)) continue;
            Area outside = new Area(inner); outside.subtract(best);
            if (outside.isEmpty()) result.subtract(inner);
        }
        return result;
    }
}
