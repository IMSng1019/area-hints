package areahint.map;

import areahint.Areashint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

/**
 * BlueMap 兼容入口（共享侧）：只做可用性判断与转发，具体实现由各版本目录的同名工厂提供。
 * 未安装 BlueMap 时绝不触碰实现类，避免在缺少依赖时触发 BlueMap 类型加载。
 */
public final class BlueMapCompat {
    private static final String BLUEMAP_MOD_ID = "bluemap";

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile BlueMapBridge bridge;

    private BlueMapCompat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        if (!FabricLoader.getInstance().isModLoaded(BLUEMAP_MOD_ID)) {
            Areashint.LOGGER.info("BlueMap 未安装，跳过地图集成。");
            return;
        }

        try {
            // 只有确认装了 BlueMap 才会执行到这里，从而加载版本目录里的实现类
            BlueMapBridge loadedBridge = BlueMapCompatFactory.create();
            loadedBridge.init();
            bridge = loadedBridge;
            available = true;
            Areashint.LOGGER.info("BlueMap 已检测到，地图联动已启用。");
        } catch (Throwable t) {
            bridge = null;
            available = false;
            Areashint.LOGGER.warn("BlueMap 已安装但兼容层初始化失败，已回退为禁用状态。", t);
        }
    }

    public static boolean isAvailable() {
        return available && bridge != null;
    }

    public static void onServerStarted(MinecraftServer server) {
        if (!initialized) {
            init();
        }
        if (!isAvailable()) {
            return;
        }

        try {
            bridge.onServerStarted(server);
        } catch (Throwable t) {
            Areashint.LOGGER.warn("BlueMap 服务端启动联动失败。", t);
        }
    }

    public static void onServerStopped() {
        if (!isAvailable()) {
            return;
        }

        try {
            bridge.onServerStopped();
        } catch (Throwable t) {
            Areashint.LOGGER.warn("BlueMap 服务端停止联动失败。", t);
        }
    }

    public static void onServerTick(long timeMs) {
        if (!isAvailable()) {
            return;
        }

        try {
            bridge.onServerTick(timeMs);
        } catch (Throwable t) {
            Areashint.LOGGER.warn("BlueMap tick 联动失败。", t);
        }
    }

    public static void requestDimensionSync(String dimensionType) {
        if (!isAvailable()) {
            return;
        }

        try {
            bridge.requestDimensionSync(dimensionType);
        } catch (Throwable t) {
            Areashint.LOGGER.warn("BlueMap 维度同步失败: {}", dimensionType, t);
        }
    }

    public static void requestFullSync() {
        if (!isAvailable()) {
            return;
        }

        try {
            bridge.requestFullSync();
        } catch (Throwable t) {
            Areashint.LOGGER.warn("BlueMap 全量同步失败。", t);
        }
    }
}
