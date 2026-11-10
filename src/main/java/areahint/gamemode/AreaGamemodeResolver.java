package areahint.gamemode;

import areahint.data.AreaData;
import areahint.geometry.AreaSpatialIndex;
import areahint.teleport.ServerAreaGeometry;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// 服务端复用既有几何工具，层级选择顺序与客户端 AreaDetector 保持一致。
public final class AreaGamemodeResolver {
    private final Map<Integer, List<AreaData>> byLevel = new TreeMap<>();
    // 一级域名和各父域名分别建立空间索引，检测时不再扫描整组远处域名。
    private final AreaSpatialIndex levelOneIndex;
    private final Map<String, AreaSpatialIndex> byParent = new HashMap<>();
    // 查询只写入当前线程自己的候选列表，同一份服务端快照可安全接受独立查询。
    private final ThreadLocal<List<AreaData>> candidateBuffer = ThreadLocal.withInitial(ArrayList::new);

    public AreaGamemodeResolver(List<AreaData> areas) {
        if (areas == null) {
            levelOneIndex = new AreaSpatialIndex(List.of());
            return;
        }
        Map<String, List<AreaData>> childrenByParent = new HashMap<>();
        for (AreaData area : areas) {
            if (area == null || area.getVertices() == null || area.getVertices().size() < 3
                    || area.getSecondVertices() == null || area.getSecondVertices().size() != 4
                    || !validVertices(area.getVertices()) || !validVertices(area.getSecondVertices())) {
                continue;
            }
            area.computeCache();
            byLevel.computeIfAbsent(area.getLevel(), ignored -> new ArrayList<>()).add(area);
            if (area.getBaseName() != null) {
                childrenByParent.computeIfAbsent(area.getBaseName(), ignored -> new ArrayList<>()).add(area);
            }
        }
        // 索引仅在加载快照时构建，输入顺序保留文件顺序以维持同距重叠域名的选择规则。
        levelOneIndex = new AreaSpatialIndex(byLevel.getOrDefault(1, List.of()));
        childrenByParent.forEach((parent, children) -> byParent.put(parent, new AreaSpatialIndex(children)));
    }

    // 不让损坏的顶点进入服务端每刻回调；其余有效域名仍正常参与游戏模式判断。
    private static boolean validVertices(List<AreaData.Vertex> vertices) {
        return vertices.stream().allMatch(vertex -> vertex != null
            && Double.isFinite(vertex.getX()) && Double.isFinite(vertex.getZ()));
    }

    // 配置和域名字段使用同一组严格的小写名称，非法值不交给 GameMode 的隐式回退处理。
    public static GameMode parseMode(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "survival" -> GameMode.SURVIVAL;
            case "creative" -> GameMode.CREATIVE;
            case "adventure" -> GameMode.ADVENTURE;
            case "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    public static GameMode requiredMode(AreaData area, GameMode fallback) {
        GameMode explicit = area == null ? null : parseMode(area.getGamemode());
        return explicit == null ? fallback : explicit;
    }

    public AreaData findArea(double x, double y, double z) {
        AreaData current = nearestContaining(levelOneIndex, 1, x, y, z);
        if (current == null) {
            return null;
        }
        for (Map.Entry<Integer, List<AreaData>> entry : byLevel.entrySet()) {
            int level = entry.getKey();
            if (level <= 1) {
                continue;
            }
            AreaSpatialIndex children = current.getName() == null ? null : byParent.get(current.getName());
            AreaData child = nearestContaining(children, level, x, y, z);
            if (child == null) {
                // 空间未命中不能视为整级无候选，只有该等级全局都不满足高度时才允许跳级。
                if (entry.getValue().stream().anyMatch(area -> ServerAreaGeometry.isWithinAltitude(area, y))) {
                    break;
                }
                continue;
            }
            current = child;
        }
        return current;
    }

    private AreaData nearestContaining(AreaSpatialIndex spatialIndex, int level, double x, double y, double z) {
        if (spatialIndex == null) {
            return null;
        }
        // 空间索引先筛选等级、高度和AABB，再仅对附近候选按中心距离及原文件顺序排序。
        List<AreaData> candidates = candidateBuffer.get();
        try {
            spatialIndex.collectCandidates(x, y, z, level, candidates);
            for (AreaData area : candidates) {
                // 包围盒只负责缩小范围，多边形边界仍由原射线法决定，避免改变入域判定。
                if (ServerAreaGeometry.isPointInPolygon(x, z, area.getVertices())) {
                    return area;
                }
            }
            return null;
        } finally {
            // 保留列表容量但释放域名引用，避免重载后旧快照残留在长寿命服务端线程中。
            candidates.clear();
        }
    }
}
