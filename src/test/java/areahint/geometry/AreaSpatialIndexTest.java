package areahint.geometry;

import areahint.data.AreaData;
import areahint.data.AreaData.AltitudeData;
import areahint.data.AreaData.Vertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AreaSpatialIndexTest {
    @Test
    void filtersByBoundsAltitudeAndLevelAndClearsReusedOutput() {
        AreaData hit = rectangle("当前", -2, -2, 2, 2, 1, new AltitudeData(80.0, 60.0));
        AreaData distant = rectangle("远处", 100, 100, 110, 110, 1, null);
        AreaData above = rectangle("高处", -2, -2, 2, 2, 1, new AltitudeData(null, 90.0));
        AreaData child = rectangle("次级", -2, -2, 2, 2, 2, null);
        AreaSpatialIndex index = new AreaSpatialIndex(List.of(hit, distant, above, child));
        List<AreaData> output = new ArrayList<>(List.of(distant));

        index.collectCandidates(0, 70, 0, 1, output);
        assertEquals(List.of(hit), output);
        index.collectCandidates(0, 70, 0, 2, output);
        assertEquals(List.of(child), output);
        index.collectCandidates(50, 70, 50, 1, output);
        assertTrue(output.isEmpty());
    }

    @Test
    void negativeCoordinatesAndAllAabbEdgesRemainInclusive() {
        AreaData area = rectangle("负坐标", -20, -30, -10, -15, 1, null);
        AreaSpatialIndex index = new AreaSpatialIndex(List.of(area));
        for (double[] point : List.of(new double[]{-20, -30}, new double[]{-10, -15},
                new double[]{-20, -20}, new double[]{-10, -20}, new double[]{-15, -30}, new double[]{-15, -15})) {
            assertEquals(List.of(area), query(index, point[0], 0, point[1], 1));
        }
        assertTrue(query(index, Math.nextDown(-20.0), 0, -20, 1).isEmpty());
        assertTrue(query(index, Math.nextUp(-10.0), 0, -20, 1).isEmpty());
        assertTrue(query(index, -15, 0, Math.nextDown(-30.0), 1).isEmpty());
        assertTrue(query(index, -15, 0, Math.nextUp(-15.0), 1).isEmpty());
    }

    @Test
    void equalDistancesKeepFileOrderIncludingRepeatedObjectsAndNames() {
        AreaData first = rectangle("同名", -2, -2, 2, 2, 1, null);
        AreaData second = rectangle("同名", -4, -4, 4, 4, 1, null);
        AreaData nearer = rectangle("较近", -1, -1, 1, 1, 1, null);
        List<AreaData> areas = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            areas.add(rectangle("远处" + i, i * 20 + 100, -10, i * 20 + 110, 10, 1, null));
        }
        areas.add(1, first);
        areas.add(20, second);
        areas.add(30, first);
        areas.add(nearer);
        assertEquals(List.of(first, second, first, nearer), query(new AreaSpatialIndex(areas), 0, 0, 0, 1));
    }

    @Test
    void distancePriorityPrecedesInputOrder() {
        AreaData farther = rectangle("远中心", -1, -1, 19, 1, 1, null);
        AreaData nearer = rectangle("近中心", -2, -2, 2, 2, 1, null);
        assertEquals(List.of(nearer, farther), query(new AreaSpatialIndex(List.of(farther, nearer)), 0, 0, 0, 1));
    }

    @Test
    void unlimitedOneSidedAndNonFiniteAltitudeRetainExistingRules() {
        List<AreaData> areas = List.of(
                rectangle("无限", -1, -1, 1, 1, 1, null),
                rectangle("仅下界", -1, -1, 1, 1, 1, new AltitudeData(null, 10.0)),
                rectangle("仅上界", -1, -1, 1, 1, 1, new AltitudeData(20.0, null)),
                rectangle("不定下界", -1, -1, 1, 1, 1, new AltitudeData(20.0, Double.NaN)),
                rectangle("不定上界", -1, -1, 1, 1, 1, new AltitudeData(Double.NaN, 10.0)),
                rectangle("倒置范围", -1, -1, 1, 1, 1, new AltitudeData(5.0, 30.0)));
        AreaSpatialIndex index = new AreaSpatialIndex(areas);
        for (double y : new double[]{-100, 5, 10, 15, 20, 30, 100, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertEquals(linearCandidates(areas, 0, y, 0, 1), query(index, 0, y, 0, 1));
        }
    }

    @Test
    void hugeAreaAndSmallNearbyAreasAreEachReturnedOnce() {
        AreaData huge = rectangle("超大域名", -30_000_000, -30_000_000, 30_000_000, 30_000_000, 1, null);
        List<AreaData> areas = new ArrayList<>(List.of(huge));
        for (int i = 0; i < 200; i++) {
            areas.add(rectangle("小域名" + i, i * 100, -2, i * 100 + 4, 2, 1, null));
        }
        AreaSpatialIndex index = new AreaSpatialIndex(areas);
        assertEquals(linearCandidates(areas, 10002, 0, 0, 1), query(index, 10002, 0, 0, 1));
        assertEquals(List.of(huge), query(index, -1_000_000, 0, -1_000_000, 1));
    }

    @Test
    void nonFiniteBoundsDoNotHideUnrelatedFiniteAreas() {
        AreaData finite = rectangle("正常", -3, -3, 3, 3, 1, null);
        AreaData infinite = rectangle("无限边界", Double.NEGATIVE_INFINITY, -1, Double.POSITIVE_INFINITY, 1, 1, null);
        AreaData nan = rectangle("不定边界", Double.NaN, -1, 2, 1, 1, null);
        AreaData missingBounds = rectangle("缺失二级顶点", -1, -1, 1, 1, 1, null);
        missingBounds.setSecondVertices(List.of());
        List<AreaData> areas = List.of(infinite, nan, missingBounds, finite);
        AreaSpatialIndex index = new AreaSpatialIndex(areas);
        for (double x : new double[]{-100, -3, 0, 3, 100, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertEquals(linearCandidates(areas, x, 0, 0, 1), query(index, x, 0, 0, 1));
        }
    }

    @Test
    void indexedResultsMatchIndependentLinearSelectionAcrossRandomWorld() {
        Random random = new Random(0xA11EA);
        List<AreaData> areas = randomAreas(random, 600);
        AreaSpatialIndex index = new AreaSpatialIndex(areas);
        for (int i = 0; i < 400; i++) {
            double x = random.nextDouble() * 4000 - 2000;
            double z = random.nextDouble() * 4000 - 2000;
            double y = random.nextDouble() * 200 - 50;
            int level = 1 + random.nextInt(4);
            assertEquals(linearCandidates(areas, x, y, z, level), query(index, x, y, z, level), "查询序号 " + i);
        }
    }

    @Test
    void rebuiltIndexUsesNewSnapshotAndDoesNotKeepInputList() {
        AreaData oldArea = rectangle("旧域名", -1, -1, 1, 1, 1, null);
        AreaData newArea = rectangle("新域名", 10, 10, 12, 12, 1, null);
        List<AreaData> source = new ArrayList<>(List.of(oldArea));
        AreaSpatialIndex oldIndex = new AreaSpatialIndex(source);
        source.clear();
        source.add(newArea);
        AreaSpatialIndex newIndex = new AreaSpatialIndex(source);
        assertEquals(List.of(oldArea), query(oldIndex, 0, 0, 0, 1));
        assertTrue(query(newIndex, 0, 0, 0, 1).isEmpty());
        assertEquals(List.of(newArea), query(newIndex, 11, 0, 11, 1));
        List<AreaData> output = new ArrayList<>(List.of(oldArea));
        new AreaSpatialIndex(List.of()).collectCandidates(0, 0, 0, 1, output);
        assertTrue(output.isEmpty());
    }

    @Test
    void alreadyComputedBoundsRemainTheSourceOfTruth() {
        AreaData area = rectangle("既有缓存", -1, -1, 1, 1, 1, null);
        area.computeCache();
        area.setSecondVertices(List.of(new Vertex(100, 100), new Vertex(102, 100),
                new Vertex(102, 102), new Vertex(100, 102)));
        AreaSpatialIndex index = new AreaSpatialIndex(List.of(area));
        assertEquals(List.of(area), query(index, 0, 0, 0, 1));
        assertTrue(query(index, 101, 0, 101, 1).isEmpty());
    }

    @Test
    void concurrentReadersUseIndependentBuffers() throws Exception {
        List<AreaData> areas = randomAreas(new Random(302), 500);
        AreaSpatialIndex index = new AreaSpatialIndex(areas);
        var executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int reader = 0; reader < 4; reader++) {
                final int seed = reader;
                tasks.add(() -> {
                    Random random = new Random(seed);
                    List<AreaData> output = new ArrayList<>();
                    for (int query = 0; query < 100; query++) {
                        double x = random.nextDouble() * 4000 - 2000;
                        double z = random.nextDouble() * 4000 - 2000;
                        int level = 1 + random.nextInt(4);
                        index.collectCandidates(x, 40, z, level, output);
                        assertEquals(linearCandidates(areas, x, 40, z, level), output);
                    }
                    return null;
                });
            }
            for (var result : executor.invokeAll(tasks)) {
                result.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static List<AreaData> randomAreas(Random random, int count) {
        List<AreaData> areas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double x = random.nextDouble() * 4000 - 2000;
            double z = random.nextDouble() * 4000 - 2000;
            double width = 1 + random.nextDouble() * 600;
            double depth = 1 + random.nextDouble() * 600;
            AltitudeData altitude = i % 3 == 0 ? null : new AltitudeData(100.0, -20.0);
            areas.add(rectangle("域名" + i, x, z, x + width, z + depth, 1 + random.nextInt(4), altitude));
        }
        return areas;
    }

    // 使用旧查询的线性高度/AABB筛选及稳定距离排序作为结果参照，不依赖树结构。
    private static List<AreaData> linearCandidates(List<AreaData> areas, double x, double y, double z, int level) {
        List<AreaData> expected = new ArrayList<>();
        for (AreaData area : areas) {
            if (!area.isCacheComputed()) {
                area.computeCache();
            }
            if (area.getLevel() == level && (area.getAltitude() == null || area.getAltitude().isInRange(y))
                    && area.isPointInCachedAABB(x, z)) {
                expected.add(area);
            }
        }
        expected.sort(Comparator.comparingDouble(area -> area.distanceSqToCenter(x, z)));
        return expected;
    }

    private static List<AreaData> query(AreaSpatialIndex index, double x, double y, double z, int level) {
        List<AreaData> result = new ArrayList<>();
        index.collectCandidates(x, y, z, level, result);
        return result;
    }

    private static AreaData rectangle(String name, double minX, double minZ, double maxX, double maxZ,
                                      int level, AltitudeData altitude) {
        List<Vertex> vertices = List.of(new Vertex(minX, minZ), new Vertex(maxX, minZ),
                new Vertex(maxX, maxZ), new Vertex(minX, maxZ));
        return new AreaData(name, vertices, vertices, altitude, level, null, null);
    }
}
