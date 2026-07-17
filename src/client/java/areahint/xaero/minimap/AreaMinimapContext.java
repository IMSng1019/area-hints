package areahint.xaero.minimap;

import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.List;

final class AreaMinimapContext {
    boolean active;
    String dimensionId;
    double renderX;
    double renderY;
    double renderZ;
    double backgroundCoordinateScale = 1.0D;
    List<OverlayArea> visibleAreas = List.of();
    OverlayArea deepestArea;
    XaeroMinimapBridge.TransformState transform = XaeroMinimapBridge.TransformState.EMPTY;
}
