package areahint.vizedit.geometry;

import areahint.data.AreaData.Vertex;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 边界笔只约束新涂面积；地形接口、岸线记忆和几何计算均不依赖游戏或地图实现。
public final class BoundaryBrush {
    public static final int UNKNOWN = 0, LAND = 1, WATER = 2;
    private static final int TILE_SIZE = 16;
    // 每次采样预算只分摊地形读取，后续采样继续补齐，不限制累计笔画、岸线或偏移数量。
    private static final int MAX_NEW_SAMPLES = 32768;
    private static final double CROSSING_THRESHOLD = 1, EPSILON = 0.0000001;

    private BoundaryBrush() { }

    @FunctionalInterface
    public interface Terrain { int sample(int x, int z); }

    // 约束和偏移随草稿复制，返回给界面的快照不共享后续笔画可变数据。
    public static final class State {
        private final Map<Edge, Double> memories = new LinkedHashMap<>();
        private Area hardForbidden = new Area();
        private Area naturalForbidden = new Area();

        public State copy() {
            State result = new State();
            result.memories.putAll(memories);
            result.hardForbidden = new Area(hardForbidden);
            result.naturalForbidden = new Area(naturalForbidden);
            return result;
        }

        public boolean same(State other) {
            return other != null && memories.equals(other.memories)
                && hardForbidden.equals(other.hardForbidden) && naturalForbidden.equals(other.naturalForbidden);
        }

        public Area forbidden() {
            Area result = new Area(hardForbidden);
            result.add(naturalForbidden);
            return result;
        }

        // 父引用或等级变化只使旧域名硬限制失效，自然岸线偏移仍随同一份草稿保留。
        public State withoutHardConstraints() {
            State result = copy();
            result.hardForbidden.reset();
            return result;
        }
    }

    public record Result(Area shape, State state, List<double[]> guides, double offset, boolean terrainAvailable) { }

    public static final class Stroke {
        private final Area existing, parent, peers;
        private final State state;
        private final Terrain terrain;
        private final double width;
        private final Map<Long, Integer> known = new HashMap<>();
        private final Set<Long> completeTiles = new HashSet<>();
        private final Map<Edge, Edge> edges = new LinkedHashMap<>();
        private final Map<Long, List<Edge>> junctions = new HashMap<>();
        private final Set<Long> missing = new HashSet<>();
        private Area shape;
        private Area rawPaint = new Area();
        private Vertex previous;
        private Edge anchor;
        private double offset;
        private int samples;
        private boolean terrainAvailable;
        private List<double[]> guides = List.of();

        public Stroke(Area existing, Area parentOrNull, List<Area> peers, State initial, Terrain terrain, double width) {
            if (!Double.isFinite(width) || width < 1 || width > VizEditGeometry.MAX_BRUSH_WIDTH)
                throw VizEditGeometry.error("geometry_limit");
            this.existing = VizEditGeometry.copy(existing);
            this.shape = new Area(this.existing);
            this.parent = parentOrNull == null ? null : new Area(parentOrNull);
            this.peers = new Area();
            if (peers != null) for (Area peer : peers) if (peer != null) this.peers.add(peer);
            this.state = initial == null ? new State() : initial.copy();
            this.terrain = terrain;
            this.width = width;
            // 普通笔或旧域名已占用的部分属于既有意图，不能被新笔的保存限制追溯删除。
            this.state.hardForbidden.subtract(this.existing);
            this.state.naturalForbidden.subtract(this.existing);
        }

        public Result paint(Vertex point) {
            VizEditGeometry.checkCoordinate(point);
            samples = 0;
            missing.clear();
            // 稀疏鼠标事件仍沿途检查岸线，避免快速拖动只在终点识别自然限制。
            Vertex from = previous;
            int steps = from == null || terrain == null ? 1 : Math.max(1, (int) Math.ceil(Math.hypot(point.getX() - from.getX(), point.getZ() - from.getZ()) / 8));
            for (int step = 1; step <= steps; step++) {
                double fraction = (double) step / steps;
                Vertex next = from == null ? point : new Vertex(from.getX() + (point.getX() - from.getX()) * fraction,
                    from.getZ() + (point.getZ() - from.getZ()) * fraction);
                apply(next);
                previous = next;
            }
            return new Result(new Area(shape), state.copy(), copyGuides(guides), offset, terrainAvailable);
        }

        private void apply(Vertex point) {
            Area paint = VizEditGeometry.stroke(previous == null ? List.of(point) : List.of(previous, point), width);
            rawPaint = VizEditGeometry.union(rawPaint, paint);
            Rectangle2D box = VizEditGeometry.stroke(List.of(point), width).getBounds2D();
            Area scope = rectangle(box.getMinX() - 2, box.getMinY() - 2, box.getMaxX() + 2, box.getMaxY() + 2);
            if (previous != null) {
                Rectangle2D moving = VizEditGeometry.stroke(List.of(previous, point), width).getBounds2D();
                scope = rectangle(moving.getMinX() - 2, moving.getMinY() - 2, moving.getMaxX() + 2, moving.getMaxY() + 2);
            }
            Area hard = new Area(scope);
            if (parent == null) hard.reset();
            else hard.subtract(parent);
            Area occupied = new Area(peers);
            occupied.intersect(scope);
            hard.add(occupied);
            hard.subtract(existing);
            state.hardForbidden.add(hard);

            List<Edge> local = identify(point);
            Field activeField = null;
            if (!local.isEmpty()) {
                Field field = new Field(merge(local));
                activeField = field;
                double distance = field.at(point.getX(), point.getZ());
                if (Double.isFinite(distance) && distance >= offset + CROSSING_THRESHOLD - EPSILON)
                    offset = Math.floor((distance + EPSILON) * 10) / 10;
                remember(local, point);
                Area natural = forbidden(scope, field, offset);
                natural.subtract(existing);
                // 仅释放本次真实笔迹获准涂到的部分，扫描余量不可放出以前被截掉的原始笔迹。
                Area authorized = new Area(paint);
                authorized.subtract(natural);
                state.naturalForbidden.subtract(authorized);
                state.naturalForbidden.add(natural);
            } else guides = List.of();
            // 只保护落笔前面积；同一笔未知地形补齐后重裁原笔迹，不把临时越界当作旧域名永久保留。
            // 闭合填充也只增加当前笔画面积，完整父域名和同级限制需同时覆盖未被笔尖扫过的内部。
            Area complete = VizEditGeometry.fillNewlyEnclosed(existing, VizEditGeometry.union(existing, rawPaint));
            paint = new Area(complete);
            paint.subtract(existing);
            Area interiorHard = new Area(paint);
            if (parent == null) interiorHard.reset();
            else interiorHard.subtract(parent);
            Area occupiedInterior = new Area(peers);
            occupiedInterior.intersect(paint);
            interiorHard.add(occupiedInterior);
            state.hardForbidden.add(interiorHard);
            paint.subtract(state.hardForbidden);
            paint.subtract(state.naturalForbidden);
            shape = VizEditGeometry.union(existing, paint);
            // 拆分旧斜边再并回可能产生伪洞或伪孤岛；仅对非法多环尝试直接裁剪完整选区，正常结果保持原算法。
            if (!shape.isSingular() && !VizEditGeometry.validate(shape).valid()) {
                Area candidate = clipComplete(complete);
                if (VizEditGeometry.validate(candidate).valid() && keepsExistingArea(candidate)
                        && BoundarySaveGuard.allows(candidate, existing, state.forbidden())) shape = candidate;
            }
            // 新获准面积必须从旧自然限制中清除，否则下一次保存会误判自身越界。
            state.naturalForbidden.subtract(shape);
            if (activeField != null) {
                // 预览取最终累积禁区的真实边缘，旧局部限制或同级域名内部不能画出并不存在的可涂分界。
                Area visibleForbidden = state.forbidden();
                visibleForbidden.intersect(scope);
                guides = guideLines(visibleForbidden, scope, activeField, offset);
            }
        }

        private Area clipComplete(Area complete) {
            if (state.hardForbidden.isEmpty() && state.naturalForbidden.isEmpty()) return complete;
            // 全部原始笔迹被挡住且没有闭合填充时，这是一笔空操作，直接保留旧边的精确坐标。
            if (clipToCurrentLimits(rawPaint).isEmpty()
                    && VizEditGeometry.contours(VizEditGeometry.union(existing, rawPaint)).stream()
                        .noneMatch(ring -> VizEditGeometry.signedArea(ring) > 0)) return new Area(existing);
            Area candidate = clipToCurrentLimits(complete);
            // 最后并回落笔前面积；历史硬禁区由调用方完整核验，不追溯裁剪旧域名，也不按面积忽略真实小洞。
            return VizEditGeometry.union(existing, candidate);
        }

        private Area clipToCurrentLimits(Area source) {
            Area candidate = new Area(source);
            // 直接使用完整父域名和同级域名，避免与已扣除旧域名的硬禁区再次沿同一条小数边求交。
            if (parent != null) candidate.intersect(parent);
            if (!peers.isEmpty()) candidate.subtract(peers);
            if (!state.naturalForbidden.isEmpty()) candidate.subtract(state.naturalForbidden);
            return candidate;
        }

        private boolean keepsExistingArea(Area candidate) {
            Area removed = VizEditGeometry.subtract(existing, candidate);
            // Area.isEmpty可能把往返线段保留为非空；仅用重复点和精确共线清理，不忽略任何非零面积缺失。
            return removed.isEmpty() || VizEditGeometry.contours(removed).stream()
                .allMatch(ring -> VizEditGeometry.simplify(ring).size() < 3);
        }

        private List<Edge> identify(Vertex point) {
            if (terrain == null) { terrainAvailable = false; return List.of(); }
            int radius = Math.min(144, Math.max(16, (int) Math.ceil(width / 2) + 8));
            if (anchor == null) {
                Edge remembered = null;
                double best = Double.POSITIVE_INFINITY, rememberedOffset = 0;
                for (var entry : state.memories.entrySet()) {
                    Edge edge = entry.getKey();
                    double along = edge.along(point.getX(), point.getZ());
                    if (along < -width / 2 - 2 || along > 1 + width / 2 + 2) continue;
                    double distance = Math.abs(edge.signed(point.getX(), point.getZ()) - entry.getValue());
                    if (distance <= radius && distance < best) {
                        best = distance; remembered = edge; rememberedOffset = entry.getValue();
                    }
                }
                // 偏移可能远大于搜索半径，先到记忆对应的原岸补采验证，再恢复同一条限制线。
                if (remembered != null) {
                    discover(remembered.midX(), remembered.midZ(), radius);
                    if (edges.containsKey(remembered)) { anchor = remembered; offset = rememberedOffset; }
                }
            }
            discover(point.getX(), point.getZ(), radius);
            if (anchor != null) discover(anchor.midX(), anchor.midZ(), radius);
            int centerX = (int) Math.floor(point.getX()), centerZ = (int) Math.floor(point.getZ());
            terrainAvailable = cell(centerX, centerZ) != UNKNOWN && cell(centerX - 1, centerZ) != UNKNOWN
                && cell(centerX + 1, centerZ) != UNKNOWN && cell(centerX, centerZ - 1) != UNKNOWN
                && cell(centerX, centerZ + 1) != UNKNOWN;
            if (anchor == null) {
                // 跨笔优先恢复最近实际接触过的限制线，水面上的新草稿不会把基准改成对岸。
                double best = Double.POSITIVE_INFINITY;
                for (var entry : state.memories.entrySet()) {
                    Edge edge = entry.getKey();
                    if (!edges.containsKey(edge)) continue;
                    double along = edge.along(point.getX(), point.getZ());
                    if (along < -width / 2 - 2 || along > 1 + width / 2 + 2) continue;
                    double distance = Math.abs(edge.signed(point.getX(), point.getZ()) - entry.getValue());
                    if (distance <= radius && distance < best) { best = distance; anchor = edge; offset = entry.getValue(); }
                }
                if (anchor == null) {
                    Edge nearest = null, supported = null;
                    double nearestDistance = Double.POSITIVE_INFINITY, supportedDistance = Double.POSITIVE_INFINITY;
                    for (Edge edge : edges.keySet()) {
                        double distance = edge.squaredDistance(point.getX(), point.getZ());
                        if (distance > (double) radius * radius) continue;
                        if (distance < nearestDistance) { nearestDistance = distance; nearest = edge; }
                        if (distance < supportedDistance && supports(edge)) { supportedDistance = distance; supported = edge; }
                    }
                    // 双岸都包含已有面积时最近岸本身也有支持，因此仍然按落笔位置自然决胜。
                    anchor = nearest != null && supports(nearest) ? nearest : supported == null ? nearest : supported;
                }
            }
            if (anchor == null) return List.of();
            terrainAvailable = true;
            int reach = Math.min(512, radius * 2 + (int) Math.min(128, offset) + 16);
            List<Edge> connected = connected(anchor, reach);
            Edge nearest = anchor;
            double best = anchor.squaredDistance(point.getX(), point.getZ());
            for (Edge edge : connected) {
                // 同一笔不直接翻到法向相反的河岸；沿实际相邻岸段的转弯仍可逐步跟随。
                if (edge.nx * anchor.nx + edge.nz * anchor.nz < 0) continue;
                double distance = edge.squaredDistance(point.getX(), point.getZ());
                if (distance < best - EPSILON) { best = distance; nearest = edge; }
            }
            anchor = nearest;
            List<Edge> result = new ArrayList<>();
            for (Edge edge : connected(anchor, reach)) {
                if (edge.nx * anchor.nx + edge.nz * anchor.nz >= 0) result.add(edge);
            }
            return result;
        }

        private boolean supports(Edge edge) {
            // 用真实已有面积与陆地侧半平面相交，窄条、远岸和斜向已有域名都不会落入离散采样间隙。
            Rectangle2D box = existing.getBounds2D();
            double x0 = box.getMinX(), z0 = box.getMinY(), x1 = box.getMaxX(), z1 = box.getMaxY();
            if (edge.nx > 0) x1 = Math.min(x1, edge.x1);
            if (edge.nx < 0) x0 = Math.max(x0, edge.x1);
            if (edge.nz > 0) z1 = Math.min(z1, edge.z1);
            if (edge.nz < 0) z0 = Math.max(z0, edge.z1);
            return x1 > x0 && z1 > z0 && existing.intersects(x0, z0, x1 - x0, z1 - z0);
        }

        private void remember(List<Edge> local, Vertex point) {
            double half = width / 2 + 3;
            for (Edge edge : local) {
                double along = edge.along(point.getX(), point.getZ());
                if (along < -half || along > 1 + half) continue;
                if (edge.squaredDistance(point.getX(), point.getZ()) > Math.pow(half + offset + 2, 2)) continue;
                state.memories.put(edge, Math.max(offset, state.memories.getOrDefault(edge, 0.0)));
            }
        }

        private List<Edge> connected(Edge first, int reach) {
            List<Edge> result = new ArrayList<>();
            Set<Edge> visited = new HashSet<>();
            ArrayDeque<Edge> queue = new ArrayDeque<>();
            Map<Edge, Integer> depth = new HashMap<>();
            queue.add(first); visited.add(first); depth.put(first, 0);
            while (!queue.isEmpty()) {
                Edge current = queue.removeFirst();
                result.add(current);
                int nextDepth = depth.get(current) + 1;
                if (nextDepth > reach) continue;
                for (long endpoint : new long[]{key(current.x1, current.z1), key(current.x2, current.z2)}) {
                    for (Edge next : junctions.getOrDefault(endpoint, List.of())) {
                        if (next.nx * current.nx + next.nz * current.nz < 0 || !visited.add(next)) continue;
                        depth.put(next, nextDepth); queue.addLast(next);
                    }
                }
            }
            return result;
        }

        private void discover(double x, double z, int radius) {
            int centerX = Math.floorDiv((int) Math.floor(x), TILE_SIZE), centerZ = Math.floorDiv((int) Math.floor(z), TILE_SIZE);
            int rings = (radius + TILE_SIZE - 1) / TILE_SIZE;
            // 先取指针附近瓦片，受帧预算限制的数据源不会先把预算耗在远处角落。
            for (int ring = 0; ring <= rings && samples < MAX_NEW_SAMPLES; ring++) {
                for (int dz = -ring; dz <= ring && samples < MAX_NEW_SAMPLES; dz++) {
                    for (int dx = -ring; dx <= ring && samples < MAX_NEW_SAMPLES; dx++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                        tile(centerX + dx, centerZ + dz);
                    }
                }
            }
        }

        private void tile(int tileX, int tileZ) {
            long tileKey = key(tileX, tileZ);
            if (completeTiles.contains(tileKey)) return;
            boolean complete = true;
            int originX = tileX * TILE_SIZE, originZ = tileZ * TILE_SIZE;
            for (int z = originZ; z < originZ + TILE_SIZE; z++) for (int x = originX; x < originX + TILE_SIZE; x++) {
                int here = cell(x, z), right = cell(x + 1, z), below = cell(x, z + 1);
                if (here == UNKNOWN || right == UNKNOWN || below == UNKNOWN) complete = false;
                if (here != UNKNOWN && right != UNKNOWN && here != right) {
                    add(here == LAND ? new Edge(x + 1, z + 1, x + 1, z, 1, 0)
                        : new Edge(x + 1, z, x + 1, z + 1, -1, 0));
                }
                if (here != UNKNOWN && below != UNKNOWN && here != below) {
                    add(here == LAND ? new Edge(x, z + 1, x + 1, z + 1, 0, 1)
                        : new Edge(x + 1, z + 1, x, z + 1, 0, -1));
                }
            }
            if (complete) completeTiles.add(tileKey);
        }

        private int cell(int x, int z) {
            if (Math.abs((long) x) > VizEditGeometry.MAX_COORDINATE || Math.abs((long) z) > VizEditGeometry.MAX_COORDINATE)
                return UNKNOWN;
            long cellKey = key(x, z);
            Integer saved = known.get(cellKey);
            if (saved != null) return saved;
            if (missing.contains(cellKey) || samples >= MAX_NEW_SAMPLES) return UNKNOWN;
            samples++;
            int result = terrain.sample(x, z);
            if (result == LAND || result == WATER) {
                known.put(cellKey, result);
            }
            else { missing.add(cellKey); result = UNKNOWN; }
            return result;
        }

        private void add(Edge edge) {
            if (edges.putIfAbsent(edge, edge) != null) return;
            junctions.computeIfAbsent(key(edge.x1, edge.z1), ignored -> new ArrayList<>()).add(edge);
            junctions.computeIfAbsent(key(edge.x2, edge.z2), ignored -> new ArrayList<>()).add(edge);
        }
    }

    // 水总在有向边右侧；岸线从真实相邻已知水陆方块提取，未知格不参与造边。
    private record Edge(int x1, int z1, int x2, int z2, int nx, int nz) {
        double midX() { return ((double) x1 + x2) / 2; }
        double midZ() { return ((double) z1 + z2) / 2; }
        double along(double x, double z) { return (x - x1) * (x2 - x1) + (z - z1) * (z2 - z1); }
        double signed(double x, double z) { return (x - midX()) * nx + (z - midZ()) * nz; }
        double squaredDistance(double x, double z) {
            double t = Math.max(0, Math.min(1, along(x, z)));
            double dx = x - x1 - t * (x2 - x1), dz = z - z1 - t * (z2 - z1);
            return dx * dx + dz * dz;
        }
    }

    private record Segment(double x1, double z1, double x2, double z2, int nx, int nz) {
        double squaredDistance(double x, double z) {
            double dx = x2 - x1, dz = z2 - z1;
            double t = Math.max(0, Math.min(1, ((x - x1) * dx + (z - z1) * dz) / (dx * dx + dz * dz)));
            double ex = x - x1 - t * dx, ez = z - z1 - t * dz;
            return ex * ex + ez * ez;
        }
    }

    // 同一直线上的方块边先合并，使长直海岸的大笔宽计算不随方块边数量平方增长。
    private static List<Segment> merge(List<Edge> source) {
        Map<String, List<Edge>> groups = new HashMap<>();
        for (Edge edge : source) {
            String group = edge.nx + ":" + edge.nz + ":" + (edge.nx == 0 ? edge.z1 : edge.x1);
            groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(edge);
        }
        List<Segment> result = new ArrayList<>();
        for (List<Edge> group : groups.values()) {
            Edge first = group.get(0);
            boolean vertical = first.nx != 0;
            group.sort(Comparator.comparingInt(e -> vertical ? Math.min(e.z1, e.z2) : Math.min(e.x1, e.x2)));
            int begin = Integer.MIN_VALUE, end = Integer.MIN_VALUE;
            for (Edge edge : group) {
                int low = vertical ? Math.min(edge.z1, edge.z2) : Math.min(edge.x1, edge.x2);
                if (begin != Integer.MIN_VALUE && low > end) {
                    result.add(vertical ? new Segment(first.x1, begin, first.x1, end, first.nx, first.nz)
                        : new Segment(begin, first.z1, end, first.z1, first.nx, first.nz));
                    begin = low;
                } else if (begin == Integer.MIN_VALUE) begin = low;
                end = low + 1;
            }
            result.add(vertical ? new Segment(first.x1, begin, first.x1, end, first.nx, first.nz)
                : new Segment(begin, first.z1, end, first.z1, first.nx, first.nz));
        }
        return result;
    }

    private static final class Field {
        private final Node tree;
        private final Map<Long, int[]> corners = new HashMap<>();
        Field(List<Segment> segments) {
            tree = new Node(new ArrayList<>(segments));
            for (Segment edge : segments) for (long endpoint : new long[]{key((int) edge.x1, (int) edge.z1), key((int) edge.x2, (int) edge.z2)}) {
                int[] corner = corners.computeIfAbsent(endpoint, ignored -> new int[3]);
                corner[0]++; corner[1] += edge.nx; corner[2] += edge.nz;
            }
        }

        double at(double x, double z) {
            Nearest nearest = new Nearest();
            tree.nearest(x, z, nearest);
            if (nearest.edge == null) return Double.NaN;
            Segment edge = nearest.edge;
            double along = edge.nx == 0 ? x : z;
            double from = edge.nx == 0 ? edge.x1 : edge.z1, to = edge.nx == 0 ? edge.x2 : edge.z2;
            // 不把可见岸线末端向未知地图无限延长；无可靠投影的小段退回普通涂抹。
            if (along <= Math.min(from, to) + EPSILON || along >= Math.max(from, to) - EPSILON) {
                boolean first = Math.abs(along - from) <= Math.abs(along - to);
                double cornerX = first ? edge.x1 : edge.x2, cornerZ = first ? edge.z1 : edge.z2;
                int[] corner = corners.get(key((int) cornerX, (int) cornerZ));
                if (corner != null && corner[0] >= 2) {
                    double side = (x - cornerX) * corner[1] + (z - cornerZ) * corner[2];
                    return (side >= 0 ? 1 : -1) * Math.sqrt(nearest.distance);
                }
                if (along < Math.min(from, to) - EPSILON || along > Math.max(from, to) + EPSILON) return Double.NaN;
            }
            return ((x - edge.x1) * edge.nx + (z - edge.z1) * edge.nz >= 0 ? 1 : -1) * Math.sqrt(nearest.distance);
        }
    }

    private static final class Nearest { Segment edge; double distance = Double.POSITIVE_INFINITY; }

    // 小型包围盒树限制复杂岸线的最近边查询成本，叶节点仅包含少量线段。
    private static final class Node {
        final double minX, minZ, maxX, maxZ;
        final Node left, right;
        final List<Segment> leaves;
        Node(List<Segment> segments) {
            double x0 = Double.POSITIVE_INFINITY, z0 = x0, x1 = Double.NEGATIVE_INFINITY, z1 = x1;
            for (Segment s : segments) {
                x0 = Math.min(x0, Math.min(s.x1, s.x2)); x1 = Math.max(x1, Math.max(s.x1, s.x2));
                z0 = Math.min(z0, Math.min(s.z1, s.z2)); z1 = Math.max(z1, Math.max(s.z1, s.z2));
            }
            minX = x0; minZ = z0; maxX = x1; maxZ = z1;
            if (segments.size() <= 8) { leaves = segments; left = right = null; }
            else {
                boolean horizontal = x1 - x0 >= z1 - z0;
                segments.sort(Comparator.comparingDouble(s -> horizontal ? s.x1 + s.x2 : s.z1 + s.z2));
                int middle = segments.size() / 2;
                left = new Node(new ArrayList<>(segments.subList(0, middle)));
                right = new Node(new ArrayList<>(segments.subList(middle, segments.size()))); leaves = List.of();
            }
        }
        void nearest(double x, double z, Nearest best) {
            if (distance(x, z) > best.distance + EPSILON) return;
            if (left != null) {
                if (left.distance(x, z) <= right.distance(x, z)) { left.nearest(x, z, best); right.nearest(x, z, best); }
                else { right.nearest(x, z, best); left.nearest(x, z, best); }
            } else for (Segment segment : leaves) {
                double distance = segment.squaredDistance(x, z);
                if (distance < best.distance) { best.distance = distance; best.edge = segment; }
            }
        }
        double distance(double x, double z) {
            double dx = Math.max(0, Math.max(minX - x, x - maxX)), dz = Math.max(0, Math.max(minZ - z, z - maxZ));
            return dx * dx + dz * dz;
        }
    }

    private static Area forbidden(Area scope, Field field, double offset) {
        Rectangle2D bounds = scope.getBounds2D();
        int x0 = (int) Math.floor(bounds.getMinX()), x1 = (int) Math.ceil(bounds.getMaxX());
        int z0 = (int) Math.floor(bounds.getMinY()), z1 = (int) Math.ceil(bounds.getMaxY());
        Path2D.Double blocked = new Path2D.Double(Path2D.WIND_NON_ZERO);
        for (int z = z0; z < z1; z++) {
            int run = Integer.MIN_VALUE;
            for (int x = x0; x < x1; x++) {
                int classification = classify(field, offset, x, z, 1);
                if (classification == 1) { if (run == Integer.MIN_VALUE) run = x; continue; }
                if (run != Integer.MIN_VALUE) { append(blocked, run, z, x, z + 1); run = Integer.MIN_VALUE; }
                if (classification == 0) {
                    // 仅限制线附近的方块细分到0.1格，平坦内外区域按整行合并。
                    for (int dz = 0; dz < 10; dz++) {
                        int smallRun = -1;
                        for (int dx = 0; dx < 10; dx++) {
                            boolean denied = classify(field, offset, x + dx / 10.0, z + dz / 10.0, 0.1) >= 0;
                            if (denied && smallRun < 0) smallRun = dx;
                            if (!denied && smallRun >= 0) {
                                append(blocked, x + smallRun / 10.0, z + dz / 10.0, x + dx / 10.0, z + (dz + 1) / 10.0); smallRun = -1;
                            }
                        }
                        if (smallRun >= 0) append(blocked, x + smallRun / 10.0, z + dz / 10.0, x + 1, z + (dz + 1) / 10.0);
                    }
                }
            }
            if (run != Integer.MIN_VALUE) append(blocked, run, z, x1, z + 1);
        }
        Area result = new Area(blocked);
        result.intersect(scope);
        return result;
    }

    // 返回-1允许、1禁止、0跨界；任何未知投影都不凭空制造硬限制。
    private static int classify(Field field, double offset, double x, double z, double size) {
        double[] distances = {field.at(x, z), field.at(x + size, z), field.at(x, z + size),
            field.at(x + size, z + size), field.at(x + size / 2, z + size / 2)};
        boolean inside = false, outside = false;
        for (double distance : distances) {
            if (!Double.isFinite(distance)) return -1;
            if (distance > offset + EPSILON) outside = true;
            else inside = true;
        }
        return outside ? inside ? 0 : 1 : -1;
    }

    private static List<double[]> guideLines(Area forbidden, Area scope, Field field, double offset) {
        List<double[]> lines = new ArrayList<>();
        Rectangle2D box = scope.getBounds2D();
        for (double[] edge : VizEditGeometry.segments(forbidden)) {
            if (lines.size() >= 512) break;
            // 仅显示真实离散限制线，去掉扫描范围外框与未知岸段截断产生的人工封口。
            double x = (edge[0] + edge[2]) / 2, z = (edge[1] + edge[3]) / 2;
            if (Math.abs(x - box.getMinX()) < EPSILON || Math.abs(x - box.getMaxX()) < EPSILON
                    || Math.abs(z - box.getMinY()) < EPSILON || Math.abs(z - box.getMaxY()) < EPSILON) continue;
            double distance = field.at(x, z);
            if (!Double.isFinite(distance) || distance > offset + EPSILON || distance < offset - 0.15) continue;
            lines.add(edge);
        }
        return lines;
    }

    private static List<double[]> copyGuides(List<double[]> source) {
        List<double[]> result = new ArrayList<>(source.size());
        for (double[] line : source) result.add(line.clone());
        return List.copyOf(result);
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static Area rectangle(double x0, double z0, double x1, double z1) {
        return new Area(new Rectangle2D.Double(x0, z0, x1 - x0, z1 - z0));
    }
    private static void append(Path2D.Double path, double x0, double z0, double x1, double z1) {
        // 先统一为一位小数的同一浮点表示，避免负坐标细分与保存量化产生伪越界。
        x0 = Math.rint(x0 * 10) / 10; z0 = Math.rint(z0 * 10) / 10;
        x1 = Math.rint(x1 * 10) / 10; z1 = Math.rint(z1 * 10) / 10;
        path.moveTo(x0, z0); path.lineTo(x1, z0); path.lineTo(x1, z1); path.lineTo(x0, z1); path.closePath();
    }
}
