package areahint.xaero.minimap;

import areahint.xaero.AreaOverlayFillResolver;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class AreaMinimapContext {
    final AreaOverlayFillResolver fillResolver = AreaOverlayFillResolver.getInstance();
    boolean active;
    String dimensionId;
    double renderX;
    double renderY;
    double renderZ;
    double backgroundCoordinateScale = 1.0D;
    List<OverlayArea> visibleAreas = List.of();
    FillPlan fillPlan = FillPlan.empty();
    XaeroMinimapBridge.TransformState transform = XaeroMinimapBridge.TransformState.EMPTY;

    // 以下几何缓冲只在渲染线程内使用，每轮收集前清空输出并把计数归零，提交后即可安全复用
    /** 当前域名的填充三角形输出，元素是复用的 6 分量坐标数组。 */
    final List<float[]> fillTriangles = new ArrayList<>();
    /** 当前域名的边界线段输出，元素是复用的 4 分量坐标数组。 */
    final List<float[]> boundaryLines = new ArrayList<>();
    /** 三角形裁剪时交替承载输入和输出的两个多边形缓冲。 */
    final List<float[]> clipPolygonA = new ArrayList<>(8);
    final List<float[]> clipPolygonB = new ArrayList<>(8);
    /** 当前三角形复用的三个屏幕坐标顶点。 */
    final float[] fillFirst = new float[2];
    final float[] fillSecond = new float[2];
    final float[] fillThird = new float[2];

    private float[][] trianglePool = new float[0][];
    private float[][] linePool = new float[0][];
    private float[][] vertexPool = new float[0][];
    private float[][] clipPointPool = new float[0][];
    private int triangleCount;
    private int lineCount;
    private int clipPointCount;

    /** 开始收集某个域名的填充三角形，只清空输出，底层数组继续复用。 */
    void resetFillScratch() {
        fillTriangles.clear();
        triangleCount = 0;
    }

    /** 开始收集某个域名的边界线段，只清空输出，底层数组继续复用。 */
    void resetBoundaryScratch() {
        boundaryLines.clear();
        lineCount = 0;
    }

    /** 开始裁剪一个三角形，裁剪过程中新建的交点从池首重新取用。 */
    void resetClipPoints() {
        clipPointCount = 0;
    }

    /** 取出一个可复用的填充三角形数组（长度 6）。 */
    float[] nextFillTriangle() {
        trianglePool = growPool(trianglePool, triangleCount);
        float[] slot = trianglePool[triangleCount];
        if (slot == null) {
            slot = new float[6];
            trianglePool[triangleCount] = slot;
        }
        triangleCount++;
        return slot;
    }

    /** 取出一个可复用的边界线段数组（长度 4）。 */
    float[] nextBoundaryLine() {
        linePool = growPool(linePool, lineCount);
        float[] slot = linePool[lineCount];
        if (slot == null) {
            slot = new float[4];
            linePool[lineCount] = slot;
        }
        lineCount++;
        return slot;
    }

    /** 取出第 index 个可复用的边界顶点数组（长度 2），同一个顶点只做一次屏幕变换。 */
    float[] boundaryVertex(int index) {
        vertexPool = growPool(vertexPool, index);
        float[] slot = vertexPool[index];
        if (slot == null) {
            slot = new float[2];
            vertexPool[index] = slot;
        }
        return slot;
    }

    /** 取出一个可复用的裁剪交点数组（长度 2），同一个三角形内不会重复发号。 */
    float[] nextClipPoint() {
        clipPointPool = growPool(clipPointPool, clipPointCount);
        float[] slot = clipPointPool[clipPointCount];
        if (slot == null) {
            slot = new float[2];
            clipPointPool[clipPointCount] = slot;
        }
        clipPointCount++;
        return slot;
    }

    /** 数组池按需扩容，返回的池保证可以访问到 index。 */
    private static float[][] growPool(float[][] pool, int index) {
        if (index < pool.length) {
            return pool;
        }
        return Arrays.copyOf(pool, Math.max(Math.max(16, pool.length * 2), index + 1));
    }
}
