package areahint.vizedit;

import areahint.data.AreaData;
import areahint.data.AreaData.Vertex;
import areahint.util.ColorUtil;
import areahint.vizedit.geometry.DraftHistory;
import areahint.vizedit.geometry.VizEditGeometry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.Screen;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// 两种画布只改变投影，不持有独立草稿；切换画布、表单和窗口大小均复用本会话。
public final class VizEditSession {
    static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public final String id;
    public final String dimension;
    public final Screen parentScreen;
    public final List<AreaData> areas = new ArrayList<>();
    public final Map<String, Set<String>> capabilities = new LinkedHashMap<>();
    public final Set<String> collapsed = new HashSet<>();
    public final DraftHistory<Draft> history = new DraftHistory<>(64, Draft::copy);
    public final boolean canCreate;
    public final boolean readOnly;
    public final String approvalId;
    public final boolean canResubmit;
    public final int approvalRevision;
    public final String dimensionalName;
    public String target;
    public String operation = "update";
    public Draft draft;
    public Draft originalDraft;
    public AreaData originalArea;
    public String search = "";
    public int treeOffset;
    public int brushWidth = 4;
    public Tool tool = Tool.BRUSH;
    public String status = "";
    public boolean invalid;
    public boolean mapMode;
    public boolean focusRequested;
    public VizEditCanvas.Grid grid = new VizEditCanvas.Grid();

    public VizEditSession(JsonObject data, Screen parentScreen, boolean readOnly) {
        this.id = string(data, "sessionId", "");
        this.dimension = string(data, "dimensionId", "");
        this.dimensionalName = string(data, "dimensionalName", this.dimension);
        this.approvalId = string(data, "approvalId", null);
        this.canResubmit = data.has("canResubmit") && data.get("canResubmit").getAsBoolean();
        this.approvalRevision = data.has("approvalRevision") ? data.get("approvalRevision").getAsInt() : 0;
        this.canCreate = data.has("canCreate") && data.get("canCreate").getAsBoolean();
        this.parentScreen = parentScreen;
        this.readOnly = readOnly;
        if (data.has("areas")) for (JsonElement item : data.getAsJsonArray("areas")) {
            AreaData area = GSON.fromJson(item, AreaData.class);
            if (area != null && area.getName() != null) areas.add(area);
        }
        if (data.has("capabilities")) for (var item : data.getAsJsonObject("capabilities").entrySet()) {
            Set<String> actions = new HashSet<>();
            if (item.getValue().isJsonArray()) for (JsonElement action : item.getValue().getAsJsonArray()) actions.add(action.getAsString());
            capabilities.put(item.getKey(), actions);
        }
        if (approvalId != null) {
            target = string(data, "target", null);
            operation = string(data, "operation", "update");
            originalArea = find(target);
            draft = originalArea == null ? new Draft() : Draft.from(originalArea);
            if (data.has("fields") && data.get("fields").isJsonObject()) draft.read(data.getAsJsonObject("fields"));
            originalDraft = draft.copy();
            focusRequested = true;
        }
    }

    public AreaData find(String name) {
        return name == null ? null : areas.stream().filter(area -> name.equals(area.getName())).findFirst().orElse(null);
    }

    public void select(String name) {
        if (approvalId != null && !readOnly) return;
        AreaData area = find(name);
        if (area == null) return;
        Draft next;
        try { next = Draft.from(area); }
        catch (VizEditGeometry.GeometryException e) { VizEditClientManager.report(e.errorKey()); return; }
        target = name;
        operation = "update";
        originalArea = area;
        draft = next;
        originalDraft = draft.copy();
        focusRequested = true;
        history.clear();
        status = "";
    }

    public void create() {
        if (!canCreate || readOnly || approvalId != null) return;
        AreaData selected = find(target);
        target = null;
        operation = "create";
        originalArea = null;
        draft = new Draft();
        if (selected != null && selected.getLevel() < Integer.MAX_VALUE) {
            draft.parent = selected.getName();
            draft.level = Integer.toString(selected.getLevel() + 1);
        }
        originalDraft = draft.copy();
        history.clear();
        status = "";
    }

    public boolean allows(String action) {
        if (readOnly || invalid || draft == null || VizEditClientManager.isDraftLocked()) return false;
        if ("create".equals(operation)) return canCreate;
        return capabilities.getOrDefault(target, Set.of()).contains(action);
    }

    public boolean canDraw() {
        return !"delete".equals(operation) && (allows("expandarea") || allows("shrinkarea"));
    }

    public boolean dirty() {
        return draft != null && ("delete".equals(operation) || !draft.same(originalDraft));
    }

    // 只在一次完整操作开始前保存历史，连续拖动不会用采样点挤掉撤销额度。
    public void checkpoint() {
        if (draft != null) history.push(draft);
    }

    public void undo() {
        if (draft != null && history.canUndo()) draft = history.undo(draft);
    }

    public void redo() {
        if (draft != null && history.canRedo()) draft = history.redo(draft);
    }

    public List<AreaData> affectedDescendants() {
        if (draft == null || originalArea == null) return List.of();
        int level;
        try { level = Integer.parseInt(draft.level); } catch (NumberFormatException e) { return List.of(); }
        boolean relevel = level != originalArea.getLevel();
        boolean rename = !Objects.equals(draft.name.trim(), target);
        if (!relevel && !rename) return List.of();
        List<AreaData> affected = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(target);
        List<String> parents = new ArrayList<>();
        parents.add(target);
        for (int i = 0; i < parents.size(); i++) for (AreaData area : areas) {
            if (!Objects.equals(area.getBaseName(), parents.get(i)) || !visited.add(area.getName())) continue;
            affected.add(area);
            if (relevel) parents.add(area.getName());
        }
        return affected;
    }

    public boolean hasChildren() {
        return target != null && areas.stream().anyMatch(area -> target.equals(area.getBaseName()));
    }

    static String string(JsonObject object, String name, String fallback) {
        return object.has(name) && !object.get(name).isJsonNull() && object.get(name).isJsonPrimitive()
            ? object.get(name).getAsString() : fallback;
    }

    public enum Tool {
        BRUSH, LINE, RECTANGLE, ELLIPSE, ERASER, WAND, PAN;
        public String key() { return "vizedit.tool." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    // 原始文本也属于草稿；非法中间输入可以继续编辑，不会因临时关闭表单而消失。
    public static final class Draft {
        public String name = "";
        public String surface = "";
        public String color = "#FFFFFF";
        public String level = "1";
        public String parent = "";
        public String min = "";
        public String max = "";
        public Area shape = new Area();
        private boolean altitudeObject;

        public static Draft from(AreaData area) {
            Draft value = new Draft();
            value.name = area.getName();
            value.surface = text(area.getSurfacename());
            value.color = area.getColor();
            value.level = Integer.toString(area.getLevel());
            value.parent = text(area.getBaseName());
            if (area.getAltitude() != null) {
                value.altitudeObject = true;
                value.min = text(area.getAltitude().getMin());
                value.max = text(area.getAltitude().getMax());
            }
            value.shape = VizEditGeometry.fromVertices(area.getVertices());
            return value;
        }

        void read(JsonObject data) {
            if (data.has("name")) name = string(data, "name", "");
            if (data.has("surfacename")) surface = string(data, "surfacename", "");
            if (data.has("color")) color = string(data, "color", "#FFFFFF");
            if (data.has("level")) level = string(data, "level", "1");
            if (data.has("base-name")) parent = string(data, "base-name", "");
            if (data.has("altitude")) {
                min = ""; max = "";
                altitudeObject = data.get("altitude").isJsonObject();
                if (data.get("altitude").isJsonObject()) {
                    min = string(data.getAsJsonObject("altitude"), "min", "");
                    max = string(data.getAsJsonObject("altitude"), "max", "");
                }
            }
            if (data.has("vertices")) {
                List<Vertex> points = new ArrayList<>();
                for (JsonElement item : data.getAsJsonArray("vertices")) points.add(GSON.fromJson(item, Vertex.class));
                shape = VizEditGeometry.fromVertices(points);
            }
        }

        public JsonObject fields(boolean deleting) {
            JsonObject value = new JsonObject();
            if (deleting) return value;
            int number = Integer.parseInt(level.trim());
            if (number < 1 || name.trim().isEmpty() || name.trim().length() > 256
                    || !(color.matches("#[0-9a-fA-F]{6}") || ColorUtil.isFlashColor(color))) throw new IllegalArgumentException();
            Double low = number(min), high = number(max);
            if (low != null && high != null && low > high) throw new IllegalArgumentException();
            value.addProperty("name", name.trim());
            value.addProperty("surfacename", surface.trim().isEmpty() ? null : surface.trim());
            value.addProperty("color", color);
            value.addProperty("level", number);
            value.addProperty("base-name", parent.trim().isEmpty() ? null : parent.trim());
            if (low == null && high == null && !altitudeObject) value.add("altitude", JsonNull.INSTANCE);
            else {
                JsonObject altitude = new JsonObject();
                altitude.addProperty("min", low);
                altitude.addProperty("max", high);
                value.add("altitude", altitude);
            }
            value.add("vertices", GSON.toJsonTree(VizEditGeometry.toVertices(shape)));
            return value;
        }

        private static Double number(String text) {
            if (text.isBlank()) return null;
            double value = Double.parseDouble(text);
            if (!Double.isFinite(value)) throw new IllegalArgumentException();
            return value;
        }

        public Draft copy() {
            Draft value = new Draft();
            value.name = name; value.surface = surface; value.color = color; value.level = level;
            value.parent = parent; value.min = min; value.max = max; value.shape = new Area(shape);
            value.altitudeObject = altitudeObject;
            return value;
        }

        public boolean same(Draft other) {
            return other != null && Objects.equals(name, other.name) && Objects.equals(surface, other.surface)
                && Objects.equals(color, other.color) && Objects.equals(level, other.level)
                && Objects.equals(parent, other.parent) && Objects.equals(min, other.min)
                && Objects.equals(max, other.max) && shape.equals(other.shape);
        }

        private static String text(Object value) { return value == null ? "" : value.toString(); }
    }
}
