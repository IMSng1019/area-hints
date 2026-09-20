package areahint.teleport;

import areahint.data.AreaData;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SafeLandingFinder {
    private static final int UDP_MAX_ATTEMPTS = 4096;
    // 连续失败上限：区域里长时间找不到可行柱时提前收手，避免主线程一直空转
    // 取 2048 是为了不改变原来的成功率语义：原实现允许 4096 次尝试，512 次提前退出在可行柱稀疏时会把成功率压到六成左右
    private static final int UDP_MAX_CONSECUTIVE_FAILURES = 2048;

    public Optional<Vec3d> findCenterLanding(ServerWorld world, AreaData area) {
        if (world == null || area == null || !area.isValid()) {
            return Optional.empty();
        }
        Bounds bounds = Bounds.from(area);
        if (bounds == null) {
            return Optional.empty();
        }

        int centerX = (int) Math.floor(centerX(area));
        int centerZ = (int) Math.floor(centerZ(area));
        Optional<Vec3d> center = scanColumn(world, area, centerX, centerZ);
        if (center.isPresent()) {
            return center;
        }

        int maxRadius = Math.max(
                Math.max(Math.abs(centerX - bounds.minX), Math.abs(centerX - bounds.maxX)),
                Math.max(Math.abs(centerZ - bounds.minZ), Math.abs(centerZ - bounds.maxZ))
        );

        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                Optional<Vec3d> north = scanIfWithinBounds(world, area, bounds, x, centerZ - radius);
                if (north.isPresent()) return north;
                Optional<Vec3d> south = scanIfWithinBounds(world, area, bounds, x, centerZ + radius);
                if (south.isPresent()) return south;
            }
            for (int z = centerZ - radius + 1; z <= centerZ + radius - 1; z++) {
                Optional<Vec3d> west = scanIfWithinBounds(world, area, bounds, centerX - radius, z);
                if (west.isPresent()) return west;
                Optional<Vec3d> east = scanIfWithinBounds(world, area, bounds, centerX + radius, z);
                if (east.isPresent()) return east;
            }
        }

        return Optional.empty();
    }

    public Optional<Vec3d> findRandomLanding(ServerWorld world, AreaData area) {
        if (world == null || area == null || !area.isValid()) {
            return Optional.empty();
        }
        Bounds bounds = Bounds.from(area);
        if (bounds == null) {
            return Optional.empty();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int consecutiveFailures = 0;
        for (int attempt = 0; attempt < UDP_MAX_ATTEMPTS; attempt++) {
            int x = random.nextInt(bounds.minX, bounds.maxX + 1);
            int z = random.nextInt(bounds.minZ, bounds.maxZ + 1);
            Optional<Vec3d> landing = scanColumn(world, area, x, z);
            if (landing.isPresent()) {
                return landing;
            }
            // 找到落点会直接返回，所以这里统计的就是连续失败次数
            consecutiveFailures++;
            if (consecutiveFailures >= UDP_MAX_CONSECUTIVE_FAILURES) {
                break;
            }
        }

        return Optional.empty();
    }

    private Optional<Vec3d> scanIfWithinBounds(ServerWorld world, AreaData area, Bounds bounds, int x, int z) {
        if (!bounds.contains(x, z)) {
            return Optional.empty();
        }
        return scanColumn(world, area, x, z);
    }

    private Optional<Vec3d> scanColumn(ServerWorld world, AreaData area, int x, int z) {
        double pointX = x + 0.5D;
        double pointZ = z + 0.5D;
        if (!ServerAreaGeometry.isPointInAABB(pointX, pointZ, area.getSecondVertices())
                || !ServerAreaGeometry.isPointInPolygon(pointX, pointZ, area.getVertices())) {
            return Optional.empty();
        }

        int minY = getMinSearchY(world, area);
        int maxY = getMaxSearchY(world, area, minY, x, z);
        for (int y = maxY; y >= minY; y--) {
            if (ServerAreaGeometry.isWithinAltitude(area, y) && isSafeLanding(world, x, y, z)) {
                return Optional.of(new Vec3d(pointX, y, pointZ));
            }
        }
        return Optional.empty();
    }

    private int getMinSearchY(ServerWorld world, AreaData area) {
        int min = world.getBottomY() + 1;
        if (area.getAltitude() != null && area.getAltitude().getMin() != null) {
            min = Math.max(min, (int) Math.ceil(area.getAltitude().getMin()));
        }
        return min;
    }

    private int getMaxSearchY(ServerWorld world, AreaData area, int minY, int x, int z) {
        int max = world.getTopY() - 2;
        if (area.getAltitude() != null && area.getAltitude().getMax() != null) {
            max = Math.min(max, (int) Math.floor(area.getAltitude().getMax()));
        }
        if (max < minY) {
            // 高度区间为空，保持原样返回，连区块都不必加载
            return max;
        }
        // 先确保该柱所在区块已加载，否则高度图取不到真实地表
        world.getChunk(x >> 4, z >> 4);
        // 用世界高度图收紧上界：落点地面必须是非空气方块，所以落点不会高于该柱最高的非空气方块
        // 用 WORLD_SURFACE 而非 MOTION_BLOCKING：后者按 blocksMovement 判定，会漏掉雪层、紫颂花这类非固体实心块的顶部落点
        int surfaceTop = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        if (surfaceTop != 0) {
            max = Math.min(max, surfaceTop);
        }
        return max;
    }

    private boolean isSafeLanding(ServerWorld world, int x, int y, int z) {
        BlockPos feetPos = new BlockPos(x, y, z);
        BlockPos headPos = feetPos.up();
        BlockPos groundPos = feetPos.down();

        BlockState ground = world.getBlockState(groundPos);
        BlockState feet = world.getBlockState(feetPos);
        BlockState head = world.getBlockState(headPos);

        return isSafeGround(world, groundPos, ground)
                && isPassable(world, feetPos, feet)
                && isPassable(world, headPos, head)
                && !isDangerous(feet)
                && !isDangerous(head);
    }

    private boolean isSafeGround(ServerWorld world, BlockPos pos, BlockState state) {
        return state.isSideSolidFullSquare(world, pos, Direction.UP) && !isDangerous(state);
    }

    private boolean isPassable(ServerWorld world, BlockPos pos, BlockState state) {
        return state.getCollisionShape(world, pos).isEmpty() && state.getFluidState().isEmpty() && !isDangerous(state);
    }

    private boolean isDangerous(BlockState state) {
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.CAMPFIRE)
                || state.isOf(Blocks.SOUL_CAMPFIRE)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.WITHER_ROSE)
                || state.isOf(Blocks.POWDER_SNOW)
                || !state.getFluidState().isEmpty();
    }

    private double centerX(AreaData area) {
        return area.getVertices().stream().mapToDouble(AreaData.Vertex::getX).average().orElse(0.0D);
    }

    private double centerZ(AreaData area) {
        return area.getVertices().stream().mapToDouble(AreaData.Vertex::getZ).average().orElse(0.0D);
    }

    private static class Bounds {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;

        private Bounds(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        private boolean contains(int x, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        private static Bounds from(AreaData area) {
            if (area == null || area.getSecondVertices() == null || area.getSecondVertices().size() != 4) {
                return null;
            }

            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (AreaData.Vertex vertex : area.getSecondVertices()) {
                minX = Math.min(minX, vertex.getX());
                maxX = Math.max(maxX, vertex.getX());
                minZ = Math.min(minZ, vertex.getZ());
                maxZ = Math.max(maxZ, vertex.getZ());
            }
            return new Bounds((int) Math.floor(minX), (int) Math.ceil(maxX), (int) Math.floor(minZ), (int) Math.ceil(maxZ));
        }
    }
}
