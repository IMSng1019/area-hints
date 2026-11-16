package areahint.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** 只处理不可变坐标的三角网格差集，可在后台计算并响应过期任务取消。 */
public final class TriangleMeshDifference {
    private static final double GEOMETRY_EPSILON = 1.0E-7D;

    private TriangleMeshDifference() {
    }

    public record Point(double x, double z) {
    }

    public record Triangle(Point first, Point second, Point third) {
    }

    public static List<Triangle> subtract(List<Triangle> source, List<Triangle> mask) {
        return subtract(source, mask, () -> false);
    }

    /** 从源网格依次扣除遮罩三角形，结果只保留不可变坐标且不修改输入列表。 */
    public static List<Triangle> subtract(List<Triangle> source, List<Triangle> mask,
                                          BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        if (source.isEmpty() || mask.isEmpty()) {
            return List.copyOf(source);
        }

        // 遮罩的包围盒和绕序只计算一次，避免每个源碎片重复准备同一几何数据。
        List<PreparedTriangle> masks = new ArrayList<>(mask.size());
        for (Triangle triangle : mask) {
            checkCancelled(cancelled);
            double orientation = cross(triangle.first(), triangle.second(), triangle.third());
            if (Math.abs(orientation) > GEOMETRY_EPSILON) {
                masks.add(new PreparedTriangle(triangle, orientation > 0.0D ? 1.0D : -1.0D,
                        Bounds.of(triangle)));
            }
        }

        List<Triangle> result = new ArrayList<>();
        for (Triangle sourceTriangle : source) {
            checkCancelled(cancelled);
            List<List<Point>> pieces = List.of(List.of(sourceTriangle.first(), sourceTriangle.second(), sourceTriangle.third()));
            for (PreparedTriangle maskTriangle : masks) {
                checkCancelled(cancelled);
                if (pieces.isEmpty()) {
                    break;
                }
                List<List<Point>> nextPieces = new ArrayList<>(pieces.size());
                for (List<Point> piece : pieces) {
                    checkCancelled(cancelled);
                    subtractTriangle(piece, maskTriangle, nextPieces, cancelled);
                }
                pieces = nextPieces;
            }
            addTriangleFans(result, pieces, cancelled);
        }
        checkCancelled(cancelled);
        return List.copyOf(result);
    }

    private static void subtractTriangle(List<Point> subject, PreparedTriangle mask,
                                         List<List<Point>> output, BooleanSupplier cancelled) {
        // 先排除真正不相交的片段，避免遮罩边的无限延长线把无关片段越切越碎。
        if (Bounds.of(subject, cancelled).separatedFrom(mask.bounds())
                || hasSeparatingAxis(subject, mask.triangle(), cancelled)) {
            output.add(subject);
            return;
        }

        // 凸片段经半平面裁剪仍然凸，每条边至多产生一个内部候选，无需再套候选列表。
        List<Point> candidate = subject;
        for (int edge = 0; edge < 3 && candidate != null; edge++) {
            checkCancelled(cancelled);
            Point edgeStart = vertex(mask.triangle(), edge);
            Point edgeEnd = vertex(mask.triangle(), (edge + 1) % 3);
            PolygonSplit split = splitByLine(candidate, edgeStart, edgeEnd, mask.insideSign(), cancelled);
            if (isUsablePolygon(split.outside(), cancelled)) {
                output.add(split.outside());
            }
            candidate = isUsablePolygon(split.inside(), cancelled) ? split.inside() : null;
        }
    }

    /** 包围盒重叠不等于几何相交，检查两个凸形状全部边的法线后才进行真正裁剪。 */
    private static boolean hasSeparatingAxis(List<Point> polygon, Triangle triangle, BooleanSupplier cancelled) {
        for (int i = 0; i < polygon.size(); i++) {
            checkCancelled(cancelled);
            if (separatedOnAxis(polygon, triangle, polygon.get(i), polygon.get((i + 1) % polygon.size()), cancelled)) {
                return true;
            }
        }
        for (int i = 0; i < 3; i++) {
            checkCancelled(cancelled);
            if (separatedOnAxis(polygon, triangle, vertex(triangle, i), vertex(triangle, (i + 1) % 3), cancelled)) {
                return true;
            }
        }
        return false;
    }

    private static boolean separatedOnAxis(List<Point> polygon, Triangle triangle, Point first, Point second,
                                            BooleanSupplier cancelled) {
        double normalX = first.z() - second.z();
        double normalZ = second.x() - first.x();
        double minPolygon = Double.POSITIVE_INFINITY;
        double maxPolygon = Double.NEGATIVE_INFINITY;
        for (Point point : polygon) {
            checkCancelled(cancelled);
            // 投影以当前边起点为原点，避免世界边缘的大坐标相减损失细小交集精度。
            double projection = (point.x() - first.x()) * normalX + (point.z() - first.z()) * normalZ;
            minPolygon = Math.min(minPolygon, projection);
            maxPolygon = Math.max(maxPolygon, projection);
        }
        double minTriangle = Double.POSITIVE_INFINITY;
        double maxTriangle = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 3; i++) {
            Point point = vertex(triangle, i);
            double projection = (point.x() - first.x()) * normalX + (point.z() - first.z()) * normalZ;
            minTriangle = Math.min(minTriangle, projection);
            maxTriangle = Math.max(maxTriangle, projection);
        }
        // 仅明确分离时跳过，接触和容差范围内的薄交集继续使用既有裁剪规则。
        return maxPolygon < minTriangle - GEOMETRY_EPSILON || maxTriangle < minPolygon - GEOMETRY_EPSILON;
    }

    private static Point vertex(Triangle triangle, int index) {
        return switch (index) {
            case 0 -> triangle.first();
            case 1 -> triangle.second();
            default -> triangle.third();
        };
    }

    private static PolygonSplit splitByLine(List<Point> polygon, Point edgeStart, Point edgeEnd,
                                            double insideSign, BooleanSupplier cancelled) {
        List<Point> inside = new ArrayList<>(polygon.size() + 1);
        List<Point> outside = new ArrayList<>(polygon.size() + 1);
        Point previous = polygon.get(polygon.size() - 1);
        double previousDistance = insideSign * cross(edgeStart, edgeEnd, previous);
        boolean previousInside = previousDistance >= -GEOMETRY_EPSILON;
        for (Point current : polygon) {
            checkCancelled(cancelled);
            double currentDistance = insideSign * cross(edgeStart, edgeEnd, current);
            boolean currentInside = currentDistance >= -GEOMETRY_EPSILON;
            if (currentInside != previousInside) {
                Point intersection = interpolateIntersection(previous, current, previousDistance, currentDistance);
                addDistinct(inside, intersection);
                addDistinct(outside, intersection);
            }
            addDistinct(currentInside ? inside : outside, current);
            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return new PolygonSplit(cleanPolygon(inside), cleanPolygon(outside));
    }

    private static Point interpolateIntersection(Point first, Point second, double firstDistance, double secondDistance) {
        double denominator = firstDistance - secondDistance;
        double ratio = Math.abs(denominator) <= GEOMETRY_EPSILON ? 0.0D : firstDistance / denominator;
        ratio = Math.max(0.0D, Math.min(1.0D, ratio));
        return new Point(first.x() + (second.x() - first.x()) * ratio,
                first.z() + (second.z() - first.z()) * ratio);
    }

    private static void addDistinct(List<Point> points, Point point) {
        if (points.isEmpty() || !samePoint(points.get(points.size() - 1), point)) {
            points.add(point);
        }
    }

    private static List<Point> cleanPolygon(List<Point> polygon) {
        if (polygon.size() > 1 && samePoint(polygon.get(0), polygon.get(polygon.size() - 1))) {
            polygon.remove(polygon.size() - 1);
        }
        return List.copyOf(polygon);
    }

    private static boolean samePoint(Point first, Point second) {
        return Math.abs(first.x() - second.x()) <= GEOMETRY_EPSILON
                && Math.abs(first.z() - second.z()) <= GEOMETRY_EPSILON;
    }

    private static boolean isUsablePolygon(List<Point> polygon, BooleanSupplier cancelled) {
        if (polygon.size() < 3) {
            return false;
        }
        Point origin = polygon.get(0);
        double doubledArea = 0.0D;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            checkCancelled(cancelled);
            doubledArea += cross(origin, polygon.get(i), polygon.get(i + 1));
        }
        return Math.abs(doubledArea) > GEOMETRY_EPSILON;
    }

    private static void addTriangleFans(List<Triangle> output, List<List<Point>> polygons, BooleanSupplier cancelled) {
        for (List<Point> polygon : polygons) {
            checkCancelled(cancelled);
            Point origin = polygon.get(0);
            for (int i = 1; i + 1 < polygon.size(); i++) {
                checkCancelled(cancelled);
                Point second = polygon.get(i);
                Point third = polygon.get(i + 1);
                if (Math.abs(cross(origin, second, third)) > GEOMETRY_EPSILON) {
                    output.add(new Triangle(origin, second, third));
                }
            }
        }
    }

    private static double cross(Point first, Point second, Point third) {
        return (second.x() - first.x()) * (third.z() - first.z())
                - (second.z() - first.z()) * (third.x() - first.x());
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new CancellationException("域名填充计算已过期");
        }
    }

    private record PreparedTriangle(Triangle triangle, double insideSign, Bounds bounds) {
    }

    private record PolygonSplit(List<Point> inside, List<Point> outside) {
    }

    private record Bounds(double minX, double minZ, double maxX, double maxZ) {
        private static Bounds of(Triangle triangle) {
            return new Bounds(Math.min(triangle.first().x(), Math.min(triangle.second().x(), triangle.third().x())),
                    Math.min(triangle.first().z(), Math.min(triangle.second().z(), triangle.third().z())),
                    Math.max(triangle.first().x(), Math.max(triangle.second().x(), triangle.third().x())),
                    Math.max(triangle.first().z(), Math.max(triangle.second().z(), triangle.third().z())));
        }

        private static Bounds of(List<Point> polygon, BooleanSupplier cancelled) {
            double minX = Double.POSITIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxZ = Double.NEGATIVE_INFINITY;
            for (Point point : polygon) {
                checkCancelled(cancelled);
                minX = Math.min(minX, point.x());
                minZ = Math.min(minZ, point.z());
                maxX = Math.max(maxX, point.x());
                maxZ = Math.max(maxZ, point.z());
            }
            return new Bounds(minX, minZ, maxX, maxZ);
        }

        private boolean separatedFrom(Bounds other) {
            return maxX < other.minX || minX > other.maxX || maxZ < other.minZ || minZ > other.maxZ;
        }
    }
}
