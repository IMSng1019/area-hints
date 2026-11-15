package areahint.util;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** 保留旧 drawBorder 的一像素内描边及即时提交顺序，避免新版延后描边丢失列表裁剪状态。 */
public final class GuiDrawCompat {
    private GuiDrawCompat() {
    }

    // 与旧版四个 fill 的端点完全一致，左右边不重复覆盖四角，半透明颜色只混合一次。
    public static void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
