package areahint.render;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * 立即模式渲染的版本适配（机制 ② 的实现侧，1.20.1–1.20.4）：
 * 这一代用 Tessellator.getBuffer() 取缓冲区、buffer.begin() 起画、每个顶点末尾必须 next()。
 * 本类位于版本目录，只有对应节点会挂载它；共享 src/ 只保留调用点。
 */
public final class BufferCompat {
    private BufferCompat() {
    }

    /** 取一个已 begin 的缓冲区（1.20.5+ 改由 Tessellator.begin 直接返回） */
    public static BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(mode, format);
        return buffer;
    }

    /** 提交一个带颜色的顶点；本代需要显式 next() 才会真正写入 */
    public static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                              float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha).next();
    }
}
