"""编译各版本的真实右键桥接，在无 Minecraft 窗口的环境回归双击事件流程。"""

import pathlib
import subprocess
import tempfile
import textwrap


ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSIONS = ("1.20.1", "1.20.4", "1.20.6", "1.21.1", "1.21.4", "1.21.5", "1.21.8")

# 仅替代游戏和 Fabric 的宿主接口，双击状态、坐标转换及延迟打开均执行仓库里的真实代码。
SOURCES = {
    "net/minecraft/client/gui/screen/Screen.java": """
        package net.minecraft.client.gui.screen;
        public class Screen {}
    """,
    "net/minecraft/client/MinecraftClient.java": """
        package net.minecraft.client;
        import net.minecraft.client.gui.screen.Screen;
        public final class MinecraftClient {
            private static final MinecraftClient INSTANCE = new MinecraftClient();
            public final Mouse mouse = new Mouse();
            public final Window window = new Window();
            public Screen currentScreen;
            public static MinecraftClient getInstance() { return INSTANCE; }
            public Window getWindow() { return window; }
            public static final class Mouse {
                public double x, y;
                public double getX() { return x; }
                public double getY() { return y; }
            }
            public static final class Window {
                public int width = 1920, height = 1080, scaledWidth = 1920, scaledHeight = 1080;
                public int getWidth() { return width; }
                public int getHeight() { return height; }
                public int getScaledWidth() { return scaledWidth; }
                public int getScaledHeight() { return scaledHeight; }
            }
        }
    """,
    "net/fabricmc/fabric/api/client/screen/v1/ScreenMouseEvents.java": """
        package net.fabricmc.fabric.api.client.screen.v1;
        import net.minecraft.client.gui.screen.Screen;
        public final class ScreenMouseEvents {
            public static final Event EVENT = new Event();
            public static Event afterMouseRelease(Screen screen) { return EVENT; }
            public static final class Event {
                public AfterMouseRelease listener;
                public void register(AfterMouseRelease listener) { this.listener = listener; }
            }
            public interface AfterMouseRelease {
                void onMouseRelease(Screen screen, double x, double y, int button);
            }
        }
    """,
    "xaero/map/gui/GuiMap.java": """
        package xaero.map.gui;
        import net.minecraft.client.gui.screen.Screen;
        public class GuiMap extends Screen {
            public int closedMenus;
            public void closeRightClick() { closedMenus++; }
        }
    """,
    "areahint/xaero/AreaOverlayRepository.java": """
        package areahint.xaero;
        public final class AreaOverlayRepository {
            public record OverlayArea(String name) {}
        }
    """,
    "areahint/management/client/AreaManagementClient.java": """
        package areahint.management.client;
        import areahint.xaero.AreaOverlayRepository.OverlayArea;
        import java.util.List;
        import xaero.map.gui.GuiMap;
        public final class AreaManagementClient {
            public static int opened;
            public static String dimension;
            public static List<OverlayArea> hits;
            public static void openForHits(GuiMap map, String value, List<OverlayArea> areas) {
                opened++;
                dimension = value;
                hits = areas;
            }
        }
    """,
    "areahint/xaero/worldmap/RightClickRegression.java": """
        package areahint.xaero.worldmap;
        import areahint.management.client.AreaManagementClient;
        import areahint.xaero.AreaOverlayRepository.OverlayArea;
        import java.util.List;
        import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
        import net.minecraft.client.MinecraftClient;
        import xaero.map.gui.GuiMap;

        public final class RightClickRegression {
            private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
            private static final List<OverlayArea> HITS = List.of(new OverlayArea("测试域名"));

            public static void main(String[] args) {
                verifyDoubleClick(1920, 1080, 0, 1);
                verifyDoubleClick(960, 540, 0, 1);
                verifyDoubleClick(640, 360, 5, 1);
                verifyDoubleClick(854, 481, 0, 1);
                verifyDoubleClick(640, 360, 7, 0);
                verifyLeftClickCancels();
                verifyScreenChangeCancels();
            }

            private static GuiMap begin(int guiWidth, int guiHeight) {
                AreaWorldMapRightClick.recordDomainClick(null, null);
                CLIENT.window.scaledWidth = guiWidth;
                CLIENT.window.scaledHeight = guiHeight;
                CLIENT.mouse.x = 900;
                CLIENT.mouse.y = 600;
                GuiMap map = new GuiMap();
                CLIENT.currentScreen = map;
                AreaManagementClient.opened = 0;
                AreaWorldMapRightClick.register(map);
                AreaWorldMapRightClick.recordDomainClick("minecraft:overworld", HITS);
                release(map, 1);
                return map;
            }

            private static void release(GuiMap map, int button) {
                // Fabric 的屏幕回调与 Screen.mouseReleased 一样使用 GUI 坐标。
                double x = CLIENT.mouse.x * CLIENT.window.scaledWidth / CLIENT.window.width;
                double y = CLIENT.mouse.y * CLIENT.window.scaledHeight / CLIENT.window.height;
                ScreenMouseEvents.EVENT.listener.onMouseRelease(map, x, y, button);
            }

            private static void verifyDoubleClick(int guiWidth, int guiHeight, double movement, int expected) {
                GuiMap map = begin(guiWidth, guiHeight);
                CLIENT.mouse.x += movement;
                // Xaero 的菜单吞掉第二次按下，因此只有抬起回调能够观察双击。
                release(map, 1);
                AreaWorldMapRightClick.tick();
                require(AreaManagementClient.opened == expected,
                    "GUI " + guiWidth + "x" + guiHeight + ", movement=" + movement
                    + ": expected " + expected + " open, got " + AreaManagementClient.opened);
                require(map.closedMenus == expected, "右键菜单关闭次数错误");
                if (expected == 1) {
                    require("minecraft:overworld".equals(AreaManagementClient.dimension), "维度丢失");
                    require(HITS.equals(AreaManagementClient.hits), "命中域名丢失");
                }
                AreaWorldMapRightClick.tick();
                require(AreaManagementClient.opened == expected, "重复打开域名管理");
            }

            private static void verifyLeftClickCancels() {
                GuiMap map = begin(640, 360);
                release(map, 0);
                release(map, 1);
                AreaWorldMapRightClick.tick();
                require(AreaManagementClient.opened == 0, "左键应取消双击");
            }

            private static void verifyScreenChangeCancels() {
                GuiMap map = begin(640, 360);
                release(map, 1);
                CLIENT.currentScreen = new GuiMap();
                AreaWorldMapRightClick.tick();
                require(AreaManagementClient.opened == 0, "界面切换应取消待打开请求");
            }

            private static void require(boolean condition, String message) {
                if (!condition) throw new AssertionError(message);
            }
        }
    """,
}


def main():
    failed = []
    with tempfile.TemporaryDirectory(prefix="areas-hint-xaero-click-") as temporary:
        root = pathlib.Path(temporary)
        fixtures = []
        for name, source in SOURCES.items():
            path = root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(textwrap.dedent(source), encoding="utf-8")
            fixtures.append(str(path))
        for version in VERSIONS:
            target = root / version
            bridge = ROOT / "versions" / version / "src/client/java/areahint/xaero/worldmap/AreaWorldMapRightClick.java"
            subprocess.run(
                ["javac", "-encoding", "UTF-8", "-d", str(target), *fixtures, str(bridge)],
                check=True,
            )
            result = subprocess.run(
                ["java", "-cp", str(target), "areahint.xaero.worldmap.RightClickRegression"],
                capture_output=True, text=True, encoding="utf-8", errors="replace",
            )
            print(f"{version}: {'PASS' if result.returncode == 0 else 'FAIL'}")
            if result.returncode:
                print(result.stderr)
                failed.append(version)
    if failed:
        raise SystemExit(f"Failed versions: {', '.join(failed)}")


if __name__ == "__main__":
    main()
