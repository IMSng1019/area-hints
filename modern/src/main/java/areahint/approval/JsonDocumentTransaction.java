package areahint.approval;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

// 独立日志保护免审保存的多个 JSON 文件，提交标记落盘前中断时统一恢复原值。
public final class JsonDocumentTransaction {
    private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    private JsonDocumentTransaction() { }

    @FunctionalInterface
    interface Writer { void write(Path path, JsonElement data) throws IOException; }

    public static void execute(Path journal, String world, Path root, Map<Path, JsonElement> replacements) throws IOException {
        execute(journal, world, root, replacements, JsonDocumentTransaction::write);
    }

    static void execute(Path journal, String world, Path root, Map<Path, JsonElement> replacements, Writer writer) throws IOException {
        root = root.toAbsolutePath().normalize();
        State state = new State(); state.world = world;
        for (var entry : replacements.entrySet()) {
            Path file = entry.getKey().toAbsolutePath().normalize();
            if (!file.startsWith(root) || file.equals(root)) throw new IOException("事务目标不属于当前世界目录");
            String relative = root.relativize(file).toString();
            resolve(root, relative);
            state.before.put(relative, read(file));
            state.after.put(relative, entry.getValue() == null ? JsonNull.INSTANCE : entry.getValue().deepCopy());
        }
        writer.write(journal, GSON.toJsonTree(state));
        try {
            for (var entry : state.after.entrySet()) writer.write(resolve(root, entry.getKey()), entry.getValue());
            state.committed = true;
            writer.write(journal, GSON.toJsonTree(state));
        } catch (IOException | RuntimeException failure) {
            boolean restored = true;
            for (var entry : state.before.entrySet()) {
                try { writer.write(resolve(root, entry.getKey()), entry.getValue()); }
                catch (IOException | RuntimeException restoreFailure) { restored = false; failure.addSuppressed(restoreFailure); }
            }
            if (restored) {
                try { Files.deleteIfExists(journal); }
                catch (IOException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            }
            throw failure;
        }
        // 提交标记已经持久化，清理失败交给启动恢复，不能向客户端报告保存失败。
        try { Files.deleteIfExists(journal); } catch (IOException ignored) { }
    }

    public static void recover(Path journal, String world, Path root) throws IOException {
        root = root.toAbsolutePath().normalize();
        State state;
        try { state = GSON.fromJson(read(journal), State.class); }
        catch (RuntimeException exception) { throw new IOException("域名编辑事务日志无效", exception); }
        if (state == null || !Objects.equals(state.world, world) || state.before == null || state.after == null
                || !state.before.keySet().equals(state.after.keySet())) throw new IOException("域名编辑事务日志无效");
        // 已提交日志只做清理，不能覆盖成功保存之后的其他合法修改。
        if (!state.committed) {
            for (var entry : state.before.entrySet()) {
                JsonElement current = read(resolve(root, entry.getKey()));
                if (!current.equals(entry.getValue()) && !current.equals(state.after.get(entry.getKey())))
                    throw new IOException("域名编辑恢复目标已被其他操作修改: " + entry.getKey());
            }
            for (var entry : state.before.entrySet()) write(resolve(root, entry.getKey()), entry.getValue());
        }
        Files.deleteIfExists(journal);
    }

    private static Path resolve(Path root, String relative) throws IOException {
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root) || file.equals(root) || !relative.endsWith(".json")) throw new IOException("事务目标路径无效");
        return file;
    }

    private static JsonElement read(Path file) throws IOException {
        if (Files.notExists(file)) return JsonNull.INSTANCE;
        try { return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)); }
        catch (RuntimeException exception) { throw new IOException("事务目标不是有效 JSON", exception); }
    }

    static void write(Path file, JsonElement data) throws IOException {
        if (data == null || data.isJsonNull()) { Files.deleteIfExists(file); return; }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
        try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static final class State {
        private String world;
        private boolean committed;
        private Map<String, JsonElement> before = new LinkedHashMap<>();
        private Map<String, JsonElement> after = new LinkedHashMap<>();
    }
}
