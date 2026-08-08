package areahint.world;

import areahint.AreashintClient;
import areahint.file.FileManager;
import areahint.network.ClientWorldNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端世界文件夹管理器
 * 负责获取服务器信息并管理客户端的世界文件夹
 */
public class ClientWorldFolderManager {
    
    // 当前世界文件夹路径
    private static Path currentWorldFolder = null;
    private static String currentWorldName = null;
    private static String currentServerAddress = null;
    private static boolean isInitialized = false;
    
    /**
     * 重置世界文件夹管理器状态
     * 在切换服务器时调用
     */
    public static void resetState() {
        AreashintClient.LOGGER.info("重置ClientWorldFolderManager状态");
        currentWorldFolder = null;
        currentWorldName = null;
        currentServerAddress = null;
        isInitialized = false;
    }
    
    /**
     * 初始化客户端世界文件夹
     * 这个方法应该在客户端连接服务器时调用
     */
    public static void initializeClientWorld() {
        try {
            AreashintClient.LOGGER.info("开始初始化客户端世界文件夹...");
            
            // 获取服务器地址
            String serverAddress = getServerAddress();

            // 临时使用默认世界名称，等待服务端响应
            String tempWorldName = "TempWorld";

            // 先建立临时目录上下文，再发送请求，避免响应先于本地初始化完成
            initializeWorldFolderTemporary(serverAddress, tempWorldName);

            // 请求服务端发送世界名称
            ClientWorldNetworking.requestWorldInfo();
            
            AreashintClient.LOGGER.info("客户端世界文件夹临时初始化完成，等待服务端世界信息");
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("初始化客户端世界文件夹失败", e);
            // 失败时使用根目录作为备用
            currentWorldFolder = FileManager.getConfigFolder();
            isInitialized = true;
        }
    }
    
    /**
     * 完成世界文件夹初始化
     * 这个方法在收到服务端世界信息后调用
     * @param worldName 从服务端获取的世界名称
     */
    public static void finalizeWorldInitialization(String worldName) {
        try {
            AreashintClient.LOGGER.info("finalizeWorldInitialization 被调用，世界名称: '{}'", worldName);
            
            if (currentServerAddress == null) {
                currentServerAddress = getServerAddress();
                AreashintClient.LOGGER.info("获取的服务器地址: '{}'", currentServerAddress);
            }
            
            initializeWorldFolder(currentServerAddress, worldName);
            isInitialized = true;
            
            AreashintClient.LOGGER.info("客户端世界文件夹初始化完成: {}", currentWorldFolder);
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("完成世界文件夹初始化失败", e);
            // 保持当前状态，使用临时文件夹
        }
    }
    
    /**
     * 临时初始化世界文件夹
     * @param serverAddress 服务器地址
     * @param tempWorldName 临时世界名称
     */
    private static void initializeWorldFolderTemporary(String serverAddress, String tempWorldName) throws IOException {
        currentServerAddress = serverAddress;
        currentWorldName = tempWorldName;
        
        // 创建临时路径，之后会被更新
        String cleanServerAddress = sanitizeInput(serverAddress);
        String cleanWorldName = sanitizeInput(tempWorldName);
        String worldFolderName = cleanServerAddress + "_" + cleanWorldName;
        
        Path baseConfigDir = FileManager.getConfigFolder();
        currentWorldFolder = baseConfigDir.resolve(worldFolderName);
    }
    
    /**
     * 初始化世界文件夹
     * @param serverAddress 服务器地址
     * @param worldName 世界名称
     */
    private static void initializeWorldFolder(String serverAddress, String worldName) throws IOException {
        AreashintClient.LOGGER.info("initializeWorldFolder 被调用 - 服务器地址: '{}', 世界名称: '{}'", serverAddress, worldName);
        
        // 清理和验证输入
        String cleanServerAddress = sanitizeInput(serverAddress);
        String cleanWorldName = sanitizeInput(worldName);
        
        AreashintClient.LOGGER.info("清理后 - 服务器地址: '{}', 世界名称: '{}'", cleanServerAddress, cleanWorldName);
        
        // 生成世界文件夹名称：IP地址+世界名称
        String worldFolderName = cleanServerAddress + "_" + cleanWorldName;
        AreashintClient.LOGGER.info("生成的世界文件夹名称: '{}'", worldFolderName);
        
        // 获取基础配置目录
        Path baseConfigDir = FileManager.getConfigFolder();
        Path worldFolderPath = baseConfigDir.resolve(worldFolderName);
        
        // 创建世界文件夹（如果不存在）
        if (Files.notExists(worldFolderPath)) {
            Files.createDirectories(worldFolderPath);
            AreashintClient.LOGGER.info("已创建客户端世界文件夹: {}", worldFolderPath);
            
            // 创建默认文件（客户端不创建区域文件，等待服务端同步）
            createDefaultClientWorldFiles(worldFolderPath);
        } else {
            AreashintClient.LOGGER.info("客户端世界文件夹已存在: {}", worldFolderPath);
        }
        
        // 设置当前世界文件夹
        currentWorldFolder = worldFolderPath;
        currentWorldName = worldName;
        currentServerAddress = serverAddress;
    }
    
    /**
     * 创建默认的客户端世界文件
     * @param worldFolderPath 世界文件夹路径
     */
    private static void createDefaultClientWorldFiles(Path worldFolderPath) throws IOException {
        // 客户端只创建维度域名文件，区域文件等待服务端同步
        createDefaultDimensionalNamesFile(worldFolderPath.resolve("dimensional_names.json"));
        
        AreashintClient.LOGGER.info("已创建客户端世界默认文件");
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
            AreashintClient.LOGGER.debug("已创建默认维度域名文件: {}", filePath.getFileName());
        }
    }
    
    /**
     * 获取服务器地址
     * @return 服务器地址
     */
    private static String getServerAddress() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            
            AreashintClient.LOGGER.info("开始获取服务器地址...");
            
            if (client.isIntegratedServerRunning()) {
                // 单人游戏，直接返回localhost，世界名称在后面拼接
                AreashintClient.LOGGER.info("检测到单人游戏，返回localhost");
                return "localhost";
            }
            
            // 多人游戏
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo != null) {
                AreashintClient.LOGGER.info("服务器信息 - 地址: '{}', 名称: '{}'", 
                    serverInfo.address, serverInfo.name);
                
                if (serverInfo.address != null) {
                    // 移除端口号（如果有的话）
                    String address = serverInfo.address;
                    int colonIndex = address.lastIndexOf(':');
                    if (colonIndex > 0) {
                        address = address.substring(0, colonIndex);
                    }
                    AreashintClient.LOGGER.info("使用服务器地址: '{}'", address);
                    return address;
                }
            }
            
            // 备用
            AreashintClient.LOGGER.warn("无法获取服务器地址，使用 'unknown'");
            return "unknown";
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("获取服务器地址失败，使用默认地址", e);
            return "unknown";
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
            AreashintClient.LOGGER.warn("客户端世界文件夹未初始化，使用备用路径");
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
            AreashintClient.LOGGER.warn("客户端世界文件夹未初始化，使用备用路径");
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
     * 清理输入字符串，确保文件夹名称安全
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    private static String sanitizeInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "Unknown";
        }
        
        AreashintClient.LOGGER.info("客户端清理输入字符串 - 原始: '{}'", input);
        
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
        
        AreashintClient.LOGGER.info("客户端清理输入字符串 - 结果: '{}'", cleaned);
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
        isInitialized = false;
        AreashintClient.LOGGER.info("客户端世界文件夹管理器已重置");
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
        return isInitialized;
    }
}
