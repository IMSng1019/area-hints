package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryBrushTest {
    private static final BoundaryBrush.Terrain COAST = (x, z) -> x < 0 ? BoundaryBrush.LAND : BoundaryBrush.WATER;
    private static final BoundaryBrush.Terrain RIVER = (x, z) -> x < 0 || x >= 6 ? BoundaryBrush.LAND : BoundaryBrush.WATER;

    private static Area rectangle(double x0, double z0, double x1, double z1) {
        return VizEditGeometry.rectangle(x0, z0, x1, z1, false);
    }

    private static BoundaryBrush.Stroke stroke(Area existing, BoundaryBrush.State state, BoundaryBrush.Terrain terrain, double width) {
        return new BoundaryBrush.Stroke(existing, null, List.of(), state, terrain, width);
    }

    @Test
    void paintingAcrossExistingFractionalEdgeDoesNotCreateMicroscopicHoles() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(17.7, 0), new Vertex(-2.8, 4.9), new Vertex(-9.6, -16.6)));
        var brush = stroke(existing, new BoundaryBrush.State(), null, 8);
        brush.paint(new Vertex(-7, 4));
        var result = brush.paint(new Vertex(24, -13));
        // 旧斜边被拆下再拼回时曾产生约4.55e-16平方格的伪洞，阻止保存和边界简化。
        var validation = VizEditGeometry.validate(result.shape());
        assertTrue(validation.valid(), validation.errorKey());
        assertEquals(1, VizEditGeometry.contours(result.shape()).size());
        assertTrue(result.shape().contains(0, 0));
        assertTrue(result.shape().contains(24, -13));
        assertDoesNotThrow(() -> BoundarySaveGuard.prepare(result.shape(), result.state().forbidden()));
    }

    @Test
    void paintingInsideExistingFractionalBoundaryPreservesAreaOutsideParent() {
        Area existing = VizEditGeometry.fromVertices(List.of(new Vertex(24, 0), new Vertex(-18.2, 24.5), new Vertex(-20.9, -28.1)));
        var brush = new BoundaryBrush.Stroke(existing, rectangle(-50, -50, -10, 50), List.of(), new BoundaryBrush.State(), null, 1);
        var result = brush.paint(new Vertex(0, 0));
        // 完全落在旧域名内部的一笔不应因父域名裁剪重算旧斜边，已有越界部分也须精确保留。
        assertTrue(existing.equals(result.shape()));
        assertTrue(VizEditGeometry.validate(result.shape()).valid());
    }

    @Test
    void closedStrokeFillsItsInteriorWithoutClosingAnOpenStroke() {
        var brush = stroke(new Area(), new BoundaryBrush.State(), null, 1);
        brush.paint(new Vertex(0, 0));
        brush.paint(new Vertex(10, 0));
        brush.paint(new Vertex(10, 10));
        var open = brush.paint(new Vertex(0, 10));
        assertFalse(open.shape().contains(5.5, 5.5));
        var closed = brush.paint(new Vertex(0, 0));
        assertTrue(closed.shape().contains(5.5, 5.5));
        assertTrue(VizEditGeometry.validate(closed.shape()).valid());
    }

    @Test
    void automaticInteriorFillRespectsPeersAndParentAcrossTheWholeInterior() {
        var brush = new BoundaryBrush.Stroke(new Area(), rectangle(-1, -1, 11, 11),
            List.of(rectangle(4, 4, 7, 7)), new BoundaryBrush.State(), null, 1);
        brush.paint(new Vertex(0, 0));
        brush.paint(new Vertex(12, 0));
        brush.paint(new Vertex(12, 12));
        brush.paint(new Vertex(0, 12));
        var closed = brush.paint(new Vertex(0, 0));
        assertFalse(closed.shape().contains(12.5, 5.5));
        assertFalse(closed.shape().contains(5.5, 5.5));
        assertTrue(closed.shape().contains(2.5, 5.5));
        assertTrue(closed.state().forbidden().contains(5.5, 5.5));
    }

    @Test
    void closedBoundaryStrokeKeepsPreviouslyErasedInterior() {
        Area existing = rectangle(2, 2, 8, 8);
        existing.subtract(rectangle(4, 4, 6, 6));
        var brush = stroke(existing, new BoundaryBrush.State(), null, 1);
        brush.paint(new Vertex(0, 0));
        brush.paint(new Vertex(10, 0));
        brush.paint(new Vertex(10, 10));
        brush.paint(new Vertex(0, 10));
        var result = brush.paint(new Vertex(0, 0));
        assertTrue(result.shape().contains(1.5, 5.5));
        assertFalse(result.shape().contains(5, 5));
        assertFalse(existing.contains(1.5, 5.5));
    }

    @Test
    void boundaryStrokeCanSpanMoreThan4096BlocksWithoutTerrain() {
        var brush = stroke(new Area(), new BoundaryBrush.State(), null, 1);
        brush.paint(new Vertex(0, 0));
        var result = brush.paint(new Vertex(5000, 0));
        assertTrue(result.shape().contains(4999.5, 0.5));
    }

    @Test
    void longStrokeKeepsDiscoveringTerrainBeyondFormerTileLimit() {
        var brush = stroke(new Area(), new BoundaryBrush.State(), (x, z) -> BoundaryBrush.LAND, 1);
        brush.paint(new Vertex(0, 0));
        // 连续沿陆地绘制并跨过原来的累计瓦片上限，末端仍应可绘制且地形可识别。
        BoundaryBrush.Result result = null;
        for (int x = 16; x <= 6000; x += 16) result = brush.paint(new Vertex(x, 0));
        assertNotNull(result);
        assertTrue(result.terrainAvailable());
        assertTrue(result.shape().contains(5999.5, 0.5));
        assertTrue(result.guides().isEmpty());
    }

    @Test
    void clipsNewPaintAtParentAndPeersWithoutRemovingExistingOverlap() {
        Area existing = rectangle(-8, -4, 3, 4);
        Area parent = rectangle(-10, -10, 0, 10);
        Area peer = rectangle(-3, 3, 0, 10);
        var brush = new BoundaryBrush.Stroke(existing, parent, List.of(peer), new BoundaryBrush.State(), null, 10);
        var result = brush.paint(new Vertex(-1, 5));
        assertTrue(result.shape().contains(2, 0));
        assertFalse(result.shape().contains(2, 6));
        assertFalse(result.shape().contains(-1, 6));
        assertTrue(result.shape().contains(-5, 5));
        Area oldForbidden = result.state().forbidden();
        oldForbidden.intersect(existing);
        assertTrue(oldForbidden.isEmpty());
    }

    @Test
    void clipsBrushOutsideShoreUntilCenterClearlyCrosses() {
        var brush = stroke(rectangle(-8, -4, -1, 4), new BoundaryBrush.State(), COAST, 8);
        var result = brush.paint(new Vertex(-0.2, 0));
        assertTrue(result.shape().contains(-0.5, 0.5));
        assertFalse(result.shape().contains(0.5, 0.5));
        assertEquals(0, result.offset());
        result = brush.paint(new Vertex(0.4, 0));
        assertFalse(result.shape().contains(0.5, 0.5));
        assertEquals(0, result.offset());
        assertTrue(result.terrainAvailable());
    }

    @Test
    void offsetIsMeasuredFromOriginalShoreAcrossPaintsAndStrokes() {
        var brush = stroke(rectangle(-8, -4, -1, 4), new BoundaryBrush.State(), COAST, 8);
        brush.paint(new Vertex(-1, 0));
        var ten = brush.paint(new Vertex(10, 0));
        assertEquals(10, ten.offset(), 0.00001);
        assertTrue(ten.shape().contains(9.5, 0.5));
        assertFalse(ten.shape().contains(10.5, 0.5));
        var next = stroke(ten.shape(), ten.state(), COAST, 8);
        var twelve = next.paint(new Vertex(12, 1));
        assertEquals(12, twelve.offset(), 0.00001);
        assertFalse(twelve.shape().contains(12.5, 1.5));
        assertTrue(twelve.shape().contains(11.5, 1.5));
        Area forbidden = twelve.state().forbidden();
        forbidden.intersect(twelve.shape());
        assertTrue(forbidden.isEmpty());
    }

    @Test
    void existingLandChoosesRiverBankEvenWhenPointerIsCloserToOtherBank() {
        var brush = stroke(rectangle(-8, -5, -1, 5), new BoundaryBrush.State(), RIVER, 4);
        var result = brush.paint(new Vertex(4, 0));
        assertEquals(4, result.offset(), 0.00001);
        assertTrue(result.shape().contains(3.5, 0.5));
        assertFalse(result.shape().contains(5.5, 0.5));
    }

    @Test
    void bankRemainsLockedWhenCrossingNarrowRiver() {
        var brush = stroke(rectangle(-8, -5, -1, 5), new BoundaryBrush.State(), RIVER, 4);
        brush.paint(new Vertex(-1, 0));
        brush.paint(new Vertex(4, 0));
        var result = brush.paint(new Vertex(8, 0));
        assertEquals(8, result.offset(), 0.00001);
        assertTrue(result.shape().contains(7.5, 0.5));
        assertFalse(result.shape().contains(8.5, 0.5));
    }

    @Test
    void bothBanksFallBackToNearestBank() {
        Area existing = rectangle(-8, -5, -1, 5);
        existing.add(rectangle(7, -5, 12, 5));
        var result = stroke(existing, new BoundaryBrush.State(), RIVER, 4).paint(new Vertex(5, 0));
        assertEquals(1, result.offset(), 0.00001);
        assertFalse(result.shape().contains(4.5, 0.5));
        assertTrue(result.shape().contains(5.5, 0.5));
    }

    @Test
    void unknownTerrainDoesNotCreateCoast() {
        BoundaryBrush.Terrain partial = (x, z) -> x < 0 ? BoundaryBrush.LAND : BoundaryBrush.UNKNOWN;
        var result = stroke(new Area(), new BoundaryBrush.State(), partial, 8).paint(new Vertex(-1, 0));
        assertTrue(result.shape().contains(1.5, 0.5));
        assertTrue(result.guides().isEmpty());
        assertFalse(result.terrainAvailable());
    }

    @Test
    void stateAndForbiddenAreDefensiveCopies() {
        var brush = stroke(new Area(), new BoundaryBrush.State(), COAST, 8);
        var first = brush.paint(new Vertex(-1, 0));
        var snapshot = first.state().copy();
        var copied = snapshot.copy();
        snapshot.forbidden().reset();
        assertTrue(snapshot.same(copied));
        brush.paint(new Vertex(6, 0));
        assertTrue(first.state().same(snapshot));
        assertFalse(brush.paint(new Vertex(8, 0)).state().same(snapshot));
    }

    @Test
    void rememberedOffsetDoesNotSpreadToDistantCoast() {
        var brush = stroke(new Area(), new BoundaryBrush.State(), COAST, 8);
        brush.paint(new Vertex(-1, 0));
        var near = brush.paint(new Vertex(10, 0));
        var far = stroke(near.shape(), near.state(), COAST, 8).paint(new Vertex(-1, 200));
        assertEquals(0, far.offset());
        assertFalse(far.shape().contains(0.5, 200.5));
    }

    @Test
    void nextStrokeKeepsOriginalBankAfterPaintingAcrossRiver() {
        var brush = stroke(rectangle(-8, -5, -1, 5), new BoundaryBrush.State(), RIVER, 4);
        brush.paint(new Vertex(-1, 0));
        var across = brush.paint(new Vertex(8, 0));
        var next = stroke(across.shape(), across.state(), RIVER, 4).paint(new Vertex(10, 1));
        assertEquals(10, next.offset(), 0.00001);
        assertFalse(next.shape().contains(10.5, 1.5));
    }

    @Test
    void followsStaircaseShoreInsteadOfExtendingFirstStraightEdge() {
        BoundaryBrush.Terrain stairs = (x, z) -> x < Math.floorDiv(z, 4) ? BoundaryBrush.LAND : BoundaryBrush.WATER;
        var brush = stroke(rectangle(-12, -12, -8, 16), new BoundaryBrush.State(), stairs, 4);
        brush.paint(new Vertex(-0.2, 1));
        brush.paint(new Vertex(0.8, 5));
        var result = brush.paint(new Vertex(1.8, 9));
        assertEquals(0, result.offset());
        assertTrue(result.shape().contains(1.5, 9.5));
        assertFalse(result.shape().contains(2.5, 9.5));
    }

    @Test
    void convexCornerDoesNotLeakPaintPastBothShoreSegments() {
        BoundaryBrush.Terrain corner = (x, z) -> x < 0 && z < 0 ? BoundaryBrush.LAND : BoundaryBrush.WATER;
        var result = stroke(rectangle(-10, -10, -3, -3), new BoundaryBrush.State(), corner, 8)
            .paint(new Vertex(-0.2, -0.2));
        assertTrue(result.shape().contains(-0.5, -0.5));
        assertFalse(result.shape().contains(1.5, 1.5));
        assertFalse(result.shape().contains(-1.5, 1.5));
    }

    @Test
    void unknownTileIsRetriedWhenTerrainBecomesAvailable() {
        boolean[] ready = {false};
        BoundaryBrush.Terrain deferred = (x, z) -> ready[0] ? COAST.sample(x, z) : BoundaryBrush.UNKNOWN;
        var brush = stroke(new Area(), new BoundaryBrush.State(), deferred, 4);
        assertFalse(brush.paint(new Vertex(-1, 0)).terrainAvailable());
        ready[0] = true;
        var result = brush.paint(new Vertex(-1, 10));
        assertTrue(result.terrainAvailable());
        assertFalse(result.shape().contains(0.5, 10.5));
    }

    @Test
    void changingParentClearsOnlyCopiedHardConstraints() {
        var hard = new BoundaryBrush.Stroke(new Area(), rectangle(-10, -10, 0, 10), List.of(),
            new BoundaryBrush.State(), null, 8).paint(new Vertex(-1, 0)).state();
        assertFalse(hard.forbidden().isEmpty());
        assertTrue(hard.withoutHardConstraints().forbidden().isEmpty());
        assertFalse(hard.forbidden().isEmpty());
        var natural = stroke(new Area(), new BoundaryBrush.State(), COAST, 8).paint(new Vertex(-1, 0)).state();
        assertTrue(natural.same(natural.withoutHardConstraints()));
    }

    @Test
    void knownInlandReportsAvailableTerrainWithoutInventingShore() {
        var result = stroke(new Area(), new BoundaryBrush.State(), (x, z) -> BoundaryBrush.LAND, 8)
            .paint(new Vertex(2, 3));
        assertTrue(result.terrainAvailable());
        assertTrue(result.guides().isEmpty());
    }

    @Test
    void naturalForbiddenIncludesOptimizationMarginOutsideActualPaint() {
        var result = stroke(new Area(), new BoundaryBrush.State(), COAST, 2).paint(new Vertex(-1, 0));
        assertFalse(result.shape().contains(0.5, 0.5));
        assertTrue(result.state().forbidden().contains(0.5, 0.5));
        assertTrue(result.state().forbidden().contains(1.5, 0.5));
    }

    @Test
    void bayOffsetDoesNotPaintUnvisitedShoreOrPropagateToNextFarStroke() {
        BoundaryBrush.Terrain bay = (x, z) -> x < 0 || z < 0 ? BoundaryBrush.LAND : BoundaryBrush.WATER;
        var brush = stroke(rectangle(-8, -5, -1, 10), new BoundaryBrush.State(), bay, 4);
        brush.paint(new Vertex(-1, 3));
        var local = brush.paint(new Vertex(3, 3));
        assertEquals(3, local.offset(), 0.00001);
        assertFalse(local.shape().contains(10, 10));
        assertFalse(local.shape().contains(2, 20));
        var distant = stroke(local.shape(), local.state(), bay, 4).paint(new Vertex(40, -0.2));
        assertEquals(0, distant.offset());
        assertFalse(distant.shape().contains(40.5, 0.5));
    }

    @Test
    void convexOffsetGuideFollowsActualCurvedLimitWithoutScopeFrame() {
        BoundaryBrush.Terrain corner = (x, z) -> x < 0 && z < 0 ? BoundaryBrush.LAND : BoundaryBrush.WATER;
        var brush = stroke(rectangle(-10, -10, -3, -3), new BoundaryBrush.State(), corner, 8);
        brush.paint(new Vertex(-0.2, -0.2));
        var result = brush.paint(new Vertex(2, 2));
        assertTrue(result.guides().stream().anyMatch(line -> (line[0] + line[2]) / 2 > 0.1
            && (line[1] + line[3]) / 2 > 0.1));
        Area forbidden = result.state().forbidden();
        for (double[] line : result.guides()) {
            double x = (line[0] + line[2]) / 2, z = (line[1] + line[3]) / 2;
            double dx = line[2] - line[0], dz = line[3] - line[1], length = Math.hypot(dx, dz);
            double nx = -dz / length * 0.02, nz = dx / length * 0.02;
            assertNotEquals(forbidden.contains(x + nx, z + nz), forbidden.contains(x - nx, z - nz));
        }
    }

    @Test
    void narrowOrDistantExistingLandStillSelectsCorrectRiverBank() {
        for (Area existing : List.of(rectangle(-7, -5, -6, 5), rectangle(-100, -5, -50, 5), rectangle(-20, 40, -10, 50))) {
            var result = stroke(existing, new BoundaryBrush.State(), RIVER, 4).paint(new Vertex(4, 0));
            assertEquals(4, result.offset(), 0.00001);
            assertFalse(result.shape().contains(5.5, 0.5));
        }
    }

    @Test
    void samePointerReclipsOnlyCurrentStrokeWhenUnknownTerrainArrives() {
        boolean[] ready = {false};
        BoundaryBrush.Terrain deferred = (x, z) -> ready[0] ? COAST.sample(x, z) : BoundaryBrush.UNKNOWN;
        Area existing = rectangle(-8, -4, 1, -2);
        var brush = stroke(existing, new BoundaryBrush.State(), deferred, 8);
        assertTrue(brush.paint(new Vertex(-0.2, 0)).shape().contains(2.5, 0.5));
        ready[0] = true;
        var result = brush.paint(new Vertex(-0.2, 0));
        assertTrue(result.terrainAvailable());
        assertFalse(result.shape().contains(2.5, 0.5));
        assertTrue(result.shape().contains(0.5, -3));
    }

    @Test
    void remembersOffsetLargerThanPointerTerrainSearchRadius() {
        var brush = stroke(rectangle(-8, -4, -1, 4), new BoundaryBrush.State(), COAST, 4);
        brush.paint(new Vertex(-1, 0));
        var first = brush.paint(new Vertex(300, 0));
        assertEquals(300, first.offset(), 0.00001);
        var second = stroke(first.shape(), first.state(), COAST, 4).paint(new Vertex(302, 1));
        assertEquals(302, second.offset(), 0.00001);
        assertFalse(second.shape().contains(302.5, 1.5));
        assertTrue(second.shape().contains(301.5, 1.5));
    }

    @Test
    void negativeFractionalOffsetDoesNotCauseFalseSaveViolation() {
        BoundaryBrush.Terrain coast = (x, z) -> x < -5 ? BoundaryBrush.LAND : BoundaryBrush.WATER;
        var brush = stroke(rectangle(-12, -4, -6, 4), new BoundaryBrush.State(), coast, 4);
        brush.paint(new Vertex(-5.2, -0.2));
        var result = brush.paint(new Vertex(-3.7, -0.2));
        assertEquals(1.3, result.offset(), 0.00001);
        var saved = assertDoesNotThrow(() -> BoundarySaveGuard.prepare(result.shape(), result.state().forbidden()));
        Area overlap = VizEditGeometry.fromVertices(saved);
        overlap.intersect(result.state().forbidden());
        assertTrue(overlap.isEmpty());
    }

    @Test
    void turningAlongMeanderingRiverDoesNotReleasePreviouslyClippedRawPaint() {
        BoundaryBrush.Terrain river = (x, z) -> Math.abs(x + 0.5 - 8 * Math.sin((z + 0.5) / 10)) < 3
            ? BoundaryBrush.WATER : BoundaryBrush.LAND;
        var brush = stroke(new Area(), new BoundaryBrush.State(), river, 8);
        BoundaryBrush.Result result = null;
        for (int z = -30; z <= 30; z += 2) {
            result = brush.paint(new Vertex(8 * Math.sin(z / 10.0) - 4.5, z));
            assertEquals(0, result.offset());
        }
        assertNotNull(result);
        for (int z = -35; z <= 35; z++) for (int x = -18; x <= 18; x++) {
            if (river.sample(x, z) == BoundaryBrush.WATER) assertFalse(result.shape().contains(x + 0.5, z + 0.5));
        }
    }
}
