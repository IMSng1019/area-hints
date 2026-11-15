package areahint.dimensional;

import areahint.AreashintClient;
import areahint.data.DimensionalNameData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端维度域名管理器
 * 负责接收和管理从服务端同步的维度名称
 */
public class ClientDimensionalNameManager {
    private static final Gson GSON = new Gson();
    
    // 默认维度名称
    private static final Map<String, String> DEFAULT_DIMENSIONAL_NAMES = Map.of(
        "minecraft:overworld", "蛮荒大陆",
        "minecraft:the_nether", "恶堕之域", 
        "minecraft:the_end", "终末之地"
    );
    
    // 当前维度名称配置
    private static Map<String, String> dimensionalNames = new HashMap<>(DEFAULT_DIMENSIONAL_NAMES);
    // 默认维度颜色
    private static final Map<String, String> DEFAULT_DIMENSIONAL_COLORS = Map.of(
        "minecraft:overworld", "#FFFFFF",
        "minecraft:the_nether", "#AA0000",
        "minecraft:the_end", "#AA00AA"
    );

    // 当前维度颜色配置
    private static Map<String, String> dimensionalColors = new HashMap<>(DEFAULT_DIMENSIONAL_COLORS);
    // 当前维度签名配置
    private static Map<String, String> dimensionalSignatures = new HashMap<>();
    
    /**
     * 初始化客户端维度域名管理器
     */
    public static void init() {
        AreashintClient.LOGGER.info("客户端维度域名管理器初始化完成");
    }
    
    /**
     * 从服务端接收的JSON数据更新维度名称
     * @param jsonData 服务端发送的JSON数据
     */
    public static void updateFromServer(String jsonData) {
        try {
            List<DimensionalNameData> dataList = GSON.fromJson(jsonData, 
                new TypeToken<List<DimensionalNameData>>(){}.getType());
            
            dimensionalNames.clear();
            dimensionalColors.clear();
            dimensionalSignatures.clear();
            dimensionalNames.putAll(DEFAULT_DIMENSIONAL_NAMES);
            dimensionalColors.putAll(DEFAULT_DIMENSIONAL_COLORS);

            for (DimensionalNameData data : dataList) {
                dimensionalNames.put(data.getDimensionId(), data.getDisplayName());
                if (data.getColor() != null) {
                    dimensionalColors.put(data.getDimensionId(), data.getColor());
                }
                if (data.getSignature() != null && !data.getSignature().trim().isEmpty()) {
                    dimensionalSignatures.put(data.getDimensionId(), data.getSignature().trim());
                }
            }
            
            AreashintClient.LOGGER.info("已从服务端更新维度域名配置: {} 个维度", dimensionalNames.size());
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("更新维度域名配置失败: " + e.getMessage());
        }
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
     * 获取所有维度域名配置
     * @return 维度域名映射
     */
    public static Map<String, String> getAllDimensionalNames() {
        return new HashMap<>(dimensionalNames);
    }
    
    /**
     * 检查维度是否有自定义名称
     * @param dimensionId 维度标识符
     * @return 是否有自定义名称
     */
    public static boolean hasDimensionalName(String dimensionId) {
        return dimensionalNames.containsKey(dimensionId) &&
               !dimensionalNames.get(dimensionId).equals(DEFAULT_DIMENSIONAL_NAMES.get(dimensionId));
    }

    public static String getDimensionalColor(String dimensionId) {
        return dimensionalColors.getOrDefault(dimensionId, null);
    }

    public static Map<String, String> getAllDimensionalColors() {
        return new HashMap<>(dimensionalColors);
    }

    public static String getDimensionalSignature(String dimensionId) {
        return dimensionalSignatures.getOrDefault(dimensionId, null);
    }

    public static boolean isSignedBy(String dimensionId, String playerName) {
        String signature = getDimensionalSignature(dimensionId);
        return signature != null && playerName != null && signature.equals(playerName.trim());
    }

    public static void resetToDefaults() {
        dimensionalNames.clear();
        dimensionalNames.putAll(DEFAULT_DIMENSIONAL_NAMES);
        dimensionalColors.clear();
        dimensionalColors.putAll(DEFAULT_DIMENSIONAL_COLORS);
        dimensionalSignatures.clear();
        AreashintClient.LOGGER.info("客户端维度域名配置已重置为默认值");
    }
} 
