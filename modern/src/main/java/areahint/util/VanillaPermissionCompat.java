package areahint.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.player.Player;

/** 保持原版数字权限等级的语义，将 1.21.11 的权限谓词差异限制在一处。 */
public final class VanillaPermissionCompat {
    private VanillaPermissionCompat() {
    }

    public static boolean hasPermissionLevel(Player player, int level) {
        if (player == null) {
            return false;
        }
        return hasPermissionLevel(player.permissions(), level);
        /*return player.hasPermissionLevel(level);
        */
    }

    public static boolean hasPermissionLevel(CommandSourceStack source, int level) {
        if (source == null) {
            return false;
        }
        return hasPermissionLevel(source.permissions(), level);
        /*return source.hasPermissionLevel(level);
        */
    }
    static boolean hasPermissionLevel(net.minecraft.server.permissions.PermissionSet permissions, int level) {
        // 原来的零级权限表示向所有有效主体开放，不能交给新版 NONE 谓词拒绝。
        if (level <= 0) {
            return true;
        }
        // fromLevel 会把过大值截到 OWNERS，先排除越界要求，避免降低原本的权限门槛。
        if (level > 4) {
            return false;
        }
        return permissions.hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(
            net.minecraft.server.permissions.PermissionLevel.byId(level)));
    }
}
