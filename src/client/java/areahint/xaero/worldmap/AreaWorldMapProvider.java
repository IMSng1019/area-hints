package areahint.xaero.worldmap;

import areahint.config.ClientConfig;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import xaero.map.element.MapElementRenderProvider;

import java.util.Iterator;

final class AreaWorldMapProvider extends MapElementRenderProvider<OverlayArea, AreaWorldMapRenderContext> {
    private Iterator<OverlayArea> iterator;

    @Override
    public void begin(int location, AreaWorldMapRenderContext context) {
        context.dimensionId = XaeroWorldMapBridge.getViewedDimensionId();
        context.areas = ClientConfig.isXaeroWorldMapOverlayEnabled()
            ? AreaOverlayRepository.getInstance().getSnapshot(context.dimensionId).areas()
            : java.util.List.of();
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
