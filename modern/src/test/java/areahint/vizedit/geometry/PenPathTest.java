package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PenPathTest {
    private static Vertex point(double x, double z) { return new Vertex(x, z); }

    private static void assertPoint(Vertex actual, double x, double z) {
        assertEquals(x, actual.getX(), 0.00001);
        assertEquals(z, actual.getZ(), 0.00001);
    }

    @Test
    void straightAnchorsProduceAnOpenStrokeOrAnExactlyFilledPolygon() {
        PenPath path = new PenPath();
        path.add(point(0, 0)); path.add(point(16, 0)); path.add(point(16, 16));
        assertEquals(3, path.flatten(false, null).size());
        assertEquals(4, path.flatten(true, null).size());
        assertFalse(path.stroke(1).contains(12, 4));
        assertEquals(128, VizEditGeometry.measure(path.fill()), 1e-10);
        assertEquals(4, path.flatten(false, point(1, 3)).size());
        assertEquals(3, path.size());
    }

    @Test
    void cornersAndIndependentHandlesSurviveMovingAndUndoSnapshots() {
        PenPath path = new PenPath();
        path.add(point(5, 5));
        path.handle(0, true, point(9, 4), true);
        PenPath snapshot = path.copy();
        path.handle(0, false, point(6, 8), false);
        path.move(0, point(10, 10));
        assertPoint(path.anchors().get(0).in(), 11, 13);
        assertPoint(path.anchors().get(0).out(), 14, 9);
        assertPoint(snapshot.anchors().get(0).in(), 1, 6);
        path.corner(0);
        assertPoint(path.anchors().get(0).in(), 10, 10);
        assertPoint(path.anchors().get(0).out(), 10, 10);
        path.remove(0);
        assertEquals(1, snapshot.size());
    }

    @Test
    void directSelectionRotatesOppositeHandleAtItsOriginalLength() {
        PenPath path = new PenPath();
        path.add(point(10, 10));
        path.handle(0, false, point(7, 10), false);
        path.editSmoothHandle(0, true, point(10, 17), 3);
        assertPoint(path.anchors().get(0).out(), 10, 17);
        assertPoint(path.anchors().get(0).in(), 10, 7);
        path.editSmoothHandle(0, true, point(10, 10), 3);
        assertPoint(path.anchors().get(0).in(), 10, 7);
    }

    @Test
    void splittingCurvedSegmentPreservesItsShapeAndBothNewHandles() {
        PenPath path = new PenPath();
        path.add(point(0, 0)); path.add(point(16, 0));
        path.handle(0, true, point(0, 16), false);
        path.handle(1, false, point(16, 16), false);
        assertEquals(1, path.segmentNear(point(2.5, 9), 0.01));
        assertEquals(-1, path.segmentNear(point(8, 6), 1));
        assertEquals(2, path.size());
        assertEquals(-1, path.insertNear(point(8, 6), 1));
        // 三次曲线在参数 0.25 时经过 (2.5, 9)，两段的控制点可由 de Casteljau 逐层插值精确求得。
        assertEquals(1, path.insertNear(point(2.5, 9), 0.01));
        assertPoint(path.anchors().get(0).out(), 0, 4);
        assertPoint(path.anchors().get(1).point(), 2.5, 9);
        assertPoint(path.anchors().get(1).in(), 1, 7);
        assertPoint(path.anchors().get(1).out(), 7, 15);
        assertPoint(path.anchors().get(2).in(), 16, 12);
        assertEquals(153.6, VizEditGeometry.measure(path.fill()), 1.0);
    }

    @Test
    void insertionOnStraightSegmentKeepsCornersAndSnappingClampsToEndpoints() {
        PenPath path = new PenPath();
        path.add(point(0, 0)); path.add(point(12, 12));
        assertEquals(1, path.insertNear(point(3, 5), 2));
        assertPoint(path.anchors().get(1).out(), 4, 4);
        assertPoint(path.anchors().get(1).in(), 4, 4);
        var edges = List.of(new double[]{0, 0, 10, 10}, new double[]{10, 10, 10, 10});
        assertPoint(PenPath.snap(point(2, 4), edges, 2), 3, 3);
        assertPoint(PenPath.snap(point(11, 12), edges, 3), 10, 10);
        assertPoint(PenPath.snap(point(2, 4), edges, 1), 2, 4);
    }

    @Test
    void selfCrossingPathFailsBeforeAreaCanNormalizeIt() {
        PenPath path = new PenPath();
        path.add(point(0, 0)); path.add(point(8, 8)); path.add(point(8, 0)); path.add(point(0, 8));
        var error = assertThrows(VizEditGeometry.GeometryException.class, path::fill);
        assertEquals("vizedit.error.geometry_self_intersection", error.errorKey());
        assertEquals(4, path.size());
    }

    @Test
    void hoverOutsideWorldLeavesExistingPreviewIntact() {
        PenPath path = new PenPath();
        path.add(point(29_999_990, 0));
        assertEquals(1, path.flatten(false, point(30_000_001, 3)).size());
        assertEquals(1, path.flatten(false, point(Double.NaN, 0)).size());
        assertThrows(VizEditGeometry.GeometryException.class, () -> path.add(point(30_000_001, 3)));
        assertEquals(1, path.size());
    }
}
