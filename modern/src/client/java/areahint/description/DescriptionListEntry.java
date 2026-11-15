package areahint.description;

/**
 * 客户端描述交互列表项。
 */
public record DescriptionListEntry(
    String id,
    String displayName,
    int level,
    String baseName,
    String signature,
    String dimension
) {
}
