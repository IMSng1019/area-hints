package areahint.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ScissorState;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

// 1.21.8 用纹理视图与显式 uniform 缓冲提交几何，保留此节点内的类名以复用既有绘制入口。
public final class GeometryRenderer1215 {
    // 保留旧 defaultBlendFunc 的独立 alpha 因子，避免地图离屏纹理的透明度在叠加时发生变化。
    private static final BlendFunction BLEND = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
    private static final RenderPipeline WORLD_TRIANGLES = createPipeline("world_triangles", VertexFormat.DrawMode.TRIANGLES, true);
    private static final RenderPipeline WORLD_LINES = createPipeline("world_lines", VertexFormat.DrawMode.DEBUG_LINES, true);
    private static final RenderPipeline OVERLAY_TRIANGLES = createPipeline("overlay_triangles", VertexFormat.DrawMode.TRIANGLES, false);
    private static final RenderPipeline OVERLAY_LINES = createPipeline("overlay_lines", VertexFormat.DrawMode.DEBUG_LINES, false);

    private GeometryRenderer1215() {
    }

    // 世界边界使用当前 Minecraft 世界输出目标；Xaero 调用显式附件重载，以保留自己的离屏绘制目标。
    public static void draw(BuiltBuffer buffer, boolean depthTest) {
        Framebuffer target = MinecraftClient.getInstance().getFramebuffer();
        draw(buffer, depthTest, target.getColorAttachmentView(), target.getDepthAttachmentView());
    }

    // 本方法取得 BuiltBuffer 所有权并负责释放；颜色和深度纹理由调用方持有，绝不能在这里关闭或清空。
    public static void draw(BuiltBuffer buffer, boolean depthTest,
                            GpuTextureView colorAttachment, GpuTextureView depthAttachment) {
        try (buffer) {
            RenderSystem.assertOnRenderThread();
            Objects.requireNonNull(colorAttachment, "域名几何绘制缺少颜色附件");
            if (depthTest) {
                Objects.requireNonNull(depthAttachment, "域名边界绘制缺少深度附件");
            }

            BuiltBuffer.DrawParameters parameters = buffer.getDrawParameters();
            if (!parameters.format().equals(VertexFormats.POSITION_COLOR)) {
                throw new IllegalArgumentException("域名几何绘制只接受 POSITION_COLOR 顶点");
            }
            RenderPipeline pipeline = selectPipeline(parameters.mode(), depthTest);
            if (parameters.indexCount() == 0) {
                return;
            }

            // 使用原版 VertexFormat 的共享即时缓冲，它会按需扩容和复用，不能当作本方法私有资源关闭。
            GpuBuffer vertices = parameters.format().uploadImmediateVertexBuffer(buffer.getBuffer());
            RenderSystem.ShapeIndexBuffer sequence = RenderSystem.getSequentialBuffer(parameters.mode());
            GpuBuffer indices = sequence.getIndexBuffer(parameters.indexCount());

            // 1.21.6 起不再自动上传矩阵，顶点颜色已包含透明度，白色调制可保持原有域名颜色。
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(
                    RenderSystem.getModelViewMatrix(), new Vector4f(1.0f), RenderSystem.getModelOffset(),
                    RenderSystem.getTextureMatrix(), RenderSystem.getShaderLineWidth());

            // 空清除值表示保留目标原有颜色与深度，防止域名覆盖层擦掉地形或 Xaero 地图。
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "areas-hint geometry", colorAttachment, OptionalInt.empty(), depthAttachment, OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                // 默认 uniform 包含当前投影，DynamicTransforms 则显式绑定本次模型视图与颜色。
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indices, sequence.getIndexType());
                // 新 RenderPass 不自动继承裁剪状态，显式复制才能保持地图窗口和 GUI 的裁剪范围。
                ScissorState scissor = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissor.method_72091()) {
                    // 此版 Yarn 尚未命名这些访问器，依次返回 x、y、width、height。
                    pass.enableScissor(scissor.method_72092(), scissor.method_72093(),
                            scissor.method_72094(), scissor.method_72095());
                }
                // 基顶点与首索引均从零开始，单实例提交完整的三角形或独立线段索引。
                pass.drawIndexed(0, 0, parameters.indexCount(), 1);
            }
        }
    }

    // DEBUG_LINES 每两个顶点组成独立线段，必须保留此模式，不能换成需要法线并展开四边形的 LINES。
    private static RenderPipeline selectPipeline(VertexFormat.DrawMode mode, boolean depthTest) {
        return switch (mode) {
            case TRIANGLES -> depthTest ? WORLD_TRIANGLES : OVERLAY_TRIANGLES;
            case DEBUG_LINES -> depthTest ? WORLD_LINES : OVERLAY_LINES;
            default -> throw new IllegalArgumentException("域名几何绘制不支持的模式：" + mode);
        };
    }

    // 世界几何读取已有深度但不写深度；二维覆盖层关闭深度测试，两者均保留透明混合并关闭背面剔除。
    private static RenderPipeline createPipeline(String name, VertexFormat.DrawMode mode, boolean depthTest) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                .withLocation(Identifier.of("areas-hint", "pipeline/" + name))
                .withVertexFormat(VertexFormats.POSITION_COLOR, mode)
                .withBlend(BLEND)
                .withDepthTestFunction(depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build());
    }
}
