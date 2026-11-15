package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VizEditGeometryTest {
    @Test
    void rejectsCrossedAndSelfTouchingPolygons() {
        assertEquals("vizedit.error.geometry_self_intersection", VizEditGeometry.validate(points(0, 0, 4, 4, 0, 4, 4, 0)).errorKey());
        assertFalse(VizEditGeometry.validate(points(0, 0, 4, 0, 4, 4, 2, 0, 0, 4)).valid());
    }

    @Test
    void distinguishesHolesFromSeparateIslandsAndRetainsDraft() {
        Area original = VizEditGeometry.rectangle(0, 0, 12, 12, false);
        Area holed = VizEditGeometry.subtract(original, VizEditGeometry.rectangle(4, 4, 8, 8, false));
        assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(holed).errorKey());
        assertTrue(original.contains(6, 6));
        Area islands = VizEditGeometry.union(original, VizEditGeometry.rectangle(20, 0, 22, 2, false));
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(islands).errorKey());
        assertTrue(holed.contains(1, 1));
        assertFalse(holed.contains(6, 6));
    }

    @Test
    void existingFractionalSlopingEdgesSurviveInteriorPainting() {
        List<Vertex> before = points(0.25, 0.75, 12.125, 1.625, 10.5, 11.25, 0.5, 9.5);
        Area result = VizEditGeometry.union(VizEditGeometry.fromVertices(before), VizEditGeometry.rectangle(3, 3, 5, 5, false));
        var delta = VizEditGeometry.difference(before, VizEditGeometry.toVertices(result));
        assertFalse(delta.added());
        assertFalse(delta.removed());
        for (Vertex expected : before) assertTrue(VizEditGeometry.toVertices(result).stream().anyMatch(v -> v.getX() == expected.getX() && v.getZ() == expected.getZ()));
    }

    @Test
    void negativeCoordinatesSnapByFloorAndThinDiagonalStaysConnected() {
        Area stroke = VizEditGeometry.stroke(points(-0.25, -0.25, 4.8, 4.8), 1);
        assertTrue(stroke.contains(-0.5, -0.5));
        assertTrue(VizEditGeometry.validate(stroke).valid());
        for (var contour : VizEditGeometry.contours(stroke)) for (Vertex v : contour) {
            assertEquals(Math.rint(v.getX()), v.getX());
            assertEquals(Math.rint(v.getZ()), v.getZ());
        }
    }

    @Test
    void ellipseAndConstrainedRectangleUseBlockBoundaries() {
        Area ellipse = VizEditGeometry.ellipse(-4.3, -3.4, 5.4, 6.7, true);
        assertTrue(VizEditGeometry.validate(ellipse).valid());
        for (var contour : VizEditGeometry.contours(ellipse)) for (Vertex v : contour) {
            assertEquals(Math.rint(v.getX()), v.getX());
            assertEquals(Math.rint(v.getZ()), v.getZ());
        }
        var rect = VizEditGeometry.rectangle(1, 2, 4, 9, true).getBounds2D();
        assertEquals(rect.getWidth(), rect.getHeight());
        Vertex constrained = VizEditGeometry.constrainLine(new Vertex(1, 2), new Vertex(7, 5));
        assertEquals(7, constrained.getX());
        assertEquals(2, constrained.getZ());
    }

    @Test
    void differenceRequiresBothPermissionsForEqualAreaMove() {
        var delta = VizEditGeometry.difference(points(0, 0, 10, 0, 10, 10, 0, 10), points(5, 0, 15, 0, 15, 10, 5, 10));
        assertTrue(delta.added());
        assertTrue(delta.removed());
        assertEquals(50, delta.addedArea());
        assertEquals(50, delta.removedArea());
    }

    @Test
    void tinyRealChangesAreNotHiddenByAreaTolerance() {
        var delta = VizEditGeometry.difference(points(0, 0, 1, 0, 1, 1, 0, 1), points(0, 0, 1.0000000001, 0, 1.0000000001, 1, 0, 1));
        assertTrue(delta.added());
        assertFalse(delta.removed());
    }

    @Test
    void onlyExactlyCollinearVerticesAreRemoved() {
        var simple = VizEditGeometry.validate(points(0, 0, 1, 0, 2, 0, 2, 2, 0, 2, 0, 0));
        assertTrue(simple.valid());
        assertEquals(4, simple.vertices().size());
        var bent = VizEditGeometry.validate(points(0, 0, 1, 0.00000000001, 2, 0, 2, 2, 0, 2));
        assertTrue(bent.valid());
        assertEquals(5, bent.vertices().size());
    }

    @Test
    void collinearInputCanBeReducedBelowVertexLimitWithoutLoss() {
        List<Vertex> border = new ArrayList<>();
        for (int i = 0; i < 5000; i++) border.add(new Vertex(i, 0));
        border.add(new Vertex(4999, 2));
        border.add(new Vertex(0, 2));
        var result = VizEditGeometry.validate(border);
        assertTrue(result.valid());
        assertEquals(4, result.vertices().size());
    }

    @Test
    void rejectsExcessiveVerticesNonfiniteCoordinatesAndWorldOverflow() {
        List<Vertex> circle = new ArrayList<>();
        for (int i = 0; i < 4097; i++) circle.add(new Vertex(Math.cos(i * 2 * Math.PI / 4097), Math.sin(i * 2 * Math.PI / 4097)));
        assertEquals("vizedit.error.geometry_vertices", VizEditGeometry.validate(circle).errorKey());
        assertEquals("vizedit.error.geometry_coordinates", VizEditGeometry.validate(points(0, 0, Double.NaN, 0, 0, 1)).errorKey());
        assertEquals("vizedit.error.geometry_coordinates", VizEditGeometry.validate(points(0, 0, 30000001, 0, 0, 1)).errorKey());
        assertThrows(VizEditGeometry.GeometryException.class, () -> VizEditGeometry.stroke(points(0, 0, 10000000, 0), 1));
    }

    @Test
    void magicWandFillsOnlyActualClosedFaceAndUsesIntersections() {
        List<double[]> open = List.of(new double[]{0, 0, 10, 0}, new double[]{10, 0, 10, 10}, new double[]{10, 10, 0, 10});
        assertTrue(VizEditGeometry.magicWand(open, 5, 5).isEmpty());
        List<double[]> closed = new ArrayList<>(open);
        closed.add(new double[]{0, 10, 0, 0});
        closed.add(new double[]{5, -5, 5, 15});
        Area face = VizEditGeometry.magicWand(closed, 2, 3);
        assertTrue(face.contains(2.5, 3.5));
        assertFalse(face.contains(7.5, 3.5));
        assertTrue(VizEditGeometry.magicWand(closed, 20, 20).isEmpty());
    }

    @Test
    void magicWandDoesNotBridgeSmallOpenGap() {
        List<double[]> almost = List.of(new double[]{0, 0, 10, 0}, new double[]{10, 0, 10, 10}, new double[]{10, 10, 0, 10}, new double[]{0, 10, 0, 0.00001});
        assertTrue(VizEditGeometry.magicWand(almost, 5, 5).isEmpty());
    }

    @Test
    void magicWandHandlesRepeatedOverlappingEdgesAndNestedFences() {
        List<double[]> fences = new ArrayList<>(VizEditGeometry.segments(VizEditGeometry.rectangle(0, 0, 20, 20, false)));
        fences.add(new double[]{3, 0, 17, 0});
        fences.add(new double[]{0, 0, 20, 0});
        fences.addAll(VizEditGeometry.segments(VizEditGeometry.rectangle(5, 5, 10, 10, false)));
        Area outside = VizEditGeometry.magicWand(fences, 2, 3);
        assertTrue(outside.contains(2.5, 3.5));
        assertFalse(outside.contains(7.5, 7.5));
        assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(outside).errorKey());
        Area inside = VizEditGeometry.magicWand(fences, 7, 7);
        assertEquals(25, VizEditGeometry.measure(inside));
    }

    @Test
    void magicWandRejectsExcessiveFenceAndRasterWorkBeforeBuildingLargeShapes() {
        List<double[]> excessive = new ArrayList<>();
        for (int i = 0; i < 4097; i++) excessive.add(new double[]{i, 0, i, 1});
        assertEquals("vizedit.error.geometry_limit", assertThrows(VizEditGeometry.GeometryException.class,
            () -> VizEditGeometry.magicWand(excessive, 1, 1)).errorKey());
        List<double[]> huge = List.of(new double[]{0, 0, 1000000, 0}, new double[]{1000000, 0, 1000000, 1000000},
            new double[]{1000000, 1000000, 0, 1000000}, new double[]{0, 1000000, 0, 0});
        assertEquals("vizedit.error.geometry_limit", assertThrows(VizEditGeometry.GeometryException.class,
            () -> VizEditGeometry.magicWand(huge, 1, 1)).errorKey());
    }

    @Test
    void fineDetailsNearWorldBoundaryRemainAndOutwardBrushIsRejected() {
        List<Vertex> polygon = points(29999990, 29999990, 29999995, 29999990.000001, 30000000, 29999990,
            30000000, 30000000, 29999990, 30000000);
        var valid = VizEditGeometry.validate(polygon);
        assertTrue(valid.valid());
        assertEquals(5, valid.vertices().size());
        assertTrue(VizEditGeometry.difference(polygon, points(29999990, 29999990, 30000000, 29999990,
            30000000, 30000000, 29999990, 30000000)).added());
        assertEquals("vizedit.error.geometry_coordinates", assertThrows(VizEditGeometry.GeometryException.class,
            () -> VizEditGeometry.stroke(points(30000000, 0), 1)).errorKey());
    }

    @Test
    void pointTouchAndRetracedEdgesCannotBeSavedAsSinglePolygon() {
        Area touching = VizEditGeometry.union(VizEditGeometry.rectangle(0, 0, 2, 2, false), VizEditGeometry.rectangle(2, 2, 4, 4, false));
        assertFalse(VizEditGeometry.validate(touching).valid());
        assertFalse(VizEditGeometry.validate(points(0, 0, 4, 0, 2, 0, 4, 4, 0, 4)).valid());
        List<Vertex> crossing = points(0, 0, 4, 4, 0, 4, 4, 0);
        assertFalse(VizEditGeometry.validate(crossing).valid());
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(VizEditGeometry.fromVertices(crossing)).errorKey());
    }

    @Test
    void oversizedBooleanResultIsRejectedBeforeReplacingDraft() {
        Path2D.Double many = new Path2D.Double();
        for (int i = 0; i < 14000; i++) {
            double x = i * 2;
            many.moveTo(x, 0); many.lineTo(x + 1, 0); many.lineTo(x + 1, 1); many.lineTo(x, 1); many.closePath();
        }
        Area large = new Area(many);
        Area original = VizEditGeometry.rectangle(0, 0, 1, 1, false);
        assertEquals("vizedit.error.geometry_vertices", assertThrows(VizEditGeometry.GeometryException.class,
            () -> VizEditGeometry.union(original, large)).errorKey());
        assertEquals(1, VizEditGeometry.measure(original));
    }

    @Test
    void boundsFollowExistingSecondaryVertexOrder() {
        List<Vertex> bounds = VizEditGeometry.bounds(points(-4, 1, 5, 2, 3, 9));
        assertEquals(-4, bounds.get(0).getX());
        assertEquals(9, bounds.get(0).getZ());
        assertEquals(5, bounds.get(2).getX());
        assertEquals(1, bounds.get(2).getZ());
    }

    private static List<Vertex> points(double... coordinates) {
        List<Vertex> points = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) points.add(new Vertex(coordinates[i], coordinates[i + 1]));
        return points;
    }
}
