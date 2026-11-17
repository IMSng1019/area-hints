package areahint.xaero;

import areahint.render.BufferCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.List;

/** 1.21.2/1.21.3 的地图立即模式后端，保持原来的状态捕获、几何准备及恢复顺序。 */
public final class OverlayRenderer1214 implements OverlayBackend {
    @Override
    public OverlayRenderHelper.RenderState beginOverlay() {
        RenderSnapshot state = RenderSnapshot.capture();
        prepareGeometryState();
        return state;
    }

    // 文字提交可能切换着色器，每次几何绘制前都恢复覆盖层所需的混合、深度及剔除状态。
    private static void prepareGeometryState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        // 域名顶点允许任意绕序，关闭剔除以保持旧版对正反方向顶点的相同显示行为。
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
    }

    @Override
    public void drawTriangles(Matrix4f matrix, List<float[]> triangles, int rgb, float alpha, float depth) {
        if (triangles == null || triangles.isEmpty()) {
            return;
        }
        prepareGeometryState();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = BufferCompat.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (float[] triangle : triangles) {
            if (triangle.length < 6) {
                continue;
            }
            BufferCompat.vertex(buffer, matrix, triangle[0], triangle[1], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, triangle[2], triangle[3], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, triangle[4], triangle[5], depth, red, green, blue, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    @Override
    public void drawLines(Matrix4f matrix, List<float[]> lines, int rgb, float alpha, float depth) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        prepareGeometryState();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = BufferCompat.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (float[] line : lines) {
            if (line.length < 4) {
                continue;
            }
            BufferCompat.vertex(buffer, matrix, line[0], line[1], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, line[2], line[3], depth, red, green, blue, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    // 快照只持有当前上下文的状态，恢复逻辑实现共享接口，具体 OpenGL 与 Shader 类型留在版本目录。
    private record RenderSnapshot(boolean blendEnabled, boolean depthEnabled, boolean cullEnabled,
                                  int blendSourceRgb, int blendDestinationRgb,
                                  int blendSourceAlpha, int blendDestinationAlpha,
                                  ShaderProgram shader) implements OverlayRenderHelper.RenderState {
        private static RenderSnapshot capture() {
            return new RenderSnapshot(
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glIsEnabled(GL11.GL_CULL_FACE),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                RenderSystem.getShader());
        }

        @Override
        public void restore() {
            RenderSystem.blendFuncSeparate(
                blendSourceRgb, blendDestinationRgb, blendSourceAlpha, blendDestinationAlpha);
            if (blendEnabled) {
                RenderSystem.enableBlend();
            } else {
                RenderSystem.disableBlend();
            }
            if (depthEnabled) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            if (cullEnabled) {
                RenderSystem.enableCull();
            } else {
                RenderSystem.disableCull();
            }
            if (shader != null) {
                RenderSystem.setShader(shader);
            }
        }
    }
}
