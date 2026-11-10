package areahint.vizedit.geometry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DraftHistoryTest {
    @Test
    void undoRedoRetainSnapshotsAndNewEditsDiscardRedo() {
        DraftHistory<List<String>> history = new DraftHistory<>(64, ArrayList::new);
        List<String> original = new ArrayList<>(List.of("first"));
        history.push(original);
        original.add("mutated");
        assertEquals(List.of("first"), history.undo(List.of("second")));
        assertEquals(List.of("second"), history.redo(List.of("first")));
        assertEquals(List.of("first"), history.undo(List.of("second")));
        history.push(List.of("third"));
        assertFalse(history.canRedo());
    }

    @Test
    void capsHistoryAtSixtyFourAndSafelyHandlesEmptyStacks() {
        DraftHistory<Integer> history = new DraftHistory<>(1000, value -> value);
        for (int i = 0; i < 100; i++) history.push(i);
        assertEquals(64, history.undoDepth());
        int value = 100;
        for (int i = 0; i < 64; i++) value = history.undo(value);
        assertEquals(36, value);
        assertFalse(history.canUndo());
        assertEquals(36, history.undo(value));
        assertEquals(37, history.redo(value));
        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }
}
