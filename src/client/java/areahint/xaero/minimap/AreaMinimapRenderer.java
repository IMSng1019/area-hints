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
import net.minecraft.text.Text;
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
        context.deepestArea = null;
        context.fillPlan = FillPlan.empty();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientConfig.isXaeroMinimapOverlayEnabled() || client == null || client.world == null
            || renderInfo == null || renderInfo.mapDimension == null || renderInfo.renderPos == null) {
            return;
        }

        context.dimensionId = renderInfo.mapDimension.getValue().toString();
        if (!context.dimensionId.equals(client.world.getRegistryKey().getValue().toString())) {
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
        context.deepestArea = fillPlan.activeArea();
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
        renderDeepestName(drawContext, context.deepestArea, now);

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
        List<float[]> triangles = new ArrayList<>();
        for (FillTriangle triangle : fillMesh) {
            float[] first = transform(triangle.first());
            float[] second = transform(triangle.second());
            float[] third = transform(triangle.third());
            if (context.transform.circle()) {
                addCircleClippedTriangle(triangles, first, second, third, context.transform.halfViewW());
            } else {
                addRectangleClippedTriangle(triangles, first, second, third,
                    context.transform.halfViewW(), context.transform.halfViewH());
            }
        }
        OverlayRenderHelper.drawTriangles(matrix, triangles, color, 0.23F, 0.0F);
    }

    private void drawAreaBoundary(Matrix4f matrix, OverlayArea area, int color) {
        List<float[]> lines = new ArrayList<>();
        for (int i = 0; i < area.vertices().size(); i++) {
            float[] first = transform(area.vertices().get(i));
            float[] second = transform(area.vertices().get((i + 1) % area.vertices().size()));
            if (context.transform.circle()) {
                float radius = Math.max(0.0F, context.transform.halfViewW() - VIEW_LINE_INSET);
                float[] clipped = clipLineToCircle(first, second, radius);
                if (clipped != null) {
                    lines.add(clipped);
                }
            } else {
                float halfWidth = Math.max(0.0F, context.transform.halfViewW() - VIEW_LINE_INSET);
                float halfHeight = Math.max(0.0F, context.transform.halfViewH() - VIEW_LINE_INSET);
                float[] clipped = clipLineToRectangle(first, second, halfWidth, halfHeight);
                if (clipped != null) {
                    lines.add(clipped);
                }
            }
        }
        OverlayRenderHelper.drawLines(matrix, lines, color, 0.9F, 0.01F);
    }

    private void renderDeepestName(DrawContext drawContext, OverlayArea area, long now) {
        if (area == null) {
            return;
        }
        float[] center = transform(new Point(area.centerX(), area.centerZ()));
        net.minecraft.client.font.TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        String name = area.displayName();
        float nameScale = OverlayRenderHelper.AREA_NAME_SCALE;
        int availableScreenWidth = Math.max(1, context.transform.halfViewW() * 2 - 16);
        int maxTextWidth = Math.max(1, (int) Math.floor(availableScreenWidth / nameScale));
        if (textRenderer.getWidth(name) > maxTextWidth) {
            String suffix = "...";
            int suffixWidth = textRenderer.getWidth(suffix);
            name = suffixWidth < maxTextWidth
                ? textRenderer.trimToWidth(name, maxTextWidth - suffixWidth) + suffix
                : textRenderer.trimToWidth(name, maxTextWidth);
        }
        int width = textRenderer.getWidth(name);
        float scaledWidth = width * nameScale;
        float scaledHeight = textRenderer.fontHeight * nameScale;
        if (context.transform.circle()) {
            // 用文字外接矩形的半对角线收缩可用半径，确保完整名称不会进入圆形小地图四角。
            float textExtent = (float) Math.hypot(scaledWidth / 2.0F, scaledHeight / 2.0F);
            float availableRadius = Math.max(0.0F,
                context.transform.halfViewW() - 4.0F - textExtent);
            float distance = (float) Math.hypot(center[0], center[1]);
            if (distance > availableRadius && distance > 1.0E-4F) {
                float scale = availableRadius / distance;
                center[0] *= scale;
                center[1] *= scale;
            }
        } else {
            float maxX = Math.max(0.0F,
                context.transform.halfViewW() - 8.0F - scaledWidth / 2.0F);
            float maxY = Math.max(0.0F,
                context.transform.halfViewH() - 8.0F - scaledHeight / 2.0F);
            center[0] = Math.max(-maxX, Math.min(maxX, center[0]));
            center[1] = Math.max(-maxY, Math.min(maxY, center[1]));
        }
        int color = 0xFF000000 | AreaOverlayColorResolver.resolve(area, now);
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(Math.round(center[0]), Math.round(center[1]), 0.0D);
        matrices.scale(nameScale, nameScale, 1.0F);
        drawContext.drawTextWithShadow(textRenderer, Text.literal(name),
            -width / 2, -textRenderer.fontHeight / 2, color);
        matrices.pop();
    }

    private void addCircleClippedTriangle(List<float[]> output, float[] first, float[] second,
                                          float[] third, float radius) {
        if (radius <= 0.0F) {
            return;
        }
        if (isInsideCircle(first, radius) && isInsideCircle(second, radius)
            && isInsideCircle(third, radius)) {
            output.add(new float[]{first[0], first[1], second[0], second[1], third[0], third[1]});
            return;
        }

        List<float[]> polygon = new ArrayList<>(List.of(first, second, third));
        double apothem = radius * Math.cos(Math.PI / CIRCLE_CLIP_SEGMENTS);
        // 使用内接正六十四边形逐边裁剪，每个输出三角形都严格留在 Xaero 的可见圆内。
        for (int edge = 0; edge < CIRCLE_CLIP_SEGMENTS && !polygon.isEmpty(); edge++) {
            double angle = Math.PI * 2.0D * (edge + 0.5D) / CIRCLE_CLIP_SEGMENTS;
            polygon = clipAgainstHalfPlane(polygon, Math.cos(angle), Math.sin(angle), apothem);
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
            output.add(new float[]{first[0], first[1], second[0], second[1], third[0], third[1]});
            return;
        }

        List<float[]> polygon = new ArrayList<>(List.of(first, second, third));
        polygon = clipAgainstHalfPlane(polygon, 1.0D, 0.0D, halfWidth);
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, -1.0D, 0.0D, halfWidth);
        }
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, 0.0D, 1.0D, halfHeight);
        }
        if (!polygon.isEmpty()) {
            polygon = clipAgainstHalfPlane(polygon, 0.0D, -1.0D, halfHeight);
        }
        addTriangleFan(output, polygon);
    }

    private void addTriangleFan(List<float[]> output, List<float[]> polygon) {
        for (int i = 1; i + 1 < polygon.size(); i++) {
            float[] point = polygon.get(i);
            float[] next = polygon.get(i + 1);
            output.add(new float[]{polygon.get(0)[0], polygon.get(0)[1],
                point[0], point[1], next[0], next[1]});
        }
    }

    private List<float[]> clipAgainstHalfPlane(List<float[]> polygon, double normalX,
                                                double normalY, double limit) {
        List<float[]> clipped = new ArrayList<>(polygon.size() + 1);
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
                    clipped.add(new float[]{
                        (float) (previous[0] + (current[0] - previous[0]) * ratio),
                        (float) (previous[1] + (current[1] - previous[1]) * ratio)
                    });
                }
            }
            if (currentInside) {
                clipped.add(current);
            }
            previous = current;
            previousDistance = currentDistance;
            previousInside = currentInside;
        }
        return clipped;
    }

    private float[] clipLineToRectangle(float[] first, float[] second, float halfWidth, float halfHeight) {
        if (halfWidth <= 0.0F || halfHeight <= 0.0F) {
            return null;
        }
        double deltaX = second[0] - first[0];
        double deltaY = second[1] - first[1];
        double[] range = {0.0D, 1.0D};
        // Liang-Barsky 参数裁剪同时覆盖线段两端都位于视口外但中部穿过视口的情况。
        if (!updateClipRange(-deltaX, first[0] + halfWidth, range)
            || !updateClipRange(deltaX, halfWidth - first[0], range)
            || !updateClipRange(-deltaY, first[1] + halfHeight, range)
            || !updateClipRange(deltaY, halfHeight - first[1], range)) {
            return null;
        }
        return new float[]{
            (float) (first[0] + deltaX * range[0]), (float) (first[1] + deltaY * range[0]),
            (float) (first[0] + deltaX * range[1]), (float) (first[1] + deltaY * range[1])
        };
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

    private float[] clipLineToCircle(float[] first, float[] second, float radius) {
        if (radius <= 0.0F) {
            return null;
        }
        if (isInsideCircle(first, radius) && isInsideCircle(second, radius)) {
            return new float[]{first[0], first[1], second[0], second[1]};
        }

        double deltaX = second[0] - first[0];
        double deltaY = second[1] - first[1];
        double quadratic = deltaX * deltaX + deltaY * deltaY;
        if (quadratic < 1.0E-8D) {
            return null;
        }
        double linear = 2.0D * (first[0] * deltaX + first[1] * deltaY);
        double constant = first[0] * first[0] + first[1] * first[1] - radius * radius;
        double discriminant = linear * linear - 4.0D * quadratic * constant;
        if (discriminant < 0.0D) {
            return null;
        }
        double root = Math.sqrt(discriminant);
        double enter = Math.max(0.0D, (-linear - root) / (2.0D * quadratic));
        double exit = Math.min(1.0D, (-linear + root) / (2.0D * quadratic));
        if (enter > exit) {
            return null;
        }
        return new float[]{
            (float) (first[0] + deltaX * enter), (float) (first[1] + deltaY * enter),
            (float) (first[0] + deltaX * exit), (float) (first[1] + deltaY * exit)
        };
    }

    private boolean isInsideCircle(float[] point, float radius) {
        return point[0] * point[0] + point[1] * point[1] <= radius * radius + 1.0E-3F;
    }

    private boolean isInsideRectangle(float[] point, float halfWidth, float halfHeight) {
        return point[0] >= -halfWidth - 1.0E-3F && point[0] <= halfWidth + 1.0E-3F
            && point[1] >= -halfHeight - 1.0E-3F && point[1] <= halfHeight + 1.0E-3F;
    }

    private float[] transform(Point point) {
        double deltaX = point.x() - context.renderX;
        double deltaZ = point.z() - context.renderZ;
        // 与 Xaero 25.2.0 的 translatePosition 保持完全相同的旋转和缩放顺序。
        double screenX = (deltaX * context.transform.ps() - deltaZ * context.transform.pc()) * context.transform.zoom();
        double screenY = (deltaX * context.transform.pc() + deltaZ * context.transform.ps()) * context.transform.zoom();
        return new float[]{(float) screenX, (float) screenY};
    }
}
