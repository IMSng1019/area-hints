package areahint.util;

import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaPermissionCompatTest {
    @Test
    void everyVanillaOperatorTierRetainsItsCommandThresholds() {
        // 使用 Minecraft 真实谓词验证整张权限等级表，而非在测试里模拟其比较方法。
        for (PermissionLevel granted : PermissionLevel.values()) {
            PermissionPredicate permissions = LeveledPermissionPredicate.fromLevel(granted);
            for (int requested = 0; requested <= 4; requested++) {
                String context = "授予等级=" + granted.getLevel() + "，要求等级=" + requested;
                if (requested <= granted.getLevel()) {
                    assertTrue(VanillaPermissionCompat.hasPermissionLevel(permissions, requested), context);
                } else {
                    assertFalse(VanillaPermissionCompat.hasPermissionLevel(permissions, requested), context);
                }
            }
        }
    }

    @Test
    void commandsOpenToEveryoneRemainAvailableWithNoPermissions() {
        // 新版 NONE 连 Level(ALL) 都拒绝，直接机械改写会误禁用默认开放的命令。
        assertFalse(PermissionPredicate.NONE.hasPermission(new Permission.Level(PermissionLevel.ALL)));
        assertTrue(VanillaPermissionCompat.hasPermissionLevel(PermissionPredicate.NONE, 0));
        assertTrue(VanillaPermissionCompat.hasPermissionLevel(PermissionPredicate.NONE, -1));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(PermissionPredicate.NONE, 1));
    }

    @Test
    void levelsAboveOwnerAreNotSilentlyClampedToOwner() {
        assertTrue(PermissionLevel.fromLevel(5) == PermissionLevel.OWNERS);
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(LeveledPermissionPredicate.OWNERS, 5));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(PermissionPredicate.ALL, Integer.MAX_VALUE));
    }

    @Test
    void missingActorsDoNotGainEvenOpenCommandPermissions() {
        assertFalse(VanillaPermissionCompat.hasPermissionLevel((PlayerEntity) null, 0));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel((ServerCommandSource) null, 0));
    }
}
