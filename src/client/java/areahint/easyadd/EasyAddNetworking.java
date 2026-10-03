package areahint.easyadd;

import areahint.data.AreaData;
import areahint.file.JsonHelper;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.debug.ClientDebugManager;
import net.minecraft.client.MinecraftClient;

/**
 * EasyAdd网络通信处理
 * 负责客户端与服务端的数据传输
 */
public class EasyAddNetworking {
    
    /**
     * 发送域名数据到服务端
     * @param areaData 域名数据
     * @param dimension 维度标识
     */
    public static void sendAreaDataToServer(AreaData areaData, String dimension) {
        try {
            // 序列化域名数据
            String jsonData = JsonHelper.toJsonSingle(areaData);
            
            // 创建数据包
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(jsonData);
            buf.writeString(dimension);
            
            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.C2S_EASYADD_AREA_DATA, buf);
            
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "向服务端发送域名数据: " + areaData.getName() + " (维度: " + dimension + ")");
            
        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                net.minecraft.text.Text.of(areahint.i18n.I18nManager.translate("easyadd.error.area") + e.getMessage()), false);
            
            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                "发送域名数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 注册客户端网络接收器
     */
    public static void registerClientReceivers() {
        // 注册服务端响应接收器
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_EASYADD_RESPONSE, 
            (client, buf) -> {
                try {
                    boolean success = buf.readBoolean();
                    String message = buf.readString();
                    int argCount = buf.readInt();
                    String[] args = new String[argCount];
                    for (int i = 0; i < argCount; i++) {
                        args[i] = buf.readString();
                    }

                    client.execute(() -> {
                        if (client.player != null) {
                            String translated = areahint.i18n.I18nManager.translate(message, (Object[]) args);
                            String color = success ? "§a" : "§c";
                            client.player.sendMessage(
                                net.minecraft.text.Text.of(color + translated), false);
                        }
                    });
                    
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                        "收到服务端响应: " + (success ? "成功" : "失败") + " - " + message);
                    
                } catch (Exception e) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.EASY_ADD,
                        "处理服务端响应时发生错误: " + e.getMessage());
                }
            });
    }
} 