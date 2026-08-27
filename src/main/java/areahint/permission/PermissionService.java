package areahint.permission;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.BooleanSupplier;

/**
 * 统一权限判断入口。
 */
public final class PermissionService {
    private PermissionService() {
    }

    public static boolean hasCommandPermission(ServerCommandSource source, String node, int fallbackLevel) {
        if (source == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(source, node), () -> source.hasPermissionLevel(fallbackLevel));
    }

    public static boolean hasCommandPermission(ServerPlayerEntity player, String node, int fallbackLevel) {
        if (player == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(player, node), () -> player.hasPermissionLevel(fallbackLevel));
    }

    public static boolean hasNodeOr(ServerPlayerEntity player, String node, BooleanSupplier fallbackRule) {
        if (player == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(player, node), fallbackRule);
    }

    public static boolean hasNodeOr(ServerCommandSource source, String node, BooleanSupplier fallbackRule) {
        if (source == null) {
            return false;
        }
        return resolve(LuckPermsCompat.checkPermission(source, node), fallbackRule);
    }

    private static boolean resolve(LuckPermsCompat.Result result, BooleanSupplier fallbackRule) {
        if (result == LuckPermsCompat.Result.TRUE) {
            return true;
        }
        if (result == LuckPermsCompat.Result.FALSE) {
            return false;
        }
        return fallbackRule.getAsBoolean();
    }
}
