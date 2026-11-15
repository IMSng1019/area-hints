package areahint.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AreaDataChunkAssemblerTest {
    @Test void completesOnlyAfterAllChunksAndAcceptsIdenticalDuplicates() {
        var chunks = new AreaDataChunkAssembler();
        assertNull(chunks.accept("overworld", "a", 0, 3, "[", 0));
        assertNull(chunks.accept("overworld", "a", 2, 3, "]", 1));
        assertNull(chunks.accept("overworld", "a", 2, 3, "]", 2));
        assertEquals("[{}]", chunks.accept("overworld", "a", 1, 3, "{}", 3));
    }

    @Test void newSnapshotCannotBeOverwrittenByOldRemainingChunks() {
        var chunks = new AreaDataChunkAssembler();
        chunks.accept("overworld", "old", 0, 2, "old", 0);
        chunks.accept("overworld", "new", 0, 2, "new", 1);
        assertNull(chunks.accept("overworld", "old", 1, 2, "!", 2));
        assertEquals("new!", chunks.accept("overworld", "new", 1, 2, "!", 3));
    }

    @Test void rejectsOversizedHeadersChunksAndConflictingDuplicates() {
        var chunks = new AreaDataChunkAssembler();
        assertNull(chunks.accept("overworld", "bad", 0, Integer.MAX_VALUE, "a", 0));
        assertNull(chunks.accept("overworld", "bad", 0, 1, "a".repeat(24001), 0));
        assertNull(chunks.accept("unknown", "bad", 0, 1, "a", 0));
        chunks.accept("overworld", "a", 0, 2, "first", 0);
        assertNull(chunks.accept("overworld", "a", 0, 2, "changed", 1));
        assertNull(chunks.accept("overworld", "a", 1, 2, "last", 2));
        assertEquals("ok", chunks.accept("the_nether", "b", 0, 1, "ok", 3));
    }

    @Test void timeoutAndDisconnectDiscardIncompleteData() {
        var chunks = new AreaDataChunkAssembler();
        chunks.accept("overworld", "a", 0, 2, "first", 0);
        assertNull(chunks.accept("overworld", "a", 1, 2, "last", 30001));
        chunks.accept("the_end", "b", 0, 2, "first", 30002);
        chunks.clear();
        assertNull(chunks.accept("the_end", "b", 1, 2, "last", 30003));
    }

    @Test void rejectsAccumulatedPayloadBeyondEightMillionCharacters() {
        var chunks = new AreaDataChunkAssembler();
        String part = "x".repeat(24000);
        for (int i = 0; i < 334; i++) assertNull(chunks.accept("overworld", "a", i, 334, part, i));
        assertEquals("[]", chunks.accept("overworld", "b", 0, 1, "[]", 335));
    }
}
