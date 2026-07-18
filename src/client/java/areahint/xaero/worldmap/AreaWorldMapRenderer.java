package areahint.xaero.worldmap;

import areahint.xaero.AreaOverlayColorResolver;
import areahint.xaero.AreaOverlayFillResolver.FillTriangle;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.OverlayRenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.text.Text;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import xaero.map.element.MapElementRenderLocation;
import xaero.map.element.MapElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import java.util.ArrayList;
import java.util.List;

final class AreaWorldMapRenderer extends MapElementRenderer<OverlayArea, AreaWorldMapRenderContext, AreaWorldMapRenderer> {
    AreaWorldMapRenderer() {
        this(new AreaWorldMapRenderContext());
    }

    private AreaWorldMapRenderer(AreaWorldMapRenderContext context) {
        super(context, new AreaWorldMapProvider(), new AreaWorldMapReader(context));
    }

    @Override
    public void beforeRender(int location, MinecraftClient client, DrawContext drawContext,
                             double cameraX, double cameraZ, double mouseX, double mouseZ, float partialTicks,
                             double mapScale, double optionalScale, TextureManager textureManager,
                             TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate,
                             MultiTextureRenderTypeRendererProvider multiTextureProvider, boolean hoveredPass) {
        immediate.draw();
        context.mapScale = mapScale > 0.0D ? mapScale : 1.0D;
        context.coordinateScale = XaeroWorldMapBridge.getCurrentCoordinateScale();
        OverlayRenderHelper.beginOverlay();
    }

    @Override
    public void afterRender(int location, MinecraftClient client, DrawContext drawContext,
                            double cameraX, double cameraZ, double mouseX, double mouseZ, float partialTicks,
                            double mapScale, double optionalScale, TextureManager textureManager,
                            TextRenderer textRenderer, VertexConsumerProvider.Immediate immediate,
                            MultiTextureRenderTypeRendererProvider multiTextureProvider, boolean hoveredPass) {
        OverlayRenderHelper.endOverlay();
    }

    @Override
    public void renderElementPre(int location, OverlayArea area, boolean hovered, MinecraftClient client,
                                 DrawContext drawContext, double cameraX, double cameraZ, double mouseX,
                                 double mouseZ, float partialTicks, double mapScale, double optionalScale,
                                 TextureManager textureManager, TextRenderer textRenderer,
                                 VertexConsumerProvider.Immediate immediate,
                                 MultiTextureRenderTypeRendererProvider multiTextureProvider,
                                 float boxScale, double partialX, double partialZ,
                                 boolean hoveredPass, float frameDelta) {
    }

    @Override
    public boolean renderElement(int location, OverlayArea area, boolean hovered, MinecraftClient client,
                                 DrawContext drawContext, double cameraX, double cameraZ, double mouseX,
                                 double mouseZ, float partialTicks, double mapScale, double optionalScale,
                                 TextureManager textureManager, TextRenderer textRenderer,
                                 VertexConsumerProvider.Immediate immediate,
                                 MultiTextureRenderTypeRendererProvider multiTextureProvider,
                                 int elementIndex, double depth, float boxScale, double partialX, double partialZ,
                                 boolean hoveredPass, float frameDelta) {
        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(partialX, partialZ, depth);
        // Xaero 只为元素中心应用地图缩放，相对顶点需要在元素局部矩阵中使用同一比例。
        float worldMapScale = (float) Math.max(0.0001D, context.mapScale);
        matrices.scale(worldMapScale, worldMapScale, 1.0F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        double coordinateScale = context.coordinateScale <= 0.0D ? 1.0D : context.coordinateScale;

        List<float[]> triangles = new ArrayList<>();
        for (FillTriangle triangle : context.fillPlan.trianglesFor(area)) {
            Point first = triangle.first();
            Point second = triangle.second();
            Point third = triangle.third();
            triangles.add(new float[]{
                relativeX(area, first, coordinateScale), relativeZ(area, first, coordinateScale),
                relativeX(area, second, coordinateScale), relativeZ(area, second, coordinateScale),
                relativeX(area, third, coordinateScale), relativeZ(area, third, coordinateScale)
            });
        }

        List<float[]> lines = new ArrayList<>();
        for (int i = 0; i < area.vertices().size(); i++) {
            Point first = area.vertices().get(i);
            Point second = area.vertices().get((i + 1) % area.vertices().size());
            lines.add(new float[]{
                relativeX(area, first, coordinateScale), relativeZ(area, first, coordinateScale),
                relativeX(area, second, coordinateScale), relativeZ(area, second, coordinateScale)
            });
        }

        int color = AreaOverlayColorResolver.resolve(area, System.currentTimeMillis());
        OverlayRenderHelper.drawTriangles(matrix, triangles, color, hovered ? 0.28F : 0.14F, 0.0F);
        OverlayRenderHelper.drawLines(matrix, lines, color, hovered ? 1.0F : 0.82F, 0.01F);
        renderName(drawContext, textRenderer, area, coordinateScale, color);
        matrices.pop();
        return true;
    }

    @Override
    public boolean shouldRender(int location, boolean hoveredPass) {
        return location == MapElementRenderLocation.WORLD_MAP;
    }

    @Override
    public int getOrder() {
        return 60;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return true;
    }

    private void renderName(DrawContext drawContext, TextRenderer textRenderer, OverlayArea area,
                            double coordinateScale, int color) {
        double screenWidth = (area.maxX() - area.minX()) / coordinateScale * context.mapScale;
        double screenHeight = (area.maxZ() - area.minZ()) / coordinateScale * context.mapScale;
        String name = area.displayName();
        int textWidth = textRenderer.getWidth(name);
        if (Math.max(screenWidth, screenHeight) < Math.max(36.0D, textWidth + 8.0D)) {
            return;
        }

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        float nameScale = (float) (OverlayRenderHelper.AREA_NAME_SCALE
            / Math.max(0.0001D, context.mapScale));
        matrices.scale(nameScale, nameScale, 1.0F);
        drawContext.drawTextWithShadow(textRenderer, Text.literal(name), -textWidth / 2, -4, 0xFF000000 | color);
        matrices.pop();
    }

    private static float relativeX(OverlayArea area, Point point, double coordinateScale) {
        return (float) ((point.x() - area.centerX()) / coordinateScale);
    }

    private static float relativeZ(OverlayArea area, Point point, double coordinateScale) {
        return (float) ((point.z() - area.centerZ()) / coordinateScale);
    }
}
