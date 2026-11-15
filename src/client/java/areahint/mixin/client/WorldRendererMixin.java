package areahint.mixin.client;

import areahint.boundviz.BoundVizRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WorldRenderer Mixin
 * 用于在世界渲染时注入边界可视化渲染
 */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    // 旧版本保留区块调试绘制后的原有时序；新版由客户端初始化时注册的 WorldRenderEvents.LAST 接管。
    //? if <=1.20.4 {
    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldRenderer;renderChunkDebugInfo(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/Camera;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRender(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline,
                         Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager,
                         Matrix4f projectionMatrix, CallbackInfo ci) {
        // 渲染边界可视化
        BoundVizRenderer.render(matrices, tickDelta);
    }
    //?}
}
