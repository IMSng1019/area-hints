package areahint.map;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlueMapPolygonGeometryTest {
    @Test
    void closingVertexDoesNotMoveTheMarkerPosition() {
        List<Vertex> open = List.of(new Vertex(10, 10), new Vertex(20, 10),
            new Vertex(20, 20), new Vertex(10, 20));
        List<Vertex> closed = new ArrayList<>(open);
        closed.add(new Vertex(10, 10));

        var expected = BlueMapPolygonGeometry.prepare(open);
        var actual = BlueMapPolygonGeometry.prepare(closed);

        assertEquals(15.0, actual.centerX());
        assertEquals(15.0, actual.centerZ());
        assertEquals(expected, actual);
        assertEquals(5, closed.size());
    }

    @Test
    void ignoresNullVerticesBeforeClosingAndCenterCalculation() {
        List<Vertex> vertices = new ArrayList<>();
        vertices.add(null);
        vertices.add(new Vertex(-30, -20));
        vertices.add(new Vertex(-10, -20));
        vertices.add(null);
        vertices.add(new Vertex(-20, -5));
        vertices.add(new Vertex(-30, -20));
        vertices.add(null);

        var polygon = BlueMapPolygonGeometry.prepare(vertices);

        assertEquals(3, polygon.vertices().size());
        assertEquals(-20.0, polygon.centerX());
        assertEquals(-15.0, polygon.centerZ());
        assertEquals(7, vertices.size());
    }

    @Test
    void rejectsPolygonWithTooFewVerticesAfterRemovingClosure() {
        assertThrows(IllegalArgumentException.class, () -> BlueMapPolygonGeometry.prepare(
            List.of(new Vertex(10, 10), new Vertex(20, 20), new Vertex(10, 10))));
    }
}
