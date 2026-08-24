package areahint.permission;

import areahint.Areashint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * LuckPerms 兼容层。
 */
public final class LuckPermsCompat {
    public enum Result {
        TRUE,
        FALSE,
        UNDEFINED
    }

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static volatile Object luckPerms;
    private static volatile Object userManager;
    private static volatile Method getUserMethod;

    private LuckPermsCompat() {
    }

    /**
     * 在服务端完成启动后初始化 LuckPerms API。
     */
    public static synchronized void initialize() {
        initialized = true;
        resolveApi(true);
    }

    /**
     * 清理当前服务器的 API 引用，支持集成服务器重复启动。
     */
    public static synchronized void shutdown() {
        initialized = false;
        clearApi();
    }

    public static boolean isAvailable() {
        return available && luckPerms != null && userManager != null && getUserMethod != null;
    }

    public static Result checkPermission(ServerPlayerEntity player, String node) {
        if (player == null || node == null || node.isBlank()) {
            return Result.UNDEFINED;
        }

        // 服务端生命周期完成前不触碰 LuckPermsProvider，避免 API 尚未注册时触发异常。
        if (!initialized) {
            return Result.UNDEFINED;
        }
        if (!available && FabricLoader.getInstance().isModLoaded("luckperms")) {
            resolveApi(false);
        }

        if (!isAvailable()) {
            return Result.UNDEFINED;
        }

        try {
            Object user = getUserMethod.invoke(userManager, player.getUuid());
            if (user == null) {
                return Result.UNDEFINED;
            }

            Method getCachedDataMethod = user.getClass().getMethod("getCachedData");
            Object cachedData = getCachedDataMethod.invoke(user);
            Method getPermissionDataMethod = cachedData.getClass().getMethod("getPermissionData");
            Object permissionData = getPermissionDataMethod.invoke(cachedData);
            Method checkPermissionMethod = permissionData.getClass().getMethod("checkPermission", String.class);
            Object result = checkPermissionMethod.invoke(permissionData, node);

            if (result instanceof Enum<?> enumResult) {
                return switch (enumResult.name()) {
                    case "TRUE" -> Result.TRUE;
                    case "FALSE" -> Result.FALSE;
                    default -> Result.UNDEFINED;
                };
            }
        } catch (Exception e) {
            Areashint.LOGGER.warn("查询 LuckPerms 权限节点失败: {} for {}", node, player.getName().getString(), e);
        }

        return Result.UNDEFINED;
    }

    private static synchronized void resolveApi(boolean logState) {
        if (!FabricLoader.getInstance().isModLoaded("luckperms")) {
            clearApi();
            if (logState) {
                Areashint.LOGGER.info("LuckPerms 未安装，Areas Hint 将使用原有权限规则。");
            }
            return;
        }

        boolean wasAvailable = isAvailable();
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method getMethod = providerClass.getMethod("get");
            Object api = getMethod.invoke(null);
            Method getUserManagerMethod = api.getClass().getMethod("getUserManager");
            Object manager = getUserManagerMethod.invoke(api);
            Method userMethod = manager.getClass().getMethod("getUser", UUID.class);

            luckPerms = api;
            userManager = manager;
            getUserMethod = userMethod;
            available = true;

            if (logState || !wasAvailable) {
                Areashint.LOGGER.info("LuckPerms 已检测到，Areas Hint 权限节点联动已启用。");
            }
        } catch (Exception e) {
            clearApi();
            if (logState) {
                Throwable cause = e instanceof InvocationTargetException invocation
                    && invocation.getCause() != null ? invocation.getCause() : e;
                if ("net.luckperms.api.LuckPermsProvider$NotLoadedException".equals(cause.getClass().getName())) {
                    Areashint.LOGGER.info("LuckPerms API 尚未就绪，Areas Hint 将在后续权限查询时重试。");
                } else {
                    Areashint.LOGGER.warn("LuckPerms 已安装但 API 不可用，Areas Hint 将回退到原有权限规则。", e);
                }
            }
        }
    }

    private static void clearApi() {
        available = false;
        luckPerms = null;
        userManager = null;
        getUserMethod = null;
    }
}
