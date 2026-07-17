package areahint.xaero.worldmap;

import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.List;

final class AreaWorldMapRenderContext {
    String dimensionId;
    List<OverlayArea> areas = List.of();
    double hoverX;
    double hoverZ;
    double mapScale = 1.0D;
    double coordinateScale = 1.0D;
}
