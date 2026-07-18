package areahint.xaero.worldmap;

import areahint.xaero.AreaOverlayFillResolver;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayRepository.OverlayArea;

import java.util.List;

final class AreaWorldMapRenderContext {
    final AreaOverlayFillResolver fillResolver = new AreaOverlayFillResolver();
    String dimensionId;
    List<OverlayArea> areas = List.of();
    FillPlan fillPlan = FillPlan.empty();
    double hoverX;
    double hoverZ;
    double mapScale = 1.0D;
    double coordinateScale = 1.0D;
}
