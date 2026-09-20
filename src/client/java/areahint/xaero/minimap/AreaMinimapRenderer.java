package areahint.xaero.minimap;

import areahint.config.ClientConfig;
import areahint.xaero.AreaOverlayColorResolver;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayFillResolver.FillTriangle;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.OverlayRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import java.util.ArrayList;
import java.util.List;

final class AreaMinimapRenderer extends MinimapElementRenderer<AreaMinimapElement, AreaMinimapContext> {
    private static final int CIRCLE_CLIP_SEGMENTS = 64;
    private static final float VIEW_LINE_INSET = 0.75F;
    // 剔除容差略大于裁剪本身使用的 1.0E-4，保证不会剔除掉仍然可见的几何
    private static final float CULL_EPSILON = 1.0E-3F;
    /** 线段裁剪复用的参数区间，避免每条边都新建 double[]。 */
    private final double[] lineClipRange = {0.0D, 1.0D};

    AreaMinimapRenderer() {
        this(new AreaMinimapContext());
    }

    private AreaMinimapRenderer(AreaMinimapContext context) {
        super(new AreaMinimapReader(), new AreaMinimapProvider(), context);
    }

    @Override
    public void preRender(MinimapElementRenderInfo renderInfo, VertexConsumerProvider.Immediate immediate,
                          MultiTextureRenderTypeRendererProvider multiTextureProvider) {
        immediate.draw();
        context.active = false;
        context.visibleAreas = List.of();
        context.fillPlan = FillPlan.empty();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientConfig.isXaeroMinimapOverlayEnabled() || client == null || client.world == null
            || renderInfo == null || renderInfo.mapDimension == null || renderInfo.renderPos == null) {
            return;
        }

        context.dimensionId = areahint.util.DimensionIdCache.getId(renderInfo.mapDimension.getValue());
        if (!context.dimensionId.equals(
                areahint.util.DimensionIdCache.getId(client.world.getRegistryKey().getValue()))) {
            return;
        }

        Vec3d renderPos = renderInfo.renderPos;
        context.renderX = renderPos.x;
        context.renderY = renderPos.y;
        context.renderZ = renderPos.z;
        context.backgroundCoordinateScale = renderInfo.backgroundCoordinateScale;
        context.transform = XaeroMinimapBridge.captureTransform();
        if (!context.transform.valid()) {
            return;
        }

        double radius = context.transform.worldRadius();
        List<OverlayArea> visible = new ArrayList<>();
        OverlaySnapshot snapshot = AreaOverlayRepository.getInstance().getSnapshot(context.dimensionId);
        for (OverlayArea area : snapshot.areas()) {
            if (!area.isVisibleAt(context.renderY)) {
                continue;
            }
            if (area.maxX() >= context.renderX - radius && area.minX() <= context.renderX + radius
                && area.maxZ() >= context.renderZ - radius && area.minZ() <= context.renderZ + radius) {
                visible.add(area);
            }
        }

        FillPlan fillPlan = context.fillResolver.resolve(snapshot, client);
        context.visibleAreas = List.copyOf(visible);
        context.fillPlan = fillPlan;
        context.active = !visible.isEmpty();
        if (context.active) {
            OverlayRenderHelper.beginOverlay();
        }
    }

    @Override
    public void postRender(MinimapElementRenderInfo renderInfo, VertexConsumerProvider.Immediate immediate,
                           MultiTextureRenderTypeRendererProvider multiTextureProvider) {
        if (context.active) {
            OverlayRenderHelper.endOverlay();
        }
    }

    @Override
    public boolean renderElement(AreaMinimapElement element, boolean outOfBounds, boolean highlighted,
                                 double depth, float optionalScale, double partialX, double partialZ,
                                 MinimapElementRenderInfo renderInfo, DrawContext drawContext,
                                 VertexConsumerProvider.Immediate immediate) {
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(partialX, partialZ, depth);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vector3f origin = matrix.transformPosition(new Vector3f());
        int halfWidth = context.transform.halfViewW();
        int halfHeight = context.transform.halfViewH();
        drawContext.enableScissor((int) Math.floor(origin.x - halfWidth), (int) Math.floor(origin.y - halfHeight),
            (int) Math.ceil(origin.x + halfWidth), (int) Math.ceil(origin.y + halfHeight));

        long now = System.currentTimeMillis();
        // 先提交进入状态处理后的填充网格，再统一绘制边界，避免后绘制的填充遮住已有边线。
        for (OverlayArea area : context.visibleAreas) {
            drawAreaFill(matrix, context.fillPlan.trianglesFor(area),
                AreaOverlayColorResolver.resolve(area, now));
        }
        for (OverlayArea area : context.visibleAreas) {
            drawAreaBoundary(matrix, area, AreaOverlayColorResolver.resolve(area, now));
        }

        drawContext.disableScissor();
        matrices.pop();
        return true;
    }

    @Override
    public boolean shouldRender(MinimapElementRenderLocation location) {
        return location == MinimapElementRenderLocation.OVER_MINIMAP;
    }

    @Override
    public int getOrder() {
        return 40;
    }

    private void drawAreaFill(Matrix4f matrix, List<FillTriangle> fillMesh, int color) {
        context.resetFillScratch();
        List<float[]> triangles = context.fillTriangles;
        boolean circle = context.transform.circle();
        float halfWidth = context.transform.halfViewW();
        float halfHeight = context.transform.halfViewH();
        for (FillTriangle triangle : fillMesh) {
            float[] first = transformInto(context.fillFirst, triangle.first());
            float[] second = transformInto(context.fillSecond, triangle.second());
            float[] third = transformInto(context.fillThird, triangle.third());
            if (circle) {
                // 包围盒完全落在可见圆外的三角形裁剪结果必为空，直接跳过 64 次半平面裁剪
                if (isTriangleOutsideCircle(first, second, third, halfWidth)) {
                    continue;
                }
                addCircleClippedTriangle(triangles, first, second, third, halfWidth);
            } else {
                if (isTriangleOutsideRectangle(first, second, third, halfWidth, halfHeight)) {
                    continue;
                }
                addRectangleClippedTriangle(triangles, first, second, third, halfWidth, halfHeight);
            }
        }
        OverlayRenderHelper.drawTriangles(matrix, triangles, color, 0.23F, 0.0F);
    }

    private void drawAreaBoundary(Matrix4f matrix, OverlayArea area, int color) {
        context.resetBoundaryScratch();
        List<float[]> lines = context.boundaryLines;
        List<Point> vertices = area.vertices();
        int vertexCount = vertices.size();
        if (vertexCount == 0) {
            return;
        }

        // 每个顶点只做一次屏幕变换，后续两条相邻边直接复用同一份坐标
        for (int i = 0; i < vertexCount; i++) {
            transformInto(context.boundaryVertex(i), vertices.get(i));
        }

        // 圆半径与矩形半宽在原实现里取的是同一个值，这里只算一次
        boolean circle = context.transform.circle();
        float halfWidth = Math.max(0.0F, context.transform.halfViewW() - VIEW_LINE_INSET);
        float halfHeight = Math.max(0.0F, context.transform.halfViewH() - VIEW_LINE_INSET);
        for (int i = 0; i < vertexCount; i++) {
            float[] first = context.boundaryVertex(i);
            float[] second = context.boundaryVertex((i + 1) % vertexCount);
            float[] clipped = context.nextBoundaryLine();
            if (circle) {
                if (clipLineToCircle(first, second, halfWidth, clipped)) {
                    lines.add(clipped);
                }
            } else if (clipLineToRectangle(first, second, halfWidth, halfHeight, clipped)) {
                lines.add(clipped);
            }
        }
        OverlayRenderHelper.drawLines(matrix, lines, color, 0.9F, 0.01F);
    }

    private void addCircleClippedTriangle(List<float[]> output, float[] first, float[] second,
                                          float[] third, float radius) {
        if (radius <= 0.0F) {
            return;
        }
        if (isInsideCircle(first, radius) && isInsideCircle(second, radius)
            && isInsideCircle(third, radius)) {
            addFillTriangle(output, first, second, third);
            return;
        }

        context.resetClipPoints();
        List<float[]> polygon = beginClipPolygon(first, second, third);
        double apothem = circleClipLimit(radius);
        // 使用内接正六十四边形逐边裁剪，每个输出三角形都严格留在 Xaero 的可见圆内。
        for (int edge = 0; edge < CIRCLE_CLIP_SEGMENTS && !polygon.isEmpty(); edge++) {
            double angle = Math.PI * 2.0D * (edge + 0.5D) / CIRCLE_CLIP_SEGMENTS;
            polygon = clipAgainstHalfPlane(polygon, otherClipPolygon(polygon),
                Math.cos(angle), Math.sin(angle), apothem);
        }
        addTriangleFan(output, polygon);
    }

    private void addRectangleClippedTriangle(List<float[]> output, float[] first, float[] second,
                                             float[] third, float halfWidth, float halfHeight) {
        if (halfWidth <= 0.0F || halfHeight <= 0.0F) {
            return;
        }
        if (isInsideRectangle(first, halfWidth, halfHeight)
            && isInsideRectangle(second, halfWidth, halfHeight)
            && isInsideRectangle(third, halfWidth, halfHeight)) {
            addFillTriangle(output, first, second, third);
            return;
        }

        context.resetClipPoints();
        List<float[]> polygon = beginClipPolygon(first, second, third);
        polygon = clipAgainstHalfPlane(polygon, otherClipPolygon(polygon), 1.0D, 0.0D, halfWidth);
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, otherClipPolygon(polygon), -1.0D, 0.0D, halfWidth);
        }
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, otherClipPolygon(polygon), 0.0D, 1.0D, halfHeight);
        }
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, otherClipPolygon(polygon), 0.0D, -1.0D, halfHeight);
        }
        addTriangleFan(output, polygon);
    }

    private void addTriangleFan(List<float[]> output, List<float[]> polygon) {
        if (polygon.size() < 3) {
            return;
        }
        float[] origin = polygon.get(0);
        for (int i = 1; i + 1 < polygon.size(); i++) {
            addFillTriangle(output, origin, polygon.get(i), polygon.get(i + 1));
        }
    }

    /** 把三角形写进复用的输出数组，坐标顺序与原实现一致。 */
    private void addFillTriangle(List<float[]> output, float[] first, float[] second, float[] third) {
        float[] slot = context.nextFillTriangle();
        slot[0] = first[0];
        slot[1] = first[1];
        slot[2] = second[0];
        slot[3] = second[1];
        slot[4] = third[0];
        slot[5] = third[1];
        output.add(slot);
    }

    /** 用传入的三个顶点初始化当前裁剪多边形，缓冲由上下文复用。 */
    private List<float[]> beginClipPolygon(float[] first, float[] second, float[] third) {
        context.clipPolygonA.clear();
        context.clipPolygonA.add(first);
        context.clipPolygonA.add(second);
        context.clipPolygonA.add(third);
        return context.clipPolygonA;
    }

    /** 返回与当前多边形缓冲配对的另一个缓冲，裁剪时输入输出交替使用。 */
    private List<float[]> otherClipPolygon(List<float[]> polygon) {
        return polygon == context.clipPolygonA ? context.clipPolygonB : context.clipPolygonA;
    }

    private List<float[]> clipAgainstHalfPlane(List<float[]> polygon, List<float[]> output, double normalX,
                                               double normalY, double limit) {
        output.clear();
        float[] previous = polygon.get(polygon.size() - 1);
        double previousDistance = previous[0] * normalX + previous[1] * normalY - limit;
        boolean previousInside = previousDistance <= 1.0E-4D;
        for (float[] current : polygon) {
            double currentDistance = current[0] * normalX + current[1] * normalY - limit;
            boolean currentInside = currentDistance <= 1.0E-4D;
            if (currentInside != previousInside) {
                double denominator = previousDistance - currentDistance;
                if (Math.abs(denominator) > 1.0E-8D) {
                    double ratio = previousDistance / denominator;
                    float[] intersection = context.nextClipPoint();
                    intersection[0] = (float) (previous[0] + (current[0] - previous[0]) * ratio);
                    intersection[1] = (float) (previous[1] + (current[1] - previous[1]) * ratio);
                    output.add(intersection);
                }
            }
            if (currentInside) {
                output.add(current);
            }
            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return output;
    }

    private boolean clipLineToRectangle(float[] first, float[] second, float halfWidth, float halfHeight,
                                        float[] output) {
        if (halfWidth <= 0.0F || halfHeight <= 0.0F) {
            return false;
        }
        double deltaX = second[0] - first[0];
        double deltaY = second[1] - first[1];
        lineClipRange[0] = 0.0D;
        lineClipRange[1] = 1.0D;
        // Liang-Barsky 参数裁剪同时覆盖线段两端都位于视口外但中部穿过视口的情况。
        if (!updateClipRange(-deltaX, first[0] + halfWidth, lineClipRange)
            || !updateClipRange(deltaX, halfWidth - first[0], lineClipRange)
            || !updateClipRange(-deltaY, first[1] + halfHeight, lineClipRange)
            || !updateClipRange(deltaY, halfHeight - first[1], lineClipRange)) {
            return false;
        }
        output[0] = (float) (first[0] + deltaX * lineClipRange[0]);
        output[1] = (float) (first[1] + deltaY * lineClipRange[0]);
        output[2] = (float) (first[0] + deltaX * lineClipRange[1]);
        output[3] = (float) (first[1] + deltaY * lineClipRange[1]);
        return true;
    }

    private boolean updateClipRange(double direction, double distance, double[] range) {
        if (Math.abs(direction) < 1.0E-8D) {
            return distance >= 0.0D;
        }
        double ratio = distance / direction;
        if (direction < 0.0D) {
            if (ratio > range[1]) {
                return false;
            }
            range[0] = Math.max(range[0], ratio);
        } else {
            if (ratio < range[0]) {
                return false;
            }
            range[1] = Math.min(range[1], ratio);
        }
        return true;
    }

    private boolean clipLineToCircle(float[] first, float[] second, float radius, float[] output) {
        if (radius <= 0.0F) {
            return false;
        }
        if (isInsideCircle(first, radius) && isInsideCircle(second, radius)) {
            output[0] = first[0];
            output[1] = first[1];
            output[2] = second[0];
            output[3] = second[1];
            return true;
        }

        double deltaX = second[0] - first[0];
        double deltaY = second[1] - first[1];
        double quadratic = deltaX * deltaX + deltaY * deltaY;
        if (quadratic < 1.0E-8D) {
            return false;
        }
        double linear = 2.0D * (first[0] * deltaX + first[1] * deltaY);
        double constant = first[0] * first[0] + first[1] * first[1] - radius * radius;
        double discriminant = linear * linear - 4.0D * quadratic * constant;
        if (discriminant < 0.0D) {
            return false;
        }
        double root = Math.sqrt(discriminant);
        double enter = Math.max(0.0D, (-linear - root) / (2.0D * quadratic));
        double exit = Math.min(1.0D, (-linear + root) / (2.0D * quadratic));
        if (enter > exit) {
            return false;
        }
        output[0] = (float) (first[0] + deltaX * enter);
        output[1] = (float) (first[1] + deltaY * enter);
        output[2] = (float) (first[0] + deltaX * exit);
        output[3] = (float) (first[1] + deltaY * exit);
        return true;
    }

    private boolean isInsideCircle(float[] point, float radius) {
        return point[0] * point[0] + point[1] * point[1] <= radius * radius + 1.0E-3F;
    }

    private boolean isInsideRectangle(float[] point, float halfWidth, float halfHeight) {
        return point[0] >= -halfWidth - 1.0E-3F && point[0] <= halfWidth + 1.0E-3F
            && point[1] >= -halfHeight - 1.0E-3F && point[1] <= halfHeight + 1.0E-3F;
    }

    private float[] transformInto(float[] output, Point point) {
        double deltaX = point.x() - context.renderX;
        double deltaZ = point.z() - context.renderZ;
        // 与 Xaero 25.2.0 的 translatePosition 保持完全相同的旋转和缩放顺序。
        double screenX = (deltaX * context.transform.ps() - deltaZ * context.transform.pc()) * context.transform.zoom();
        double screenY = (deltaX * context.transform.pc() + deltaZ * context.transform.ps()) * context.transform.zoom();
        output[0] = (float) screenX;
        output[1] = (float) screenY;
        return output;
    }

    /**
     * 三角形包围盒是否完全落在可见圆外。
     * <p>
     * 完全在外时逐边裁剪的结果必然为空，直接整块跳过；容差取 CULL_EPSILON，不会剔除仍然可见的三角形。
     */
    /**
     * 可见圆裁剪使用的内切圆半径（内接正六十四边形的边心距）。
     * <p>
     * 裁剪与剔除必须共用同一个边界，剔除侧否则会丢掉仍有一段落在内接多边形内部的三角形。
     */
    private static float circleClipLimit(float radius) {
        return (float) (radius * Math.cos(Math.PI / CIRCLE_CLIP_SEGMENTS));
    }

    private boolean isTriangleOutsideCircle(float[] first, float[] second, float[] third, float radius) {
        if (radius <= 0.0F) {
            return true;
        }
        float minX = Math.min(first[0], Math.min(second[0], third[0]));
        float maxX = Math.max(first[0], Math.max(second[0], third[0]));
        float minY = Math.min(first[1], Math.min(second[1], third[1]));
        float maxY = Math.max(first[1], Math.max(second[1], third[1]));
        // 圆心在原点，取包围盒上离圆心最近的点与半径比较
        float nearestX = Math.max(minX, Math.min(0.0F, maxX));
        float nearestY = Math.max(minY, Math.min(0.0F, maxY));
        // 裁剪用的是内接正六十四边形（内切圆半径比 radius 小约 0.12%），剔除必须按这个更保守的边界判断，
        // 否则恰好落在可见圆与内接多边形之间那条极窄带内的三角形会被误丢
        float limit = circleClipLimit(radius) + CULL_EPSILON;
        return nearestX * nearestX + nearestY * nearestY > limit * limit;
    }

    /**
     * 三角形包围盒是否完全落在可见矩形外。
     * <p>
     * 四条边任一方向都已分离时裁剪结果必然为空，直接整块跳过。
     */
    private boolean isTriangleOutsideRectangle(float[] first, float[] second, float[] third,
                                               float halfWidth, float halfHeight) {
        if (halfWidth <= 0.0F || halfHeight <= 0.0F) {
            return true;
        }
        float minX = Math.min(first[0], Math.min(second[0], third[0]));
        float maxX = Math.max(first[0], Math.max(second[0], third[0]));
        float minY = Math.min(first[1], Math.min(second[1], third[1]));
        float maxY = Math.max(first[1], Math.max(second[1], third[1]));
        return maxX < -halfWidth - CULL_EPSILON || minX > halfWidth + CULL_EPSILON
            || maxY < -halfHeight - CULL_EPSILON || minY > halfHeight + CULL_EPSILON;
    }
}
