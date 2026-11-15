package areahint.easyadd;

import areahint.data.AreaData;
import areahint.detection.RayCasting;
import areahint.debug.ClientDebugManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * EasyAdd几何计算工具
 * 负责坐标计算、验证和转换
 */
public class EasyAddGeometry {
    
    /**
     * 计算包围盒（AABB）作为二级顶点
     * @param recordedPoints 记录的坐标点
     * @return 包围盒的四个顶点
     */
    public static List<AreaData.Vertex> calculateBoundingBox(List<BlockPos> recordedPoints) {
        if (recordedPoints.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 找到最大最小坐标
        int minX = recordedPoints.get(0).getX();
        int maxX = recordedPoints.get(0).getX();
        int minZ = recordedPoints.get(0).getZ();
        int maxZ = recordedPoints.get(0).getZ();
        
        for (BlockPos pos : recordedPoints) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        
        // 构建包围盒的四个顶点（按逆时针顺序）
        List<AreaData.Vertex> boundingBox = new ArrayList<>();
        boundingBox.add(new AreaData.Vertex(minX, minZ)); // 左下
        boundingBox.add(new AreaData.Vertex(maxX, minZ)); // 右下
        boundingBox.add(new AreaData.Vertex(maxX, maxZ)); // 右上
        boundingBox.add(new AreaData.Vertex(minX, maxZ)); // 左上
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "计算包围盒: (" + minX + "," + minZ + ") 到 (" + maxX + "," + maxZ + ")");
        
        return boundingBox;
    }
    
    /**
     * 计算高度范围
     * @param recordedPoints 记录的坐标点
     * @return 高度数据对象
     */
    public static AreaData.AltitudeData calculateAltitudeRange(List<BlockPos> recordedPoints) {
        if (recordedPoints.isEmpty()) {
            return new AreaData.AltitudeData(null, null);
        }
        
        int minY = recordedPoints.get(0).getY();
        int maxY = recordedPoints.get(0).getY();
        
        for (BlockPos pos : recordedPoints) {
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }
        
        // 扩展高度范围，给一些缓冲空间
        minY -= 5;  // 向下扩展5格
        maxY += 10; // 向上扩展10格
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "计算高度范围: " + minY + " ~ " + maxY);
        
        return new AreaData.AltitudeData((double)maxY, (double)minY);
    }
    
    /**
     * 验证子域名是否完全包含在父域名内
     * @param childArea 子域名
     * @param parentArea 父域名
     * @return 是否有效包含
     */
    public static boolean validateAreaInParent(AreaData childArea, AreaData parentArea) {
        // 1. 验证高度范围
        if (!validateAltitudeInParent(childArea, parentArea)) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "高度验证失败: 子域名高度超出父域名范围");
            return false;
        }
        
        // 2. 验证所有一级顶点是否在父域名内
        if (!validateVerticesInParent(childArea, parentArea)) {
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "顶点验证失败: 子域名顶点超出父域名边界");
            return false;
        }
        
        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
            "域名验证成功: " + childArea.getName() + " 完全包含在 " + parentArea.getName() + " 内");
        
        return true;
    }
    
    /**
     * 验证高度范围是否在父域名内
     */
    private static boolean validateAltitudeInParent(AreaData childArea, AreaData parentArea) {
        AreaData.AltitudeData childAlt = childArea.getAltitude();
        AreaData.AltitudeData parentAlt = parentArea.getAltitude();
        
        // 如果父域名没有高度限制，则子域名总是有效
        if (parentAlt == null) {
            return true;
        }
        
        // 如果子域名没有高度限制，但父域名有，则无效
        if (childAlt == null) {
            return false;
        }
        
        // 检查子域名的高度范围是否在父域名内
        Double parentMin = parentAlt.getMin();
        Double parentMax = parentAlt.getMax();
        Double childMin = childAlt.getMin();
        Double childMax = childAlt.getMax();
        
        if (parentMin != null && childMin != null && childMin < parentMin) {
            return false;
        }
        
        if (parentMax != null && childMax != null && childMax > parentMax) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证所有顶点是否在父域名内
     */
    private static boolean validateVerticesInParent(AreaData childArea, AreaData parentArea) {
        List<AreaData.Vertex> childVertices = childArea.getVertices();
        List<AreaData.Vertex> parentVertices = parentArea.getVertices();
        
        if (childVertices == null || parentVertices == null) {
            return false;
        }
        
        // 检查每个子域名顶点是否在父域名内
        for (AreaData.Vertex childVertex : childVertices) {
            double x = childVertex.getX();
            double z = childVertex.getZ();
            
            // 使用RayCasting类的多边形包含检测方法
            if (!RayCasting.isPointInPolygon(x, z, parentVertices)) {
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                    "顶点 (" + x + "," + z + ") 不在父域名内");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 计算多边形的质心（中心点）
     * @param vertices 顶点列表
     * @return 质心坐标
     */
    public static BlockPos calculateCentroid(List<AreaData.Vertex> vertices) {
        if (vertices.isEmpty()) {
            return new BlockPos(0, 0, 0);
        }
        
        double sumX = 0;
        double sumZ = 0;
        
        for (AreaData.Vertex vertex : vertices) {
            sumX += vertex.getX();
            sumZ += vertex.getZ();
        }
        
        int centerX = (int) Math.round(sumX / vertices.size());
        int centerZ = (int) Math.round(sumZ / vertices.size());
        
        return new BlockPos(centerX, 0, centerZ);
    }
    
    /**
     * 计算多边形的面积
     * @param vertices 顶点列表
     * @return 面积（平方米）
     */
    public static double calculateArea(List<AreaData.Vertex> vertices) {
        if (vertices.size() < 3) {
            return 0;
        }
        
        double area = 0;
        int n = vertices.size();
        
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            AreaData.Vertex vi = vertices.get(i);
            AreaData.Vertex vj = vertices.get(j);
            
            area += vi.getX() * vj.getZ();
            area -= vj.getX() * vi.getZ();
        }
        
        return Math.abs(area) / 2.0;
    }
} 