package areahint.render;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

// 26.3 使用 RenderPearl 的临时 GPU 内存与编译管线，保留反向深度读取和透明混合。
public final class GeometryRenderer1215 {
    // 保留旧 defaultBlendFunc 的独立 alpha 因子，避免地图离屏纹理的透明度在叠加时发生变化。
    private static final BlendFunction BLEND = new BlendFunction(
            BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ONE, BlendFactor.ZERO);
    private static final RenderPipeline WORLD_TRIANGLES = createPipeline("world_triangles", PrimitiveTopology.TRIANGLES, true);
    private static final RenderPipeline WORLD_LINES = createPipeline("world_lines", PrimitiveTopology.DEBUG_LINES, true);
    private static final RenderPipeline OVERLAY_TRIANGLES = createPipeline("overlay_triangles", PrimitiveTopology.TRIANGLES, false);
    private static final RenderPipeline OVERLAY_LINES = createPipeline("overlay_lines", PrimitiveTopology.DEBUG_LINES, false);

    private GeometryRenderer1215() {
    }

    // 世界边界使用当前 Minecraft 世界输出目标；Xaero 调用显式附件重载，以保留自己的离屏绘制目标。
    public static void draw(MeshData buffer, boolean depthTest) {
        RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        draw(buffer, depthTest, target.getColorTextureView(), target.getDepthTextureView());
    }

    // 本方法取得 BuiltBuffer 所有权并负责释放；颜色和深度纹理由调用方持有，绝不能在这里关闭或清空。
    public static void draw(MeshData buffer, boolean depthTest,
                            GpuTextureView colorAttachment, GpuTextureView depthAttachment) {
        try (buffer) {
            draw(buffer.vertexBuffer(), buffer.drawState(), depthTest, colorAttachment, depthAttachment);
        }
    }

    // 上传阶段临时分配直接缓冲，提交后立即释放；不让原生内存跨越提取与绘制阶段。
    static void draw(BufferCompat.WorldBatch batch) {
        ByteBuffer data = MemoryUtil.memAlloc(batch.vertices().length);
        try {
            data.put(batch.vertices()).flip();
            RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            draw(data, batch.parameters(), true, target.getColorTextureView(), target.getDepthTextureView());
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private static void draw(ByteBuffer data, MeshData.DrawState parameters, boolean depthTest,
                             GpuTextureView colorAttachment, GpuTextureView depthAttachment) {
        RenderSystem.assertOnRenderThread();
        Objects.requireNonNull(colorAttachment, "域名几何绘制缺少颜色附件");
        if (depthTest) {
            Objects.requireNonNull(depthAttachment, "域名边界绘制缺少深度附件");
        }
        if (!parameters.format().equals(DefaultVertexFormat.POSITION_COLOR)) {
            throw new IllegalArgumentException("域名几何绘制只接受 POSITION_COLOR 顶点");
        }
        RenderPipeline pipeline = selectPipeline(parameters.primitiveTopology(), depthTest);
        if (parameters.indexCount() == 0) {
            return;
        }
        // 每次从当前缓存取得编译结果，资源重载后不能继续持有旧缓存中的已编译管线。
        CompiledRenderPipeline compiledPipeline = RenderSystem.getCompiledPipeline(pipeline);

        // 临时内存立即复制 CPU 数据，GPU 切片由原版按提交周期管理，不能由本方法关闭或手工回收。
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        GpuBufferSlice vertices = encoder.transientMemory().uploadGpu(
                data, parameters.format().getVertexSize(), GpuBuffer.USAGE_VERTEX);
        RenderSystem.AutoStorageIndexBuffer sequence = RenderSystem.getSequentialBuffer(parameters.primitiveTopology());
        GpuBuffer indices = sequence.getBuffer(parameters.indexCount());

        // 位置偏移已写入顶点；1.21.11 删除线宽 uniform，纯色管线使用单位纹理矩阵与白色调制。
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0f), new Vector3f(),
                new Matrix4f());

        // 空清除值表示保留目标原有颜色与深度，防止域名覆盖层擦掉地形或 Xaero 地图。
        try (RenderPass pass = encoder.createRenderPass(
                () -> "areas-hint geometry", colorAttachment, Optional.empty(), depthAttachment, OptionalDouble.empty())) {
            pass.setPipeline(compiledPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, vertices);
            pass.setIndexBuffer(indices, sequence.type());
            // 新 RenderPass 不自动继承裁剪状态，显式复制才能保持地图窗口和 GUI 的裁剪范围。
            ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
            if (scissor.enabled()) {
                pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
            }
            // 26.2 五参顺序为索引数、实例数、首索引、顶点偏移、首实例，不能沿用旧版参数位置。
            pass.drawIndexed(parameters.indexCount(), 1, 0, 0, 0);
        }
        // 编码器底层由全局共享，Minecraft 在帧末统一 submit；此处提交会提前推进临时内存回收周期。
    }

    // DEBUG_LINES 每两个顶点组成独立线段，必须保留此模式，不能换成需要法线并展开四边形的 LINES。
    private static RenderPipeline selectPipeline(PrimitiveTopology mode, boolean depthTest) {
        return switch (mode) {
            case TRIANGLES -> depthTest ? WORLD_TRIANGLES : OVERLAY_TRIANGLES;
            case DEBUG_LINES -> depthTest ? WORLD_LINES : OVERLAY_LINES;
            default -> throw new IllegalArgumentException("域名几何绘制不支持的模式：" + mode);
        };
    }

    // 世界几何读取已有深度但不写深度；二维覆盖层关闭深度测试，两者均保留透明混合并关闭背面剔除。
    private static RenderPipeline createPipeline(String name, PrimitiveTopology mode, boolean depthTest) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("areas-hint", "pipeline/" + name))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(mode)
                .withColorTargetState(new ColorTargetState(BLEND))
                .withDepthStencilState(depthTest
                        ? Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                        : Optional.empty())
                .withCull(false)
                .build());
    }
}
