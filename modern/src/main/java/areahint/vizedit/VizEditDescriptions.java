package areahint.vizedit;

import areahint.data.AreaData;
import areahint.description.DescriptionData;
import areahint.description.DescriptionFileManager;
import com.google.gson.*;
import java.util.Objects;

// 描述继续按联合域名共享独立文件，编辑器仅生成事务差异，不在准备阶段写盘。
public final class VizEditDescriptions {
    private VizEditDescriptions() { }

    public record Result(VizEditChangeService.DescriptionChange change, JsonObject dependencies) { }

    public static String surface(JsonObject area) {
        String surface = VizEditChangeService.text(area, "surfacename", true);
        return surface == null ? VizEditChangeService.text(area, "name", false) : surface;
    }

    public static String key(String surface) {
        return DescriptionFileManager.sanitizeDescriptionFileName(surface) + ".json";
    }

    public static String content(JsonElement document) {
        if (document == null || document.isJsonNull()) return "";
        if (!document.isJsonObject()) throw VizEditChangeService.error("invalid");
        return VizEditChangeService.multiline(document.getAsJsonObject().get("description"), 32767);
    }

    public static JsonElement document(JsonObject documents, String key) {
        return documents != null && documents.has(key) ? documents.get(key) : JsonNull.INSTANCE;
    }

    public static Result prepare(JsonObject original, JsonObject proposed, JsonObject fields, String dimension,
            String author, VizEditChangeService.PermissionCheck permissions, VizEditChangeService.DescriptionContext context) {
        JsonObject dependencies = new JsonObject();
        if (proposed == null) return new Result(null, dependencies);
        boolean explicit = fields.has("description");
        if (context == null) {
            if (explicit) throw VizEditChangeService.error("invalid");
            return new Result(null, dependencies);
        }
        String surface = surface(proposed);
        String targetKey = key(surface);
        String oldSurface = original == null ? null : surface(original);
        boolean moved = oldSurface != null && !oldSurface.equals(surface);
        if (!explicit && !moved) return new Result(null, dependencies);
        JsonElement target = document(context.current(), targetKey);
        JsonElement source = oldSurface == null ? JsonNull.INSTANCE : document(context.current(), key(oldSurface));
        depend(context, targetKey, dependencies);
        if (oldSurface != null) depend(context, key(oldSurface), dependencies);
        if (!explicit && content(document(context.baseline(), key(oldSurface))).isBlank()) return new Result(null, dependencies);
        if (!explicit && !target.isJsonNull()) return new Result(null, dependencies);
        // 文件名清理可能把不同联合域名映射到同一路径，不能覆盖另一个域名的描述。
        if (target.isJsonObject()) {
            JsonElement existingSurface = target.getAsJsonObject().get("surfaceName");
            if (existingSurface != null && !existingSurface.isJsonNull() && !surface.equals(existingSurface.getAsString()))
                throw VizEditChangeService.error("description_conflict");
        }
        if (moved && source.isJsonObject()) {
            JsonElement sourceSurface = source.getAsJsonObject().get("surfaceName");
            if (sourceSurface != null && !sourceSurface.isJsonNull() && !oldSurface.equals(sourceSurface.getAsString()))
                throw VizEditChangeService.error("description_conflict");
        }
        String text = explicit ? VizEditChangeService.multiline(fields.get("description"), 32767) : content(source);
        if (text.isBlank()) text = "";
        if (text.equals(content(target))) return new Result(null, dependencies);
        AreaData permissionArea = VizEditChangeService.GSON.fromJson(original == null ? proposed : original, AreaData.class);
        if (explicit && !permissions.allowed(text.isEmpty() ? "deletedescription" : target.isJsonNull() ? "adddescription" : "replacedescription", permissionArea))
            throw VizEditChangeService.error("denied");
        JsonElement after = JsonNull.INSTANCE;
        if (!text.isEmpty()) {
            DescriptionData value = new DescriptionData();
            value.setTargetType("area");
            value.setTargetName(VizEditChangeService.text(proposed, "name", false));
            value.setSurfaceName(surface);
            value.setDimension(dimension.substring(dimension.indexOf(':') + 1));
            value.setDescription(text);
            value.setAuthor(author);
            value.setUpdatedAt(context.updatedAt());
            after = VizEditChangeService.GSON.toJsonTree(value);
        }
        return new Result(new VizEditChangeService.DescriptionChange(context.folder().resolve(targetKey), target.deepCopy(), after), dependencies);
    }

    private static void depend(VizEditChangeService.DescriptionContext context, String key, JsonObject dependencies) {
        JsonElement before = document(context.baseline(), key);
        if (!Objects.equals(before, document(context.current(), key))) throw VizEditChangeService.error("conflict");
        dependencies.add(key, before.deepCopy());
    }
}
