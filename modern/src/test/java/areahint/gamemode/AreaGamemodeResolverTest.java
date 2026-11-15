package areahint.gamemode;

import areahint.data.AreaData;
import areahint.data.AreaData.AltitudeData;
import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AreaGamemodeResolverTest {
    @Test
    void equalDistanceOverlapsRetainFileOrderAfterSpatialPartitioning() {
        AreaData first = rectangle("first", 1, null, -6, -10, 14, 10);
        AreaData second = rectangle("second", 1, null, -14, -10, 6, 10);
        List<AreaData> areas = new ArrayList<>();
        areas.add(first);
        for (int i = 0; i < 30; i++) {
            areas.add(rectangle("far-" + i, 1, null, 100 + i * 20, 100, 110 + i * 20, 110));
        }
        areas.add(second);

        assertSame(first, new AreaGamemodeResolver(areas).findArea(0, 64, 0));
        areas.set(0, second);
        areas.set(areas.size() - 1, first);
        assertSame(second, new AreaGamemodeResolver(areas).findArea(0, 64, 0));
    }

    @Test
    void descendsOnlyThroughTheSelectedParent() {
        AreaData selected = rectangle("selected", 1, null, -20, -20, 20, 20);
        AreaData other = rectangle("other", 1, null, -15, -20, 25, 20);
        AreaData child = rectangle("child", 2, "selected", -10, -10, 10, 10);
        AreaData grandchild = rectangle("grandchild", 3, "child", -5, -5, 5, 5);
        AreaData unrelated = rectangle("unrelated", 4, "other", -2, -2, 2, 2);

        assertSame(grandchild, new AreaGamemodeResolver(List.of(other, unrelated, child, selected, grandchild))
            .findArea(0, 64, 0));
    }

    @Test
    void skipsALevelOnlyWhenNoAreaAtThatLevelMatchesAltitude() {
        AreaData root = rectangle("root", 1, null, -20, -20, 20, 20);
        AreaData highChild = rectangle("high-child", 2, "root", -10, -10, 10, 10);
        highChild.setAltitude(new AltitudeData(null, 100.0));
        // 保留旧文件中跨级父链的处理方式，不能把全局无高度候选误当成查找终止。
        AreaData skippedLevelChild = rectangle("skipped-child", 3, "root", -5, -5, 5, 5);

        assertSame(skippedLevelChild, new AreaGamemodeResolver(List.of(root, highChild, skippedLevelChild))
            .findArea(0, 64, 0));
    }

    @Test
    void stopsWhenAChildMatchesAltitudeButMissesItsBoundingBox() {
        AreaData root = rectangle("root", 1, null, -100, -100, 100, 100);
        AreaData remoteChild = rectangle("remote", 2, "root", 40, 40, 60, 60);
        AreaData skippedLevelChild = rectangle("skipped-child", 3, "root", -5, -5, 5, 5);

        assertSame(root, new AreaGamemodeResolver(List.of(root, remoteChild, skippedLevelChild))
            .findArea(0, 64, 0));
    }

    @Test
    void stopsWhenOnlyAnotherParentsChildMatchesAltitude() {
        AreaData root = rectangle("root", 1, null, -100, -100, 100, 100);
        AreaData unrelatedChild = rectangle("unrelated", 2, "another-parent", 40, 40, 60, 60);
        AreaData skippedLevelChild = rectangle("skipped-child", 3, "root", -5, -5, 5, 5);

        assertSame(root, new AreaGamemodeResolver(List.of(root, unrelatedChild, skippedLevelChild))
            .findArea(0, 64, 0));
    }

    @Test
    void boundingBoxHitStillRequiresTheOriginalPolygonCheck() {
        AreaData root = rectangle("root", 1, null, -100, -100, 100, 100);
        AreaData triangle = rectangle("triangle", 2, "root", -10, -10, 10, 10);
        triangle.setVertices(List.of(new Vertex(-10, -10), new Vertex(10, -10), new Vertex(-10, 10)));
        AreaData skippedLevelChild = rectangle("skipped-child", 3, "root", 0, 0, 10, 10);

        assertSame(root, new AreaGamemodeResolver(List.of(root, triangle, skippedLevelChild))
            .findArea(5, 64, 5));
    }

    @Test
    void preservesPolygonBoundaryAndInclusiveAltitudeRules() {
        AreaData area = rectangle("negative", 1, null, -20, -20, -10, -10);
        area.setAltitude(new AltitudeData(80.0, 60.0));
        AreaGamemodeResolver resolver = new AreaGamemodeResolver(List.of(area));

        assertSame(area, resolver.findArea(-20, 60, -15));
        assertSame(area, resolver.findArea(-15, 80, -20));
        assertNull(resolver.findArea(-10, 64, -15));
        assertNull(resolver.findArea(-15, 64, -10));
        assertNull(resolver.findArea(-15, 59.9, -15));
        assertNull(resolver.findArea(-15, 80.1, -15));
    }

    @Test
    void ignoresInvalidVerticesAndEmptyData() {
        AreaData valid = rectangle("valid", 1, null, -10, -10, 10, 10);
        AreaData invalid = rectangle("invalid", 1, null, -10, -10, 10, 10);
        invalid.setVertices(Arrays.asList(new Vertex(0, 0), null, new Vertex(2, 2)));
        AreaData nonFinite = rectangle("non-finite", 1, null, -10, -10, 10, 10);
        nonFinite.setVertices(List.of(new Vertex(Double.NaN, 0), new Vertex(0, 0), new Vertex(2, 2)));

        assertSame(valid, new AreaGamemodeResolver(Arrays.asList(null, invalid, nonFinite, valid)).findArea(0, 64, 0));
        assertNull(new AreaGamemodeResolver(List.of()).findArea(0, 64, 0));
        assertNull(new AreaGamemodeResolver(null).findArea(0, 64, 0));
    }

    @Test
    void replacingTheResolverUsesTheNewGeometryWithoutChangingTheOldSnapshot() {
        AreaData oldArea = rectangle("same-name", 1, null, -10, -10, 10, 10);
        AreaData newArea = rectangle("same-name", 1, null, 100, 100, 120, 120);
        AreaGamemodeResolver before = new AreaGamemodeResolver(List.of(oldArea));
        AreaGamemodeResolver after = new AreaGamemodeResolver(List.of(newArea));

        assertSame(oldArea, before.findArea(0, 64, 0));
        assertNull(after.findArea(0, 64, 0));
        assertSame(newArea, after.findArea(110, 64, 110));
        assertNull(before.findArea(110, 64, 110));
    }

    private static AreaData rectangle(String name, int level, String parent,
                                      double minX, double minZ, double maxX, double maxZ) {
        List<Vertex> vertices = List.of(new Vertex(minX, minZ), new Vertex(maxX, minZ),
            new Vertex(maxX, maxZ), new Vertex(minX, maxZ));
        return new AreaData(name, vertices, vertices, null, level, parent, null);
    }
}
