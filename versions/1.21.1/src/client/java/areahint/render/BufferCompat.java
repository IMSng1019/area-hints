package areahint.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;

/**
 * 立即模式渲染的版本适配（机制 ② 的实现侧，1.21+）：
 * 这一代 Tessellator.getBuffer()/buffer.begin()/VertexConsumer.next() 全被移除，
 * 缓冲区改由 Tessellator.begin(...) 直接返回，顶点链末尾不需要再调用 next()。
 */
public final class BufferCompat {
    private BufferCompat() {
    }

    // 世界边界保留本版本原有全局状态：透明混合、读取深度、不写深度，并允许任意顶点绕序。
    public static void beginWorld() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
    }

    // 世界边界结束后恢复原有深度写入、背面剔除和混合状态，保持该版本既有渲染行为。
    public static void endWorld() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // 本版本仍由 BufferRenderer 提交并释放构建后的缓冲，共享几何不直接依赖具体绘制后端。
    public static void drawWorld(BufferBuilder buffer) {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    /** 取一个已 begin 的缓冲区 */
    public static BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) {
        return Tessellator.getInstance().begin(mode, format);
    }

    /** 提交一个带颜色的顶点；本代顶点链末尾不再需要 next() */
    public static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                              float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha);
    }
}
