package areahint.xaero;

// 阶段 6 第三方隔离：与 1.21.1 同代际（Xaero 25.3.2 / 世界地图 1.39.x，BlueMap 2.7.8），逐字复制。
// 复制理由：机制 ② 要求每个活跃节点各挂一份实现；本文件与 1.21.1 版本目录的对应文件保持一致。
import areahint.AreashintClient;
import areahint.xaero.minimap.XaeroMinimapBridge;
import areahint.xaero.worldmap.XaeroWorldMapBridge;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * 1.21.5 节点 的 Xaero 装配实现：仅在依赖存在时加载桥接，实际接口不兼容时单独降级。
 * 编译参考版本与 gradle.properties 的 deps.1.21.5.xaero_minimap / xaero_world_map 保持一致。
 */
public final class XaeroCompatImpl1211 implements XaeroBridgeHost {
    private static final String MINIMAP_ID = "xaerominimap";
    private static final String MINIMAP_VERSION = "25.3.2";
    private static final String WORLD_MAP_ID = "xaeroworldmap";
    private static final String WORLD_MAP_VERSION = "1.39.5";

    @Override
    public void initialize() {
        String minimapVersion = installedVersion(MINIMAP_ID, MINIMAP_VERSION);
        if (minimapVersion != null) {
            try {
                if (XaeroMinimapBridge.initialize()) {
                    AreashintClient.LOGGER.info("已装配 {} {} 联动。", MINIMAP_ID, minimapVersion);
                }
            } catch (Throwable throwable) {
                AreashintClient.LOGGER.warn("{} 接口加载失败，已关闭对应联动且不影响 Areas Hint 本体。", MINIMAP_ID, throwable);
            }
        }

        String worldMapVersion = installedVersion(WORLD_MAP_ID, WORLD_MAP_VERSION);
        if (worldMapVersion != null) {
            try {
                if (XaeroWorldMapBridge.initialize()) {
                    AreashintClient.LOGGER.info("已装配 {} {} 联动。", WORLD_MAP_ID, worldMapVersion);
                }
            } catch (Throwable throwable) {
                AreashintClient.LOGGER.warn("{} 接口加载失败，已关闭对应联动且不影响 Areas Hint 本体。", WORLD_MAP_ID, throwable);
            }
        }
    }

    /** 返回已安装版本供日志使用；未安装时不加载任何 Xaero 类型，版本差异交给桥接实际校验 */
    private static String installedVersion(String modId, String referenceVersion) {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded(modId)) {
            AreashintClient.LOGGER.info("未安装 {}，已跳过对应 Xaero 联动。", modId);
            return null;
        }

        ModContainer container = loader.getModContainer(modId).orElse(null);
        String installedVersion = container == null ? "unknown" : container.getMetadata().getVersion().getFriendlyString();
        if (!referenceVersion.equals(installedVersion)) {
            AreashintClient.LOGGER.info("检测到 {} {}（编译参考版本 {}），将检查实际联动接口。",
                modId, installedVersion, referenceVersion);
        }
        return installedVersion;
    }
}
