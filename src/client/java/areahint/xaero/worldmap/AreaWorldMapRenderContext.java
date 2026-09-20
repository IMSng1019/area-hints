package areahint.xaero.worldmap;

import areahint.xaero.AreaOverlayFillResolver;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class AreaWorldMapRenderContext {
    final AreaOverlayFillResolver fillResolver = AreaOverlayFillResolver.getInstance();
    String dimensionId;
    List<OverlayArea> areas = List.of();
    FillPlan fillPlan = FillPlan.empty();
    double hoverX;
    double hoverZ;
    double mapScale = 1.0D;
    double coordinateScale = 1.0D;

    // 以下几何缓冲只在渲染线程内使用，每个域名收集前清空输出并归零计数，提交后即可安全复用
    /** 当前域名的填充三角形输出，元素是复用的 6 分量坐标数组。 */
    final List<float[]> triangles = new ArrayList<>();
    /** 当前域名的边界线段输出，元素是复用的 4 分量坐标数组。 */
    final List<float[]> lines = new ArrayList<>();

    private float[][] trianglePool = new float[0][];
    private float[][] linePool = new float[0][];
    private int triangleCount;
    private int lineCount;

    /** 开始收集某个域名的几何，只清空输出，底层数组继续复用。 */
    void resetScratch() {
        triangles.clear();
        triangleCount = 0;
        lines.clear();
        lineCount = 0;
    }

    /** 取出一个可复用的填充三角形数组（长度 6）。 */
    float[] nextTriangle() {
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
    float[] nextLine() {
        linePool = growPool(linePool, lineCount);
        float[] slot = linePool[lineCount];
        if (slot == null) {
            slot = new float[4];
            linePool[lineCount] = slot;
        }
        lineCount++;
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
