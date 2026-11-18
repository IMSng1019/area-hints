"""用真实钢笔控制器和几何代码复现两套客户端的跨工具撤销与闭合手势。"""

from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]

AREA = """
package areahint.data;
import java.util.List;
public class AreaData {
    public record Vertex(double getX, double getZ) {}
    public String getName() { return ""; }
    public List<Vertex> getVertices() { return List.of(); }
}
"""

CHECK = """
package areahint.vizedit;
import areahint.data.AreaData;
import areahint.data.AreaData.Vertex;
import areahint.vizedit.geometry.DraftHistory;
import java.awt.geom.Area;
import java.util.List;

class VizEditClientManager {
    static boolean isBusy() { return false; }
}
class VizEditSession {
    static class Draft {
        Area shape = new Area();
        Draft copy() { Draft copy = new Draft(); copy.shape = new Area(shape); return copy; }
    }
    Draft draft = new Draft();
    List<AreaData> areas = List.of();
    String target, status;
    int brushWidth = 2;
    DraftHistory<Draft> history = new DraftHistory<>(64, Draft::copy);
    void checkpoint() { history.push(draft); }
    void reportError(String key) { throw new AssertionError(key); }
    boolean allows(String key) { return true; }
    boolean canDraw() { return true; }
}
class PenHistoryCheck {
    static void click(PenController pen, int x, int z) {
        pen.press(new Vertex(x,z), 1, false, false, false);
        pen.release();
    }
    public static void main(String[] args) {
        var session = new VizEditSession();
        var pen = new PenController(session);
        click(pen,0,0); click(pen,20,0); click(pen,20,20);
        if (args[0].equals("draft-undo")) {
            // 画笔完成后回滚正式草稿时，历史返回同一目标的深拷贝。
            session.checkpoint();
            session.draft = session.history.undo(session.draft);
            if (pen.path().size() != 3) throw new AssertionError("draft undo discarded pen anchors");
        } else {
            pen.press(new Vertex(0,0), 1, false, false, false);
            pen.undo();
            pen.release();
            if (!session.draft.shape.isEmpty() || pen.path().size() != 3 || session.history.canUndo())
                throw new AssertionError("undo on first anchor committed or removed part of the path");
        }
    }
}
"""


def main():
    with tempfile.TemporaryDirectory() as temp:
        work = Path(temp)
        area, check = work / "AreaData.java", work / "PenHistoryCheck.java"
        area.write_text(AREA, encoding="utf-8")
        check.write_text(CHECK, encoding="utf-8")
        failed = []
        for tree in [ROOT / "src", ROOT / "modern/src"]:
            geometry = tree / "main/java/areahint/vizedit/geometry"
            controller = tree / "client/java/areahint/vizedit/PenController.java"
            subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(work), area, check,
                            controller, *geometry.glob("*.java")], check=True)
            for case in ["draft-undo", "undo-close"]:
                result = subprocess.run(["java", "-cp", str(work), "areahint.vizedit.PenHistoryCheck", case])
                if result.returncode:
                    failed.append((tree, case))
        if failed:
            raise AssertionError(failed)
        print("Both EditViz pen history checks passed on both source trees.")


if __name__ == "__main__":
    main()
