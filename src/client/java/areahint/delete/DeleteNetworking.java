package areahint.delete;

import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import areahint.file.JsonHelper;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Delete网络通信处理
 * 负责客户端与服务端的删除请求传输
 */
public class DeleteNetworking {

    /**
     * 请求服务端发送可删除的域名列表
     * @param dimension 维度标识
     */
    public static void requestDeletableAreas(String dimension) {
        try {
            // 创建数据包
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(dimension);

            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.C2S_REQUEST_DELETABLE_AREAS, buf);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "向服务端请求可删除域名列表 (维度: " + dimension + ")");

        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                net.minecraft.text.Text.of(I18nManager.translate("message.error.area.delete.list") + e.getMessage()), false);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "请求可删除域名列表失败: " + e.getMessage());
        }
    }

    /**
     * 发送删除请求到服务端
     * @param areaName 要删除的域名名称
     * @param dimension 维度标识
     */
    public static void sendDeleteRequestToServer(String areaName, String dimension) {
        try {
            // 创建数据包
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(areaName);
            buf.writeString(dimension);

            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.C2S_DELETE_AREA, buf);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "向服务端发送删除请求: " + areaName + " (维度: " + dimension + ")");

        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                net.minecraft.text.Text.of(I18nManager.translate("message.error.delete") + e.getMessage()), false);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                "发送删除请求失败: " + e.getMessage());
        }
    }

    /**
     * 注册客户端网络接收器
     */
    public static void registerClientReceivers() {
        // 注册可删除域名列表接收器
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DELETABLE_AREAS_LIST,
            (client, buf) -> {
                try {
                    int count = buf.readInt();
                    List<AreaData> areas = new ArrayList<>();

                    for (int i = 0; i < count; i++) {
                        String json = buf.readString();
                        AreaData area = JsonHelper.fromJsonSingle(json);
                        if (area != null) {
                            areas.add(area);
                        }
                    }

                    client.execute(() -> {
                        DeleteManager.getInstance().receiveDeletableAreas(areas);
                    });

                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                        "收到服务端可删除域名列表: " + count + " 个域名");

                } catch (Exception e) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                        "处理可删除域名列表时发生错误: " + e.getMessage());
                }
            });

        // 注册服务端响应接收器
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DELETE_RESPONSE,
            (client, buf) -> {
                try {
                    boolean success = buf.readBoolean();
                    String message = buf.readString();

                    client.execute(() -> {
                        if (client.player != null) {
                            if (success) {
                                client.player.sendMessage(
                                    net.minecraft.text.Text.of("§a" + I18nManager.translate("message.success.area.delete") + message), false);
                            } else {
                                client.player.sendMessage(
                                    net.minecraft.text.Text.of("§c" + message), false);
                            }
                        }
                    });

                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                        "收到服务端删除响应: " + (success ? "成功" : "失败") + " - " + message);

                } catch (Exception e) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.AREA_DETECTION,
                        "处理服务端删除响应时发生错误: " + e.getMessage());
                }
            });
    }
}
