package areahint.render;

import areahint.util.ColorUtil;

/**
 * 闪烁颜色动画辅助类
 * 根据时间计算闪烁动画的当前颜色
 */
public class FlashColorHelper {

    // 黑白灰颜色序列 (黑→深灰→灰→白)
    private static final int[] BW_COLORS = {
        0x000000, 0x555555, 0xAAAAAA, 0xFFFFFF
    };

    // 彩虹颜色序列 (红→橙→黄→绿→青→蓝→紫→粉)
    private static final int[] RAINBOW_COLORS = {
        0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
        0x55FFFF, 0x5555FF, 0xAA00AA, 0xFF55FF
    };

    // 渐变周期（毫秒）
    private static final long BW_CYCLE_MS = 2000;
    private static final long RAINBOW_CYCLE_MS = 3000;

    /**
     * 判断是否为闪烁颜色模式
     */
    public static boolean isFlashMode(String color) {
        return ColorUtil.isFlashColor(color);
    }

    /**
     * 判断是否为单字模式
     */
    public static boolean isPerCharMode(String color) {
        return ColorUtil.FLASH_BW_CHAR.equals(color)
            || ColorUtil.FLASH_RAINBOW_CHAR.equals(color);
    }

    /**
     * 计算整体模式下的当前颜色
     */
    public static int getWholeColor(String mode, long timeMs) {
        if (ColorUtil.FLASH_BW_ALL.equals(mode)) {
            return interpolateCycle(BW_COLORS, timeMs, BW_CYCLE_MS);
        } else if (ColorUtil.FLASH_RAINBOW_ALL.equals(mode)) {
            return interpolateCycle(RAINBOW_COLORS, timeMs, RAINBOW_CYCLE_MS);
        }
        return 0xFFFFFF;
    }

    /**
     * 计算单字模式下某个字符的当前颜色
     * @param charIndex 字符索引，用于产生相位偏移
     */
    public static int getCharColor(String mode, long timeMs, int charIndex) {
        long offset = charIndex * 200L; // 每个字符偏移200ms
        if (ColorUtil.FLASH_BW_CHAR.equals(mode)) {
            return interpolateCycle(BW_COLORS, timeMs + offset, BW_CYCLE_MS);
        } else if (ColorUtil.FLASH_RAINBOW_CHAR.equals(mode)) {
            return interpolateCycle(RAINBOW_COLORS, timeMs + offset, RAINBOW_CYCLE_MS);
        }
        return 0xFFFFFF;
    }

    /**
     * 在颜色序列中进行循环插值（ping-pong方式）
     */
    private static int interpolateCycle(int[] colors, long timeMs, long cycleMs) {
        // ping-pong: 正向走完再反向走回
        int segments = colors.length - 1;
        long fullCycle = cycleMs * 2; // 正向+反向
        float pos = (float) (timeMs % fullCycle) / cycleMs;
        if (pos > 1.0f) {
            pos = 2.0f - pos; // 反向
        }

        float idx = pos * segments;
        int lo = Math.min((int) idx, segments - 1);
        int hi = lo + 1;
        float frac = idx - lo;

        return lerpColor(colors[lo], colors[hi], frac);
    }

    /**
     * 线性插值两个RGB颜色
     */
    private static int lerpColor(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (r << 16) | (g << 8) | b;
    }
}
