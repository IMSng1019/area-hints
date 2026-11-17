package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AreaContoursTest {
    // 连续扫描行的宽度交替变化，覆盖不同 JDK 在长轮廓内产生水平分段接缝的情况。
    private static Area strips(int rows, double offset) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int row = 0; row < rows; row++) {
            double right = offset + 10 + row % 2 * 2;
            path.moveTo(offset, row);
            path.lineTo(right, row);
            path.lineTo(right, row + 1);
            path.lineTo(offset, row + 1);
            path.closePath();
        }
        return new Area(path);
    }

    @Test
    void adjacentScanlineSectionsProduceOneExactBoundary() {
        for (int rows : List.of(513, 1025)) {
            Area shape = strips(rows, 0);
            var rings = VizEditGeometry.contours(shape);
            assertEquals(1, rings.size(), "rows=" + rows);
            var checked = VizEditGeometry.validate(shape);
            assertTrue(checked.valid(), checked.errorKey());
            Area difference = VizEditGeometry.fromVertices(checked.vertices());
            difference.exclusiveOr(shape);
            assertTrue(difference.isEmpty());
        }
    }

    @Test
    void mergingScanlineSectionsPreservesRealHoles() {
        Area shape = strips(1025, 0);
        shape.subtract(new Area(new Rectangle2D.Double(2, 10, 2, 1000)));
        assertEquals(2, VizEditGeometry.contours(shape).size());
        assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(shape).errorKey());
        assertFalse(shape.contains(3, 512.5));
        assertEquals(11274 - 2000, VizEditGeometry.measure(shape));
    }

    @Test
    void pointContactDoesNotJoinSeparateRegions() {
        Area shape = strips(1025, 0);
        shape.add(new Area(new Rectangle2D.Double(10, 1025, 10, 10)));
        assertEquals(2, VizEditGeometry.contours(shape).size());
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(shape).errorKey());
    }

    @Test
    void narrowRealGapRemainsOpenAfterMergingScanlineSections() {
        Area shape = strips(1025, 0);
        shape.add(strips(1025, 12.00001));
        assertEquals(2, VizEditGeometry.contours(shape).size());
        assertFalse(shape.contains(12.000005, 511.5));
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(shape).errorKey());
    }

    @Test
    void boundaryBrushDoesNotCountRetracedRoundingLinesAsSeparateIslands() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(-4.3, 3.2), new Vertex(-24.2, -17.6), new Vertex(9.2, -28.3)));
        assertTrue(VizEditGeometry.validate(existing).valid());
        var brush = new BoundaryBrush.Stroke(existing, null, List.of(), new BoundaryBrush.State(), null, 6);
        brush.paint(new Vertex(-6, 7));
        Area result = brush.paint(new Vertex(-9, -2)).shape();
        // 跨过旧斜边后曾留下从交点到旧顶点再原路返回的线段，零面积路径不能被计为孤岛。
        var checked = VizEditGeometry.validate(result);
        assertTrue(checked.valid(), checked.errorKey());
        assertEquals(1, VizEditGeometry.contours(result).size());
        assertTrue(result.contains(-9, -2));
        assertTrue(VizEditGeometry.validate(existing).valid());
        assertDoesNotThrow(() -> VizEditGeometry.prepareForSave(result));
        assertTrue(VizEditGeometry.validate(VizEditGeometry.optimize(result, null)).valid());
    }

    @Test
    void microscopicRealIslandsRemainSeparate() {
        Area shape = new Area(new Rectangle2D.Double(0, 0, 4, 4));
        shape.add(new Area(new Rectangle2D.Double(10, 10, 0.00000001, 0.00000001)));
        assertEquals(2, VizEditGeometry.contours(shape).size());
        assertTrue(shape.contains(10.000000005, 10.000000005));
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(shape).errorKey());
    }

    @Test
    void zeroSignedAreaDoesNotDiscardNonCollinearContours() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(26.2, 0), new Vertex(-21.5, 15), new Vertex(-15.2, -17.8)));
        Area parent = new Area(new Rectangle2D.Double(-50, -50, 40, 100));
        var brush = new BoundaryBrush.Stroke(existing, parent, List.of(), new BoundaryBrush.State(), null, 1);
        Area result = brush.paint(new Vertex(0, 0)).shape();
        var rings = VizEditGeometry.contours(result);
        // 自交或舍入轮廓可能正负面积相消，不能仅因面积求和为零就删除仍有非共线点的环。
        assertTrue(rings.stream().anyMatch(ring -> VizEditGeometry.signedArea(ring) == 0 && VizEditGeometry.simplify(ring).size() >= 3));
        assertEquals(2, rings.size());
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(result).errorKey());
    }

    @Test
    void fractionalSlopingEdgesKeepTheirExactGeometry() {
        List<Vertex> points = new ArrayList<>();
        points.add(new Vertex(0.125, 0.25));
        for (int row = 0; row <= 1200; row++) points.add(new Vertex(row % 2 == 0 ? 10.125 : 12.375, row + 0.25));
        points.add(new Vertex(0.125, 1200.25));
        Area original = VizEditGeometry.fromVertices(points);
        var rings = VizEditGeometry.contours(original);
        assertEquals(1, rings.size());
        Area difference = VizEditGeometry.fromVertices(rings.get(0));
        difference.exclusiveOr(original);
        assertTrue(difference.isEmpty());
        assertTrue(VizEditGeometry.validate(rings.get(0)).valid());
    }
}
