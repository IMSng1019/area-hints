package areahint.render;

import areahint.boundviz.BoundVizRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import java.util.List;

/** 26.1.2 的几何快照随 LevelRenderState 传递，提取时使用同帧相机位置与原版剔除视锥。 */
public final class WorldRenderBridge {
    private static final RenderStateDataKey<List<BufferCompat.WorldBatch>> GEOMETRY =
            RenderStateDataKey.create(() -> "areas-hint world geometry");

    private WorldRenderBridge() {
    }

    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            // 局部栈只应用相机平移，视角旋转在绘制时由原版全局 model-view 统一处理。
            var camera = context.levelState().cameraRenderState;
            List<BufferCompat.WorldBatch> batches = BufferCompat.extractWorld(() -> BoundVizRenderer.render(
                    new PoseStack(), camera.cullFrustum, camera.pos));
            context.levelState().setData(GEOMETRY, batches);
        });
        LevelRenderEvents.END_MAIN.register(context -> {
            List<BufferCompat.WorldBatch> batches = context.levelState().getData(GEOMETRY);
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
