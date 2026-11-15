package areahint.world;

import areahint.Areashint;
import areahint.file.FileManager;
import areahint.network.Packets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * 世界文件夹管理器
 * 负责管理不同世界的独立文件夹和文件路径
 */
public class WorldFolderManager {
    
    // 当前世界文件夹路径
    private static Path currentWorldFolder = null;
    private static String currentWorldName = null;
    private static String currentServerAddress = null;
    
    // 世界文件夹名称验证模式（确保文件夹名称安全）
    private static final Pattern SAFE_FOLDER_NAME = Pattern.compile("[a-zA-Z0-9._-]+");
    
    /**
     * 初始化世界文件夹管理器
     * 这个方法应该在服务端每次启动时调用
     * @param server 服务器实例
     */
    public static void initializeServerWorld(MinecraftServer server) {
        try {
            // 获取服务器世界名称
            String worldName = getServerWorldName(server);
            String serverAddress = "localhost"; // 服务端默认使用localhost
            
            initializeWorldFolder(serverAddress, worldName);
            createDatabaseFolders(currentWorldFolder, server);

            Areashint.LOGGER.info("服务端世界文件夹初始化完成: {}", currentWorldFolder);
            
        } catch (Exception e) {
            Areashint.LOGGER.error("初始化服务端世界文件夹失败", e);
            // 失败时使用根目录作为备用
            currentWorldFolder = FileManager.getConfigFolder();
        }
    }
    
    /**
     * 初始化客户端世界文件夹
     * 这个方法应该在客户端连接服务器时调用
     * @param serverAddress 服务器地址
     * @param worldName 世界名称
     */
    public static void initializeClientWorld(String serverAddress, String worldName) {
        try {
            initializeWorldFolder(serverAddress, worldName);
            Areashint.LOGGER.info("客户端世界文件夹初始化完成: {}", currentWorldFolder);
            
        } catch (Exception e) {
            Areashint.LOGGER.error("初始化客户端世界文件夹失败", e);
            // 失败时使用根目录作为备用
            currentWorldFolder = FileManager.getConfigFolder();
        }
    }
    
    /**
     * 初始化世界文件夹
     * @param serverAddress 服务器地址
     * @param worldName 世界名称
     */
    private static void initializeWorldFolder(String serverAddress, String worldName) throws IOException {
        Areashint.LOGGER.info("服务端 initializeWorldFolder 被调用 - 服务器地址: '{}', 世界名称: '{}'", serverAddress, worldName);
        
        // 清理和验证输入
        String cleanServerAddress = sanitizeInput(serverAddress);
        String cleanWorldName = sanitizeInput(worldName);
        
        Areashint.LOGGER.info("服务端清理后 - 服务器地址: '{}', 世界名称: '{}'", cleanServerAddress, cleanWorldName);
        
        // 生成世界文件夹名称：IP地址+世界名称
        String worldFolderName = cleanServerAddress + "_" + cleanWorldName;
        Areashint.LOGGER.info("服务端生成的世界文件夹名称: '{}'", worldFolderName);
        
        // 获取基础配置目录
        Path baseConfigDir = FileManager.getConfigFolder();
        Path worldFolderPath = baseConfigDir.resolve(worldFolderName);
        
        // 创建世界文件夹（如果不存在）
        if (Files.notExists(worldFolderPath)) {
            Files.createDirectories(worldFolderPath);
            Areashint.LOGGER.info("已创建世界文件夹: {}", worldFolderPath);

            // 创建默认文件
            createDefaultWorldFiles(worldFolderPath);
        } else {
            Areashint.LOGGER.info("世界文件夹已存在: {}", worldFolderPath);
        }

        createDatabaseFolders(worldFolderPath);
        
        // 设置当前世界文件夹
        currentWorldFolder = worldFolderPath;
        currentWorldName = worldName;
        currentServerAddress = serverAddress;
    }
    
    /**
     * 创建默认的世界文件
     * @param worldFolderPath 世界文件夹路径
     */
    private static void createDefaultWorldFiles(Path worldFolderPath) throws IOException {
        // 创建维度文件
        createEmptyAreaFile(worldFolderPath.resolve(Areashint.OVERWORLD_FILE));
        createEmptyAreaFile(worldFolderPath.resolve(Areashint.NETHER_FILE));
        createEmptyAreaFile(worldFolderPath.resolve(Areashint.END_FILE));
        
        // 创建维度域名文件
        createDefaultDimensionalNamesFile(worldFolderPath.resolve("dimensional_names.json"));
        
        Areashint.LOGGER.info("已创建世界默认文件");
    }
    
    /**
     * 创建空的区域文件
     */
    private static void createEmptyAreaFile(Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            Files.writeString(filePath, "[]");
            Areashint.LOGGER.debug("已创建空区域文件: {}", filePath.getFileName());
        }
    }
    
    /**
     * 创建默认的维度域名文件
     */
    private static void createDefaultDimensionalNamesFile(Path filePath) throws IOException {
        if (Files.notExists(filePath)) {
            String defaultDimensionalNames = "[\n" +
                "  {\n" +
                "    \"dimensionId\": \"minecraft:overworld\",\n" +
                "    \"displayName\": \"蛮荒大陆\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dimensionId\": \"minecraft:the_nether\",\n" +
                "    \"displayName\": \"恶堕之域\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"dimensionId\": \"minecraft:the_end\",\n" +
                "    \"displayName\": \"终末之地\"\n" +
                "  }\n" +
                "]";
            
            Files.writeString(filePath, defaultDimensionalNames);
            Areashint.LOGGER.debug("已创建默认维度域名文件: {}", filePath.getFileName());
        }
    }
    
    /**
     * 获取当前世界的维度文件路径
     * @param dimensionFileName 维度文件名
     * @return 维度文件路径
     */
    public static Path getWorldDimensionFile(String dimensionFileName) {
        if (currentWorldFolder == null) {
            // 如果世界文件夹未初始化，返回根目录下的文件（备用模式）
            Areashint.LOGGER.warn("世界文件夹未初始化，使用备用路径");
            return FileManager.getDimensionFile(dimensionFileName);
        }
        return currentWorldFolder.resolve(dimensionFileName);
    }
    
    /**
     * 获取当前世界的维度域名文件路径
     * @return 维度域名文件路径
     */
    public static Path getWorldDimensionalNamesFile() {
        if (currentWorldFolder == null) {
            // 如果世界文件夹未初始化，返回根目录下的文件（备用模式）
            Areashint.LOGGER.warn("世界文件夹未初始化，使用备用路径");
            return FileManager.getConfigFile("dimensional_names.json");
        }
        return currentWorldFolder.resolve("dimensional_names.json");
    }
    
    /**
     * 获取当前世界文件夹路径
     * @return 世界文件夹路径，如果未初始化则返回根配置目录
     */
    public static Path getCurrentWorldFolder() {
        if (currentWorldFolder == null) {
            return FileManager.getConfigFolder();
        }
        return currentWorldFolder;
    }

    /**
     * 创建数据库文件夹结构
     * @param worldFolderPath 世界文件夹路径
     */
    public static void createDatabaseFolders(Path worldFolderPath) throws IOException {
        Path databaseFolder = worldFolderPath.resolve("database");
        Files.createDirectories(databaseFolder);
        Files.createDirectories(databaseFolder.resolve(Packets.getDatabaseFolderForDimension(Packets.DIMENSION_OVERWORLD)));
        Files.createDirectories(databaseFolder.resolve(Packets.getDatabaseFolderForDimension(Packets.DIMENSION_NETHER)));
        Files.createDirectories(databaseFolder.resolve(Packets.getDatabaseFolderForDimension(Packets.DIMENSION_END)));
    }

    /**
     * 根据服务端已加载维度创建描述数据库目录。
     * @param worldFolderPath 世界文件夹路径
     * @param server 服务器实例
     */
    public static void createDatabaseFolders(Path worldFolderPath, MinecraftServer server) throws IOException {
        createDatabaseFolders(worldFolderPath);
        if (server == null) {
            return;
        }
        Path databaseFolder = worldFolderPath.resolve("database");
        for (ServerLevel world : server.getAllLevels()) {
            Identifier dimensionId = world.dimension().identifier();
            String dimensionFolder = getDatabaseFolderNameForDimension(Packets.convertDimensionPathToType(dimensionId.getPath()));
            if (dimensionFolder == null || dimensionFolder.isBlank() || "Unknown".equals(dimensionFolder)) {
                dimensionFolder = sanitizeInput(dimensionId.toString());
            }
            Files.createDirectories(databaseFolder.resolve(dimensionFolder));
        }
    }

    /**
     * 获取当前世界数据库根目录
     * @return 数据库根目录路径
     */
    public static Path getWorldDatabaseFolder() {
        return getCurrentWorldFolder().resolve("database");
    }

    /**
     * 获取当前世界指定维度的数据库目录
     * @param dimensionType 维度类型
     * @return 维度数据库目录路径
     */
    public static Path getWorldDimensionDatabaseFolder(String dimensionType) {
        return getWorldDatabaseFolder().resolve(getDatabaseFolderNameForDimension(dimensionType));
    }

    /**
     * 获取描述数据库使用的安全维度目录名。
     * @param dimensionType 维度类型
     * @return 安全维度目录名
     */
    public static String getDatabaseFolderNameForDimension(String dimensionType) {
        String vanillaFolderName = Packets.getDatabaseFolderForDimension(dimensionType);
        if (vanillaFolderName != null) {
            return vanillaFolderName;
        }
        return sanitizeInput(dimensionType);
    }
    
    /**
     * 获取服务器世界名称
     * @param server 服务器实例
     * @return 世界名称
     */
    private static String getServerWorldName(MinecraftServer server) {
        try {
            Areashint.LOGGER.info("开始获取服务器世界名称...");
            
            // 尝试获取世界名称（优先）
            String worldName = server.getWorldData().getLevelName();
            Areashint.LOGGER.info("getLevelName() 返回: '{}'", worldName);
            if (worldName != null && !worldName.trim().isEmpty() && !worldName.equals("world")) {
                String finalName = worldName.trim();
                Areashint.LOGGER.info("使用世界名称: '{}'", finalName);
                return finalName;
            }
            
            // 尝试获取服务器MOTD
            String serverMotd = server.getMotd();
            Areashint.LOGGER.info("getServerMotd() 返回: '{}'", serverMotd);
            if (serverMotd != null && !serverMotd.trim().isEmpty()) {
                String finalName = serverMotd.trim();
                Areashint.LOGGER.info("使用服务器MOTD: '{}'", finalName);
                return finalName;
            }
            
            // 尝试获取服务器运行目录名称
            try {
                String runningDir = System.getProperty("user.dir");
                if (runningDir != null) {
                    String dirName = java.nio.file.Paths.get(runningDir).getFileName().toString();
                    Areashint.LOGGER.info("运行目录名称: '{}'", dirName);
                    if (!dirName.equals("server") && !dirName.trim().isEmpty()) {
                        String finalName = dirName.trim();
                        Areashint.LOGGER.info("使用运行目录名称: '{}'", finalName);
                        return finalName;
                    }
                }
            } catch (Exception e) {
                Areashint.LOGGER.warn("获取运行目录名称失败", e);
            }
            
            // 尝试从存档路径获取名称
            try {
                String savePath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toString();
                Areashint.LOGGER.info("存档路径: '{}'", savePath);
                
                // 从路径中提取世界名称
                if (savePath != null && !savePath.trim().isEmpty()) {
                    String pathName = java.nio.file.Paths.get(savePath).getFileName().toString();
                    if (!pathName.equals("world") && !pathName.trim().isEmpty()) {
                        String finalName = pathName.trim();
                        Areashint.LOGGER.info("使用路径名称: '{}'", finalName);
                        return finalName;
                    }
                }
            } catch (Exception e) {
                Areashint.LOGGER.warn("从存档路径获取名称失败", e);
            }
            
            // 默认名称
            Areashint.LOGGER.info("使用默认名称: 'Server'");
            return "Server";
            
        } catch (Exception e) {
            Areashint.LOGGER.error("获取服务器世界名称失败，使用默认名称", e);
            return "Server";
        }
    }
    
    /**
     * 清理输入字符串，确保文件夹名称安全
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    private static String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Unknown";
        }
        
        Areashint.LOGGER.info("清理输入字符串 - 原始: '{}'", input);
        
        // 移除文件系统不安全字符，但保留中文字符
        // Windows不允许的字符: < > : " | ? * \ /
        // 其他控制字符和换行符也要移除
        String cleaned = input.replaceAll("[<>:\"|?*\\\\/\\r\\n\\t\\x00-\\x1f\\x7f]", "_");
        
        // 移除开头和结尾的空格和点（Windows文件夹名称限制）
        cleaned = cleaned.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");
        
        // 限制长度（考虑到中文字符可能占用更多字节）
        if (cleaned.length() > 50) {
            cleaned = cleaned.substring(0, 50);
        }
        
        // 确保不为空
        if (cleaned.isEmpty()) {
            cleaned = "Unknown";
        }
        
        Areashint.LOGGER.info("清理输入字符串 - 结果: '{}'", cleaned);
        return cleaned;
    }
    
    /**
     * 重置世界文件夹管理器
     * 用于切换世界或断开连接时
     */
    public static void reset() {
        currentWorldFolder = null;
        currentWorldName = null;
        currentServerAddress = null;
        Areashint.LOGGER.info("世界文件夹管理器已重置");
    }
    
    /**
     * 获取当前世界名称
     * @return 当前世界名称
     */
    public static String getCurrentWorldName() {
        return currentWorldName;
    }
    
    /**
     * 获取当前服务器地址
     * @return 当前服务器地址
     */
    public static String getCurrentServerAddress() {
        return currentServerAddress;
    }
    
    /**
     * 检查世界文件夹是否已初始化
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return currentWorldFolder != null;
    }
} 