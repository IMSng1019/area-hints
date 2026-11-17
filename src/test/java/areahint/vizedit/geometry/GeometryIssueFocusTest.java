package areahint.vizedit.geometry;

import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

class GeometryIssueFocusTest {
    private static Area shapeWithHole() {
        Area shape = new Area(new Rectangle2D.Double(0, 0, 20, 20));
        shape.subtract(new Area(new Rectangle2D.Double(8, 8, 2, 2)));
        return shape;
    }

    @Test
    void consumesAutomaticFocusOnlyOnceWhileKeepingMarker() {
        var focus = new GeometryIssueFocus();
        Area shape = shapeWithHole();
        var issue = focus.report(shape, "vizedit.error.geometry_holes");
        assertNotNull(issue);
        assertSame(issue, focus.consume(shape));
        assertNull(focus.consume(shape));
        assertSame(issue, focus.current(shape));
    }

    @Test
    void replacingDraftWithEqualUndoCopyClearsFocusAndMarker() {
        var focus = new GeometryIssueFocus();
        Area original = shapeWithHole();
        assertNotNull(focus.report(original, "vizedit.error.geometry_holes"));
        Area restored = new Area(original);
        assertTrue(original.equals(restored));
        assertNull(focus.current(restored));
        assertNull(focus.consume(restored));
        assertNull(focus.current(original));
    }

    @Test
    void consumingWithReplacedDraftAlsoClearsStaleIssue() {
        var focus = new GeometryIssueFocus();
        Area original = shapeWithHole();
        assertNotNull(focus.report(original, "vizedit.error.geometry_holes"));
        assertNull(focus.consume(new Area(original)));
        assertNull(focus.current(original));
    }

    @Test
    void unrelatedErrorClearsPreviousIssueWithoutChangingDraft() {
        var focus = new GeometryIssueFocus();
        Area shape = shapeWithHole(), before = new Area(shape);
        assertNotNull(focus.report(shape, "vizedit.error.geometry_holes"));
        assertNull(focus.report(shape, "vizedit.boundary.save_conflict"));
        assertNull(focus.current(shape));
        assertNull(focus.consume(shape));
        assertTrue(before.equals(shape));
    }

    @Test
    void reportingAgainAllowsAnotherFocusAndClearRemovesIt() {
        var focus = new GeometryIssueFocus();
        Area shape = shapeWithHole();
        focus.report(shape, "vizedit.error.geometry_holes");
        assertNotNull(focus.consume(shape));
        var next = focus.report(shape, "vizedit.error.geometry_holes");
        assertSame(next, focus.consume(shape));
        focus.clear();
        assertNull(focus.current(shape));
        assertNull(focus.consume(shape));
    }
}
