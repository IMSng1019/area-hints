package areahint.data;

/**
 * 维度域名数据类
 * 用于存储维度名称信息
 */
public class DimensionalNameData {
    private String dimensionId;     // 维度标识符 (如 "minecraft:overworld")
    private String displayName;     // 显示名称 (如 "蛮荒大陆")
    private String color;           // 颜色 (十六进制，如 "#FFFFFF")
    private String signature;       // 维度域名创建者签名，旧配置没有该字段时保持为null

    public DimensionalNameData() {}

    public DimensionalNameData(String dimensionId, String displayName) {
        this.dimensionId = dimensionId;
        this.displayName = displayName;
        this.color = null;
        this.signature = null;
    }

    public DimensionalNameData(String dimensionId, String displayName, String color) {
        this.dimensionId = dimensionId;
        this.displayName = displayName;
        this.color = color;
        this.signature = null;
    }

    public DimensionalNameData(String dimensionId, String displayName, String color, String signature) {
        this.dimensionId = dimensionId;
        this.displayName = displayName;
        this.color = color;
        this.signature = signature;
    }

    public String getDimensionId() { return dimensionId; }
    public void setDimensionId(String dimensionId) { this.dimensionId = dimensionId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    @Override
    public String toString() {
        return String.format("DimensionalNameData{dimensionId='%s', displayName='%s', color='%s', signature='%s'}",
                dimensionId, displayName, color, signature);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DimensionalNameData that = (DimensionalNameData) obj;
        return java.util.Objects.equals(dimensionId, that.dimensionId) &&
               java.util.Objects.equals(displayName, that.displayName) &&
               java.util.Objects.equals(color, that.color) &&
               java.util.Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(dimensionId, displayName, color, signature);
    }
} 
