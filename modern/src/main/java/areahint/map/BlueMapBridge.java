package areahint.map;

import net.minecraft.server.MinecraftServer;

/**
 * BlueMap 可选依赖桥接接口。
 */
public interface BlueMapBridge {
    void init();

    void onServerStarted(MinecraftServer server);

    void onServerStopped();

    void onServerTick(long timeMs);

    void requestDimensionSync(String dimensionType);

    void requestFullSync();
}
