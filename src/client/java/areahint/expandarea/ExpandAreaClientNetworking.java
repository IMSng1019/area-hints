package areahint.expandarea;

import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.TranslatableMessage;
import areahint.util.AreaDataConverter;
import com.google.gson.JsonObject;

public class ExpandAreaClientNetworking {
    
    /**
     * 发送扩展后的域名数据到服务端
     */
    public static void sendExpandedAreaToServer(AreaData expandedArea, String dimension) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();

            // 将AreaData转换为JsonObject
            JsonObject areaJson = AreaDataConverter.toJsonObject(expandedArea);

            // 写入数据
            buf.writeString(areaJson.toString());
            buf.writeString(dimension);  // 添加维度信息

            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.EXPAND_AREA_CHANNEL, buf);

            System.out.println("已发送扩展域名数据到服务端: " + expandedArea.getName() + " (维度: " + dimension + ")");

        } catch (Exception e) {
            System.err.println("发送扩展域名数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册客户端网络处理器
     */
    public static void registerClientNetworking() {
        // 注册接收服务端响应的处理器
        ClientNetworkCompat.get().registerClientReceiver(
            Channel.EXPAND_AREA_RESPONSE_CHANNEL,
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