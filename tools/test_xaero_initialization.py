#!/usr/bin/env python3
"""编译真实 Xaero 装配类，使用可控依赖验证所有版本的可选联动初始化。"""

from pathlib import Path
import re
import shutil
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]

SOURCES = {
    "net/fabricmc/loader/api/FabricLoader.java": """
package net.fabricmc.loader.api;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();
    public final Map<String, String> versions = new HashMap<>();
    public static FabricLoader getInstance() { return INSTANCE; }
    public boolean isModLoaded(String id) { return versions.containsKey(id); }
    public Optional<ModContainer> getModContainer(String id) {
        return Optional.ofNullable(versions.get(id)).map(ModContainer::new);
    }
}
""",
    "net/fabricmc/loader/api/ModContainer.java": """
package net.fabricmc.loader.api;
public record ModContainer(String version) {
    public Metadata getMetadata() { return new Metadata(version); }
    public record Metadata(String value) {
        public Version getVersion() { return new Version(value); }
    }
    public record Version(String value) {
        public String getFriendlyString() { return value; }
    }
}
""",
    "areahint/AreashintClient.java": """
package areahint;
import java.util.ArrayList;
import java.util.List;
public final class AreashintClient {
    public static final Logger LOGGER = new Logger();
    public static final class Logger {
        public final List<Event> info = new ArrayList<>();
        public final List<Event> warn = new ArrayList<>();
        public void info(String format, Object... args) { info.add(new Event(format, args)); }
        public void warn(String format, Object... args) { warn.add(new Event(format, args)); }
        public long enabled(String id) {
            return info.stream().filter(e -> (e.format().contains("已启用") || e.format().contains("已装配"))
                && List.of(e.args()).contains(id)).count();
        }
    }
    public record Event(String format, Object[] args) {}
}
""",
}

BRIDGE = """
package areahint.xaero.PACKAGE;
public final class CLASS {
    public static int calls;
    public static boolean result = true;
    public static boolean fail;
    public static boolean initialize() {
        calls++;
        if (fail) throw new LinkageError("Simulated incompatible Xaero API");
        return result;
    }
}
"""

HARNESS = r"""
import areahint.AreashintClient;
import areahint.xaero.XaeroBridgeHost;
import areahint.xaero.minimap.XaeroMinimapBridge;
import areahint.xaero.worldmap.XaeroWorldMapBridge;
import net.fabricmc.loader.api.FabricLoader;
public final class XaeroInitializationTest {
    private static final String MINI = "xaerominimap";
    private static final String WORLD = "xaeroworldmap";
    private static String implementation;
    private static String miniVersion;
    private static String worldVersion;
    private static int failures;
    public static void main(String[] args) throws Exception {
        implementation = args[0]; miniVersion = args[1]; worldVersion = args[2];
        test("both absent", () -> {
            initialize(); expectCalls(0, 0);
        });
        test("only minimap installed", () -> {
            install(MINI, miniVersion); initialize(); expectCalls(1, 0);
        });
        test("only world map installed", () -> {
            install(WORLD, worldVersion); initialize(); expectCalls(0, 1);
        });
        test("configured versions", () -> {
            installBoth(); initialize(); expectCalls(1, 1); expectEnabled(1, 1);
        });
        test("newer versions on same Minecraft", () -> {
            install(MINI, newer(miniVersion)); install(WORLD, newer(worldVersion));
            initialize(); expectCalls(1, 1); expectEnabled(1, 1);
        });
        test("minimap linkage failure is isolated", () -> {
            installBoth(); XaeroMinimapBridge.fail = true;
            initialize(); expectCalls(1, 1); expectEnabled(0, 1);
            require(!AreashintClient.LOGGER.warn.isEmpty(), "linkage failure should be logged");
        });
        test("world map linkage failure is isolated", () -> {
            installBoth(); XaeroWorldMapBridge.fail = true;
            initialize(); expectCalls(1, 1); expectEnabled(1, 0);
            require(!AreashintClient.LOGGER.warn.isEmpty(), "linkage failure should be logged");
        });
        test("minimap false result is not logged as enabled", () -> {
            installBoth(); XaeroMinimapBridge.result = false;
            initialize(); expectCalls(1, 1); expectEnabled(0, 1);
        });
        test("world map false result is not logged as enabled", () -> {
            installBoth(); XaeroWorldMapBridge.result = false;
            initialize(); expectCalls(1, 1); expectEnabled(1, 0);
        });
        test("both false results are not logged as enabled", () -> {
            installBoth(); XaeroMinimapBridge.result = XaeroWorldMapBridge.result = false;
            initialize(); expectCalls(1, 1); expectEnabled(0, 0);
        });
        System.out.println((10 - failures) + "/10 passed");
        if (failures != 0) System.exit(1);
    }
    private static String newer(String version) {
        String[] parts = version.split("\\.");
        return parts[0] + "." + (Integer.parseInt(parts[1]) + 1) + ".0";
    }
    private static void install(String id, String version) {
        FabricLoader.getInstance().versions.put(id, version);
    }
    private static void installBoth() { install(MINI, miniVersion); install(WORLD, worldVersion); }
    private static void initialize() throws Exception {
        ((XaeroBridgeHost) Class.forName(implementation).getConstructor().newInstance()).initialize();
    }
    private static void expectCalls(int mini, int world) {
        require(XaeroMinimapBridge.calls == mini, "minimap calls: expected " + mini + ", got " + XaeroMinimapBridge.calls);
        require(XaeroWorldMapBridge.calls == world, "world map calls: expected " + world + ", got " + XaeroWorldMapBridge.calls);
    }
    private static void expectEnabled(long mini, long world) {
        require(AreashintClient.LOGGER.enabled(MINI) == mini, "incorrect minimap enabled log");
        require(AreashintClient.LOGGER.enabled(WORLD) == world, "incorrect world map enabled log");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void test(String name, Checked action) throws Exception {
        FabricLoader.getInstance().versions.clear();
        AreashintClient.LOGGER.info.clear(); AreashintClient.LOGGER.warn.clear();
        XaeroMinimapBridge.calls = XaeroWorldMapBridge.calls = 0;
        XaeroMinimapBridge.result = XaeroWorldMapBridge.result = true;
        XaeroMinimapBridge.fail = XaeroWorldMapBridge.fail = false;
        try { action.run(); }
        catch (AssertionError failure) { failures++; System.out.println("FAIL " + name + ": " + failure.getMessage()); }
    }
    private interface Checked { void run() throws Exception; }
}
"""


def run(command: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")


def main() -> int:
    javac, java = shutil.which("javac"), shutil.which("java")
    if not javac or not java:
        raise SystemExit("需要 PATH 中的 Java 17 或更高版本 JDK（java 与 javac）。")
    properties = dict(re.findall(r"^([^#\s][^=]*)=(.*)$", (ROOT / "gradle.properties").read_text(encoding="utf-8"), re.M))
    implementations = sorted(ROOT.glob("versions/*/src/client/java/areahint/xaero/XaeroCompatImpl*.java"))
    implementations += sorted(ROOT.glob("modern/versions/*/src/client/java/areahint/xaero/XaeroCompatImpl*.java"))
    if len(implementations) != 12:
        raise SystemExit(f"预期 12 个版本的装配实现，实际 {len(implementations)}；请更新测试矩阵。")
    failed = 0
    with tempfile.TemporaryDirectory(prefix="areas-hint-xaero-init-") as temporary:
        temporary = Path(temporary).resolve()
        for implementation in implementations:
            version = implementation.parents[5].name
            branch = ROOT / "modern" if "modern" in implementation.relative_to(ROOT).parts else ROOT
            directory = temporary / version
            directory.mkdir()
            sources = dict(SOURCES)
            for package, name in [("minimap", "XaeroMinimapBridge"), ("worldmap", "XaeroWorldMapBridge")]:
                sources[f"areahint/xaero/{package}/{name}.java"] = BRIDGE.replace("PACKAGE", package).replace("CLASS", name)
            sources["XaeroInitializationTest.java"] = HARNESS
            for relative, source in sources.items():
                target = directory / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source, encoding="utf-8")
            host = branch / "src/client/java/areahint/xaero/XaeroBridgeHost.java"
            compilation = run([javac, "--release", "17", "-encoding", "UTF-8", "-d", str(directory),
                               str(implementation), str(host), *map(str, directory.rglob("*.java"))])
            if compilation.returncode:
                print(f"{version}: compile FAILED\n{compilation.stderr}")
                failed += 1
                continue
            versions = []
            for dependency in ["xaero_minimap", "xaero_world_map"]:
                key = f"deps.{version}.{dependency}"
                versions.append(properties.get(f"{key}_suggest", properties[key].split("_", 1)[0]))
            result = run([java, "-cp", str(directory), "XaeroInitializationTest",
                          f"areahint.xaero.{implementation.stem}", *versions])
            print(f"{version}: {result.stdout.strip()}")
            if result.returncode:
                failed += 1
                if result.stderr:
                    print(result.stderr)
    print(f"Xaero initialization: {len(implementations) - failed}/{len(implementations)} versions passed")
    return int(failed != 0)


if __name__ == "__main__":
    raise SystemExit(main())
