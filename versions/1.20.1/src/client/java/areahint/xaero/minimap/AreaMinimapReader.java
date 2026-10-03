// 阶段 6 第三方隔离搬迁：Xaero 小地图 25.3.2 / 世界地图 1.39.2（1.20.1 节点）的绑定实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；仅当本节点 Xaero API 不同时才在本文件内单独适配。

package areahint.xaero.minimap;

import net.minecraft.client.MinecraftClient;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;

final class AreaMinimapReader extends MinimapElementReader<AreaMinimapElement, AreaMinimapContext> {
    @Override
    public boolean isHidden(AreaMinimapElement element, AreaMinimapContext context) {
        return !context.active;
    }

    @Override
    public double getRenderX(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) {
        return context.renderX;
    }

    @Override
    public double getRenderY(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) {
        return context.renderY;
    }

    @Override
    public double getRenderZ(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) {
        return context.renderZ;
    }

    @Override
    public double getCoordinateScale(AreaMinimapElement element, AreaMinimapContext context,
                                     MinimapElementRenderInfo renderInfo) {
        return context.backgroundCoordinateScale;
    }

    @Override
    public boolean shouldScalePartialCoordinates(AreaMinimapElement element, AreaMinimapContext context,
                                                 MinimapElementRenderInfo renderInfo) {
        return false;
    }

    @Override public int getInteractionBoxLeft(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getInteractionBoxRight(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getInteractionBoxTop(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getInteractionBoxBottom(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getRenderBoxLeft(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getRenderBoxRight(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getRenderBoxTop(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getRenderBoxBottom(AreaMinimapElement element, AreaMinimapContext context, float partialTicks) { return 0; }
    @Override public int getLeftSideLength(AreaMinimapElement element, MinecraftClient client) { return 0; }
    @Override public String getMenuName(AreaMinimapElement element) { return ""; }
    @Override public String getFilterName(AreaMinimapElement element) { return ""; }
    @Override public int getMenuTextFillLeftPadding(AreaMinimapElement element) { return 0; }
    @Override public int getRightClickTitleBackgroundColor(AreaMinimapElement element) { return 0; }
    @Override public boolean shouldScaleBoxWithOptionalScale() { return false; }
}
