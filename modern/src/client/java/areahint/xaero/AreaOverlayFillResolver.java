package areahint.xaero;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.geometry.TriangleMeshDifference;
import areahint.log.AreaChangeTracker;
import areahint.log.AreaChangeTracker.DetectionState;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.AreaOverlayRepository.Triangle;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * 根据玩家当前进入的域名，为两类 Xaero 地图生成一致的填充三角网格。
 */
public final class AreaOverlayFillResolver {
    private static final double GEOMETRY_EPSILON = 1.0E-7D;
    private static final AreaOverlayFillResolver INSTANCE = new AreaOverlayFillResolver();

    // 单独的守护线程只处理不可变几何；渲染入口从不等待计算，最多保留十六份最近使用的计划。
    private final AsyncOverlayCache<FillCacheKey, FillPlan> plans = new AsyncOverlayCache<>(
        Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "AreaHint-OverlayGeometry");
            thread.setDaemon(true);
            return thread;
        }), 16, (key, failure) -> AreashintClient.LOGGER.warn("计算 Xaero 域名填充失败: {}", key, failure));
    private String lastPlayerDimensionId;
    private long cachedFallbackSnapshotRevision = Long.MIN_VALUE;
    private String cachedFallbackDimensionId;
    private double cachedFallbackX = Double.NaN;
    private double cachedFallbackY = Double.NaN;
    private double cachedFallbackZ = Double.NaN;
    private long cachedFallbackCheckTime = Long.MIN_VALUE;
    private String cachedFallbackAreaName;

    private AreaOverlayFillResolver() {
    }

    public static AreaOverlayFillResolver getInstance() {
        return INSTANCE;
    }

    /**
     * 读取已完成的计划或提交后台任务，首次计算期间使用当前快照的基础填充。
     */
    public synchronized FillPlan resolve(OverlaySnapshot snapshot, Minecraft client) {
        OverlaySnapshot safeSnapshot = snapshot == null ? OverlaySnapshot.empty("") : snapshot;
        String playerDimensionId = client == null || client.level == null
            ? null : areahint.util.DimensionIdCache.getId(client.level.dimension().identifier());
        String normalizedPlayerDimension = AreaOverlayRepository.normalizeDimensionId(playerDimensionId);
        // 离开维度后取消该维度的旧计算，避免大型旧任务阻挡新维度的地图。
        if (!Objects.equals(lastPlayerDimensionId, normalizedPlayerDimension)) {
            if (lastPlayerDimensionId != null) {
                plans.invalidate(lastPlayerDimensionId);
            }
            lastPlayerDimensionId = normalizedPlayerDimension;
        }
        if (client == null || client.player == null || client.level == null || safeSnapshot.areas().isEmpty()) {
            plans.invalidate(safeSnapshot.dimensionId());
            return safeSnapshot.basePlan();
        }
        String activeAreaName = resolveActiveAreaName(safeSnapshot, client,
            playerDimensionId, normalizedPlayerDimension);
        OverlayArea activeArea = activeAreaName == null ? null : safeSnapshot.byName().get(activeAreaName);
        String resolvedActiveAreaName = activeArea == null ? null : activeArea.name();
        // 同一份几何在反复进出后仍可复用，不把每次域名切换的检测版本放进缓存键。
        FillCacheKey cacheKey = new FillCacheKey(safeSnapshot.revision(), safeSnapshot.dimensionId(),
            resolvedActiveAreaName);
        return plans.request(safeSnapshot.dimensionId(), cacheKey,
            cancelled -> buildPlan(safeSnapshot, activeArea, cancelled), safeSnapshot.basePlan());
    }

    // 新快照发布时同时淘汰该维度的旧结果和请求，其它维度的地图缓存仍可复用。
    public synchronized void invalidateDimension(String dimensionId) {
        plans.invalidate(dimensionId);
        if (Objects.equals(cachedFallbackDimensionId, dimensionId)) {
            resetFallback();
        }
    }

    // 断开世界后清除整个计算代次，已在后台运行的旧任务不能再发布结果。
    public synchronized void clear() {
        plans.clear();
        lastPlayerDimensionId = null;
        resetFallback();
    }

    private void resetFallback() {
        cachedFallbackSnapshotRevision = Long.MIN_VALUE;
        cachedFallbackDimensionId = null;
        cachedFallbackX = Double.NaN;
        cachedFallbackY = Double.NaN;
        cachedFallbackZ = Double.NaN;
        cachedFallbackCheckTime = Long.MIN_VALUE;
        cachedFallbackAreaName = null;
    }

    /**
     * 主功能开启时只消费 AreaChangeTracker，关闭时才复用一次受检测频率限制的备用判定。
     */
    private String resolveActiveAreaName(OverlaySnapshot snapshot, Minecraft client,
                                         String playerDimensionId, String normalizedPlayerDimension) {
        if (client == null || client.player == null || client.level == null) {
            return null;
        }
        if (!Objects.equals(snapshot.dimensionId(), normalizedPlayerDimension)) {
            return null;
        }
        if (ClientConfig.isEnabled()) {
            DetectionState detectionState = AreaChangeTracker.getDetectionState();
            String detectionDimension = AreaOverlayRepository.normalizeDimensionId(detectionState.dimensionId());
            return Objects.equals(detectionDimension, normalizedPlayerDimension)
                ? detectionState.areaName() : null;
        }

        return resolveFallbackActiveAreaName(snapshot, client, playerDimensionId);
    }

    /**
     * 独立覆盖层模式只在玩家移动且达到配置间隔后检测，两个 Xaero 渲染入口共享该结果。
     */
    private String resolveFallbackActiveAreaName(OverlaySnapshot snapshot, Minecraft client,
                                                 String playerDimensionId) {
        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        boolean sameSnapshotAndDimension = cachedFallbackSnapshotRevision == snapshot.revision()
            && Objects.equals(cachedFallbackDimensionId, playerDimensionId);
        boolean samePosition = Double.compare(cachedFallbackX, playerX) == 0
            && Double.compare(cachedFallbackY, playerY) == 0
            && Double.compare(cachedFallbackZ, playerZ) == 0;
        if (sameSnapshotAndDimension && samePosition) {
            return cachedFallbackAreaName;
        }

        long now = System.currentTimeMillis();
        long elapsed = cachedFallbackCheckTime == Long.MIN_VALUE
            ? Long.MAX_VALUE : now - cachedFallbackCheckTime;
        if (sameSnapshotAndDimension && elapsed >= 0L && elapsed < getDetectionIntervalMillis()) {
            return cachedFallbackAreaName;
        }

        AreaData detectedArea = AreashintClient.getAreaDetector() == null ? null
            : AreashintClient.getAreaDetector().findAreaForXaeroOverlay(playerX, playerY, playerZ);
        String detectedAreaName = detectedArea == null ? null : detectedArea.getName();
        cachedFallbackSnapshotRevision = snapshot.revision();
        cachedFallbackDimensionId = playerDimensionId;
        cachedFallbackX = playerX;
        cachedFallbackY = playerY;
        cachedFallbackZ = playerZ;
        cachedFallbackCheckTime = now;
        cachedFallbackAreaName = detectedAreaName;
        return detectedAreaName;
    }

    private long getDetectionIntervalMillis() {
        double frequency = ClientConfig.getFrequency();
        if (!Double.isFinite(frequency) || frequency <= 0.0D) {
            return 1000L;
        }
        return Math.max(1L, Math.round(1000.0D / frequency));
    }

    private static FillPlan buildPlan(OverlaySnapshot snapshot, OverlayArea activeArea, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        if (activeArea == null) {
            return snapshot.basePlan();
        }
        Set<String> ancestorNames = findAncestorNames(snapshot, activeArea);
        List<TriangleMeshDifference.Triangle> activeMask = ancestorNames.isEmpty() ? List.of()
            : toGeometry(snapshot.basePlan().trianglesFor(activeArea), cancelled);
        Map<String, List<FillTriangle>> trianglesByAreaName = new LinkedHashMap<>(
            snapshot.basePlan().trianglesByAreaName());
        trianglesByAreaName.put(activeArea.name(), List.of());
        // 不受挖空影响的域名直接复用基础列表，只为当前域名的有效祖先计算差集。
        for (String ancestorName : ancestorNames) {
            checkCancelled(cancelled);
            List<TriangleMeshDifference.Triangle> source = toGeometry(
                snapshot.basePlan().trianglesByAreaName().getOrDefault(ancestorName, List.of()), cancelled);
            List<TriangleMeshDifference.Triangle> difference = TriangleMeshDifference.subtract(source, activeMask, cancelled);
            List<FillTriangle> resolved = new ArrayList<>(difference.size());
            for (TriangleMeshDifference.Triangle triangle : difference) {
                checkCancelled(cancelled);
                resolved.add(new FillTriangle(fromGeometry(triangle.first()),
                    fromGeometry(triangle.second()), fromGeometry(triangle.third())));
            }
            trianglesByAreaName.put(ancestorName, List.copyOf(resolved));
        }
        checkCancelled(cancelled);
        return new FillPlan(activeArea, trianglesByAreaName);
    }

    private static List<TriangleMeshDifference.Triangle> toGeometry(List<FillTriangle> triangles, BooleanSupplier cancelled) {
        List<TriangleMeshDifference.Triangle> result = new ArrayList<>(triangles.size());
        for (FillTriangle triangle : triangles) {
            checkCancelled(cancelled);
            result.add(new TriangleMeshDifference.Triangle(toGeometry(triangle.first()),
                toGeometry(triangle.second()), toGeometry(triangle.third())));
        }
        return List.copyOf(result);
    }

    private static TriangleMeshDifference.Point toGeometry(Point point) {
        return new TriangleMeshDifference.Point(point.x(), point.z());
    }

    private static Point fromGeometry(TriangleMeshDifference.Point point) {
        return new Point(point.x(), point.z());
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("域名填充请求已过期");
        }
    }

    // 基础网格随快照只转换一次，普通渲染帧和后台计划共享这些不可变列表。
    static FillPlan buildBasePlan(List<OverlayArea> areas) {
        Map<String, List<FillTriangle>> triangles = new LinkedHashMap<>();
        for (OverlayArea area : areas) {
            triangles.put(area.name(), convertTriangles(area));
        }
        return new FillPlan(null, triangles);
    }

    /**
     * 只沿当前域名声明的父链挖空，兄弟域名和未进入的子域名仍保留完整填充。
     */
    private static Set<String> findAncestorNames(OverlaySnapshot snapshot, OverlayArea activeArea) {
        Set<String> ancestors = new HashSet<>();
        Set<String> visited = new HashSet<>();
        OverlayArea child = activeArea;
        String parentName = activeArea == null ? null : activeArea.baseName();
        while (parentName != null && visited.add(parentName)) {
            OverlayArea parent = snapshot.byName().get(parentName);
            if (parent == null || child == null || parent.level() != child.level() - 1) {
                break;
            }
            ancestors.add(parent.name());
            child = parent;
            parentName = parent.baseName();
        }
        return ancestors;
    }

    private static List<FillTriangle> convertTriangles(OverlayArea area) {
        List<FillTriangle> converted = new ArrayList<>(area.triangles().size());
        for (Triangle triangle : area.triangles()) {
            if (!isValidIndex(area, triangle.first()) || !isValidIndex(area, triangle.second())
                || !isValidIndex(area, triangle.third())) {
                continue;
            }
            FillTriangle convertedTriangle = new FillTriangle(
                area.vertices().get(triangle.first()), area.vertices().get(triangle.second()),
                area.vertices().get(triangle.third()));
            if (!isDegenerate(convertedTriangle.first(), convertedTriangle.second(), convertedTriangle.third())) {
                converted.add(convertedTriangle);
            }
        }
        return List.copyOf(converted);
    }

    private static boolean isValidIndex(OverlayArea area, int index) {
        return index >= 0 && index < area.vertices().size();
    }

    private static boolean isDegenerate(Point first, Point second, Point third) {
        return Math.abs(cross(first, second, third)) <= GEOMETRY_EPSILON;
    }

    private static double cross(Point first, Point second, Point third) {
        return (second.x() - first.x()) * (third.z() - first.z())
            - (second.z() - first.z()) * (third.x() - first.x());
    }

    private record FillCacheKey(long snapshotRevision, String snapshotDimensionId, String activeAreaName) {
    }

    public record FillTriangle(Point first, Point second, Point third) {
    }

    /**
     * 保存当前进入域名和所有域名最终填充网格的不可变渲染计划。
     */
    public record FillPlan(OverlayArea activeArea, Map<String, List<FillTriangle>> trianglesByAreaName) {
        public FillPlan {
            Map<String, List<FillTriangle>> immutable = new LinkedHashMap<>();
            if (trianglesByAreaName != null) {
                trianglesByAreaName.forEach((name, triangles) ->
                    immutable.put(name, List.copyOf(triangles == null ? List.of() : triangles)));
            }
            trianglesByAreaName = Map.copyOf(immutable);
        }

        public List<FillTriangle> trianglesFor(OverlayArea area) {
            return area == null ? List.of()
                : trianglesByAreaName.getOrDefault(area.name(), List.of());
        }

        public static FillPlan empty() {
            return new FillPlan(null, Map.of());
        }
    }

}
