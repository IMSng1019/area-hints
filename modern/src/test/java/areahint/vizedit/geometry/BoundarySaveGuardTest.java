package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;
import java.awt.geom.Area;
import static org.junit.jupiter.api.Assertions.*;

class BoundarySaveGuardTest {
    @Test
    void preservesSteppedSharedBoundaryWhenSimplificationCutsTheCorner() {
        Area shape = polygon(0, 0, 12, 0, 12, 4, 11.6, 4, 11.6, 8, 0, 8);
        Area forbidden = polygon(11.6, 4, 12, 4, 12, 8, 11.6, 8);
        Area unguarded = VizEditGeometry.fromVertices(VizEditGeometry.prepareForSave(shape));
        unguarded.intersect(forbidden);
        assertFalse(unguarded.isEmpty(), "This fixture must exercise the quantization-only fallback");
        Area saved = VizEditGeometry.fromVertices(BoundarySaveGuard.prepare(shape, forbidden));
        Area overlap = new Area(saved);
        overlap.intersect(forbidden);
        assertTrue(overlap.isEmpty());
        assertTrue(VizEditGeometry.validate(saved).valid());
    }

    @Test
    void rejectsRoundingAcrossAnExactLimitAndPreservesInput() {
        Area shape = polygon(0, 0, 10.06, 0, 10.06, 8, 0, 8);
        Area before = new Area(shape);
        Area forbidden = polygon(10.06, -2, 20, -2, 20, 12, 10.06, 12);
        var error = assertThrows(VizEditGeometry.GeometryException.class,
            () -> BoundarySaveGuard.prepare(shape, forbidden));
        assertEquals("vizedit.boundary.save_conflict", error.errorKey());
        assertTrue(before.equals(shape));
    }

    @Test
    void ordinarySavingStillRejectsHoles() {
        Area shape = polygon(0, 0, 12, 0, 12, 12, 0, 12);
        shape.subtract(polygon(4, 4, 8, 4, 8, 8, 4, 8));
        assertEquals("vizedit.error.geometry_holes", assertThrows(VizEditGeometry.GeometryException.class,
            () -> BoundarySaveGuard.prepare(shape, new Area())).errorKey());
    }

    @Test
    void manualPaintingMayCrossAnOldGuideButOptimizationCannotExtendThatCrossing() {
        Area before = polygon(0, 0, 12, 0, 12, 8, 0, 8);
        Area forbidden = polygon(10, -2, 20, -2, 20, 12, 10, 12);
        assertTrue(BoundarySaveGuard.allows(new Area(before), before, forbidden));
        assertFalse(BoundarySaveGuard.allows(polygon(0, 0, 13, 0, 13, 8, 0, 8), before, forbidden));
    }

    @Test
    void constrainedCornerDoesNotPreventReducingTheRestOfASteppedCoast() {
        java.util.ArrayList<Vertex> vertices = new java.util.ArrayList<>(java.util.List.of(
            new Vertex(0, 0), new Vertex(80, 0), new Vertex(80, 20),
            new Vertex(79.6, 20), new Vertex(79.6, 24), new Vertex(60, 24)));
        for (int i = 0; i < 60; i++) {
            vertices.add(new Vertex(60 - i, 25 + i));
            vertices.add(new Vertex(59 - i, 25 + i));
        }
        Area shape = VizEditGeometry.fromVertices(vertices);
        Area forbidden = polygon(-5, -5, 85, -5, 85, 90, -5, 90);
        forbidden.subtract(shape);
        Area unguarded = VizEditGeometry.fromVertices(VizEditGeometry.prepareForSave(shape));
        unguarded.intersect(forbidden);
        assertFalse(unguarded.isEmpty(), "This fixture must contain a shortcut that crosses the limit");

        var saved = BoundarySaveGuard.prepare(shape, forbidden);
        Area result = VizEditGeometry.fromVertices(saved);
        result.intersect(forbidden);
        assertTrue(result.isEmpty());
        assertTrue(saved.size() < vertices.size() / 3,
            "A protected local corner must not force all safe staircase vertices to be kept: " + saved.size());
        assertTrue(VizEditGeometry.validate(saved).valid());
        for (Vertex point : saved) {
            assertEquals(Math.rint(point.getX() * 10), point.getX() * 10, 1e-9);
            assertEquals(Math.rint(point.getZ() * 10), point.getZ() * 10, 1e-9);
        }
    }

    private static Area polygon(double... values) {
        java.util.ArrayList<Vertex> vertices = new java.util.ArrayList<>();
        for (int i = 0; i < values.length; i += 2) vertices.add(new Vertex(values[i], values[i + 1]));
        return VizEditGeometry.fromVertices(vertices);
    }
}
