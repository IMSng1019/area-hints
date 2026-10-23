package areahint.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

/**
 * EntryListWidget 跨版本兼容基类，统一构造参数和列表滚动位置的访问接口。
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

    // 新版滚动控件将 Amount 改为 Y，页面更新列表时通过统一入口保留滚动位置。
    public double getScrollOffset() {
        //? if <=1.21.1 {
        return getScrollAmount();
        //?} else {
        /*return getScrollY();*/
        //?}
    }

    public void setScrollOffset(double offset) {
        //? if <=1.21.1 {
        setScrollAmount(offset);
        //?} else {
        /*setScrollY(offset);*/
        //?}
    }
}
