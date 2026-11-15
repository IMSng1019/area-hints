package areahint.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// 正式域名同步按维度组包；完整性验证通过前不能覆盖客户端正式数据。
public final class AreaDataChunkAssembler {
    private static final Set<String> DIMENSIONS = Set.of("overworld", "the_nether", "the_end");
    private final Map<String, Transfer> transfers = new HashMap<>();

    // 一个维度只保留最新传输，过期包、冲突重复包和未从首片开始的传输都不进入正式数据。
    public synchronized String accept(String dimension, String id, int index, int count, String part, long now) {
        expire(now);
        if (!DIMENSIONS.contains(dimension) || id == null || id.isBlank() || id.length() > 128
                || count < 1 || count > 1024 || index < 0 || index >= count || part == null || part.length() > 24_000) return null;
        Transfer transfer = transfers.get(dimension);
        if (transfer == null || !transfer.id.equals(id)) {
            if (index != 0) return null;
            transfer = new Transfer(id, count, now);
            transfers.put(dimension, transfer);
        }
        if (transfer.parts.length != count || transfer.parts[index] != null && !transfer.parts[index].equals(part)) {
            transfers.remove(dimension);
            return null;
        }
        if (transfer.parts[index] == null) {
            transfer.parts[index] = part;
            transfer.length += part.length();
            transfer.received++;
        }
        if (transfer.length > 8_000_000) {
            transfers.remove(dimension);
            return null;
        }
        if (transfer.received != count) return null;
        transfers.remove(dimension);
        return String.join("", transfer.parts);
    }

    public synchronized void expire(long now) { transfers.values().removeIf(value -> now - value.started > 30_000L); }
    public synchronized void discard(String dimension) { transfers.remove(dimension); }
    public synchronized void clear() { transfers.clear(); }

    private static final class Transfer {
        private final String id;
        private final String[] parts;
        private final long started;
        private int length;
        private int received;
        private Transfer(String id, int count, long started) { this.id = id; this.parts = new String[count]; this.started = started; }
    }
}
