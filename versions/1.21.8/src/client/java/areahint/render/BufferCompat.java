package areahint.render;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
// 1.21.5 起 VertexFormat 移到 com.mojang.blaze3d.vertex
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * 立即模式渲染的版本适配（机制 ② 的实现侧，1.21+）：
 * 这一代 Tessellator.getBuffer()/buffer.begin()/VertexConsumer.next() 全被移除，
 * 缓冲区改由 Tessellator.begin(...) 直接返回，顶点链末尾不需要再调用 next()。
 */
public final class BufferCompat {
    private BufferCompat() {
    }

    public static void beginWorld() {
        // 本版本的混合、深度和剔除随每次 GPU 管线提交设置，开始绘制时无需修改全局状态。
    }

    public static void endWorld() {
        // 每次提交自行关闭 RenderPass，后续绘制重新应用自己的管线，无需恢复旧式全局状态。
    }

    // 新版后端持有并释放 BuiltBuffer，管线读取现有深度但不写入，保持世界边界的遮挡和透明效果。
    public static void drawWorld(BufferBuilder buffer) {
        GeometryRenderer1215.draw(buffer.end(), true);
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
