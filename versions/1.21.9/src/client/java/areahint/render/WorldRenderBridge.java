package areahint.render;

import areahint.boundviz.BoundVizRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.List;

/** 1.21.9 的世界渲染事件尚未提供，版本专属 Mixin 在原版同帧阶段调用此桥接。 */
public final class WorldRenderBridge {
    private static final RenderStateDataKey<List<BufferCompat.WorldBatch>> GEOMETRY =
            RenderStateDataKey.create(() -> "areas-hint world geometry");

    private WorldRenderBridge() {
    }

    public static void register() {
        // 保留共享初始化入口，实际注入由本版本的 WorldRenderer1219Mixin 配置注册。
    }

    public static void extract(WorldRenderState state, Matrix4f viewMatrix, Matrix4f cullProjectionMatrix) {
        // 原生缓冲在提取时关闭，状态只保存纯字节快照，并随 Fabric 的 WorldRenderState.clear 一起清理。
        List<BufferCompat.WorldBatch> batches = BufferCompat.extractWorld(() -> BoundVizRenderer.render(
                new MatrixStack(), new Matrix4f(viewMatrix), new Matrix4f(cullProjectionMatrix),
                state.cameraRenderState.pos));
        state.setData(GEOMETRY, batches);
    }

    public static void draw(WorldRenderState state) {
        List<BufferCompat.WorldBatch> batches = state.getData(GEOMETRY);
        if (batches == null) {
            return;
        }
        // 主绘制阶段只消费同帧快照及现有输出附件，保持原版深度和透明状态，不重新读取世界。
        for (BufferCompat.WorldBatch batch : batches) {
            GeometryRenderer1215.draw(batch);
        }
    }
}
