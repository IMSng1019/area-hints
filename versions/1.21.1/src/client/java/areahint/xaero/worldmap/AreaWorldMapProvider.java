// 阶段 6 第三方隔离搬迁：Xaero 小地图 25.3.2 / 世界地图 1.39.2（1.21.1 节点）的绑定实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；仅当本节点 Xaero API 不同时才在本文件内单独适配。

package areahint.xaero.worldmap;

import areahint.config.ClientConfig;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import net.minecraft.client.MinecraftClient;
import xaero.map.element.MapElementRenderProvider;

import java.util.Iterator;

final class AreaWorldMapProvider extends MapElementRenderProvider<OverlayArea, AreaWorldMapRenderContext> {
    private Iterator<OverlayArea> iterator;

    @Override
    public void begin(int location, AreaWorldMapRenderContext context) {
        context.dimensionId = XaeroWorldMapBridge.getViewedDimensionId();
        if (!ClientConfig.isXaeroWorldMapOverlayEnabled()) {
            context.areas = java.util.List.of();
            context.fillPlan = FillPlan.empty();
            iterator = context.areas.iterator();
            return;
        }

        OverlaySnapshot snapshot = AreaOverlayRepository.getInstance().getSnapshot(context.dimensionId);
        context.areas = snapshot.areas();
        MinecraftClient client = MinecraftClient.getInstance();
        context.fillPlan = context.fillResolver.resolve(snapshot, client);
        iterator = context.areas.iterator();
    }

    @Override
    public boolean hasNext(int location, AreaWorldMapRenderContext context) {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public OverlayArea getNext(int location, AreaWorldMapRenderContext context) {
        return iterator == null ? null : iterator.next();
    }

    @Override
    public void end(int location, AreaWorldMapRenderContext context) {
        iterator = null;
    }
}
