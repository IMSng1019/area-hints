package areahint.vizedit;

import areahint.Areashint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** 图集每列对应一个工具，上下两行分别用于可用和禁用状态。 */
final class VizEditToolIcons {
    static final int SIZE = 12;
    private static final int ATLAS_WIDTH = 128;
    private static final int ATLAS_HEIGHT = 32;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Areashint.MOD_ID, "textures/gui/vizedit_tools.png");

    private VizEditToolIcons() {}

    static void draw(GuiGraphicsExtractor context, VizEditSession.Tool tool, int x, int y, boolean active) {
        int u = tool.ordinal() * SIZE;
        int v = active ? 0 : SIZE;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, (float) u, (float) v, SIZE, SIZE, ATLAS_WIDTH, ATLAS_HEIGHT);
    }
}
