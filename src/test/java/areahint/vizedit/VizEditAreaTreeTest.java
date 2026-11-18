package areahint.vizedit;

import areahint.data.AreaData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VizEditAreaTreeTest {
    @Test
    void reusesVisibleRowsAndPreservesSortedDepthFirstOrder() {
        AreaData city = area("City", 1, null);
        AreaData west = area("West", 2, "City");
        AreaData east = area("East", 2, "City");
        AreaData ward = area("Ward", 3, "East");
        VizEditAreaTree tree = new VizEditAreaTree(List.of(west, ward, city, east));
        var rows = tree.rows("");
        assertEquals(List.of(new VizEditAreaTree.Row(city, 0, true), new VizEditAreaTree.Row(east, 1, true),
            new VizEditAreaTree.Row(ward, 2, false), new VizEditAreaTree.Row(west, 1, false)), rows);
        assertSame(rows, tree.rows(""));
        tree.toggleCollapsed("City");
        assertEquals(List.of(new VizEditAreaTree.Row(city, 0, true)), tree.rows(""));
        assertEquals(List.of(new VizEditAreaTree.Row(east, 0, true)), tree.rows("  eAsT "));
        tree.toggleCollapsed("City");
        assertEquals(rows, tree.rows(""));
    }

    @Test
    void keepsMissingParentsAndCyclesSelectableOnce() {
        AreaData orphan = area("Orphan", 3, "Missing");
        AreaData a = area("A", 2, "B");
        AreaData b = area("B", 2, "A");
        VizEditAreaTree tree = new VizEditAreaTree(List.of(b, orphan, a));
        assertEquals(List.of(new VizEditAreaTree.Row(orphan, 0, false), new VizEditAreaTree.Row(a, 0, true),
            new VizEditAreaTree.Row(b, 1, true)), tree.rows(""));
        tree.toggleCollapsed("A");
        assertEquals(List.of(new VizEditAreaTree.Row(orphan, 0, false), new VizEditAreaTree.Row(a, 0, true)), tree.rows(""));
    }

    private static AreaData area(String name, int level, String parent) {
        AreaData area = new AreaData();
        area.setName(name);
        area.setLevel(level);
        area.setBaseName(parent);
        return area;
    }
}
