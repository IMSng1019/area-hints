package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// 形状布尔运算与工具参考 Province（MIT，Copyright (c) 2026 Shuuki_），许可证随模组放在 META-INF/Province-LICENSE。
public final class VizEditGeometry {
    public static final int MAX_VERTICES = 4096;
    public static final double MAX_COORDINATE = 30_000_000;
    public static final int MAX_DRAG_SPAN = 4096;
    public static final int MAX_BRUSH_WIDTH = 256;
    public static final int MAX_DRAFT_VERTICES = 65_536;

    private VizEditGeometry() { }

    // 验证结果仅返回新顶点副本；失败时调用方保留原草稿，不能用空列表替换正在编辑的形状。
    public record Validation(boolean valid, String errorKey, List<Vertex> vertices) { }

    // 权限判断直接使用差集是否为空，极小但真实的面积变化也不能被浮点容差掩盖。
    public record Delta(boolean added, boolean removed, double addedArea, double removedArea) { }

    public static final class GeometryException extends IllegalArgumentException {
        private final String errorKey;
        public GeometryException(String errorKey) {
            super(errorKey);
            this.errorKey = errorKey;
        }
        public String errorKey() { return errorKey; }
    }

    // 原域名只做矢量转换，绝不对已有小数顶点、斜边或整个布尔运算结果重新方块化。
    public static Area fromVertices(List<Vertex> vertices) {
        if (vertices == null || vertices.isEmpty()) return new Area();
        if (vertices.size() > MAX_DRAFT_VERTICES) throw error("geometry_vertices");
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            checkCoordinate(v);
            if (i == 0) path.moveTo(v.getX(), v.getZ());
            else path.lineTo(v.getX(), v.getZ());
        }
        path.closePath();
        return new Area(path);
    }

    public static Area union(Area first, Area second) {
        Area result = copy(first);
        if (second != null) result.add(second);
        checkDraftComplexity(result);
        return result;
    }

    public static Area subtract(Area first, Area second) {
        Area result = copy(first);
        if (second != null) result.subtract(second);
        checkDraftComplexity(result);
        return result;
    }

    public static Area copy(Area area) { return area == null ? new Area() : new Area(area); }

    public static Delta difference(List<Vertex> before, List<Vertex> after) {
        Area previous = fromVertices(before);
        Area current = fromVertices(after);
        Area added = subtract(current, previous);
        Area removed = subtract(previous, current);
        return new Delta(!added.isEmpty(), !removed.isEmpty(), measure(added), measure(removed));
    }

    public static Area stroke(List<Vertex> points, double width) {
        if (!Double.isFinite(width) || width < 1 || width > MAX_BRUSH_WIDTH) throw error("geometry_limit");
        if (points == null || points.isEmpty()) return new Area();
        if (points.size() > 8192) throw error("geometry_limit");
        double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vertex point : points) {
            checkCoordinate(point);
            minX = Math.min(minX, point.getX()); minZ = Math.min(minZ, point.getZ());
            maxX = Math.max(maxX, point.getX()); maxZ = Math.max(maxZ, point.getZ());
        }
        if (maxX - minX > MAX_DRAG_SPAN || maxZ - minZ > MAX_DRAG_SPAN) throw error("geometry_limit");
        Area result = BlockSnap.brushStroke(points, width);
        checkBounds(result);
        return result;
    }

    public static Area rectangle(double x0, double z0, double x1, double z1, boolean constrain) {
        double[] box = shapeBounds(x0, z0, x1, z1, constrain);
        return new Area(new Rectangle2D.Double(box[0], box[1], box[2], box[3]));
    }

    public static Area ellipse(double x0, double z0, double x1, double z1, boolean constrain) {
        double[] box = shapeBounds(x0, z0, x1, z1, constrain);
        return BlockSnap.snap(new Area(new Ellipse2D.Double(box[0], box[1], box[2], box[3])));
    }

    // Shift 直线沿较长轴吸为水平或垂直，保持 Province 原有约束手感。
    public static Vertex constrainLine(Vertex start, Vertex end) {
        checkCoordinate(start); checkCoordinate(end);
        return Math.abs(end.getX() - start.getX()) >= Math.abs(end.getZ() - start.getZ())
            ? new Vertex(end.getX(), start.getZ()) : new Vertex(start.getX(), end.getZ());
    }

    private static double[] shapeBounds(double x0, double z0, double x1, double z1, boolean constrain) {
        checkCoordinate(new Vertex(x0, z0)); checkCoordinate(new Vertex(x1, z1));
        x0 = Math.floor(x0); z0 = Math.floor(z0); x1 = Math.floor(x1); z1 = Math.floor(z1);
        double dx = x1 - x0, dz = z1 - z0;
        if (constrain) {
            double side = Math.max(Math.abs(dx), Math.abs(dz));
            x1 = x0 + Math.copySign(side, dx); z1 = z0 + Math.copySign(side, dz);
        }
        if (Math.abs(x1 - x0) > MAX_DRAG_SPAN || Math.abs(z1 - z0) > MAX_DRAG_SPAN) throw error("geometry_limit");
        checkCoordinate(new Vertex(x1, z1));
        return new double[]{Math.min(x0, x1), Math.min(z0, z1), Math.abs(x1 - x0), Math.abs(z1 - z0)};
    }

    // 魔棒只从实际闭合平面图取面；不添加外包围框、不补缺口，也不按上级或同级自动裁剪。
    public static Area magicWand(List<double[]> segments, double x, double z) {
        checkCoordinate(new Vertex(x, z));
        Area face = FenceFaces.faceAt(segments, Math.floor(x) + 0.5, Math.floor(z) + 0.5);
        return face == null ? new Area() : BlockSnap.snap(face);
    }

    // 原始顶点必须在创建 Area 之前校验，否则 Area 会把自交路径悄悄分割为看似正常的多个环。
    public static Validation validate(List<Vertex> input) {
        if (input == null || input.size() < 3) return invalid("geometry_empty");
        if (input.size() > MAX_DRAFT_VERTICES) return invalid("geometry_vertices");
        for (Vertex v : input) if (!validCoordinate(v)) return invalid("geometry_coordinates");
        List<Vertex> points = simplify(input);
        int n = points.size();
        if (n < 3) return invalid("geometry_empty");
        if (n > MAX_VERTICES) return invalid("geometry_vertices");
        for (int i = 0; i < n; i++) {
            Vertex a = points.get(i), b = points.get((i + 1) % n), c = points.get((i + 2) % n);
            if (orientation(a, b, c) == 0) return invalid("geometry_self_intersection");
            for (int j = i + 2; j < n; j++) {
                if (i == 0 && j == n - 1) continue;
                if (intersects(a, b, points.get(j), points.get((j + 1) % n))) return invalid("geometry_self_intersection");
            }
        }
        if (signedArea(points) == 0) return invalid("geometry_empty");
        return new Validation(true, null, List.copyOf(points));
    }

    public static Validation validate(Area shape) {
        if (shape == null || shape.isEmpty()) return invalid("geometry_empty");
        List<List<Vertex>> rings;
        try { rings = contours(shape); }
        catch (GeometryException failure) { return new Validation(false, failure.errorKey(), List.of()); }
        if (rings.size() != 1) {
            // Area 规范化后的外环顺时针、洞逆时针，正面积的附加环表示无法写入单环域名格式的洞。
            for (List<Vertex> ring : rings) if (signedArea(ring) > 0) return invalid("geometry_holes");
            return invalid("geometry_disconnected");
        }
        return validate(rings.get(0));
    }

    public static List<Vertex> toVertices(Area shape) {
        Validation result = validate(shape);
        if (!result.valid()) throw new GeometryException(result.errorKey());
        return result.vertices();
    }

    // 原有边界的路径遍历使用原始直线端点；仅新椭圆在 BlockSnap 内按扫描线离散。
    public static List<List<Vertex>> contours(Area shape) {
        List<List<Vertex>> rings = new ArrayList<>();
        if (shape == null) return rings;
        PathIterator iterator = shape.getPathIterator(null);
        double[] xy = new double[6];
        List<Vertex> ring = null;
        int count = 0;
        while (!iterator.isDone()) {
            int kind = iterator.currentSegment(xy);
            if (kind == PathIterator.SEG_MOVETO) {
                ring = new ArrayList<>();
                rings.add(ring);
                ring.add(new Vertex(xy[0], xy[1]));
            } else if (kind == PathIterator.SEG_LINETO && ring != null) {
                ring.add(new Vertex(xy[0], xy[1]));
            } else if (kind != PathIterator.SEG_CLOSE) {
                throw error("geometry_self_intersection");
            }
            if (++count > MAX_DRAFT_VERTICES) throw error("geometry_vertices");
            iterator.next();
        }
        return rings;
    }

    public static List<double[]> segments(Area shape) {
        List<double[]> edges = new ArrayList<>();
        for (List<Vertex> ring : contours(shape)) {
            for (int i = 0; i < ring.size(); i++) {
                Vertex a = ring.get(i), b = ring.get((i + 1) % ring.size());
                if (!same(a, b)) edges.add(new double[]{a.getX(), a.getZ(), b.getX(), b.getZ()});
            }
        }
        return edges;
    }

    public static double measure(Area shape) {
        double sum = 0;
        for (List<Vertex> ring : contours(shape)) sum += signedArea(ring);
        return Math.abs(sum);
    }

    public static List<Vertex> bounds(List<Vertex> vertices) {
        if (vertices == null || vertices.isEmpty()) return List.of();
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Vertex v : vertices) {
            checkCoordinate(v);
            minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
            minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
        }
        return List.of(new Vertex(minX, maxZ), new Vertex(maxX, maxZ), new Vertex(maxX, minZ), new Vertex(minX, minZ));
    }

    // 只移除重复点和严格共线且位于前后点之间的中间点，绝不按误差阈值简化旧斜边。
    private static List<Vertex> simplify(List<Vertex> input) {
        List<Vertex> points = new ArrayList<>();
        for (Vertex source : input) {
            Vertex point = new Vertex(source.getX(), source.getZ());
            if (!points.isEmpty() && same(points.get(points.size() - 1), point)) continue;
            while (points.size() > 1 && removable(points.get(points.size() - 2), points.get(points.size() - 1), point)) points.remove(points.size() - 1);
            points.add(point);
        }
        if (points.size() > 1 && same(points.get(0), points.get(points.size() - 1))) points.remove(points.size() - 1);
        boolean changed = true;
        while (changed && points.size() > 2) {
            changed = false;
            int last = points.size() - 1;
            if (removable(points.get(last - 1), points.get(last), points.get(0))) {
                points.remove(last); changed = true;
            }
            if (points.size() > 2 && removable(points.get(points.size() - 1), points.get(0), points.get(1))) {
                points.remove(0); changed = true;
            }
        }
        return points;
    }

    private static boolean removable(Vertex a, Vertex b, Vertex c) {
        return orientation(a, b, c) == 0 && between(b.getX(), a.getX(), c.getX()) && between(b.getZ(), a.getZ(), c.getZ());
    }

    private static boolean between(double value, double first, double second) { return value >= Math.min(first, second) && value <= Math.max(first, second); }
    private static boolean same(Vertex a, Vertex b) { return a.getX() == b.getX() && a.getZ() == b.getZ(); }

    private static boolean intersects(Vertex a, Vertex b, Vertex c, Vertex d) {
        if (Math.max(a.getX(), b.getX()) < Math.min(c.getX(), d.getX()) || Math.max(c.getX(), d.getX()) < Math.min(a.getX(), b.getX())
            || Math.max(a.getZ(), b.getZ()) < Math.min(c.getZ(), d.getZ()) || Math.max(c.getZ(), d.getZ()) < Math.min(a.getZ(), b.getZ())) return false;
        int abC = orientation(a, b, c), abD = orientation(a, b, d), cdA = orientation(c, d, a), cdB = orientation(c, d, b);
        return abC * abD <= 0 && cdA * cdB <= 0;
    }

    // 大坐标附近的叉积相消时采用精确十进制判号，避免把非常短的真实斜边当作共线点删除。
    private static int orientation(Vertex a, Vertex b, Vertex c) {
        double left = (b.getX() - a.getX()) * (c.getZ() - a.getZ());
        double right = (b.getZ() - a.getZ()) * (c.getX() - a.getX());
        double determinant = left - right;
        if (Math.abs(determinant) > (Math.abs(left) + Math.abs(right)) * 1e-14) return determinant > 0 ? 1 : -1;
        BigDecimal ax = BigDecimal.valueOf(a.getX()), az = BigDecimal.valueOf(a.getZ());
        return BigDecimal.valueOf(b.getX()).subtract(ax).multiply(BigDecimal.valueOf(c.getZ()).subtract(az))
            .subtract(BigDecimal.valueOf(b.getZ()).subtract(az).multiply(BigDecimal.valueOf(c.getX()).subtract(ax))).signum();
    }

    static double signedArea(List<Vertex> ring) {
        if (ring.size() < 3) return 0;
        double x0 = ring.get(0).getX(), z0 = ring.get(0).getZ(), total = 0;
        for (int i = 1; i + 1 < ring.size(); i++) {
            Vertex a = ring.get(i), b = ring.get(i + 1);
            total += (a.getX() - x0) * (b.getZ() - z0) - (b.getX() - x0) * (a.getZ() - z0);
        }
        return total / 2;
    }

    static void checkBounds(Area shape) {
        Rectangle2D bounds = shape.getBounds2D();
        checkCoordinate(new Vertex(bounds.getMinX(), bounds.getMinY()));
        checkCoordinate(new Vertex(bounds.getMaxX(), bounds.getMaxY()));
    }

    // 布尔运算必须在把结果交还界面之前检查预览预算，超限时旧草稿仍然可以安全渲染和撤销。
    private static void checkDraftComplexity(Area shape) {
        PathIterator iterator = shape.getPathIterator(null);
        int count = 0;
        while (!iterator.isDone()) {
            if (++count > MAX_DRAFT_VERTICES) throw error("geometry_vertices");
            iterator.next();
        }
    }

    static void checkCoordinate(Vertex vertex) { if (!validCoordinate(vertex)) throw error("geometry_coordinates"); }
    private static boolean validCoordinate(Vertex vertex) {
        return vertex != null && Double.isFinite(vertex.getX()) && Double.isFinite(vertex.getZ())
            && Math.abs(vertex.getX()) <= MAX_COORDINATE && Math.abs(vertex.getZ()) <= MAX_COORDINATE;
    }
    static GeometryException error(String suffix) { return new GeometryException("vizedit.error." + suffix); }
    private static Validation invalid(String suffix) { return new Validation(false, "vizedit.error." + suffix, List.of()); }
}
