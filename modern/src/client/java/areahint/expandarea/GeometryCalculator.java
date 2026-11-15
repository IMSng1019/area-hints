package areahint.expandarea;

import areahint.data.AreaData;
import java.util.*;
import net.minecraft.client.Minecraft;

/**
 * 几何计算工具类
 * 处理域名扩展的复杂几何算法
 * 注意：此类的功能已迁移到ExpandAreaManager中，保留此类以保持向后兼容性
 */
@Deprecated
public class GeometryCalculator {
    
    private final AreaData originalArea;
    private final List<Double[]> newVertices;
    private final Minecraft client;
    
    public GeometryCalculator(AreaData originalArea, List<Double[]> newVertices) {
        this.originalArea = originalArea;
        this.newVertices = newVertices;
        this.client = Minecraft.getInstance();
    }
    
    /**
     * 执行域名扩展计算
     * @return 扩展后的域名数据
     * @deprecated 此方法已迁移到ExpandAreaManager中
     */
    @Deprecated
    public AreaData expandArea() {
        // 返回null，提示使用新的实现
        System.err.println("警告：GeometryCalculator.expandArea() 已弃用，请使用 ExpandAreaManager 中的新实现");
        return null;
    }
    
    /**
     * 从AreaData中提取顶点坐标
     */
    private List<Double[]> extractVerticesFromArea(AreaData area) {
        List<Double[]> vertices = new ArrayList<>();
        List<AreaData.Vertex> verticesList = area.getVertices();
        
        for (AreaData.Vertex vertex : verticesList) {
            double x = vertex.getX();
            double z = vertex.getZ();
            vertices.add(new Double[]{x, z});
        }
        
        return vertices;
    }
    
    /**
     * 计算边界点
     */
    private List<Double[]> calculateBoundaryPoints(List<Double[]> originalVertices) {
        List<Double[]> boundaryPoints = new ArrayList<>();
        
        for (Double[] newVertex : newVertices) {
            // 检查新顶点是否在原域名内部
            if (isPointInPolygon(newVertex, originalVertices)) {
                continue; // 跳过内部点
            }
            
            // 找到最近的边界点
            Double[] boundaryPoint = findNearestBoundaryPoint(newVertex, originalVertices);
            if (boundaryPoint != null) {
                boundaryPoints.add(boundaryPoint);
            }
        }
        
        return boundaryPoints;
    }
    
    /**
     * 过滤外部顶点
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
     * 合并并排序顶点
     */
    private List<Double[]> combineAndSortVertices(List<Double[]> originalVertices, 
                                                   List<Double[]> externalVertices, 
                                                   List<Double[]> boundaryPoints) {
        
        List<Double[]> allVertices = new ArrayList<>();
        allVertices.addAll(originalVertices);
        allVertices.addAll(externalVertices);
        allVertices.addAll(boundaryPoints);
        
        // 移除重复点
        allVertices = removeDuplicatePoints(allVertices);
        
        // 按逆时针方向排序
        return sortVerticesCounterClockwise(allVertices);
    }
    
    /**
     * 检测并修复线段交叉
     */
    private List<Double[]> fixCrossings(List<Double[]> vertices) {
        // 简化实现：检测基本的自相交并修复
        List<Double[]> fixedVertices = new ArrayList<>(vertices);
        
        // 检测连续三个点形成的角度，如果是凹角则可能需要调整
        for (int i = 1; i < fixedVertices.size() - 1; i++) {
            Double[] prev = fixedVertices.get(i - 1);
            Double[] curr = fixedVertices.get(i);
            Double[] next = fixedVertices.get(i + 1);
            
            // 计算叉积判断方向
            double crossProduct = (curr[0] - prev[0]) * (next[1] - curr[1]) - 
                                  (curr[1] - prev[1]) * (next[0] - curr[0]);
            
            // 如果方向错误，可能需要调整（简化处理）
            if (crossProduct > 0) {
                // 这里可以添加更复杂的修复逻辑
            }
        }
        
        return fixedVertices;
    }
    
    /**
     * 重新计算二级顶点（AABB边界框）
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
        
        // 创建AABB四个角点
        List<Double[]> secondVertices = new ArrayList<>();
        secondVertices.add(new Double[]{minX, minZ}); // 左下
        secondVertices.add(new Double[]{maxX, minZ}); // 右下
        secondVertices.add(new Double[]{maxX, maxZ}); // 右上
        secondVertices.add(new Double[]{minX, maxZ}); // 左上
        
        return secondVertices;
    }
    
    /**
     * 更新高度信息
     */
    private AreaData.AltitudeData updateAltitude() {
        AreaData.AltitudeData altitude = originalArea.getAltitude();
        if (altitude == null) {
            return new AreaData.AltitudeData(320.0, -64.0); // 默认高度范围
        }
        
        // 获取当前玩家高度
        double playerY = client.player != null ? client.player.getY() : 64.0;
        
        // 扩展高度范围以包含新区域
        Double newMin = altitude.getMin();
        Double newMax = altitude.getMax();
        
        if (newMin == null || playerY < newMin) {
            newMin = playerY - 10; // 给一些缓冲
        }
        if (newMax == null || playerY > newMax) {
            newMax = playerY + 10; // 给一些缓冲
        }
        
        return new AreaData.AltitudeData(newMax, newMin);
    }
    
    /**
     * 将Double[]列表转换为Vertex列表
     */
    private List<AreaData.Vertex> convertToVertexList(List<Double[]> coordinates) {
        List<AreaData.Vertex> vertices = new ArrayList<>();
        for (Double[] coord : coordinates) {
            vertices.add(new AreaData.Vertex(coord[0], coord[1]));
        }
        return vertices;
    }
    
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
     * 找到距离指定点最近的边界点
     */
    private Double[] findNearestBoundaryPoint(Double[] point, List<Double[]> polygon) {
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
     */
    private Double[] getClosestPointOnSegment(Double[] point, Double[] segStart, Double[] segEnd) {
        double px = point[0], py = point[1];
        double ax = segStart[0], ay = segStart[1];
        double bx = segEnd[0], by = segEnd[1];
        
        double dx = bx - ax;
        double dy = by - ay;
        
        if (dx == 0 && dy == 0) {
            return new Double[]{ax, ay};
        }
        
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        
        return new Double[]{ax + t * dx, ay + t * dy};
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
        vertices.sort((v1, v2) -> {
            double angle1 = Math.atan2(v1[1] - finalCenterZ, v1[0] - finalCenterX);
            double angle2 = Math.atan2(v2[1] - finalCenterZ, v2[0] - finalCenterX);
            return Double.compare(angle1, angle2);
        });
        
        return vertices;
    }
} 