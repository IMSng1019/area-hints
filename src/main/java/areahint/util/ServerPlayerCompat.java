package areahint.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** 集中处理服务端玩家世界访问器的版本改名，命令源的 getWorld 不受此变更影响。 */
public final class ServerPlayerCompat {
    private ServerPlayerCompat() {
    }

    public static ServerWorld world(ServerPlayerEntity player) {
        //? if >=1.21.9 {
        /*return player.getEntityWorld();*/
        //?} else {
        return (ServerWorld) player.getWorld();
        //?}
    }

    // 通过玩家当前服务端世界取得所属服务器，保持跨维度传送与在线身份校验使用同一实例。
    public static MinecraftServer server(ServerPlayerEntity player) {
        return world(player).getServer();
    }

    // 操作员名单在 1.21.9 改用轻量玩家配置条目，仍按相同账号身份查询。
    public static boolean isOperator(ServerPlayerEntity player) {
        //? if >=1.21.9 {
        /*return server(player).getPlayerManager().isOperator(new net.minecraft.server.PlayerConfigEntry(player.getGameProfile()));*/
        //?} else {
        return server(player).getPlayerManager().isOperator(player.getGameProfile());
        //?}
    }
}
