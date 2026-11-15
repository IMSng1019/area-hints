package areahint.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
// 1.21.5 起 VertexFormat 移到 com.mojang.blaze3d.vertex
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 立即模式渲染的版本适配（机制 ② 的实现侧，1.21+）：
 * 这一代 Tessellator.getBuffer()/buffer.begin()/VertexConsumer.next() 全被移除，
 * 缓冲区改由 Tessellator.begin(...) 直接返回，顶点链末尾不需要再调用 next()。
 */
public final class BufferCompat {
    // 仅在提取阶段收集世界几何，地图覆盖层仍在自己的绘制阶段即时提交。
    private static final ThreadLocal<List<WorldBatch>> EXTRACTED_WORLD = new ThreadLocal<>();

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
        List<WorldBatch> batches = EXTRACTED_WORLD.get();
        if (batches == null) {
            GeometryRenderer1215.draw(buffer.buildOrThrow(), true);
            return;
        }
        // 原生 BuiltBuffer 必须在本次提取内关闭，纯字节快照即使跳过绘制也不会泄漏堆外内存。
        try (MeshData built = buffer.buildOrThrow()) {
            ByteBuffer source = built.vertexBuffer().duplicate();
            byte[] vertices = new byte[source.remaining()];
            source.get(vertices);
            batches.add(new WorldBatch(vertices, built.drawState()));
        }
    }

    // 返回只读批次列表；每个顶点数组由本次提取独占，绘制阶段只读取而不修改。
    static List<WorldBatch> extractWorld(Runnable extraction) {
        if (EXTRACTED_WORLD.get() != null) {
            throw new IllegalStateException("域名世界几何不能嵌套提取");
        }
        List<WorldBatch> batches = new ArrayList<>();
        EXTRACTED_WORLD.set(batches);
        try {
            extraction.run();
            return List.copyOf(batches);
        } finally {
            EXTRACTED_WORLD.remove();
        }
    }

    record WorldBatch(byte[] vertices, MeshData.DrawState parameters) {
    }

    /** 取一个已 begin 的缓冲区 */
    public static BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
        return Tesselator.getInstance().begin(mode, format);
    }

    /** 提交一个带颜色的顶点；本代顶点链末尾不再需要 next() */
    public static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                              float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }
}
