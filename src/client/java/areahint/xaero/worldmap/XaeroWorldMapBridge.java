package areahint.xaero.worldmap;

import areahint.AreashintClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import xaero.map.WorldMap;
import xaero.map.gui.GuiMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Xaero 世界地图 1.39.2 桥接入口。
 */
public final class XaeroWorldMapBridge {
    private static boolean registered;
    private static boolean failed;
    private static Field viewedDimensionField;
    private static Field fallbackDimensionField;
    private static Method coordinateScaleMethod;

    private XaeroWorldMapBridge() {
    }

    public static void initialize() {
        resolveReflection();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 先处理右键双击安排好的域名管理打开，再补齐渲染器注册
            AreaWorldMapRightClick.tick();
            tryRegister();
        });
        // Xaero 菜单会吞掉落在菜单上的右击，双击判定必须借助界面级的抬起回调
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GuiMap map) {
                AreaWorldMapRightClick.register(map);
            }
        });
        tryRegister();
    }

    public static String getViewedDimensionId() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen instanceof GuiMap map) {
            RegistryKey<World> key = readDimension(map, viewedDimensionField);
            if (key == null) {
                key = readDimension(map, fallbackDimensionField);
            }
            if (key != null) {
                return key.getValue().toString();
            }
        }
        return client != null && client.world != null
            ? client.world.getRegistryKey().getValue().toString() : null;
    }

    public static double getCurrentCoordinateScale() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !(client.currentScreen instanceof GuiMap map) || coordinateScaleMethod == null) {
            return 1.0D;
        }
        try {
            Object value = coordinateScaleMethod.invoke(map);
            return value instanceof Number number && number.doubleValue() > 0.0D ? number.doubleValue() : 1.0D;
        } catch (Exception ignored) {
            return 1.0D;
        }
    }

    private static void tryRegister() {
        if (registered || failed) {
            return;
        }
        try {
            if (WorldMap.mapElementRenderHandler == null) {
                return;
            }
            WorldMap.mapElementRenderHandler.add(new AreaWorldMapRenderer());
            registered = true;
            AreashintClient.LOGGER.info("Xaero 世界地图域名覆盖层渲染器已注册。");
        } catch (Throwable throwable) {
            failed = true;
            AreashintClient.LOGGER.warn("注册 Xaero 世界地图域名覆盖层失败，已安全禁用。", throwable);
        }
    }

    private static void resolveReflection() {
        try {
            viewedDimensionField = GuiMap.class.getDeclaredField("lastViewedDimensionId");
            viewedDimensionField.setAccessible(true);
            fallbackDimensionField = GuiMap.class.getDeclaredField("lastNonNullViewedDimensionId");
            fallbackDimensionField.setAccessible(true);
            coordinateScaleMethod = GuiMap.class.getDeclaredMethod("getCurrentMapCoordinateScale");
            coordinateScaleMethod.setAccessible(true);
        } catch (Exception e) {
            failed = true;
            AreashintClient.LOGGER.warn("Xaero 世界地图 1.39.2 反射接口不匹配，已安全禁用。", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static RegistryKey<World> readDimension(GuiMap map, Field field) {
        if (field == null) {
            return null;
        }
        try {
            return (RegistryKey<World>) field.get(map);
        } catch (Exception ignored) {
            return null;
        }
    }
}
