package areahint.deletehint;

import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.TranslatableMessage;
import areahint.util.AreaDataConverter;
import com.google.gson.JsonObject;

/**
 * DeleteHint客户端网络通信
 */
public class DeleteHintClientNetworking {

    /**
     * 发送修改后的域名数据到服务端
     */
    public static void sendToServer(AreaData area, String dimension) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            JsonObject json = AreaDataConverter.toJsonObject(area);
            buf.writeString(json.toString());
            buf.writeString(dimension);
            ClientNetworkCompat.get().sendToServer(Channel.DELETEHINT_AREA_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册客户端网络接收器
     */
    public static void registerClientReceivers() {
        ClientNetworkCompat.get().registerClientReceiver(
            Channel.DELETEHINT_AREA_RESPONSE_CHANNEL,
            (client, buf) -> {
                boolean success = buf.readBoolean();
                net.minecraft.text.MutableText message = TranslatableMessage.read(buf);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                            message.formatted(success ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED), false);
                    }
                });
            }
        );
    }
}
