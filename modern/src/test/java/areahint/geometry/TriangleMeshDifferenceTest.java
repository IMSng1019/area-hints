package areahint.geometry;

import areahint.geometry.TriangleMeshDifference.Point;
import areahint.geometry.TriangleMeshDifference.Triangle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TriangleMeshDifferenceTest {
    @Test
    void removesOnlyTheInteriorMaskAndKeepsTheHoleEmpty() {
        List<Triangle> source = rectangle(0, 0, 4, 4);
        List<Triangle> mask = rectangle(1, 1, 3, 3);
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(12.0, area(result), 1.0E-8);
        assertCoverage(source, mask, result, 0, 0, 4, 4);
        assertFalse(contains(result, new Point(2, 2)));
    }

    @Test
    void subtractsOnlyTheIntersectionWhenMaskCrossesTheParentBoundary() {
        List<Triangle> source = rectangle(0, 0, 4, 4);
        List<Triangle> mask = rectangle(2, -2, 6, 2);
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(12.0, area(result), 1.0E-8);
        assertCoverage(source, mask, result, -1, -1, 6, 5);
    }

    @Test
    void supportsConcaveMasksRepresentedBySeveralTriangles() {
        List<Triangle> source = rectangle(0, 0, 5, 5);
        List<Triangle> mask = new ArrayList<>(rectangle(1, 1, 2, 4));
        mask.addAll(rectangle(2, 1, 4, 2));
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(20.0, area(result), 1.0E-8);
        assertCoverage(source, mask, result, 0, 0, 5, 5);
    }

    @Test
    void supportsAConcaveParentAndSeveralDisconnectedRemainingPieces() {
        List<Triangle> source = new ArrayList<>(rectangle(0, 0, 2, 6));
        source.addAll(rectangle(2, 0, 6, 2));
        List<Triangle> mask = rectangle(-1, 1, 7, 1.5);
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(17.0, area(result), 1.0E-8);
        assertCoverage(source, mask, result, -1, -1, 7, 7);
    }

    @Test
    void preservesCoverageForBothWindingDirectionsAtLargeNegativeCoordinates() {
        List<Triangle> source = rectangle(-29_999_990, -29_999_990, -29_999_980, -29_999_980);
        List<Triangle> mask = rectangle(-29_999_988, -29_999_988, -29_999_983, -29_999_983);
        for (List<Triangle> sourceWinding : List.of(source, reverse(source))) {
            for (List<Triangle> maskWinding : List.of(mask, reverse(mask))) {
                List<Triangle> result = TriangleMeshDifference.subtract(sourceWinding, maskWinding);
                assertEquals(75.0, area(result), 1.0E-6);
                assertCoverage(source, mask, result, -29_999_990, -29_999_990, -29_999_980, -29_999_980);
            }
        }
    }

    @Test
    void doesNotSplitDisjointTrianglesWhoseBoundingBoxesOverlap() {
        Triangle source = triangle(0, 0, 4, 0, 0, 4);
        Triangle mask = triangle(3, 3, 5, 3, 3, 5);

        assertEquals(List.of(source), TriangleMeshDifference.subtract(List.of(source), List.of(mask)));
    }

    @Test
    void keepsDisjointAndTouchingMasksFromCreatingExtraTriangles() {
        Triangle source = triangle(0, 0, 4, 0, 0, 4);
        for (Triangle mask : List.of(triangle(10, 10, 14, 10, 10, 14),
                triangle(4, 0, 4, 4, 0, 4), triangle(4, 0, 5, -1, 5, 1))) {
            List<Triangle> result = TriangleMeshDifference.subtract(List.of(source), List.of(mask));
            assertEquals(8.0, area(result), 1.0E-8);
            assertEquals(1, result.size());
        }
    }

    @Test
    void removesFullyCoveredMeshesAndIgnoresDegenerateMasks() {
        List<Triangle> source = rectangle(0, 0, 4, 4);
        assertTrue(TriangleMeshDifference.subtract(source, source).isEmpty());
        assertTrue(TriangleMeshDifference.subtract(source, rectangle(-1, -1, 5, 5)).isEmpty());
        assertEquals(source, TriangleMeshDifference.subtract(source, List.of(triangle(0, 0, 2, 2, 4, 4))));
    }

    @Test
    void keepsThinRealIntersectionsInsteadOfTreatingThemAsDisjoint() {
        List<Triangle> source = rectangle(0, 0, 10, 10);
        List<Triangle> mask = rectangle(9.999, -1, 11, 11);
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(99.99, area(result), 1.0E-7);
        assertFalse(contains(result, new Point(9.9995, 5.4321)));
    }

    @Test
    void boundsFragmentGrowthForADenseChildBoundary() {
        int count = 128;
        List<Triangle> source = rectangle(-200, -200, 200, 200);
        List<Triangle> mask = circle(count, 100);
        List<Triangle> result = TriangleMeshDifference.subtract(source, mask);

        assertEquals(area(source) - area(mask), area(result), 1.0E-6);
        assertTrue(result.size() < count * 16, "无关遮罩不应把网格扩张为成千上万的碎片: " + result.size());
        assertCoverage(source, mask, result, -200, -200, 200, 200);
    }

    @Test
    void returnsImmutableResultsWithoutMutatingInputsIncludingTheEmptyMaskPath() {
        List<Triangle> source = new ArrayList<>(rectangle(0, 0, 4, 4));
        List<Triangle> original = List.copyOf(source);
        List<Triangle> result = TriangleMeshDifference.subtract(source, List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.add(source.get(0)));
        source.clear();
        assertEquals(original, result);
        assertTrue(TriangleMeshDifference.subtract(List.of(), original).isEmpty());
    }

    @Test
    void honoursCancellationBeforeStartingEvenForAnEmptyMesh() {
        assertThrows(CancellationException.class,
                () -> TriangleMeshDifference.subtract(List.of(), List.of(), () -> true));
    }

    @Test
    void honoursCancellationDuringGeometryWork() {
        AtomicInteger checkpoints = new AtomicInteger();
        assertThrows(CancellationException.class, () -> TriangleMeshDifference.subtract(
                rectangle(-200, -200, 200, 200), circle(128, 100),
                () -> checkpoints.incrementAndGet() >= 700));
        assertEquals(700, checkpoints.get());
    }

    private static Triangle triangle(double ax, double az, double bx, double bz, double cx, double cz) {
        return new Triangle(new Point(ax, az), new Point(bx, bz), new Point(cx, cz));
    }

    private static List<Triangle> rectangle(double minX, double minZ, double maxX, double maxZ) {
        return List.of(triangle(minX, minZ, maxX, minZ, minX, maxZ),
                triangle(maxX, minZ, maxX, maxZ, minX, maxZ));
    }

    private static List<Triangle> circle(int count, double radius) {
        List<Point> vertices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            vertices.add(new Point(Math.cos(angle) * radius, Math.sin(angle) * radius));
        }
        List<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < count - 2; i++) {
            triangles.add(new Triangle(vertices.get(count - 1), vertices.get(i), vertices.get(i + 1)));
        }
        return triangles;
    }

    private static List<Triangle> reverse(List<Triangle> source) {
        return source.stream().map(t -> new Triangle(t.first(), t.third(), t.second())).toList();
    }

    private static double area(List<Triangle> triangles) {
        return triangles.stream().mapToDouble(t -> Math.abs(cross(t.first(), t.second(), t.third())) * 0.5).sum();
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x());
    }

    private static boolean contains(List<Triangle> triangles, Point point) {
        for (Triangle triangle : triangles) {
            double a = cross(triangle.first(), triangle.second(), point);
            double b = cross(triangle.second(), triangle.third(), point);
            double c = cross(triangle.third(), triangle.first(), point);
            if (!(a < -1.0E-8 || b < -1.0E-8 || c < -1.0E-8)
                    || !(a > 1.0E-8 || b > 1.0E-8 || c > 1.0E-8)) {
                return true;
            }
        }
        return false;
    }

    // 非整齐采样偏移避开三角剖分的边和域名边界，仅比较内部覆盖语义。
    private static void assertCoverage(List<Triangle> source, List<Triangle> mask, List<Triangle> result,
                                       double minX, double minZ, double maxX, double maxZ) {
        for (int x = 0; x < 23; x++) {
            for (int z = 0; z < 23; z++) {
                Point point = new Point(minX + (maxX - minX) * (x + 0.314159) / 23,
                        minZ + (maxZ - minZ) * (z + 0.271828) / 23);
                assertEquals(contains(source, point) && !contains(mask, point), contains(result, point), point.toString());
            }
        }
    }
}
