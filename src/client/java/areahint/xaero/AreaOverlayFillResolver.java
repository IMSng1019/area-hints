package areahint.xaero;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.log.AreaChangeTracker;
import areahint.log.AreaChangeTracker.DetectionState;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.AreaOverlayRepository.Triangle;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 根据玩家当前进入的域名，为两类 Xaero 地图生成一致的填充三角网格。
 */
public final class AreaOverlayFillResolver {
    private static final double GEOMETRY_EPSILON = 1.0E-7D;
    private static final AreaOverlayFillResolver INSTANCE = new AreaOverlayFillResolver();

    // 每个维度只保存最近计划，世界地图查看其他维度时不会挤掉小地图正在使用的计划
    private final Map<String, CachedFillPlan> cachedPlans = new HashMap<>();
    private long cachedFallbackSnapshotRevision = Long.MIN_VALUE;
    private String cachedFallbackDimensionId;
    private double cachedFallbackX = Double.NaN;
    private double cachedFallbackY = Double.NaN;
    private double cachedFallbackZ = Double.NaN;
    private long cachedFallbackCheckTime = Long.MIN_VALUE;
    private long fallbackStateRevision;
    private String cachedFallbackAreaName;

    private AreaOverlayFillResolver() {
    }

    public static AreaOverlayFillResolver getInstance() {
        return INSTANCE;
    }

    /**
     * 读取模组已经完成的域名判定，并让两张地图复用同一个按维度缓存的差集结果。
     */
    public synchronized FillPlan resolve(OverlaySnapshot snapshot, MinecraftClient client) {
        OverlaySnapshot safeSnapshot = snapshot == null ? OverlaySnapshot.empty("") : snapshot;
        String playerDimensionId = client == null || client.world == null
            ? null : client.world.getRegistryKey().getValue().toString();
        String normalizedPlayerDimension = AreaOverlayRepository.normalizeDimensionId(playerDimensionId);
        ActiveAreaState activeState = resolveActiveAreaState(safeSnapshot, client,
            playerDimensionId, normalizedPlayerDimension);
        OverlayArea activeArea = activeState.areaName() == null ? null
            : safeSnapshot.byName().get(activeState.areaName());
        String resolvedActiveAreaName = activeArea == null ? null : activeArea.name();
        FillCacheKey cacheKey = new FillCacheKey(safeSnapshot.revision(), safeSnapshot.dimensionId(),
            activeState.dimensionId(), activeState.revision(), resolvedActiveAreaName);
        CachedFillPlan cached = cachedPlans.get(safeSnapshot.dimensionId());
        if (cached != null && cached.key().equals(cacheKey)) {
            return cached.plan();
        }

        FillPlan plan = buildPlan(safeSnapshot, activeArea);
        cachedPlans.put(safeSnapshot.dimensionId(), new CachedFillPlan(cacheKey, plan));
        return plan;
    }

    /**
     * 主功能开启时只消费 AreaChangeTracker，关闭时才复用一次受检测频率限制的备用判定。
     */
    private ActiveAreaState resolveActiveAreaState(OverlaySnapshot snapshot, MinecraftClient client,
                                                   String playerDimensionId,
                                                   String normalizedPlayerDimension) {
        if (client == null || client.player == null || client.world == null) {
            return ActiveAreaState.empty();
        }
        if (!Objects.equals(snapshot.dimensionId(), normalizedPlayerDimension)) {
            return new ActiveAreaState(null, normalizedPlayerDimension, Long.MIN_VALUE);
        }
        if (ClientConfig.isEnabled()) {
            DetectionState detectionState = AreaChangeTracker.getDetectionState();
            String detectionDimension = AreaOverlayRepository.normalizeDimensionId(detectionState.dimensionId());
            String activeAreaName = Objects.equals(detectionDimension, normalizedPlayerDimension)
                ? detectionState.areaName() : null;
            return new ActiveAreaState(activeAreaName, detectionDimension, detectionState.revision());
        }

        return resolveFallbackActiveAreaState(snapshot, client, playerDimensionId,
            normalizedPlayerDimension);
    }

    /**
     * 独立覆盖层模式只在玩家移动且达到配置间隔后检测，两个 Xaero 渲染入口共享该结果。
     */
    private ActiveAreaState resolveFallbackActiveAreaState(OverlaySnapshot snapshot, MinecraftClient client,
                                                            String playerDimensionId,
                                                            String normalizedPlayerDimension) {
        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        boolean sameSnapshotAndDimension = cachedFallbackSnapshotRevision == snapshot.revision()
            && Objects.equals(cachedFallbackDimensionId, playerDimensionId);
        boolean samePosition = Double.compare(cachedFallbackX, playerX) == 0
            && Double.compare(cachedFallbackY, playerY) == 0
            && Double.compare(cachedFallbackZ, playerZ) == 0;
        if (sameSnapshotAndDimension && samePosition) {
            return new ActiveAreaState(cachedFallbackAreaName,
                normalizedPlayerDimension, fallbackStateRevision);
        }

        long now = System.currentTimeMillis();
        long elapsed = cachedFallbackCheckTime == Long.MIN_VALUE
            ? Long.MAX_VALUE : now - cachedFallbackCheckTime;
        if (sameSnapshotAndDimension && elapsed >= 0L && elapsed < getDetectionIntervalMillis()) {
            return new ActiveAreaState(cachedFallbackAreaName,
                normalizedPlayerDimension, fallbackStateRevision);
        }

        AreaData detectedArea = AreashintClient.getAreaDetector() == null ? null
            : AreashintClient.getAreaDetector().findAreaForXaeroOverlay(playerX, playerY, playerZ);
        String detectedAreaName = detectedArea == null ? null : detectedArea.getName();
        if (!Objects.equals(cachedFallbackDimensionId, playerDimensionId)
                || !Objects.equals(cachedFallbackAreaName, detectedAreaName)) {
            fallbackStateRevision++;
        }
        cachedFallbackSnapshotRevision = snapshot.revision();
        cachedFallbackDimensionId = playerDimensionId;
        cachedFallbackX = playerX;
        cachedFallbackY = playerY;
        cachedFallbackZ = playerZ;
        cachedFallbackCheckTime = now;
        cachedFallbackAreaName = detectedAreaName;
        return new ActiveAreaState(detectedAreaName, normalizedPlayerDimension, fallbackStateRevision);
    }

    private long getDetectionIntervalMillis() {
        double frequency = ClientConfig.getFrequency();
        if (!Double.isFinite(frequency) || frequency <= 0.0D) {
            return 1000L;
        }
        return Math.max(1L, Math.round(1000.0D / frequency));
    }

    private FillPlan buildPlan(OverlaySnapshot snapshot, OverlayArea activeArea) {
        List<FillTriangle> activeMask = activeArea == null
            ? List.of() : convertTriangles(activeArea);
        Set<String> ancestorNames = findAncestorNames(snapshot, activeArea);
        Map<String, List<FillTriangle>> trianglesByAreaName = new LinkedHashMap<>();

        for (OverlayArea area : snapshot.areas()) {
            List<FillTriangle> source = convertTriangles(area);
            List<FillTriangle> resolved;
            if (activeArea != null && activeArea.name().equals(area.name())) {
                resolved = List.of();
            } else if (ancestorNames.contains(area.name())) {
                resolved = subtractMesh(source, activeMask);
            } else {
                resolved = source;
            }
            trianglesByAreaName.put(area.name(), resolved);
        }
        return new FillPlan(activeArea, trianglesByAreaName);
    }

    /**
     * 只沿当前域名声明的父链挖空，兄弟域名和未进入的子域名仍保留完整填充。
     */
    private Set<String> findAncestorNames(OverlaySnapshot snapshot, OverlayArea activeArea) {
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

    private List<FillTriangle> convertTriangles(OverlayArea area) {
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

    private boolean isValidIndex(OverlayArea area, int index) {
        return index >= 0 && index < area.vertices().size();
    }

    /**
     * 依次减去当前域名的每个三角形，遮罩三角形互不重叠时结果等于减去完整当前域名。
     */
    private List<FillTriangle> subtractMesh(List<FillTriangle> source, List<FillTriangle> mask) {
        if (source.isEmpty() || mask.isEmpty()) {
            return source;
        }

        List<FillTriangle> result = new ArrayList<>();
        for (FillTriangle sourceTriangle : source) {
            List<List<Point>> pieces = new ArrayList<>();
            pieces.add(List.of(sourceTriangle.first(), sourceTriangle.second(), sourceTriangle.third()));
            for (FillTriangle maskTriangle : mask) {
                if (pieces.isEmpty()) {
                    break;
                }
                List<List<Point>> nextPieces = new ArrayList<>();
                for (List<Point> piece : pieces) {
                    nextPieces.addAll(subtractTriangle(piece, maskTriangle));
                }
                pieces = nextPieces;
            }
            addTriangleFans(result, pieces);
        }
        return List.copyOf(result);
    }

    /**
     * 用遮罩三角形的三条有向边逐次划分候选多边形，已经落在任一边外侧的部分立即保留。
     */
    private List<List<Point>> subtractTriangle(List<Point> subject, FillTriangle mask) {
        double orientation = cross(mask.first(), mask.second(), mask.third());
        if (Math.abs(orientation) <= GEOMETRY_EPSILON) {
            return List.of(subject);
        }

        double insideSign = orientation > 0.0D ? 1.0D : -1.0D;
        List<List<Point>> candidates = new ArrayList<>();
        candidates.add(subject);
        List<List<Point>> outsidePieces = new ArrayList<>();
        Point[] maskPoints = {mask.first(), mask.second(), mask.third()};

        for (int edge = 0; edge < maskPoints.length && !candidates.isEmpty(); edge++) {
            Point edgeStart = maskPoints[edge];
            Point edgeEnd = maskPoints[(edge + 1) % maskPoints.length];
            List<List<Point>> nextCandidates = new ArrayList<>();
            for (List<Point> candidate : candidates) {
                PolygonSplit split = splitByLine(candidate, edgeStart, edgeEnd, insideSign);
                if (isUsablePolygon(split.outside())) {
                    outsidePieces.add(split.outside());
                }
                if (isUsablePolygon(split.inside())) {
                    nextCandidates.add(split.inside());
                }
            }
            candidates = nextCandidates;
        }
        return outsidePieces;
    }

    private PolygonSplit splitByLine(List<Point> polygon, Point edgeStart, Point edgeEnd, double insideSign) {
        List<Point> inside = new ArrayList<>(polygon.size() + 1);
        List<Point> outside = new ArrayList<>(polygon.size() + 1);
        Point previous = polygon.get(polygon.size() - 1);
        double previousDistance = signedDistance(edgeStart, edgeEnd, previous, insideSign);
        boolean previousInside = previousDistance >= -GEOMETRY_EPSILON;

        for (Point current : polygon) {
            double currentDistance = signedDistance(edgeStart, edgeEnd, current, insideSign);
            boolean currentInside = currentDistance >= -GEOMETRY_EPSILON;
            if (currentInside != previousInside) {
                Point intersection = interpolateIntersection(previous, current,
                    previousDistance, currentDistance);
                addDistinct(inside, intersection);
                addDistinct(outside, intersection);
            }
            addDistinct(currentInside ? inside : outside, current);
            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return new PolygonSplit(cleanPolygon(inside), cleanPolygon(outside));
    }

    private double signedDistance(Point edgeStart, Point edgeEnd, Point point, double insideSign) {
        return insideSign * cross(edgeStart, edgeEnd, point);
    }

    private Point interpolateIntersection(Point first, Point second,
                                          double firstDistance, double secondDistance) {
        double denominator = firstDistance - secondDistance;
        double ratio = Math.abs(denominator) <= GEOMETRY_EPSILON
            ? 0.0D : firstDistance / denominator;
        ratio = Math.max(0.0D, Math.min(1.0D, ratio));
        return new Point(first.x() + (second.x() - first.x()) * ratio,
            first.z() + (second.z() - first.z()) * ratio);
    }

    private void addDistinct(List<Point> points, Point point) {
        if (points.isEmpty() || !samePoint(points.get(points.size() - 1), point)) {
            points.add(point);
        }
    }

    private List<Point> cleanPolygon(List<Point> polygon) {
        if (polygon.size() > 1 && samePoint(polygon.get(0), polygon.get(polygon.size() - 1))) {
            polygon.remove(polygon.size() - 1);
        }
        return List.copyOf(polygon);
    }

    private boolean samePoint(Point first, Point second) {
        return Math.abs(first.x() - second.x()) <= GEOMETRY_EPSILON
            && Math.abs(first.z() - second.z()) <= GEOMETRY_EPSILON;
    }

    private boolean isUsablePolygon(List<Point> polygon) {
        if (polygon.size() < 3) {
            return false;
        }
        Point origin = polygon.get(0);
        double doubledArea = 0.0D;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            doubledArea += cross(origin, polygon.get(i), polygon.get(i + 1));
        }
        return Math.abs(doubledArea) > GEOMETRY_EPSILON;
    }

    private void addTriangleFans(List<FillTriangle> output, List<List<Point>> polygons) {
        for (List<Point> polygon : polygons) {
            Point origin = polygon.get(0);
            for (int i = 1; i + 1 < polygon.size(); i++) {
                Point second = polygon.get(i);
                Point third = polygon.get(i + 1);
                if (!isDegenerate(origin, second, third)) {
                    output.add(new FillTriangle(origin, second, third));
                }
            }
        }
    }

    private boolean isDegenerate(Point first, Point second, Point third) {
        return Math.abs(cross(first, second, third)) <= GEOMETRY_EPSILON;
    }

    private double cross(Point first, Point second, Point third) {
        return (second.x() - first.x()) * (third.z() - first.z())
            - (second.z() - first.z()) * (third.x() - first.x());
    }

    private record ActiveAreaState(String areaName, String dimensionId, long revision) {
        private static ActiveAreaState empty() {
            return new ActiveAreaState(null, null, Long.MIN_VALUE);
        }
    }

    private record FillCacheKey(long snapshotRevision, String snapshotDimensionId,
                                String detectionDimensionId, long detectionRevision,
                                String activeAreaName) {
    }

    private record CachedFillPlan(FillCacheKey key, FillPlan plan) {
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

    private record PolygonSplit(List<Point> inside, List<Point> outside) {
    }
}
