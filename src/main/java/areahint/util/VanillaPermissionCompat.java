package areahint.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

/** 保持原版数字权限等级的语义，将 1.21.11 的权限谓词差异限制在一处。 */
public final class VanillaPermissionCompat {
    private VanillaPermissionCompat() {
    }

    public static boolean hasPermissionLevel(PlayerEntity player, int level) {
        if (player == null) {
            return false;
        }
        //? if >=1.21.11 {
        /*return hasPermissionLevel(player.getPermissions(), level);*/
        //?} else {
        return player.hasPermissionLevel(level);
        //?}
    }

    public static boolean hasPermissionLevel(ServerCommandSource source, int level) {
        if (source == null) {
            return false;
        }
        //? if >=1.21.11 {
        /*return hasPermissionLevel(source.getPermissions(), level);*/
        //?} else {
        return source.hasPermissionLevel(level);
        //?}
    }

    //? if >=1.21.11 {
    /*static boolean hasPermissionLevel(net.minecraft.command.permission.PermissionPredicate permissions, int level) {
        // 原来的零级权限表示向所有有效主体开放，不能交给新版 NONE 谓词拒绝。
        if (level <= 0) {
            return true;
        }
        // fromLevel 会把过大值截到 OWNERS，先排除越界要求，避免降低原本的权限门槛。
        if (level > 4) {
            return false;
        }
        return permissions.hasPermission(new net.minecraft.command.permission.Permission.Level(
            net.minecraft.command.permission.PermissionLevel.fromLevel(level)));
    }*/
    //?}
}
