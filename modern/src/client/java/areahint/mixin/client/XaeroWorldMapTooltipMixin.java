package areahint.mixin.client;

import areahint.xaero.XaeroTooltipFallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Xaero 缺席时跳过此 Mixin；不在共享类签名中引用任何可选地图类型。
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class XaeroWorldMapTooltipMixin {
    @Inject(method = "hoveredElementTooltipHelper", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void areahint$restoreLegacyTooltip(@Coerce Object holder, boolean overMenu,
                                              CallbackInfoReturnable<Object> info) {
        if (info.getReturnValue() != null) return;
        Object tooltip = XaeroTooltipFallback.resolve(null, holder, overMenu);
        // 返回原有 CursorBox，由 Xaero 自己处理提示位置、菜单遮挡与鼠标拖动期间的隐藏。
        if (tooltip != null) info.setReturnValue(tooltip);
    }
}
