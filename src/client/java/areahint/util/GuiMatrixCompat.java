package areahint.util;

import net.minecraft.client.gui.DrawContext;

/** 仅适配 GUI 的二维变换；1.21.6 起使用 Matrix3x2fStack，世界渲染仍保留独立的 MatrixStack。 */
public final class GuiMatrixCompat {
    private GuiMatrixCompat() {
    }

    // 在当前 DrawContext 自有栈上保存变换，确保文字与背景使用同一份 GUI 状态。
    public static void push(DrawContext context) {
        //? if >=1.21.6 {
        /*context.getMatrices().pushMatrix();*/
        //?} else {
        context.getMatrices().push();
        //?}
    }

    // 新旧 GUI 栈的压栈与出栈必须成对，避免后续 HUD 继承域名提示的缩放和平移。
    public static void pop(DrawContext context) {
        //? if >=1.21.6 {
        /*context.getMatrices().popMatrix();*/
        //?} else {
        context.getMatrices().pop();
        //?}
    }

    // 域名提示只沿屏幕平面移动，旧栈的 Z 平移保持为零。
    public static void translate(DrawContext context, float x, float y) {
        //? if >=1.21.6 {
        /*context.getMatrices().translate(x, y);*/
        //?} else {
        context.getMatrices().translate(x, y, 0);
        //?}
    }

    // 二维缩放与旧栈 Z 倍率为一的变换等价，不改变标题和副字幕的尺寸计算。
    public static void scale(DrawContext context, float x, float y) {
        //? if >=1.21.6 {
        /*context.getMatrices().scale(x, y);*/
        //?} else {
        context.getMatrices().scale(x, y, 1.0F);
        //?}
    }
}
