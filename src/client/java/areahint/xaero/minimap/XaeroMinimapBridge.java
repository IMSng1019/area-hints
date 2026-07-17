package areahint.xaero.minimap;

import areahint.AreashintClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import xaero.common.HudMod;
import xaero.hud.minimap.element.render.MinimapElementRendererHandler;

import java.lang.reflect.Field;

/**
 * Xaero 小地图 25.2.0 桥接入口。
 */
public final class XaeroMinimapBridge {
    private static boolean registered;
    private static boolean failed;
    private static Field psField;
    private static Field pcField;
    private static Field zoomField;
    private static Field halfViewWField;
    private static Field halfViewHField;
    private static Field circleField;

    private XaeroMinimapBridge() {
    }

    public static void initialize() {
        resolveReflection();
        ClientTickEvents.END_CLIENT_TICK.register(client -> tryRegister());
        tryRegister();
    }

    public static TransformState captureTransform() {
        try {
            Object handler = getHandler();
            if (handler == null) {
                return TransformState.EMPTY;
            }
            return new TransformState(
                psField.getDouble(handler), pcField.getDouble(handler), zoomField.getDouble(handler),
                halfViewWField.getInt(handler), halfViewHField.getInt(handler), circleField.getBoolean(handler));
        } catch (Exception ignored) {
            return TransformState.EMPTY;
        }
    }

    private static void tryRegister() {
        if (registered || failed) {
            return;
        }
        try {
            MinimapElementRendererHandler handler = getHandler();
            if (handler == null) {
                return;
            }
            handler.add(new AreaMinimapRenderer());
            registered = true;
            AreashintClient.LOGGER.info("Xaero 小地图域名覆盖层渲染器已注册。");
        } catch (Throwable throwable) {
            failed = true;
            AreashintClient.LOGGER.warn("注册 Xaero 小地图域名覆盖层失败，已安全禁用。", throwable);
        }
    }

    private static MinimapElementRendererHandler getHandler() {
        if (HudMod.INSTANCE == null || HudMod.INSTANCE.getMinimap() == null) {
            return null;
        }
        return HudMod.INSTANCE.getMinimap().getOverMapRendererHandler();
    }

    private static void resolveReflection() {
        try {
            Class<?> handlerClass = Class.forName("xaero.hud.minimap.element.render.over.MinimapElementOverMapRendererHandler");
            psField = field(handlerClass, "ps");
            pcField = field(handlerClass, "pc");
            zoomField = field(handlerClass, "zoom");
            halfViewWField = field(handlerClass, "halfViewW");
            halfViewHField = field(handlerClass, "halfViewH");
            circleField = field(handlerClass, "circle");
        } catch (Exception e) {
            failed = true;
            AreashintClient.LOGGER.warn("Xaero 小地图 25.2.0 反射接口不匹配，已安全禁用。", e);
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public record TransformState(double ps, double pc, double zoom, int halfViewW, int halfViewH, boolean circle) {
        static final TransformState EMPTY = new TransformState(0.0D, 1.0D, 0.0D, 0, 0, false);

        public boolean valid() {
            return Double.isFinite(zoom) && Math.abs(zoom) > 1.0E-6D && halfViewW > 0 && halfViewH > 0;
        }

        public double worldRadius() {
            if (!valid()) {
                return 0.0D;
            }
            double screenRadius = circle ? halfViewW : Math.hypot(halfViewW, halfViewH);
            return screenRadius / Math.abs(zoom) + 2.0D;
        }
    }
}
