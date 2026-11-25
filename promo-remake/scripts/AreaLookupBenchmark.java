import areahint.data.AreaData;
import areahint.detection.AreaDetector;
import areahint.file.FileManager;
import areahint.world.ClientWorldFolderManager;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

// 用 Loom 的 1.20.4 客户端 classpath 和真实域名文件运行，计时范围只包含坐标查询和结果计数。
public class AreaLookupBenchmark {
    private static final int POINTS = 32_768;
    private static final int WARMUP = 8;
    private static final int ROUNDS = 15;
    private record Point(double x, double z) {}

    private static Point[] samples(List<AreaData> areas) {
        double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (var area : areas) {
            area.computeCache();
            minX = Math.min(minX, area.getCachedMinX());
            minZ = Math.min(minZ, area.getCachedMinZ());
            maxX = Math.max(maxX, area.getCachedMaxX());
            maxZ = Math.max(maxZ, area.getCachedMaxZ());
        }
        var points = new Point[POINTS];
        var random = new Random(20261126L);
        for (int i = 0; i < points.length; i++) {
            var area = areas.get(random.nextInt(areas.size()));
            points[i] = switch (i % 4) {
                // 四分之一均匀覆盖整个地图，用于纳入域外空地与广阔的一级域名。
                case 0 -> new Point(random.nextDouble(minX,maxX), random.nextDouble(minZ,maxZ));
                // 四分之一落在随机真实边界附近，以两个方向各八格的扰动跨过边缘。
                case 1 -> {
                    var vertices = area.getVertices();
                    int j = random.nextInt(vertices.size());
                    var a = vertices.get(j);
                    var b = vertices.get((j+1)%vertices.size());
                    double t = random.nextDouble();
                    yield new Point(a.getX()+(b.getX()-a.getX())*t+random.nextDouble(-8,8),
                            a.getZ()+(b.getZ()-a.getZ())*t+random.nextDouble(-8,8));
                }
                // 一半按域名等概率取范围内的点，使小范围三级域名也得到覆盖。
                default -> new Point(random.nextDouble(area.getCachedMinX(),area.getCachedMaxX()),
                        random.nextDouble(area.getCachedMinZ(),area.getCachedMaxZ()));
            };
        }
        return points;
    }

    private static int query(AreaDetector detector, Point[] points) {
        int checksum = 0;
        for (var point : points) {
            var hit = detector.findAreaRaw(point.x,72,point.z);
            if (hit != null) checksum += hit.getLevel();
        }
        return checksum;
    }

    public static void main(String[] args) throws Exception {
        var file = Path.of(args[0]).toRealPath();
        // 将文件路径注入客户端的离线世界上下文，正常使用产品的 JSON 读取、建索引及识别入口。
        var folder = ClientWorldFolderManager.class.getDeclaredField("currentWorldFolder");
        folder.setAccessible(true);
        folder.set(null,file.getParent());
        var detector = new AreaDetector();
        detector.loadAreaData(file.getFileName().toString());
        var areas = FileManager.readAreaData(file);
        var points = samples(areas);
        var hits = new java.util.TreeMap<Integer,Integer>();
        for (var p : points) {
            var hit = detector.findAreaRaw(p.x,72,p.z);
            hits.merge(hit == null ? 0 : hit.getLevel(), 1, Integer::sum);
        }
        if (hits.size() < 2) throw new IllegalStateException("无有效的混合命中结果");
        int expected = hits.entrySet().stream().mapToInt(e -> e.getKey()*e.getValue()).sum();
        for (int i=0; i<WARMUP; i++) {
            if (query(detector,points)!=expected) throw new IllegalStateException("预热结果不一致");
        }
        long[] batches = new long[ROUNDS];
        for (int i=0; i<ROUNDS; i++) {
            long start=System.nanoTime();
            int checksum=query(detector,points);
            batches[i]=System.nanoTime()-start;
            if (checksum!=expected) throw new IllegalStateException("计时结果不一致");
        }
        System.out.println("java="+System.getProperty("java.version")+" points="+POINTS+" levels="+hits+" batch_ns="+Arrays.toString(batches));
        Arrays.sort(batches);
        System.out.printf(Locale.ROOT,"median batch mean = %.3f us/query%n",batches[ROUNDS/2]/(1000.0*POINTS));
    }
}
