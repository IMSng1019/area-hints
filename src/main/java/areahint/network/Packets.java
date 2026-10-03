package areahint.network;

import areahint.Areashint;

/**
 * 维度常量与维度文件名映射工具。
 * 说明：通道标识已全部迁到 {@link Channel}（由各版本的 NetworkCompat 实现转成该版本的 Identifier），
 * 本类因此不再持有任何 Minecraft 类型，也就不需要任何版本分支。
 */
public class Packets {
    // 维度类型常量
    public static final String DIMENSION_OVERWORLD = "overworld";
    public static final String DIMENSION_NETHER = "the_nether";
    public static final String DIMENSION_END = "the_end";

    /**
     * 将维度路径转换为维度类型字符串
     * @param dimensionPath 维度路径
     * @return 维度类型字符串，如果未知则返回null
     */
    public static String convertDimensionPathToType(String dimensionPath) {
        if (dimensionPath == null) {
            return null;
        }
        
        switch (dimensionPath) {
            case "overworld":
                return DIMENSION_OVERWORLD;
            case "the_nether":
                return DIMENSION_NETHER;
            case "the_end":
                return DIMENSION_END;
            default:
                return null;
        }
    }
    
    /**
     * 获取维度对应的文件名
     * @param dimensionType 维度类型
     * @return 文件名，如果未知则返回null
     */
    public static String getFileNameForDimension(String dimensionType) {
        if (dimensionType == null) {
            return null;
        }

        switch (dimensionType) {
            case DIMENSION_OVERWORLD:
                return Areashint.OVERWORLD_FILE;
            case DIMENSION_NETHER:
                return Areashint.NETHER_FILE;
            case DIMENSION_END:
                return Areashint.END_FILE;
            default:
                return null;
        }
    }

    public static String getDatabaseFolderForDimension(String dimensionType) {
        if (dimensionType == null) {
            return null;
        }

        switch (dimensionType) {
            case DIMENSION_OVERWORLD:
                return DIMENSION_OVERWORLD;
            case DIMENSION_NETHER:
                return DIMENSION_NETHER;
            case DIMENSION_END:
                return DIMENSION_END;
            default:
                return null;
        }
    }
}
