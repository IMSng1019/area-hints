package areahint.config;

import areahint.Areashint;
import areahint.file.FileManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

// 服务端配置只从全局目录加载，客户端世界目录内的副本只用于显示功能限制。
public final class ServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 新增选项只需登记默认值、类型检查和转换规则，加载时会自动补齐或修复。
    private static final Map<String, Option<?>> OPTIONS = new LinkedHashMap<>();
    private static final Option<Boolean> SUBMIT_APPROVAL = booleanOption("submit_approval", true);
    private static final Option<Boolean> FORCE_USE_AREAHINT = booleanOption("force_use_areahint", false);
    private static final Option<GameMode> DEFAULT_GAMEMODE = register(new Option<>(
            "default_gamemode", ServerConfig::serverDefaultGamemode, ServerConfig::isValidGamemode,
            value -> GameMode.byName(value.getAsString()), mode -> new JsonPrimitive(mode.getName())));
    // 完整快照一次替换，登录网络线程不会读到配置修复过程中的中间状态。
    private static volatile JsonObject current = defaults();

    private ServerConfig() { }

    public static void load() {
        Path file = FileManager.getConfigFile("server-config.json");
        JsonObject loaded = new JsonObject();
        boolean changed = false;
        boolean mayWrite = true;
        try {
            if (Files.exists(file)) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                try {
                    JsonElement root = readStrictJson(content);
                    if (!root.isJsonObject()) {
                        throw new IllegalArgumentException("服务端配置的根节点必须为 JSON 对象");
                    }
                    loaded = root.getAsJsonObject();
                } catch (IOException | RuntimeException malformed) {
                    // 无法解析时先保留完整原文件，备份失败则不覆盖管理员的原始配置。
                    Path backup = Files.createTempFile(file.getParent(), "server-config.invalid-", ".json.bak");
                    Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
                    Areashint.LOGGER.warn("服务端配置损坏，原文件已备份至 {}，将补齐默认配置", backup, malformed);
                    changed = true;
                }
            } else {
                changed = true;
            }
        } catch (IOException e) {
            mayWrite = false;
            Areashint.LOGGER.error("读取或备份服务端配置失败，使用默认配置且保留原文件", e);
        }

        // 保留未知字段，避免旧版模组回写时删除未来版本或其他工具添加的配置。
        for (Option<?> option : OPTIONS.values()) {
            JsonElement value = loaded.get(option.key);
            if (value == null || !option.valid.test(value)) {
                loaded.add(option.key, option.defaultJson());
                changed = true;
                if (value != null) {
                    Areashint.LOGGER.warn("服务端配置项 {} 的值不合法，已恢复默认值", option.key);
                }
            }
        }
        current = loaded.deepCopy();
        if (changed && mayWrite) {
            try {
                writeAtomically(file, loaded);
            } catch (IOException e) {
                Areashint.LOGGER.error("回写服务端配置失败，本次启动仍使用已校验的配置", e);
            }
        }
    }

    public static boolean isSubmitApproval() {
        return SUBMIT_APPROVAL.read(current);
    }

    public static boolean isForceUseAreahint() {
        return FORCE_USE_AREAHINT.read(current);
    }

    public static GameMode getDefaultGamemode() {
        return DEFAULT_GAMEMODE.read(current);
    }

    public static JsonObject toJson() {
        JsonObject snapshot = current;
        JsonObject result = new JsonObject();
        // 仅同步模组认识的字段，磁盘中的未知字段仍然原样保留。
        for (String key : OPTIONS.keySet()) {
            result.add(key, snapshot.get(key).deepCopy());
        }
        return result;
    }

    private static GameMode serverDefaultGamemode() {
        MinecraftServer server = Areashint.getServer();
        // 专用服务器的默认游戏模式来自 server.properties，单人世界沿用当前世界默认值。
        return server == null ? GameMode.SURVIVAL : server.getDefaultGameMode();
    }

    private static boolean isValidGamemode(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return false;
        }
        return switch (value.getAsString()) {
            case "survival", "creative", "adventure", "spectator" -> true;
            default -> false;
        };
    }

    private static Option<Boolean> booleanOption(String key, boolean defaultValue) {
        return register(new Option<>(key, () -> defaultValue,
                value -> value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean(),
                JsonElement::getAsBoolean, JsonPrimitive::new));
    }

    private static <T> Option<T> register(Option<T> option) {
        if (OPTIONS.putIfAbsent(option.key, option) != null) {
            throw new IllegalStateException("重复登记服务端配置项: " + option.key);
        }
        return option;
    }

    private static JsonObject defaults() {
        JsonObject result = new JsonObject();
        for (Option<?> option : OPTIONS.values()) {
            result.add(option.key, option.defaultJson());
        }
        return result;
    }

    private static JsonElement readStrictJson(String content) throws IOException {
        // JsonParser 默认宽松解析；显式使用严格读取器，避免非法 JSON 被当成有效配置保留。
        try (JsonReader reader = new JsonReader(new StringReader(content))) {
            reader.setLenient(false);
            JsonElement root = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("服务端配置包含多余 JSON 内容");
            }
            return root;
        }
    }

    private static void writeAtomically(Path file, JsonObject data) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "server-config-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(data) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static final class Option<T> {
        private final String key;
        private final Supplier<T> defaultValue;
        private final Predicate<JsonElement> valid;
        private final Function<JsonElement, T> decode;
        private final Function<T, JsonElement> encode;

        private Option(String key, Supplier<T> defaultValue, Predicate<JsonElement> valid,
                       Function<JsonElement, T> decode, Function<T, JsonElement> encode) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.valid = valid;
            this.decode = decode;
            this.encode = encode;
        }

        private JsonElement defaultJson() {
            return encode.apply(defaultValue.get());
        }

        private T read(JsonObject snapshot) {
            return decode.apply(snapshot.get(key));
        }
    }
}
