package areahint.approval;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonDocumentTransactionTest {
    @TempDir Path root;

    @Test void savesAreaAndDescriptionTogether() throws IOException {
        Path area = root.resolve("areas.json"), description = root.resolve("description.json");
        Path journal = root.resolve("save.editviz-transaction");
        JsonDocumentTransaction.execute(journal, "world", root,
            Map.of(area, json("area"), description, json("description")), JsonDocumentTransaction::write);
        assertEquals(json("area"), read(area));
        assertEquals(json("description"), read(description));
        assertFalse(Files.exists(journal));
    }

    @Test void failureRestoresExistingAreaAndDoesNotLeaveNewDescription() throws IOException {
        Path area = root.resolve("areas.json"), description = root.resolve("description.json");
        Path journal = root.resolve("save.editviz-transaction");
        JsonDocumentTransaction.write(area, json("old"));
        Map<Path, JsonElement> changes = new LinkedHashMap<>();
        changes.put(area, json("new")); changes.put(description, json("description"));
        assertThrows(IOException.class, () -> JsonDocumentTransaction.execute(journal, "world", root, changes,
            (path, data) -> {
                if (path.equals(description) && !data.isJsonNull()) throw new IOException("模拟描述写入失败");
                JsonDocumentTransaction.write(path, data);
            }));
        assertEquals(json("old"), read(area));
        assertFalse(Files.exists(description));
        assertFalse(Files.exists(journal));
    }

    @Test void interruptedSaveRecoversButDoesNotOverwriteLaterExternalChanges() throws IOException {
        Path area = root.resolve("areas.json"), description = root.resolve("description.json");
        Path journal = root.resolve("save.editviz-transaction");
        JsonDocumentTransaction.write(area, json("old"));
        Map<Path, JsonElement> changes = new LinkedHashMap<>();
        changes.put(area, json("new")); changes.put(description, json("description"));
        assertThrows(InterruptedSave.class, () -> JsonDocumentTransaction.execute(journal, "world", root, changes,
            (path, data) -> {
                if (path.equals(description)) throw new InterruptedSave();
                JsonDocumentTransaction.write(path, data);
            }));
        assertEquals(json("new"), read(area));
        JsonDocumentTransaction.write(area, json("external"));
        assertThrows(IOException.class, () -> JsonDocumentTransaction.recover(journal, "world", root));
        assertEquals(json("external"), read(area));
        assertTrue(Files.exists(journal));
        JsonDocumentTransaction.write(area, json("new"));
        JsonDocumentTransaction.recover(journal, "world", root);
        assertEquals(json("old"), read(area));
        assertFalse(Files.exists(description));
        assertFalse(Files.exists(journal));
    }

    @Test void committedJournalOnlyCleansUpAndWrongWorldIsRejected() throws IOException {
        Path area = root.resolve("areas.json"), journal = root.resolve("save.editviz-transaction");
        assertThrows(InterruptedSave.class, () -> JsonDocumentTransaction.execute(journal, "world", root,
            Map.of(area, json("new")), (path, data) -> {
                JsonDocumentTransaction.write(path, data);
                if (path.equals(journal) && data.getAsJsonObject().get("committed").getAsBoolean()) throw new InterruptedSave();
            }));
        assertThrows(IOException.class, () -> JsonDocumentTransaction.recover(journal, "other", root));
        JsonDocumentTransaction.write(area, json("later"));
        JsonDocumentTransaction.recover(journal, "world", root);
        assertEquals(json("later"), read(area));
        assertFalse(Files.exists(journal));
    }

    private static JsonObject json(String value) { JsonObject object = new JsonObject(); object.addProperty("value", value); return object; }
    private static JsonElement read(Path path) throws IOException { return Files.exists(path) ? JsonParser.parseString(Files.readString(path)) : JsonNull.INSTANCE; }
    private static class InterruptedSave extends Error { }
}
