package areahint.vizedit;

import areahint.AreashintClient;
import areahint.vizedit.geometry.BoundaryBrush;
import areahint.vizedit.geometry.BoundaryTerrainCache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Objects;

// 编辑器只接收水陆快照；可选地图类型留在各版本桥接内，未装地图仍可读取已加载地表。
public final class BoundaryTerrainSource {
    private static Provider provider;
    private static boolean reportedFailure;
    private static long samplingTick;

    // 主线程被一次笔画计算占用时 tick 不会前进，慢读取也不能重置本轮预算。
    static { ClientTickEvents.END_CLIENT_TICK.register(client -> samplingTick++); }

    private BoundaryTerrainSource() { }

    public interface Provider { MapSource open(String dimension); }
    public interface MapSource {
        boolean valid();
        byte[] read(int chunkX, int chunkZ);
    }

    public static void install(Provider value) { provider = value; reportedFailure = false; }

    public static BoundaryBrush.Terrain open(String dimension) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        var connection = client.getConnection();
        if (world == null || !Objects.equals(dimension, world.dimension().identifier().toString()))
            return (x, z) -> BoundaryBrush.UNKNOWN;
        return new BoundaryBrush.Terrain() {
            private MapSource remote;
            private BoundaryTerrainCache cache = newCache();
            private long nextOpen = System.nanoTime();
            private boolean invalid;

            @Override public int sample(int x, int z) {
                if (invalid || client.level != world || client.getConnection() != connection) {
                    invalid = true;
                    return BoundaryBrush.UNKNOWN;
                }
                if (Math.abs((long) x) > 30_000_000L || Math.abs((long) z) > 30_000_000L)
                    return BoundaryBrush.UNKNOWN;
                try {
                    // 地图多世界选择变化后立即清掉旧快照，不能混用同坐标的另一个世界。
                    if (remote != null && !remote.valid()) { remote = null; cache = newCache(); }
                    long now = System.nanoTime();
                    if (remote == null && provider != null && now - nextOpen >= 0) {
                        nextOpen = now + 250_000_000L;
                        remote = provider.open(dimension);
                    }
                    return cache.sample(x, z);
                } catch (RuntimeException | LinkageError failure) {
                    // 兼容接口失效时保留域名几何约束，避免第三方模组错误关闭整个编辑器。
                    remote = null;
                    cache = newCache();
                    if (!reportedFailure) {
                        reportedFailure = true;
                        AreashintClient.LOGGER.warn("边界笔地形读取暂不可用", failure);
                    }
                    return BoundaryBrush.UNKNOWN;
                }
            }

            private byte[] read(int chunkX, int chunkZ) {
                byte[] local = loadedSurface(world, chunkX, chunkZ);
                return local != null ? local : remote != null && remote.valid() ? remote.read(chunkX, chunkZ) : null;
            }

            private BoundaryTerrainCache newCache() {
                return new BoundaryTerrainCache(this::read, System::nanoTime, () -> samplingTick);
            }
        };
    }

    private static byte[] loadedSurface(ClientLevel world, int chunkX, int chunkZ) {
        // 先查客户端已有区块；后续高度与方块读取均在同一主线程，绝不请求生成或加载世界区块。
        if (!world.hasChunk(chunkX, chunkZ)) return null;
        byte[] result = new byte[256];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int worldX = (chunkX << 4) + x, worldZ = (chunkZ << 4) + z;
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
            if (y < world.getMinY()) continue;
            pos.set(worldX, y, worldZ);
            BlockState state = world.getBlockState(pos);
            // 雪层与睡莲不改变下方水面的类别，但遇到树冠、实体桥面就停止，不向地下追水。
            if ((state.is(Blocks.SNOW) || state.is(Blocks.LILY_PAD)) && y > world.getMinY()) {
                pos.set(worldX, y - 1, worldZ);
                state = world.getBlockState(pos);
            }
            result[x * 16 + z] = (byte) classify(state);
        }
        return result;
    }

    public static int classify(BlockState state) {
        if (state == null || state.isAir()) return BoundaryBrush.UNKNOWN;
        // 普通冰与霜冰属于冻结水面，蓝冰和浮冰山本体不一概当作河道。
        if (state.is(Blocks.WATER) || state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)
                || state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT))
            return BoundaryBrush.WATER;
        if (state.is(BlockTags.LEAVES) || state.is(Blocks.SNOW) || state.is(Blocks.LILY_PAD)
                || !state.getFluidState().isEmpty()) return BoundaryBrush.UNKNOWN;
        return BoundaryBrush.LAND;
    }

    // Xaero 已经保存薄覆盖层下的地表状态，因此雪层与睡莲自身不参与水陆判断。
    public static boolean thinCover(BlockState state) {
        return state != null && (state.is(Blocks.SNOW) || state.is(Blocks.LILY_PAD));
    }
}
