package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// 只优化有效单环且量化与简化共用一格误差预算；交互优化失败保留原稿，保存入口则强制验证量化结果。
final class BoundaryOptimizer {
    private static final double SCALE = 10;
    private static final double MAX_ERROR_SQUARED = 1.0 * 1.0;
    private static final double OLD_EDGE_EPSILON = 0.00000001;
    private static final int MAX_WORK = 8_000_000;

    private BoundaryOptimizer() { }

    static Area optimize(Area shape, Area preserved) {
        return optimize(shape, preserved, null);
    }

    static Area optimize(Area shape, Area preserved, Area forbidden) {
        Area original = VizEditGeometry.copy(shape);
        if (original.isEmpty()) return original;
        try {
            List<List<Vertex>> contours = VizEditGeometry.contours(original);
            if (contours.size() != 1) return original;
            List<Vertex> ring = VizEditGeometry.simplify(contours.get(0));
            Budget budget = new Budget();
            if (!simple(ring, budget)) return original;
            EdgeTree oldEdges = preserved == null || preserved.isEmpty() ? null : EdgeTree.from(preserved);
            Constraint constraint = Constraint.from(forbidden, original, budget);
            List<Vertex> result = reduce(ring, oldEdges, constraint, budget);
            if (result.size() > ring.size() || !simple(result, budget)
                    || Math.signum(VizEditGeometry.signedArea(result)) != Math.signum(VizEditGeometry.signedArea(ring))
                    || constraint != null && !constraint.allows(result, budget)) return original;
            return VizEditGeometry.fromVertices(result);
        } catch (BudgetExceeded | VizEditGeometry.GeometryException ignored) {
            // 自动简化只是一项可选优化，预算耗尽保留完整草稿，不把复杂度转成几何错误。
            return original;
        }
    }

    // 保存必须返回实际提交的量化顶点，不能再遍历 Area 取点，以免正规化计算重新产生多位小数。
    static List<Vertex> prepareForSave(Area shape) {
        return prepareForSave(shape, null);
    }

    static List<Vertex> prepareForSave(Area shape, Area forbidden) {
        if (shape == null || shape.isEmpty()) throw VizEditGeometry.error("geometry_empty");
        List<List<Vertex>> contours = VizEditGeometry.contours(shape);
        if (contours.size() != 1) {
            for (List<Vertex> contour : contours) if (VizEditGeometry.signedArea(contour) > 0)
                throw VizEditGeometry.error("geometry_holes");
            throw VizEditGeometry.error("geometry_disconnected");
        }
        List<Vertex> ring = VizEditGeometry.simplify(contours.get(0));
        if (ring.size() < 3 || VizEditGeometry.signedArea(ring) == 0) throw VizEditGeometry.error("geometry_empty");
        // 原环及禁区的合法性必须完整核验，不能因可选简化的工作预算耗尽而拒绝保存。
        if (!isSimple(ring)) throw VizEditGeometry.error("geometry_self_intersection");
        Constraint constraint = Constraint.from(forbidden, shape, Budget.unlimited());
        Budget budget = new Budget();
        double direction = Math.signum(VizEditGeometry.signedArea(ring));
        try {
            List<Vertex> reduced = reduce(ring, null, constraint, budget);
            VizEditGeometry.Validation checked = VizEditGeometry.validate(reduced);
            if (reduced.size() <= ring.size() && checked.valid()
                    && Math.signum(VizEditGeometry.signedArea(checked.vertices())) == direction
                    && (constraint == null || constraint.allows(checked.vertices(), budget))) return checked.vertices();
        } catch (BudgetExceeded ignored) {
            // 优化预算耗尽仍可尝试只量化原环；回退结果也必须全部为一位小数，绝不能返回原来的高精度顶点。
        }
        List<Vertex> rounded = new ArrayList<>(ring.size());
        for (Vertex point : ring) rounded.add(round(point));
        VizEditGeometry.Validation checked = VizEditGeometry.validate(rounded);
        if (!checked.valid()) throw new VizEditGeometry.GeometryException(checked.errorKey());
        if (Math.signum(VizEditGeometry.signedArea(checked.vertices())) != direction)
            throw VizEditGeometry.error("geometry_self_intersection");
        // 量化回退仍完整检查禁区；预算只决定是否继续减点，不决定合法轮廓是否允许保存。
        if (constraint != null && !constraint.allows(checked.vertices(), Budget.unlimited()))
            throw new VizEditGeometry.GeometryException("vizedit.boundary.save_conflict");
        return checked.vertices();
    }

    private static List<Vertex> reduce(List<Vertex> ring, EdgeTree oldEdges, Constraint constraint, Budget budget) {
        int count = ring.size(), length = count * 2;
        double direction = Math.signum(VizEditGeometry.signedArea(ring));
        Vertex[] source = new Vertex[length + 1], rounded = new Vertex[length + 1];
        boolean[] keep = new boolean[length + 1];
        boolean[] sharedEdges = new boolean[count];
        if (oldEdges != null) for (int i = 0; i < count; i++)
            sharedEdges[i] = oldEdges.overlaps(ring.get(i), ring.get((i + 1) % count), budget);
        double farthestDistance = -1;
        int farthest = 0;
        for (int i = 0; i < count; i++) {
            Vertex point = ring.get(i), next = ring.get((i + 1) % count);
            boolean protectedPoint = sharedEdges[i] || sharedEdges[(i + count - 1) % count]
                || oldEdges != null && oldEdges.contains(point, budget);
            source[i * 2] = point;
            rounded[i * 2] = protectedPoint ? point : round(point);
            keep[i * 2] = protectedPoint || count <= 4 || sharpCorner(ring.get((i + count - 1) % count), point, next);
            // 原边中点为阶梯轮廓提供居中的直线端点，避免只有旧拐点可选时生成不必要的短折线。
            Vertex middle = new Vertex(point.getX() + (next.getX() - point.getX()) / 2,
                point.getZ() + (next.getZ() - point.getZ()) / 2);
            source[i * 2 + 1] = middle;
            rounded[i * 2 + 1] = round(middle);
            double distance = distanceSquared(point, ring.get(0));
            if (distance > farthestDistance) { farthestDistance = distance; farthest = i * 2; }
        }
        source[length] = source[0]; rounded[length] = rounded[0];
        keep[0] = keep[length] = keep[farthest] = true;
        ArrayDeque<int[]> pending = new ArrayDeque<>();
        int previous = 0;
        for (int i = 1; i <= length; i++) if (keep[i]) {
            pending.push(new int[]{previous, i}); previous = i;
        }
        while (!pending.isEmpty()) {
            int[] interval = pending.pop();
            int start = interval[0], end = interval[1];
            int worst = -1;
            int furthest = -1;
            double furthestDistance = -1;
            int inward = -1;
            double inwardDistance = -1;
            double worstDistance = MAX_ERROR_SQUARED;
            for (int i = start + 1; i < end; i++) {
                budget.use();
                double distance = segmentDistanceSquared(source[i], rounded[start], rounded[end]);
                if (distance > furthestDistance || distance == furthestDistance
                        && Math.abs(i * 2 - start - end) < Math.abs(furthest * 2 - start - end)) {
                    furthestDistance = distance; furthest = i;
                }
                double cross = (rounded[end].getX() - rounded[start].getX()) * (source[i].getZ() - rounded[start].getZ())
                    - (rounded[end].getZ() - rounded[start].getZ()) * (source[i].getX() - rounded[start].getX());
                if (cross * direction > 0 && (distance > inwardDistance || distance == inwardDistance
                        && Math.abs(i * 2 - start - end) < Math.abs(inward * 2 - start - end))) {
                    inwardDistance = distance; inward = i;
                }
                if (distance > worstDistance || worst >= 0 && distance == worstDistance
                        && Math.abs(i * 2 - start - end) < Math.abs(worst * 2 - start - end)) {
                    worstDistance = distance; worst = i;
                }
            }
            // 每条候选捷径单独核验禁区，局部受限只继续拆分本段，其他安全岸段仍按原算法减点。
            boolean constrained = worst < 0 && constraint != null
                && !constraint.allowsShortcut(source, rounded, start, end, budget);
            if (worst < 0 && (!constrained || furthest < 0)) continue;
            // 优先在最坏点旁的原边中点分段；两侧仍对全部原始点复核，绝不将前一次简化结果继续累计近似。
            // 越界时优先保留凹向域名内部的点，否则绝对最远的安全凸角会让整条阶梯逐点拆回原状。
            int split = constrained ? inward >= 0 ? inward : furthest : worst;
            if (!constrained && (worst & 1) == 0) {
                if (worst + 1 < end && (worst - 1 <= start || worst * 2 < start + end)) split = worst + 1;
                else if (worst - 1 > start) split = worst - 1;
            }
            keep[split] = true;
            pending.push(new int[]{start, split});
            pending.push(new int[]{split, end});
        }
        List<Vertex> result = new ArrayList<>();
        for (int i = 0; i < length; i++) if (keep[i]) result.add(rounded[i]);
        return VizEditGeometry.simplify(result);
    }

    // 禁区先扣除用户已明确涂入的面积；后续只阻止简化新增越界，不改变其他工具的绘制语义。
    private static final class Constraint {
        private final Area forbidden;
        private final long vertices;

        private Constraint(Area forbidden, long vertices) { this.forbidden = forbidden; this.vertices = vertices; }

        static Constraint from(Area forbidden, Area original, Budget budget) {
            if (forbidden == null || forbidden.isEmpty()) return null;
            long inputSize = size(forbidden, budget) + size(original, budget);
            budget.use(16L * inputSize);
            Area effective = new Area(forbidden);
            effective.subtract(original);
            return effective.isEmpty() ? null : new Constraint(effective, size(effective, budget));
        }

        boolean allowsShortcut(Vertex[] source, Vertex[] rounded, int start, int end, Budget budget) {
            // 原链与新捷径围出的变化带包含端点量化误差；单段越界会继续细分，最终仍核验整环。
            budget.use(16L * (vertices + end - start + 4));
            Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
            path.moveTo(source[start].getX(), source[start].getZ());
            for (int i = start + 1; i <= end; i++) path.lineTo(source[i].getX(), source[i].getZ());
            path.lineTo(rounded[end].getX(), rounded[end].getZ());
            path.lineTo(rounded[start].getX(), rounded[start].getZ());
            path.closePath();
            if (!forbidden.getBounds2D().intersects(path.getBounds2D())) return true;
            Area changed = new Area(path);
            changed.intersect(forbidden);
            return changed.isEmpty();
        }

        boolean allows(List<Vertex> ring, Budget budget) {
            budget.use(16L * (vertices + ring.size()));
            Area candidate = VizEditGeometry.fromVertices(ring);
            candidate.intersect(forbidden);
            return candidate.isEmpty();
        }

        private static long size(Area area, Budget budget) {
            PathIterator iterator = area.getPathIterator(null);
            long result = 0;
            while (!iterator.isDone()) {
                budget.use();
                result++;
                iterator.next();
            }
            return result;
        }
    }

    // 原始链在新线段半径一格的凸胶囊内，则每条原边都在误差内；链连续且端点误差不超过0.071格，反向覆盖同样成立。
    private static double segmentDistanceSquared(Vertex point, Vertex start, Vertex end) {
        double dx = end.getX() - start.getX(), dz = end.getZ() - start.getZ();
        double squaredLength = dx * dx + dz * dz;
        if (squaredLength == 0) return distanceSquared(point, start);
        double t = Math.max(0, Math.min(1, ((point.getX() - start.getX()) * dx + (point.getZ() - start.getZ()) * dz) / squaredLength));
        double ex = point.getX() - start.getX() - t * dx, ez = point.getZ() - start.getZ() - t * dz;
        return ex * ex + ez * ez;
    }

    private static double distanceSquared(Vertex first, Vertex second) {
        double dx = first.getX() - second.getX(), dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static Vertex round(Vertex point) {
        double x = Math.rint(point.getX() * SCALE) / SCALE, z = Math.rint(point.getZ() * SCALE) / SCALE;
        return new Vertex(x == 0 ? 0 : x, z == 0 ? 0 : z);
    }

    // 长边形成的明显转角保留为锚点；一格阶梯的小拐角交给整条原始链的误差检查统一处理。
    private static boolean sharpCorner(Vertex before, Vertex point, Vertex after) {
        double ax = point.getX() - before.getX(), az = point.getZ() - before.getZ();
        double bx = after.getX() - point.getX(), bz = after.getZ() - point.getZ();
        double a = ax * ax + az * az, b = bx * bx + bz * bz;
        return a > 1 && b > 1 && ax * bx + az * bz < 0.5 * Math.sqrt(a * b);
    }

    // 正式校验不设点数或工作量上限，空间索引只跳过包围盒互不相交的边对。
    static boolean isSimple(List<Vertex> ring) {
        return simple(ring, Budget.unlimited());
    }

    // 交互简化可使用工作预算，但保存和输入验证始终检查全部可能相交的边。
    private static boolean simple(List<Vertex> ring, Budget budget) {
        int count = ring.size();
        if (count < 3 || VizEditGeometry.signedArea(ring) == 0) return false;
        Edge[] edges = new Edge[count];
        for (int i = 0; i < count; i++) {
            Vertex a = ring.get(i), b = ring.get((i + 1) % count), c = ring.get((i + 2) % count);
            VizEditGeometry.checkCoordinate(a);
            if (distanceSquared(a, b) == 0) return false;
            // 共线正常中点已无损删除，剩余共线相邻边是折返，必须保留原稿而不是自动修复。
            if (VizEditGeometry.orientation(a, b, c) == 0) return false;
            edges[i] = new Edge(a, b, i);
        }
        EdgeTree tree = new EdgeTree(edges, 0, count);
        for (Edge edge : edges) if (tree.crosses(edge, count, budget)) return false;
        return true;
    }

    private static final class Edge {
        final Vertex start, end;
        final int index;
        final double minX, maxX, minZ, maxZ;
        Edge(Vertex start, Vertex end, int index) {
            this.start = start; this.end = end; this.index = index;
            minX = Math.min(start.getX(), end.getX()); maxX = Math.max(start.getX(), end.getX());
            minZ = Math.min(start.getZ(), end.getZ()); maxZ = Math.max(start.getZ(), end.getZ());
        }
    }

    // 旧边界使用包围盒树定位，只锁定落在原边上的端点；交点保留完整精度以免把相邻旧斜边挪开。
    private static final class EdgeTree {
        final double minX, maxX, minZ, maxZ;
        final Edge[] edges;
        final int from, to;
        final EdgeTree left, right;

        static EdgeTree from(Area area) {
            List<Edge> edges = new ArrayList<>();
            for (List<Vertex> ring : VizEditGeometry.contours(area)) for (int i = 0; i < ring.size(); i++)
                edges.add(new Edge(ring.get(i), ring.get((i + 1) % ring.size()), i));
            return edges.isEmpty() ? null : new EdgeTree(edges.toArray(Edge[]::new), 0, edges.size());
        }

        EdgeTree(Edge[] edges, int from, int to) {
            this.edges = edges; this.from = from; this.to = to;
            double x0 = Double.POSITIVE_INFINITY, x1 = Double.NEGATIVE_INFINITY;
            double z0 = Double.POSITIVE_INFINITY, z1 = Double.NEGATIVE_INFINITY;
            for (int i = from; i < to; i++) {
                x0 = Math.min(x0, edges[i].minX); x1 = Math.max(x1, edges[i].maxX);
                z0 = Math.min(z0, edges[i].minZ); z1 = Math.max(z1, edges[i].maxZ);
            }
            minX = x0; maxX = x1; minZ = z0; maxZ = z1;
            if (to - from <= 8) { left = right = null; return; }
            boolean horizontal = maxX - minX >= maxZ - minZ;
            Arrays.sort(edges, from, to, Comparator.comparingDouble(edge -> horizontal ? edge.minX + edge.maxX : edge.minZ + edge.maxZ));
            int middle = (from + to) / 2;
            left = new EdgeTree(edges, from, middle); right = new EdgeTree(edges, middle, to);
        }

        // 每对非相邻边只验证一次，两轴包围盒剪枝避免竖向锯齿或长横边退化成全量两两检查。
        boolean crosses(Edge edge, int count, Budget budget) {
            budget.use();
            if (edge.maxX < minX || edge.minX > maxX || edge.maxZ < minZ || edge.minZ > maxZ) return false;
            if (left != null) return left.crosses(edge, count, budget) || right.crosses(edge, count, budget);
            for (int i = from; i < to; i++) {
                budget.use();
                Edge other = edges[i];
                int separation = other.index - edge.index;
                if (separation <= 1 || separation == count - 1 || edge.maxX < other.minX || other.maxX < edge.minX
                        || edge.maxZ < other.minZ || other.maxZ < edge.minZ) continue;
                if (VizEditGeometry.intersects(edge.start, edge.end, other.start, other.end)) return true;
            }
            return false;
        }

        boolean contains(Vertex point, Budget budget) {
            budget.use();
            if (point.getX() < minX - OLD_EDGE_EPSILON || point.getX() > maxX + OLD_EDGE_EPSILON
                    || point.getZ() < minZ - OLD_EDGE_EPSILON || point.getZ() > maxZ + OLD_EDGE_EPSILON) return false;
            if (left != null) return left.contains(point, budget) || right.contains(point, budget);
            for (int i = from; i < to; i++) {
                budget.use();
                if (segmentDistanceSquared(point, edges[i].start, edges[i].end) <= OLD_EDGE_EPSILON * OLD_EDGE_EPSILON) return true;
            }
            return false;
        }

        // 新边可能在两端都延伸超出原边，仍要保留其中共线重叠的旧边，不能只通过端点落点判断。
        boolean overlaps(Vertex start, Vertex end, Budget budget) {
            budget.use();
            double x0 = Math.min(start.getX(), end.getX()), x1 = Math.max(start.getX(), end.getX());
            double z0 = Math.min(start.getZ(), end.getZ()), z1 = Math.max(start.getZ(), end.getZ());
            if (x1 < minX || x0 > maxX || z1 < minZ || z0 > maxZ) return false;
            if (left != null) return left.overlaps(start, end, budget) || right.overlaps(start, end, budget);
            for (int i = from; i < to; i++) {
                budget.use();
                Edge edge = edges[i];
                if (x1 < edge.minX || x0 > edge.maxX || z1 < edge.minZ || z0 > edge.maxZ) continue;
                if (Math.min(x1, edge.maxX) <= Math.max(x0, edge.minX)
                        && Math.min(z1, edge.maxZ) <= Math.max(z0, edge.minZ)) continue;
                if (VizEditGeometry.orientation(start, end, edge.start) == 0
                        && VizEditGeometry.orientation(start, end, edge.end) == 0) return true;
            }
            return false;
        }
    }

    private static final class Budget {
        private int remaining = MAX_WORK;
        private final boolean limited;
        Budget() { this(true); }
        private Budget(boolean limited) { this.limited = limited; }
        static Budget unlimited() { return new Budget(false); }
        void use() { if (limited && --remaining < 0) throw new BudgetExceeded(); }
        void use(long amount) {
            if (!limited) return;
            if (amount > remaining) throw new BudgetExceeded();
            remaining -= (int) amount;
        }
    }

    private static final class BudgetExceeded extends RuntimeException {
        @Override public synchronized Throwable fillInStackTrace() { return this; }
    }
}
