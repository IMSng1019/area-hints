// 26.1.2 世界地图使用 Xaero 独立的三维绘制上下文，保留域名几何与比例计算。

package areahint.xaero.worldmap;

import areahint.xaero.AreaOverlayColorResolver;
import areahint.xaero.AreaOverlayFillResolver.FillTriangle;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.OverlayRenderHelper;
import areahint.xaero.OverlayRenderer1215;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

final class AreaWorldMapRenderer extends ElementRenderer<OverlayArea, AreaWorldMapRenderContext, AreaWorldMapRenderer> {
    private final List<PendingLabel> pendingLabels = new ArrayList<>();
    private MapElementGraphics labelGraphics;

    AreaWorldMapRenderer() {
        this(new AreaWorldMapRenderContext());
    }

    private AreaWorldMapRenderer(AreaWorldMapRenderContext context) {
        super(context, new AreaWorldMapProvider(), new AreaWorldMapReader(context));
    }

    @Override
    public void preRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource immediate,
                          MultiTextureRenderTypeRendererProvider multiTextureProvider, boolean hoveredPass) {
        immediate.endBatch();
        pendingLabels.clear();
        labelGraphics = null;
        context.mapScale = renderInfo.scale > 0.0D ? renderInfo.scale : 1.0D;
        context.coordinateScale = XaeroWorldMapBridge.getCurrentCoordinateScale();
    }

    @Override
    public void postRender(ElementRenderInfo renderInfo, MultiBufferSource.BufferSource immediate,
                           MultiTextureRenderTypeRendererProvider multiTextureProvider, boolean hoveredPass) {
        // 整批域名几何完成后绘制标签，维持旧版标签覆盖填充的顺序，并在 Xaero 切换目标前提交。
        if (labelGraphics == null || pendingLabels.isEmpty()) {
            return;
        }
        PoseStack matrices = labelGraphics.pose();
        matrices.pushPose();
        try {
            Font textRenderer = Minecraft.getInstance().font;
            for (PendingLabel label : pendingLabels) {
                matrices.last().pose().set(label.matrix());
                labelGraphics.drawString(textRenderer, label.text(), label.x(), -4, label.color(), true);
            }
            labelGraphics.flush();
        } finally {
            matrices.popPose();
            pendingLabels.clear();
            labelGraphics = null;
        }
    }

    @Override
    public void renderElementShadow(OverlayArea area, boolean hovered, float boxScale,
                                    double partialX, double partialZ, ElementRenderInfo renderInfo,
                                    MapElementGraphics graphics, MultiBufferSource.BufferSource immediate,
                                    MultiTextureRenderTypeRendererProvider multiTextureProvider) {
    }

    @Override
    public boolean renderElement(OverlayArea area, boolean hovered, double depth, float boxScale,
                                 double partialX, double partialZ, ElementRenderInfo renderInfo,
                                 MapElementGraphics graphics, MultiBufferSource.BufferSource immediate,
                                 MultiTextureRenderTypeRendererProvider multiTextureProvider) {
        PoseStack matrices = graphics.pose();
        graphics.flush();
        // Xaero 的地图几何继续使用三维矩阵与即时目标，不能送进原版 GUI 的二维延迟队列。
        OverlayRenderer1215.beginOverlay(renderInfo.framebuffer);
        matrices.pushPose();
        try {
            matrices.translate(partialX, partialZ, depth);
            // Xaero 只为元素中心应用地图缩放，相对顶点需要在元素局部矩阵中使用同一比例。
            float worldMapScale = (float) Math.max(0.0001D, context.mapScale);
            matrices.scale(worldMapScale, worldMapScale, 1.0F);
            Matrix4f matrix = matrices.last().pose();
            double coordinateScale = context.coordinateScale <= 0.0D ? 1.0D : context.coordinateScale;

            // 域名中心与坐标比例在循环外只读一次，坐标数组由上下文缓冲复用
            context.resetScratch();
            List<float[]> triangles = context.triangles;
            double centerX = area.centerX();
            double centerZ = area.centerZ();
            for (FillTriangle triangle : context.fillPlan.trianglesFor(area)) {
                Point first = triangle.first();
                Point second = triangle.second();
                Point third = triangle.third();
                float[] slot = context.nextTriangle();
                slot[0] = relativeX(first.x(), centerX, coordinateScale);
                slot[1] = relativeZ(first.z(), centerZ, coordinateScale);
                slot[2] = relativeX(second.x(), centerX, coordinateScale);
                slot[3] = relativeZ(second.z(), centerZ, coordinateScale);
                slot[4] = relativeX(third.x(), centerX, coordinateScale);
                slot[5] = relativeZ(third.z(), centerZ, coordinateScale);
                triangles.add(slot);
            }

            List<float[]> lines = context.lines;
            List<Point> vertices = area.vertices();
            int vertexCount = vertices.size();
            for (int i = 0; i < vertexCount; i++) {
                Point first = vertices.get(i);
                Point second = vertices.get((i + 1) % vertexCount);
                float[] slot = context.nextLine();
                slot[0] = relativeX(first.x(), centerX, coordinateScale);
                slot[1] = relativeZ(first.z(), centerZ, coordinateScale);
                slot[2] = relativeX(second.x(), centerX, coordinateScale);
                slot[3] = relativeZ(second.z(), centerZ, coordinateScale);
                lines.add(slot);
            }

            int color = AreaOverlayColorResolver.resolve(area, System.currentTimeMillis());
            OverlayRenderHelper.drawTriangles(matrix, triangles, color, hovered ? 0.28F : 0.14F, 0.0F);
            OverlayRenderHelper.drawLines(matrix, lines, color, hovered ? 1.0F : 0.82F, 0.01F);
            queueName(graphics, Minecraft.getInstance().font, area, coordinateScale, color);
        } finally {
            OverlayRenderHelper.endOverlay();
            matrices.popPose();
        }
        return true;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean hoveredPass) {
        return location == ElementRenderLocation.WORLD_MAP;
    }

    @Override
    public int getOrder() {
        return 60;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return true;
    }

    private void queueName(MapElementGraphics graphics, Font textRenderer, OverlayArea area,
                            double coordinateScale, int color) {
        double mapScale = context.mapScale;
        double screenWidth = (area.maxX() - area.minX()) / coordinateScale * mapScale;
        double screenHeight = (area.maxZ() - area.minZ()) / coordinateScale * mapScale;
        String name = area.displayName();
        int textWidth = textRenderer.width(name);
        if (Math.max(screenWidth, screenHeight) < Math.max(36.0D, textWidth + 8.0D)) {
            return;
        }

        PoseStack matrices = graphics.pose();
        matrices.pushPose();
        try {
            float nameScale = (float) (OverlayRenderHelper.AREA_NAME_SCALE
                / Math.max(0.0001D, mapScale));
            matrices.scale(nameScale, nameScale, 1.0F);
            // Xaero 每个元素退出后会恢复矩阵，因此必须保存变换快照供本批末尾使用。
            labelGraphics = graphics;
            pendingLabels.add(new PendingLabel(new Matrix4f(matrices.last().pose()),
                Component.literal(name), -textWidth / 2, 0xFF000000 | color));
        } finally {
            matrices.popPose();
        }
    }

    private static float relativeX(double pointX, double centerX, double coordinateScale) {
        return (float) ((pointX - centerX) / coordinateScale);
    }

    private static float relativeZ(double pointZ, double centerZ, double coordinateScale) {
        return (float) ((pointZ - centerZ) / coordinateScale);
    }

    private record PendingLabel(Matrix4f matrix, Component text, int x, int color) {
    }
}
