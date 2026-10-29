package areahint.data;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;

/**
 * 区域数据模型
 * 包含区域的基本信息：名称、顶点、边界框、高度范围、等级、基础名称、颜色和副字幕
 */
public class AreaData {
    public static final String RURAL_TYPE = "rural";
    public static final String URBAN_TYPE = "urban";
    private String name;                    // 区域名称
    private List<Vertex> vertices;          // 多边形顶点列表
    @SerializedName("second-vertices")
    private List<Vertex> secondVertices;    // AABB边界框顶点
    private AltitudeData altitude;          // 高度范围（新增）
    private int level;                      // 区域等级
    @SerializedName("base-name")
    private String baseName;               // 基础名称（上级区域）
    private String signature;              // 域名创建者签名
    private List<String> signatures = new ArrayList<>(); // 域名创建者签名列表
    private String color;                  // 域名颜色（十六进制格式）
    private String surfacename;            // 联合域名（表面域名）
    private String subtitle;               // 副字幕文本；字段不存在或为空时不显示
    private String subtitlecolor;          // 副字幕颜色（十六进制格式或闪烁颜色）
    @JsonAdapter(GamemodeAdapter.class)
    private String gamemode;               // 域名要求的游戏模式；未设置时使用服务端默认值
    private String type;                   // 域名类型；旧文件缺失时按 rural 处理，城市化后为 urban

    // 预计算缓存（transient确保Gson序列化时忽略）
    private transient double cachedMinX, cachedMaxX, cachedMinZ, cachedMaxZ;
    private transient double cachedCenterX, cachedCenterZ;
    private transient boolean cacheComputed = false;

    // 构造函数
    public AreaData() {
        this.vertices = new ArrayList<>();
        this.secondVertices = new ArrayList<>();
    }

    public AreaData(String name, List<Vertex> vertices, List<Vertex> secondVertices, 
                   AltitudeData altitude, int level, String baseName, String signature) {
        this.name = name;
        this.vertices = vertices;
        this.secondVertices = secondVertices;
        this.altitude = altitude;
        this.level = level;
        this.baseName = baseName;
        this.signature = signature;
        this.color = "#FFFFFF"; // 默认为白色
    }

    public AreaData(String name, List<Vertex> vertices, List<Vertex> secondVertices, 
                   AltitudeData altitude, int level, String baseName, String signature, String color) {
        this.name = name;
        this.vertices = vertices;
        this.secondVertices = secondVertices;
        this.altitude = altitude;
        this.level = level;
        this.baseName = baseName;
        this.signature = signature;
        this.color = color;
        this.surfacename = null; // 默认无联合域名
    }

    public AreaData(String name, List<Vertex> vertices, List<Vertex> secondVertices, 
                   AltitudeData altitude, int level, String baseName, String signature, String color, String surfacename) {
        this.name = name;
        this.vertices = vertices;
        this.secondVertices = secondVertices;
        this.altitude = altitude;
        this.level = level;
        this.baseName = baseName;
        this.signature = signature;
        this.color = color;
        this.surfacename = surfacename;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public List<Vertex> getSecondVertices() {
        return secondVertices;
    }

    public void setSecondVertices(List<Vertex> secondVertices) {
        this.secondVertices = secondVertices;
    }

    public AltitudeData getAltitude() {
        return altitude;
    }

    public void setAltitude(AltitudeData altitude) {
        this.altitude = altitude;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getSignatures() {
        signatures = cleanSignatures(signatures);
        return signatures;
    }

    public void setSignatures(List<String> signatures) {
        this.signatures = cleanSignatures(signatures);
    }

    public List<String> getAllSignatures() {
        List<String> allSignatures = new ArrayList<>();
        String cleanedSignature = cleanSignature(signature);
        if (cleanedSignature != null) {
            allSignatures.add(cleanedSignature);
        }
        for (String playerName : cleanSignatures(getSignatures())) {
            if (!allSignatures.contains(playerName)) {
                allSignatures.add(playerName);
            }
        }
        return allSignatures;
    }

    public boolean hasSignature(String playerName) {
        String cleanedPlayerName = cleanSignature(playerName);
        return cleanedPlayerName != null && getAllSignatures().contains(cleanedPlayerName);
    }

    public void addSignature(String playerName) {
        String cleanedPlayerName = cleanSignature(playerName);
        if (cleanedPlayerName == null || hasSignature(cleanedPlayerName)) {
            return;
        }
        getSignatures().add(cleanedPlayerName);
    }

    public void removeSignature(String playerName) {
        String cleanedPlayerName = cleanSignature(playerName);
        if (cleanedPlayerName == null) {
            return;
        }
        getSignatures().removeIf(signature -> cleanedPlayerName.equals(cleanSignature(signature)));
    }

    private static List<String> cleanSignatures(List<String> signatures) {
        List<String> cleanedSignatures = new ArrayList<>();
        if (signatures == null) {
            return cleanedSignatures;
        }
        for (String signature : signatures) {
            String cleanedSignature = cleanSignature(signature);
            if (cleanedSignature != null && !cleanedSignatures.contains(cleanedSignature)) {
                cleanedSignatures.add(cleanedSignature);
            }
        }
        return cleanedSignatures;
    }

    private static String cleanSignature(String signature) {
        if (signature == null) {
            return null;
        }
        String cleanedSignature = signature.trim();
        return cleanedSignature.isEmpty() ? null : cleanedSignature;
    }

    public String getColor() {
        return color != null ? color : "#FFFFFF"; // 如果为null，默认返回白色
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSurfacename() {
        return surfacename;
    }

    public void setSurfacename(String surfacename) {
        this.surfacename = surfacename;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = cleanOptionalText(subtitle);
    }

    public String getSubtitleColor() {
        return subtitlecolor != null ? subtitlecolor : "#FFFFFF";
    }

    public void setSubtitleColor(String subtitlecolor) {
        this.subtitlecolor = cleanOptionalText(subtitlecolor);
    }

    public boolean hasSubtitle() {
        return subtitle != null && !subtitle.trim().isEmpty();
    }

    public String getGamemode() {
        return gamemode;
    }

    public void setGamemode(String gamemode) {
        this.gamemode = gamemode;
    }

    // 缺省类型只在读取语义上视为 rural，不因为查询或复制就给旧文件补写字段。
    public String getType() {
        return type == null || type.isBlank() ? RURAL_TYPE : type;
    }

    // 序列化和几何复制使用原始设置，区分缺省 rural 与文件中显式设置的 rural。
    public String getExplicitType() {
        return type == null || type.isBlank() ? null : type;
    }

    public void setType(String type) {
        this.type = type == null || type.isBlank() ? null : type;
    }

    // FileManager 等直接使用 Gson 的入口同样忽略错误类型，避免一项模式属性损坏整份域名列表。
    public static final class GamemodeAdapter implements com.google.gson.JsonDeserializer<String>, com.google.gson.JsonSerializer<String> {
        @Override
        public String deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type,
                                  com.google.gson.JsonDeserializationContext context) {
            return json.isJsonPrimitive() && json.getAsJsonPrimitive().isString() ? json.getAsString() : null;
        }

        // 显式写出字符串或空值，避免 Gson 2.10.1 将仅有读取实现的字段适配器回退为 String 反射序列化。
        @Override
        public com.google.gson.JsonElement serialize(String value, java.lang.reflect.Type type,
                                                     com.google.gson.JsonSerializationContext context) {
            return value == null ? com.google.gson.JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(value);
        }
    }

    private static String cleanOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * 预计算AABB边界和中心点缓存
     */
    public void computeCache() {
        if (secondVertices != null && secondVertices.size() == 4) {
            cachedMinX = Double.MAX_VALUE; cachedMaxX = -Double.MAX_VALUE;
            cachedMinZ = Double.MAX_VALUE; cachedMaxZ = -Double.MAX_VALUE;
            for (Vertex v : secondVertices) {
                cachedMinX = Math.min(cachedMinX, v.getX());
                cachedMaxX = Math.max(cachedMaxX, v.getX());
                cachedMinZ = Math.min(cachedMinZ, v.getZ());
                cachedMaxZ = Math.max(cachedMaxZ, v.getZ());
            }
        }
        if (vertices != null && !vertices.isEmpty()) {
            cachedCenterX = 0; cachedCenterZ = 0;
            for (Vertex v : vertices) {
                cachedCenterX += v.getX();
                cachedCenterZ += v.getZ();
            }
            cachedCenterX /= vertices.size();
            cachedCenterZ /= vertices.size();
        }
        cacheComputed = true;
    }

    public boolean isPointInCachedAABB(double x, double z) {
        return cacheComputed && x >= cachedMinX && x <= cachedMaxX && z >= cachedMinZ && z <= cachedMaxZ;
    }

    public double distanceSqToCenter(double x, double z) {
        double dx = x - cachedCenterX;
        double dz = z - cachedCenterZ;
        return dx * dx + dz * dz;
    }

    public boolean isCacheComputed() {
        return cacheComputed;
    }

    /**
     * 验证区域数据的有效性
     * @return 验证是否通过
     */
    public boolean isValid() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (vertices == null || vertices.size() < 3) {
            return false;
        }
        
        if (secondVertices == null || secondVertices.size() != 4) {
            return false;
        }
        
        if (level < 1) {
            return false;
        }
        
        // 验证高度数据
        if (altitude != null && !altitude.isValid()) {
            return false;
        }
        
        // 验证颜色格式（如果存在）
        if (color != null && !color.matches("^#[0-9A-Fa-f]{6}$")
                && !areahint.util.ColorUtil.isFlashColor(color)) {
            return false;
        }

        // 副字幕颜色与主标题颜色相互独立，但可用颜色格式保持一致。
        if (subtitlecolor != null && !subtitlecolor.matches("^#[0-9A-Fa-f]{6}$")
                && !areahint.util.ColorUtil.isFlashColor(subtitlecolor)) {
            return false;
        }
        
        return true;
    }

    /**
     * 顶点数据模型
     */
    public static class Vertex {
        private double x;
        private double z;

        public Vertex() {}

        public Vertex(double x, double z) {
            this.x = x;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        @Override
        public String toString() {
            return String.format("Vertex{x=%.1f, z=%.1f}", x, z);
        }
    }

    /**
     * 高度范围数据模型
     */
    public static class AltitudeData {
        private Double max;  // 最大高度，null表示无限制
        private Double min;  // 最小高度，null表示无限制

        public AltitudeData() {}

        public AltitudeData(Double max, Double min) {
            this.max = max;
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        /**
         * 验证高度数据的有效性
         * @return 验证是否通过
         */
        public boolean isValid() {
            // 如果max和min都不为null，max必须大于等于min
            if (max != null && min != null) {
                return max >= min;
            }
            return true;
        }

        /**
         * 检查给定高度是否在范围内
         * @param y 要检查的高度
         * @return 是否在范围内
         */
        public boolean isInRange(double y) {
            if (min != null && y < min) {
                return false;
            }
            if (max != null && y > max) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("AltitudeData{max=%s, min=%s}", 
                max != null ? max.toString() : "null", 
                min != null ? min.toString() : "null");
        }
    }

    @Override
    public String toString() {
        return String.format("AreaData{name='%s', level=%d, altitude=%s, vertices=%d, baseName='%s', signature='%s', color='%s', surfacename='%s', subtitle='%s', subtitlecolor='%s'}",
            name, level, altitude, vertices != null ? vertices.size() : 0, baseName, signature, color, surfacename, subtitle, subtitlecolor);
    }
} 
