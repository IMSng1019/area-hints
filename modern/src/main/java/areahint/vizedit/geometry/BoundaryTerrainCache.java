package areahint.vizedit.geometry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

// 只缓存不可变的水陆分类，不保留世界区块或第三方地图对象；所有调用均来自客户端主线程。
public final class BoundaryTerrainCache {
    private static final int MAX_TILES = 256;
    private static final int READS_PER_WINDOW = 8;
    private static final long READ_WINDOW = 16_000_000L;
    private static final long UNKNOWN_TTL = 250_000_000L;
    private static final long KNOWN_TTL = 5_000_000_000L;
    private final Reader reader;
    private final LongSupplier clock;
    private final LongSupplier budgetEpoch;
    private final Map<Long, Entry> tiles = new LinkedHashMap<>(32, 0.75f, true);
    private long lastEpoch;
    private int reads;

    // 每个瓦片按 x * 16 + z 存放 256 列，0/1/2 分别代表未知、陆地、水面。
    public interface Reader { byte[] read(int chunkX, int chunkZ); }
    public BoundaryTerrainCache(Reader reader) { this(reader, System::nanoTime); }
    public BoundaryTerrainCache(Reader reader, LongSupplier clock) {
        this(reader, clock, () -> clock.getAsLong() / READ_WINDOW);
    }

    public BoundaryTerrainCache(Reader reader, LongSupplier clock, LongSupplier budgetEpoch) {
        this.reader = reader;
        this.clock = clock;
        this.budgetEpoch = budgetEpoch;
        lastEpoch = budgetEpoch.getAsLong();
    }

    public int sample(int x, int z) {
        long now = clock.getAsLong();
        long key = ((long) (x >> 4) << 32) ^ ((z >> 4) & 0xffffffffL);
        Entry entry = tiles.get(key);
        if (entry == null || now - entry.created >= entry.ttl) {
            long epoch = budgetEpoch.getAsLong();
            if (epoch != lastEpoch) { lastEpoch = epoch; reads = 0; }
            if (reads >= READS_PER_WINDOW) return 0;
            reads++;
            byte[] source = reader.read(x >> 4, z >> 4);
            byte[] copy = source != null && source.length == 256 ? source.clone() : new byte[256];
            boolean unknown = false;
            for (int i = 0; i < copy.length; i++) {
                if (copy[i] != 1 && copy[i] != 2) { copy[i] = 0; unknown = true; }
            }
            entry = new Entry(copy, now, unknown ? UNKNOWN_TTL : KNOWN_TTL);
            tiles.put(key, entry);
            if (tiles.size() > MAX_TILES) tiles.remove(tiles.keySet().iterator().next());
        }
        return entry.data[(x & 15) * 16 + (z & 15)];
    }

    private record Entry(byte[] data, long created, long ttl) { }
}
