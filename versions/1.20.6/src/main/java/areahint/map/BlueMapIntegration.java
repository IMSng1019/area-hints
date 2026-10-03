// 阶段 6 第三方隔离搬迁：BlueMap API（1.20.6 节点）的集成实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；BlueMap API 版本不同时在本文件内单独适配。

package areahint.map;

import areahint.Areashint;
import areahint.data.AreaData;
import areahint.file.FileManager;
import areahint.network.Packets;
import areahint.world.WorldFolderManager;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BlueMap 实际集成实现。
 */
public final class BlueMapIntegration implements BlueMapBridge {
    private static final String MARKER_SET_ID = "areas-hint-domains";
    private static final long MIN_DYNAMIC_REFRESH_INTERVAL_MS = BlueMapFlashColorEngine.getMinimumBucketMs();

    private final Map<String, DimensionRuntime> dimensionRuntimes = new ConcurrentHashMap<>();

    private volatile MinecraftServer server;
    private volatile BlueMapAPI api;
    private volatile boolean serverReady;
    private volatile boolean apiReady;
    private volatile boolean initialSyncDone;
    private volatile long lastDynamicRefreshTimeMs;

    @Override
    public void init() {
        BlueMapAPI.onEnable(this::handleBlueMapEnable);
        BlueMapAPI.onDisable(this::handleBlueMapDisable);
        BlueMapAPI.getInstance().ifPresent(this::handleBlueMapEnable);
    }

    @Override
    public void onServerStarted(MinecraftServer server) {
        this.server = server;
        this.serverReady = server != null;
        tryInitialFullSync();
    }

    @Override
    public void onServerStopped() {
        clearAllMarkerSets();
        this.serverReady = false;
        this.server = null;
        this.initialSyncDone = false;
        this.lastDynamicRefreshTimeMs = 0L;
    }

    @Override
    public void onServerTick(long timeMs) {
        if (!isReady() || dimensionRuntimes.isEmpty()) {
            return;
        }
        if (timeMs - lastDynamicRefreshTimeMs < MIN_DYNAMIC_REFRESH_INTERVAL_MS) {
            return;
        }

        lastDynamicRefreshTimeMs = timeMs;
        for (DimensionRuntime runtime : dimensionRuntimes.values()) {
            updateDynamicMarkers(runtime.dynamicMarkers(), timeMs);
        }
    }

    @Override
    public void requestDimensionSync(String dimensionType) {
        if (!isReady()) {
            return;
        }
        syncDimension(dimensionType);
    }

    @Override
    public void requestFullSync() {
        if (!isReady()) {
            return;
        }

        syncDimension(Packets.DIMENSION_OVERWORLD);
        syncDimension(Packets.DIMENSION_NETHER);
        syncDimension(Packets.DIMENSION_END);
    }

    private void handleBlueMapEnable(BlueMapAPI api) {
        this.api = api;
        this.apiReady = true;
        Areashint.LOGGER.info("BlueMap API 已就绪，等待服务器世界同步。");
        tryInitialFullSync();
    }

    private void handleBlueMapDisable(BlueMapAPI api) {
        clearAllMarkerSets();
        this.apiReady = false;
        this.api = null;
        this.initialSyncDone = false;
        this.lastDynamicRefreshTimeMs = 0L;
        Areashint.LOGGER.info("BlueMap API 已关闭，已清理区域提示 marker。");
    }

    private void tryInitialFullSync() {
        if (!isReady() || initialSyncDone) {
            return;
        }

        requestFullSync();
        initialSyncDone = true;
        Areashint.LOGGER.info("BlueMap 首次全量同步完成。");
    }

    private boolean isReady() {
        return apiReady && serverReady && api != null && server != null;
    }

    private void syncDimension(String dimensionType) {
        ServerWorld serverWorld = resolveServerWorld(dimensionType);
        if (serverWorld == null) {
            Areashint.LOGGER.debug("BlueMap 跳过未知维度同步: {}", dimensionType);
            return;
        }

        Optional<BlueMapWorld> worldOptional = api.getWorld(serverWorld);
        if (worldOptional.isEmpty()) {
            Areashint.LOGGER.debug("BlueMap 中未找到维度 {} 对应的世界，已跳过。", serverWorld.getRegistryKey().getValue());
            return;
        }

        DimensionRuntime runtime = buildDimensionRuntime(serverWorld, dimensionType, System.currentTimeMillis());
        dimensionRuntimes.put(dimensionType, runtime);

        BlueMapWorld blueMapWorld = worldOptional.get();
        for (BlueMapMap map : blueMapWorld.getMaps()) {
            map.getMarkerSets().put(MARKER_SET_ID, runtime.markerSet());
        }
    }

    private DimensionRuntime buildDimensionRuntime(ServerWorld serverWorld, String dimensionType, long timeMs) {
        String dimensionId = serverWorld.getRegistryKey().getValue().toString();
        MarkerSet markerSet = MarkerSet.builder()
            .label("区域提示 - " + areahint.dimensional.DimensionalNameManager.getDimensionalName(dimensionId))
            .toggleable(true)
            .defaultHidden(false)
            .build();

        Map<String, BlueMapDynamicMarkerState> dynamicMarkers = new HashMap<>();
        String fileName = Packets.getFileNameForDimension(dimensionType);
        if (fileName == null) {
            return new DimensionRuntime(markerSet, dynamicMarkers);
        }

        Path areaFile = WorldFolderManager.getWorldDimensionFile(fileName);
        List<AreaData> areas = FileManager.readAreaData(areaFile);
        for (AreaData area : areas) {
            try {
                BlueMapMarkerFactory.MarkerBuildResult markerBuildResult =
                    BlueMapMarkerFactory.createMarkerDefinition(area, serverWorld, dimensionType, timeMs);
                markerSet.getMarkers().put(markerBuildResult.markerId(), markerBuildResult.marker());
                if (markerBuildResult.dynamicState() != null) {
                    dynamicMarkers.put(markerBuildResult.markerId(), markerBuildResult.dynamicState());
                }
            } catch (Exception e) {
                Areashint.LOGGER.warn("BlueMap 跳过无效区域 '{}'，维度 {}。", area != null ? area.getName() : "unknown", dimensionType, e);
            }
        }

        return new DimensionRuntime(markerSet, dynamicMarkers);
    }

    private void updateDynamicMarkers(Map<String, BlueMapDynamicMarkerState> dynamicMarkers, long timeMs) {
        if (dynamicMarkers == null || dynamicMarkers.isEmpty()) {
            return;
        }

        for (BlueMapDynamicMarkerState state : dynamicMarkers.values()) {
            long bucketMs = BlueMapFlashColorEngine.getBucketMs(state.getFlashMode());
            long phaseBucket = BlueMapFlashColorEngine.resolvePhaseBucket(
                state.getFlashMode(),
                timeMs,
                state.getPhaseOffsetMs(),
                bucketMs
            );
            if (phaseBucket == state.getLastPhaseBucket()) {
                continue;
            }

            int rgb = BlueMapFlashColorEngine.resolveShiftedRgb(
                state.getFlashMode(),
                timeMs,
                state.getPhaseOffsetMs()
            );
            if (rgb != state.getLastAppliedColor()) {
                state.getMarker().setLineWidth(state.getLineWidth());
                state.getMarker().setLineColor(resolveFlashOutlineColor(rgb));
                state.getMarker().setFillColor(BlueMapFlashColorEngine.toColor(rgb, state.getFillAlpha()));
                state.setLastAppliedColor(rgb);
            }
            state.setLastPhaseBucket(phaseBucket);
        }
    }

    private de.bluecolored.bluemap.api.math.Color resolveFlashOutlineColor(int fillRgb) {
        int red = (fillRgb >> 16) & 0xFF;
        int green = (fillRgb >> 8) & 0xFF;
        int blue = fillRgb & 0xFF;
        double luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255.0;

        if (luminance > 0.6) {
            return new de.bluecolored.bluemap.api.math.Color(
                (int) (red * 0.35),
                (int) (green * 0.35),
                (int) (blue * 0.35)
            );
        }

        return new de.bluecolored.bluemap.api.math.Color(
            Math.min(255, (int) (red * 0.45 + 255 * 0.55)),
            Math.min(255, (int) (green * 0.45 + 255 * 0.55)),
            Math.min(255, (int) (blue * 0.45 + 255 * 0.55))
        );
    }

    private ServerWorld resolveServerWorld(String dimensionType) {
        if (server == null || dimensionType == null) {
            return null;
        }

        return switch (dimensionType) {
            case Packets.DIMENSION_OVERWORLD -> server.getWorld(World.OVERWORLD);
            case Packets.DIMENSION_NETHER -> server.getWorld(World.NETHER);
            case Packets.DIMENSION_END -> server.getWorld(World.END);
            default -> null;
        };
    }

    private void clearAllMarkerSets() {
        dimensionRuntimes.clear();
        if (api == null) {
            return;
        }

        for (BlueMapMap map : api.getMaps()) {
            map.getMarkerSets().remove(MARKER_SET_ID);
        }
    }

    private record DimensionRuntime(MarkerSet markerSet, Map<String, BlueMapDynamicMarkerState> dynamicMarkers) {
    }
}
