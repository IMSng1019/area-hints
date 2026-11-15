package areahint.gamemode;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.GameType;

// 游戏模式通知独立于标题显示开关；本地数据只决定是否通知，不直接改变玩家模式。
public final class AreaGamemodeClient {
    private static ClientPacketListener connection;
    private static boolean requestedPolicy;
    private static boolean enabled;
    private static GameType defaultMode = GameType.SURVIVAL;
    private static GameType lastRequired;
    private static String lastDimension;
    private static int ticks;

    private AreaGamemodeClient() { }

    public static void register() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_AREA_GAMEMODE_POLICY, (client, reader) -> {
            boolean required = reader.readBoolean();
            GameType fallback = AreaGamemodeResolver.parseMode(reader.readString(32));
            ClientPacketListener handler = client.getConnection();
            client.execute(() -> {
                if (handler == null || client.getConnection() != handler) {
                    return;
                }
                connection = handler;
                enabled = required && fallback != null;
                defaultMode = fallback == null ? GameType.SURVIVAL : fallback;
                invalidate();
            });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset(handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(null));
        ClientTickEvents.END_CLIENT_TICK.register(AreaGamemodeClient::tick);
    }

    private static void reset(ClientPacketListener handler) {
        connection = handler;
        requestedPolicy = false;
        enabled = false;
        defaultMode = GameType.SURVIVAL;
        ticks = 0;
        invalidate();
    }

    // 同步到新文件后强制重新通知，即使域名名称或玩家坐标都未发生变化。
    public static void invalidate() {
        lastRequired = null;
        lastDimension = null;
    }

    private static void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.getConnection() == null) {
            return;
        }
        if (connection != client.getConnection()) {
            reset(client.getConnection());
        }
        if (!requestedPolicy && ClientNetworkCompat.get().canSend(Channel.C2S_AREA_GAMEMODE_POLICY)) {
            requestedPolicy = true;
            ClientNetworkCompat.get().sendToServer(Channel.C2S_AREA_GAMEMODE_POLICY, ClientNetworkCompat.get().newBuffer());
        }
        // 固定每五刻检查一次，不受玩家可调的标题检测频率影响，也不会每帧发送网络包。
        if (!enabled || ++ticks % 5 != 0 || AreashintClient.getAreaDetector() == null
                || !ClientNetworkCompat.get().canSend(Channel.C2S_AREA_GAMEMODE_CHECK)) {
            return;
        }
        /*String dimension = client.world.getDimensionKey().getValue().toString();
        */
        String dimension = client.level.dimension().identifier().toString();
        AreaData area = AreashintClient.getAreaDetector().findAreaForGamemode(
            client.player.getX(), client.player.getY(), client.player.getZ());
        GameType desired = AreaGamemodeResolver.requiredMode(area, defaultMode);
        if (desired != lastRequired || !dimension.equals(lastDimension)) {
            lastRequired = desired;
            lastDimension = dimension;
            ClientNetworkCompat.get().sendToServer(Channel.C2S_AREA_GAMEMODE_CHECK, ClientNetworkCompat.get().newBuffer());
        }
    }
}
