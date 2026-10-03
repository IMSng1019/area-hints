package areahint.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

/**
 * EntryListWidget 构造器跨版本兼容基类：1.20.1 为 6 参（top/bottom），1.20.2 起为 5 参（y）。
 */
public abstract class CompatEntryListWidget<E extends ElementListWidget.Entry<E>> extends ElementListWidget<E> {
    protected CompatEntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        // 1.20.2 起构造器只收一个 y，1.20.1 需要 top/bottom 两个绝对坐标，分支收敛在此处
        //? if >=1.20.2 {
        super(client, width, height, y, itemHeight);
        //?} else {
        /*super(client, width, height, y, y + height, itemHeight);*/
        //?}
    }
}
