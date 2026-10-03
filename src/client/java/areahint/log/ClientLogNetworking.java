package areahint.log;

import areahint.AreashintClient;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;

/**
 * 客户端日志网络处理
 * 发送进入/离开域名消息到服务端
 */
public class ClientLogNetworking {

    /**
     * 发送进入域名消息到服务端
     * @param areaName 域名名称
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称（可为null）
     * @param dimensionalName 维度域名名称
     */
    public static void sendEnterAreaMessage(String areaName, int areaLevel, String surfaceName, String dimensionalName) {
        if (!ClientNetworkCompat.get().canSend(Channel.C2S_AREA_LOG)) {
            return;
        }

        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString("enter");
            buf.writeString(areaName);
            buf.writeInt(areaLevel);
            buf.writeString(surfaceName != null ? surfaceName : "");
            buf.writeString(dimensionalName != null ? dimensionalName : "");

            ClientNetworkCompat.get().sendToServer(Channel.C2S_AREA_LOG, buf);

            // 同时记录到客户端日志
            ClientLogManager.logEnterArea(areaLevel, surfaceName, areaName, dimensionalName);

        } catch (Exception e) {
            AreashintClient.LOGGER.error("发送进入域名消息失败", e);
        }
    }

    /**
     * 发送离开域名消息到服务端
     * @param areaName 域名名称
     * @param areaLevel 域名等级
     * @param surfaceName 联合域名名称（可为null）
     * @param dimensionalName 维度域名名称
     */
    public static void sendLeaveAreaMessage(String areaName, int areaLevel, String surfaceName, String dimensionalName) {
        if (!ClientNetworkCompat.get().canSend(Channel.C2S_AREA_LOG)) {
            return;
        }

        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString("leave");
            buf.writeString(areaName);
            buf.writeInt(areaLevel);
            buf.writeString(surfaceName != null ? surfaceName : "");
            buf.writeString(dimensionalName != null ? dimensionalName : "");

            ClientNetworkCompat.get().sendToServer(Channel.C2S_AREA_LOG, buf);

            // 同时记录到客户端日志
            ClientLogManager.logLeaveArea(areaLevel, surfaceName, areaName, dimensionalName);

        } catch (Exception e) {
            AreashintClient.LOGGER.error("发送离开域名消息失败", e);
        }
    }
}
