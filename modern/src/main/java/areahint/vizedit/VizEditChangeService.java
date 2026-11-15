package areahint.vizedit;

import areahint.data.AreaData;
import areahint.util.ColorUtil;
import areahint.vizedit.geometry.VizEditGeometry;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

// 纯数据变更服务不依赖玩家或世界实例，正式保存、审批草稿和最终批准共用同一套约束。
public final class VizEditChangeService {
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final Set<String> EDITABLE_FIELDS = Set.of("name", "surfacename", "color", "altitude", "level", "base-name", "vertices");

    private VizEditChangeService() { }

    @FunctionalInterface
    public interface PermissionCheck {
        boolean allowed(String operation, AreaData original);
    }

    // before/after 只包含真正变化的记录，dependencies 则保留提交时需要核验的完整目标和后代快照。
    public record Prepared(JsonArray document, JsonArray before, JsonArray after, JsonArray dependencies,
                           String operation, String targetName, String resultName, JsonObject fields) { }

    public static final class EditException extends IllegalArgumentException {
        public EditException(String key) { super(key); }
    }

    public static EditException error(String reason) {
        return new EditException(reason.startsWith("vizedit.") ? reason : "vizedit.error." + reason);
    }

    public static Prepared prepare(JsonArray baseline, JsonArray current, String operation, String target,
            JsonObject fields, String dimensionId, String dimensionalName, String author, PermissionCheck permissions) {
        if (!Set.of("create", "update", "delete").contains(operation) || fields == null) throw error("invalid");
        if (!EDITABLE_FIELDS.containsAll(fields.keySet())) throw error("invalid");
        Map<String, JsonObject> old = index(baseline);
        Map<String, JsonObject> actual = index(current);
        boolean creating = "create".equals(operation);
        boolean deleting = "delete".equals(operation);
        JsonObject original = creating ? null : actual.get(target);
        if (creating && target != null && !target.isBlank()) throw error("invalid");
        if (!creating && (original == null || !Objects.equals(old.get(target), original))) throw error("conflict");
        JsonObject proposed = creating ? new JsonObject() : original.deepCopy();
        if (creating) {
            // 创建者由服务端签入，其他非开放字段不能通过客户端 JSON 注入。
            proposed.addProperty("signature", author);
            proposed.add("signatures", new JsonArray());
            proposed.add("surfacename", JsonNull.INSTANCE);
            proposed.add("altitude", JsonNull.INSTANCE);
            proposed.addProperty("color", "#FFFFFF");
            proposed.add("base-name", JsonNull.INSTANCE);
            proposed.addProperty("level", 1);
        }
        if (!deleting) {
            fields.entrySet().forEach(entry -> proposed.add(entry.getKey(), entry.getValue().deepCopy()));
            normalize(proposed);
            if (original != null) preserveEquivalentFields(original, proposed);
        }
        String newName = deleting ? target : text(proposed, "name", false);
        if (!deleting && actual.containsKey(newName) && !newName.equals(target)) throw error("duplicate");
        // 新建或改名不能占用维度根身份，否则原先合法的一级域名父引用会被重解释为普通域名。
        if (!deleting && (creating || !Objects.equals(target, newName))
                && (newName.equals(dimensionId) || newName.equals(dimensionalName))) throw error("parent");
        int oldLevel = creating ? 0 : level(original);
        int newLevel = deleting ? oldLevel : level(proposed);
        long levelDelta = creating ? 0 : (long) newLevel - oldLevel;
        boolean renamed = !creating && !deleting && !Objects.equals(target, newName);
        Set<String> dependent = new LinkedHashSet<>();
        if (!creating) {
            dependent.add(target);
            Set<String> previousChildren = levelDelta != 0 ? descendants(old, target) : directChildren(old, target);
            Set<String> currentChildren = levelDelta != 0 ? descendants(actual, target) : directChildren(actual, target);
            if (levelDelta != 0 || renamed || deleting) {
                // 后代集合也属于基线，新加入或移走的成员会使整笔变更失败。
                if (!previousChildren.equals(currentChildren)) throw error("conflict");
                dependent.addAll(previousChildren);
            }
            for (String name : dependent) {
                if (!Objects.equals(old.get(name), actual.get(name))) throw error("conflict");
            }
            if (deleting && !currentChildren.isEmpty()) throw error("children");
        }
        AreaData beforeArea = original == null ? null : GSON.fromJson(original, AreaData.class);
        if (creating) require(permissions, "easyadd", null);
        else if (deleting) require(permissions, "delete", beforeArea);
        else {
            AreaData afterArea = GSON.fromJson(proposed, AreaData.class);
            if (renamed || levelDelta != 0 || !Objects.equals(beforeArea.getBaseName(), afterArea.getBaseName())
                    || !Objects.equals(beforeArea.getSurfacename(), afterArea.getSurfacename())) {
                require(permissions, "rename", beforeArea);
            }
            if (!Objects.equals(beforeArea.getColor(), afterArea.getColor())) require(permissions, "recolor", beforeArea);
            if (!sameAltitude(beforeArea.getAltitude(), afterArea.getAltitude())) {
                require(permissions, "sethigh", beforeArea);
            }
            if (!Objects.equals(original.get("vertices"), proposed.get("vertices"))) {
                var delta = VizEditGeometry.difference(beforeArea.getVertices(), afterArea.getVertices());
                if (delta.added()) require(permissions, "expandarea", beforeArea);
                if (delta.removed()) require(permissions, "shrinkarea", beforeArea);
            }
        }
        Map<String, JsonObject> replacements = new LinkedHashMap<>();
        if (!deleting) replacements.put(newName, proposed);
        if (!creating && !deleting) {
            for (String name : dependent) {
                if (name.equals(target)) continue;
                JsonObject descendant = actual.get(name).deepCopy();
                if (levelDelta != 0) {
                    require(permissions, "rename", GSON.fromJson(descendant, AreaData.class));
                    long shifted = (long) level(descendant) + levelDelta;
                    if (shifted < 1 || shifted > Integer.MAX_VALUE) throw error("level");
                    descendant.addProperty("level", shifted);
                }
                if (renamed && Objects.equals(target, text(descendant, "base-name", true))) {
                    descendant.addProperty("base-name", newName);
                }
                replacements.put(name, descendant);
            }
        }
        Map<String, JsonObject> merged = new LinkedHashMap<>(actual);
        if (!creating) merged.remove(target);
        merged.putAll(replacements);
        for (JsonObject changed : replacements.values()) validateParent(changed, merged, dimensionId, dimensionalName);
        JsonArray before = new JsonArray();
        JsonArray after = new JsonArray();
        for (var entry : actual.entrySet()) {
            if (!entry.getValue().equals(merged.get(entry.getKey()))) before.add(entry.getValue().deepCopy());
        }
        for (var entry : merged.entrySet()) {
            if (!entry.getValue().equals(actual.get(entry.getKey()))) after.add(entry.getValue().deepCopy());
        }
        if (before.isEmpty() && after.isEmpty()) throw error("no_changes");
        JsonArray document = new JsonArray();
        merged.values().forEach(value -> document.add(value.deepCopy()));
        JsonArray dependencies = new JsonArray();
        dependent.forEach(name -> dependencies.add(actual.get(name).deepCopy()));
        return new Prepared(document, before, after, dependencies, operation, target, deleting ? null : newName,
            deleting ? new JsonObject() : fields(proposed));
    }

    // 生成客户端可编辑投影，署名、副字幕、城市类型、游戏模式和自定义字段始终保留在服务端原记录。
    public static JsonObject fields(JsonObject area) {
        JsonObject result = new JsonObject();
        for (String key : EDITABLE_FIELDS) {
            if (area.has(key)) result.add(key, area.get(key).deepCopy());
        }
        return result;
    }

    public static Map<String, JsonObject> index(JsonArray array) {
        if (array == null) throw error("invalid");
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (JsonElement item : array) {
            if (!item.isJsonObject()) throw error("invalid");
            JsonObject value = item.getAsJsonObject();
            String name = text(value, "name", false);
            if (result.put(name, value) != null) throw error("duplicate");
        }
        return result;
    }

    private static void normalize(JsonObject object) {
        object.addProperty("name", text(object, "name", false));
        object.addProperty("level", level(object));
        for (String key : List.of("surfacename", "base-name")) {
            String value = text(object, key, true);
            object.add(key, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
        }
        String color = text(object, "color", true);
        if (color == null) color = "#FFFFFF";
        if (!ColorUtil.isValidColor(color)) throw error("color");
        object.addProperty("color", color);
        JsonElement altitude = object.get("altitude");
        if (altitude != null && !altitude.isJsonNull()) {
            if (!altitude.isJsonObject() || !Set.of("min", "max").containsAll(altitude.getAsJsonObject().keySet())) throw error("altitude");
            Double min = bound(altitude.getAsJsonObject(), "min");
            Double max = bound(altitude.getAsJsonObject(), "max");
            if (min != null && max != null && min > max) throw error("altitude");
            object.add("altitude", min == null && max == null ? JsonNull.INSTANCE
                : GSON.toJsonTree(new AreaData.AltitudeData(max, min)));
        }
        JsonElement vertices = object.get("vertices");
        if (vertices == null || !vertices.isJsonArray() || vertices.getAsJsonArray().size() > 16384) throw error("geometry_vertices");
        List<AreaData.Vertex> points = new ArrayList<>();
        for (JsonElement item : vertices.getAsJsonArray()) {
            if (!item.isJsonObject()) throw error("geometry_coordinates");
            JsonObject point = item.getAsJsonObject();
            points.add(new AreaData.Vertex(coordinate(point, "x"), coordinate(point, "z")));
        }
        var checked = VizEditGeometry.validate(points);
        if (!checked.valid()) throw error(checked.errorKey());
        // 先拒绝原始非法多边形，再强制一位小数并重新验证，不能通过量化把自交或退化输入悄悄修好。
        var quantized = VizEditGeometry.validate(VizEditVertexFormat.quantize(checked.vertices()));
        if (!quantized.valid()) throw error(quantized.errorKey());
        // 服务端不再次有损简化，已预览及审批的几何重复提交时不会继续漂移；权限仍按最终真实面积增减核对。
        object.add("vertices", VizEditVertexFormat.encode(quantized.vertices()));
        object.add("second-vertices", VizEditVertexFormat.encode(VizEditGeometry.bounds(quantized.vertices())));
    }

    private static double coordinate(JsonObject point, String key) {
        JsonElement value = point.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw error("geometry_coordinates");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || Math.abs(number) > VizEditGeometry.MAX_COORDINATE) throw error("geometry_coordinates");
        return number;
    }

    // 属性表单会补齐缺省值，但语义未变时恢复原始存在性和值，不能借改名附带重写高度或颜色。
    private static void preserveEquivalentFields(JsonObject original, JsonObject proposed) {
        AreaData before = GSON.fromJson(original, AreaData.class);
        AreaData after = GSON.fromJson(proposed, AreaData.class);
        if (sameAltitude(before.getAltitude(), after.getAltitude())) restoreField(original, proposed, "altitude");
        String oldColor = before.getColor(), newColor = after.getColor();
        if (Objects.equals(oldColor, newColor) || oldColor.startsWith("#") && oldColor.equalsIgnoreCase(newColor)) {
            restoreField(original, proposed, "color");
        }
        for (String key : List.of("surfacename", "base-name")) {
            if (Objects.equals(text(original, key, true), text(proposed, key, true))) restoreField(original, proposed, key);
        }
    }

    private static void restoreField(JsonObject original, JsonObject proposed, String key) {
        if (original.has(key)) proposed.add(key, original.get(key).deepCopy());
        else proposed.remove(key);
    }

    // 缺省高度、null、空对象和上下限都为 null 的对象均表示无限制，数字形式不同也不构成高度修改。
    private static boolean sameAltitude(AreaData.AltitudeData first, AreaData.AltitudeData second) {
        return sameBound(first == null ? null : first.getMin(), second == null ? null : second.getMin())
            && sameBound(first == null ? null : first.getMax(), second == null ? null : second.getMax());
    }

    private static boolean sameBound(Double first, Double second) {
        return first == null ? second == null : second != null && first.doubleValue() == second.doubleValue();
    }

    private static Double bound(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw error("altitude");
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result < -64 || result > 320) throw error("altitude");
        return result;
    }

    private static int level(JsonObject object) {
        try {
            JsonElement value = object.get("level");
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw error("level");
            int level = new BigDecimal(value.getAsString()).intValueExact();
            if (level < 1) throw error("level");
            return level;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw error("level");
        }
    }

    public static String text(JsonObject object, String key, boolean optional) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            if (optional) return null;
            throw error("invalid");
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw error("invalid");
        String result = value.getAsString().trim();
        if (result.length() > 256 || result.codePoints().anyMatch(Character::isISOControl)) throw error("invalid");
        if (result.isEmpty()) {
            if (optional) return null;
            throw error("invalid");
        }
        return result;
    }

    private static Set<String> directChildren(Map<String, JsonObject> areas, String parent) {
        Set<String> result = new LinkedHashSet<>();
        areas.forEach((name, value) -> {
            if (Objects.equals(parent, text(value, "base-name", true))) result.add(name);
        });
        return result;
    }

    private static Set<String> descendants(Map<String, JsonObject> areas, String parent) {
        Map<String, List<String>> children = new HashMap<>();
        areas.forEach((name, value) -> children.computeIfAbsent(text(value, "base-name", true), key -> new ArrayList<>()).add(name));
        Set<String> result = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>(children.getOrDefault(parent, List.of()));
        while (!queue.isEmpty()) {
            String name = queue.removeFirst();
            if (name.equals(parent) || !result.add(name)) throw error("cycle");
            queue.addAll(children.getOrDefault(name, List.of()));
        }
        return result;
    }

    private static void validateParent(JsonObject area, Map<String, JsonObject> areas, String dimensionId, String dimensionalName) {
        String name = text(area, "name", false);
        String parent = text(area, "base-name", true);
        int level = level(area);
        if (Objects.equals(name, parent)) throw error("cycle");
        if (level == 1) {
            // 普通域名与维度显示名同名时引用存在歧义，不能用维度特殊根规则绕过普通父域等级校验。
            if (parent != null && (areas.containsKey(parent) || !parent.equals(dimensionId) && !parent.equals(dimensionalName))) throw error("parent");
            return;
        }
        JsonObject parentArea = parent == null ? null : areas.get(parent);
        if (parentArea == null || level(parentArea) != level - 1) throw error("parent");
        Set<String> visited = new HashSet<>();
        visited.add(name);
        while (parent != null && areas.containsKey(parent)) {
            if (!visited.add(parent)) throw error("cycle");
            parent = text(areas.get(parent), "base-name", true);
        }
    }

    private static void require(PermissionCheck permissions, String operation, AreaData original) {
        if (permissions == null || !permissions.allowed(operation, original)) throw error("denied");
    }
}
