package areahint.mixin.client;

import areahint.render.WorldRenderBridge;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 为没有 Fabric 世界渲染事件的 1.21.9 补齐与后续版本一致的提取和主绘制时序。 */
@Mixin(WorldRenderer.class)
public abstract class WorldRenderer1219Mixin {
    @Shadow
    @Final
    private WorldRenderState worldRenderState;

    // 世界边界是原版最后一项状态提取，此时相机、视图和裁剪投影均属于当前帧。
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/WorldBorderRendering;updateRenderState(Lnet/minecraft/world/border/WorldBorder;Lnet/minecraft/util/math/Vec3d;DLnet/minecraft/client/render/state/WorldBorderRenderState;)V",
            shift = At.Shift.AFTER))
    private void areashint$extractWorld(ObjectAllocator allocator, RenderTickCounter tickCounter,
                                       boolean renderBlockOutline, Camera camera, Matrix4f viewMatrix,
                                       Matrix4f projectionMatrix, Matrix4f cullProjectionMatrix,
                                       GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky,
                                       CallbackInfo ci) {
        WorldRenderBridge.extract(worldRenderState, viewMatrix, cullProjectionMatrix);
    }

    // 只在主帧图任务最后一次即时批次提交前绘制，避开粒子、天气和界面的后续阶段。
    @Inject(method = "method_62214", at = @At(value = "INVOKE:LAST",
            target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V"))
    private void areashint$drawWorld(CallbackInfo ci) {
        WorldRenderBridge.draw(worldRenderState);
    }
}
