package areahint.xaero;

import areahint.render.BufferCompat;
import areahint.render.GeometryRenderer1215;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;

/** 26.1.2 的地图覆盖层保存调用方的纹理视图，每次提交独立 GPU 管线而不改动后续文字的全局状态。 */
public final class OverlayRenderer1215 implements OverlayBackend {
    private static final ThreadLocal<Deque<RenderTarget>> TARGETS = ThreadLocal.withInitial(ArrayDeque::new);

    public OverlayRenderer1215() {
    }

    @Override
    public OverlayRenderHelper.RenderState beginOverlay() {
        beginOverlay(null);
        return OverlayRenderer1215::endOverlay;
    }

    public static void beginOverlay(com.mojang.blaze3d.pipeline.RenderTarget suppliedTarget) {
        // Xaero 的 ImprovedFramebuffer 会临时替换客户端 framebuffer，必须在回调中即时取值，不能缓存主屏目标。
        com.mojang.blaze3d.pipeline.RenderTarget target = suppliedTarget != null ? suppliedTarget : Minecraft.getInstance().getMainRenderTarget();
        TARGETS.get().push(new RenderTarget(target.getColorTextureView(), target.getDepthTextureView()));
    }

    public static void endOverlay() {
        Deque<RenderTarget> targets = TARGETS.get();
        if (!targets.isEmpty()) {
            targets.pop();
        }
        if (targets.isEmpty()) {
            TARGETS.remove();
        }
    }

    @Override
    public void drawTriangles(Matrix4f matrix, List<float[]> triangles, int rgb, float alpha, float depth) {
        if (triangles == null || triangles.isEmpty()) {
            return;
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = BufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (float[] triangle : triangles) {
            if (triangle.length < 6) {
                continue;
            }
            BufferCompat.vertex(buffer, matrix, triangle[0], triangle[1], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, triangle[2], triangle[3], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, triangle[4], triangle[5], depth, red, green, blue, alpha);
        }
        draw(buffer.build());
    }

    @Override
    public void drawLines(Matrix4f matrix, List<float[]> lines, int rgb, float alpha, float depth) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        BufferBuilder buffer = BufferCompat.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (float[] line : lines) {
            if (line.length < 4) {
                continue;
            }
            BufferCompat.vertex(buffer, matrix, line[0], line[1], depth, red, green, blue, alpha);
            BufferCompat.vertex(buffer, matrix, line[2], line[3], depth, red, green, blue, alpha);
        }
        draw(buffer.build());
    }

    private static void draw(MeshData buffer) {
        if (buffer == null) {
            return;
        }
        RenderTarget target = TARGETS.get().peek();
        if (target == null) {
            buffer.close();
            throw new IllegalStateException("地图覆盖层必须在 beginOverlay 与 endOverlay 之间绘制");
        }
        // 后端显式使用传入附件、继承当前裁剪矩形，并以禁用深度和剔除的透明管线绘制；其负责释放 BuiltBuffer。
        GeometryRenderer1215.draw(buffer, false, target.color(), target.depth());
    }

    private record RenderTarget(GpuTextureView color, GpuTextureView depth) {
    }
}
