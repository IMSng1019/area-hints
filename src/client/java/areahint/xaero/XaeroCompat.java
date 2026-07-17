package areahint.xaero;

import areahint.AreashintClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.lang.reflect.Method;

/**
 * Xaero 可选兼容入口，只在版本精确匹配时反射加载对应桥接类。
 */
public final class XaeroCompat {
    private static final String MINIMAP_ID = "xaerominimap";
    private static final String MINIMAP_VERSION = "25.2.0";
    private static final String WORLD_MAP_ID = "xaeroworldmap";
    private static final String WORLD_MAP_VERSION = "1.39.2";

    private XaeroCompat() {
    }

    public static void initialize() {
        initializeBridge(MINIMAP_ID, MINIMAP_VERSION, "areahint.xaero.minimap.XaeroMinimapBridge");
        initializeBridge(WORLD_MAP_ID, WORLD_MAP_VERSION, "areahint.xaero.worldmap.XaeroWorldMapBridge");
    }

    private static void initializeBridge(String modId, String supportedVersion, String bridgeClassName) {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded(modId)) {
            AreashintClient.LOGGER.info("未安装 {}，已跳过对应 Xaero 联动。", modId);
            return;
        }

        ModContainer container = loader.getModContainer(modId).orElse(null);
        String installedVersion = container == null ? "unknown" : container.getMetadata().getVersion().getFriendlyString();
        if (!supportedVersion.equals(installedVersion)) {
            AreashintClient.LOGGER.warn("检测到不受支持的 {} 版本 {}，仅支持 {}，已安全禁用对应联动。",
                modId, installedVersion, supportedVersion);
            return;
        }

        try {
            Class<?> bridgeClass = Class.forName(bridgeClassName);
            Method initialize = bridgeClass.getMethod("initialize");
            initialize.invoke(null);
            AreashintClient.LOGGER.info("已启用 {} {} 联动。", modId, installedVersion);
        } catch (Throwable throwable) {
            AreashintClient.LOGGER.warn("{} 接口加载失败，已关闭对应联动且不影响 Areas Hint 本体。", modId, throwable);
        }
    }
}
