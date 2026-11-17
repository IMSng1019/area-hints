package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 新版 JDK 会按扫描工作量把同一 Area 切成贴边子环，提取时精确消除人工水平接缝再恢复实际轮廓。
final class AreaContours {
    private AreaContours() { }

    private record Point(double x, double z) {
        Point { if (x == 0) x = 0; if (z == 0) z = 0; }
        Vertex vertex() { return new Vertex(x, z); }
    }
    private record Edge(Point from, Point to) { }
    private record Event(double x, int forward, int backward) { }

    static List<List<Vertex>> read(Area shape) {
        List<List<Vertex>> rings = new ArrayList<>();
        if (shape == null) return rings;
        PathIterator iterator = shape.getPathIterator(null);
        double[] xy = new double[6];
        List<Vertex> ring = null;
        while (!iterator.isDone()) {
            int kind = iterator.currentSegment(xy);
            if (kind == PathIterator.SEG_MOVETO) {
                ring = new ArrayList<>();
                rings.add(ring);
                ring.add(new Vertex(xy[0], xy[1]));
            } else if (kind == PathIterator.SEG_LINETO && ring != null) {
                ring.add(new Vertex(xy[0], xy[1]));
            } else if (kind != PathIterator.SEG_CLOSE) {
                throw VizEditGeometry.error("geometry_self_intersection");
            }
            iterator.next();
        }
        // 布尔运算可能留下精确退化的往返线，仅移除无损简化后不足三个点的路径，不按面积或容差删环。
        rings.removeIf(AreaContours::degenerate);
        if (rings.size() < 2) return rings;
        List<List<Vertex>> joined = joinSeams(rings);
        if (joined != rings) joined.removeIf(AreaContours::degenerate);
        return joined;
    }

    private static boolean degenerate(List<Vertex> ring) {
        if (ring.size() < 3) return true;
        Vertex first = ring.get(0), second = null;
        // 正常轮廓发现一个非共线点即可保留，避免每帧为整个大外环分配简化顶点副本。
        for (Vertex point : ring) {
            if (second == null) {
                if (point.getX() != first.getX() || point.getZ() != first.getZ()) second = point;
            } else if (VizEditGeometry.orientation(first, second, point) != 0) return false;
        }
        // 全共线仍沿用原严格简化条件，不额外删除简化后保留三个点以上的复杂折返路径。
        return VizEditGeometry.simplify(ring).size() < 3;
    }

    private static List<List<Vertex>> joinSeams(List<List<Vertex>> rings) {
        List<Edge> edges = new ArrayList<>();
        Map<Double, List<Edge>> horizontal = new HashMap<>();
        for (List<Vertex> ring : rings) for (int i = 0; i < ring.size(); i++) {
            Vertex a = ring.get(i), b = ring.get((i + 1) % ring.size());
            Point from = new Point(a.getX(), a.getZ()), to = new Point(b.getX(), b.getZ());
            if (from.equals(to)) continue;
            Edge edge = new Edge(from, to);
            if (from.z == to.z) horizontal.computeIfAbsent(from.z, ignored -> new ArrayList<>()).add(edge);
            else edges.add(edge);
        }
        boolean joined = false;
        for (var group : horizontal.entrySet()) {
            List<Edge> original = group.getValue();
            if (original.size() == 1) { edges.add(original.get(0)); continue; }
            List<Event> events = new ArrayList<>();
            for (Edge edge : original) {
                boolean forward = edge.from.x < edge.to.x;
                events.add(new Event(Math.min(edge.from.x, edge.to.x), forward ? 1 : 0, forward ? 0 : 1));
                events.add(new Event(Math.max(edge.from.x, edge.to.x), forward ? -1 : 0, forward ? 0 : -1));
            }
            events.sort(Comparator.comparingDouble(Event::x));
            List<Edge> remaining = new ArrayList<>();
            int index = 0, forward = 0, backward = 0;
            boolean cancelled = false;
            while (index < events.size()) {
                double x = events.get(index).x;
                do {
                    Event event = events.get(index++);
                    forward += event.forward; backward += event.backward;
                } while (index < events.size() && events.get(index).x == x);
                if (index == events.size()) break;
                double next = events.get(index).x;
                // 只有正长度的反向重叠才是接缝，单点接触和任何真实小缺口都不能被合并。
                if (forward > 0 && backward > 0) cancelled = true;
                int net = forward - backward;
                if (net == 0) continue;
                Point left = new Point(x, group.getKey()), right = new Point(next, group.getKey());
                // 每个原始端点都保留为切点，防止部分重叠接缝丢失与斜边或竖边的连接。
                remaining.add(net > 0 ? new Edge(left, right) : new Edge(right, left));
            }
            joined |= cancelled;
            edges.addAll(cancelled ? remaining : original);
        }
        if (!joined) return rings;
        return trace(edges, rings);
    }

    private static List<List<Vertex>> trace(List<Edge> edges, List<List<Vertex>> original) {
        Map<Point, List<Integer>> outgoing = new HashMap<>();
        for (int i = 0; i < edges.size(); i++) outgoing.computeIfAbsent(edges.get(i).from, ignored -> new ArrayList<>()).add(i);
        int[] next = new int[edges.size()];
        for (int i = 0; i < edges.size(); i++) {
            Edge incoming = edges.get(i);
            List<Integer> candidates = outgoing.get(incoming.to);
            if (candidates == null) return original;
            double reverse = Math.atan2(incoming.from.z - incoming.to.z, incoming.from.x - incoming.to.x);
            double best = Double.POSITIVE_INFINITY;
            int chosen = -1;
            for (int candidate : candidates) {
                Edge edge = edges.get(candidate);
                double turn = Math.atan2(edge.to.z - edge.from.z, edge.to.x - edge.from.x) - reverse;
                if (turn <= 0) turn += Math.PI * 2;
                // 取反向入边的逆时针后继，使仅在顶点相碰的两个区域仍沿各自边界闭合。
                if (turn < best) { best = turn; chosen = candidate; }
            }
            if (chosen < 0) return original;
            next[i] = chosen;
        }
        boolean[] used = new boolean[edges.size()];
        List<List<Vertex>> result = new ArrayList<>();
        for (int start = 0; start < edges.size(); start++) {
            if (used[start]) continue;
            List<Vertex> ring = new ArrayList<>();
            int current = start;
            while (!used[current]) {
                used[current] = true;
                ring.add(edges.get(current).from.vertex());
                current = next[current];
            }
            // 无法完整重接时保留原轮廓交给既有校验，不补线或丢弃任何原始区域。
            if (current != start || ring.size() < 3) return original;
            result.add(ring);
        }
        return result;
    }
}
