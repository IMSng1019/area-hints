package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EraserStrokeTest {
    @Test
    void changingWidthRebuildsEveryAcceptedSegmentEvenAfterABorderFailure() {
        Area original = VizEditGeometry.rectangle(29999985, -5, 30000000, 10, false);
        Vertex a = new Vertex(29999990, 0), b = new Vertex(29999996, 2);
        Vertex c = new Vertex(29999999, 2), d = new Vertex(29999995, 4);
        EraserStroke stroke = new EraserStroke(original);
        stroke.sample(a, 1);
        stroke.sample(b, 1);
        Area wider = stroke.sample(b, 3);
        assertTrue(wider.equals(VizEditGeometry.subtract(original, VizEditGeometry.stroke(List.of(a, b), 3))));
        assertThrows(VizEditGeometry.GeometryException.class, () -> stroke.sample(c, 3));
        assertThrows(VizEditGeometry.GeometryException.class, () -> stroke.sample(d, 3));
        Area recovered = stroke.sample(d, 1);
        assertTrue(recovered.equals(VizEditGeometry.subtract(original, VizEditGeometry.stroke(List.of(a, b, c, d), 1))));
    }

    @Test
    void matchesEveryPreviewWhileCrossingAndRetracingFractionalEdges() {
        Area original = VizEditGeometry.fromVertices(List.of(new Vertex(-7.17, -2.38),
            new Vertex(8.61, -5.72), new Vertex(6.38, 8.51), new Vertex(-5.77, 4.68)));
        Area preserved = new Area(original);
        List<Vertex> samples = List.of(new Vertex(-6.75, -2.8), new Vertex(-6.6, -2.8),
            new Vertex(-6.15, -2.8), new Vertex(5.6, 3.7), new Vertex(-2.1, 6.1),
            new Vertex(2.19, -4.26), new Vertex(5.6, 3.7), new Vertex(-6.75, -2.8));
        for (int width : List.of(1, 4, 9)) {
            EraserStroke stroke = new EraserStroke(original);
            List<Vertex> accepted = new ArrayList<>();
            for (Vertex point : samples) {
                Vertex last = accepted.isEmpty() ? null : accepted.get(accepted.size() - 1);
                if (last == null || Math.abs(point.getX() - last.getX()) + Math.abs(point.getZ() - last.getZ()) >= 0.5)
                    accepted.add(point);
                Area expected = VizEditGeometry.subtract(original, VizEditGeometry.stroke(accepted, width));
                assertTrue(expected.equals(stroke.sample(point, width)));
            }
            assertTrue(original.equals(preserved));
        }
    }

    @Test
    void reusesPreviewUntilTheNextAcceptedCell() {
        Area original = VizEditGeometry.rectangle(0, 0, 5, 5, false);
        EraserStroke stroke = new EraserStroke(original);
        Area first = stroke.sample(new Vertex(0.3, 0.5), 1);
        assertFalse(first.contains(0.5, 0.5));
        assertSame(first, stroke.sample(new Vertex(0.9, 0.5), 1));
        // 同格内达标的采样也更新采样基准，下一次跨格但不足半格时仍应沿用原预览。
        assertSame(first, stroke.sample(new Vertex(1.1, 0.5), 1));
        Area second = stroke.sample(new Vertex(1.5, 0.5), 1);
        assertFalse(second.contains(1.5, 0.5));
        assertTrue(first.contains(1.5, 0.5));
    }

    @Test
    void keepsTheLastValidPreviewWhenBrushFootprintCrossesTheWorldBorder() {
        Area original = VizEditGeometry.rectangle(29999990, 0, 30000000, 10, false);
        EraserStroke stroke = new EraserStroke(original);
        Area valid = stroke.sample(new Vertex(29999995, 3), 4);
        assertThrows(VizEditGeometry.GeometryException.class, () -> stroke.sample(new Vertex(29999999, 3), 4));
        assertThrows(VizEditGeometry.GeometryException.class, () -> stroke.sample(new Vertex(29999993, 3), 4));
        assertTrue(valid.equals(VizEditGeometry.subtract(original,
            VizEditGeometry.stroke(List.of(new Vertex(29999995, 3)), 4))));
    }
}
