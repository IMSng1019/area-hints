package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeometryIssueLocatorTest {
    private static Area rectangle(double x, double z, double width, double height) {
        return new Area(new Rectangle2D.Double(x, z, width, height));
    }

    @Test
    void focusesLargestHoleWithoutChangingDraft() {
        Area shape = rectangle(0, 0, 100, 100);
        shape.subtract(rectangle(10, 12, 2, 2));
        shape.subtract(rectangle(40, 50, 8, 6));
        Area before = new Area(shape);
        var issue = GeometryIssueLocator.locate(shape, "vizedit.error.geometry_holes");
        assertNotNull(issue);
        assertEquals(new Rectangle2D.Double(40, 50, 8, 6), issue.bounds());
        assertEquals(44, issue.x());
        assertEquals(53, issue.z());
        assertTrue(before.equals(shape));
    }

    @Test
    void retainsMicroscopicHoleAndIslandLocations() {
        Area shape = rectangle(0, 0, 10, 10);
        shape.subtract(rectangle(5, 6, 0.00000001, 0.00000001));
        var hole = GeometryIssueLocator.locate(shape, "vizedit.error.geometry_holes");
        assertNotNull(hole);
        assertEquals(5, hole.bounds().getMinX());
        assertTrue(hole.bounds().getWidth() > 0);
        Area island = rectangle(20, 30, 0.00000001, 0.00000001);
        shape = rectangle(0, 0, 10, 10);
        shape.add(island);
        var issue = GeometryIssueLocator.locate(shape, "vizedit.error.geometry_disconnected");
        assertNotNull(issue);
        assertEquals(island.getBounds2D(), issue.bounds());
    }

    @Test
    void focusesExtraIslandInsteadOfLargestMainland() {
        Area shape = rectangle(100, 100, 3, 5);
        shape.add(rectangle(-50, -60, 100, 100));
        var issue = GeometryIssueLocator.locate(shape, "vizedit.error.geometry_disconnected");
        assertNotNull(issue);
        assertEquals(new Rectangle2D.Double(100, 100, 3, 5), issue.bounds());
        assertEquals(101.5, issue.x());
        assertEquals(102.5, issue.z());
    }

    @Test
    void concaveHoleAndIslandMarkersStayOnTheActualProblemRegion() {
        List<Vertex> outline = List.of(new Vertex(20, 20), new Vertex(26, 20), new Vertex(26, 22),
            new Vertex(22, 22), new Vertex(22, 26), new Vertex(20, 26));
        Area concave = VizEditGeometry.fromVertices(outline);
        Area holed = rectangle(0, 0, 100, 100);
        holed.subtract(concave);
        Area disconnected = rectangle(-100, -100, 20, 20);
        disconnected.add(concave);
        for (var issue : List.of(GeometryIssueLocator.locate(holed, "vizedit.error.geometry_holes"),
                GeometryIssueLocator.locate(disconnected, "vizedit.error.geometry_disconnected"))) {
            assertFalse(concave.contains(issue.bounds().getCenterX(), issue.bounds().getCenterY()));
            assertTrue(concave.contains(issue.x(), issue.z()) || onBoundary(outline, issue.x(), issue.z()));
            assertEquals(concave.getBounds2D(), issue.bounds());
        }
    }

    private static boolean onBoundary(List<Vertex> ring, double x, double z) {
        Vertex point = new Vertex(x, z);
        for (int i = 0; i < ring.size(); i++) {
            Vertex a = ring.get(i), b = ring.get((i + 1) % ring.size());
            if (VizEditGeometry.orientation(a, b, point) == 0 && x >= Math.min(a.getX(), b.getX()) && x <= Math.max(a.getX(), b.getX())
                    && z >= Math.min(a.getZ(), b.getZ()) && z <= Math.max(a.getZ(), b.getZ())) return true;
        }
        return false;
    }

    @Test
    void locatesActualSelfContactOfNormalizedCrossing() {
        Area shape = VizEditGeometry.fromVertices(List.of(new Vertex(0, 0), new Vertex(10, 10),
            new Vertex(0, 10), new Vertex(10, 0)));
        var issue = GeometryIssueLocator.locate(shape, "vizedit.error.geometry_self_intersection");
        assertNotNull(issue);
        assertEquals(5, issue.x(), 0.000000001);
        assertEquals(5, issue.z(), 0.000000001);
    }

    @Test
    void ignoresExactlyDegenerateDifferenceInsteadOfInventingAnIsland() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(17.7, 0), new Vertex(-2.8, 4.9), new Vertex(-9.6, -16.6)));
        Area paint = VizEditGeometry.stroke(List.of(new Vertex(-7, 4), new Vertex(24, -13)), 8);
        Area difference = VizEditGeometry.subtract(existing, VizEditGeometry.union(existing, paint));
        assertNull(GeometryIssueLocator.locate(difference, "vizedit.error.geometry_disconnected"));
    }

    @Test
    void locatesUnclassifiedNonCollinearRingWhenDoubleAreaCancelsToZero() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(26.2, 0), new Vertex(-21.5, 15), new Vertex(-15.2, -17.8)));
        var brush = new BoundaryBrush.Stroke(existing, rectangle(-50, -50, 40, 100), List.of(), new BoundaryBrush.State(), null, 1);
        Area result = brush.paint(new Vertex(0, 0)).shape();
        assertEquals("vizedit.error.geometry_disconnected", VizEditGeometry.validate(result).errorKey());
        var ambiguous = VizEditGeometry.contours(result).stream().filter(ring -> VizEditGeometry.signedArea(ring) == 0).findFirst().orElseThrow();
        var issue = GeometryIssueLocator.locate(result, "vizedit.error.geometry_disconnected");
        assertNotNull(issue);
        assertTrue(onBoundary(ambiguous, issue.x(), issue.z()));
        assertNull(GeometryIssueLocator.locate(result, "vizedit.error.geometry_holes"));
    }

    @Test
    void aRealHoleAloneIsNotReportedAsADisconnectedIsland() {
        Area shape = rectangle(0, 0, 20, 20);
        shape.subtract(rectangle(8, 8, 2, 2));
        assertNull(GeometryIssueLocator.locate(shape, "vizedit.error.geometry_disconnected"));
        assertNotNull(GeometryIssueLocator.locate(shape, "vizedit.error.geometry_holes"));
    }

    @Test
    void boundsAreDefensiveCopies() {
        var bounds = new Rectangle2D.Double(1, 2, 3, 4);
        var issue = new GeometryIssueLocator.Issue(bounds, 2.5, 4);
        bounds.setRect(0, 0, 100, 100);
        issue.bounds().setRect(0, 0, 200, 200);
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), issue.bounds());
    }

    @Test
    void validShapesAndUnsupportedErrorsHaveNoInventedLocation() {
        Area shape = rectangle(0, 0, 10, 10);
        for (String key : List.of("vizedit.error.geometry_holes", "vizedit.error.geometry_disconnected",
                "vizedit.error.geometry_self_intersection", "vizedit.boundary.save_conflict"))
            assertNull(GeometryIssueLocator.locate(shape, key));
        assertNull(GeometryIssueLocator.locate(null, "vizedit.error.geometry_holes"));
        assertNull(GeometryIssueLocator.locate(shape, null));
    }

    @Test
    void largeValidOutlineDoesNotProduceAnIntersectionLocation() {
        List<Vertex> vertices = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            double angle = i * Math.PI * 2 / 20000;
            vertices.add(new Vertex(Math.cos(angle) * 1000, Math.sin(angle) * 1000));
        }
        assertNull(GeometryIssueLocator.locate(VizEditGeometry.fromVertices(vertices), "vizedit.error.geometry_self_intersection"));
    }
}
