package areahint.xaero;

import areahint.render.FlashColorHelper;
import areahint.util.ColorUtil;
import areahint.xaero.AreaOverlayRepository.OverlayArea;

/**
 * 覆盖层动态颜色解析器，颜色变化不会触发几何快照重建。
 */
public final class AreaOverlayColorResolver {
    private AreaOverlayColorResolver() {
    }

    public static int resolve(OverlayArea area, long timeMillis) {
        String color = area == null ? null : area.color();
        if (color == null) {
            return 0xFFFFFF;
        }
        if (ColorUtil.isFlashColor(color)) {
            long phasedTime = timeMillis + area.colorPhase();
            return FlashColorHelper.isPerCharMode(color)
                ? FlashColorHelper.getCharColor(color, phasedTime, 0)
                : FlashColorHelper.getWholeColor(color, phasedTime);
        }
        try {
            String normalized = ColorUtil.normalizeColor(color);
            return Integer.parseInt(normalized.substring(1), 16);
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
    }
}
