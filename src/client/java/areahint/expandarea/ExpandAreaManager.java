package areahint.expandarea;

import areahint.commandui.CommandVisualLaunchContext;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.i18n.I18nManager;
import areahint.expandarea.ExpandAreaClientNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

/**
 * 域名扩展管理器
 * 按照提示词实现复杂的几何算法来处理域名扩展
 */
public class ExpandAreaManager {
    private static ExpandAreaManager instance;
    private MinecraftClient client;
    private String selectedAreaName;
    private String selectedDimensionId;
    private AreaData selectedArea;
    private List<Double[]> newVertices;
    private boolean isRecording = false;
    private boolean isActive = false;
    private ExpandAreaUI ui;
    
    public static ExpandAreaManager getInstance() {
        if (instance == null) {
            instance = new ExpandAreaManager();
        }
        return instance;
    }
    
    private ExpandAreaManager() {
        this.client = MinecraftClient.getInstance();
        this.newVertices = new ArrayList<>();
        this.ui = new ExpandAreaUI(this);
    }
    
    /**
     * 开始域名扩展流程
     */
    public void startExpandArea() {
        System.out.println("DEBUG: startExpandArea() 被调用");
        if (client.player == null) {
            System.out.println("DEBUG: client.player 为 null");
            return;
        }
        
        System.out.println("DEBUG: 设置 isActive = true");
        isActive = true;  // 设置为活动状态
        
        // 获取可修改的域名列表
        System.out.println("DEBUG: 开始获取可修改的域名列表");
        List<AreaData> modifiableAreas = getModifiableAreas();
        System.out.println("DEBUG: 找到 " + modifiableAreas.size() + " 个可修改的域名");
        
        if (modifiableAreas.isEmpty()) {
            System.out.println("DEBUG: 没有可扩展的域名");
            sendMessage(I18nManager.translate("expandarea.error.area.expand_4"), Formatting.RED);
            sendMessage(I18nManager.translate("expandarea.message.area.expand_2"), Formatting.GRAY);
            isActive = false;
            return;
        }
        
        // 直接显示域名选择界面
        System.out.println("DEBUG: 显示域名选择界面");
        ui.showAreaSelection(modifiableAreas);
        System.out.println("DEBUG: startExpandArea() 执行完成");
    }

    /**
     * 启动图形选择流程，只初始化状态并返回可扩展域名。
     */
    public List<AreaData> beginVisualSelection() {
        if (client.player == null) {
            return List.of();
        }

        isActive = true;
        List<AreaData> modifiableAreas = getModifiableAreas();
        if (modifiableAreas.isEmpty()) {
            sendMessage(I18nManager.translate("expandarea.error.area.expand_4"), Formatting.RED);
            sendMessage(I18nManager.translate("expandarea.message.area.expand_2"), Formatting.GRAY);
            isActive = false;
        }
        return modifiableAreas;
    }
    
    /**
     * 检查玩家是否有权限修改指定域名
     */
    private boolean checkPermission(AreaData area) {
        if (client.player == null) {
            return false;
        }
        
        String playerName = client.player.getGameProfile().getName();
        
        // 检查是否为管理员（权限等级2）
        if (client.player.hasPermissionLevel(2)) {
            return true;
        }
        
        // 检查是否为域名的basename引用玩家
        if (area.getBaseName() != null) {
            AreaData baseArea = findAreaByName(area.getBaseName());
            if (baseArea != null && baseArea.hasSignature(playerName)) {
                return true;
            }
        }

        // 检查是否为域名签名者
        return area.hasSignature(playerName);
    }
    
    /**
     * 根据域名名称选择域名
     */
    public void selectAreaByName(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            sendMessage(I18nManager.translate("dividearea.error.area"), Formatting.RED);
            return;
        }

        // 移除引号（如果存在）
        String cleanedName = areaName.trim();
        if (cleanedName.startsWith("\"") && cleanedName.endsWith("\"") && cleanedName.length() > 1) {
            cleanedName = cleanedName.substring(1, cleanedName.length() - 1);
        }

        // 从已加载的可修改域名列表中查找域名（而不是重新从文件加载）
        List<AreaData> modifiableAreas = getModifiableAreas();
        AreaData area = null;
        for (AreaData a : modifiableAreas) {
            if (a.getName().equals(cleanedName)) {
                area = a;
                break;
            }
        }

        if (area == null) {
            sendMessage("§c" + I18nManager.translate("addhint.message.area_2") + cleanedName + I18nManager.translate("expandarea.message.expand.permission"), Formatting.RED);
            sendMessage(I18nManager.translate("expandarea.prompt.area.expand.permission"), Formatting.GRAY);
            return;
        }

        // 选择该域名（权限已在getModifiableAreas中检查）
        handleAreaSelection(area);
    }
    
    /**
     * 取消扩展流程
     */
    public void cancel() {
        if (!isActive) {
            return;
        }

        isActive = false;
        isRecording = false;
        selectedArea = null;
        selectedAreaName = null;
        selectedDimensionId = null;
        newVertices.clear();

        // 清除边界可视化的临时顶点
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();

        ui.showCancelMessage();
    }
    
    /**
     * 获取可修改的域名列表
     */
    private List<AreaData> getModifiableAreas() {
        List<AreaData> result = new ArrayList<>();
        String playerName = client.player.getGameProfile().getName();
        boolean isAdmin = client.player.hasPermissionLevel(2);
        
        System.out.println("DEBUG: 玩家名称: " + playerName + ", 是否管理员: " + isAdmin);
        
        // 获取所有域名
        List<AreaData> allAreas = loadAllAreas();
        
        for (AreaData area : allAreas) {
            System.out.println("DEBUG: 检查域名: " + area.getName() + ", 签名: " + area.getSignature() + ", 基础域名: " + area.getBaseName());
            if (isAdmin) {
                // 管理员可以修改所有域名
                result.add(area);
                System.out.println("DEBUG: 管理员权限，添加域名: " + area.getName());
            } else {
                // 普通玩家只能修改自己签名的或basename引用自己签名域名的域名
                if (area.hasSignature(playerName)) {
                    result.add(area);
                    System.out.println("DEBUG: 玩家签名的域名，添加: " + area.getName());
                } else if (area.getBaseName() != null) {
                    AreaData baseArea = findAreaByName(area.getBaseName());
                    if (baseArea != null && baseArea.hasSignature(playerName)) {
                        result.add(area);
                        System.out.println("DEBUG: 基于玩家签名域名的扩展，添加: " + area.getName());
                    }
                }
            }
        }
        
        System.out.println("DEBUG: 找到 " + result.size() + " 个可修改的域名");
        // 地图入口的目标已经由服务端授权，客户端只为本次流程补入该目标，最终写入仍由服务端复核。
        return CommandVisualLaunchContext.ensureTargetIncluded("expandarea", result, allAreas);
    }
    
    /**
     * 处理域名选择
     */
    public void handleAreaSelection(AreaData selectedArea) {
        this.selectedArea = selectedArea;
        this.selectedAreaName = selectedArea.getName();
        // 锁定选择域名时的维度，途中切换维度后服务端会拒绝旧维度请求而不会误改同名域名。
        this.selectedDimensionId = client.world == null ? null
            : client.world.getRegistryKey().getValue().toString();
        
        sendMessage(I18nManager.translate("dividearea.prompt.area") + areahint.util.AreaDataConverter.getDisplayName(selectedArea), Formatting.GREEN);
        sendMessage(I18nManager.translate("expandarea.button.area.record.save"), Formatting.GRAY);
        
        // 开始记录模式
        startRecording();
    }
    
    /**
     * 开始记录新顶点
     */
    private void startRecording() {
        this.isActive = true;
        this.isRecording = true;
        this.newVertices.clear();
        ui.showRecordingInterface();
    }
    
    /**
     * 记录当前位置作为新顶点
     * 坐标会被取整为整数
     */
    public void recordCurrentPosition() {
        if (!isRecording || client.player == null) {
            return;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        // 取整为整数
        int roundedX = (int) Math.round(x);
        int roundedZ = (int) Math.round(z);
        newVertices.add(new Double[]{(double) roundedX, (double) roundedZ});

        sendMessage(I18nManager.translate("dividearea.message.record_3") + newVertices.size() + ": §6(" +
                   roundedX + ", " + String.format("%.1f", y) + ", " + roundedZ + ")",
                   Formatting.GREEN);

        // 更新边界可视化的临时顶点
        List<net.minecraft.util.math.BlockPos> blockPosList = new java.util.ArrayList<>();
        for (Double[] vertex : newVertices) {
            blockPosList.add(new net.minecraft.util.math.BlockPos(vertex[0].intValue(), (int) y, vertex[1].intValue()));
        }
        areahint.boundviz.BoundVizManager.getInstance().setTemporaryVertices(blockPosList, true);

        // 显示选项按钮（参考EasyAdd的实现）
        ui.showPointRecordedOptions(newVertices.size());
    }
    
    /**
     * 完成记录并处理扩展
     */
    public void finishRecording() {
        if (!isRecording || newVertices.size() < 3) {
            sendMessage(I18nManager.translate("expandarea.error.vertex.record"), Formatting.RED);
            return;
        }
        
        this.isRecording = false;
        
        try {
            // 进行几何计算和域名扩展
            processAreaExpansion();
        } catch (Exception e) {
            sendMessage(I18nManager.translate("expandarea.error.area.expand_3") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        }
    }
    
    /**
     * 处理域名扩展的核心逻辑
     * 按照提示词实现复杂的几何算法
     */
    private void processAreaExpansion() {
        sendMessage(I18nManager.translate("expandarea.message.area.expand_3"), Formatting.YELLOW);
        
        try {
            // 1. 高度验证 - 按照提示词逻辑
            if (!validateHeightsAccordingToPrompt()) {
                return;
            }
            
            // 2. 提取原域名顶点
            List<Double[]> originalVertices = extractOriginalVertices();
            
            // 3. 计算新添加区域的高度范围
            double[] newAreaHeightRange = calculateNewAreaHeightRange();
            
            // 4. 检查新添加顶点是否在原域名内，删除内部顶点
            List<Double[]> externalVertices = filterExternalVertices(originalVertices);
            
            if (externalVertices.isEmpty()) {
                sendMessage(I18nManager.translate("expandarea.error.area.vertex.add"), Formatting.RED);
                return;
            }
            
            // 5. 计算边界点 - 按照提示词的复杂算法
            // List<Double[]> boundaryPoints = calculateBoundaryPoints(originalVertices, externalVertices);
            
            // 6. 计算临近点和边界点
            List<Double[]> adjacentPoints = calculateAdjacentPoints(originalVertices, externalVertices);
            List<Double[]> finalBoundaryPoints = calculateFinalBoundaryPoints(adjacentPoints, originalVertices);
            
            // 7. 合并顶点并排序
            List<Double[]> combinedVertices = combineVertices(originalVertices, externalVertices, finalBoundaryPoints);
            
            // 8. 检测并修复交叉
            List<Double[]> fixedVertices = fixCrossings(combinedVertices);
            
            // 9. 重新计算二级顶点
            List<Double[]> secondVertices = calculateSecondVertices(fixedVertices);
            
            // 10. 更新高度信息
            AreaData.AltitudeData updatedAltitude = updateAltitudeData(newAreaHeightRange);
            
            // 11. 创建扩展后的域名
            AreaData expandedArea = createExpandedArea(fixedVertices, secondVertices, updatedAltitude);

            // 12. 获取当前维度信息
            if (selectedDimensionId == null) {
                sendMessage(I18nManager.translate("dividearea.error.dimension"), Formatting.RED);
                return;
            }

            // 13. 发送给服务端
            ExpandAreaClientNetworking.sendExpandedAreaToServer(expandedArea, selectedDimensionId);

            sendMessage(I18nManager.translate("expandarea.message.area.finish.expand"), Formatting.GREEN);
            
        } catch (Exception e) {
            sendMessage(I18nManager.translate("expandarea.error.area.expand_2") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        } finally {
            // 重置状态
            reset();
        }
    }
    
    /**
     * 按照提示词验证新添加区域的高度
     * 若新添加的区域的最高高度比原域名的最高高度小最低高度比原域名的最低高度高则不该变域名的高度
     * 反之则将报错
     */
    private boolean validateHeightsAccordingToPrompt() {
        if (client.player == null || selectedArea.getAltitude() == null) {
            return true; // 如果没有高度限制则通过
        }
        
        // 计算新添加区域的高度范围
        double[] newAreaHeightRange = calculateNewAreaHeightRange();
        double newMinHeight = newAreaHeightRange[0];
        double newMaxHeight = newAreaHeightRange[1];
        
        AreaData.AltitudeData altitude = selectedArea.getAltitude();
        if (altitude != null && altitude.getMax() != null && altitude.getMin() != null) {
            double originalMaxHeight = altitude.getMax();
            double originalMinHeight = altitude.getMin();
            
            // 按照提示词逻辑：新区域最高高度 < 原域名最高高度 且 新区域最低高度 > 原域名最低高度
            if (newMaxHeight < originalMaxHeight && newMinHeight > originalMinHeight) {
                // 不改变域名的高度
                sendMessage(I18nManager.translate("expandarea.message.area.altitude"), Formatting.GREEN);
                return true;
            } else {
                // 反之则将报错
                sendMessage(I18nManager.translate("expandarea.error.area.altitude.expand"), Formatting.RED);
                sendMessage(I18nManager.translate("expandarea.error.area.altitude") + originalMinHeight + " ~ " + originalMaxHeight, Formatting.RED);
                sendMessage(I18nManager.translate("expandarea.error.altitude") + newMinHeight + " ~ " + newMaxHeight, Formatting.RED);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 计算新添加区域的高度范围
     */
    private double[] calculateNewAreaHeightRange() {
        if (client.player == null) {
            return new double[]{64.0, 64.0}; // 默认高度
        }
        
        double playerY = client.player.getY();
        // 给新区域一个合理的高度范围（上下各10格）
        return new double[]{playerY - 10.0, playerY + 10.0};
    }
    
    /**
     * 提取原域名的顶点坐标
     */
    private List<Double[]> extractOriginalVertices() {
        List<Double[]> vertices = new ArrayList<>();
        List<AreaData.Vertex> verticesList = selectedArea.getVertices();
        
        // 检查顶点列表是否为null
        if (verticesList == null) {
            System.err.println("警告: 原域名的顶点列表为null，无法进行扩展操作");
            return vertices; // 返回空列表
        }
        
        for (AreaData.Vertex vertex : verticesList) {
            vertices.add(new Double[]{vertex.getX(), vertex.getZ()});
        }
        
        return vertices;
    }
    
    /**
     * 过滤外部顶点 - 删除在原域名内部的顶点
     */
    private List<Double[]> filterExternalVertices(List<Double[]> originalVertices) {
        List<Double[]> externalVertices = new ArrayList<>();
        
        for (Double[] vertex : newVertices) {
            if (!isPointInPolygon(vertex, originalVertices)) {
                externalVertices.add(vertex);
            }
        }
        
        return externalVertices;
    }
    
    /**
     * 计算边界点 - 按照提示词的复杂算法
     */
    private List<Double[]> calculateBoundaryPoints(List<Double[]> originalVertices, List<Double[]> externalVertices) {
        List<Double[]> boundaryPoints = new ArrayList<>();
        
        // 计算新添加顶点与原域名边界的交叉点
        for (int i = 0; i < externalVertices.size(); i++) {
            int nextIndex = (i + 1) % externalVertices.size();
            Double[] currentVertex = externalVertices.get(i);
            Double[] nextVertex = externalVertices.get(nextIndex);
            
            // 计算这条线段与原域名边界的交叉点
            List<Double[]> intersections = findLinePolygonIntersections(currentVertex, nextVertex, originalVertices);
            boundaryPoints.addAll(intersections);
        }
        
        return boundaryPoints;
    }
    
    /**
     * 计算临近点 - 按照提示词：只计算初始点和末尾点的临近点
     * 一个点只能对应一个临近点
     * 初始点和末尾点是最开始玩家的起点和终点（可能因为在域名内而被删除）
     */
    private List<Double[]> calculateAdjacentPoints(List<Double[]> originalVertices, List<Double[]> externalVertices) {
        List<Double[]> adjacentPoints = new ArrayList<>();
        
        if (externalVertices == null || externalVertices.isEmpty()) {
            return adjacentPoints;
        }
        
        // 按照提示词：只计算初始点和末尾点的临近点
        // 初始点：外部顶点的第一个点
        Double[] initialPoint = externalVertices.get(0);
        // 末尾点：外部顶点的最后一个点
        Double[] endPoint = externalVertices.get(externalVertices.size() - 1);
        
        // 为初始点找到最近的临近点（原域名边界上的点）
        Double[] initialAdjacent = findNearestPointOnPolygon(initialPoint, originalVertices);
        if (initialAdjacent != null) {
            adjacentPoints.add(initialAdjacent);
        }
        
        // 为末尾点找到最近的临近点（原域名边界上的点）
        // 如果初始点和末尾点是同一个点，只添加一次
        if (initialPoint != endPoint && 
            (initialPoint[0] != endPoint[0] || initialPoint[1] != endPoint[1])) {
            Double[] endAdjacent = findNearestPointOnPolygon(endPoint, originalVertices);
            if (endAdjacent != null) {
                adjacentPoints.add(endAdjacent);
            }
        }
        
        return adjacentPoints;
    }
    
    /**
     * 计算最终边界点 - 处理一个点对应多个边界点的情况
     * 所有返回的边界点坐标都会被取整为整数
     */
    private List<Double[]> calculateFinalBoundaryPoints(List<Double[]> adjacentPoints, List<Double[]> originalVertices) {
        List<Double[]> finalBoundaryPoints = new ArrayList<>();
        
        // 对于每个临近点，找到对应的边界点
        for (Double[] adjacentPoint : adjacentPoints) {
            // 如果这个点有多个边界点，取中位值
            List<Double[]> boundaryPointsForAdjacent = findBoundaryPointsForAdjacent(adjacentPoint, originalVertices);
            
            if (boundaryPointsForAdjacent.size() == 1) {
                // 单个边界点也要确保取整（getClosestPointOnSegment已经取整了）
                finalBoundaryPoints.add(boundaryPointsForAdjacent.get(0));
            } else if (boundaryPointsForAdjacent.size() > 1) {
                // 计算中位值（已取整）
                Double[] medianPoint = calculateMedianPoint(boundaryPointsForAdjacent);
                // 计算与边界的交点（已取整）
                Double[] intersectionPoint = findIntersectionWithBoundary(adjacentPoint, medianPoint, originalVertices);
                if (intersectionPoint != null) {
                    finalBoundaryPoints.add(intersectionPoint);
                }
            }
        }
        
        return finalBoundaryPoints;
    }
    
    /**
     * 合并顶点并排序
     * 关键：必须保留所有原顶点，只删除两个边界点之间的原顶点
     */
    private List<Double[]> combineVertices(List<Double[]> originalVertices, List<Double[]> externalVertices, List<Double[]> boundaryPoints) {
        // 检查输入参数是否为null
        if (originalVertices == null || originalVertices.isEmpty()) {
            // 如果没有原顶点，只返回新顶点
            List<Double[]> result = new ArrayList<>();
            if (externalVertices != null) {
                result.addAll(externalVertices);
            }
            if (boundaryPoints != null) {
                result.addAll(boundaryPoints);
            }
            return result;
        }
        
        // 按照提示词：原顶点 边界点 新顶点 边界点 原顶点 的顺序排列
        // 关键：sortVerticesForExpansion 必须保留所有原顶点（除了被删除的两个边界点之间的点）
        return sortVerticesForExpansion(originalVertices, externalVertices, boundaryPoints);
    }
    
    /**
     * 检测并修复交叉
     * 注意：新顶点与原顶点的交叉检测已在sortVerticesForExpansion中完成
     * 这里只检测整体顶点列表的自相交（作为备用检查）
     */
    private List<Double[]> fixCrossings(List<Double[]> vertices) {
        // 由于新顶点与原顶点的交叉检测已在sortVerticesForExpansion中完成
        // 这里只返回原列表（如果需要，可以添加额外的自相交检测）
        return new ArrayList<>(vertices);
    }
    
    /**
     * 重新计算二级顶点（AABB边界框）
     * 坐标会被取整为整数
     */
    private List<Double[]> calculateSecondVertices(List<Double[]> vertices) {
        if (vertices.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 计算边界框
        double minX = vertices.get(0)[0];
        double maxX = vertices.get(0)[0];
        double minZ = vertices.get(0)[1];
        double maxZ = vertices.get(0)[1];
        
        for (Double[] vertex : vertices) {
            minX = Math.min(minX, vertex[0]);
            maxX = Math.max(maxX, vertex[0]);
            minZ = Math.min(minZ, vertex[1]);
            maxZ = Math.max(maxZ, vertex[1]);
        }
        
        // 创建AABB四个角点，坐标取整为整数
        List<Double[]> secondVertices = new ArrayList<>();
        secondVertices.add(new Double[]{(double) Math.round(minX), (double) Math.round(minZ)}); // 左下
        secondVertices.add(new Double[]{(double) Math.round(maxX), (double) Math.round(minZ)}); // 右下
        secondVertices.add(new Double[]{(double) Math.round(maxX), (double) Math.round(maxZ)}); // 右上
        secondVertices.add(new Double[]{(double) Math.round(minX), (double) Math.round(maxZ)}); // 左上
        
        return secondVertices;
    }
    
    /**
     * 更新高度信息
     * 合并新添加区域的高度与原区域的高度：
     * - 最高高度：取两个区域中更高的那一个（null表示无限制，比任何数值都大）
     * - 最低高度：取两个区域中更低的那一个（null表示无限制，比任何数值都小）
     */
    private AreaData.AltitudeData updateAltitudeData(double[] newAreaHeightRange) {
        AreaData.AltitudeData originalAltitude = selectedArea.getAltitude();
        
        if (originalAltitude == null) {
            // 如果原域名没有高度信息，使用新区域的高度
            return new AreaData.AltitudeData(newAreaHeightRange[1], newAreaHeightRange[0]);
        }
        
        // 检查原始高度数据是否完整（不为null）
        Double originalMin = originalAltitude.getMin();
        Double originalMax = originalAltitude.getMax();
        
        // 处理null高度的情况（null表示无限制）
        // null比任何数值都大（最高高度）或小（最低高度）
        Double mergedMin;
        Double mergedMax;
        
        // 最低高度：取两个区域中更低的那一个
        // 如果任一区域为null（无限制），合并后也应该是null（无限制）
        if (originalMin == null) {
            // 原域名最低高度无限制，合并后也应该无限制
            mergedMin = null;
        } else {
            // 原域名有具体的最低高度，与新区域比较
            // 由于新区域总是有具体数值（从calculateNewAreaHeightRange获取），直接比较
            mergedMin = Math.min(originalMin, newAreaHeightRange[0]);
        }
        
        // 最高高度：取两个区域中更高的那一个
        // 如果任一区域为null（无限制），合并后也应该是null（无限制）
        if (originalMax == null) {
            // 原域名最高高度无限制，合并后也应该无限制
            mergedMax = null;
        } else {
            // 原域名有具体的最高高度，与新区域比较
            // 由于新区域总是有具体数值（从calculateNewAreaHeightRange获取），直接比较
            mergedMax = Math.max(originalMax, newAreaHeightRange[1]);
        }
        
        return new AreaData.AltitudeData(mergedMax, mergedMin);
    }
    
    /**
     * 创建扩展后的域名
     */
    private AreaData createExpandedArea(List<Double[]> vertices, List<Double[]> secondVertices, AreaData.AltitudeData altitude) {
        return new AreaData(
            selectedArea.getName(),
            convertToVertexList(vertices),
            convertToVertexList(secondVertices),
            altitude,
            selectedArea.getLevel(),
            selectedArea.getBaseName(),
            selectedArea.getSignature(),
            selectedArea.getColor(),
            selectedArea.getSurfacename()
        );
    }
    
    /**
     * 重置管理器状态
     */
    public void reset() {
        this.selectedAreaName = null;
        this.selectedDimensionId = null;
        this.selectedArea = null;
        this.newVertices.clear();
        this.isRecording = false;
        this.isActive = false;
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
    }
    
    /**
     * 继续记录更多顶点
     */
    public void continueRecording() {
        if (!isRecording || client.player == null) {
            return;
        }
        
        sendMessage(I18nManager.translate("expandarea.message.vertex.record.continue") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("easyadd.message.record"), Formatting.GREEN);
    }
    
    /**
     * 完成记录并保存域名扩展
     */
    public void finishAndSave() {
        if (!isRecording || client.player == null) {
            return;
        }
        
        if (newVertices.size() < 3) {
            sendMessage(I18nManager.translate("expandarea.error.vertex.record"), Formatting.RED);
            return;
        }
        
        // 停止记录模式
        this.isRecording = false;
        
        // 使用现有的处理逻辑
        try {
            processAreaExpansion();
        } catch (Exception e) {
            sendMessage(I18nManager.translate("expandarea.error.area.expand_3") + e.getMessage(), Formatting.RED);
            e.printStackTrace();
        }
    }
    
    /**
     * 检查是否处于活动状态
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 检查是否处于记录状态
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * 停止扩展流程
     */
    public void stopExpand() {
        isActive = false;
        isRecording = false;
        selectedArea = null;
        selectedAreaName = null;
        newVertices.clear();
        areahint.boundviz.BoundVizManager.getInstance().clearTemporaryVertices();
    }
    
    /**
     * 根据名称查找域名
     */
    private AreaData findAreaByName(String name) {
        List<AreaData> areas = loadAllAreas();
        for (AreaData area : areas) {
            if (area.getName().equals(name)) {
                return area;
            }
        }
        return null;
    }
    
    /**
     * 加载当前维度的所有域名
     */
    private List<AreaData> loadAllAreas() {
        List<AreaData> areas = new ArrayList<>();
        try {
            // 获取当前维度
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                String currentDimension = client.world.getRegistryKey().getValue().toString();
                String fileName = getFileNameForCurrentDimension(currentDimension);
                
                if (fileName != null) {
                    Path areaPath = areahint.world.ClientWorldFolderManager.getWorldDimensionFile(fileName);
                    System.out.println("DEBUG: 尝试加载域名文件: " + areaPath);
                    
                    if (areaPath.toFile().exists()) {
                        areas = FileManager.readAreaData(areaPath);
                        System.out.println("DEBUG: 从 " + fileName + " 加载了 " + areas.size() + " 个域名");
                    } else {
                        System.out.println("DEBUG: 文件不存在: " + fileName);
                    }
                } else {
                    System.out.println("DEBUG: 无法确定当前维度的文件名");
                }
            } else {
                System.out.println("DEBUG: 无法获取当前世界信息");
            }
        } catch (Exception e) {
            System.out.println("DEBUG: 加载域名文件失败: " + e.getMessage());
            e.printStackTrace();
        }
        return areas;
    }
    
    /**
     * 根据维度ID获取对应的文件名
     */
    private String getFileNameForCurrentDimension(String dimensionId) {
        if (dimensionId == null) return null;
        
        if (dimensionId.contains("overworld")) {
            return areahint.Areashint.OVERWORLD_FILE;
        } else if (dimensionId.contains("nether")) {
            return areahint.Areashint.NETHER_FILE;
        } else if (dimensionId.contains("end")) {
            return areahint.Areashint.END_FILE;
        }
        return null;
    }
    
    /**
     * 发送消息给玩家
     */
    private void sendMessage(String message, Formatting formatting) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message).formatted(formatting), false);
        }
    }
    
    // ==================== 辅助方法实现 ====================
    
    /**
     * 判断点是否在多边形内部（射线法）
     */
    private boolean isPointInPolygon(Double[] point, List<Double[]> polygon) {
        if (polygon.size() < 3) return false;
        
        double x = point[0];
        double y = point[1];
        boolean inside = false;
        
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            double xi = polygon.get(i)[0];
            double yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0];
            double yj = polygon.get(j)[1];
            
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    /**
     * 计算线段与多边形的交点
     */
    private List<Double[]> findLinePolygonIntersections(Double[] lineStart, Double[] lineEnd, List<Double[]> polygon) {
        List<Double[]> intersections = new ArrayList<>();
        
        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            Double[] polyStart = polygon.get(i);
            Double[] polyEnd = polygon.get(j);
            
            Double[] intersection = getLineIntersection(lineStart, lineEnd, polyStart, polyEnd);
            if (intersection != null) {
                intersections.add(intersection);
            }
        }
        
        return intersections;
    }
    
    /**
     * 计算两条线段的交点
     * 返回的坐标会被取整为整数
     */
    private Double[] getLineIntersection(Double[] line1Start, Double[] line1End, Double[] line2Start, Double[] line2End) {
        double x1 = line1Start[0], y1 = line1Start[1];
        double x2 = line1End[0], y2 = line1End[1];
        double x3 = line2Start[0], y3 = line2Start[1];
        double x4 = line2End[0], y4 = line2End[1];
        
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) {
            return null; // 平行线
        }
        
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            double x = x1 + t * (x2 - x1);
            double z = y1 + t * (y2 - y1);
            // 取整为整数
            return new Double[]{(double) Math.round(x), (double) Math.round(z)};
        }
        
        return null;
    }
    
    /**
     * 找到距离指定点最近的多边形边界点
     */
    private Double[] findNearestPointOnPolygon(Double[] point, List<Double[]> polygon) {
        if (polygon.size() < 2) return null;
        
        Double[] nearestPoint = null;
        double minDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < polygon.size(); i++) {
            int j = (i + 1) % polygon.size();
            Double[] p1 = polygon.get(i);
            Double[] p2 = polygon.get(j);
            
            // 计算点到线段的最近点
            Double[] closest = getClosestPointOnSegment(point, p1, p2);
            double distance = calculateDistance(point, closest);
            
            if (distance < minDistance) {
                minDistance = distance;
                nearestPoint = closest;
            }
        }
        
        return nearestPoint;
    }
    
    /**
     * 计算点到线段的最近点
     * 返回的坐标会被取整为整数
     */
    private Double[] getClosestPointOnSegment(Double[] point, Double[] segStart, Double[] segEnd) {
        double px = point[0], py = point[1];
        double ax = segStart[0], ay = segStart[1];
        double bx = segEnd[0], by = segEnd[1];
        
        double dx = bx - ax;
        double dy = by - ay;
        
        if (dx == 0 && dy == 0) {
            // 取整为整数
            return new Double[]{(double) Math.round(ax), (double) Math.round(ay)};
        }
        
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        
        double x = ax + t * dx;
        double z = ay + t * dy;
        // 取整为整数
        return new Double[]{(double) Math.round(x), (double) Math.round(z)};
    }
    
    /**
     * 计算两点间距离
     */
    private double calculateDistance(Double[] p1, Double[] p2) {
        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * 为临近点找到对应的边界点
     */
    private List<Double[]> findBoundaryPointsForAdjacent(Double[] adjacentPoint, List<Double[]> originalVertices) {
        List<Double[]> boundaryPoints = new ArrayList<>();
        
        // 简化实现：找到距离临近点最近的几个边界点
        for (int i = 0; i < originalVertices.size(); i++) {
            int j = (i + 1) % originalVertices.size();
            Double[] p1 = originalVertices.get(i);
            Double[] p2 = originalVertices.get(j);
            
            Double[] closest = getClosestPointOnSegment(adjacentPoint, p1, p2);
            if (calculateDistance(adjacentPoint, closest) < 50.0) { // 50格内的边界点
                boundaryPoints.add(closest);
            }
        }
        
        return boundaryPoints;
    }
    
    /**
     * 计算多个点的中位值
     * 返回的坐标会被取整为整数
     */
    private Double[] calculateMedianPoint(List<Double[]> points) {
        if (points.isEmpty()) return null;
        if (points.size() == 1) {
            // 单个点也要取整
            Double[] point = points.get(0);
            return new Double[]{(double) Math.round(point[0]), (double) Math.round(point[1])};
        }
        
        double sumX = 0, sumY = 0;
        for (Double[] point : points) {
            sumX += point[0];
            sumY += point[1];
        }
        
        double x = sumX / points.size();
        double z = sumY / points.size();
        // 取整为整数
        return new Double[]{(double) Math.round(x), (double) Math.round(z)};
    }
    
    /**
     * 计算与边界的交点
     * 返回的坐标会被取整为整数
     */
    private Double[] findIntersectionWithBoundary(Double[] point1, Double[] point2, List<Double[]> originalVertices) {
        // 简化实现：返回两点连线的中点
        double x = (point1[0] + point2[0]) / 2;
        double z = (point1[1] + point2[1]) / 2;
        // 取整为整数
        return new Double[]{(double) Math.round(x), (double) Math.round(z)};
    }
    
    /**
     * 移除重复点
     */
    private List<Double[]> removeDuplicatePoints(List<Double[]> vertices) {
        List<Double[]> unique = new ArrayList<>();
        final double EPSILON = 0.001;
        
        for (Double[] vertex : vertices) {
            boolean isDuplicate = false;
            for (Double[] existing : unique) {
                if (calculateDistance(vertex, existing) < EPSILON) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                unique.add(vertex);
            }
        }
        
        return unique;
    }
    
    /**
     * 按照提示词排序顶点：原顶点 边界点 新顶点 边界点 原顶点
     * 正确实现：找到边界点应该插入的位置，然后按照顺序插入
     * 关键：必须保留所有原顶点，只删除两个边界点之间的原顶点
     */
    private List<Double[]> sortVerticesForExpansion(List<Double[]> originalVertices, List<Double[]> externalVertices, List<Double[]> boundaryPoints) {
        if (originalVertices == null || originalVertices.isEmpty()) {
            return externalVertices != null ? new ArrayList<>(externalVertices) : new ArrayList<>();
        }
        
        if (externalVertices == null || externalVertices.isEmpty()) {
            return new ArrayList<>(originalVertices);
        }
        
        if (boundaryPoints == null || boundaryPoints.isEmpty()) {
            // 如果没有边界点，保留所有原顶点，然后添加新顶点，最后排序防止交叉
            List<Double[]> allVertices = new ArrayList<>(originalVertices);
            allVertices.addAll(externalVertices);
            return sortVerticesWithoutCrossing(allVertices);
        }
        
        // 找到两个主要的边界点（对应新顶点的起点和终点）
        Double[] startBoundaryPoint = null;
        Double[] endBoundaryPoint = null;
        
        if (boundaryPoints.size() >= 2) {
            // 找到距离新顶点起点和终点最近的边界点
            Double[] newStart = externalVertices.get(0);
            Double[] newEnd = externalVertices.get(externalVertices.size() - 1);
            
            double minDistToStart = Double.MAX_VALUE;
            double minDistToEnd = Double.MAX_VALUE;
            
            for (Double[] bp : boundaryPoints) {
                double distToStart = calculateDistance(newStart, bp);
                double distToEnd = calculateDistance(newEnd, bp);
                
                if (distToStart < minDistToStart) {
                    minDistToStart = distToStart;
                    startBoundaryPoint = bp;
                }
                if (distToEnd < minDistToEnd) {
                    minDistToEnd = distToEnd;
                    endBoundaryPoint = bp;
                }
            }
        } else if (boundaryPoints.size() == 1) {
            // 只有一个边界点，使用同一个点作为起点和终点
            startBoundaryPoint = boundaryPoints.get(0);
            endBoundaryPoint = boundaryPoints.get(0);
        }
        
        // 如果找不到边界点，保留所有原顶点，然后添加新顶点
        if (startBoundaryPoint == null || endBoundaryPoint == null) {
            List<Double[]> allVertices = new ArrayList<>(originalVertices);
            allVertices.addAll(externalVertices);
            if (boundaryPoints != null) {
                allVertices.addAll(boundaryPoints);
            }
            // 对所有顶点重新排序，防止交叉
            return sortVerticesWithoutCrossing(allVertices);
        }
        
        // 找到边界点应该插入的位置（在原域名的哪两个顶点之间）
        int startBoundaryIndex = findBoundaryPointInsertIndex(startBoundaryPoint, originalVertices);
        int endBoundaryIndex = findBoundaryPointInsertIndex(endBoundaryPoint, originalVertices);
        
        // 不进行简单的索引交换，使用模运算计算两段弧上的原顶点数量并删除较短的一段
        int n = originalVertices.size();

        // 判断新顶点的插入方向（正向或反向）
        List<Double[]> orderedNewVertices = determineNewVerticesOrder(externalVertices, startBoundaryPoint, endBoundaryPoint);

        // 如果新/原交叉，反转新顶点顺序
        if (hasCrossingBetweenNewAndOriginal(originalVertices, orderedNewVertices, startBoundaryPoint, endBoundaryPoint)) {
            List<Double[]> reversed = new ArrayList<>();
            for (int i = orderedNewVertices.size() - 1; i >= 0; i--) {
                reversed.add(orderedNewVertices.get(i));
            }
            orderedNewVertices = reversed;
        }

        // 计算沿正方向从 start 到 end 的要删除的原顶点数量（不包括 start 本身）
        int forwardDeleteCount = (endBoundaryIndex - startBoundaryIndex + n) % n;
        int backwardDeleteCount = n - forwardDeleteCount;

        // 标记要保留的原顶点
        boolean[] keep = new boolean[n];
        for (int i = 0; i < n; i++) keep[i] = true;

        if (n <= 2) {
            // 太少顶点，直接合并
            List<Double[]> fallback = new ArrayList<>(originalVertices);
            fallback.add(startBoundaryPoint);
            fallback.addAll(orderedNewVertices);
            fallback.add(endBoundaryPoint);
            return sortVerticesWithoutCrossing(fallback);
        }

        if (forwardDeleteCount <= backwardDeleteCount) {
            // 删除从 startBoundaryIndex+1 到 endBoundaryIndex（包含 endBoundaryIndex）
            int idx = (startBoundaryIndex + 1) % n;
            for (int k = 0; k < forwardDeleteCount; k++) {
                keep[idx] = false;
                idx = (idx + 1) % n;
            }
        } else {
            // 删除从 endBoundaryIndex+1 到 startBoundaryIndex（包含 startBoundaryIndex）
            int idx = (endBoundaryIndex + 1) % n;
            for (int k = 0; k < backwardDeleteCount; k++) {
                keep[idx] = false;
                idx = (idx + 1) % n;
            }
        }

        // 确保至少保留一个原顶点（防止误删全部）
        boolean anyKept = false;
        for (boolean b : keep) { if (b) { anyKept = true; break; } }
        if (!anyKept) {
            // 回退到保留所有原顶点的方案
            List<Double[]> fallbackVertices = new ArrayList<>(originalVertices);
            fallbackVertices.addAll(externalVertices);
            if (boundaryPoints != null) fallbackVertices.addAll(boundaryPoints);
            return sortVerticesWithoutCrossing(fallbackVertices);
        }

        // 找到插入点：通常在 startBoundaryIndex 之后插入，但如果 start 被删除，向前找到最近保留的顶点作为插入点
        int insertAfter = startBoundaryIndex;
        int safeCounter = 0;
        while (safeCounter < n && !keep[insertAfter]) {
            insertAfter = (insertAfter - 1 + n) % n;
            safeCounter++;
        }

        List<Double[]> finalVertices = new ArrayList<>();
        boolean inserted = false;

        // 以原始顺序遍历，保留未被删除的点，并在 insertAfter 后插入边界点与新顶点
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                finalVertices.add(originalVertices.get(i));
            }
            if (!inserted && i == insertAfter) {
                // 插入边界点与新顶点（保持顺序：startBoundary -> newVertices -> endBoundary）
                finalVertices.add(startBoundaryPoint);
                finalVertices.addAll(orderedNewVertices);
                finalVertices.add(endBoundaryPoint);
                inserted = true;
            }
        }

        // 如果尚未插入（极端情况），追加到末尾
        if (!inserted) {
            finalVertices.add(startBoundaryPoint);
            finalVertices.addAll(orderedNewVertices);
            finalVertices.add(endBoundaryPoint);
        }

        // 最后进行去重与防交叉排序
        return sortVerticesWithoutCrossing(finalVertices);
    }
    
    /**
     * 找到边界点应该插入的位置（在原域名的哪两个顶点之间）
     * 返回边界点所在边的起始顶点索引
     */
    private int findBoundaryPointInsertIndex(Double[] boundaryPoint, List<Double[]> originalVertices) {
        double minDistance = Double.MAX_VALUE;
        int edgeIndex = -1;
        
        for (int i = 0; i < originalVertices.size(); i++) {
            int j = (i + 1) % originalVertices.size();
            Double[] p1 = originalVertices.get(i);
            Double[] p2 = originalVertices.get(j);
            
            // 计算边界点到线段的距离
            Double[] closest = getClosestPointOnSegment(boundaryPoint, p1, p2);
            double distance = calculateDistance(boundaryPoint, closest);
            
            if (distance < minDistance) {
                minDistance = distance;
                edgeIndex = i;
            }
        }
        
        return edgeIndex >= 0 ? edgeIndex : 0;
    }
    
    /**
     * 判断新顶点的插入顺序（正向或反向）
     */
    private List<Double[]> determineNewVerticesOrder(List<Double[]> newVertices, Double[] boundaryStart, Double[] boundaryEnd) {
        if (newVertices == null || newVertices.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 检查第一个顶点离哪个边界点更近
        Double[] firstVertex = newVertices.get(0);
        double distToStart = calculateDistance(firstVertex, boundaryStart);
        double distToEnd = calculateDistance(firstVertex, boundaryEnd);
        
        // 如果第一个顶点离起始边界点更近，保持正向
        if (distToStart < distToEnd) {
            return new ArrayList<>(newVertices);
        } else {
            // 否则反向
            List<Double[]> reversed = new ArrayList<>();
            for (int i = newVertices.size() - 1; i >= 0; i--) {
                reversed.add(newVertices.get(i));
            }
            return reversed;
        }
    }
    
    /**
     * 按逆时针方向排序顶点
     */
    private List<Double[]> sortVerticesCounterClockwise(List<Double[]> vertices) {
        if (vertices.size() < 3) return vertices;
        
        // 计算重心
        double centerX = 0, centerZ = 0;
        for (Double[] vertex : vertices) {
            centerX += vertex[0];
            centerZ += vertex[1];
        }
        centerX /= vertices.size();
        centerZ /= vertices.size();
        
        final double finalCenterX = centerX;
        final double finalCenterZ = centerZ;
        
        // 按极角排序
        List<Double[]> sorted = new ArrayList<>(vertices);
        sorted.sort((v1, v2) -> {
            double angle1 = Math.atan2(v1[1] - finalCenterZ, v1[0] - finalCenterX);
            double angle2 = Math.atan2(v2[1] - finalCenterZ, v2[0] - finalCenterX);
            return Double.compare(angle1, angle2);
        });
        
        return sorted;
    }
    
    /**
     * 对所有顶点重新排序，防止线段交叉
     * 按照提示词：如果新添加入的区域的一级顶点与原有区域的顶点所连接成的线段有交叉则证明新添加入的区域的顶点的顺序反了
     * 需要重新排列新加入的区域的一级顶点，再进行插入
     */
    private List<Double[]> sortVerticesWithoutCrossing(List<Double[]> vertices) {
        if (vertices.size() < 3) return vertices;
        
        // 先按逆时针排序
        List<Double[]> sorted = sortVerticesCounterClockwise(vertices);
        
        // 检测并修复交叉
        int maxIterations = 20; // 最多迭代20次
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            boolean hasCrossing = false;
            int crossingI = -1, crossingJ = -1;
            
            // 检测所有线段对是否有交叉
            for (int i = 0; i < sorted.size(); i++) {
                int nextI = (i + 1) % sorted.size();
                Double[] p1 = sorted.get(i);
                Double[] p2 = sorted.get(nextI);
                
                for (int j = i + 2; j < sorted.size(); j++) {
                    int nextJ = (j + 1) % sorted.size();
                    // 跳过相邻边和闭合边
                    if (nextI == j || nextJ == i) continue;
                    if (i == 0 && nextJ == 0) continue;
                    if (nextI == 0 && j == sorted.size() - 1) continue;
                    
                    Double[] p3 = sorted.get(j);
                    Double[] p4 = sorted.get(nextJ);
                    
                    // 检查线段是否交叉（不包括端点）
                    if (doSegmentsCross(p1, p2, p3, p4)) {
                        hasCrossing = true;
                        crossingI = i;
                        crossingJ = j;
                        break;
                    }
                }
                if (hasCrossing) break;
            }
            
            // 如果没有交叉，排序完成
            if (!hasCrossing) {
                break;
            }
            
            // 修复交叉：反转交叉段之间的顶点顺序
            if (crossingI >= 0 && crossingJ >= 0) {
                // 反转从 crossingI+1 到 crossingJ 之间的顶点
                int start = (crossingI + 1) % sorted.size();
                int end = crossingJ;
                
                if (start < end) {
                    // 正常情况：反转 [start, end] 区间
                    reverseSegment(sorted, start, end);
                } else {
                    // 跨越边界：反转两段
                    // 反转 [start, size-1] 和 [0, end]
                    reverseSegment(sorted, start, sorted.size() - 1);
                    reverseSegment(sorted, 0, end);
                }
            }
        }
        
        // 如果仍然有交叉，使用基于凸包的排序算法作为最后手段
        if (detectCrossings(sorted)) {
            sorted = sortVerticesByConvexHull(vertices);
        }
        
        return sorted;
    }
    
    /**
     * 检查两条线段是否交叉（不包括端点）
     */
    private boolean doSegmentsCross(Double[] p1, Double[] p2, Double[] p3, Double[] p4) {
        // 使用现有的getLineIntersection方法检测交叉
        Double[] intersection = getLineIntersection(p1, p2, p3, p4);
        if (intersection == null) {
            return false;
        }
        
        // 检查交点是否在线段内部（不包括端点）
        double eps = 1e-6;
        boolean onSegment1 = isPointOnSegmentStrict(intersection, p1, p2, eps);
        boolean onSegment2 = isPointOnSegmentStrict(intersection, p3, p4, eps);
        
        return onSegment1 && onSegment2;
    }
    
    /**
     * 检查点是否在线段内部（不包括端点）
     */
    private boolean isPointOnSegmentStrict(Double[] point, Double[] segStart, Double[] segEnd, double epsilon) {
        // 检查点是否在线段所在的直线上
        double dx = segEnd[0] - segStart[0];
        double dy = segEnd[1] - segStart[1];
        double dx1 = point[0] - segStart[0];
        double dy1 = point[1] - segStart[1];
        
        // 叉积检查是否共线
        double cross = dx * dy1 - dy * dx1;
        if (Math.abs(cross) > epsilon) {
            return false; // 不共线
        }
        
        // 检查点是否在线段内部（不包括端点）
        double dot = dx1 * dx + dy1 * dy;
        double lenSq = dx * dx + dy * dy;
        
        if (lenSq < epsilon) {
            // 线段长度为0，检查点是否与起点重合
            return false; // 端点不算交叉
        }
        
        double t = dot / lenSq;
        // t必须在(0, 1)范围内，不包括端点
        return t > epsilon && t < 1.0 - epsilon;
    }
    
    
    /**
     * 反转列表中指定区间的元素
     */
    private void reverseSegment(List<Double[]> list, int start, int end) {
        while (start < end) {
            Double[] temp = list.get(start);
            list.set(start, list.get(end));
            list.set(end, temp);
            start++;
            end--;
        }
    }
    
    /**
     * 基于凸包的排序算法，确保没有交叉
     */
    private List<Double[]> sortVerticesByConvexHull(List<Double[]> vertices) {
        if (vertices.size() < 3) return vertices;
        
        // 找到最下方的点（如果y相同，取最左边的）
        int bottomIndex = 0;
        for (int i = 1; i < vertices.size(); i++) {
            Double[] current = vertices.get(i);
            Double[] bottom = vertices.get(bottomIndex);
            if (current[1] < bottom[1] || (current[1] == bottom[1] && current[0] < bottom[0])) {
                bottomIndex = i;
            }
        }
        
        // 以最下方的点为起点，按极角排序
        Double[] bottomPoint = vertices.get(bottomIndex);
        List<Double[]> sorted = new ArrayList<>(vertices);
        
        sorted.sort((v1, v2) -> {
            // 计算相对于bottomPoint的极角
            double angle1 = Math.atan2(v1[1] - bottomPoint[1], v1[0] - bottomPoint[0]);
            double angle2 = Math.atan2(v2[1] - bottomPoint[1], v2[0] - bottomPoint[0]);
            
            // 如果角度相同，按距离排序
            if (Math.abs(angle1 - angle2) < 1e-10) {
                double dist1 = calculateDistance(v1, bottomPoint);
                double dist2 = calculateDistance(v2, bottomPoint);
                return Double.compare(dist1, dist2);
            }
            
            return Double.compare(angle1, angle2);
        });
        
        return sorted;
    }
    
    /**
     * 检测交叉
     */
    private boolean detectCrossings(List<Double[]> vertices) {
        // 简化实现：检测基本的自相交
        for (int i = 0; i < vertices.size(); i++) {
            for (int j = i + 2; j < vertices.size(); j++) {
                if (i == 0 && j == vertices.size() - 1) continue; // 跳过相邻边
                
                int nextI = (i + 1) % vertices.size();
                int nextJ = (j + 1) % vertices.size();
                
                Double[] intersection = getLineIntersection(
                    vertices.get(i), vertices.get(nextI),
                    vertices.get(j), vertices.get(nextJ)
                );
                
                if (intersection != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 检测新顶点与原顶点之间的交叉
     * 如果新添加入的区域的一级顶点与原有区域的顶点所连接成的线段有交叉
     * 则证明新添加入的区域的顶点的顺序反了
     */
    private boolean hasCrossingBetweenNewAndOriginal(List<Double[]> originalVertices, List<Double[]> newVertices, 
                                                      Double[] startBoundary, Double[] endBoundary) {
        if (newVertices == null || newVertices.size() < 2) {
            return false;
        }
        
        // 检测新顶点之间的线段与原顶点之间的线段是否有交叉
        for (int i = 0; i < newVertices.size(); i++) {
            int nextNewIndex = (i + 1) % newVertices.size();
            Double[] newStart = newVertices.get(i);
            Double[] newEnd = newVertices.get(nextNewIndex);
            
            // 检查这条新顶点线段是否与原顶点线段交叉
            for (int j = 0; j < originalVertices.size(); j++) {
                int nextOriginalIndex = (j + 1) % originalVertices.size();
                Double[] originalStart = originalVertices.get(j);
                Double[] originalEnd = originalVertices.get(nextOriginalIndex);
                
                // 跳过边界点所在的边（避免误判）
                if (isPointOnSegment(startBoundary, originalStart, originalEnd) || 
                    isPointOnSegment(endBoundary, originalStart, originalEnd)) {
                    continue;
                }
                
                // 检查线段是否交叉
                Double[] intersection = getLineIntersection(newStart, newEnd, originalStart, originalEnd);
                if (intersection != null) {
                    return true; // 发现交叉
                }
            }
        }
        
        return false;
    }
    
    /**
     * 判断点是否在线段上
     */
    private boolean isPointOnSegment(Double[] point, Double[] segStart, Double[] segEnd) {
        if (point == null || segStart == null || segEnd == null) {
            return false;
        }
        
        // 计算点到线段起点的向量和线段向量
        double dx1 = point[0] - segStart[0];
        double dy1 = point[1] - segStart[1];
        double dx2 = segEnd[0] - segStart[0];
        double dy2 = segEnd[1] - segStart[1];
        
        // 如果线段长度为0，检查点是否与起点重合
        if (Math.abs(dx2) < 1e-10 && Math.abs(dy2) < 1e-10) {
            return Math.abs(dx1) < 1e-10 && Math.abs(dy1) < 1e-10;
        }
        
        // 计算点积和叉积
        double dot = dx1 * dx2 + dy1 * dy2;
        double cross = dx1 * dy2 - dy1 * dx2;
        
        // 如果叉积不为0，点不在线段上
        if (Math.abs(cross) > 1e-10) {
            return false;
        }
        
        // 检查点是否在线段范围内
        double t = dot / (dx2 * dx2 + dy2 * dy2);
        return t >= 0 && t <= 1;
    }
    
    /**
     * 将Double[]列表转换为Vertex列表
     * 坐标会被取整为整数
     */
    private List<AreaData.Vertex> convertToVertexList(List<Double[]> coordinates) {
        List<AreaData.Vertex> vertices = new ArrayList<>();
        
        // 检查输入参数是否为null
        if (coordinates == null) {
            System.err.println("警告: convertToVertexList 接收到null坐标列表");
            return vertices; // 返回空列表
        }
        
        for (Double[] coord : coordinates) {
            if (coord != null && coord.length >= 2) {
                // 将坐标取整为整数
                int x = (int) Math.round(coord[0]);
                int z = (int) Math.round(coord[1]);
                vertices.add(new AreaData.Vertex(x, z));
            }
        }
        return vertices;
    }
    
    // Getter方法
    public AreaData getSelectedArea() { return selectedArea; }
    public List<Double[]> getNewVertices() { return newVertices; }
}
