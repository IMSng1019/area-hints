package areahint.vizedit;

import com.google.gson.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VizEditDescriptionsTest {
    private static final String DIMENSION = "minecraft:overworld";

    private static JsonObject area(String name, String surface) {
        JsonObject area = new JsonObject();
        area.addProperty("name", name);
        area.addProperty("surfacename", surface);
        area.addProperty("signature", "creator");
        area.addProperty("level", 1);
        return area;
    }

    private static JsonObject document(String surface, String text) {
        JsonObject value = new JsonObject();
        value.addProperty("surfaceName", surface);
        value.addProperty("description", text);
        return value;
    }

    private static VizEditChangeService.DescriptionContext context(JsonObject before, JsonObject current) {
        return new VizEditChangeService.DescriptionContext(Path.of("descriptions"), before, current, 123456L);
    }

    private static VizEditDescriptions.Result prepare(JsonObject original, JsonObject proposed, JsonObject fields,
            JsonObject before, JsonObject current) {
        return VizEditDescriptions.prepare(original, proposed, fields, DIMENSION, "creator", (action, area) -> true,
            context(before, current));
    }

    @Test void descriptionUsesExistingSharedFileFormatAndCapturesMissingBaseline() {
        JsonObject fields = new JsonObject();
        fields.addProperty("description", "First line\nSecond line");
        JsonObject empty = new JsonObject();
        var result = prepare(null, area("A", "Shared"), fields, empty, empty);
        assertEquals(Path.of("descriptions", "Shared.json"), result.change().file());
        assertTrue(result.change().before().isJsonNull());
        JsonObject after = result.change().after().getAsJsonObject();
        assertEquals("area", after.get("targetType").getAsString());
        assertEquals("A", after.get("targetName").getAsString());
        assertEquals("Shared", after.get("surfaceName").getAsString());
        assertEquals("overworld", after.get("dimension").getAsString());
        assertEquals(123456L, after.get("updatedAt").getAsLong());
        assertTrue(result.dependencies().get("Shared.json").isJsonNull());
    }

    @Test void sharedDescriptionChangesConflictAndRequireOwnPermission() {
        JsonObject before = new JsonObject();
        before.add("Shared.json", document("Shared", "Old"));
        JsonObject current = before.deepCopy();
        current.add("Shared.json", document("Shared", "Concurrent"));
        JsonObject fields = new JsonObject();
        fields.addProperty("description", "New");
        var error = assertThrows(VizEditChangeService.EditException.class,
            () -> prepare(area("A", "Shared"), area("A", "Shared"), fields, before, current));
        assertEquals("vizedit.error.conflict", error.getMessage());
        error = assertThrows(VizEditChangeService.EditException.class,
            () -> VizEditDescriptions.prepare(area("A", "Shared"), area("A", "Shared"), fields,
                DIMENSION, "creator", (action, area) -> false, context(before, before)));
        assertEquals("vizedit.error.denied", error.getMessage());
        assertEquals("Old", VizEditDescriptions.content(before.get("Shared.json")));
    }

    @Test void renameCarriesDescriptionToEmptyDestinationWithoutRemovingSharedSource() {
        JsonObject before = new JsonObject();
        before.add("Old.json", document("Old", "Keep me"));
        var result = prepare(area("Old", null), area("New", null), new JsonObject(), before, before);
        assertEquals("Keep me", VizEditDescriptions.content(result.change().after()));
        assertEquals(Path.of("descriptions", "New.json"), result.change().file());
        assertTrue(result.dependencies().has("Old.json"));
        assertTrue(result.dependencies().get("New.json").isJsonNull());
        assertEquals("Keep me", VizEditDescriptions.content(before.get("Old.json")));
        JsonObject occupied = before.deepCopy();
        occupied.add("New.json", document("New", "Existing shared description"));
        assertNull(prepare(area("Old", null), area("New", null), new JsonObject(), occupied, occupied).change());
    }

    @Test void sanitizedNameCollisionCannotOverwriteAnotherSurfaceDescription() {
        JsonObject descriptions = new JsonObject();
        descriptions.add("A_B.json", document("A/B", "Protected"));
        JsonObject fields = new JsonObject();
        fields.addProperty("description", "Overwrite");
        var error = assertThrows(VizEditChangeService.EditException.class,
            () -> prepare(null, area("A:B", null), fields, descriptions, descriptions));
        assertEquals("vizedit.error.description_conflict", error.getMessage());
    }

    @Test void renameCannotSilentlyAdoptConcurrentlyCreatedDestinationDescription() {
        JsonObject before = new JsonObject();
        before.add("Old.json", document("Old", "Keep me"));
        JsonObject current = before.deepCopy();
        current.add("New.json", document("New", "Concurrent"));
        var error = assertThrows(VizEditChangeService.EditException.class,
            () -> prepare(area("Old", null), area("New", null), new JsonObject(), before, current));
        assertEquals("vizedit.error.conflict", error.getMessage());
    }

    @Test void renameCannotLoseConcurrentlyAddedSourceDescription() {
        JsonObject before = new JsonObject();
        JsonObject current = new JsonObject();
        current.add("Old.json", document("Old", "Concurrent"));
        var error = assertThrows(VizEditChangeService.EditException.class,
            () -> prepare(area("Old", null), area("New", null), new JsonObject(), before, current));
        assertEquals("vizedit.error.conflict", error.getMessage());
    }

    @Test void clearingDescriptionDeletesDocumentAndReplaysExactly() {
        JsonObject descriptions = new JsonObject();
        descriptions.add("A.json", document("A", "Old"));
        JsonObject fields = new JsonObject();
        fields.addProperty("description", "");
        var result = prepare(area("A", null), area("A", null), fields, descriptions, descriptions);
        assertTrue(result.change().after().isJsonNull());
        var replay = prepare(area("A", null), area("A", null), fields, result.dependencies(), descriptions);
        assertEquals(result.change(), replay.change());
    }
}
