package areahint.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 26.2 移除 Tesselator，按线程复用 CPU 分配器，帧快照仍只保存独立字节数组。 */
public final class BufferCompat {
    // 仅在提取阶段收集世界几何，地图覆盖层仍在自己的绘制阶段即时提交。
    private static final ThreadLocal<List<WorldBatch>> EXTRACTED_WORLD = new ThreadLocal<>();
    // 提取和绘制若由不同线程执行，各自拥有分配器，客户端停止时统一释放预留的原生容量。
    private static final ConcurrentLinkedQueue<ByteBufferBuilder> ALLOCATORS = new ConcurrentLinkedQueue<>();
    private static final ThreadLocal<ByteBufferBuilder> CPU_BUFFER = ThreadLocal.withInitial(() -> {
        ByteBufferBuilder allocator = new ByteBufferBuilder(256 * 1024);
        ALLOCATORS.add(allocator);
        return allocator;
    });

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

    // 每次 MeshData.close 都会释放本批结果并回收分配器的已用区间，容量保留供下一次复用。
    public static BufferBuilder beginTriangles() {
        return new BufferBuilder(CPU_BUFFER.get(), PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
    }

    public static BufferBuilder beginLines() {
        return new BufferBuilder(CPU_BUFFER.get(), PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
    }

    static void closeCpuBuffers() {
        ByteBufferBuilder allocator;
        while ((allocator = ALLOCATORS.poll()) != null) {
            allocator.close();
        }
        CPU_BUFFER.remove();
        EXTRACTED_WORLD.remove();
    }

    /** 提交一个带颜色的顶点；本代顶点链末尾不再需要 next() */
    public static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z,
                              float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, x, y, z).setColor(red, green, blue, alpha);
    }
}
