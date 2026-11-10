package areahint.geometry;

import areahint.data.AreaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// 域名加载时构建只读包围盒树；发布后不修改所含域名，数据变化时由调用方整体重建索引。
public final class AreaSpatialIndex {
    // 小集合直接遍历叶片，避免为少量域名继续划分树节点。
    private static final int LEAF_CAPACITY = 8;
    // 同一线程的各个索引复用候选缓冲，查询结束清空引用，不把旧维度的数据留在线程中。
    private static final ThreadLocal<List<Entry>> QUERY_BUFFER = ThreadLocal.withInitial(ArrayList::new);

    private final Entry[] entries;
    private final Entry[] fallbackEntries;
    private final Node root;

    // 每次出现的域名单独记录序号，重名或同一对象重复出现时仍保留文件中的稳定选择顺序。
    public AreaSpatialIndex(List<AreaData> areas) {
        List<Entry> indexed = new ArrayList<>();
        List<Entry> fallback = new ArrayList<>();
        if (areas != null) {
            for (int ordinal = 0; ordinal < areas.size(); ordinal++) {
                AreaData area = areas.get(ordinal);
                if (area == null) {
                    continue;
                }
                if (!area.isCacheComputed()) {
                    area.computeCache();
                }
                Entry entry = new Entry(area, ordinal);
                // 非有限边界只在独立集合中按原规则判断，避免NaN污染父节点边界而漏掉正常域名。
                if (entry.hasFiniteBounds()) {
                    indexed.add(entry);
                } else {
                    fallback.add(entry);
                }
            }
        }
        entries = indexed.toArray(Entry[]::new);
        fallbackEntries = fallback.toArray(Entry[]::new);
        root = entries.length == 0 ? null : build(0, entries.length);
    }

    // 清空调用方独享的输出列表，仅按距离排序通过等级、高度和AABB筛选的候选，再由调用方判断多边形。
    public void collectCandidates(double x, double y, double z, int level, List<AreaData> output) {
        output.clear();
        List<Entry> candidates = QUERY_BUFFER.get();
        candidates.clear();
        try {
            collect(root, x, y, z, level, candidates);
            for (Entry entry : fallbackEntries) {
                if (entry.matches(x, y, z, level)) {
                    candidates.add(entry);
                }
            }
            // 树的空间遍历顺序不参与命中优先级，距离相同仍按原始序号取先出现的域名。
            candidates.sort((first, second) -> {
                int distanceOrder = Double.compare(first.area.distanceSqToCenter(x, z),
                        second.area.distanceSqToCenter(x, z));
                return distanceOrder != 0 ? distanceOrder : Integer.compare(first.ordinal, second.ordinal);
            });
            for (Entry entry : candidates) {
                output.add(entry.area);
            }
        } finally {
            // 只保留缓冲容量，不让长寿命检测线程继续引用已经卸载的域名快照。
            candidates.clear();
        }
    }

    // 按较长方向的边界中心划分平衡子树，每个域名只占一个叶片位置，超大域名不会复制到空间格子中。
    private Node build(int start, int end) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (int i = start; i < end; i++) {
            Entry entry = entries[i];
            minX = Math.min(minX, entry.minX);
            maxX = Math.max(maxX, entry.maxX);
            minZ = Math.min(minZ, entry.minZ);
            maxZ = Math.max(maxZ, entry.maxZ);
        }
        if (end - start <= LEAF_CAPACITY) {
            return new Node(minX, maxX, minZ, maxZ, start, end, null, null);
        }
        boolean splitX = maxX - minX >= maxZ - minZ;
        // 先各除以二再相加，避免极大但有限的同号坐标在计算中点时溢出。
        Arrays.sort(entries, start, end, Comparator.comparingDouble(entry -> splitX
                ? entry.minX / 2.0 + entry.maxX / 2.0 : entry.minZ / 2.0 + entry.maxZ / 2.0));
        int middle = start + (end - start) / 2;
        return new Node(minX, maxX, minZ, maxZ, start, end, build(start, middle), build(middle, end));
    }

    // 只访问包围盒包含查询点的分支，精确射线判断仍由原检测器执行。
    private void collect(Node node, double x, double y, double z, int level, List<Entry> candidates) {
        if (node == null || !node.contains(x, z)) {
            return;
        }
        if (node.left == null) {
            for (int i = node.start; i < node.end; i++) {
                Entry entry = entries[i];
                if (entry.matches(x, y, z, level)) {
                    candidates.add(entry);
                }
            }
            return;
        }
        collect(node.left, x, y, z, level, candidates);
        collect(node.right, x, y, z, level, candidates);
    }

    // 条目保存构建时的AABB数值及出现序号，查询不重新扫描顶点或分配域名副本。
    private static final class Entry {
        private final AreaData area;
        private final int ordinal;
        private final double minX;
        private final double maxX;
        private final double minZ;
        private final double maxZ;

        private Entry(AreaData area, int ordinal) {
            this.area = area;
            this.ordinal = ordinal;
            minX = area.getCachedMinX();
            maxX = area.getCachedMaxX();
            minZ = area.getCachedMinZ();
            maxZ = area.getCachedMaxZ();
        }

        private boolean hasFiniteBounds() {
            return Double.isFinite(minX) && Double.isFinite(maxX)
                    && Double.isFinite(minZ) && Double.isFinite(maxZ);
        }

        private boolean matches(double x, double y, double z, int level) {
            // 比较符与原缓存AABB保持一致，边界包含、NaN不命中以及无限坐标的处理都不改变。
            return area.getLevel() == level && x >= minX && x <= maxX && z >= minZ && z <= maxZ
                    && (area.getAltitude() == null || area.getAltitude().isInRange(y));
        }
    }

    // 节点只引用构建完成后的只读区间和子节点，多线程查询不在节点内保存临时状态。
    private static final class Node {
        private final double minX;
        private final double maxX;
        private final double minZ;
        private final double maxZ;
        private final int start;
        private final int end;
        private final Node left;
        private final Node right;

        private Node(double minX, double maxX, double minZ, double maxZ, int start, int end,
                     Node left, Node right) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.start = start;
            this.end = end;
            this.left = left;
            this.right = right;
        }

        private boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }
}
