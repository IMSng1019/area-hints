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
    // 当前维度的域名索引快照（volatile整体替换：域名列表与全部索引同一时刻原子可见）
    private volatile AreaIndex index = AreaIndex.EMPTY;

    // 候选列表缓存：每个线程独占一份，热路径不再反复创建临时List（绝不与主线程共享同一实例）
    private final ThreadLocal<List<AreaData>> candidateBuffer = ThreadLocal.withInitial(ArrayList::new);

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

        // 一次性预建全部索引：名称索引、等级分组、子域名分组（键为上级域名name）
        Map<String, AreaData> areasByName = new HashMap<>();
        Map<Integer, List<AreaData>> areasByLevel = new HashMap<>();
        Map<String, List<AreaData>> areasByBaseName = new HashMap<>();
        for (AreaData area : loadedAreas) {
            // 同名域名与旧实现的线性扫描保持一致：取最先出现的那个
            if (area.getName() != null) {
                areasByName.putIfAbsent(area.getName(), area);
            }
            areasByLevel.computeIfAbsent(area.getLevel(), k -> new ArrayList<>()).add(area);
            String baseName = area.getBaseName();
            if (baseName != null) {
                areasByBaseName.computeIfAbsent(baseName, k -> new ArrayList<>()).add(area);
            }
        }

        // 升序去重的等级数组：逐级查找时只遍历数据中真实存在的等级，等级数值异常时也不会空转
        int[] sortedLevels = new int[areasByLevel.size()];
        int levelCursor = 0;
        for (Integer level : areasByLevel.keySet()) {
            sortedLevels[levelCursor++] = level;
        }
        Arrays.sort(sortedLevels);

        // 列表与索引整体发布：检测线程读到的只会是完整一致的旧数据或完整一致的新数据，不会读到半成品
        index = new AreaIndex(loadedAreas, areasByName, areasByLevel, areasByBaseName, sortedLevels);

        // 输出所有加载的区域名称
        if (!loadedAreas.isEmpty()) {
            StringBuilder names = new StringBuilder("加载的区域名称: ");
            for (AreaData area : loadedAreas) {
                names.append(area.getName()).append("(等级:").append(area.getLevel()).append("), ");
            }
            AreashintClient.LOGGER.info(names.toString());
        }

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
        if (AreashintClient.LOGGER.isDebugEnabled()) {
            AreashintClient.LOGGER.debug("玩家在区域: {}, 格式化后的名称: {}", currentArea.getName(), formattedName);
        }
        
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
     * 全程只读一次索引快照，不再重建HashMap分组、不再拷贝排序列表，也不再逐级新建子域名列表
     * @param x 玩家的X坐标
     * @param y 玩家的Y坐标（高度）
     * @param z 玩家的Z坐标
     * @return 玩家所在的区域，如果不在任何区域内则返回null
     */
    private AreaData findArea(double x, double y, double z) {
        // 读取一次索引快照，整个检测过程使用同一份一致的索引
        AreaIndex snapshot = index;

        // 如果没有加载任何区域数据，直接返回null
        if (snapshot.areas.isEmpty()) {
            if (AreashintClient.LOGGER.isDebugEnabled()) {
                AreashintClient.LOGGER.debug("没有加载任何区域数据，无法检测");
            }
            return null;
        }

        // 热路径日志开关：关闭debug时不做任何字符串格式化与参数数组分配
        boolean debugLog = AreashintClient.LOGGER.isDebugEnabled();
        boolean debugInfo = ClientDebugManager.isDebugEnabled();

        // 步骤1: 高度预筛选 + 一级域名命中（直接用预建的等级索引，不再重建分组与排序key）
        List<AreaData> levelOneAreas = snapshot.areasByLevel.get(1);
        int levelOneCount = levelOneAreas == null ? 0 : levelOneAreas.size();
        List<AreaData> candidates = candidateBuffer.get();
        candidates.clear();
        if (levelOneAreas != null) {
            for (AreaData area : levelOneAreas) {
                if (AltitudeFilter.isPlayerInAltitudeRange(y, area)) {
                    candidates.add(area);
                }
            }
        }

        if (debugInfo) {
            ClientDebugManager.sendDebugInfo(DebugCategory.AREA_DETECTION, String.format(
                "高度预筛选(一级域名): 玩家高度=%.1f, 一级域名总数=%d, 筛选后=%d",
                y, levelOneCount, candidates.size()));
        }

        // 一级域名全部被高度筛掉时不可能命中任何域名（旧实现也是直接返回null）
        if (candidates.isEmpty()) {
            if (debugLog) {
                AreashintClient.LOGGER.debug("经过高度预筛选后，没有符合条件的一级域名");
            }
            return null;
        }

        // 按距离原地排序（List.sort是稳定排序，距离相同时保持areas中的原始顺序，命中结果与旧实现一致）
        sortAreasByDistance(candidates, x, z);

        if (debugLog) {
            AreashintClient.LOGGER.debug("检查玩家({}, {})是否在一级域名内，一级域名数量: {}", x, z, candidates.size());
        }

        AreaData inLevelOne = null;
        for (AreaData area : candidates) {
            if (isInsideArea(area, x, z, debugLog)) {
                inLevelOne = area;
                break;
            }
        }

        // 如果玩家不在任何一级域名内
        if (inLevelOne == null) {
            if (debugLog) {
                AreashintClient.LOGGER.debug("玩家不在任何一级域名内");
            }
            return null;
        }
        
        if (debugLog) {
            AreashintClient.LOGGER.debug("玩家在一级域名 {} 内，继续检查更高级别域名", inLevelOne.getName());
        }

        // 步骤2: 沿 baseName 链逐级向下查找子域名（等级语义与旧实现一致：某一级没命中就停止）
        AreaData highestArea = inLevelOne;
        String baseName = inLevelOne.getName();

        for (int levelIndex = 0; levelIndex < snapshot.sortedLevels.length; levelIndex++) {
            int level = snapshot.sortedLevels[levelIndex];
            // 只往下找比一级域名更深的等级（升序，与旧实现的等级遍历顺序一致）
            if (level <= inLevelOne.getLevel()) {
                continue;
            }

            // 用预建的子域名索引直接取出以当前域名为上级的候选，不再逐级新建List收集子域名
            candidates.clear();
            if (baseName != null) {
                List<AreaData> childAreas = snapshot.areasByBaseName.get(baseName);
                if (childAreas != null) {
                    for (AreaData area : childAreas) {
                        // 等级与高度条件与旧实现逐级筛选完全一致
                        if (area.getLevel() == level && AltitudeFilter.isPlayerInAltitudeRange(y, area)) {
                            candidates.add(area);
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                // 旧实现里“该等级完全没有通过高度筛选的域名”会整级跳过，其余情况一律停止查找
                if (hasAreaInAltitudeRange(snapshot.areasByLevel.get(level), y)) {
                    if (debugLog) {
                        AreashintClient.LOGGER.debug("在当前级别没有找到区域，停止查找");
                    }
                    break;
                }
                continue;
            }

            if (debugLog) {
                AreashintClient.LOGGER.debug("检查 {} 级域名，基于上级域名 {}，符合条件的下级域名数量: {}",
                        level, baseName, candidates.size());
            }

            // 按距离原地排序
            sortAreasByDistance(candidates, x, z);

            boolean foundInCurrentLevel = false;

            for (AreaData area : candidates) {
                if (isInsideArea(area, x, z, debugLog)) {
                    highestArea = area;
                    baseName = area.getName();
                    foundInCurrentLevel = true;
                    if (debugLog) {
                        AreashintClient.LOGGER.debug("找到更高级别域名: {}", area.getName());
                    }
                    break;
                }
            }

            // 如果在当前级别没有找到区域，就停止查找
            if (!foundInCurrentLevel) {
                if (debugLog) {
                    AreashintClient.LOGGER.debug("在当前级别没有找到区域，停止查找");
                }
                break;
            }
        }

        if (debugLog) {
            AreashintClient.LOGGER.debug("最终确定玩家在区域: {}", highestArea.getName());
        }
        return highestArea;
    }
    
    /**
     * 判断玩家是否在指定域名内（AABB预筛 + 射线法，判定结果与旧实现完全一致）
     * @param area 域名
     * @param x 玩家的X坐标
     * @param z 玩家的Z坐标
     * @param debugLog 是否输出热路径debug日志
     * @return 是否在域名内
     */
    private boolean isInsideArea(AreaData area, double x, double z, boolean debugLog) {
        // 先用缓存AABB快速排除
        boolean inAABB = area.isCacheComputed() ? area.isPointInCachedAABB(x, z)
            : RayCasting.isPointInAABB(x, z, area.getSecondVertices());
        if (debugLog) {
            AreashintClient.LOGGER.debug("检查区域 {} 的AABB: {}", area.getName(), inAABB ? "在内部" : "在外部");
        }

        if (!inAABB) {
            return false;
        }

        boolean inPolygon = RayCasting.isPointInPolygon(x, z, area.getVertices());
        if (debugLog) {
            AreashintClient.LOGGER.debug("检查区域 {} 的多边形: {}", area.getName(), inPolygon ? "在内部" : "在外部");
        }
        return inPolygon;
    }

    /**
     * 判断某个等级下是否存在通过高度预筛选的域名（只判定，不产生任何列表分配）
     * @param levelAreas 该等级的域名列表
     * @param y 玩家的Y坐标
     * @return 是否存在符合高度条件的域名
     */
    private static boolean hasAreaInAltitudeRange(List<AreaData> levelAreas, double y) {
        if (levelAreas == null) {
            return false;
        }
        for (AreaData area : levelAreas) {
            if (AltitudeFilter.isPlayerInAltitudeRange(y, area)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 按照与玩家的距离原地排序候选域名列表
     * 列表由检测线程独占，因此直接原地排序，不再额外拷贝一层
     * @param candidates 候选域名列表（会被原地排序）
     * @param x 玩家的X坐标
     * @param z 玩家的Z坐标
     */
    private void sortAreasByDistance(List<AreaData> candidates, double x, double z) {
        if (candidates == null || candidates.size() < 2) {
            return;
        }
        
        candidates.sort((a1, a2) -> {
            double d1 = distanceToArea(a1, x, z);
            double d2 = distanceToArea(a2, x, z);
            return Double.compare(d1, d2);
        });
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
        
        // 读取一次快照，保证层级链构建使用的列表与索引来自同一代数据
        List<AreaData> snapshotAreas = index.areas;
        
        switch (style) {
            case "full":
                // 显示完整路径（带颜色）
                result = DomainRenderer.buildDomainDisplayText(area, snapshotAreas);
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
                    result = DomainRenderer.buildDomainDisplayText(area, snapshotAreas);
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
        
        // 直接用预建的名称索引，避免全量线性扫描
        return index.areasByName.get(name);
    }

    /**
     * 域名索引快照
     * 域名列表与三个索引在同一时刻通过一次volatile写入整体发布，检测线程读到的永远是完整一致的一份数据
     */
    private static final class AreaIndex {
        // 尚未加载任何域名数据时使用的空索引
        static final AreaIndex EMPTY = new AreaIndex(Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), new int[0]);

        final List<AreaData> areas;                            // 当前维度的全部域名（与旧实现的areas顺序一致）
        final Map<String, AreaData> areasByName;               // 域名name -> AreaData
        final Map<Integer, List<AreaData>> areasByLevel;       // 等级 -> 该等级的域名列表
        final Map<String, List<AreaData>> areasByBaseName;     // 上级域名name -> 子域名列表
        final int[] sortedLevels;                              // 数据中出现的等级，升序去重

        AreaIndex(List<AreaData> areas, Map<String, AreaData> areasByName,
                  Map<Integer, List<AreaData>> areasByLevel, Map<String, List<AreaData>> areasByBaseName,
                  int[] sortedLevels) {
            this.areas = areas;
            this.areasByName = areasByName;
            this.areasByLevel = areasByLevel;
            this.areasByBaseName = areasByBaseName;
            this.sortedLevels = sortedLevels;
        }
    }
} 
