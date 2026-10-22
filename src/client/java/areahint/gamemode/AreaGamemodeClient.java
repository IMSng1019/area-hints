package areahint.gamemode;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.world.GameMode;

// 游戏模式通知独立于标题显示开关；本地数据只决定是否通知，不直接改变玩家模式。
public final class AreaGamemodeClient {
    private static ClientPlayNetworkHandler connection;
    private static boolean requestedPolicy;
    private static boolean enabled;
    private static GameMode defaultMode = GameMode.SURVIVAL;
    private static GameMode lastRequired;
    private static String lastDimension;
    private static int ticks;

    private AreaGamemodeClient() { }

    public static void register() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_AREA_GAMEMODE_POLICY, (client, reader) -> {
            boolean required = reader.readBoolean();
            GameMode fallback = AreaGamemodeResolver.parseMode(reader.readString(32));
            ClientPlayNetworkHandler handler = client.getNetworkHandler();
            client.execute(() -> {
                if (handler == null || client.getNetworkHandler() != handler) {
                    return;
                }
                connection = handler;
                enabled = required && fallback != null;
                defaultMode = fallback == null ? GameMode.SURVIVAL : fallback;
                invalidate();
            });
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset(handler));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset(null));
        ClientTickEvents.END_CLIENT_TICK.register(AreaGamemodeClient::tick);
    }

    private static void reset(ClientPlayNetworkHandler handler) {
        connection = handler;
        requestedPolicy = false;
        enabled = false;
        defaultMode = GameMode.SURVIVAL;
        ticks = 0;
        invalidate();
    }

    // 同步到新文件后强制重新通知，即使域名名称或玩家坐标都未发生变化。
    public static void invalidate() {
        lastRequired = null;
        lastDimension = null;
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null || client.getNetworkHandler() == null) {
            return;
        }
        if (connection != client.getNetworkHandler()) {
            reset(client.getNetworkHandler());
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
        //? if <=1.20.4 {
        String dimension = client.world.getDimensionKey().getValue().toString();
        //?} else {
        /*String dimension = client.world.getRegistryKey().getValue().toString();*/
        //?}
        AreaData area = AreashintClient.getAreaDetector().findAreaForGamemode(
            client.player.getX(), client.player.getY(), client.player.getZ());
        GameMode desired = AreaGamemodeResolver.requiredMode(area, defaultMode);
        if (desired != lastRequired || !dimension.equals(lastDimension)) {
            lastRequired = desired;
            lastDimension = dimension;
            ClientNetworkCompat.get().sendToServer(Channel.C2S_AREA_GAMEMODE_CHECK, ClientNetworkCompat.get().newBuffer());
        }
    }
}
