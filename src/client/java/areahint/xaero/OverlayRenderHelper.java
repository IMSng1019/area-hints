package areahint.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Xaero 两类地图共用的二维彩色几何提交工具。
 */
public final class OverlayRenderHelper {
    private static final ThreadLocal<Deque<RenderState>> RENDER_STATES =
        ThreadLocal.withInitial(ArrayDeque::new);

    private OverlayRenderHelper() {
    }

    public static void beginOverlay() {
        RENDER_STATES.get().push(RenderState.capture());
        prepareGeometryState();
    }

    /**
     * 文字渲染会切换全局着色器，因此每次提交几何前都必须恢复覆盖层状态。
     */
    private static void prepareGeometryState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    public static void endOverlay() {
        Deque<RenderState> states = RENDER_STATES.get();
        if (states.isEmpty()) {
            return;
        }
        RenderState state = states.pop();
        state.restore();
        if (states.isEmpty()) {
            RENDER_STATES.remove();
        }
    }

    public static void drawTriangles(Matrix4f matrix, List<float[]> triangles, int rgb, float alpha, float depth) {
        if (triangles == null || triangles.isEmpty()) {
            return;
        }
        prepareGeometryState();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (float[] triangle : triangles) {
            if (triangle.length < 6) {
                continue;
            }
            buffer.vertex(matrix, triangle[0], triangle[1], depth).color(red, green, blue, alpha).next();
            buffer.vertex(matrix, triangle[2], triangle[3], depth).color(red, green, blue, alpha).next();
            buffer.vertex(matrix, triangle[4], triangle[5], depth).color(red, green, blue, alpha).next();
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    public static void drawLines(Matrix4f matrix, List<float[]> lines, int rgb, float alpha, float depth) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        prepareGeometryState();
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (float[] line : lines) {
            if (line.length < 4) {
                continue;
            }
            buffer.vertex(matrix, line[0], line[1], depth).color(red, green, blue, alpha).next();
            buffer.vertex(matrix, line[2], line[3], depth).color(red, green, blue, alpha).next();
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private record RenderState(boolean blendEnabled, boolean depthEnabled,
                               int blendSourceRgb, int blendDestinationRgb,
                               int blendSourceAlpha, int blendDestinationAlpha,
                               ShaderProgram shader) {
        private static RenderState capture() {
            return new RenderState(
                GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                RenderSystem.getShader());
        }

        private void restore() {
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
            if (shader != null) {
                RenderSystem.setShader(() -> shader);
            }
        }
    }
}
