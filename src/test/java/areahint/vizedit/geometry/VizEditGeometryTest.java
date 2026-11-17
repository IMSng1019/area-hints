package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.time.Duration;
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
    void largeSelectionsAreNotLimitedByTheirCoordinateSpan() {
        Area rectangle = VizEditGeometry.rectangle(-30000000, -30000000, 30000000, 30000000, false);
        assertEquals(3600000000000000d, VizEditGeometry.measure(rectangle));
        assertTrue(VizEditGeometry.validate(rectangle).valid());
        assertTrue(VizEditGeometry.ellipse(-2500, -2500, 2500, 2500, false).contains(0, 0));
        assertTrue(VizEditGeometry.stroke(points(-2500, 0, 2500, 0), 1).contains(2499.5, 0.5));
    }

    @Test
    void veryTallNarrowEllipseBatchesRowsWithoutChangingBlockCenterSampling() {
        Area narrow = assertTimeout(Duration.ofSeconds(5), () -> VizEditGeometry.ellipse(0, -5000000, 4, 5000000, false));
        assertTrue(narrow.contains(1.5, 0.5));
        assertFalse(narrow.contains(-0.5, 0.5));
        for (int height : List.of(13, 401)) {
            var ellipse = new java.awt.geom.Ellipse2D.Double(-5, -height / 2, 10, height);
            Area snapped = BlockSnap.snap(new Area(ellipse));
            Path2D.Double flattenedPath = new Path2D.Double();
            flattenedPath.append(new Area(ellipse).getPathIterator(null, 0.125), false);
            var flattened = new Area(flattenedPath);
            for (int z = -height / 2 - 1; z <= height / 2 + 1; z++) for (int x = -6; x <= 6; x++)
                assertEquals(flattened.contains(x + 0.5, z + 0.5), snapped.contains(x + 0.5, z + 0.5));
        }
    }

    @Test
    void additivePenFillsOnlyNewlyEnclosedVoidsAcrossStrokes() {
        Area open = VizEditGeometry.stroke(points(0, 0, 10, 0, 10, 10, 0, 10), 1);
        assertFalse(VizEditGeometry.fillNewlyEnclosed(new Area(), open).contains(5.5, 5.5));
        Area closed = VizEditGeometry.union(open, VizEditGeometry.stroke(points(0, 10, 0, 0), 1));
        Area filled = VizEditGeometry.fillNewlyEnclosed(open, closed);
        assertTrue(filled.contains(5.5, 5.5));
        assertTrue(VizEditGeometry.validate(filled).valid());
        assertFalse(open.contains(5.5, 5.5));
        assertFalse(closed.contains(5.5, 5.5));
        Area oldHole = VizEditGeometry.subtract(VizEditGeometry.rectangle(20, 0, 30, 10, false),
            VizEditGeometry.rectangle(23, 3, 27, 7, false));
        Area before = VizEditGeometry.union(oldHole, open);
        Area result = VizEditGeometry.fillNewlyEnclosed(before, VizEditGeometry.union(oldHole, closed));
        assertTrue(result.contains(5.5, 5.5));
        assertFalse(result.contains(25.5, 5.5));
    }

    @Test
    void fillingAgainstFractionalSlopesDoesNotLeaveMicroscopicHoles() {
        List<List<Vertex>> originals = List.of(points(0.8, 0.2, 10.3, 3.1, 5, 10),
            points(0.7, 0.9, 10.3, 3.1, 8.8, 10.7, -2.7, 7.7));
        List<List<Vertex>> strokes = List.of(points(5, 5, 5, 0, 3, 0), points(9, 4, 5, 0, 9, 6));
        for (int i = 0; i < originals.size(); i++) {
            Area before = VizEditGeometry.fromVertices(originals.get(i));
            Area painted = VizEditGeometry.union(before, VizEditGeometry.stroke(strokes.get(i), 1));
            assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(painted).errorKey());
            Area filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
            var checked = VizEditGeometry.validate(filled);
            assertTrue(checked.valid(), checked.errorKey());
            assertEquals(1, VizEditGeometry.contours(filled).size());
            assertTrue(VizEditGeometry.subtract(painted, filled).isEmpty());
            assertDoesNotThrow(() -> VizEditGeometry.prepareForSave(filled));
            assertTrue(VizEditGeometry.validate(VizEditGeometry.optimize(filled, null)).valid());
        }
    }

    @Test
    void successiveBrushStrokesFillVoidsExposedByRebuildingOptimizedEdges() {
        Area empty = new Area();
        Area first = VizEditGeometry.fillNewlyEnclosed(empty,
            VizEditGeometry.stroke(points(68, 41, 75, 63, 88, 69, 79, 73, 81, 68), 4));
        Area before = VizEditGeometry.optimize(first, empty);
        assertTrue(VizEditGeometry.validate(before).valid());
        List<Vertex> second = points(74, 63, 71, 75, 81, 64);
        Area filled = before;
        for (int count = 1; count <= second.size(); count++) {
            Area painted = VizEditGeometry.union(before, VizEditGeometry.stroke(second.subList(0, count), 1));
            filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
        }
        var checked = VizEditGeometry.validate(filled);
        assertTrue(checked.valid(), checked.errorKey());
        assertTrue(filled.contains(78.1, 69.2));
        Area completed = VizEditGeometry.optimize(filled, before);
        assertTrue(VizEditGeometry.validate(completed).valid());
        assertDoesNotThrow(() -> VizEditGeometry.prepareForSave(completed));
    }

    @Test
    void closingNewVoidsLeavesAnExistingFractionalHoleExact() {
        Area empty = new Area();
        Area first = VizEditGeometry.fillNewlyEnclosed(empty,
            VizEditGeometry.stroke(points(68, 41, 75, 63, 88, 69, 79, 73, 81, 68), 4));
        Area before = VizEditGeometry.optimize(first, empty);
        Area oldDomain = VizEditGeometry.rectangle(100, 40, 110, 80, false);
        oldDomain.subtract(VizEditGeometry.fromVertices(points(102.25, 45.125, 107.375, 46.25, 105.5, 75.875)));
        before.add(oldDomain);
        Area painted = VizEditGeometry.union(before, VizEditGeometry.stroke(points(74, 63, 71, 75, 81, 64), 1));
        Area filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
        Area preserved = new Area(filled);
        preserved.intersect(VizEditGeometry.rectangle(100, 40, 110, 80, false));
        assertTrue(preserved.equals(oldDomain));
        assertTrue(filled.contains(78.1, 69.2));
        assertEquals(1, VizEditGeometry.contours(filled).stream().filter(ring -> VizEditGeometry.signedArea(ring) > 0).count());
    }

    @Test
    void fillingNewVoidKeepsTheUnpaintedPartOfAnOldHole() {
        Area before = VizEditGeometry.subtract(VizEditGeometry.rectangle(0, 0, 12, 12, false),
            VizEditGeometry.rectangle(4, 4, 8, 8, false));
        Area open = VizEditGeometry.stroke(points(20, 0, 30, 0, 30, 10, 20, 10), 1);
        before.add(open);
        Area painted = VizEditGeometry.union(before, VizEditGeometry.rectangle(4, 4, 6, 8, false));
        painted.add(VizEditGeometry.stroke(points(20, 10, 20, 0), 1));
        Area filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
        assertTrue(filled.contains(25.5, 5.5));
        assertTrue(filled.contains(5, 5));
        assertFalse(filled.contains(7, 5));
        assertFalse(before.contains(5, 5));
        assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(filled).errorKey());
    }

    @Test
    void fillingDoesNotEraseOldHolesInsideNestedIslands() {
        Area before = VizEditGeometry.rectangle(0, 0, 100, 100, false);
        before.subtract(VizEditGeometry.rectangle(10, 10, 90, 90, false));
        before.add(VizEditGeometry.rectangle(20, 20, 80, 80, false));
        before.subtract(VizEditGeometry.rectangle(30, 30, 70, 70, false));
        Area painted = VizEditGeometry.union(before, VizEditGeometry.rectangle(25, 25, 45, 45, false));
        Area filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
        assertTrue(filled.equals(painted));
        assertFalse(filled.contains(15, 15));
        assertFalse(filled.contains(50, 50));
        assertTrue(filled.contains(35, 35));
    }

    @Test
    void fillingNewVoidPreservesEvenMicroscopicOldHoles() {
        Area before = VizEditGeometry.rectangle(0, 0, 4, 4, false);
        before.subtract(VizEditGeometry.fromVertices(points(1, 1, 1.00000001, 1, 1.00000001, 1.00000001, 1, 1.00000001)));
        before.add(VizEditGeometry.stroke(points(10, 0, 20, 0, 20, 10, 10, 10), 1));
        Area painted = VizEditGeometry.union(before, VizEditGeometry.stroke(points(10, 10, 10, 0), 1));
        Area filled = VizEditGeometry.fillNewlyEnclosed(before, painted);
        assertTrue(filled.contains(15.5, 5.5));
        assertFalse(filled.contains(1.000000005, 1.000000005));
        assertEquals("vizedit.error.geometry_holes", VizEditGeometry.validate(filled).errorKey());
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
    void arbitrarilyLongCollinearInputIsReducedWithoutLoss() {
        List<Vertex> border = new ArrayList<>();
        for (int i = 0; i < 70000; i++) border.add(new Vertex(i, 0));
        border.add(new Vertex(69999, 2));
        border.add(new Vertex(0, 2));
        var result = VizEditGeometry.validate(border);
        assertTrue(result.valid());
        assertEquals(4, result.vertices().size());
        assertEquals(139998, VizEditGeometry.measure(VizEditGeometry.fromVertices(border)));
    }

    @Test
    void rejectsNonfiniteCoordinatesWorldOverflowAndInvalidBrushWidth() {
        assertEquals("vizedit.error.geometry_coordinates", VizEditGeometry.validate(points(0, 0, Double.NaN, 0, 0, 1)).errorKey());
        assertEquals("vizedit.error.geometry_coordinates", VizEditGeometry.validate(points(0, 0, 30000001, 0, 0, 1)).errorKey());
        assertThrows(VizEditGeometry.GeometryException.class, () -> VizEditGeometry.stroke(points(0, 0), 257));
        assertThrows(VizEditGeometry.GeometryException.class, () -> VizEditGeometry.stroke(points(0, 0), Double.NaN));
    }

    @Test
    void detailedBoundaryCanBeValidatedAndSavedAboveTheFormerVertexLimit() {
        List<Vertex> border = teeth(5000);
        assertTrue(VizEditGeometry.validate(border).valid());
        List<Vertex> saved = VizEditGeometry.prepareForSave(VizEditGeometry.fromVertices(border));
        assertTrue(saved.size() > 4096);
        assertTrue(VizEditGeometry.validate(saved).valid());
        assertTrue(saved.stream().anyMatch(point -> point.getX() == 2500 && point.getZ() == 0));
        assertTrue(saved.stream().anyMatch(point -> point.getX() == 2501 && point.getZ() == 5));
    }

    @Test
    void longAndDenselySampledStrokesHaveNoFixedPathLimit() {
        Area longStroke = VizEditGeometry.stroke(points(0, 0, 70000, 0), 1);
        assertTrue(longStroke.contains(69999.5, 0.5));
        assertEquals(70001, VizEditGeometry.measure(longStroke));
        List<Vertex> samples = new ArrayList<>();
        for (int i = 0; i < 8193; i++) samples.add(new Vertex(0, 0));
        assertEquals(1, VizEditGeometry.measure(VizEditGeometry.stroke(samples, 1)));
    }

    @Test
    void wideStrokeCanExceedTheFormerSpanBudget() {
        Area stroke = VizEditGeometry.stroke(points(0, 0, 4100, 0), 256);
        assertTrue(stroke.contains(4099.5, 0.5));
        assertTrue(stroke.contains(2000.5, 127.5));
        assertFalse(stroke.contains(2000.5, 129.5));
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
    void magicWandAcceptsManyFenceEdgesAndLargeSimpleFaces() {
        List<double[]> many = new ArrayList<>(VizEditGeometry.segments(VizEditGeometry.rectangle(0, 0, 10, 10, false)));
        for (int i = 0; i < 4097; i++) many.add(new double[]{100 + i, 0, 100 + i, 1});
        assertEquals(100, VizEditGeometry.measure(VizEditGeometry.magicWand(many, 1, 1)));
        List<double[]> huge = List.of(new double[]{0, 0, 1000000, 0}, new double[]{1000000, 0, 1000000, 1000000},
            new double[]{1000000, 1000000, 0, 1000000}, new double[]{0, 1000000, 0, 0});
        Area selected = assertTimeout(Duration.ofSeconds(5), () -> VizEditGeometry.magicWand(huge, 1, 1));
        assertEquals(1000000000000d, VizEditGeometry.measure(selected));
        assertTrue(VizEditGeometry.validate(selected).valid());
    }

    @Test
    void magicWandSplitsDenseFenceIntersectionsWithoutAComplexityLimit() {
        List<double[]> grid = new ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            grid.add(new double[]{i, 0, i, 100});
            grid.add(new double[]{0, i, 100, i});
        }
        assertEquals(1, VizEditGeometry.measure(VizEditGeometry.magicWand(grid, 50, 50)));
    }

    @Test
    void magicWandRasterizesLargeSlopingFacesWithoutARowLimit() {
        List<double[]> triangle = List.of(new double[]{0, 0, 40000, 0}, new double[]{40000, 0, 0, 40000}, new double[]{0, 40000, 0, 0});
        Area selected = VizEditGeometry.magicWand(triangle, 1, 1);
        assertEquals(40000d * 39999 / 2, VizEditGeometry.measure(selected));
        assertEquals(1, VizEditGeometry.contours(selected).size());
        assertTrue(VizEditGeometry.contours(selected).get(0).size() > 65536);
        assertTrue(VizEditGeometry.validate(selected).valid());
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
    void largeBooleanDraftsRetainAllContoursAndTheOriginalSnapshot() {
        Path2D.Double many = new Path2D.Double();
        for (int i = 0; i < 14000; i++) {
            double x = i * 2;
            many.moveTo(x, 0); many.lineTo(x + 1, 0); many.lineTo(x + 1, 1); many.lineTo(x, 1); many.closePath();
        }
        Area large = new Area(many);
        Area original = VizEditGeometry.rectangle(0, 0, 1, 1, false);
        Area combined = VizEditGeometry.union(original, large);
        assertEquals(14000, VizEditGeometry.measure(combined));
        assertEquals(14000, VizEditGeometry.contours(combined).size());
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(combined).errorKey());
        assertEquals(1, VizEditGeometry.measure(original));
    }

    @Test
    void savingWithComplexForbiddenAreasCanFallBackToTheRoundedBoundary() {
        Path2D.Double restrictions = new Path2D.Double();
        for (int i = 0; i < 14000; i++) {
            double x = i * 2;
            restrictions.moveTo(x, 20); restrictions.lineTo(x + 1, 20);
            restrictions.lineTo(x + 1, 21); restrictions.lineTo(x, 21); restrictions.closePath();
        }
        List<Vertex> original = teeth(5000);
        List<Vertex> saved = VizEditGeometry.prepareForSave(VizEditGeometry.fromVertices(original), new Area(restrictions));
        assertEquals(original.size(), saved.size());
        assertTrue(VizEditGeometry.validate(saved).valid());
        for (Vertex point : saved) {
            assertEquals(Math.rint(point.getX() * 10), point.getX() * 10);
            assertEquals(Math.rint(point.getZ() * 10), point.getZ() * 10);
        }
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

    private static List<Vertex> teeth(int count) {
        List<Vertex> result = new ArrayList<>();
        for (int i = 0; i < count; i++) result.add(new Vertex(i, i % 2 == 0 ? 0 : 5));
        result.add(new Vertex(count - 1, -10));
        result.add(new Vertex(0, -10));
        return result;
    }
}
