package areahint.util;

import net.minecraft.util.Identifier;

/**
 * 维度标识符字符串缓存。
 * <p>
 * 维度标识符对象在同一个世界里是稳定复用的，而 {@code toString()} 每次都会分配新字符串。
 * 主线的每帧判定、Xaero 填充差集解析都会按帧读取维度 ID，因此这里按对象身份缓存最近一次的转换结果。
 * 缓存只保存最近一个维度，世界或维度切换后自动失效，不需要额外清理。
 */
public final class DimensionIdCache {
    // 最近一次转换的维度标识符（按引用比较）
    private static volatile Identifier cachedIdentifier;

    // 与 cachedIdentifier 对应的维度 ID 字符串
    private static volatile String cachedId;

    private DimensionIdCache() {
    }

    /**
     * 获取维度 ID 字符串，同一个 Identifier 实例重复调用不会重复分配。
     * @param dimension 维度标识符，允许为 null
     * @return 维度 ID 字符串；入参为 null 时返回 null
     */
    public static String getId(Identifier dimension) {
        if (dimension == null) {
            return null;
        }
        if (dimension == cachedIdentifier) {
            String cached = cachedId;
            if (cached != null) {
                return cached;
            }
        }
        String id = dimension.toString();
        // 只是缓存加速，写入顺序不影响正确性，因此无需加锁
        cachedId = id;
        cachedIdentifier = dimension;
        return id;
    }
}
