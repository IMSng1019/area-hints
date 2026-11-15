package areahint.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.player.Player;

class VanillaPermissionCompatTest {
    @Test
    void everyVanillaOperatorTierRetainsItsCommandThresholds() {
        // 使用 Minecraft 真实谓词验证整张权限等级表，而非在测试里模拟其比较方法。
        for (PermissionLevel granted : PermissionLevel.values()) {
            PermissionSet permissions = LevelBasedPermissionSet.forLevel(granted);
            for (int requested = 0; requested <= 4; requested++) {
                String context = "授予等级=" + granted.id() + "，要求等级=" + requested;
                if (requested <= granted.id()) {
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
        assertFalse(PermissionSet.NO_PERMISSIONS.hasPermission(new Permission.HasCommandLevel(PermissionLevel.ALL)));
        assertTrue(VanillaPermissionCompat.hasPermissionLevel(PermissionSet.NO_PERMISSIONS, 0));
        assertTrue(VanillaPermissionCompat.hasPermissionLevel(PermissionSet.NO_PERMISSIONS, -1));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(PermissionSet.NO_PERMISSIONS, 1));
    }

    @Test
    void levelsAboveOwnerAreNotSilentlyClampedToOwner() {
        assertTrue(PermissionLevel.byId(5) == PermissionLevel.OWNERS);
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(LevelBasedPermissionSet.OWNER, 5));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel(PermissionSet.ALL_PERMISSIONS, Integer.MAX_VALUE));
    }

    @Test
    void missingActorsDoNotGainEvenOpenCommandPermissions() {
        assertFalse(VanillaPermissionCompat.hasPermissionLevel((Player) null, 0));
        assertFalse(VanillaPermissionCompat.hasPermissionLevel((CommandSourceStack) null, 0));
    }
}
