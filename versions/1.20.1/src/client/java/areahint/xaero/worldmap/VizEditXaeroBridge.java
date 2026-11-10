package areahint.xaero.worldmap;

import areahint.AreashintClient;
import areahint.vizedit.VizEditCanvas;
import areahint.vizedit.VizEditClientManager;
import areahint.vizedit.VizEditScreen;
import areahint.vizedit.VizEditSession;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import xaero.map.WorldMapSession;
import xaero.map.gui.GuiMap;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

// 参考 ProvinceMapHook 与 XaeroMapProjection 的屏幕事件接法，MIT 来源见打包的 Province-LICENSE。
public final class VizEditXaeroBridge {
    private static Field cameraX;
    private static Field cameraZ;
    private static Field scale;
    private static Field screenScale;
    private static Field resetCamera;
    private static Field cameraDestination;
    private static Field destinationAnimationX;
    private static Field destinationAnimationZ;
    private static Method changeZoom;
    private static GuiMap map;
    private static VizEditSession mapSession;
    private static boolean usable;

    private VizEditXaeroBridge() { }

    public static void initialize() {
        try {
            cameraX = field("cameraX"); cameraZ = field("cameraZ"); scale = field("scale"); screenScale = field("screenScale");
            resetCamera = field("shouldResetCameraPos"); cameraDestination = field("cameraDestination");
            destinationAnimationX = field("cameraDestinationAnimX"); destinationAnimationZ = field("cameraDestinationAnimZ");
            changeZoom = GuiMap.class.getDeclaredMethod("changeZoom", double.class, int.class); changeZoom.setAccessible(true);
            usable = true;
        } catch (ReflectiveOperationException e) {
            AreashintClient.LOGGER.warn("Xaero 可视化编辑接口不匹配，坐标网格仍可使用", e); return;
        }
        VizEditClientManager.installMap(new VizEditClientManager.MapAccess() {
            @Override public boolean available() {
                return usable && WorldMapSession.getCurrentSession() != null && WorldMapSession.getCurrentSession().isUsable();
            }
            @Override public boolean open(VizEditSession session) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (!available() || client.player == null) return false;
                if (map == null || mapSession != session) {
                    map = new GuiMap(session.parentScreen, session.parentScreen, WorldMapSession.getCurrentSession().getMapProcessor(), client.player);
                    mapSession = session;
                }
                client.setScreen(map); return true;
            }
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof GuiMap current) || !active(current)) return;
            Projection projection = new Projection(current);
            VizEditScreen editor = new VizEditScreen(mapSession, projection, current);
            editor.init(client, width, height);
            ScreenEvents.afterRender(current).register((ignored, context, mouseX, mouseY, delta) -> {
                if (!active(current)) return;
                // 使用原始双精度鼠标坐标，避免低倍缩放下一个 GUI 像素跳过数个方块。
                double preciseX = client.mouse.getX() * width / client.getWindow().getWidth();
                double preciseY = client.mouse.getY() * height / client.getWindow().getHeight();
                editor.samplePointer(preciseX, preciseY);
                var matrices = context.getMatrices();
                matrices.push(); matrices.loadIdentity(); matrices.translate(0, 0, 600);
                try { editor.render(context, mouseX, mouseY, delta); }
                finally { matrices.pop(); }
            });
            ScreenMouseEvents.allowMouseClick(current).register((ignored, x, y, button) -> {
                if (!active(current)) return true;
                editor.mouseClicked(x, y, button);
                // 原版字符输入交给同一个文本框焦点；按键仍由下方 allowKeyPress 消费。
                current.setFocused(editor.getFocused()); return false;
            });
            ScreenMouseEvents.allowMouseRelease(current).register((ignored, x, y, button) -> {
                if (!active(current)) return true;
                editor.mouseReleased(x, y, button); return false;
            });
            ScreenMouseEvents.allowMouseScroll(current).register((ignored, x, y, horizontal, vertical) -> {
                if (!active(current)) return true;
                editor.scroll(x, y, vertical); return false;
            });
            ScreenKeyboardEvents.allowKeyPress(current).register((ignored, key, scan, modifiers) -> {
                if (!active(current)) return true;
                editor.keyPressed(key, scan, modifiers); current.setFocused(editor.getFocused()); return false;
            });
            ScreenKeyboardEvents.allowKeyRelease(current).register((ignored, key, scan, modifiers) -> {
                if (!active(current)) return true;
                editor.keyReleased(key, scan, modifiers); return false;
            });
        });
    }

    private static boolean active(GuiMap current) {
        return usable && current == map && mapSession != null && mapSession == VizEditClientManager.session() && mapSession.mapMode;
    }
    private static Field field(String name) throws ReflectiveOperationException {
        Field value = GuiMap.class.getDeclaredField(name); value.setAccessible(true); return value;
    }

    // Xaero 的 cameraX/Z 位于地图坐标系，域名坐标需同时处理 GUI 比例与当前维度比例。
    private static final class Projection implements VizEditCanvas {
        private final GuiMap screen;
        Projection(GuiMap screen) { this.screen = screen; }
        private double read(Field field) {
            try { return field.getDouble(screen); }
            catch (IllegalAccessException e) { usable = false; return 1; }
        }
        private double zoom() { return Math.max(0.000001, read(scale)); }
        private double guiScale() { return Math.max(0.000001, read(screenScale)); }
        private double dimensionScale() { return Math.max(0.000001, XaeroWorldMapBridge.getCurrentCoordinateScale()); }
        private double halfWidth() { return MinecraftClient.getInstance().getWindow().getWidth() / 2.0; }
        private double halfHeight() { return MinecraftClient.getInstance().getWindow().getHeight() / 2.0; }
        @Override public double screenX(double x) { return (halfWidth() + (x / dimensionScale() - read(cameraX)) * zoom()) / guiScale(); }
        @Override public double screenY(double z) { return (halfHeight() + (z / dimensionScale() - read(cameraZ)) * zoom()) / guiScale(); }
        @Override public double worldX(double x) { return ((x * guiScale() - halfWidth()) / zoom() + read(cameraX)) * dimensionScale(); }
        @Override public double worldZ(double y) { return ((y * guiScale() - halfHeight()) / zoom() + read(cameraZ)) * dimensionScale(); }
        @Override public double pixelsPerBlock() { return zoom() / guiScale() / dimensionScale(); }
        @Override public boolean isTerrain() { return true; }
        @Override public boolean isCurrentDimension() { return Objects.equals(mapSession.dimension, XaeroWorldMapBridge.getViewedDimensionId()); }
        @Override public void pan(double dx, double dy) {
            try {
                resetCamera.setBoolean(screen, false); cameraDestination.set(screen, null);
                destinationAnimationX.set(screen, null); destinationAnimationZ.set(screen, null);
                cameraX.setDouble(screen, read(cameraX) - dx * guiScale() / zoom());
                cameraZ.setDouble(screen, read(cameraZ) - dy * guiScale() / zoom());
            } catch (IllegalAccessException e) { usable = false; }
        }
        @Override public void zoom(double amount, double x, double y) {
            try { changeZoom.invoke(screen, amount, 0); }
            catch (ReflectiveOperationException e) { usable = false; }
        }
        @Override public void frame(Rectangle2D bounds, int left, int top, int width, int height) {
            double dx = screenX(bounds.getCenterX()) - (left + width / 2.0);
            double dy = screenY(bounds.getCenterY()) - (top + height / 2.0);
            pan(-dx, -dy);
        }
    }
}
