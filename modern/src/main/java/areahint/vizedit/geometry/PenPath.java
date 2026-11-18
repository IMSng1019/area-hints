package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 钢笔保留世界坐标下的锚点和方向手柄，预览、描边与闭合填充使用相同的折线精度。
public final class PenPath {
    private static final double FLATNESS = 0.05;
    private final List<Anchor> nodes = new ArrayList<>();

    // Vertex 本身可写，在接口两侧复制，使撤销快照可以安全共享不可变锚点。
    public record Anchor(Vertex point, Vertex in, Vertex out) {
        public Anchor {
            point = checkedCopy(point);
            in = checkedCopy(in);
            out = checkedCopy(out);
        }
        @Override public Vertex point() { return copyPoint(point); }
        @Override public Vertex in() { return copyPoint(in); }
        @Override public Vertex out() { return copyPoint(out); }
    }

    public int size() { return nodes.size(); }
    public List<Anchor> anchors() { return Collections.unmodifiableList(nodes); }

    public PenPath copy() {
        PenPath snapshot = new PenPath();
        snapshot.nodes.addAll(nodes);
        return snapshot;
    }

    public int add(Vertex point) {
        nodes.add(new Anchor(point, point, point));
        return nodes.size() - 1;
    }

    public void remove(int index) { nodes.remove(index); }

    public void corner(int index) {
        Vertex point = nodes.get(index).point;
        nodes.set(index, new Anchor(point, point, point));
    }

    // 移动锚点时同步平移两侧手柄，保留局部切线的方向与长度。
    public void move(int index, Vertex point) {
        Anchor old = nodes.get(index);
        double dx = point.getX() - old.point.getX(), dz = point.getZ() - old.point.getZ();
        nodes.set(index, new Anchor(point, translate(old.in, dx, dz), translate(old.out, dx, dz)));
    }

    public void handle(int index, boolean outgoing, Vertex target, boolean mirror) {
        Anchor old = nodes.get(index);
        Vertex opposite = outgoing ? old.in : old.out;
        if (mirror) opposite = interpolate(old.point, target, -1);
        nodes.set(index, outgoing ? new Anchor(old.point, opposite, target) : new Anchor(old.point, target, opposite));
    }

    // 调整平滑点时只转动另一侧手柄；此前插点或单侧调节形成的不等长手柄仍保留各自长度。
    public void editSmoothHandle(int index, boolean outgoing, Vertex target, double oppositeLength) {
        Anchor old = nodes.get(index);
        double dx = target.getX() - old.point.getX(), dz = target.getZ() - old.point.getZ();
        double length = Math.hypot(dx, dz);
        Vertex opposite = outgoing ? old.in : old.out;
        if (length > 0) opposite = translate(old.point, -dx / length * oppositeLength, -dz / length * oppositeLength);
        nodes.set(index, outgoing ? new Anchor(old.point, opposite, target) : new Anchor(old.point, target, opposite));
    }

    // 闭合时补回起点，悬停时只追加临时末段，两种操作都保留原锚点列表。
    public List<Vertex> flatten(boolean closed, Vertex cursor) {
        List<Vertex> vertices = new ArrayList<>();
        if (nodes.isEmpty()) return vertices;
        vertices.add(nodes.get(0).point());
        for (int i = 1; i < size(); i++) appendCurve(vertices, nodes.get(i - 1), nodes.get(i));
        Anchor last = nodes.get(size() - 1);
        if (closed && size() > 1) appendCurve(vertices, last, nodes.get(0));
        else if (VizEditGeometry.validCoordinate(cursor)) appendCurve(vertices, last, new Anchor(cursor, cursor, cursor));
        return vertices;
    }

    private static void appendCurve(List<Vertex> vertices, Anchor from, Anchor to) {
        visitChords(new Curve(from.point, from.out, to.in, to.point), 0, 1,
            (curve, start, end) -> vertices.add(copyPoint(curve.end)));
    }

    public Area fill() {
        // 先检查原始路径，防止 Area 将交叉路径重组为不同的合法轮廓后掩盖错误。
        var checked = VizEditGeometry.validate(flatten(true, null));
        if (!checked.valid()) throw new VizEditGeometry.GeometryException(checked.errorKey());
        return VizEditGeometry.fromVertices(checked.vertices());
    }

    public Area stroke(int width) { return VizEditGeometry.stroke(flatten(false, null), width); }

    // 在真正的曲线参数处切分并保留左右控制点，插入锚点不会改变原有路径。
    public int insertNear(Vertex target, double radius) {
        Hit hit = nearest(target, radius);
        if (hit.index < 0 || hit.t <= 0 || hit.t >= 1) return -1;
        Anchor from = nodes.get(hit.index - 1), to = nodes.get(hit.index);
        Anchor inserted;
        if (hit.curve.straight()) {
            Vertex point = interpolate(from.point, to.point, hit.t);
            inserted = new Anchor(point, point, point);
        } else {
            Split split = hit.curve.split(hit.t);
            nodes.set(hit.index - 1, new Anchor(from.point, from.in, split.left.first));
            nodes.set(hit.index, new Anchor(to.point, split.right.second, to.out));
            inserted = new Anchor(split.left.end, split.left.second, split.right.first);
        }
        nodes.add(hit.index, inserted);
        return hit.index;
    }

    // 返回命中线段的终点索引，供画布直接选择并移动整条线段。
    public int segmentNear(Vertex target, double radius) { return nearest(target, radius).index; }

    private Hit nearest(Vertex target, double radius) {
        Hit hit = new Hit(target, radius);
        for (int i = 1; i < size(); i++) {
            Anchor from = nodes.get(i - 1), to = nodes.get(i);
            hit.consider(i, new Curve(from.point, from.out, to.in, to.point));
        }
        return hit;
    }

    private static final class Hit {
        final Vertex target;
        double distanceSquared;
        int index = -1;
        Curve curve;
        double t;

        Hit(Vertex target, double radius) {
            this.target = target;
            this.distanceSquared = radius * radius;
        }

        void consider(int segment, Curve original) {
            if (original.straight()) {
                double parameter = projection(target, original.start, original.end);
                update(segment, original, parameter, interpolate(original.start, original.end, parameter));
                return;
            }
            visitChords(original, 0, 1, (part, t0, t1) -> {
                Vertex projected = interpolate(part.start, part.end, projection(target, part.start, part.end));
                // 折线误差只用来筛候选，实际命中与最终参数在贝塞尔曲线上求取。
                double reach = Math.sqrt(distanceSquared) + FLATNESS;
                if (squared(target, projected) > reach * reach) return;
                double lo = 0, hi = 1;
                for (int i = 0; i < 72; i++) {
                    double a = (2 * lo + hi) / 3, b = (lo + 2 * hi) / 3;
                    if (squared(target, part.at(a)) <= squared(target, part.at(b))) hi = b;
                    else lo = a;
                }
                update(segment, original, t0, part.start);
                update(segment, original, t1, part.end);
                double middle = (lo + hi) / 2;
                update(segment, original, t0 + (t1 - t0) * middle, part.at(middle));
            });
        }

        private void update(int segment, Curve original, double parameter, Vertex candidate) {
            double distance = squared(candidate, target);
            if (distance <= distanceSquared) {
                distanceSquared = distance;
                index = segment;
                curve = original;
                t = parameter;
            }
        }
    }

    // 投影后将参数限制在线段内，因此斜边中央与顶点都可以成为吸附位置。
    public static Vertex snap(Vertex point, List<double[]> edges, double radius) {
        double x = point.getX(), z = point.getZ();
        double nearestX = x, nearestZ = z;
        double best = radius * radius;
        for (double[] edge : edges) {
            // 逐帧吸附要遍历参考边，先过滤指针范围外的边，投影仅使用基本数值以减少分配。
            if (x + radius < Math.min(edge[0], edge[2]) || x - radius > Math.max(edge[0], edge[2])
                || z + radius < Math.min(edge[1], edge[3]) || z - radius > Math.max(edge[1], edge[3])) continue;
            double dx = edge[2] - edge[0], dz = edge[3] - edge[1];
            double t = projection(x - edge[0], z - edge[1], dx, dz);
            double projectedX = t == 1 ? edge[2] : edge[0] + t * dx;
            double projectedZ = t == 1 ? edge[3] : edge[1] + t * dz;
            double offsetX = projectedX - x, offsetZ = projectedZ - z;
            double distance = offsetX * offsetX + offsetZ * offsetZ;
            if (distance <= best) {
                best = distance;
                nearestX = projectedX;
                nearestZ = projectedZ;
            }
        }
        return new Vertex(nearestX, nearestZ);
    }

    private record Curve(Vertex start, Vertex first, Vertex second, Vertex end) {
        boolean straight() { return squared(start, first) == 0 && squared(second, end) == 0; }

        Vertex at(double t) {
            double s = 1 - t;
            double a = s * s * s, b = 3 * s * s * t, c = 3 * s * t * t, d = t * t * t;
            return new Vertex(a * start.getX() + b * first.getX() + c * second.getX() + d * end.getX(),
                a * start.getZ() + b * first.getZ() + c * second.getZ() + d * end.getZ());
        }

        // de Casteljau 切分既用于曲线展平，也用于插点后重建两段精确相接的控制多边形。
        Split split(double t) {
            Vertex a = interpolate(start, first, t), b = interpolate(first, second, t), c = interpolate(second, end, t);
            Vertex d = interpolate(a, b, t), e = interpolate(b, c, t);
            Vertex middle = interpolate(d, e, t);
            return new Split(new Curve(start, a, d, middle), new Curve(middle, e, c, end));
        }
    }

    private record Split(Curve left, Curve right) { }
    private interface Chord { void accept(Curve part, double t0, double t1); }

    private static void visitChords(Curve curve, double t0, double t1, Chord visitor) {
        double flatness = CubicCurve2D.getFlatnessSq(curve.start.getX(), curve.start.getZ(),
            curve.first.getX(), curve.first.getZ(), curve.second.getX(), curve.second.getZ(), curve.end.getX(), curve.end.getZ());
        if (flatness <= FLATNESS * FLATNESS) {
            visitor.accept(curve, t0, t1);
            return;
        }
        Split split = curve.split(0.5);
        double middle = (t0 + t1) / 2;
        visitChords(split.left, t0, middle, visitor);
        visitChords(split.right, middle, t1, visitor);
    }

    private static double projection(Vertex point, Vertex start, Vertex end) {
        return projection(point.getX() - start.getX(), point.getZ() - start.getZ(), end.getX() - start.getX(), end.getZ() - start.getZ());
    }

    private static double projection(double offsetX, double offsetZ, double dx, double dz) {
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0) return 0;
        return Math.max(0, Math.min(1, (offsetX * dx + offsetZ * dz) / lengthSquared));
    }

    private static double squared(Vertex a, Vertex b) {
        double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static Vertex interpolate(Vertex a, Vertex b, double t) {
        return translate(a, (b.getX() - a.getX()) * t, (b.getZ() - a.getZ()) * t);
    }

    private static Vertex translate(Vertex a, double dx, double dz) { return new Vertex(a.getX() + dx, a.getZ() + dz); }
    private static Vertex copyPoint(Vertex p) { return new Vertex(p.getX(), p.getZ()); }
    private static Vertex checkedCopy(Vertex point) {
        VizEditGeometry.checkCoordinate(point);
        return copyPoint(point);
    }
}
