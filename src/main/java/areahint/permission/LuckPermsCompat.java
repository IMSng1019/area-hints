package areahint.permission;

import areahint.Areashint;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
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
    private static volatile boolean fabricPermissionsResolved;
    private static volatile Method getPermissionValueMethod;

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
        fabricPermissionsResolved = false;
        getPermissionValueMethod = null;
    }

    public static boolean isAvailable() {
        return available && luckPerms != null && userManager != null && getUserMethod != null;
    }

    /**
     * 优先通过 Fabric 权限桥接查询命令源，使 LuckPerms 能记录控制台和玩家触发的节点。
     */
    public static Result checkPermission(ServerCommandSource source, String node) {
        if (source == null || node == null || node.isBlank()) {
            return Result.UNDEFINED;
        }

        Result fabricResult = checkFabricPermission(source, node);
        if (fabricResult != Result.UNDEFINED) {
            return fabricResult;
        }

        return checkLuckPermsPermission(source.getPlayer(), node);
    }

    /**
     * 玩家权限查询保留 LuckPerms 公共 API 回退，兼容未暴露 Fabric 权限桥接的服务端环境。
     */
    public static Result checkPermission(ServerPlayerEntity player, String node) {
        if (player == null || node == null || node.isBlank()) {
            return Result.UNDEFINED;
        }

        Result fabricResult = checkFabricPermission(player.getCommandSource(), node);
        if (fabricResult != Result.UNDEFINED) {
            return fabricResult;
        }

        return checkLuckPermsPermission(player, node);
    }

    /**
     * 使用控制台命令源提交全部节点，LuckPerms editor 随后即可从运行时注册表读取完整清单。
     */
    public static void registerPermissionNodes(MinecraftServer server) {
        initialize();
        if (server == null || !FabricLoader.getInstance().isModLoaded("luckperms")) {
            return;
        }
        if (!resolveFabricPermissionsApi(true)) {
            Areashint.LOGGER.warn("无法登记 Areas Hint 权限节点: Fabric Permissions API 不可用。");
            return;
        }

        ServerCommandSource source = server.getCommandSource();
        int registered = 0;
        for (String node : PermissionNodes.all()) {
            if (invokeFabricPermissionCheck(source, node)) {
                registered++;
            }
        }

        Areashint.LOGGER.info("已向 LuckPerms 提交 {} 个 Areas Hint 权限节点。", registered);
    }

    private static Result checkFabricPermission(CommandSource source, String node) {
        if (source == null || !resolveFabricPermissionsApi(false)) {
            return Result.UNDEFINED;
        }

        try {
            Object result = getPermissionValueMethod.invoke(null, source, node);
            return convertEnumResult(result);
        } catch (Exception e) {
            Areashint.LOGGER.warn("通过 Fabric Permissions API 查询权限节点失败: {}", node, e);
            return Result.UNDEFINED;
        }
    }

    private static boolean invokeFabricPermissionCheck(CommandSource source, String node) {
        try {
            getPermissionValueMethod.invoke(null, source, node);
            return true;
        } catch (Exception e) {
            Areashint.LOGGER.warn("向 LuckPerms 登记权限节点失败: {}", node, e);
            return false;
        }
    }

    private static synchronized boolean resolveFabricPermissionsApi(boolean logState) {
        if (fabricPermissionsResolved) {
            return getPermissionValueMethod != null;
        }

        fabricPermissionsResolved = true;
        if (!FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
            return false;
        }

        try {
            Class<?> permissionsClass = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            getPermissionValueMethod = permissionsClass.getMethod("getPermissionValue", CommandSource.class, String.class);
            if (logState) {
                Areashint.LOGGER.info("Fabric Permissions API 已检测到，开始登记 Areas Hint 权限节点。");
            }
            return true;
        } catch (Exception e) {
            getPermissionValueMethod = null;
            if (logState) {
                Areashint.LOGGER.warn("Fabric Permissions API 已安装但无法调用。", e);
            }
            return false;
        }
    }

    private static Result checkLuckPermsPermission(ServerPlayerEntity player, String node) {
        if (player == null) {
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
            return convertEnumResult(result);
        } catch (Exception e) {
            Areashint.LOGGER.warn("查询 LuckPerms 权限节点失败: {} for {}", node, player.getName().getString(), e);
        }

        return Result.UNDEFINED;
    }

    private static Result convertEnumResult(Object result) {
        if (result instanceof Enum<?> enumResult) {
            return switch (enumResult.name()) {
                case "TRUE" -> Result.TRUE;
                case "FALSE" -> Result.FALSE;
                default -> Result.UNDEFINED;
            };
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
