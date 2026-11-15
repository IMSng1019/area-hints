// 1.21.8 世界地图 1.40.2 使用新的元素迭代协议，域名筛选与填充规则仍由共享层负责。

package areahint.xaero.worldmap;

import areahint.config.ClientConfig;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import net.minecraft.client.MinecraftClient;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

import java.util.Iterator;

final class AreaWorldMapProvider extends ElementRenderProvider<OverlayArea, AreaWorldMapRenderContext> {
    private Iterator<OverlayArea> iterator;

    @Override
    public void begin(ElementRenderLocation location, AreaWorldMapRenderContext context) {
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
    public boolean hasNext(ElementRenderLocation location, AreaWorldMapRenderContext context) {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public OverlayArea getNext(ElementRenderLocation location, AreaWorldMapRenderContext context) {
        return iterator == null ? null : iterator.next();
    }

    @Override
    public void end(ElementRenderLocation location, AreaWorldMapRenderContext context) {
        iterator = null;
    }
}
