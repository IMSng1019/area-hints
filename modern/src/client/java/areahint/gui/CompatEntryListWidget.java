package areahint.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

/**
 * EntryListWidget 跨版本兼容基类，统一构造参数和列表滚动位置的访问接口。
 * 1.21.9 后条目内容坐标已包含旧版边距；绘制仍取整行宽度，保持现有按钮与说明布局。
 */
public abstract class CompatEntryListWidget<E extends ContainerObjectSelectionList.Entry<E>> extends ContainerObjectSelectionList<E> {
    protected CompatEntryListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
        // 1.20.2 起构造器只收一个 y，1.20.1 需要 top/bottom 两个绝对坐标，分支收敛在此处
        super(client, width, height, y, itemHeight);
        /*super(client, width, height, y, y + height, itemHeight);*/
    }

    // 新版滚动控件将 Amount 改为 Y，页面更新列表时通过统一入口保留滚动位置。
    public double getScrollOffset() {
        /*return getScrollAmount();
        */
        return scrollAmount();
    }

    public void setScrollOffset(double offset) {
        /*setScrollAmount(offset);
        */
        setScrollAmount(offset);
    }
}
