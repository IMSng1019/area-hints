package areahint.xaero;

import areahint.AreashintClient;
import areahint.xaero.minimap.XaeroMinimapBridge;
import areahint.xaero.worldmap.XaeroWorldMapBridge;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * 1.20.4 基线节点 的 Xaero 装配实现：只在依赖存在且版本精确匹配时才触碰桥接类。
 * 支持版本号与 gradle.properties 的 deps.1.20.4.xaero_minimap / xaero_world_map 保持一致。
 */
public final class XaeroCompatImpl1204 implements XaeroBridgeHost {
    private static final String MINIMAP_ID = "xaerominimap";
    private static final String MINIMAP_VERSION = "25.2.0";
    private static final String WORLD_MAP_ID = "xaeroworldmap";
    private static final String WORLD_MAP_VERSION = "1.39.2";

    @Override
    public void initialize() {
        String minimapVersion = matchedVersion(MINIMAP_ID, MINIMAP_VERSION);
        if (minimapVersion != null) {
            try {
                XaeroMinimapBridge.initialize();
                AreashintClient.LOGGER.info("已启用 {} {} 联动。", MINIMAP_ID, minimapVersion);
            } catch (Throwable throwable) {
                AreashintClient.LOGGER.warn("{} 接口加载失败，已关闭对应联动且不影响 Areas Hint 本体。", MINIMAP_ID, throwable);
            }
        }

        String worldMapVersion = matchedVersion(WORLD_MAP_ID, WORLD_MAP_VERSION);
        if (worldMapVersion != null) {
            try {
                XaeroWorldMapBridge.initialize();
                AreashintClient.LOGGER.info("已启用 {} {} 联动。", WORLD_MAP_ID, worldMapVersion);
            } catch (Throwable throwable) {
                AreashintClient.LOGGER.warn("{} 接口加载失败，已关闭对应联动且不影响 Areas Hint 本体。", WORLD_MAP_ID, throwable);
            }
        }
    }

    /** 版本匹配时返回已安装版本号，未安装或不匹配返回 null（此时不加载任何 Xaero 类型） */
    private static String matchedVersion(String modId, String supportedVersion) {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded(modId)) {
            AreashintClient.LOGGER.info("未安装 {}，已跳过对应 Xaero 联动。", modId);
            return null;
        }

        ModContainer container = loader.getModContainer(modId).orElse(null);
        String installedVersion = container == null ? "unknown" : container.getMetadata().getVersion().getFriendlyString();
        if (!supportedVersion.equals(installedVersion)) {
            AreashintClient.LOGGER.warn("检测到不受支持的 {} 版本 {}，仅支持 {}，已安全禁用对应联动。",
                modId, installedVersion, supportedVersion);
            return null;
        }
        return installedVersion;
    }
}
