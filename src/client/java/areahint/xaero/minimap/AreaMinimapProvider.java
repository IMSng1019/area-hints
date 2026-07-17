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
