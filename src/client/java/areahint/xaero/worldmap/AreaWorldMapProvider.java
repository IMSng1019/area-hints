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
