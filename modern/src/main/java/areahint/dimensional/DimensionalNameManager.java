package areahint.dimensional;

import areahint.Areashint;
import areahint.data.DimensionalNameData;
import areahint.file.FileManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 维度域名管理器
 * 负责管理维度名称的配置、加载和保存
 */
public class DimensionalNameManager {
    private static final String DIMENSIONAL_NAMES_FILE = "dimensional_names.json";
    private static final Gson GSON = new Gson();
    
    // 默认维度名称
    private static final Map<String, String> DEFAULT_DIMENSIONAL_NAMES = Map.of(
        "minecraft:overworld", "蛮荒大陆",
        "minecraft:the_nether", "恶堕之域", 
        "minecraft:the_end", "终末之地"
    );
    
    // 当前维度名称配置
    private static Map<String, String> dimensionalNames = new HashMap<>();
    // 当前维度颜色配置
    private static Map<String, String> dimensionalColors = new HashMap<>();
    // 当前维度签名配置
    private static Map<String, String> dimensionalSignatures = new HashMap<>();
    
    /**
     * 初始化维度域名管理器
     */
    public static void init() {
        loadDimensionalNames();
        Areashint.LOGGER.info("维度域名管理器初始化完成");
    }
    
    /**
     * 加载维度域名配置
     */
    public static void loadDimensionalNames() {
        Path configFile = getDimensionalNamesFile();
        
        try {
            if (Files.notExists(configFile)) {
                // 创建默认配置文件
                createDefaultDimensionalNamesFile();
            }
            
            String json = Files.readString(configFile, StandardCharsets.UTF_8);
            List<DimensionalNameData> dataList = GSON.fromJson(json, 
                new TypeToken<List<DimensionalNameData>>(){}.getType());
            
            dimensionalNames.clear();
            dimensionalColors.clear();
            dimensionalSignatures.clear();
            for (DimensionalNameData data : dataList) {
                dimensionalNames.put(data.getDimensionId(), data.getDisplayName());
                if (data.getColor() != null) {
                    dimensionalColors.put(data.getDimensionId(), data.getColor());
                }
                if (data.getSignature() != null && !data.getSignature().trim().isEmpty()) {
                    dimensionalSignatures.put(data.getDimensionId(), data.getSignature().trim());
                }
            }
            
            Areashint.LOGGER.info("已加载维度域名配置: {} 个维度", dimensionalNames.size());
            
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            Areashint.LOGGER.error("加载维度域名配置失败，使用默认配置: " + e.getMessage());
            dimensionalNames.clear();
            dimensionalNames.putAll(DEFAULT_DIMENSIONAL_NAMES);
            dimensionalColors.clear();
            dimensionalSignatures.clear();
        }
    }
    
    /**
     * 保存维度域名配置
     * @return 是否保存成功
     */
    public static boolean saveDimensionalNames() {
        Path configFile = getDimensionalNamesFile();
        
        try {
            List<DimensionalNameData> dataList = new ArrayList<>();
            for (Map.Entry<String, String> entry : dimensionalNames.entrySet()) {
                dataList.add(new DimensionalNameData(entry.getKey(), entry.getValue(),
                    dimensionalColors.get(entry.getKey()),
                    dimensionalSignatures.get(entry.getKey())));
            }
            
            String json = GSON.toJson(dataList);
            Files.writeString(configFile, json, StandardCharsets.UTF_8);
            
            Areashint.LOGGER.info("维度域名配置已保存");
            return true;
            
        } catch (IOException e) {
            Areashint.LOGGER.error("保存维度域名配置失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建默认维度域名配置文件
     */
    private static void createDefaultDimensionalNamesFile() throws IOException {
        List<DimensionalNameData> defaultData = new ArrayList<>();
        for (Map.Entry<String, String> entry : DEFAULT_DIMENSIONAL_NAMES.entrySet()) {
            defaultData.add(new DimensionalNameData(entry.getKey(), entry.getValue()));
        }
        
        String json = GSON.toJson(defaultData);
        Path configFile = getDimensionalNamesFile();
        
        // 确保目录存在
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, json, StandardCharsets.UTF_8);
        
        // 初始化当前配置
        dimensionalNames.clear();
        dimensionalNames.putAll(DEFAULT_DIMENSIONAL_NAMES);
        dimensionalColors.clear();
        dimensionalSignatures.clear();
        
        Areashint.LOGGER.info("已创建默认维度域名配置文件: " + configFile);
    }
    
    /**
     * 获取维度域名配置文件路径
     * @return 配置文件路径
     */
    private static Path getDimensionalNamesFile() {
        // 使用世界文件夹管理器获取路径
        return areahint.world.WorldFolderManager.getWorldDimensionalNamesFile();
    }
    
    /**
     * 获取维度的显示名称
     * @param dimensionId 维度标识符
     * @return 维度显示名称
     */
    public static String getDimensionalName(String dimensionId) {
        return dimensionalNames.getOrDefault(dimensionId, 
            DEFAULT_DIMENSIONAL_NAMES.getOrDefault(dimensionId, dimensionId));
    }
    
    /**
     * 设置维度的显示名称
     * @param dimensionId 维度标识符
     * @param displayName 显示名称
     */
    public static void setDimensionalName(String dimensionId, String displayName) {
        dimensionalNames.put(dimensionId, displayName);
        Areashint.LOGGER.info("已更新维度域名: {} -> {}", dimensionId, displayName);
    }

    /**
     * 设置维度域名，并在没有签名时记录首次创建者。
     * @param dimensionId 维度标识符
     * @param displayName 显示名称
     * @param signature 创建者签名
     */
    public static void setDimensionalName(String dimensionId, String displayName, String signature) {
        setDimensionalName(dimensionId, displayName);
        setDimensionalSignatureIfAbsent(dimensionId, signature);
    }
    
    /**
     * 获取所有维度域名配置
     * @return 维度域名映射
     */
    public static Map<String, String> getAllDimensionalNames() {
        return new HashMap<>(dimensionalNames);
    }
    
    /**
     * 获取所有配置的维度ID列表
     * @return 维度ID集合
     */
    public static Set<String> getAllDimensionIds() {
        return new HashSet<>(dimensionalNames.keySet());
    }
    
    public static boolean hasDimensionalName(String dimensionId) {
        return dimensionalNames.containsKey(dimensionId);
    }

    public static String getDimensionalColor(String dimensionId) {
        return dimensionalColors.getOrDefault(dimensionId, null);
    }

    public static void setDimensionalColor(String dimensionId, String color) {
        dimensionalColors.put(dimensionId, color);
        Areashint.LOGGER.info("已更新维度域名颜色: {} -> {}", dimensionId, color);
    }

    public static Map<String, String> getAllDimensionalColors() {
        return new HashMap<>(dimensionalColors);
    }

    public static String getDimensionalSignature(String dimensionId) {
        return dimensionalSignatures.getOrDefault(dimensionId, null);
    }

    public static void setDimensionalSignature(String dimensionId, String signature) {
        String cleanedSignature = cleanSignature(signature);
        if (cleanedSignature == null) {
            dimensionalSignatures.remove(dimensionId);
            Areashint.LOGGER.info("已清除维度域名签名: {}", dimensionId);
            return;
        }
        dimensionalSignatures.put(dimensionId, cleanedSignature);
        Areashint.LOGGER.info("已更新维度域名签名: {} -> {}", dimensionId, cleanedSignature);
    }

    /**
     * 只在维度域名还没有签名时写入签名，避免后续改名覆盖原创建者。
     */
    public static void setDimensionalSignatureIfAbsent(String dimensionId, String signature) {
        if (getDimensionalSignature(dimensionId) != null) {
            return;
        }
        String cleanedSignature = cleanSignature(signature);
        if (cleanedSignature != null) {
            dimensionalSignatures.put(dimensionId, cleanedSignature);
            Areashint.LOGGER.info("已记录维度域名首次签名: {} -> {}", dimensionId, cleanedSignature);
        }
    }

    public static Map<String, String> getAllDimensionalSignatures() {
        return new HashMap<>(dimensionalSignatures);
    }

    public static void resetToDefaults() {
        dimensionalNames.clear();
        dimensionalNames.putAll(DEFAULT_DIMENSIONAL_NAMES);
        dimensionalColors.clear();
        dimensionalSignatures.clear();
        Areashint.LOGGER.info("维度域名配置已重置为默认值");
    }

    private static String cleanSignature(String signature) {
        if (signature == null) {
            return null;
        }
        String cleanedSignature = signature.trim();
        return cleanedSignature.isEmpty() ? null : cleanedSignature;
    }
} 
