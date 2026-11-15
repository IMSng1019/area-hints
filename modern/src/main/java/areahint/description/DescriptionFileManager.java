package areahint.description;

import areahint.world.WorldFolderManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 域名描述文件管理器。
 */
public final class DescriptionFileManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String UNKNOWN_NAME = "Unknown";
    private static final String FILE_EXTENSION = ".json";

    private DescriptionFileManager() {
    }

    public static String sanitizeDescriptionFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return UNKNOWN_NAME;
        }

        String sanitized = name.replaceAll("[<>:\"|?*\\\\/\\r\\n\\t\\x00-\\x1f\\x7f]", "_");
        sanitized = sanitized.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");

        if (sanitized.isEmpty()) {
            return UNKNOWN_NAME;
        }
        return isWindowsReservedDeviceName(sanitized) ? sanitized + "_" : sanitized;
    }

    private static boolean isWindowsReservedDeviceName(String fileName) {
        String baseName = fileName;
        int dotIndex = baseName.indexOf('.');
        if (dotIndex >= 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        String upper = baseName.toUpperCase(java.util.Locale.ROOT);
        return upper.equals("CON") || upper.equals("PRN") || upper.equals("AUX") || upper.equals("NUL")
            || upper.matches("COM[1-9]") || upper.matches("LPT[1-9]");
    }

    public static Path getAreaDescriptionFile(String dimensionType, String surfaceName) {
        return WorldFolderManager.getWorldDimensionDatabaseFolder(dimensionType)
            .resolve(sanitizeDescriptionFileName(surfaceName) + FILE_EXTENSION);
    }

    public static Path getAreaDescriptionFile(Path databaseFolder, String dimensionType, String surfaceName) {
        return databaseFolder
            .resolve(WorldFolderManager.getDatabaseFolderNameForDimension(dimensionType))
            .resolve(sanitizeDescriptionFileName(surfaceName) + FILE_EXTENSION);
    }

    public static Path getDimensionalDescriptionFile(String dimensionalDisplayName) {
        return getDimensionalDescriptionFile(WorldFolderManager.getWorldDatabaseFolder(), dimensionalDisplayName);
    }

    public static Path getDimensionalDescriptionFile(Path databaseFolder, String dimensionalDisplayName) {
        return databaseFolder.resolve(sanitizeDescriptionFileName(dimensionalDisplayName) + FILE_EXTENSION);
    }

    public static DescriptionData readDescription(Path filePath) {
        if (Files.notExists(filePath)) {
            return null;
        }

        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            return GSON.fromJson(json, DescriptionData.class);
        } catch (IOException | JsonSyntaxException e) {
            return null;
        }
    }

    public static boolean writeDescription(Path filePath, DescriptionData data) {
        if (data == null) {
            return false;
        }

        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, GSON.toJson(data), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteDescription(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
