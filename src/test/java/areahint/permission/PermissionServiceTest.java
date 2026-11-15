package areahint.permission;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PermissionServiceTest {
    @Test
    void explicitDenialCannotBeOverriddenByAnOperatorFallback() {
        assertFalse(PermissionService.resolve(LuckPermsCompat.Result.FALSE,
            () -> fail("显式拒绝不能继续查询原版操作员权限")));
    }

    @Test
    void explicitGrantDoesNotDependOnVanillaPermissions() {
        assertTrue(PermissionService.resolve(LuckPermsCompat.Result.TRUE,
            () -> fail("显式允许不能继续查询原版权限")));
    }

    @Test
    void undefinedNodeRetainsBothAllowedAndDeniedFallbackResults() {
        for (boolean allowed : new boolean[] {false, true}) {
            AtomicInteger calls = new AtomicInteger();
            assertEquals(allowed, PermissionService.resolve(LuckPermsCompat.Result.UNDEFINED, () -> {
                calls.incrementAndGet();
                return allowed;
            }));
            assertEquals(1, calls.get(), "缺省节点必须且只能执行一次原版或签名回退");
        }
    }
}
