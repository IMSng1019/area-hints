package areahint.gamemode;

import areahint.data.AreaData;
import areahint.teleport.ServerAreaGeometry;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// 服务端复用既有几何工具，层级选择顺序与客户端 AreaDetector 保持一致。
public final class AreaGamemodeResolver {
    private final Map<Integer, List<AreaData>> byLevel = new TreeMap<>();
    private final Map<String, List<AreaData>> byParent = new HashMap<>();

    public AreaGamemodeResolver(List<AreaData> areas) {
        if (areas == null) {
            return;
        }
        for (AreaData area : areas) {
            if (area == null || area.getVertices() == null || area.getVertices().size() < 3
                    || area.getSecondVertices() == null || area.getSecondVertices().size() != 4
                    || !validVertices(area.getVertices()) || !validVertices(area.getSecondVertices())) {
                continue;
            }
            area.computeCache();
            byLevel.computeIfAbsent(area.getLevel(), ignored -> new ArrayList<>()).add(area);
            if (area.getBaseName() != null) {
                byParent.computeIfAbsent(area.getBaseName(), ignored -> new ArrayList<>()).add(area);
            }
        }
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
        AreaData current = nearestContaining(byLevel.get(1), 1, x, y, z);
        if (current == null) {
            return null;
        }
        for (Map.Entry<Integer, List<AreaData>> entry : byLevel.entrySet()) {
            int level = entry.getKey();
            if (level <= 1) {
                continue;
            }
            List<AreaData> children = current.getName() == null ? null : byParent.get(current.getName());
            // 保留客户端的高度预筛语义：该等级完全没有高度候选时才跳过整个等级。
            boolean hasChild = children != null && children.stream().anyMatch(
                area -> area.getLevel() == level && ServerAreaGeometry.isWithinAltitude(area, y));
            if (!hasChild) {
                if (entry.getValue().stream().anyMatch(area -> ServerAreaGeometry.isWithinAltitude(area, y))) {
                    break;
                }
                continue;
            }
            AreaData child = nearestContaining(children, level, x, y, z);
            if (child == null) {
                break;
            }
            current = child;
        }
        return current;
    }

    private static AreaData nearestContaining(List<AreaData> areas, int level, double x, double y, double z) {
        if (areas == null) {
            return null;
        }
        // 稳定排序确保距离相同时仍选文件中先出现的域名，与标题检测的重叠域名规则一致。
        List<AreaData> candidates = new ArrayList<>();
        for (AreaData area : areas) {
            if (area.getLevel() == level && ServerAreaGeometry.isWithinAltitude(area, y)) {
                candidates.add(area);
            }
        }
        candidates.sort(Comparator.comparingDouble(area -> area.distanceSqToCenter(x, z)));
        for (AreaData area : candidates) {
            if (area.isPointInCachedAABB(x, z)
                    && ServerAreaGeometry.isPointInPolygon(x, z, area.getVertices())) {
                return area;
            }
        }
        return null;
    }
}
