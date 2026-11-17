package areahint.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

/**
 * EntryListWidget 跨版本兼容基类，统一构造参数和列表滚动位置的访问接口。
 * 1.21.9 后条目内容坐标已包含旧版边距；绘制仍取整行宽度，保持现有按钮与说明布局。
 */
public abstract class CompatEntryListWidget<E extends ElementListWidget.Entry<E>> extends ElementListWidget<E> {
    protected CompatEntryListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        // 1.20.3 起构造器只收一个 y，旧版需要 top/bottom 两个绝对坐标，分支收敛在此处。
        //? if >=1.20.3 {
        super(client, width, height, y, itemHeight);
        //?} else {
        /*super(client, width, height, y, y + height, itemHeight);*/
        //?}
    }

    // 1.21.4 起滚动控件将 Amount 改为 Y，页面更新列表时通过统一入口保留滚动位置。
    public double getScrollOffset() {
        //? if <1.21.4 {
        return getScrollAmount();
        //?} else {
        /*return getScrollY();*/
        //?}
    }

    public void setScrollOffset(double offset) {
        //? if <1.21.4 {
        setScrollAmount(offset);
        //?} else {
        /*setScrollY(offset);*/
        //?}
    }
}
