package areahint.detection;

import areahint.AreashintClient;
import areahint.data.AreaData;

import java.util.List;

/**
 * 射线法实现类
 * 用于检测点是否在多边形内
 */
public class RayCasting {
    
    /**
     * 使用射线法检测点是否在多边形内部
     * 基本思想是从给定点向右（正X方向）发射一条水平射线，
     * 并计算该射线与多边形边的交点数量：
     * - 如果交点数量为奇数：点在多边形内部。
     * - 如果交点数量为偶数：点在多边形外部。
     *
     * @param x 点的X坐标
     * @param z 点的Z坐标
     * @param vertices 多边形的顶点列表
     * @return 如果点在多边形内部返回true，否则返回false
     */
    public static boolean isPointInPolygon(double x, double z, List<AreaData.Vertex> vertices) {
        if (vertices == null || vertices.size() < 3) {
            // 热路径日志加debug守卫：关闭debug时不再产生字符串格式化与varargs数组分配
            if (AreashintClient.LOGGER.isDebugEnabled()) {
                AreashintClient.LOGGER.debug("多边形顶点数量不足，无法进行射线检测");
            }
            return false;
        }
        
        int intersections = 0;
        int vertexCount = vertices.size();
        
        // 每次检测都会走到这里：debug关闭时整段日志与参数装箱全部跳过
        boolean debugLog = AreashintClient.LOGGER.isDebugEnabled();
        if (debugLog) {
            AreashintClient.LOGGER.debug("进行射线检测，点坐标({}, {})，多边形顶点数: {}", x, z, vertexCount);
        }
        
        for (int i = 0; i < vertexCount; i++) {
            // 获取当前边的两个顶点
            AreaData.Vertex current = vertices.get(i);
            AreaData.Vertex next = vertices.get((i + 1) % vertexCount);
            
            // 检查边是否与射线相交
            boolean intersect = isIntersect(x, z, current, next);
            if (intersect) {
                intersections++;
                if (debugLog) {
                    AreashintClient.LOGGER.debug("边 {} 与射线相交，当前交点数: {}", i, intersections);
                }
            }
        }
        
        // 奇数个交点表示点在多边形内部，偶数个交点表示点在多边形外部
        boolean isInside = (intersections % 2 == 1);
        if (debugLog) {
            AreashintClient.LOGGER.debug("射线检测结果: 交点数 {}，点{}多边形内部", intersections, isInside ? "在" : "不在");
        }
        return isInside;
    }
    
    /**
     * 检查从点(x,z)向右发射的水平射线是否与线段相交
     * 
     * @param x 点的X坐标
     * @param z 点的Z坐标
     * @param v1 线段的第一个顶点
     * @param v2 线段的第二个顶点
     * @return 如果射线与线段相交返回true，否则返回false
     */
    private static boolean isIntersect(double x, double z, AreaData.Vertex v1, AreaData.Vertex v2) {
        // 获取线段的两个端点坐标
        double x1 = v1.getX();
        double z1 = v1.getZ();
        double x2 = v2.getX();
        double z2 = v2.getZ();
        
        // 检查点是否在线段的两个端点的水平线上
        if ((z1 <= z && z2 > z) || (z2 <= z && z1 > z)) {
            // 计算交点的X坐标
            double xIntersect = x1 + (z - z1) * (x2 - x1) / (z2 - z1);
            
            // 如果交点在射线的右侧（点的X坐标的右侧），则射线与线段相交
            if (xIntersect > x) {
                // 相交日志同样加debug守卫，避免在多边形循环里分配参数数组
                if (AreashintClient.LOGGER.isDebugEnabled()) {
                    AreashintClient.LOGGER.debug("线段({}, {}) - ({}, {})与从点({}, {})发射的射线相交，交点X坐标: {}",
                            x1, z1, x2, z2, x, z, xIntersect);
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查点是否在轴对齐包围盒（AABB）内部
     * AABB由四个点定义，分别是左上角、右上角、右下角和左下角
     * 
     * @param x 点的X坐标
     * @param z 点的Z坐标
     * @param secondVertices AABB的四个顶点（左上、右上、右下、左下）
     * @return 如果点在AABB内部返回true，否则返回false
     */
    public static boolean isPointInAABB(double x, double z, List<AreaData.Vertex> secondVertices) {
        if (secondVertices == null || secondVertices.size() != 4) {
            // 热路径日志加debug守卫
            if (AreashintClient.LOGGER.isDebugEnabled()) {
                AreashintClient.LOGGER.debug("AABB顶点数量不正确，应为4个点");
            }
            return false;
        }
        
        // 计算AABB的边界
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        
        for (AreaData.Vertex vertex : secondVertices) {
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minZ = Math.min(minZ, vertex.getZ());
            maxZ = Math.max(maxZ, vertex.getZ());
        }
        
        // 检查点是否在AABB内部
        boolean isInside = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        if (AreashintClient.LOGGER.isDebugEnabled()) {
            AreashintClient.LOGGER.debug("AABB检测边界: [{}, {}] - [{}, {}]，点({}, {}){}AABB内部",
                    minX, minZ, maxX, maxZ, x, z, isInside ? "在" : "不在");
        }
        return isInside;
    }
}
