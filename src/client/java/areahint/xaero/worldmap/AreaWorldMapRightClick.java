package areahint.xaero.worldmap;

import areahint.management.client.AreaManagementClient;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import xaero.map.gui.GuiMap;

import java.util.List;

/**
 * 世界地图域名右键双击的判定与域名管理的延迟打开。
 */
final class AreaWorldMapRightClick {
    // 双击判定窗口，超过该间隔的两次右击各自算单击
    private static final long DOUBLE_CLICK_MILLIS = 400L;
    // 双击允许的鼠标位移（界面像素），超过该距离视为两次独立右击
    private static final double DOUBLE_CLICK_MAX_MOUSE_OFFSET = 6.0D;
    // 鼠标右键编号，与 GLFW 保持一致
    private static final int RIGHT_BUTTON = 1;

    // 最近一次落在域名上的右击：时间、鼠标位置、维度和命中域名
    private static long domainClickMillis;
    private static double domainClickMouseX;
    private static double domainClickMouseY;
    private static String domainClickDimensionId;
    private static List<OverlayArea> domainClickHits;
    // 登记这次右击的抬起事件还没到达，抬起回调需要先忽略它
    private static boolean ownReleasePending;

    // 待打开的域名管理请求，只存活到本次客户端 tick 结束
    private static GuiMap pendingMap;
    private static String pendingDimensionId;
    private static List<OverlayArea> pendingHits;

    private AreaWorldMapRightClick() {
    }

    /** 为已经打开的世界地图界面注册右键抬起回调。 */
    static void register(Screen screen) {
        ScreenMouseEvents.afterMouseRelease(screen).register(AreaWorldMapRightClick::onMouseRelease);
    }

    /** 记录一次落在域名上的右击，供紧随其后的抬起回调判断双击。 */
    static void recordDomainClick(String dimensionId, List<OverlayArea> hits) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.mouse == null || dimensionId == null || hits == null || hits.isEmpty()) {
            clearDomainClick();
            return;
        }
        domainClickMillis = System.currentTimeMillis();
        domainClickMouseX = client.mouse.getX();
        domainClickMouseY = client.mouse.getY();
        domainClickDimensionId = dimensionId;
        domainClickHits = List.copyOf(hits);
        ownReleasePending = true;
    }

    /** 判断当前右击是否构成双击的第二下，命中时同时消费掉记录。 */
    static boolean isDoubleRightClick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.mouse == null
            || !isDoubleClickOf(domainClickMillis, domainClickMouseX, domainClickMouseY,
                client.mouse.getX(), client.mouse.getY())) {
            clearDomainClick();
            return false;
        }
        clearDomainClick();
        return true;
    }

    /** 抬起回调：Xaero 菜单会吞掉落在菜单上的第二次右击，只有抬起事件仍然能观察到它。 */
    private static void onMouseRelease(Screen screen, double mouseX, double mouseY, int button) {
        if (button != RIGHT_BUTTON) {
            // 两次右击之间夹了别的按键，之前的记录不再作为双击的第一下
            clearDomainClick();
            return;
        }
        if (domainClickMillis == 0L
            || !isSamePosition(domainClickMouseX, domainClickMouseY, mouseX, mouseY)) {
            // 鼠标已经离开上次右击的位置，之前的记录不再参与双击判定
            clearDomainClick();
            return;
        }
        if (ownReleasePending) {
            // 这次抬起属于刚登记的右击本身，继续交给 Xaero 正常处理
            ownReleasePending = false;
            return;
        }
        if (System.currentTimeMillis() - domainClickMillis <= DOUBLE_CLICK_MILLIS) {
            // 两次右击足够接近，直接进入域名管理
            String dimensionId = domainClickDimensionId;
            List<OverlayArea> hits = domainClickHits;
            clearDomainClick();
            if (screen instanceof GuiMap map) {
                openLater(map, dimensionId, hits);
            }
            return;
        }
        // 窗口已过：把这次抬起当成新的第一次右击，菜单遮挡下的连续右击仍能继续配对
        domainClickMillis = System.currentTimeMillis();
    }

    /** 双击成立时安排在本 tick 结束后打开域名管理，同时关闭 Xaero 的右键菜单。 */
    static void openLater(GuiMap map, String dimensionId, List<OverlayArea> hits) {
        if (map == null || dimensionId == null || hits == null || hits.isEmpty()) {
            return;
        }
        pendingMap = map;
        pendingDimensionId = dimensionId;
        pendingHits = List.copyOf(hits);
    }

    /** 由世界地图桥接在每个客户端 tick 末尾调用。 */
    static void tick() {
        GuiMap map = pendingMap;
        if (map == null) {
            return;
        }
        String dimensionId = pendingDimensionId;
        List<OverlayArea> hits = pendingHits;
        pendingMap = null;
        pendingDimensionId = null;
        pendingHits = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.currentScreen != map) {
            // 界面已经切换，放弃这次双击
            return;
        }
        // 先关掉 Xaero 为第一次右击创建的菜单，返回地图时不会再弹出
        map.closeRightClick();
        AreaManagementClient.openForHits(map, dimensionId, hits);
    }

    /** 以登记的右击为起点，判断新的右击是否落在双击窗口和允许的位移内。 */
    private static boolean isDoubleClickOf(long millis, double baseX, double baseY,
                                           double mouseX, double mouseY) {
        return millis != 0L && System.currentTimeMillis() - millis <= DOUBLE_CLICK_MILLIS
            && isSamePosition(baseX, baseY, mouseX, mouseY);
    }

    private static boolean isSamePosition(double baseX, double baseY, double mouseX, double mouseY) {
        return Math.abs(mouseX - baseX) <= DOUBLE_CLICK_MAX_MOUSE_OFFSET
            && Math.abs(mouseY - baseY) <= DOUBLE_CLICK_MAX_MOUSE_OFFSET;
    }

    private static void clearDomainClick() {
        domainClickMillis = 0L;
        domainClickDimensionId = null;
        domainClickHits = null;
        ownReleasePending = false;
    }
}
