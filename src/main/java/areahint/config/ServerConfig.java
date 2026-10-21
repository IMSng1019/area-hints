package areahint.config;

import areahint.Areashint;
import areahint.file.FileManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// 服务端配置只从全局目录加载，客户端世界目录内的副本只用于显示功能限制。
public final class ServerConfig {
    private static boolean submitApproval = true;

    private ServerConfig() { }

    public static void load() {
        submitApproval = true;
        Path file = FileManager.getConfigFile("server-config.json");
        try {
            if (Files.notExists(file)) {
                Files.writeString(file, "{\n  \"submit_approval\": true\n}\n", StandardCharsets.UTF_8);
                return;
            }
            JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
            if (root.isJsonObject()) {
                JsonElement option = root.getAsJsonObject().get("submit_approval");
                if (option != null && option.isJsonPrimitive() && option.getAsJsonPrimitive().isBoolean()) {
                    submitApproval = option.getAsBoolean();
                }
            }
        } catch (Exception e) {
            Areashint.LOGGER.error("读取服务端配置失败，使用默认审批设置", e);
        }
    }

    public static boolean isSubmitApproval() {
        return submitApproval;
    }

    public static JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("submit_approval", submitApproval);
        return result;
    }
}
