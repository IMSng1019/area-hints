package areahint.xaero.minimap;

import areahint.xaero.AreaOverlayFillResolver;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.List;

final class AreaMinimapContext {
    final AreaOverlayFillResolver fillResolver = new AreaOverlayFillResolver();
    boolean active;
    String dimensionId;
    double renderX;
    double renderY;
    double renderZ;
    double backgroundCoordinateScale = 1.0D;
    List<OverlayArea> visibleAreas = List.of();
    OverlayArea deepestArea;
    FillPlan fillPlan = FillPlan.empty();
    XaeroMinimapBridge.TransformState transform = XaeroMinimapBridge.TransformState.EMPTY;
}
