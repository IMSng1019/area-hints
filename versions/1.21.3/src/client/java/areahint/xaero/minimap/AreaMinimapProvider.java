// 阶段 6 第三方隔离：与 1.21.1 同代际（Xaero 25.3.2 / 世界地图 1.39.x，BlueMap 2.7.8），逐字复制。
// 复制理由：机制 ② 要求每个活跃节点各挂一份实现；本文件与 1.21.1 版本目录的对应文件保持一致。

package areahint.xaero.minimap;

import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;

final class AreaMinimapProvider extends MinimapElementRenderProvider<AreaMinimapElement, AreaMinimapContext> {
    private boolean consumed;

    @Override
    public void begin(MinimapElementRenderLocation location, AreaMinimapContext context) {
        consumed = !context.active;
    }

    @Override
    public boolean hasNext(MinimapElementRenderLocation location, AreaMinimapContext context) {
        return !consumed;
    }

    @Override
    public AreaMinimapElement getNext(MinimapElementRenderLocation location, AreaMinimapContext context) {
        consumed = true;
        return AreaMinimapElement.BATCH;
    }

    @Override
    public void end(MinimapElementRenderLocation location, AreaMinimapContext context) {
        consumed = true;
    }
}
