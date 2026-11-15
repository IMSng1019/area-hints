package areahint.render;

import areahint.boundviz.BoundVizRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.List;

/** 1.21.9 起将世界读取与 GPU 绘制分开，域名几何快照随原版 WorldRenderState 传递。 */
public final class WorldRenderBridge {
    private static final RenderStateDataKey<List<BufferCompat.WorldBatch>> GEOMETRY =
            RenderStateDataKey.create(() -> "areas-hint world geometry");

    private WorldRenderBridge() {
    }

    public static void register() {
        WorldRenderEvents.END_EXTRACTION.register(context -> {
            // 局部栈只应用相机平移，视角旋转在绘制时由原版全局 model-view 统一处理。
            List<BufferCompat.WorldBatch> batches = BufferCompat.extractWorld(() -> BoundVizRenderer.render(
                    new MatrixStack(), new Matrix4f(context.viewMatrix()), new Matrix4f(context.cullProjectionMatrix()),
                    context.worldState().cameraRenderState.pos));
            context.worldState().setData(GEOMETRY, batches);
        });
        WorldRenderEvents.END_MAIN.register(context -> {
            List<BufferCompat.WorldBatch> batches = context.worldState().getData(GEOMETRY);
            if (batches == null) {
                return;
            }
            // 此阶段只消费帧快照与当前 GPU 目标，不读取玩家、域名管理器或世界方块。
            for (BufferCompat.WorldBatch batch : batches) {
                GeometryRenderer1215.draw(batch);
            }
        });
    }
}
