package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 改编自 Province core/BlockSnap.java（MIT），只用于新笔画；扫描线取方块中心，细笔用四连通阶梯补齐。
final class BlockSnap {
    private static final int MAX_PATH_CELLS = 65_536;
    private static final int MAX_SPANS = 1_000_000;
    private BlockSnap() { }

    static Area snap(Area area) {
        if (area == null || area.isEmpty()) return new Area();
        VizEditGeometry.checkBounds(area);
        Rectangle2D box = area.getBounds2D();
        int from = (int) Math.floor(box.getMinY()), to = (int) Math.ceil(box.getMaxY()) - 1;
        if ((long) to - from > VizEditGeometry.MAX_DRAG_SPAN) throw VizEditGeometry.error("geometry_limit");
        List<double[]> edges = edgesOf(area);
        edges.sort((a, b) -> Double.compare(a[1], b[1]));
        Builder builder = new Builder();
        List<double[]> active = new ArrayList<>();
        double[] hits = new double[8];
        int cursor = 0;
        for (int row = from; row <= to; row++) {
            double z = row + 0.5;
            while (cursor < edges.size() && edges.get(cursor)[1] <= z) active.add(edges.get(cursor++));
            active.removeIf(edge -> edge[3] <= z);
            if (hits.length < active.size()) hits = new double[active.size()];
            for (int i = 0; i < active.size(); i++) {
                double[] edge = active.get(i);
                hits[i] = edge[0] + (edge[2] - edge[0]) * (z - edge[1]) / (edge[3] - edge[1]);
            }
            Arrays.sort(hits, 0, active.size());
            List<int[]> runs = new ArrayList<>();
            for (int i = 0; i + 1 < active.size(); i += 2) {
                int first = (int) Math.ceil(hits[i] - 0.5), last = (int) Math.ceil(hits[i + 1] - 0.5) - 1;
                if (last >= first) runs.add(new int[]{first, last});
            }
            builder.row(row, runs);
        }
        return builder.build();
    }

    // 曲线扁平化仅发生在新椭圆扫描线内，绝不会把已有域名的斜边变为阶梯。
    private static List<double[]> edgesOf(Area area) {
        List<double[]> edges = new ArrayList<>();
        PathIterator iterator = area.getPathIterator(null, 0.125);
        double[] xy = new double[6];
        double startX = 0, startZ = 0, previousX = 0, previousZ = 0;
        while (!iterator.isDone()) {
            switch (iterator.currentSegment(xy)) {
                case PathIterator.SEG_MOVETO -> { startX = previousX = xy[0]; startZ = previousZ = xy[1]; }
                case PathIterator.SEG_LINETO -> { addEdge(edges, previousX, previousZ, xy[0], xy[1]); previousX = xy[0]; previousZ = xy[1]; }
                case PathIterator.SEG_CLOSE -> { addEdge(edges, previousX, previousZ, startX, startZ); previousX = startX; previousZ = startZ; }
                default -> throw VizEditGeometry.error("geometry_self_intersection");
            }
            if (edges.size() > VizEditGeometry.MAX_DRAFT_VERTICES) throw VizEditGeometry.error("geometry_limit");
            iterator.next();
        }
        return edges;
    }

    private static void addEdge(List<double[]> edges, double x0, double z0, double x1, double z1) {
        if (z0 != z1) edges.add(z0 < z1 ? new double[]{x0, z0, x1, z1} : new double[]{x1, z1, x0, z0});
    }

    static Area brushStroke(List<Vertex> points, double width) {
        List<int[]> path = pathCells(points);
        int radius = (int) Math.round((Math.max(1, width) - 1) / 2);
        if ((long) path.size() * (radius * 2 + 1) > MAX_SPANS) throw VizEditGeometry.error("geometry_limit");
        Map<Integer, List<int[]>> spans = new HashMap<>();
        int rowFrom = Integer.MAX_VALUE, rowTo = Integer.MIN_VALUE;
        for (int[] cell : path) {
            for (int dz = -radius; dz <= radius; dz++) {
                int half = (int) Math.floor(Math.sqrt((double) radius * radius - (double) dz * dz));
                int row = cell[1] + dz;
                spans.computeIfAbsent(row, ignored -> new ArrayList<>()).add(new int[]{cell[0] - half, cell[0] + half});
                rowFrom = Math.min(rowFrom, row); rowTo = Math.max(rowTo, row);
            }
        }
        Builder builder = new Builder();
        for (int row = rowFrom; row <= rowTo; row++) {
            List<int[]> spansAtRow = spans.get(row);
            builder.row(row, spansAtRow == null ? List.of() : merge(spansAtRow));
        }
        return builder.build();
    }

    private static List<int[]> merge(List<int[]> spans) {
        spans.sort((a, b) -> Integer.compare(a[0], b[0]));
        List<int[]> runs = new ArrayList<>();
        int from = spans.get(0)[0], to = spans.get(0)[1];
        for (int i = 1; i < spans.size(); i++) {
            int[] next = spans.get(i);
            if (next[0] <= to + 1) to = Math.max(to, next[1]);
            else { runs.add(new int[]{from, to}); from = next[0]; to = next[1]; }
        }
        runs.add(new int[]{from, to});
        return runs;
    }

    // 不抽稀用户笔画，以免绕行笔画被捷径截断；过长笔画明确报错，保留已完成的草稿。
    private static List<int[]> pathCells(List<Vertex> points) {
        List<int[]> cells = new ArrayList<>();
        int[] previous = null;
        for (Vertex point : points) {
            int[] cell = new int[]{(int) Math.floor(point.getX()), (int) Math.floor(point.getZ())};
            if (previous == null) cells.add(cell);
            else connect(cells, previous, cell);
            previous = cell;
        }
        return cells;
    }

    // 每一步只移动一个轴，避免单格宽斜线仅在角点相接而变成多个不连续区域。
    private static void connect(List<int[]> cells, int[] from, int[] to) {
        int dx = Math.abs(to[0] - from[0]), dz = Math.abs(to[1] - from[1]);
        if ((long) cells.size() + dx + dz > MAX_PATH_CELLS) throw VizEditGeometry.error("geometry_limit");
        int sx = from[0] < to[0] ? 1 : -1, sz = from[1] < to[1] ? 1 : -1;
        int error = dx - dz, x = from[0], z = from[1];
        while (x != to[0] || z != to[1]) {
            if (error * 2 > -dz && x != to[0]) { error -= dz; x += sx; }
            else if (z != to[1]) { error += dx; z += sz; }
            else { error -= dz; x += sx; }
            cells.add(new int[]{x, z});
        }
    }

    // 相同方块区间的连续行合并成矩形，再一次构造 Area，避免逐方块进行昂贵的布尔并集。
    private static final class Builder {
        private final List<double[]> rectangles = new ArrayList<>();
        private List<int[]> current;
        private int firstRow, lastRow;
        void row(int row, List<int[]> runs) {
            if (current != null && row == lastRow + 1 && same(current, runs)) { lastRow = row; return; }
            flush(); current = runs; firstRow = lastRow = row;
        }
        void flush() {
            if (current == null) return;
            for (int[] run : current) {
                if (rectangles.size() >= VizEditGeometry.MAX_DRAFT_VERTICES / 4) throw VizEditGeometry.error("geometry_limit");
                rectangles.add(new double[]{run[0], firstRow, run[1] + 1.0, lastRow + 1.0});
            }
            current = null;
        }
        Area build() {
            flush();
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
            for (double[] rect : rectangles) {
                path.moveTo(rect[0], rect[1]); path.lineTo(rect[2], rect[1]);
                path.lineTo(rect[2], rect[3]); path.lineTo(rect[0], rect[3]); path.closePath();
            }
            return new Area(path);
        }
        private boolean same(List<int[]> a, List<int[]> b) {
            if (a.size() != b.size()) return false;
            for (int i = 0; i < a.size(); i++) if (!Arrays.equals(a.get(i), b.get(i))) return false;
            return true;
        }
    }
}
