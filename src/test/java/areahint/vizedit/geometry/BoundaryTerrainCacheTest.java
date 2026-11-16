package areahint.vizedit.geometry;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundaryTerrainCacheTest {
    @Test
    void retriesUnknownTilesAfterLoadingWithoutKeepingAFalseLandBoundary() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger reads = new AtomicInteger();
        BoundaryTerrainCache cache = new BoundaryTerrainCache((x, z) ->
            reads.incrementAndGet() == 1 ? null : tile(2), clock::get);
        assertEquals(0, cache.sample(4, 7));
        assertEquals(0, cache.sample(4, 7));
        assertEquals(1, reads.get());
        clock.set(300_000_000L);
        assertEquals(2, cache.sample(4, 7));
        assertEquals(2, reads.get());
    }

    @Test
    void copiesSourceTilesAndAddressesNegativeWorldCoordinates() {
        byte[] source = tile(1);
        source[15 * 16 + 14] = 2;
        BoundaryTerrainCache cache = new BoundaryTerrainCache((x, z) -> {
            assertEquals(-1, x);
            assertEquals(-2, z);
            return source;
        }, () -> 0);
        assertEquals(2, cache.sample(-1, -18));
        source[15 * 16 + 14] = 1;
        assertEquals(2, cache.sample(-1, -18));
    }

    @Test
    void throttlesNewReadsButLetsTheNextFrameFinishMissingTiles() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger reads = new AtomicInteger();
        BoundaryTerrainCache cache = new BoundaryTerrainCache((x, z) -> {
            reads.incrementAndGet();
            return tile(1);
        }, clock::get);
        for (int i = 0; i < 8; i++) assertEquals(1, cache.sample(i * 16, 0));
        assertEquals(0, cache.sample(128, 0));
        assertEquals(8, reads.get());
        clock.set(20_000_000L);
        assertEquals(1, cache.sample(128, 0));
        assertEquals(9, reads.get());
    }

    @Test
    void slowReadCannotResetBudgetInsideTheSameClientTick() {
        AtomicLong clock = new AtomicLong(), tick = new AtomicLong();
        BoundaryTerrainCache cache = new BoundaryTerrainCache((x, z) -> {
            clock.addAndGet(50_000_000L);
            return tile(1);
        }, clock::get, tick::get);
        for (int i = 0; i < 8; i++) assertEquals(1, cache.sample(i * 16, 0));
        assertEquals(0, cache.sample(128, 0));
        tick.incrementAndGet();
        assertEquals(1, cache.sample(128, 0));
    }

    @Test
    void evictsOldTilesAndRefreshesKnownTerrain() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger originReads = new AtomicInteger();
        BoundaryTerrainCache cache = new BoundaryTerrainCache((x, z) -> {
            if (x == 0) originReads.incrementAndGet();
            return tile(1);
        }, clock::get);
        for (int i = 0; i < 257; i++) {
            if (i % 8 == 0) clock.addAndGet(20_000_000L);
            assertEquals(1, cache.sample(i * 16, 0));
        }
        cache.sample(0, 0);
        assertEquals(2, originReads.get());
        clock.addAndGet(6_000_000_000L);
        cache.sample(0, 0);
        assertEquals(3, originReads.get());
    }

    private static byte[] tile(int type) {
        byte[] result = new byte[256];
        Arrays.fill(result, (byte) type);
        return result;
    }
}
