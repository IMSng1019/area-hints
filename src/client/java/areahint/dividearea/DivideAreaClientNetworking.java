package areahint.dividearea;

import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.util.AreaDataConverter;
import areahint.network.TranslatableMessage;
import com.google.gson.JsonObject;

public class DivideAreaClientNetworking {

    /**
     * 发送分割后的两个域名数据到服务端
     */
    public static void sendDividedAreasToServer(AreaData area1, AreaData area2, String originalAreaName, String dimension) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            JsonObject json1 = AreaDataConverter.toJsonObject(area1);
            JsonObject json2 = AreaDataConverter.toJsonObject(area2);
            buf.writeString(json1.toString());
            buf.writeString(json2.toString());
            buf.writeString(originalAreaName);
            buf.writeString(dimension);
            ClientNetworkCompat.get().sendToServer(Channel.DIVIDE_AREA_CHANNEL, buf);
        } catch (Exception e) {
            System.err.println("发送分割域名数据失败: " + e.getMessage());
        }
    }

    /**
     * 注册客户端网络处理器
     */
    public static void registerClientNetworking() {
        ClientNetworkCompat.get().registerClientReceiver(
            Channel.DIVIDE_AREA_RESPONSE_CHANNEL,
            (client, buf) -> {
                try {
                    boolean success = buf.readBoolean();
                    net.minecraft.text.MutableText message = TranslatableMessage.read(buf);
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(
                                message.formatted(success ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED), false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );
    }
}
