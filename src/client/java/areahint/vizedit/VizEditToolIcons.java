package areahint.vizedit;

import areahint.Areashint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** 图集每列对应一个工具，上下两行分别用于可用和禁用状态。 */
final class VizEditToolIcons {
    static final int SIZE = 12;
    private static final int ATLAS_WIDTH = 128;
    private static final int ATLAS_HEIGHT = 32;
    private static final Identifier TEXTURE = Identifier.tryParse(Areashint.MOD_ID + ":textures/gui/vizedit_tools.png");

    private VizEditToolIcons() {}

    static void draw(DrawContext context, VizEditSession.Tool tool, int x, int y, boolean active) {
        int u = tool.ordinal() * SIZE;
        int v = active ? 0 : SIZE;
        // 新版改由渲染层工厂或 GUI 管线提交同一图集。
        //? if <=1.21.1 {
        context.drawTexture(TEXTURE, x, y, u, v, SIZE, SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
        //?} else if <1.21.6 {
        /*context.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, TEXTURE, x, y, (float) u, (float) v, SIZE, SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);*/
        //?} else {
        /*context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, (float) u, (float) v, SIZE, SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);*/
        //?}
    }
}
