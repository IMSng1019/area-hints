// 阶段 6 第三方隔离搬迁：BlueMap API（1.20.4 节点）的集成实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；BlueMap API 版本不同时在本文件内单独适配。

package areahint.map;

import areahint.util.ColorUtil;
import de.bluecolored.bluemap.api.math.Color;

/**
 * BlueMap 服务端动态颜色引擎。
 * 复刻客户端 FlashColorHelper 的颜色序列与周期语义。
 */
public final class BlueMapFlashColorEngine {
    private static final int[] BW_COLORS = {
        0x000000, 0x555555, 0xAAAAAA, 0xFFFFFF
    };

    private static final int[] RAINBOW_COLORS = {
        0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55,
        0x55FFFF, 0x5555FF, 0xAA00AA, 0xFF55FF
    };

    public static final long BW_CYCLE_MS = 2000L;
    public static final long RAINBOW_CYCLE_MS = 3000L;
    public static final long PER_CHAR_OFFSET_MS = 200L;
    public static final long BW_BUCKET_MS = 250L;
    public static final long RAINBOW_BUCKET_MS = 150L;

    private BlueMapFlashColorEngine() {
    }

    public static boolean isPerCharMode(String mode) {
        return ColorUtil.FLASH_BW_CHAR.equals(mode)
            || ColorUtil.FLASH_RAINBOW_CHAR.equals(mode);
    }

    public static long getCycleMs(String mode) {
        if (ColorUtil.FLASH_BW_ALL.equals(mode) || ColorUtil.FLASH_BW_CHAR.equals(mode)) {
            return BW_CYCLE_MS;
        }
        if (ColorUtil.FLASH_RAINBOW_ALL.equals(mode) || ColorUtil.FLASH_RAINBOW_CHAR.equals(mode)) {
            return RAINBOW_CYCLE_MS;
        }
        return 0L;
    }

    public static long getFullCycleMs(String mode) {
        long cycleMs = getCycleMs(mode);
        return cycleMs > 0L ? cycleMs * 2L : 0L;
    }

    public static long getBucketMs(String mode) {
        if (ColorUtil.FLASH_BW_ALL.equals(mode) || ColorUtil.FLASH_BW_CHAR.equals(mode)) {
            return BW_BUCKET_MS;
        }
        if (ColorUtil.FLASH_RAINBOW_ALL.equals(mode) || ColorUtil.FLASH_RAINBOW_CHAR.equals(mode)) {
            return RAINBOW_BUCKET_MS;
        }
        return BW_BUCKET_MS;
    }

    public static long getMinimumBucketMs() {
        return Math.min(BW_BUCKET_MS, RAINBOW_BUCKET_MS);
    }

    public static int resolveWholeRgb(String mode, long timeMs) {
        return resolveShiftedRgb(mode, timeMs, 0L);
    }

    public static Color resolveWholeColor(String mode, long timeMs) {
        return toColor(resolveWholeRgb(mode, timeMs));
    }

    public static Color resolveWholeColor(String mode, long timeMs, float alpha) {
        return toColor(resolveWholeRgb(mode, timeMs), alpha);
    }

    public static int resolveShiftedRgb(String mode, long timeMs, long offsetMs) {
        long shiftedTimeMs = timeMs + offsetMs;
        if (ColorUtil.FLASH_BW_ALL.equals(mode) || ColorUtil.FLASH_BW_CHAR.equals(mode)) {
            return interpolateCycle(BW_COLORS, shiftedTimeMs, BW_CYCLE_MS);
        }
        if (ColorUtil.FLASH_RAINBOW_ALL.equals(mode) || ColorUtil.FLASH_RAINBOW_CHAR.equals(mode)) {
            return interpolateCycle(RAINBOW_COLORS, shiftedTimeMs, RAINBOW_CYCLE_MS);
        }
        return 0xFFFFFF;
    }

    public static Color resolveShiftedColor(String mode, long timeMs, long offsetMs) {
        return toColor(resolveShiftedRgb(mode, timeMs, offsetMs));
    }

    public static Color resolveShiftedColor(String mode, long timeMs, long offsetMs, float alpha) {
        return toColor(resolveShiftedRgb(mode, timeMs, offsetMs), alpha);
    }

    public static long resolveStablePhaseOffset(String mode, String stableKey) {
        if (!isPerCharMode(mode) || stableKey == null || stableKey.isBlank()) {
            return 0L;
        }

        long fullCycleMs = getFullCycleMs(mode);
        if (fullCycleMs <= 0L) {
            return 0L;
        }

        return Math.floorMod((long) stableKey.hashCode(), fullCycleMs);
    }

    public static long resolvePhaseBucket(String mode, long timeMs, long offsetMs, long bucketMs) {
        long fullCycleMs = getFullCycleMs(mode);
        if (fullCycleMs <= 0L || bucketMs <= 0L) {
            return 0L;
        }

        long shiftedTimeMs = Math.floorMod(timeMs + offsetMs, fullCycleMs);
        return shiftedTimeMs / bucketMs;
    }

    public static Color toColor(int rgb) {
        return new Color(rgb);
    }

    public static Color toColor(int rgb, float alpha) {
        return alpha >= 1.0f ? new Color(rgb) : new Color(rgb, alpha);
    }

    private static int interpolateCycle(int[] colors, long timeMs, long cycleMs) {
        int segments = colors.length - 1;
        long fullCycleMs = cycleMs * 2L;
        float position = (float) (timeMs % fullCycleMs) / cycleMs;
        if (position > 1.0f) {
            position = 2.0f - position;
        }

        float index = position * segments;
        int lowIndex = Math.min((int) index, segments - 1);
        int highIndex = lowIndex + 1;
        float fraction = index - lowIndex;

        return lerpColor(colors[lowIndex], colors[highIndex], fraction);
    }

    private static int lerpColor(int firstColor, int secondColor, float progress) {
        int red = (int) (((firstColor >> 16) & 0xFF) * (1 - progress) + ((secondColor >> 16) & 0xFF) * progress);
        int green = (int) (((firstColor >> 8) & 0xFF) * (1 - progress) + ((secondColor >> 8) & 0xFF) * progress);
        int blue = (int) ((firstColor & 0xFF) * (1 - progress) + (secondColor & 0xFF) * progress);
        return (red << 16) | (green << 8) | blue;
    }
}
