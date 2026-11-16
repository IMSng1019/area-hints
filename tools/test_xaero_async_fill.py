#!/usr/bin/env python3
"""编译真实覆盖层源码，验证两套映射分支的异步填充接入与几何语义。"""

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[1]

# 只替代游戏、文件和配置边界；快照、填充计划、几何与异步缓存均编译仓库真实源码。
STUBS = {
    "areahint/Areashint.java": """
package areahint;
public final class Areashint {
    public static final String OVERWORLD_FILE = "overworld.json";
    public static final String NETHER_FILE = "nether.json";
    public static final String END_FILE = "end.json";
}
""",
    "areahint/AreashintClient.java": """
package areahint;
import areahint.data.AreaData;
public final class AreashintClient {
    public static final Logger LOGGER = new Logger();
    public static Detector getAreaDetector() { return new Detector(); }
    public static final class Detector {
        public AreaData findAreaForXaeroOverlay(double x, double y, double z) { return null; }
    }
    public static final class Logger {
        public void debug(String text, Object... args) { }
        public void warn(String text, Object... args) { }
        public void error(String text, Object... args) { }
    }
}
""",
    "areahint/config/ClientConfig.java": """
package areahint.config;
public final class ClientConfig {
    public static boolean isEnabled() { return true; }
    public static double getFrequency() { return 20; }
}
""",
    "areahint/log/AreaChangeTracker.java": """
package areahint.log;
public final class AreaChangeTracker {
    public static DetectionState state = new DetectionState(null, "minecraft:overworld", 0);
    public static DetectionState getDetectionState() { return state; }
    public record DetectionState(String areaName, String dimensionId, long revision) { }
}
""",
    "areahint/util/DimensionIdCache.java": """
package areahint.util;
public final class DimensionIdCache {
    public static String getId(String dimension) { return dimension; }
}
""",
    "areahint/world/ClientWorldFolderManager.java": """
package areahint.world;
import java.nio.file.Path;
public final class ClientWorldFolderManager {
    public static Path getWorldDimensionFile(String file) { return Path.of(file); }
}
""",
    "areahint/file/FileManager.java": """
package areahint.file;
import areahint.data.AreaData;
import java.nio.file.Path;
import java.util.List;
public final class FileManager {
    public static List<AreaData> data = List.of();
    public static List<AreaData> readAreaData(Path path) { return data; }
}
""",
}

CLIENT = """
package net.minecraft.client;
public final class CLIENT_CLASS {
    public World world = new World();
    public World level = world;
    public Player player = new Player();
    public static final class World {
        public Key getRegistryKey() { return new Key(); }
        public Key dimension() { return new Key(); }
    }
    public static final class Key {
        public String getValue() { return "minecraft:overworld"; }
        public String identifier() { return getValue(); }
    }
    public static final class Player {
        public double getX() { return 0; }
        public double getY() { return 64; }
        public double getZ() { return 0; }
    }
}
"""

HARNESS = r"""
package areahint.xaero;
import areahint.data.AreaData;
import areahint.geometry.PolygonGeometry;
import areahint.xaero.AreaOverlayFillResolver.FillPlan;
import areahint.xaero.AreaOverlayFillResolver.FillTriangle;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import areahint.xaero.AreaOverlayRepository.OverlaySnapshot;
import areahint.xaero.AreaOverlayRepository.Point;
import areahint.xaero.AreaOverlayRepository.Triangle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public final class AsyncFillIntegrationTest {
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        test("snapshot base plan is immutable and reused", () -> {
            OverlayArea area = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlaySnapshot snapshot = snapshot(area);
            FillPlan first = base(snapshot);
            require(first == base(snapshot), "base plan must be prepared once per snapshot");
            require(first.trianglesFor(area) == base(snapshot).trianglesFor(area), "base meshes must be reused");
            close(400, area(first.trianglesFor(area)));
            try {
                first.trianglesFor(area).clear();
                throw new AssertionError("published meshes must be immutable");
            } catch (UnsupportedOperationException expected) { }
        });
        test("active child holes all valid ancestors only", () -> {
            OverlayArea root = rectangle("root", 1, null, 0, 0, 20, 20);
            OverlayArea parent = rectangle("parent", 2, "root", 2, 2, 18, 18);
            OverlayArea child = rectangle("child", 3, "parent", 6, 6, 10, 10);
            OverlayArea sibling = rectangle("sibling", 3, "parent", 14, 14, 16, 16);
            OverlayArea deeper = rectangle("deeper", 4, "child", 7, 7, 8, 8);
            OverlaySnapshot snapshot = snapshot(root, parent, child, sibling, deeper);
            FillPlan plan = build(snapshot, child, () -> false);
            close(384, area(plan.trianglesFor(root)));
            close(240, area(plan.trianglesFor(parent)));
            require(plan.trianglesFor(child).isEmpty(), "active domain must be transparent");
            close(4, area(plan.trianglesFor(sibling)));
            close(1, area(plan.trianglesFor(deeper)));
            require(plan.trianglesFor(sibling) == base(snapshot).trianglesFor(sibling), "unaffected sibling mesh must be reused");
            require(plan.trianglesFor(deeper) == base(snapshot).trianglesFor(deeper), "inactive descendant mesh must be reused");
            require(!covers(plan.trianglesFor(root), 8.2, 8.1), "root must not fill child interior");
            require(covers(plan.trianglesFor(root), 19, 19), "root exterior must stay filled");
        });
        test("missing parent stops ancestor subtraction", () -> {
            OverlayArea root = rectangle("root", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "missing", 6, 6, 10, 10);
            FillPlan plan = build(snapshot(root, child), child, () -> false);
            close(400, area(plan.trianglesFor(root)));
            require(plan.trianglesFor(child).isEmpty(), "active domain still has empty fill");
        });
        test("invalid parent level does not hole unrelated levels", () -> {
            OverlayArea root = rectangle("root", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 3, "root", 6, 6, 10, 10);
            FillPlan plan = build(snapshot(root, child), child, () -> false);
            close(400, area(plan.trianglesFor(root)));
        });
        test("no active domain returns prebuilt base plan", () -> {
            OverlaySnapshot snapshot = snapshot(rectangle("root", 1, null, 0, 0, 20, 20));
            require(build(snapshot, null, () -> false) == base(snapshot), "unchanged geometry should return the snapshot base plan");
        });
        test("partly outside child subtracts only intersection", () -> {
            OverlayArea parent = rectangle("parent", 1, null, 0, 0, 10, 10);
            OverlayArea child = rectangle("child", 2, "parent", 8, 8, 12, 12);
            FillPlan plan = build(snapshot(parent, child), child, () -> false);
            close(96, area(plan.trianglesFor(parent)));
            require(!covers(plan.trianglesFor(parent), 9, 9), "overlap must be removed");
            require(covers(plan.trianglesFor(parent), 1, 1), "non-overlap must remain");
        });
        test("reversed winding preserves fill semantics", () -> {
            OverlayArea parent = polygon("parent", 1, null, List.of(new Point(0, 0), new Point(0, 10), new Point(10, 10), new Point(10, 0)));
            OverlayArea child = rectangle("child", 2, "parent", 2, 2, 4, 4);
            close(96, area(build(snapshot(parent, child), child, () -> false).trianglesFor(parent)));
        });
        test("cancellation reaches geometry calculation", () -> {
            OverlayArea parent = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "parent", 6, 6, 10, 10);
            try {
                build(snapshot(parent, child), child, () -> true);
                throw new AssertionError("cancelled fill must not publish a partial result");
            } catch (CancellationException expected) { }
        });
        test("repository refresh publishes new base geometry", () -> {
            AreaOverlayRepository repository = AreaOverlayRepository.getInstance();
            repository.clear();
            AreaData first = source("same-name", 0, 0, 10, 10);
            areahint.file.FileManager.data = List.of(first);
            repository.refreshDimension("overworld");
            OverlaySnapshot before = repository.getSnapshot("overworld");
            close(100, area(base(before).trianglesFor(before.byName().get("same-name"))));
            areahint.file.FileManager.data = List.of(source("same-name", 0, 0, 20, 20));
            repository.refreshDimension("overworld");
            OverlaySnapshot after = repository.getSnapshot("overworld");
            require(after.revision() > before.revision(), "refresh must invalidate geometry keys");
            close(400, area(base(after).trianglesFor(after.byName().get("same-name"))));
            close(100, area(base(before).trianglesFor(before.byName().get("same-name"))));
            repository.clear();
            require(repository.getSnapshot("overworld").areas().isEmpty(), "disconnect must release old snapshot");
        });
        test("resolve returns base immediately while worker is occupied", () -> {
            clearResolver();
            OverlayArea parent = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "parent", 6, 6, 10, 10);
            OverlaySnapshot snapshot = snapshot(parent, child);
            active("child", 1);
            whileWorkerBlocked(() -> {
                FillPlan immediate = callOnRenderThread(snapshot);
                require(immediate == base(snapshot), "first unresolved frame must use this snapshot's base fill");
                close(400, area(immediate.trianglesFor(parent)));
            });
            FillPlan completed = resolve(snapshot);
            close(384, area(completed.trianglesFor(parent)));
            require(completed.trianglesFor(child).isEmpty(), "completed geometry must become visible");
        });
        test("same geometry is reused after leave and reenter", () -> {
            clearResolver();
            OverlayArea parent = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "parent", 6, 6, 10, 10);
            OverlaySnapshot snapshot = snapshot(parent, child);
            active("child", 1);
            resolve(snapshot);
            flushWorker();
            FillPlan completed = resolve(snapshot);
            active(null, 2);
            resolve(snapshot);
            flushWorker();
            active("child", 3);
            whileWorkerBlocked(() -> require(callOnRenderThread(snapshot) == completed,
                "detection revision changes must not invalidate identical geometry"));
        });
        test("new snapshot fallback never reuses old named geometry", () -> {
            clearResolver();
            OverlayArea oldParent = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "parent", 6, 6, 10, 10);
            OverlaySnapshot old = snapshot(oldParent, child);
            active("child", 1);
            resolve(old);
            flushWorker();
            close(384, area(resolve(old).trianglesFor(oldParent)));
            OverlayArea newParent = rectangle("parent", 1, null, 0, 0, 30, 30);
            OverlaySnapshot next = new OverlaySnapshot("minecraft:overworld", List.of(newParent, child),
                Map.of("parent", newParent, "child", child), 2);
            whileWorkerBlocked(() -> {
                FillPlan fallback = callOnRenderThread(next);
                require(fallback == base(next), "new snapshot must use its own base plan before work completes");
                close(900, area(fallback.trianglesFor(newParent)));
            });
            close(884, area(resolve(next).trianglesFor(newParent)));
        });
        test("repository clear also discards completed resolver state", () -> {
            clearResolver();
            OverlayArea parent = rectangle("parent", 1, null, 0, 0, 20, 20);
            OverlayArea child = rectangle("child", 2, "parent", 6, 6, 10, 10);
            OverlaySnapshot snapshot = snapshot(parent, child);
            active("child", 1);
            resolve(snapshot);
            flushWorker();
            require(resolve(snapshot) != base(snapshot), "setup must publish a completed entry plan");
            AreaOverlayRepository.getInstance().clear();
            whileWorkerBlocked(() -> require(callOnRenderThread(snapshot) == base(snapshot),
                "disconnect must invalidate resolver cache even if a caller still holds the old snapshot"));
        });
        System.out.println(passed + "/" + (passed + failed) + " passed");
        if (failed != 0) System.exit(1);
    }

    private static FillPlan base(OverlaySnapshot snapshot) throws Exception {
        try {
            return (FillPlan) OverlaySnapshot.class.getMethod("basePlan").invoke(snapshot);
        } catch (NoSuchMethodException absent) {
            throw new AssertionError("snapshot must prepare and reuse its base fill plan", absent);
        }
    }

    private static void clearResolver() throws Exception {
        try {
            AreaOverlayFillResolver.class.getMethod("clear").invoke(AreaOverlayFillResolver.getInstance());
        } catch (NoSuchMethodException absent) {
            throw new AssertionError("resolver must clear pending and completed world state", absent);
        }
    }

    private static FillPlan resolve(OverlaySnapshot snapshot) {
        return AreaOverlayFillResolver.getInstance().resolve(snapshot, new net.minecraft.client.CLIENT_CLASS());
    }

    private static void active(String name, long revision) {
        areahint.log.AreaChangeTracker.state = new areahint.log.AreaChangeTracker.DetectionState(name, "minecraft:overworld", revision);
    }

    // 测试只占住真实后台执行器，不替换缓存实现，确保渲染调用不依赖计算完成。
    private static Executor worker() throws Exception {
        Field plans = AreaOverlayFillResolver.class.getDeclaredField("plans");
        plans.setAccessible(true);
        Object cache = plans.get(AreaOverlayFillResolver.getInstance());
        Field executor = cache.getClass().getDeclaredField("executor");
        executor.setAccessible(true);
        return (Executor) executor.get(cache);
    }

    private static void flushWorker() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        worker().execute(done::countDown);
        require(done.await(3, TimeUnit.SECONDS), "background worker did not finish queued geometry");
    }

    private static void whileWorkerBlocked(Checked check) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch released = new CountDownLatch(1);
        worker().execute(() -> {
            entered.countDown();
            try { released.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        require(entered.await(3, TimeUnit.SECONDS), "worker gate was not reached");
        try { check.run(); }
        finally { released.countDown(); flushWorker(); }
    }

    private static FillPlan callOnRenderThread(OverlaySnapshot snapshot) throws Exception {
        ExecutorService renderer = Executors.newSingleThreadExecutor();
        try {
            return renderer.submit(() -> resolve(snapshot)).get(1, TimeUnit.SECONDS);
        } finally {
            renderer.shutdownNow();
        }
    }

    private static FillPlan build(OverlaySnapshot snapshot, OverlayArea active, BooleanSupplier cancelled) throws Exception {
        Method method;
        try {
            method = AreaOverlayFillResolver.class.getDeclaredMethod("buildPlan", OverlaySnapshot.class, OverlayArea.class, BooleanSupplier.class);
        } catch (NoSuchMethodException absent) {
            throw new AssertionError("fill construction must accept cooperative cancellation", absent);
        }
        method.setAccessible(true);
        try {
            return (FillPlan) method.invoke(null, snapshot, active, cancelled);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw failure;
        }
    }

    private static OverlaySnapshot snapshot(OverlayArea... areas) {
        Map<String, OverlayArea> byName = new LinkedHashMap<>();
        for (OverlayArea area : areas) byName.put(area.name(), area);
        return new OverlaySnapshot("minecraft:overworld", List.of(areas), byName, 1);
    }

    private static OverlayArea rectangle(String name, int level, String parent, double x1, double z1, double x2, double z2) {
        return polygon(name, level, parent, List.of(new Point(x1, z1), new Point(x2, z1), new Point(x2, z2), new Point(x1, z2)));
    }

    private static OverlayArea polygon(String name, int level, String parent, List<Point> points) {
        List<AreaData.Vertex> vertices = points.stream().map(p -> new AreaData.Vertex(p.x(), p.z())).toList();
        List<Triangle> triangles = PolygonGeometry.triangulate(vertices).stream().map(t -> new Triangle(t[0], t[1], t[2])).toList();
        double minX = points.stream().mapToDouble(Point::x).min().orElseThrow();
        double maxX = points.stream().mapToDouble(Point::x).max().orElseThrow();
        double minZ = points.stream().mapToDouble(Point::z).min().orElseThrow();
        double maxZ = points.stream().mapToDouble(Point::z).max().orElseThrow();
        return new OverlayArea(name, null, level, parent, List.of(), "#FFFFFF", null, null,
            points, triangles, minX, maxX, minZ, maxZ, (minX + maxX) / 2, (minZ + maxZ) / 2, 0);
    }

    private static AreaData source(String name, double x1, double z1, double x2, double z2) {
        List<AreaData.Vertex> vertices = List.of(new AreaData.Vertex(x1, z1), new AreaData.Vertex(x2, z1),
            new AreaData.Vertex(x2, z2), new AreaData.Vertex(x1, z2));
        return new AreaData(name, vertices, vertices, null, 1, null, null);
    }

    private static double area(List<FillTriangle> triangles) {
        double result = 0;
        for (FillTriangle triangle : triangles) result += Math.abs(cross(triangle.first(), triangle.second(), triangle.third())) / 2;
        return result;
    }

    private static boolean covers(List<FillTriangle> triangles, double x, double z) {
        Point point = new Point(x, z);
        for (FillTriangle triangle : triangles) {
            double a = cross(triangle.first(), triangle.second(), point);
            double b = cross(triangle.second(), triangle.third(), point);
            double c = cross(triangle.third(), triangle.first(), point);
            if ((a >= -1e-8 && b >= -1e-8 && c >= -1e-8) || (a <= 1e-8 && b <= 1e-8 && c <= 1e-8)) return true;
        }
        return false;
    }

    private static double cross(Point a, Point b, Point c) {
        return (b.x() - a.x()) * (c.z() - a.z()) - (b.z() - a.z()) * (c.x() - a.x());
    }

    private static void close(double expected, double actual) {
        require(Math.abs(expected - actual) < 1e-6, "expected area " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void test(String name, Checked check) {
        try { check.run(); passed++; }
        catch (Throwable failure) { failed++; System.out.println("FAIL " + name + ": " + failure); }
    }

    private interface Checked { void run() throws Exception; }
}
"""


def run(command: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=60)


def source_text(relative: Path, revision: str | None) -> str | None:
    if revision:
        result = run(["git", "show", f"{revision}:{relative.as_posix()}"])
        return result.stdout if result.returncode == 0 else None
    path = ROOT / relative
    return path.read_text(encoding="utf-8") if path.exists() else None


def find_gson() -> Path:
    caches = [ROOT / ".gradle-home", Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))]
    for cache in caches:
        matches = sorted((cache / "caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1").glob("*/gson-2.10.1.jar"))
        if matches:
            return matches[0]
    raise SystemExit("未找到已缓存 Gson 2.10.1；请先运行基线版本 Gradle 构建以解析依赖。")


def main() -> int:
    sys.stdout.reconfigure(errors="backslashreplace")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-ref", help="可选 Git 修订，用于确认回归测试能够捕获旧实现")
    args = parser.parse_args()
    javac, java = shutil.which("javac"), shutil.which("java")
    if not javac or not java:
        raise SystemExit("需要 PATH 中的 Java 17 或更高版本 JDK（java 与 javac）。")
    gson = find_gson()
    failed = 0
    with tempfile.TemporaryDirectory(prefix="areas-hint-xaero-fill-") as temporary:
        for name, branch, client_class in [("legacy", Path("src"), "MinecraftClient"), ("modern", Path("modern/src"), "Minecraft")]:
            directory = Path(temporary) / name
            directory.mkdir()
            sources = dict(STUBS)
            sources[f"net/minecraft/client/{client_class}.java"] = CLIENT.replace("CLIENT_CLASS", client_class)
            sources["areahint/xaero/AsyncFillIntegrationTest.java"] = HARNESS.replace("CLIENT_CLASS", client_class)
            production = [
                "client/java/areahint/xaero/AreaOverlayFillResolver.java",
                "client/java/areahint/xaero/AreaOverlayRepository.java",
                "main/java/areahint/data/AreaData.java",
                "main/java/areahint/geometry/PolygonGeometry.java",
                "main/java/areahint/geometry/TriangleMeshDifference.java",
                "main/java/areahint/xaero/AsyncOverlayCache.java",
                "main/java/areahint/network/Packets.java",
                "main/java/areahint/util/ColorUtil.java",
            ]
            for relative in production:
                content = source_text(branch / relative, args.source_ref)
                if content is not None:
                    sources[relative.split("/java/", 1)[1]] = content
            for relative, content in sources.items():
                path = directory / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8")
            compilation = run([javac, "--release", "17", "-encoding", "UTF-8", "-cp", str(gson),
                               "-d", str(directory), *map(str, directory.rglob("*.java"))])
            if compilation.returncode:
                print(f"{name}: compile FAILED\n{compilation.stderr}")
                failed += 1
                continue
            result = run([java, "-cp", os.pathsep.join([str(directory), str(gson)]), "areahint.xaero.AsyncFillIntegrationTest"])
            print(f"{name}: {result.stdout.strip()}")
            if result.returncode:
                failed += 1
                if result.stderr:
                    print(result.stderr)
    print(f"Xaero async fill integration: {2 - failed}/2 source branches passed")
    return int(failed != 0)


if __name__ == "__main__":
    raise SystemExit(main())
