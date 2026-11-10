package areahint.vizedit;

import areahint.data.AreaData;
import areahint.data.AreaData.Vertex;
import areahint.i18n.I18nManager;
import areahint.vizedit.geometry.VizEditGeometry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

// 网格界面与 Xaero 浮层复用本控制器；只有画布投影来自不同实现。
public final class VizEditScreen extends Screen {
    private final VizEditSession session;
    private final VizEditCanvas canvas;
    private final Screen terrainScreen;
    private final List<ButtonWidget> editingButtons = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Vertex> stroke = new ArrayList<>();
    private TextFieldWidget search;
    private ButtonWidget undo;
    private ButtonWidget redo;
    private ButtonWidget properties;
    private ButtonWidget delete;
    private ButtonWidget save;
    private ButtonWidget newArea;
    private ButtonWidget switchCanvas;
    private ButtonWidget saveDraft;
    private int panelWidth;
    private int canvasTop;
    private int canvasBottom;
    private int treeBottom;
    private boolean drawing;
    private boolean panning;
    private boolean spaceDown;
    private double dragX;
    private double dragY;
    private double sampledX = Double.NaN;
    private double sampledY = Double.NaN;
    private boolean sampledShift;
    private Area beforeStroke;
    private VizEditSession.Draft beforeDraft;

    public VizEditScreen(VizEditSession session, VizEditCanvas canvas, Screen terrainScreen) {
        super(Text.literal(t("vizedit.ui.title")));
        this.session = session; this.canvas = canvas; this.terrainScreen = terrainScreen;
    }

    @Override protected void init() {
        editingButtons.clear();
        panelWidth = Math.min(184, Math.max(104, width / 4));
        canvasTop = width - panelWidth < 360 ? 92 : 70;
        canvasBottom = Math.max(canvasTop + 20, height - 28);
        treeBottom = Math.max(112, height - (session.approvalId != null ? 88 : 64));
        if (canvas instanceof VizEditCanvas.Grid grid) grid.viewport(panelWidth + 4, canvasTop, Math.max(20, width - panelWidth - 8), canvasBottom - canvasTop);
        int left = panelWidth + 5;
        int available = Math.max(70, width - left - 5);
        int actionW = Math.max(20, (available - 12) / 4);
        properties = button("vizedit.ui.properties", left, 6, actionW, () -> client.setScreen(new VizEditPropertiesScreen(session)));
        save = button("vizedit.ui.save", left + actionW + 4, 6, actionW, () -> VizEditClientManager.review(false, null));
        switchCanvas = button(canvas.isTerrain() ? "vizedit.ui.grid" : "vizedit.ui.map", left + 2 * (actionW + 4), 6, actionW, VizEditClientManager::switchCanvas);
        button("vizedit.ui.close", left + 3 * (actionW + 4), 6, actionW, VizEditClientManager::requestClose);
        int columns = canvasTop > 70 ? 4 : 7;
        int toolW = Math.max(16, (available - (columns - 1) * 3) / columns);
        for (VizEditSession.Tool tool : VizEditSession.Tool.values()) {
            int index = tool.ordinal();
            ButtonWidget button = button(tool.key(), left + index % columns * (toolW + 3), 30 + index / columns * 22, toolW, () -> session.tool = tool);
            if (tool != VizEditSession.Tool.PAN) editingButtons.add(button);
        }
        search = new TextFieldWidget(textRenderer, 6, 26, panelWidth - 12, 18, Text.literal(t("vizedit.ui.search")));
        search.setMaxLength(128); search.setText(session.search);
        search.setPlaceholder(Text.literal(t("vizedit.ui.search")));
        search.setChangedListener(value -> { session.search = value; session.treeOffset = 0; });
        addDrawableChild(search);
        int half = (panelWidth - 16) / 2;
        newArea = button("vizedit.ui.new", 6, 48, half, () -> VizEditClientManager.changeTarget(null));
        delete = button("vizedit.ui.delete", 10 + half, 48, half, () -> {
            if (session.draft == null || session.hasChildren()) return;
            session.operation = "delete";
            VizEditClientManager.review(false, () -> {});
        });
        undo = button("vizedit.ui.undo", 6, height - 46, half, () -> { session.undo(); refreshState(); });
        redo = button("vizedit.ui.redo", 10 + half, height - 46, half, () -> { session.redo(); refreshState(); });
        if (session.approvalId != null) saveDraft = button("vizedit.ui.draft", 6, height - 70, panelWidth - 12,
            () -> VizEditClientManager.review(true, null));
        button("−", left, canvasTop - 18, 18, () -> session.brushWidth = Math.max(1, session.brushWidth - 1));
        button("+", left + 22, canvasTop - 18, 18, () -> session.brushWidth = Math.min(256, session.brushWidth + 1));
        refreshState();
    }

    private ButtonWidget button(String key, int x, int y, int w, Runnable action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(t(key)), ignored -> action.run()).dimensions(x, y, w, 18).build());
    }
    private void refreshState() {
        boolean editable = !VizEditClientManager.isBusy() && !VizEditClientManager.isDraftLocked() && !session.readOnly && !session.invalid;
        properties.active = session.draft != null && !VizEditClientManager.isBusy();
        save.active = !VizEditClientManager.isBusy() && !session.readOnly && !session.invalid && session.draft != null;
        if (saveDraft != null) saveDraft.active = save.active;
        if (session.approvalId != null && !session.canResubmit) save.setMessage(Text.literal(t("vizedit.ui.draft")));
        newArea.active = editable && session.canCreate && session.approvalId == null;
        delete.active = editable && session.allows("delete") && !session.hasChildren() && session.originalArea != null
            && (session.approvalId == null || "delete".equals(session.operation));
        undo.active = editable && session.history.canUndo(); redo.active = editable && session.history.canRedo();
        switchCanvas.active = !VizEditClientManager.isBusy() && (canvas.isTerrain() || VizEditClientManager.mapAvailable());
        for (int i = 0; i < editingButtons.size(); i++) editingButtons.get(i).active = editable
            && session.canDraw() && canvas.isCurrentDimension()
            && session.allows(VizEditSession.Tool.values()[i] == VizEditSession.Tool.ERASER ? "shrinkarea" : "expandarea");
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { finishStroke(); VizEditClientManager.requestClose(); }

    // 新版 Screen.render 会自动绘制整屏遮罩或模糊，地图浮层必须覆盖此入口以免地形被再次压暗。
    //? if >=1.20.2 {
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    //?} else {
    /*@Override public void renderBackground(DrawContext context) {*/
    //?}
        // 面板和独立网格均由 render 按各自范围绘制背景，地形画布保持透明并沿用 Xaero 原始颜色。
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshState();
        // Xaero 首帧已建立相机后再定位，避免 init 中被地图自己的重置步骤覆盖。
        if (session.focusRequested && session.draft != null && !session.draft.shape.isEmpty() && canvas.isCurrentDimension()) {
            canvas.frame(session.draft.shape.getBounds2D(), panelWidth, canvasTop, width - panelWidth, canvasBottom - canvasTop);
            session.focusRequested = false;
        }
        if (!canvas.isTerrain()) {
            context.fill(panelWidth, canvasTop, width, canvasBottom, 0xFF151E29);
            drawGrid(context);
        }
        drawAreas(context);
        context.fill(0, 0, panelWidth, height, 0xF0171B22);
        context.fill(panelWidth, 0, width, canvasTop, 0xF0171B22);
        context.fill(panelWidth, canvasBottom, width, height, 0xF0171B22);
        label(context, t("vizedit.ui.title"), 6, 8, panelWidth - 12, 0xFFFFFF);
        label(context, session.dimensionalName + " [0.5]", 8, 77, panelWidth - 16, 0x9BDCF3);
        rebuildRows();
        context.enableScissor(4, 92, panelWidth - 4, treeBottom);
        int visibleRows = Math.max(1, (treeBottom - 92) / 17);
        session.treeOffset = Math.max(0, Math.min(session.treeOffset, Math.max(0, rows.size() - visibleRows)));
        for (int i = session.treeOffset; i < Math.min(rows.size(), session.treeOffset + visibleRows); i++) {
            Row row = rows.get(i);
            int y = 92 + (i - session.treeOffset) * 17;
            if (Objects.equals(row.area.getName(), session.target)) context.fill(4, y - 1, panelWidth - 4, y + 15, 0xFF25465F);
            int indent = Math.min(panelWidth / 3, row.depth * 9);
            String prefix = row.children ? session.collapsed.contains(row.area.getName()) ? "+ " : "− " : "  ";
            label(context, prefix + row.area.getName() + " [" + row.area.getLevel() + "]", 7 + indent, y + 3, panelWidth - 15 - indent, 0xE7ECF3);
        }
        context.disableScissor();
        String mode = session.readOnly ? t("vizedit.ui.readonly") : session.draft == null ? t("vizedit.ui.empty")
            : session.draft.name + (session.dirty() ? " *" : "") + " · " + t(session.tool.key());
        label(context, mode, panelWidth + 52, canvasTop - 13, width - panelWidth - 58, 0xFFDD77);
        String status = VizEditClientManager.isBusy() ? t("vizedit.ui.busy") : session.status;
        if (status.isEmpty()) status = t("vizedit.ui.help");
        label(context, status, 7, height - 20, width - 14, session.invalid ? 0xFF8888 : 0xCFD6DF);
        if (mouseY >= canvasTop && mouseY < canvasBottom && mouseX > panelWidth) {
            String coordinates = String.format(Locale.ROOT, "X %.2f  Z %.2f · %s", canvas.worldX(mouseX), canvas.worldZ(mouseY),
                I18nManager.translate("vizedit.ui.brush_width", session.brushWidth));
            label(context, coordinates, panelWidth + 8, canvasBottom - 12, width - panelWidth - 16, 0xFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
        if (mouseY >= height - 24) context.drawOrderedTooltip(textRenderer,
            textRenderer.wrapLines(Text.literal(status), Math.min(360, Math.max(80, width - 24))), mouseX, mouseY);
    }

    private void drawGrid(DrawContext context) {
        double unit = Math.pow(2, Math.ceil(Math.log(32 / canvas.pixelsPerBlock()) / Math.log(2)));
        unit = Math.max(1, unit);
        double minX = canvas.worldX(panelWidth), maxX = canvas.worldX(width);
        double minZ = canvas.worldZ(canvasTop), maxZ = canvas.worldZ(canvasBottom);
        for (double x = Math.ceil(minX / unit) * unit; x <= maxX; x += unit) {
            int sx = (int) canvas.screenX(x); context.fill(sx, canvasTop, sx + 1, canvasBottom, x == 0 ? 0xFF586A7E : 0xFF263544);
            label(context, Long.toString(Math.round(x)), sx + 2, canvasTop + 2, 70, 0x8191A3);
        }
        for (double z = Math.ceil(minZ / unit) * unit; z <= maxZ; z += unit) {
            int sy = (int) canvas.screenY(z); context.fill(panelWidth, sy, width, sy + 1, z == 0 ? 0xFF586A7E : 0xFF263544);
            label(context, Long.toString(Math.round(z)), panelWidth + 3, sy + 2, 70, 0x8191A3);
        }
    }

    private void drawAreas(DrawContext context) {
        context.enableScissor(panelWidth + 1, canvasTop, width, canvasBottom);
        for (AreaData area : session.areas) {
            if (Objects.equals(area.getName(), session.target)) continue;
            boolean parent = session.draft != null && Objects.equals(area.getName(), session.draft.parent);
            drawPolygon(context, List.of(area.getVertices()), parent ? 0x609BDCF3 : 0x357A8DA4, parent ? 0xAA9BDCF3 : 0x886F829A, false);
        }
        if (session.draft != null) {
            try {
                List<List<Vertex>> contours = VizEditGeometry.contours(session.draft.shape);
                drawPolygon(context, contours, "delete".equals(session.operation) ? 0x55FF5666 : 0x5548B9DF,
                    "delete".equals(session.operation) ? 0xFFFF6677 : 0xFFFFD166, true);
            } catch (VizEditGeometry.GeometryException e) { session.status = t(e.errorKey()); }
        }
        context.disableScissor();
    }

    // 以屏幕扫描线填充草稿，孔洞和不连续部分也可预览；最终保存仍交几何内核严格校验。
    private void drawPolygon(DrawContext context, List<List<Vertex>> contours, int fill, int line, boolean filled) {
        List<double[]> edges = new ArrayList<>();
        for (List<Vertex> points : contours) {
            if (points == null || points.size() < 2) continue;
            for (int i = 0; i < points.size(); i++) {
                Vertex a = points.get(i), b = points.get((i + 1) % points.size());
                if (a == null || b == null) continue;
                double x0 = canvas.screenX(a.getX()), y0 = canvas.screenY(a.getZ());
                double x1 = canvas.screenX(b.getX()), y1 = canvas.screenY(b.getZ());
                if (!Double.isFinite(x0) || !Double.isFinite(y0) || !Double.isFinite(x1) || !Double.isFinite(y1)) continue;
                edges.add(new double[] {x0, y0, x1, y1});
            }
        }
        if (filled && edges.size() <= 20000) {
            List<Double> intersections = new ArrayList<>();
            for (int y = canvasTop; y < canvasBottom; y += 2) {
                intersections.clear();
                for (double[] edge : edges) {
                    double scan = y + 0.5;
                    if ((edge[1] <= scan && edge[3] > scan) || (edge[3] <= scan && edge[1] > scan))
                        intersections.add(edge[0] + (scan - edge[1]) * (edge[2] - edge[0]) / (edge[3] - edge[1]));
                }
                intersections.sort(Double::compare);
                for (int i = 0; i + 1 < intersections.size(); i += 2) {
                    int x0 = (int) Math.max(panelWidth, Math.min(width, intersections.get(i)));
                    int x1 = (int) Math.max(panelWidth, Math.min(width, intersections.get(i + 1)));
                    if (x1 > x0) context.fill(x0, y, x1, Math.min(y + 2, canvasBottom), fill);
                }
            }
        }
        for (double[] edge : edges) drawLine(context, edge, line);
    }

    // Liang–Barsky 裁剪后才采样屏幕线段，远处的大坐标不会产生超长绘制循环。
    private void drawLine(DrawContext context, double[] e, int color) {
        double dx = e[2] - e[0], dy = e[3] - e[1], start = 0, end = 1;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {e[0] - panelWidth, width - e[0], e[1] - canvasTop, canvasBottom - e[1]};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) { if (q[i] < 0) return; continue; }
            double ratio = q[i] / p[i];
            if (p[i] < 0) start = Math.max(start, ratio); else end = Math.min(end, ratio);
            if (start > end) return;
        }
        double x0 = e[0] + start * dx, y0 = e[1] + start * dy;
        double x1 = e[0] + end * dx, y1 = e[1] + end * dy;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.round(x0 + (x1 - x0) * i / steps), y = (int) Math.round(y0 + (y1 - y0) * i / steps);
            context.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void rebuildRows() {
        rows.clear();
        String query = session.search.trim().toLowerCase(Locale.ROOT);
        List<AreaData> sorted = session.areas.stream().sorted(Comparator.comparingInt(AreaData::getLevel).thenComparing(AreaData::getName)).toList();
        Set<String> visited = new HashSet<>();
        if (!query.isEmpty()) {
            for (AreaData area : sorted) if (area.getName().toLowerCase(Locale.ROOT).contains(query)) rows.add(new Row(area, 0, hasChildren(area)));
            return;
        }
        for (AreaData area : sorted) if (area.getBaseName() == null || session.find(area.getBaseName()) == null) addRows(area, 0, sorted, visited);
        // 旧文件即使带环或失联引用也必须仍能选择查看，树遍历不递归进入已经访问的名字。
        for (AreaData area : sorted) if (!visited.contains(area.getName())) addRows(area, 0, sorted, visited);
    }
    private void addRows(AreaData area, int depth, List<AreaData> sorted, Set<String> visited) {
        if (!visited.add(area.getName())) return;
        rows.add(new Row(area, depth, hasChildren(area)));
        if (session.collapsed.contains(area.getName())) { markDescendants(area.getName(), sorted, visited); return; }
        // 显式栈防止任意正整数等级形成很深的调用栈。
        List<Row> pending = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) if (Objects.equals(sorted.get(i).getBaseName(), area.getName())) pending.add(new Row(sorted.get(i), depth + 1, hasChildren(sorted.get(i))));
        while (!pending.isEmpty()) {
            Row row = pending.remove(pending.size() - 1);
            if (!visited.add(row.area.getName())) continue;
            rows.add(row);
            if (session.collapsed.contains(row.area.getName())) { markDescendants(row.area.getName(), sorted, visited); continue; }
            for (int i = sorted.size() - 1; i >= 0; i--) if (Objects.equals(sorted.get(i).getBaseName(), row.area.getName()))
                pending.add(new Row(sorted.get(i), row.depth + 1, hasChildren(sorted.get(i))));
        }
    }
    private static void markDescendants(String root, List<AreaData> sorted, Set<String> visited) {
        List<String> pending = new ArrayList<>(); pending.add(root);
        for (int i = 0; i < pending.size(); i++) for (AreaData area : sorted)
            if (Objects.equals(area.getBaseName(), pending.get(i)) && visited.add(area.getName())) pending.add(area.getName());
    }
    private boolean hasChildren(AreaData area) { return session.areas.stream().anyMatch(child -> Objects.equals(child.getBaseName(), area.getName())); }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return true;
        if (mouseX < panelWidth) {
            if (mouseY >= 92 && mouseY < treeBottom) {
                int index = session.treeOffset + (int) ((mouseY - 92) / 17);
                if (index < rows.size()) {
                    Row row = rows.get(index);
                    if (row.children && mouseX < 22 + Math.min(panelWidth / 3, row.depth * 9)) {
                        if (!session.collapsed.remove(row.area.getName())) session.collapsed.add(row.area.getName());
                    } else if (!Objects.equals(session.target, row.area.getName())) VizEditClientManager.changeTarget(row.area.getName());
                    else if (session.draft != null && !session.draft.shape.isEmpty())
                        canvas.frame(session.draft.shape.getBounds2D(), panelWidth, canvasTop, width - panelWidth, canvasBottom - canvasTop);
                }
            }
            return true;
        }
        if (!inCanvas(mouseX, mouseY) || VizEditClientManager.isBusy()) return true;
        setFocused(null); search.setFocused(false);
        dragX = mouseX; dragY = mouseY;
        if (spaceDown || session.tool == VizEditSession.Tool.PAN) { panning = true; return true; }
        if (!session.canDraw() || !canvas.isCurrentDimension()
                || !session.allows(session.tool == VizEditSession.Tool.ERASER ? "shrinkarea" : "expandarea")) return true;
        if (session.tool == VizEditSession.Tool.WAND) {
            try {
                List<double[]> edges = new ArrayList<>(VizEditGeometry.segments(session.draft.shape));
                Rectangle2D view = new Rectangle2D.Double(canvas.worldX(panelWidth), canvas.worldZ(canvasTop),
                    canvas.worldX(width) - canvas.worldX(panelWidth), canvas.worldZ(canvasBottom) - canvas.worldZ(canvasTop));
                for (AreaData area : session.areas) if (!Objects.equals(area.getName(), session.target)) {
                    List<Vertex> vertices = area.getVertices();
                    if (vertices == null || vertices.isEmpty()) continue;
                    double minX = Double.POSITIVE_INFINITY, minZ = minX, maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;
                    for (Vertex v : vertices) { minX = Math.min(minX, v.getX()); minZ = Math.min(minZ, v.getZ()); maxX = Math.max(maxX, v.getX()); maxZ = Math.max(maxZ, v.getZ()); }
                    if (!view.intersects(minX, minZ, maxX - minX, maxZ - minZ) && !Objects.equals(area.getName(), session.draft.parent)) continue;
                    // 附近域名必须整环加入，不能截掉视窗外边界后制造不存在的闭合面。
                    for (int i = 0; i < vertices.size(); i++) {
                        Vertex a = vertices.get(i), b = vertices.get((i + 1) % vertices.size());
                        edges.add(new double[] {a.getX(), a.getZ(), b.getX(), b.getZ()});
                    }
                }
                Area fill = VizEditGeometry.magicWand(edges, canvas.worldX(mouseX), canvas.worldZ(mouseY));
                if (fill.isEmpty()) session.status = t("vizedit.client.no_closed_region");
                else { session.checkpoint(); session.draft.shape = VizEditGeometry.union(session.draft.shape, fill); }
            } catch (VizEditGeometry.GeometryException e) { session.status = t(e.errorKey()); }
            return true;
        }
        drawing = true; stroke.clear(); beforeStroke = new Area(session.draft.shape); beforeDraft = session.draft.copy();
        sampledX = Double.NaN; sampledY = Double.NaN;
        stroke.add(new Vertex(canvas.worldX(mouseX), canvas.worldZ(mouseY)));
        updateStroke(mouseX, mouseY); return true;
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { samplePointer(x, y); return true; }
    public void samplePointer(double x, double y) {
        if (panning) { canvas.pan(x - dragX, y - dragY); dragX = x; dragY = y; }
        if (drawing && inCanvas(x, y)) updateStroke(x, y);
    }
    private void updateStroke(double x, double y) {
        if (session.draft == null || beforeStroke == null) return;
        boolean constrain = hasShiftDown();
        if (Double.compare(x, sampledX) == 0 && Double.compare(y, sampledY) == 0 && constrain == sampledShift) return;
        sampledX = x; sampledY = y; sampledShift = constrain;
        Vertex start = stroke.get(0), point = new Vertex(canvas.worldX(x), canvas.worldZ(y));
        try {
            Area paint;
            if (session.tool == VizEditSession.Tool.RECTANGLE) paint = VizEditGeometry.rectangle(start.getX(), start.getZ(), point.getX(), point.getZ(), constrain);
            else if (session.tool == VizEditSession.Tool.ELLIPSE) paint = VizEditGeometry.ellipse(start.getX(), start.getZ(), point.getX(), point.getZ(), constrain);
            else if (session.tool == VizEditSession.Tool.LINE) {
                if (constrain) point = VizEditGeometry.constrainLine(start, point);
                paint = VizEditGeometry.stroke(List.of(start, point), session.brushWidth);
            } else {
                Vertex last = stroke.get(stroke.size() - 1);
                if (Math.abs(point.getX() - last.getX()) + Math.abs(point.getZ() - last.getZ()) >= 0.5) stroke.add(point);
                if (stroke.size() > 8192) throw new IllegalArgumentException();
                paint = VizEditGeometry.stroke(stroke, session.brushWidth);
            }
            session.draft.shape = session.tool == VizEditSession.Tool.ERASER ? VizEditGeometry.subtract(beforeStroke, paint) : VizEditGeometry.union(beforeStroke, paint);
            session.status = "";
        } catch (VizEditGeometry.GeometryException e) { session.status = t(e.errorKey()); }
        catch (IllegalArgumentException e) { session.status = t("vizedit.error.geometry_limit"); }
    }
    @Override public boolean mouseReleased(double x, double y, int button) { finishStroke(); return true; }
    private void finishStroke() {
        if (drawing && beforeDraft != null && session.draft != null && !session.draft.same(beforeDraft)) session.history.push(beforeDraft);
        drawing = false; panning = false; beforeStroke = null; beforeDraft = null; stroke.clear();
    }

    // Fabric 事件的滚轮参数在所有活跃版本均有横纵两轴，原版 Screen 签名由预处理适配。
    public boolean scroll(double x, double y, double amount) {
        if (x < panelWidth) session.treeOffset = Math.max(0, session.treeOffset - (int) Math.signum(amount) * 3);
        else if (inCanvas(x, y) && !drawing && !panning) canvas.zoom(amount, x, y);
        return true;
    }
    //? if >=1.20.2 {
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) { return scroll(x, y, vertical); }
    //?} else {
    /*@Override public boolean mouseScrolled(double x, double y, double vertical) { return scroll(x, y, vertical); }*/
    //?}

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (getFocused() instanceof TextFieldWidget field && field.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { field.setFocused(false); setFocused(null); return true; }
            return field.keyPressed(key, scan, modifiers);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (key == GLFW.GLFW_KEY_SPACE) { spaceDown = true; return true; }
        if (hasControlDown() && !VizEditClientManager.isBusy() && !VizEditClientManager.isDraftLocked() && !session.readOnly && !session.invalid) {
            if (key == GLFW.GLFW_KEY_Z) { finishStroke(); if (hasShiftDown()) session.redo(); else session.undo(); return true; }
            if (key == GLFW.GLFW_KEY_Y) { finishStroke(); session.redo(); return true; }
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE) { spaceDown = false; return true; }
        return super.keyReleased(key, scan, modifiers);
    }
    private boolean inCanvas(double x, double y) { return x > panelWidth && x < width && y >= canvasTop && y < canvasBottom; }
    private void label(DrawContext context, String text, int x, int y, int maxWidth, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(text, Math.max(1, maxWidth))), x, y, color);
    }
    private static String t(String key) { return I18nManager.translate(key); }
    private record Row(AreaData area, int depth, boolean children) { }
}
