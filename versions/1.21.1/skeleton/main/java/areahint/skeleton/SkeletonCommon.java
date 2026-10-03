package areahint.skeleton;

/**
 * 1.21.1 目标在阶段 0 探针里只构建空骨架 jar
 * 本类仅用于让该版本的产物可以被辨认，不接入模组初始化流程
 */
public final class SkeletonCommon {
    private SkeletonCommon() {
    }

    /** 骨架标识，用于确认 jar 来自 1.21.1 目标 */
    public static String id() {
        return "skeleton-common";
    }
}
