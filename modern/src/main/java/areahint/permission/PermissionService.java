package areahint.permission;

import java.util.function.BooleanSupplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * 统一权限判断入口。
 */
public final class PermissionService {
    private PermissionService() {
    }

    public static boolean hasCommandPermission(CommandSourceStack source, String node, int fallbackLevel) {
        if (source == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(source, node), () -> areahint.util.VanillaPermissionCompat.hasPermissionLevel(source, fallbackLevel));
    }

    public static boolean hasCommandPermission(ServerPlayer player, String node, int fallbackLevel) {
        if (player == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(player, node), () -> areahint.util.VanillaPermissionCompat.hasPermissionLevel(player, fallbackLevel));
    }

    public static boolean hasNodeOr(ServerPlayer player, String node, BooleanSupplier fallbackRule) {
        if (player == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(player, node), fallbackRule);
    }

    public static boolean hasNodeOr(CommandSourceStack source, String node, BooleanSupplier fallbackRule) {
        if (source == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(source, node), fallbackRule);
    }

    // 显式节点优先于原版等级与签名回退，缺省时才计算业务规则。
    static boolean resolve(LuckPermsCompat.Result result, BooleanSupplier fallbackRule) {
        if (result == LuckPermsCompat.Result.TRUE) {
            return true;
        }
        if (result == LuckPermsCompat.Result.FALSE) {
            return false;
        }
        return fallbackRule.getAsBoolean();
    }
}
