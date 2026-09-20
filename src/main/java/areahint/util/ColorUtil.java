package areahint.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

/**
 * 颜色工具类
 * 提供颜色验证、转换和预定义颜色常量
 */
public class ColorUtil {
    
    // 特殊闪烁颜色模式常量
    public static final String FLASH_BW_ALL = "FLASH_BW_ALL";           // 整体黑白闪烁
    public static final String FLASH_RAINBOW_ALL = "FLASH_RAINBOW_ALL"; // 整体彩虹闪烁
    public static final String FLASH_BW_CHAR = "FLASH_BW_CHAR";         // 单字黑白闪烁
    public static final String FLASH_RAINBOW_CHAR = "FLASH_RAINBOW_CHAR"; // 单字彩虹闪烁

    private static final Set<String> FLASH_MODES = new HashSet<>(Arrays.asList(
        FLASH_BW_ALL, FLASH_RAINBOW_ALL, FLASH_BW_CHAR, FLASH_RAINBOW_CHAR
    ));

    /**
     * 判断是否为特殊闪烁颜色模式
     */
    public static boolean isFlashColor(String color) {
        return color != null && FLASH_MODES.contains(color);
    }

    // 预定义颜色映射
    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    
    static {
        COLOR_MAP.put("白色", "#FFFFFF");
        COLOR_MAP.put("红色", "#FF0000");
        COLOR_MAP.put("粉红色", "#FF69B4");
        COLOR_MAP.put("橙色", "#FFA500");
        COLOR_MAP.put("黄色", "#FFFF00");
        COLOR_MAP.put("棕色", "#8B4513");
        COLOR_MAP.put("浅绿色", "#90EE90");
        COLOR_MAP.put("深绿色", "#006400");
        COLOR_MAP.put("浅蓝色", "#87CEEB");
        COLOR_MAP.put("深蓝色", "#000080");
        COLOR_MAP.put("浅紫色", "#DDA0DD");
        COLOR_MAP.put("紫色", "#800080");
        COLOR_MAP.put("灰色", "#808080");
        COLOR_MAP.put("黑色", "#000000");
    }
    
    /**
     * 验证颜色格式是否正确
     * @param color 颜色字符串
     * @return 是否有效
     */
    public static boolean isValidColor(String color) {
        if (color == null) {
            return false;
        }
        return isFlashColor(color) || isHexColor(color);
    }
    
    /**
     * 判断是否为#RRGGBB格式的十六进制颜色
     * 手写校验等价于正则"^#[0-9A-Fa-f]{6}$"，避免热路径反复编译正则
     * @param color 颜色字符串
     * @return 是否符合#RRGGBB格式
     */
    private static boolean isHexColor(String color) {
        if (color == null || color.length() != 7 || color.charAt(0) != '#') {
            return false;
        }
        for (int i = 1; i < 7; i++) {
            char c = color.charAt(i);
            boolean hexDigit = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
            if (!hexDigit) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 将颜色名称转换为十六进制代码
     * @param colorName 颜色名称
     * @return 十六进制颜色代码，如果不存在返回null
     */
    public static String getColorHex(String colorName) {
        return COLOR_MAP.get(colorName);
    }
    
    /**
     * 获取所有可用的颜色名称
     * @return 颜色名称集合
     */
    public static Set<String> getAvailableColorNames() {
        return COLOR_MAP.keySet();
    }
    
    /**
     * 验证并标准化颜色代码
     * @param color 输入的颜色
     * @return 标准化的颜色代码，如果无效返回白色
     */
    public static String normalizeColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            return "#FFFFFF";
        }

        // 如果是闪烁颜色模式，直接返回
        if (isFlashColor(color)) {
            return color;
        }

        // 如果是颜色名称，转换为十六进制
        String hexColor = getColorHex(color);
        if (hexColor != null) {
            return hexColor;
        }

        // 如果是十六进制代码，验证格式
        String trimmedColor = color.trim().toUpperCase();
        if (!trimmedColor.startsWith("#")) {
            trimmedColor = "#" + trimmedColor;
        }

        if (isValidColor(trimmedColor)) {
            return trimmedColor;
        }

        // 默认返回白色
        return "#FFFFFF";
    }
    
    /**
     * 将十六进制颜色转换为Minecraft颜色代码
     * @param hexColor 十六进制颜色
     * @return Minecraft颜色代码
     */
    public static String hexToMinecraftColor(String hexColor) {
        if (!isValidColor(hexColor)) {
            return "§f"; // 默认白色
        }
        return mapToMinecraftColorCode(hexColor);
    }
    
    /**
     * 将已通过校验的颜色转换为Minecraft颜色代码
     * @param hexColor 已通过校验的颜色
     * @return Minecraft颜色代码
     */
    private static String mapToMinecraftColorCode(String hexColor) {
        // 简化的颜色映射（可以根据需要扩展）
        switch (hexColor.toUpperCase()) {
            case "#000000": return "§0"; // 黑色
            case "#0000AA": return "§1"; // 深蓝色
            case "#00AA00": return "§2"; // 深绿色
            case "#00AAAA": return "§3"; // 深青色
            case "#AA0000": return "§4"; // 深红色
            case "#AA00AA": return "§5"; // 紫色
            case "#FFAA00": return "§6"; // 金色
            case "#AAAAAA": return "§7"; // 灰色
            case "#555555": return "§8"; // 深灰色
            case "#5555FF": return "§9"; // 蓝色
            case "#55FF55": return "§a"; // 绿色
            case "#55FFFF": return "§b"; // 青色
            case "#FF5555": return "§c"; // 红色
            case "#FF55FF": return "§d"; // 粉红色
            case "#FFFF55": return "§e"; // 黄色
            case "#FFFFFF": return "§f"; // 白色
            default:
                // 对于自定义颜色，尝试找到最接近的Minecraft颜色
                return findClosestMinecraftColor(hexColor);
        }
    }
    
    /**
     * 找到最接近的Minecraft颜色
     * @param hexColor 十六进制颜色
     * @return 最接近的Minecraft颜色代码
     */
    private static String findClosestMinecraftColor(String hexColor) {
        // 解析RGB值
        int rgb = Integer.parseInt(hexColor.substring(1), 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        
        // 简化的颜色匹配算法
        if (r > 200 && g > 200 && b > 200) return "§f"; // 白色
        if (r < 100 && g < 100 && b < 100) return "§0"; // 黑色
        if (r > g && r > b) return "§c"; // 红色
        if (g > r && g > b) return "§a"; // 绿色
        if (b > r && b > g) return "§9"; // 蓝色
        if (r > 150 && g > 150) return "§e"; // 黄色
        
        return "§f"; // 默认白色
    }
    
    /**
     * 创建带颜色的文本
     * @param text 文本内容
     * @param hexColor 十六进制颜色（可以为null，默认使用白色）
     * @return 带颜色代码的文本
     */
    public static String colorText(String text, String hexColor) {
        // 闪烁颜色直接返回原文本（可能为null），与旧实现完全一致
        if (isFlashColor(hexColor)) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        appendColorText(builder, text, hexColor);
        return builder.toString();
    }
    
    /**
     * 将带颜色的文本追加到StringBuilder
     * 输出与colorText逐字符一致，供渲染热路径避免中间字符串拼接
     * @param builder 目标StringBuilder
     * @param text 文本内容
     * @param hexColor 十六进制颜色（可以为null，默认使用白色）
     */
    public static void appendColorText(StringBuilder builder, String text, String hexColor) {
        // 如果颜色为null或空字符串，使用默认白色
        if (hexColor == null || hexColor.trim().isEmpty()) {
            builder.append("§f").append(text).append("§r");
            return;
        }
        // 闪烁颜色由渲染器动态处理，不嵌入§颜色代码（null文本不追加任何内容）
        if (isFlashColor(hexColor)) {
            if (text != null) {
                builder.append(text);
            }
            return;
        }
        // 此处已排除闪烁颜色，isValidColor等同于isHexColor，直接复用判定结果避免重复解析
        builder.append(isHexColor(hexColor) ? mapToMinecraftColorCode(hexColor) : "§f").append(text).append("§r");
    }
    
    /**
     * 解析十六进制颜色为RGB数组
     * @param hexColor 十六进制颜色（如 #FF0000）
     * @return RGB数组 [r, g, b]，范围0-255
     */
    public static int[] parseColor(String hexColor) {
        if (!isValidColor(hexColor)) {
            return new int[]{255, 255, 255}; // 默认白色
        }

        try {
            int rgb = Integer.parseInt(hexColor.substring(1), 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            return new int[]{r, g, b};
        } catch (Exception e) {
            return new int[]{255, 255, 255}; // 默认白色
        }
    }
}