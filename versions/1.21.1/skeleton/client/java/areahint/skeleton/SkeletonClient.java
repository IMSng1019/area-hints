package areahint.skeleton;

/**
 * 1.21.1 骨架版本的客户端环境标记类
 * 用来验证 splitEnvironmentSourceSets() 下 client source set 仍然独立参与编译
 */
public final class SkeletonClient {
    private SkeletonClient() {
    }

    /** 骨架标识，用于确认 jar 里的客户端环境类 */
    public static String id() {
        return "skeleton-client";
    }
}
