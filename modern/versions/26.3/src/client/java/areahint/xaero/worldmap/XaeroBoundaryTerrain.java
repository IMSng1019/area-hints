package areahint.xaero.worldmap;

import areahint.vizedit.BoundaryTerrainSource;
import areahint.vizedit.geometry.BoundaryBrush;
import net.minecraft.client.Minecraft;
import xaero.map.WorldMapSession;
import xaero.map.region.MapBlock;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTile;
import xaero.map.region.state.UnknownBlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// Xaero 源数据与版本 API 仅在此桥接内访问，编辑器拿到的都是脱离地图对象的水陆快照。
final class XaeroBoundaryTerrain {
    private static final int SURFACE_LAYER = Integer.MAX_VALUE;

    private XaeroBoundaryTerrain() { }

    static BoundaryTerrainSource.MapSource open(String dimension) {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || !session.isUsable()) return null;
        var processor = session.getMapProcessor();
        var mapWorld = processor.getMapWorld();
        if (mapWorld == null || processor.isWaitingForWorldUpdate()) return null;
        var mapDimension = mapWorld.getCurrentDimension();
        if (mapDimension == null || !Objects.equals(dimension, mapDimension.getDimId().identifier().toString())) return null;
        String multiworld = mapDimension.getCurrentMultiworld();
        var playerMap = mapDimension.getPlayerMapKey();
        // 手动浏览其他已记录世界时，不能把该世界岸线与当前域名或当前已加载区块拼接。
        if (playerMap == null || !playerMap.equals(mapDimension.getSelectedMapKeyUnsynced())
                || !Objects.equals(multiworld, playerMap.getMw())) return null;
        return new BoundaryTerrainSource.MapSource() {
            private final Map<Long, Long> requests = new LinkedHashMap<>();

            @Override public boolean valid() {
                return WorldMapSession.getCurrentSession() == session && session.isUsable()
                    && session.getMapProcessor() == processor && processor.getMapWorld() == mapWorld
                    && mapWorld.getCurrentDimension() == mapDimension
                    && Objects.equals(multiworld, mapDimension.getCurrentMultiworld())
                    && (mapDimension.getFutureMultiworldUnsynced() == null
                        || Objects.equals(multiworld, mapDimension.getFutureMultiworldUnsynced()))
                    && processor.getWorld() == Minecraft.getInstance().level
                    && !processor.isWaitingForWorldUpdate();
            }

            @Override public byte[] read(int chunkX, int chunkZ) {
                if (!valid() || !processor.getMapSaveLoad().isRegionDetectionComplete()) return null;
                int regionX = chunkX >> 5, regionZ = chunkZ >> 5;
                MapRegion region = processor.getLeafMapRegion(SURFACE_LAYER, regionX, regionZ, false);
                // 只创建已有地图文件的区域句柄；高亮或未探索空白不得触发地形生成。
                if (region == null && processor.regionDetectionExists(SURFACE_LAYER, regionX, regionZ))
                    region = processor.getLeafMapRegion(SURFACE_LAYER, regionX, regionZ, true);
                if (region == null) return null;
                boolean needsLoad;
                // 与 Xaero 写入线程相同的顺序：先暂停写入互斥锁，再读区域加载状态；不在此锁内做磁盘读取。
                synchronized (region.writerThreadPauseSync) {
                    synchronized (region) {
                        if (region.getLoadState() == 1) return null;
                        MapTile tile = processor.getMapTile(SURFACE_LAYER, chunkX, chunkZ);
                        if (tile != null && tile.isLoaded() && tile.getWrittenCaveStart() == SURFACE_LAYER)
                            return copy(tile);
                        // 已完成加载却缺失瓦片意味着没有探索数据，不能不断重读整片区域。
                        needsLoad = (region.getLoadState() == 0 || region.getLoadState() == 4)
                            && region.canRequestReload_unsynced();
                    }
                }
                if (needsLoad && processor.getMapSaveLoad().getSizeOfToLoad() < 8) {
                    long key = ((long) regionX << 32) ^ (regionZ & 0xffffffffL);
                    long now = System.nanoTime();
                    Long previous = requests.get(key);
                    if (previous == null || now - previous >= 1_000_000_000L) {
                        requests.put(key, now);
                        if (requests.size() > 16) requests.remove(requests.keySet().iterator().next());
                        // 使用原有后台源数据队列，false 避免每次鼠标采样都把区域插到队首。
                        processor.getMapSaveLoad().requestLoad(region, "Areas Hint boundary brush", false);
                    }
                }
                return null;
            }
        };
    }

    private static byte[] copy(MapTile tile) {
        byte[] result = new byte[256];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
            result[x * 16 + z] = (byte) classify(tile.getBlock(x, z));
        return result;
    }

    private static int classify(MapBlock block) {
        if (block == null || block.getState() instanceof UnknownBlockState) return BoundaryBrush.UNKNOWN;
        int base = BoundaryTerrainSource.classify(block.getState());
        if (base == BoundaryBrush.UNKNOWN) return BoundaryBrush.UNKNOWN;
        boolean water = base == BoundaryBrush.WATER;
        var overlays = block.getOverlays();
        if (overlays != null) for (var overlay : overlays) {
            if (overlay == null || overlay.getState() instanceof UnknownBlockState) return BoundaryBrush.UNKNOWN;
            if (BoundaryTerrainSource.thinCover(overlay.getState())) continue;
            int type = BoundaryTerrainSource.classify(overlay.getState());
            // 树冠、玻璃或其他覆盖层可能遮住岸线，保守标记未知，不凭水下底块臆造可见岸线。
            if (type != BoundaryBrush.WATER) return BoundaryBrush.UNKNOWN;
            water = true;
        }
        return water ? BoundaryBrush.WATER : BoundaryBrush.LAND;
    }
}
