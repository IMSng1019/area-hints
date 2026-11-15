package areahint.util;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.ArrayList;

/**
 * 联合域名处理器
 * 处理域名的显示名称逻辑，优先显示surfacename，否则显示name
 */
public class SurfaceNameHandler {
    
    /**
     * 获取域名的显示名称
     * @param areaData 域名JSON数据
     * @return 显示名称（优先surfacename，否则name）
     */
    public static String getDisplayName(JsonObject areaData) {
        if (areaData == null) {
            return "未知域名";
        }
        
        // 优先检查surfacename
        if (areaData.has("surfacename") && 
            !areaData.get("surfacename").isJsonNull() && 
            !areaData.get("surfacename").getAsString().trim().isEmpty()) {
            return areaData.get("surfacename").getAsString();
        }
        
        // fallback到name
        if (areaData.has("name") && 
            !areaData.get("name").isJsonNull()) {
            return areaData.get("name").getAsString();
        }
        
        return "未知域名";
    }
    
    /**
     * 获取域名的实际名称（用于ID识别）
     * @param areaData 域名JSON数据
     * @return 实际域名名称（name字段）
     */
    public static String getActualName(JsonObject areaData) {
        if (areaData == null) {
            return null;
        }
        
        if (areaData.has("name") && 
            !areaData.get("name").isJsonNull()) {
            return areaData.get("name").getAsString();
        }
        
        return null;
    }
    
    /**
     * 获取域名的联合名称
     * @param areaData 域名JSON数据
     * @return 联合域名名称（surfacename字段）
     */
    public static String getSurfaceName(JsonObject areaData) {
        if (areaData == null) {
            return null;
        }
        
        if (areaData.has("surfacename") && 
            !areaData.get("surfacename").isJsonNull() && 
            !areaData.get("surfacename").getAsString().trim().isEmpty()) {
            return areaData.get("surfacename").getAsString();
        }
        
        return null;
    }
    
    /**
     * 设置域名的联合名称
     * @param areaData 域名JSON数据
     * @param surfaceName 联合域名名称
     */
    public static void setSurfaceName(JsonObject areaData, String surfaceName) {
        if (areaData == null) {
            return;
        }
        
        if (surfaceName == null || surfaceName.trim().isEmpty()) {
            areaData.addProperty("surfacename", (String) null);
        } else {
            areaData.addProperty("surfacename", surfaceName.trim());
        }
    }
    
    /**
     * 检查域名名称（name字段）是否重复
     * @param allAreas 所有域名数据列表
     * @param newAreaName 新域名的name字段
     * @param excludeArea 排除的域名（用于更新时排除自身）
     * @return 是否重复
     */
    public static boolean isDuplicateAreaName(List<JsonObject> allAreas, 
                                            String newAreaName, 
                                            JsonObject excludeArea) {
        if (newAreaName == null || newAreaName.trim().isEmpty()) {
            return false;
        }
        
        String trimmedName = newAreaName.trim();
        
        for (JsonObject area : allAreas) {
            // 排除指定的域名（通常是正在更新的域名自身）
            if (excludeArea != null && area.equals(excludeArea)) {
                continue;
            }
            
            String existingName = getActualName(area);
            if (existingName != null && existingName.equals(trimmedName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查域名名称（name字段）是否重复（不排除任何域名）
     * @param allAreas 所有域名数据列表
     * @param newAreaName 新域名的name字段
     * @return 是否重复
     */
    public static boolean isDuplicateAreaName(List<JsonObject> allAreas, String newAreaName) {
        return isDuplicateAreaName(allAreas, newAreaName, null);
    }
    
    /**
     * 格式化显示域名信息
     * @param areaData 域名JSON数据
     * @return 格式化的域名信息字符串
     */
    public static String formatAreaInfo(JsonObject areaData) {
        if (areaData == null) {
            return "§c无效的域名数据";
        }
        
        String displayName = getDisplayName(areaData);
        String actualName = getActualName(areaData);
        String surfaceName = getSurfaceName(areaData);
        
        StringBuilder info = new StringBuilder();
        info.append("§6").append(displayName);
        
        // 如果有联合域名且与实际名称不同，显示实际名称
        if (surfaceName != null && !surfaceName.equals(actualName)) {
            info.append(" §7(实际域名: ").append(actualName).append(")");
        }
        
        return info.toString();
    }
    
    /**
     * 根据显示名称查找域名（优先匹配surfacename，然后匹配name）
     * @param allAreas 所有域名数据列表
     * @param searchName 搜索的名称
     * @return 匹配的域名列表
     */
    public static List<JsonObject> findAreasByDisplayName(List<JsonObject> allAreas, String searchName) {
        List<JsonObject> matches = new ArrayList<>();
        
        if (searchName == null || searchName.trim().isEmpty()) {
            return matches;
        }
        
        String trimmedSearch = searchName.trim();
        
        // 首先精确匹配surfacename
        for (JsonObject area : allAreas) {
            String surfaceName = getSurfaceName(area);
            if (surfaceName != null && surfaceName.equals(trimmedSearch)) {
                matches.add(area);
            }
        }
        
        // 如果没有匹配到surfacename，再匹配name
        if (matches.isEmpty()) {
            for (JsonObject area : allAreas) {
                String actualName = getActualName(area);
                if (actualName != null && actualName.equals(trimmedSearch)) {
                    matches.add(area);
                }
            }
        }
        
        return matches;
    }
    
    /**
     * 根据实际名称查找域名（只匹配name字段）
     * @param allAreas 所有域名数据列表
     * @param actualName 实际域名名称
     * @return 匹配的域名，如果没找到返回null
     */
    public static JsonObject findAreaByActualName(List<JsonObject> allAreas, String actualName) {
        if (actualName == null || actualName.trim().isEmpty()) {
            return null;
        }
        
        String trimmedName = actualName.trim();
        
        for (JsonObject area : allAreas) {
            String areaActualName = getActualName(area);
            if (areaActualName != null && areaActualName.equals(trimmedName)) {
                return area;
            }
        }
        
        return null;
    }
} 