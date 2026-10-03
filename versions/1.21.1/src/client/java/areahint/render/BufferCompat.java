package areahint.render;

import net.minecraft.client.render.BufferBuilder;
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
