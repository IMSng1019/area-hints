package areahint.vizedit;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class VizEditChangeServiceTest {
    // 审批存储显式保留 null，测试同样不能在序列化回读时丢弃基线中的空字段。
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String ROOT = "minecraft:overworld";

    private static JsonObject area(String name, int level, String parent) {
        JsonObject value = JsonParser.parseString("{\"vertices\":[{\"x\":0,\"z\":0},{\"x\":10,\"z\":0},{\"x\":10,\"z\":10},{\"x\":0,\"z\":10}],\"color\":\"#FFFFFF\",\"altitude\":null,\"surfacename\":null,\"signature\":\"owner\",\"subtitle\":\"保留\",\"gamemode\":\"creative\",\"custom\":{\"a\":1}}").getAsJsonObject();
        value.addProperty("name", name);
        value.addProperty("level", level);
        value.add("base-name", parent == null ? JsonNull.INSTANCE : GSON.toJsonTree(parent));
        return value;
    }

    private static JsonArray list(JsonObject... areas) {
        JsonArray list = new JsonArray();
        for (JsonObject area : areas) list.add(area);
        return list;
    }

    private static VizEditChangeService.Prepared change(JsonArray baseline, JsonArray current,
            String operation, String target, JsonObject fields, Set<String> denied) {
        return VizEditChangeService.prepare(baseline, current, operation, target, fields, ROOT, ROOT,
            "creator", (action, original) -> !denied.contains(action + ":" + (original == null ? "" : original.getName())));
    }

    @Test void renamePreservesUneditableAttributesAndRepairsChildren() {
        JsonObject root = area("A", 1, null);
        JsonArray baseline = list(root, area("B", 2, "A"));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("name", "New");
        var result = change(baseline, baseline, "update", "A", fields, Set.of());
        var after = VizEditChangeService.index(result.after());
        assertEquals("保留", after.get("New").get("subtitle").getAsString());
        assertEquals(root.get("custom"), after.get("New").get("custom"));
        assertEquals("creative", after.get("New").get("gamemode").getAsString());
        assertEquals("New", after.get("B").get("base-name").getAsString());
    }

    @Test void subtreeLevelChangesAreAtomicAndCheckEveryDescendant() {
        JsonObject root = area("A", 1, null);
        JsonArray baseline = list(root, area("B", 2, "A"), area("C", 3, "B"), area("P", 1, null));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("level", 2);
        fields.addProperty("base-name", "P");
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, baseline, "update", "A", fields, Set.of("rename:C")));
        assertEquals(3, VizEditChangeService.index(baseline).get("C").get("level").getAsInt());
        var result = change(baseline, baseline, "update", "A", fields, Set.of());
        assertEquals(4, VizEditChangeService.index(result.after()).get("C").get("level").getAsInt());
    }

    @Test void unrelatedChangesDoNotBlockButNewChildrenDo() {
        JsonObject root = area("A", 1, null);
        JsonArray baseline = list(root, area("B", 2, "A"));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("name", "New");
        assertDoesNotThrow(() -> change(baseline, list(root, area("B", 2, "A"), area("Z", 1, null)), "update", "A", fields, Set.of()));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, list(root, area("B", 2, "A"), area("C", 2, "A")), "update", "A", fields, Set.of()));
    }

    @Test void mixedGeometryRequiresExpandAndShrink() {
        JsonObject root = area("A", 1, null);
        JsonArray baseline = list(root);
        JsonObject fields = VizEditChangeService.fields(root);
        fields.add("vertices", JsonParser.parseString("[{\"x\":5,\"z\":0},{\"x\":15,\"z\":0},{\"x\":15,\"z\":10},{\"x\":5,\"z\":10}]"));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, baseline, "update", "A", fields, Set.of("expandarea:A")));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, baseline, "update", "A", fields, Set.of("shrinkarea:A")));
        assertDoesNotThrow(() -> change(baseline, baseline, "update", "A", fields, Set.of()));
    }

    @Test void refusesDeleteParentAndProtectedFields() {
        JsonObject root = area("A", 1, null);
        JsonArray baseline = list(root, area("B", 2, "A"));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, baseline, "delete", "A", new JsonObject(), Set.of()));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("signature", "intruder");
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, baseline, "update", "A", fields, Set.of()));
    }

    @Test void creationUsesServerSignatureAndChecksEasyadd() {
        JsonObject fields = VizEditChangeService.fields(area("New", 1, null));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(new JsonArray(), new JsonArray(), "create", null, fields, Set.of("easyadd:")));
        var created = change(new JsonArray(), new JsonArray(), "create", null, fields, Set.of());
        assertEquals("creator", created.after().get(0).getAsJsonObject().get("signature").getAsString());
        assertTrue(created.after().get(0).getAsJsonObject().get("altitude").isJsonNull());
        assertFalse(created.after().get(0).getAsJsonObject().has("gamemode"));
    }

    @Test void snapshotDetectsChangesToUneditableProperties() {
        JsonObject original = area("A", 1, null);
        JsonArray baseline = list(original);
        JsonObject modified = original.deepCopy();
        modified.addProperty("signature", "newOwner");
        JsonObject fields = VizEditChangeService.fields(original);
        fields.addProperty("name", "New");
        var failure = assertThrows(VizEditChangeService.EditException.class,
            () -> change(baseline, list(modified), "update", "A", fields, Set.of()));
        assertEquals("vizedit.error.conflict", failure.getMessage());
    }

    @Test void approvalDependencySnapshotCanBeReplayedWithoutBlockingUnrelatedAreas() {
        JsonObject original = area("A", 1, null);
        JsonArray baseline = list(original, area("B", 2, "A"));
        JsonObject fields = VizEditChangeService.fields(original);
        fields.addProperty("name", "New");
        var submitted = change(baseline, baseline, "update", "A", fields, Set.of());
        VizEditMetadata metadata = GSON.fromJson(GSON.toJson(VizEditMetadata.from(submitted)), VizEditMetadata.class);
        assertEquals(submitted.dependencies(), metadata.dependencies);
        JsonArray later = baseline.deepCopy();
        later.add(area("Unrelated", 1, null));
        var approved = change(metadata.dependencies, later, metadata.operation, metadata.targetName, metadata.fields, Set.of());
        assertEquals(submitted.before(), approved.before());
        assertEquals(submitted.after(), approved.after());
        assertTrue(VizEditChangeService.index(approved.document()).containsKey("Unrelated"));
        later.add(area("NewChild", 2, "A"));
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(metadata.dependencies, later, metadata.operation, metadata.targetName, metadata.fields, Set.of()));
    }

    @Test void resubmissionRechecksPermissionsAndPreservesLatestProtectedData() {
        JsonObject original = area("A", 1, null);
        JsonObject fields = VizEditChangeService.fields(original);
        fields.addProperty("name", "New");
        JsonObject latest = original.deepCopy();
        latest.addProperty("subtitle", "最新副字幕");
        JsonArray current = list(latest);
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(current, current, "update", "A", fields, Set.of("rename:A")));
        var resubmitted = change(current, current, "update", "A", fields, Set.of());
        assertEquals("最新副字幕", resubmitted.after().get(0).getAsJsonObject().get("subtitle").getAsString());
    }

    @Test void highLevelsAreAllowedButFractionalLevelsAndCyclesAreRejected() {
        JsonObject root = area("A", 19, "P");
        JsonArray current = list(root, area("P", 18, "Higher"), area("Higher", 17, null));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("color", "#123456");
        assertDoesNotThrow(() -> change(current, current, "update", "A", fields, Set.of()));
        fields.addProperty("level", 19.5);
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(current, current, "update", "A", fields, Set.of()));
        JsonObject cyclic = area("A", 2, "B");
        JsonArray cycle = list(cyclic, area("B", 1, "A"));
        JsonObject cycleFields = VizEditChangeService.fields(cyclic);
        cycleFields.addProperty("color", "#123456");
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(cycle, cycle, "update", "A", cycleFields, Set.of()));
    }

    @Test void descendantOverflowRejectsTheWholeChange() {
        JsonObject root = area("A", 1, null);
        JsonArray current = list(root, area("B", Integer.MAX_VALUE, "A"), area("P", 1, null));
        JsonObject fields = VizEditChangeService.fields(root);
        fields.addProperty("level", 2);
        fields.addProperty("base-name", "P");
        assertThrows(VizEditChangeService.EditException.class,
            () -> change(current, current, "update", "A", fields, Set.of()));
        assertEquals(1, root.get("level").getAsInt());
    }

    @Test void renamePreservesUnlimitedAltitudeRepresentationsWithoutHeightPermission() {
        for (String representation : new String[] {"missing", "null", "{}", "{\"min\":null,\"max\":null}"}) {
            JsonObject original = area("A", 1, null);
            if ("missing".equals(representation)) original.remove("altitude");
            else original.add("altitude", JsonParser.parseString(representation));
            JsonArray current = list(original);
            JsonObject fields = VizEditChangeService.fields(original);
            fields.addProperty("name", "New");
            fields.add("altitude", JsonNull.INSTANCE);
            var result = VizEditChangeService.prepare(current, current, "update", "A", fields, ROOT, ROOT,
                "creator", (operation, before) -> "rename".equals(operation));
            JsonObject saved = VizEditChangeService.index(result.after()).get("New");
            assertEquals(original.has("altitude"), saved.has("altitude"), representation);
            assertEquals(original.get("altitude"), saved.get("altitude"), representation);
        }
    }

    @Test void equivalentBoundsAndOptionalFieldsKeepTheirOriginalRepresentation() {
        JsonObject original = area("A", 1, "");
        original.add("altitude", JsonParser.parseString("{\"min\":12}"));
        original.addProperty("surfacename", "");
        original.remove("color");
        JsonArray current = list(original);
        JsonObject fields = VizEditChangeService.fields(original);
        fields.addProperty("name", "New");
        fields.add("altitude", JsonParser.parseString("{\"min\":12.0,\"max\":null}"));
        fields.add("surfacename", JsonNull.INSTANCE);
        fields.add("base-name", JsonNull.INSTANCE);
        fields.addProperty("color", "#FFFFFF");
        var result = VizEditChangeService.prepare(current, current, "update", "A", fields, ROOT, ROOT,
            "creator", (operation, before) -> "rename".equals(operation));
        JsonObject saved = VizEditChangeService.index(result.after()).get("New");
        for (String key : new String[] {"altitude", "surfacename", "base-name", "color"}) {
            assertEquals(original.has(key), saved.has(key), key);
            assertEquals(original.get(key), saved.get(key), key);
        }
        fields.add("altitude", JsonParser.parseString("{\"min\":13}"));
        assertThrows(VizEditChangeService.EditException.class,
            () -> VizEditChangeService.prepare(current, current, "update", "A", fields, ROOT, ROOT,
                "creator", (operation, before) -> "rename".equals(operation)));
    }

    @Test void dimensionLabelCannotBypassSelfReferenceOrRegularParentRules() {
        JsonObject fields = VizEditChangeService.fields(area("Dimension", 1, "Dimension"));
        assertThrows(VizEditChangeService.EditException.class,
            () -> VizEditChangeService.prepare(new JsonArray(), new JsonArray(), "create", null, fields,
                ROOT, "Dimension", "creator", (operation, before) -> true));
        JsonArray current = list(area("Dimension", 1, null));
        fields.addProperty("name", "Other");
        assertThrows(VizEditChangeService.EditException.class,
            () -> VizEditChangeService.prepare(current, current, "create", null, fields,
                ROOT, "Dimension", "creator", (operation, before) -> true));
        fields.add("base-name", JsonNull.INSTANCE);
        assertDoesNotThrow(() -> VizEditChangeService.prepare(current, current, "create", null, fields,
            ROOT, "Dimension", "creator", (operation, before) -> true));
    }

    @Test void creationAndRenameCannotShadowDimensionRootButExistingNamesRemainEditable() {
        JsonArray current = list(area("A", 1, "World"));
        for (String reserved : new String[] {ROOT, "World"}) {
            JsonObject creation = VizEditChangeService.fields(area(reserved, 1, null));
            var creationError = assertThrows(VizEditChangeService.EditException.class,
                () -> VizEditChangeService.prepare(current, current, "create", null, creation,
                    ROOT, "World", "creator", (operation, before) -> true));
            assertEquals("vizedit.error.parent", creationError.getMessage());
            JsonObject rename = VizEditChangeService.fields(current.get(0).getAsJsonObject());
            rename.addProperty("name", reserved);
            rename.add("base-name", JsonNull.INSTANCE);
            var renameError = assertThrows(VizEditChangeService.EditException.class,
                () -> VizEditChangeService.prepare(current, current, "update", "A", rename,
                    ROOT, "World", "creator", (operation, before) -> true));
            assertEquals("vizedit.error.parent", renameError.getMessage());
            JsonObject legacy = area(reserved, 1, null);
            JsonArray legacyCurrent = list(legacy);
            JsonObject recolor = VizEditChangeService.fields(legacy);
            recolor.addProperty("color", "#123456");
            assertDoesNotThrow(() -> VizEditChangeService.prepare(legacyCurrent, legacyCurrent, "update", reserved,
                recolor, ROOT, "World", "creator", (operation, before) -> "recolor".equals(operation)));
        }
    }
}
