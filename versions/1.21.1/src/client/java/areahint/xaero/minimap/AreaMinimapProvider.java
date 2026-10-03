// 阶段 6 第三方隔离搬迁：Xaero 小地图 25.3.2 / 世界地图 1.39.2（1.21.1 节点）的绑定实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；仅当本节点 Xaero API 不同时才在本文件内单独适配。

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
