// 阶段 6 第三方隔离：与 1.21.1 同代际（Xaero 25.3.2 / 世界地图 1.39.x，BlueMap 2.7.8），逐字复制。
// 复制理由：机制 ② 要求每个活跃节点各挂一份实现；本文件与 1.21.1 版本目录的对应文件保持一致。

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

    public static MinimapTransformState captureTransform() {
        try {
            Object handler = getHandler();
            if (handler == null) {
                return MinimapTransformState.EMPTY;
            }
            return new MinimapTransformState(
                psField.getDouble(handler), pcField.getDouble(handler), zoomField.getDouble(handler),
                halfViewWField.getInt(handler), halfViewHField.getInt(handler), circleField.getBoolean(handler));
        } catch (Exception ignored) {
            return MinimapTransformState.EMPTY;
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

    /**
     * 兼容占位：历史基线的产物里有 areahint/xaero/minimap/XaeroMinimapBridge$TransformState.class，
     * 为保证 1.20.4 产物与基线逐条一致（D1）而保留；实际逻辑已迁到 src 的 MinimapTransformState。
     */
    public record TransformState(double ps, double pc, double zoom, int halfViewW, int halfViewH, boolean circle) {
    }
}
