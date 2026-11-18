package areahint.vizedit;

import areahint.data.AreaData;
import areahint.data.AreaData.Vertex;
import areahint.vizedit.geometry.DraftHistory;
import areahint.vizedit.geometry.PenPath;
import areahint.vizedit.geometry.VizEditGeometry;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 管理未提交钢笔路径；网格和地形画布都使用世界坐标，仅传入与缩放对应的命中半径。 */
final class PenController {
    private enum Gesture { NONE, ADD, INSERT, ANCHOR, MOVE, SEGMENT, CONVERT, IN, OUT, CLOSE }
    private record State(PenPath path, int selected) {
        State copy() { return new State(path.copy(), selected); }
    }
    private record Hit(int index, Gesture gesture) { }

    private final VizEditSession session;
    private final DraftHistory<State> history = new DraftHistory<>(64, State::copy);
    private PenPath path = new PenPath();
    private int selected = -1;
    private List<double[]> snapEdges;
    private Gesture gesture = Gesture.NONE;
    private State before;
    private Vertex pressPoint;
    private Vertex initialAnchor;
    private double radius;
    private boolean moved;
    private boolean changed;

    PenController(VizEditSession session) { this.session = session; }

    // 路径属于当前编辑目标，切换目标由会话显式取消；撤销同一目标的画笔修改会更换草稿实例。
    PenPath path() { return path; }
    int selected() { return selected; }
    boolean active() { return path.size() > 0 || history.canUndo() || history.canRedo(); }
    boolean dragging() { return gesture != Gesture.NONE; }
    boolean closing() { return gesture == Gesture.CLOSE; }
    boolean canUndo() { return dragging() || history.canUndo(); }
    boolean canRedo() { return !dragging() && history.canRedo(); }

    private boolean editable() {
        return !VizEditClientManager.isBusy() && session.canDraw() && session.allows("expandarea");
    }

    void press(Vertex point, double hitRadius, boolean shift, boolean alt, boolean ctrl) {
        if (!editable()) return;
        release();
        radius = hitRadius;
        pressPoint = point;
        before = new State(path.copy(), selected);
        try {
            Hit hit = hit(point, radius);
            if (hit != null) {
                selected = hit.index;
                if (hit.gesture != Gesture.ANCHOR) gesture = hit.gesture;
                else if (ctrl) gesture = Gesture.MOVE;
                else if (alt) gesture = Gesture.CONVERT;
                else if (selected == 0 && path.size() > 1) gesture = Gesture.CLOSE;
                else gesture = Gesture.ANCHOR;
            } else if (ctrl) {
                int segment = path.segmentNear(point, hitRadius);
                if (segment >= 1) {
                    selected = segment;
                    gesture = Gesture.SEGMENT;
                } else if (path.size() > 0) {
                    // Ctrl 点击空白结束开放路径，与 Enter 使用同一描边操作。
                    commit(false);
                }
            } else {
                // 点在线段上时按原曲线插点，Alt 落点优先吸附其他域名，Shift 用于延长当前路径。
                int index = alt || shift ? -1 : path.insertNear(point, hitRadius);
                if (index >= 0) {
                    selected = index;
                    gesture = Gesture.INSERT;
                } else {
                    selected = path.add(placement(point, hitRadius, shift, alt));
                    gesture = Gesture.ADD;
                }
                changed = true;
            }
        } catch (VizEditGeometry.GeometryException e) {
            session.reportError(e.errorKey());
        }
        if (gesture == Gesture.NONE) before = null;
        else initialAnchor = path.anchors().get(selected).point();
    }

    // 重叠时取屏幕距离最近的可见元素；收拢的方向柄与锚点重合，只响应锚点操作。
    private Hit hit(Vertex point, double hitRadius) {
        Hit nearest = null;
        double best = hitRadius;
        var anchors = path.anchors();
        for (int i = 0; i < anchors.size(); i++) {
            var anchor = anchors.get(i);
            double d = distance(point, anchor.point());
            if (d <= best) { nearest = new Hit(i, Gesture.ANCHOR); best = d; }
            d = distance(point, anchor.in());
            if (d < best && distance(anchor.in(), anchor.point()) > 0) { nearest = new Hit(i, Gesture.IN); best = d; }
            d = distance(point, anchor.out());
            if (d < best && distance(anchor.out(), anchor.point()) > 0) { nearest = new Hit(i, Gesture.OUT); best = d; }
        }
        return nearest;
    }

    Vertex preview(Vertex point, double hitRadius, boolean shift, boolean alt) {
        if (!alt && path.size() > 1) {
            Vertex first = path.anchors().get(0).point();
            if (distance(point, first) <= hitRadius) return first;
        }
        return placement(point, hitRadius, shift, alt);
    }

    private Vertex placement(Vertex point, double hitRadius, boolean shift, boolean alt) {
        if (shift && path.size() > 0) point = constrain(path.anchors().get(path.size() - 1).point(), point);
        return alt ? snap(point, hitRadius) : point;
    }

    private Vertex snap(Vertex point, double hitRadius) {
        if (snapEdges == null) {
            snapEdges = new ArrayList<>();
            for (AreaData area : session.areas) {
                if (Objects.equals(area.getName(), session.target)) continue;
                var vertices = area.getVertices();
                if (vertices == null || vertices.size() < 2) continue;
                for (int i = 0; i < vertices.size(); i++) {
                    var a = vertices.get(i);
                    var b = vertices.get((i + 1) % vertices.size());
                    if (a != null && b != null) snapEdges.add(new double[]{a.getX(), a.getZ(), b.getX(), b.getZ()});
                }
            }
        }
        return PenPath.snap(point, snapEdges, hitRadius);
    }

    void drag(Vertex point, boolean shift, boolean alt) {
        if (gesture == Gesture.NONE || !editable()) return;
        // 两个屏幕像素左右的容差区分单击与拉柄，允许鼠标释放前轻微抖动。
        if (!moved && distance(pressPoint, point) < radius * 0.4) return;
        moved = true;
        try {
            Vertex anchor = path.anchors().get(selected).point();
            if (gesture == Gesture.MOVE || gesture == Gesture.ANCHOR || gesture == Gesture.INSERT || gesture == Gesture.SEGMENT) {
                // 固定按下时的锚点与光标偏移，移动时手柄随锚点整体平移。
                Vertex start = initialAnchor;
                Vertex target = new Vertex(start.getX() + point.getX() - pressPoint.getX(), start.getZ() + point.getZ() - pressPoint.getZ());
                if (shift) target = constrain(start, target);
                if (alt) target = snap(target, radius);
                if (gesture == Gesture.SEGMENT) {
                    // 两个端点必须一同通过坐标检查，任一方向柄越界时保留上一帧的整段。
                    PenPath next = before.path.copy();
                    Vertex left = before.path.anchors().get(selected - 1).point();
                    next.move(selected - 1, new Vertex(left.getX() + (target.getX() - start.getX()), left.getZ() + (target.getZ() - start.getZ())));
                    next.move(selected, target);
                    path = next;
                } else path.move(selected, target);
            } else {
                boolean outgoing = gesture != Gesture.IN && gesture != Gesture.CLOSE;
                // 普通拉点生成一对方向柄，Alt 拖动现有柄只更改一侧。
                Vertex target = shift ? constrain(anchor, point) : point;
                if (gesture == Gesture.IN || gesture == Gesture.OUT) {
                    PenPath.Anchor previous = before.path.anchors().get(selected);
                    if (!alt && linked(previous)) {
                        double length = distance(previous.point(), outgoing ? previous.in() : previous.out());
                        path.editSmoothHandle(selected, outgoing, target, length);
                    } else path.handle(selected, outgoing, target, false);
                } else path.handle(selected, outgoing, target, gesture == Gesture.CONVERT || !alt);
            }
            changed = true;
        } catch (VizEditGeometry.GeometryException e) {
            session.reportError(e.errorKey());
        }
    }

    // 转折点的独立方向柄在松开 Alt 后仍保持独立，平滑点两侧沿同一直线反向变化。
    private static boolean linked(PenPath.Anchor anchor) {
        double ax = anchor.in().getX() - anchor.point().getX(), az = anchor.in().getZ() - anchor.point().getZ();
        double bx = anchor.out().getX() - anchor.point().getX(), bz = anchor.out().getZ() - anchor.point().getZ();
        return ax * bx + az * bz < 0 && Math.abs(ax * bz - az * bx) <= 1e-8 * Math.hypot(ax, az) * Math.hypot(bx, bz);
    }

    private static Vertex constrain(Vertex origin, Vertex point) {
        double dx = point.getX() - origin.getX(), dz = point.getZ() - origin.getZ();
        double diagonalThreshold = Math.tan(Math.PI / 8);
        if (Math.abs(dz) <= Math.abs(dx) * diagonalThreshold) return new Vertex(point.getX(), origin.getZ());
        if (Math.abs(dx) <= Math.abs(dz) * diagonalThreshold) return new Vertex(origin.getX(), point.getZ());
        double step = Math.hypot(dx, dz) / Math.sqrt(2);
        return new Vertex(origin.getX() + Math.copySign(step, dx), origin.getZ() + Math.copySign(step, dz));
    }

    private static double distance(Vertex a, Vertex b) {
        return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ());
    }

    void release() {
        if (gesture == Gesture.NONE) return;
        if (!editable()) { cancel(); return; }
        boolean close = gesture == Gesture.CLOSE;
        if (!moved && gesture == Gesture.ANCHOR) {
            removeSelected();
            changed = true;
        }
        if (!moved && gesture == Gesture.CONVERT) {
            path.corner(selected);
            changed = true;
        }
        if (changed) history.push(before);
        endGesture();
        if (close) commit(true);
    }

    private void removeSelected() {
        path.remove(selected);
        selected = Math.min(selected, path.size() - 1);
    }

    void delete() {
        release();
        if (!editable() || selected < 0) return;
        history.push(new State(path, selected));
        removeSelected();
    }

    void undo() {
        if (dragging()) { abortGesture(); return; }
        if (editable() && history.canUndo()) restore(history.undo(new State(path, selected)));
    }

    void redo() {
        if (dragging()) return;
        if (editable() && history.canRedo()) restore(history.redo(new State(path, selected)));
    }

    private void restore(State state) {
        path = state.path;
        selected = state.selected;
        session.status = "";
    }

    void finishOpen() {
        release();
        if (path.size() > 0) commit(false);
    }

    // 工作路径只在结束时写入面积，闭合出错保留全部锚点供调整，成功后整体可撤销。
    private void commit(boolean closed) {
        if (!editable()) return;
        try {
            Area paint = closed ? path.fill() : path.stroke(session.brushWidth);
            Area shape = VizEditGeometry.union(session.draft.shape, paint);
            if (!shape.equals(session.draft.shape)) {
                session.checkpoint();
                session.draft.shape = shape;
            }
            session.status = "";
            cancel();
        } catch (VizEditGeometry.GeometryException e) {
            session.reportError(e.errorKey());
        }
    }

    private void endGesture() {
        gesture = Gesture.NONE;
        before = null;
        moved = false;
        changed = false;
    }

    // 窗口或维度在一次按住手势中发生切换时，仅回滚此次手势，保留此前未提交的路径。
    void abortGesture() {
        if (before != null) restore(before);
        endGesture();
    }

    void cancel() {
        endGesture();
        path = new PenPath();
        selected = -1;
        history.clear();
        snapEdges = null;
    }
}
