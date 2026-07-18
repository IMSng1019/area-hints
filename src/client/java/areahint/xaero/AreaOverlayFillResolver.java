package areahint.xaero;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.log.AreaChangeTracker;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.AreaOverlayRepository.Triangle;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
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

    private long cachedRevision = Long.MIN_VALUE;
    private String cachedDimensionId;
    private String cachedActiveAreaName;
    private FillPlan cachedPlan = FillPlan.empty();
    private long cachedDetectionRevision = Long.MIN_VALUE;
    private String cachedDetectionDimensionId;
    private double cachedDetectionX = Double.NaN;
    private double cachedDetectionY = Double.NaN;
    private double cachedDetectionZ = Double.NaN;
    private String cachedDetectedAreaName;

    /**
     * 读取主检测器的当前域名并复用最近一次差集结果，玩家仍在同一域名时不会重复计算网格。
     */
    public FillPlan resolve(OverlaySnapshot snapshot, MinecraftClient client) {
        OverlaySnapshot safeSnapshot = snapshot == null ? OverlaySnapshot.empty("") : snapshot;
        String playerDimensionId = client == null || client.world == null
            ? null : client.world.getRegistryKey().getValue().toString();
        String normalizedPlayerDimension = AreaOverlayRepository.normalizeDimensionId(playerDimensionId);
        String activeAreaName = Objects.equals(safeSnapshot.dimensionId(), normalizedPlayerDimension)
            ? resolveActiveAreaName(safeSnapshot, client, playerDimensionId) : null;
        OverlayArea activeArea = activeAreaName != null
            ? safeSnapshot.byName().get(activeAreaName) : null;
        String resolvedActiveAreaName = activeArea == null ? null : activeArea.name();

        if (cachedRevision == safeSnapshot.revision()
            && Objects.equals(cachedDimensionId, safeSnapshot.dimensionId())
            && Objects.equals(cachedActiveAreaName, resolvedActiveAreaName)) {
            return cachedPlan;
        }

        cachedRevision = safeSnapshot.revision();
        cachedDimensionId = safeSnapshot.dimensionId();
        cachedActiveAreaName = resolvedActiveAreaName;
        cachedPlan = buildPlan(safeSnapshot, activeArea);
        return cachedPlan;
    }

    /**
     * 主功能开启时以 AreaChangeTracker 为权威，关闭时复用 AreaDetector 保持 Xaero 独立开关可用。
     */
    private String resolveActiveAreaName(OverlaySnapshot snapshot, MinecraftClient client,
                                         String playerDimensionId) {
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        if (ClientConfig.isEnabled()) {
            AreaData currentArea = AreaChangeTracker.getCurrentAreaData();
            return currentArea == null ? null : currentArea.getName();
        }

        double playerX = client.player.getX();
        double playerY = client.player.getY();
        double playerZ = client.player.getZ();
        if (cachedDetectionRevision == snapshot.revision()
            && Objects.equals(cachedDetectionDimensionId, playerDimensionId)
            && Double.compare(cachedDetectionX, playerX) == 0
            && Double.compare(cachedDetectionY, playerY) == 0
            && Double.compare(cachedDetectionZ, playerZ) == 0) {
            return cachedDetectedAreaName;
        }

        AreaData detectedArea = AreashintClient.getAreaDetector() == null ? null
            : AreashintClient.getAreaDetector().findAreaForXaeroOverlay(playerX, playerY, playerZ);
        cachedDetectionRevision = snapshot.revision();
        cachedDetectionDimensionId = playerDimensionId;
        cachedDetectionX = playerX;
        cachedDetectionY = playerY;
        cachedDetectionZ = playerZ;
        cachedDetectedAreaName = detectedArea == null ? null : detectedArea.getName();
        return cachedDetectedAreaName;
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
