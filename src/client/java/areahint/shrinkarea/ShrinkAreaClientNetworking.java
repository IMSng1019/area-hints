package areahint.shrinkarea;

import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.TranslatableMessage;
import areahint.util.AreaDataConverter;
import net.minecraft.util.Identifier;
import com.google.gson.JsonObject;

/**
 * 收缩域名客户端网络通信
 * 负责客户端与服务端之间的收缩域名数据传输
 */
public class ShrinkAreaClientNetworking {
    
    /**
     * 发送收缩后的域名数据到服务端
     */
    public static void sendShrunkAreaToServer(AreaData shrunkArea, String dimension) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();

            // 将AreaData转换为JsonObject
            JsonObject areaJson = AreaDataConverter.toJsonObject(shrunkArea);

            // 写入数据
            buf.writeString(areaJson.toString());
            buf.writeString(dimension);  // 添加维度信息

            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.SHRINK_AREA_CHANNEL, buf);

            System.out.println("已发送收缩域名数据到服务端: " + shrunkArea.getName() + " (维度: " + dimension + ")");

        } catch (Exception e) {
            System.err.println("发送收缩域名数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 注册客户端网络处理器
     */
    public static void registerClientNetworking() {
        // 注册接收服务端响应的处理器
        ClientNetworkCompat.get().registerClientReceiver(
            Channel.SHRINK_AREA_RESPONSE_CHANNEL,
            (client, buf) -> {
                try {
                    boolean success = buf.readBoolean();
                    net.minecraft.text.MutableText message = TranslatableMessage.read(buf);

                    client.execute(() -> {
                        handleShrinkAreaResponse(success, message);
                    });
                    
                } catch (Exception e) {
                    System.err.println("处理收缩域名响应时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        );
    }
    
    /**
     * 处理收缩域名响应
     */
    private static void handleShrinkAreaResponse(boolean success, net.minecraft.text.MutableText message) {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(
                    message.formatted(success ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED),
                    false
                );
            }
        });
    }
} 