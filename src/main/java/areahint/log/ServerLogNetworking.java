package areahint.log;

import areahint.Areashint;
import areahint.network.Channel;
import areahint.network.NetworkCompat;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 服务端日志网络处理
 * 接收客户端发送的进入/离开域名消息
 */
public class ServerLogNetworking {

    /**
     * 初始化服务端日志网络处理
     */
    public static void init() {
        // 注册接收客户端日志消息的处理器
        NetworkCompat.get().registerServerReceiver(Channel.C2S_AREA_LOG, (server, player, buf) -> {
            // 读取数据
            String action = buf.readString(); // "enter" 或 "leave"
            String areaName = buf.readString();
            int areaLevel = buf.readInt();
            String surfaceNameRaw = buf.readString();
            String surfaceName = surfaceNameRaw.isEmpty() ? null : surfaceNameRaw;
            String dimensionalNameRaw = buf.readString();
            String dimensionalName = dimensionalNameRaw.isEmpty() ? null : dimensionalNameRaw;

            // 在服务器线程上处理
            server.execute(() -> {
                String playerName = player.getName().getString();

                if ("enter".equals(action)) {
                    ServerLogManager.logPlayerEnterArea(playerName, areaLevel, surfaceName, areaName, dimensionalName);
                } else if ("leave".equals(action)) {
                    ServerLogManager.logPlayerLeaveArea(playerName, areaLevel, surfaceName, areaName, dimensionalName);
                }
            });
        });

        Areashint.LOGGER.info("服务端日志网络处理初始化完成");
    }
}
