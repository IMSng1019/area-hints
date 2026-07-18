package areahint.detection;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.util.AreaDataConverter;
import areahint.debug.ClientDebugManager;
import areahint.debug.ClientDebugManager.DebugCategory;
import areahint.render.DomainRenderer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 区域检测器类
 * 用于检测玩家所在的区域
 */
public class AreaDetector {
    // 当前维度的区域数据（volatile保证异步线程可见性）
    private volatile List<AreaData> areas = new ArrayList<>();

    // 按区域等级分组的区域数据
    private volatile Map<Integer, List<AreaData>> areasByLevel = new HashMap<>();
    
    // 最后一次检测的时间（毫秒）
    private long lastDetectionTime = 0;
    
    /**
     * 加载区域数据
     * @param fileName 区域数据文件名
     */
    public void loadAreaData(String fileName) {
        // 使用客户端世界文件夹管理器获取文件路径
        Path areaFile = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
        List<AreaData> loadedAreas = FileManager.readAreaData(areaFile);
        AreashintClient.LOGGER.info("已加载区域数据: {} 个区域", loadedAreas.size());

        // 预计算AABB边界和中心点缓存
        for (AreaData area : loadedAreas) {
            area.computeCache();
        }

        areas = loadedAreas;
        
        // 输出所有加载的区域名称
        if (!areas.isEmpty()) {
            StringBuilder names = new StringBuilder("加载的区域名称: ");
            for (AreaData area : areas) {
                names.append(area.getName()).append("(等级:").append(area.getLevel()).append("), ");
            }
            AreashintClient.LOGGER.info(names.toString());
        }
        
        // 按等级分组（构建新map再赋值，保证线程安全）
        Map<Integer, List<AreaData>> newAreasByLevel = new HashMap<>();
        for (AreaData area : loadedAreas) {
            newAreasByLevel.computeIfAbsent(area.getLevel(), k -> new ArrayList<>()).add(area);
        }
        areasByLevel = newAreasByLevel;
        
        // 输出按等级分组的信息
        for (Map.Entry<Integer, List<AreaData>> entry : areasByLevel.entrySet()) {
            AreashintClient.LOGGER.info("等级 {} 的区域数量: {}", entry.getKey(), entry.getValue().size());
        }
    }
    
    /**
     * 检测玩家所在的区域
     * @param x 玩家的X坐标
     * @param y 玩家的Y坐标（高度）
     * @param z 玩家的Z坐标
     * @return 玩家所在的区域名称（根据配置的样式格式化），如果不在任何区域内则返回null
     */
    public String detectPlayerArea(double x, double y, double z) {
        // 如果模组被禁用，直接返回null，不进行任何检测
        if (!ClientConfig.isEnabled()) {
            return null;
        }
        
        // 更新最后一次检测时间
        lastDetectionTime = System.currentTimeMillis();
        
        // 获取玩家当前所在的区域
        if (ClientDebugManager.isDebugEnabled()) {
            ClientDebugManager.sendDebugInfo(DebugCategory.PLAYER_POSITION, 
                String.format("检测玩家位置: (%.2f, %.2f, %.2f)", x, y, z));
        }
        
        AreaData currentArea = findArea(x, y, z);
        
        // 如果没有找到区域
        if (currentArea == null) {
            if (ClientDebugManager.isDebugEnabled()) {
                ClientDebugManager.sendDebugInfo(DebugCategory.AREA_DETECTION, "没有找到玩家所在区域");
            }
            return null;
        }
        
        // 根据配置的样式格式化区域名称
        String style = ClientConfig.getTitleStyle();
        String formattedName = formatAreaName(currentArea, style);
        AreashintClient.LOGGER.debug("玩家在区域: {}, 格式化后的名称: {}", currentArea.getName(), formattedName);
        
        if (ClientDebugManager.isDebugEnabled()) {
            ClientDebugManager.sendDebugInfo(DebugCategory.AREA_DETECTION, 
                String.format("玩家在区域: %s, 等级: %d, 格式化后: %s", 
                currentArea.getName(), currentArea.getLevel(), formattedName));
        }
        
        return formattedName;
    }
    
    /**
     * 检查是否应该进行检测（根据配置的频率）
     * @return 如果应该进行检测返回true，否则返回false
     */
    public boolean shouldDetect() {
        double frequency = ClientConfig.getFrequency();
        long interval = Math.max(1L, Math.round(1000.0 / frequency)); // 小数频率也按每秒次数换算为毫秒间隔
        return System.currentTimeMillis() - lastDetectionTime >= interval;
    }
    
    /**
     * 根据已有的AreaData格式化区域名称（避免重复检测）
     */
    public String formatAreaNameFromData(AreaData area) {
        if (area == null) return null;
        return formatAreaName(area, ClientConfig.getTitleStyle());
    }

    /**
     * 查找玩家所在的区域（返回原始AreaData，不格式化）
     * @param x 玩家的X坐标
     * @param y 玩家的Y坐标（高度）
     * @param z 玩家的Z坐标
     * @return 玩家所在的区域数据，如果不在任何区域内则返回null
     */
    public AreaData findAreaRaw(double x, double y, double z) {
        // 模组关闭时禁止任何域名计算，仍保留已同步到本地的文件数据。
        if (!ClientConfig.isEnabled()) {
            return null;
        }

        return findArea(x, y, z);
    }

    /**
     * 为独立开启的 Xaero 覆盖层执行同一套域名命中，不受主标题显示开关限制。
     * @param x 玩家X坐标
     * @param y 玩家Y坐标
     * @param z 玩家Z坐标
     * @return 玩家所在的最深层域名，不在任何域名内时返回null
     */
    public AreaData findAreaForXaeroOverlay(double x, double y, double z) {
        return findArea(x, y, z);
    }

    /**
     * 查找玩家所在的区域
     * @param x 玩家的X坐标
     * @param y 玩家的Y坐标（高度）
     * @param z 玩家的Z坐标
     * @return 玩家所在的区域，如果不在任何区域内则返回null
     */
    private AreaData findArea(double x, double y, double z) {
        // 如果没有加载任何区域数据，直接返回null
        if (areas.isEmpty()) {
            AreashintClient.LOGGER.debug("没有加载任何区域数据，无法检测");
            return null;
        }
        
        // 步骤1: 高度预筛选
        List<AreaData> filteredAreas = AltitudeFilter.filterByAltitude(y, areas);
        if (filteredAreas.isEmpty()) {
            AreashintClient.LOGGER.debug("经过高度预筛选后，没有符合条件的区域");
            return null;
        }
        
        // 重新按等级分组筛选后的区域
        Map<Integer, List<AreaData>> filteredAreasByLevel = new HashMap<>();
        for (AreaData area : filteredAreas) {
            filteredAreasByLevel.computeIfAbsent(area.getLevel(), k -> new ArrayList<>()).add(area);
        }
        
        // 按照区域等级排序（1为顶级域名，2为二级域名，以此类推）
        List<Integer> levels = new ArrayList<>(filteredAreasByLevel.keySet());
        Collections.sort(levels);
        
        // 先检查玩家是否在一级域名内
        List<AreaData> levelOneAreas = filteredAreasByLevel.getOrDefault(1, Collections.emptyList());
        AreaData inLevelOne = null;
        
        AreashintClient.LOGGER.debug("检查玩家({}, {})是否在一级域名内，一级域名数量: {}", x, z, levelOneAreas.size());
        
        // 对每个一级域名进行距离排序
        List<AreaData> sortedLevelOneAreas = sortAreasByDistance(levelOneAreas, x, z);
        
        for (AreaData area : sortedLevelOneAreas) {
            // 先用缓存AABB快速排除
            boolean inAABB = area.isCacheComputed() ? area.isPointInCachedAABB(x, z)
                : RayCasting.isPointInAABB(x, z, area.getSecondVertices());
            AreashintClient.LOGGER.debug("检查区域 {} 的AABB: ", area.getName(), inAABB ? "在内部" : "在外部");

            if (inAABB) {
                boolean inPolygon = RayCasting.isPointInPolygon(x, z, area.getVertices());
                AreashintClient.LOGGER.debug("检查区域 {} 的多边形: {}", area.getName(), inPolygon ? "在内部" : "在外部");

                if (inPolygon) {
                    inLevelOne = area;
                    break;
                }
            }
        }

        // 如果玩家不在任何一级域名内
        if (inLevelOne == null) {
            AreashintClient.LOGGER.debug("玩家不在任何一级域名内");
            return null;
        }
        
        AreashintClient.LOGGER.debug("玩家在一级域名 {} 内，继续检查更高级别域名", inLevelOne.getName());
        
        // 继续检查更高级别的域名
        AreaData highestArea = inLevelOne;
        String baseName = inLevelOne.getName();
        
        for (int i = 1; i < levels.size(); i++) {
            int currentLevel = levels.get(i);
            List<AreaData> currentLevelAreas = filteredAreasByLevel.getOrDefault(currentLevel, Collections.emptyList());
            
            // 只检查基于当前域名的下一级域名
            List<AreaData> childAreas = new ArrayList<>();
            for (AreaData area : currentLevelAreas) {
                if (baseName.equals(area.getBaseName())) {
                    childAreas.add(area);
                }
            }
            
            AreashintClient.LOGGER.debug("检查 {} 级域名，基于上级域名 {}，符合条件的下级域名数量: {}", 
                    currentLevel, baseName, childAreas.size());
            
            // 按距离排序
            List<AreaData> sortedChildAreas = sortAreasByDistance(childAreas, x, z);
            
            boolean foundInCurrentLevel = false;
            
            for (AreaData area : sortedChildAreas) {
                // 先用缓存AABB快速排除
                boolean inAABB = area.isCacheComputed() ? area.isPointInCachedAABB(x, z)
                    : RayCasting.isPointInAABB(x, z, area.getSecondVertices());
                AreashintClient.LOGGER.debug("检查区域 {} 的AABB: {}", area.getName(), inAABB ? "在内部" : "在外部");

                if (inAABB) {
                    boolean inPolygon = RayCasting.isPointInPolygon(x, z, area.getVertices());
                    AreashintClient.LOGGER.debug("检查区域 {} 的多边形: {}", area.getName(), inPolygon ? "在内部" : "在外部");
                    
                    if (inPolygon) {
                        highestArea = area;
                        baseName = area.getName();
                        foundInCurrentLevel = true;
                        AreashintClient.LOGGER.debug("找到更高级别域名: {}", area.getName());
                        break;
                    }
                }
            }
            
            // 如果在当前级别没有找到区域，就停止查找
            if (!foundInCurrentLevel) {
                AreashintClient.LOGGER.debug("在当前级别没有找到区域，停止查找");
                break;
            }
        }
        
        AreashintClient.LOGGER.debug("最终确定玩家在区域: {}", highestArea.getName());
        return highestArea;
    }
    
    /**
     * 按照与玩家的距离排序区域列表
     * @param areas 区域列表
     * @param x 玩家的X坐标
     * @param z 玩家的Z坐标
     * @return 排序后的区域列表
     */
    private List<AreaData> sortAreasByDistance(List<AreaData> areas, double x, double z) {
        if (areas == null || areas.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 创建一个新列表，避免修改原始列表
        List<AreaData> result = new ArrayList<>(areas);
        
        // 按到玩家的距离排序
        result.sort((a1, a2) -> {
            double d1 = distanceToArea(a1, x, z);
            double d2 = distanceToArea(a2, x, z);
            return Double.compare(d1, d2);
        });
        
        return result;
    }
    
    /**
     * 计算点到区域的近似距离
     * 这里使用到多边形中心点的距离作为近似
     * @param area 区域
     * @param x 点的X坐标
     * @param z 点的Z坐标
     * @return 距离
     */
    private double distanceToArea(AreaData area, double x, double z) {
        if (area.isCacheComputed()) {
            return area.distanceSqToCenter(x, z); // 用平方距离排序即可，无需sqrt
        }
        double centerX = 0, centerZ = 0;
        List<AreaData.Vertex> vertices = area.getVertices();
        for (AreaData.Vertex vertex : vertices) {
            centerX += vertex.getX();
            centerZ += vertex.getZ();
        }
        centerX /= vertices.size();
        centerZ /= vertices.size();
        double dx = x - centerX, dz = z - centerZ;
        return dx * dx + dz * dz;
    }
    
    /**
     * 根据配置的样式格式化区域名称
     * @param area 区域
     * @param style 样式（full、simple、mixed）
     * @return 格式化后的区域名称（带颜色）
     */
    private String formatAreaName(AreaData area, String style) {
        if (area == null) {
            return null;
        }
        
        String result;
        
        switch (style) {
            case "full":
                // 显示完整路径（带颜色）
                result = DomainRenderer.buildDomainDisplayText(area, areas);
                break;
            case "simple":
                // 仅显示当前级别（带颜色）
                result = DomainRenderer.getSimpleDomainText(area);
                break;
            case "mixed":
                // 混合模式（带颜色）
                if (area.getLevel() == 1) {
                    // 一级域名只显示自身
                    result = DomainRenderer.getSimpleDomainText(area);
                } else if (area.getLevel() == 2) {
                    // 二级域名显示一级+二级
                    result = DomainRenderer.buildDomainDisplayText(area, areas);
                } else {
                    // 三级及以上只显示当前级别
                    result = DomainRenderer.getSimpleDomainText(area);
                }
                break;
            default:
                result = DomainRenderer.getSimpleDomainText(area);
                break;
        }
        
        return result;
    }
    
    /**
     * 构建区域的完整路径
     * @param area 区域
     * @return 完整路径
     */
    private String buildFullPath(AreaData area) {
        if (area == null) {
            return "";
        }
        
        List<String> path = new ArrayList<>();
        path.add(AreaDataConverter.getDisplayName(area));
        
        AreaData current = area;
        while (current.getLevel() > 1 && current.getBaseName() != null) {
            AreaData parent = findAreaByName(current.getBaseName());
            if (parent == null) {
                break;
            }
            
            path.add(0, parent.getName());
            current = parent;
        }
        
        return String.join("·", path);
    }
    
    /**
     * 根据名称查找区域
     * @param name 区域名称
     * @return 找到的区域，如果未找到则返回null
     */
    private AreaData findAreaByName(String name) {
        if (name == null) {
            return null;
        }
        
        for (AreaData area : areas) {
            if (name.equals(area.getName())) {
                return area;
            }
        }
        
        return null;
    }
} 
