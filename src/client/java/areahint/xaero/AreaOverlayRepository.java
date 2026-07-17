package areahint.xaero;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.geometry.PolygonGeometry;
import areahint.network.Packets;
import areahint.world.ClientWorldFolderManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Xaero 覆盖层使用的不可变域名快照，文件读取和三角化只在数据更新时发生。
 */
public final class AreaOverlayRepository {
    private static final AreaOverlayRepository INSTANCE = new AreaOverlayRepository();

    private final AtomicLong revisionCounter = new AtomicLong();
    private volatile Map<String, OverlaySnapshot> snapshots = Map.of();

    private AreaOverlayRepository() {
    }

    public static AreaOverlayRepository getInstance() {
        return INSTANCE;
    }

    public synchronized void refreshDimension(String dimension) {
        String dimensionId = normalizeDimensionId(dimension);
        String dimensionType = toDimensionType(dimensionId);
        String fileName = Packets.getFileNameForDimension(dimensionType);
        if (dimensionId == null || fileName == null) {
            return;
        }

        try {
            Path areaFile = ClientWorldFolderManager.getWorldDimensionFile(fileName);
            List<AreaData> source = FileManager.readAreaData(areaFile);
            OverlaySnapshot snapshot = buildSnapshot(dimensionId, source);
            Map<String, OverlaySnapshot> updated = new HashMap<>(snapshots);
            updated.put(dimensionId, snapshot);
            snapshots = Map.copyOf(updated);
            AreashintClient.LOGGER.debug("已刷新 Xaero 域名覆盖层快照: {}，{} 个域名", dimensionId, snapshot.areas().size());
        } catch (Exception e) {
            AreashintClient.LOGGER.warn("刷新 Xaero 域名覆盖层快照失败: {}", dimensionId, e);
        }
    }

    public synchronized void refreshAll() {
        refreshDimension(Packets.DIMENSION_OVERWORLD);
        refreshDimension(Packets.DIMENSION_NETHER);
        refreshDimension(Packets.DIMENSION_END);
    }

    public synchronized void clear() {
        snapshots = Map.of();
        revisionCounter.incrementAndGet();
    }

    public OverlaySnapshot getSnapshot(String dimension) {
        String dimensionId = normalizeDimensionId(dimension);
        if (dimensionId == null) {
            return OverlaySnapshot.empty("");
        }
        return snapshots.getOrDefault(dimensionId, OverlaySnapshot.empty(dimensionId));
    }

    public List<OverlayArea> queryPoint(String dimension, double x, double z) {
        List<OverlayArea> matches = new ArrayList<>();
        for (OverlayArea area : getSnapshot(dimension).areas()) {
            if (area.contains(x, z)) {
                matches.add(area);
            }
        }
        matches.sort(Comparator.comparingInt(OverlayArea::level).reversed()
            .thenComparing(OverlayArea::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(matches);
    }

    public OverlayArea find(String dimension, String areaName) {
        if (areaName == null) {
            return null;
        }
        return getSnapshot(dimension).byName().get(areaName);
    }

    public static String normalizeDimensionId(String dimension) {
        if (dimension == null || dimension.trim().isEmpty()) {
            return null;
        }
        String value = dimension.trim().toLowerCase();
        int colon = value.lastIndexOf(':');
        String path = colon >= 0 ? value.substring(colon + 1) : value;
        return switch (path) {
            case Packets.DIMENSION_OVERWORLD -> "minecraft:overworld";
            case Packets.DIMENSION_NETHER -> "minecraft:the_nether";
            case Packets.DIMENSION_END -> "minecraft:the_end";
            default -> null;
        };
    }

    public static String toDimensionType(String dimension) {
        String dimensionId = normalizeDimensionId(dimension);
        if (dimensionId == null) {
            return null;
        }
        return Packets.convertDimensionPathToType(dimensionId.substring(dimensionId.lastIndexOf(':') + 1));
    }

    private OverlaySnapshot buildSnapshot(String dimensionId, List<AreaData> source) {
        List<OverlayArea> areas = new ArrayList<>();
        if (source != null) {
            for (AreaData area : source) {
                OverlayArea overlayArea = buildArea(area);
                if (overlayArea != null) {
                    areas.add(overlayArea);
                }
            }
        }
        areas.sort(Comparator.comparingInt(OverlayArea::level)
            .thenComparing(OverlayArea::name, String.CASE_INSENSITIVE_ORDER));
        Map<String, OverlayArea> byName = new LinkedHashMap<>();
        for (OverlayArea area : areas) {
            byName.put(area.name(), area);
        }
        return new OverlaySnapshot(dimensionId, List.copyOf(areas), Map.copyOf(byName), revisionCounter.incrementAndGet());
    }

    private OverlayArea buildArea(AreaData area) {
        if (area == null || area.getName() == null || area.getVertices() == null || area.getVertices().size() < 3) {
            return null;
        }

        List<Point> vertices = new ArrayList<>();
        for (AreaData.Vertex vertex : area.getVertices()) {
            vertices.add(new Point(vertex.getX(), vertex.getZ()));
        }

        List<AreaData.Vertex> boundsSource = area.getSecondVertices() != null && area.getSecondVertices().size() == 4
            ? area.getSecondVertices() : area.getVertices();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (AreaData.Vertex vertex : boundsSource) {
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minZ = Math.min(minZ, vertex.getZ());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        Point center = calculateCentroid(vertices);
        List<Triangle> triangles = new ArrayList<>();
        for (int[] triangle : PolygonGeometry.triangulate(area.getVertices())) {
            if (triangle.length == 3) {
                triangles.add(new Triangle(triangle[0], triangle[1], triangle[2]));
            }
        }

        Double minY = area.getAltitude() == null ? null : area.getAltitude().getMin();
        Double maxY = area.getAltitude() == null ? null : area.getAltitude().getMax();
        return new OverlayArea(area.getName(), clean(area.getSurfacename()), area.getLevel(), clean(area.getBaseName()),
            List.copyOf(area.getAllSignatures()), area.getColor(), minY, maxY, List.copyOf(vertices),
            List.copyOf(triangles), minX, maxX, minZ, maxZ, center.x(), center.z(), stablePhase(area.getName()));
    }

    private static Point calculateCentroid(List<Point> vertices) {
        double area = 0.0D;
        double centerX = 0.0D;
        double centerZ = 0.0D;
        for (int i = 0; i < vertices.size(); i++) {
            Point current = vertices.get(i);
            Point next = vertices.get((i + 1) % vertices.size());
            double cross = current.x() * next.z() - next.x() * current.z();
            area += cross;
            centerX += (current.x() + next.x()) * cross;
            centerZ += (current.z() + next.z()) * cross;
        }
        if (Math.abs(area) > 1.0E-7D) {
            return new Point(centerX / (3.0D * area), centerZ / (3.0D * area));
        }

        centerX = 0.0D;
        centerZ = 0.0D;
        for (Point vertex : vertices) {
            centerX += vertex.x();
            centerZ += vertex.z();
        }
        return new Point(centerX / vertices.size(), centerZ / vertices.size());
    }

    private static long stablePhase(String name) {
        return Math.floorMod(name == null ? 0L : name.hashCode() * 137L, 6000L);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public record Point(double x, double z) {
    }

    public record Triangle(int first, int second, int third) {
    }

    public record OverlayArea(String name, String surfaceName, int level, String baseName, List<String> signatures,
                              String color, Double minY, Double maxY, List<Point> vertices, List<Triangle> triangles,
                              double minX, double maxX, double minZ, double maxZ,
                              double centerX, double centerZ, long colorPhase) {
        public OverlayArea {
            signatures = List.copyOf(signatures == null ? List.of() : signatures);
            vertices = List.copyOf(vertices == null ? List.of() : vertices);
            triangles = List.copyOf(triangles == null ? List.of() : triangles);
            color = color == null ? "#FFFFFF" : color;
        }

        public boolean contains(double x, double z) {
            if (x < minX || x > maxX || z < minZ || z > maxZ) {
                return false;
            }
            boolean inside = false;
            for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
                Point first = vertices.get(j);
                Point second = vertices.get(i);
                if (isPointOnSegment(x, z, first, second)) {
                    return true;
                }
                boolean crosses = (second.z() > z) != (first.z() > z)
                    && x < (first.x() - second.x()) * (z - second.z())
                    / (first.z() - second.z()) + second.x();
                if (crosses) {
                    inside = !inside;
                }
            }
            return inside;
        }

        private static boolean isPointOnSegment(double x, double z, Point first, Point second) {
            double cross = (x - first.x()) * (second.z() - first.z())
                - (z - first.z()) * (second.x() - first.x());
            if (Math.abs(cross) > 1.0E-7D) {
                return false;
            }
            double dot = (x - first.x()) * (second.x() - first.x())
                + (z - first.z()) * (second.z() - first.z());
            if (dot < 0.0D) {
                return false;
            }
            double lengthSquared = Math.pow(second.x() - first.x(), 2.0D)
                + Math.pow(second.z() - first.z(), 2.0D);
            return dot <= lengthSquared;
        }

        public boolean isVisibleAt(double y) {
            return (minY == null || y >= minY) && (maxY == null || y <= maxY);
        }

        public String displayName() {
            return surfaceName == null ? name : surfaceName;
        }
    }

    public record OverlaySnapshot(String dimensionId, List<OverlayArea> areas,
                                  Map<String, OverlayArea> byName, long revision) {
        public OverlaySnapshot {
            areas = List.copyOf(areas == null ? List.of() : areas);
            byName = Map.copyOf(byName == null ? Map.of() : byName);
        }

        public static OverlaySnapshot empty(String dimensionId) {
            return new OverlaySnapshot(dimensionId, List.of(), Map.of(), 0L);
        }
    }
}
