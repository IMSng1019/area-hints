package areahint.rename;

import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.NetworkReader;
import areahint.debug.ClientDebugManager;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;

/**
 * Rename网络通信处理
 * 负责客户端与服务端的数据传输
 */
public class RenameNetworking {

    /**
     * 发送重命名请求到服务端
     * @param oldName 原域名名称
     * @param newName 新域名名称
     * @param newSurfaceName 新联合域名（可为null）
     * @param dimension 维度标识
     */
    public static void sendRenameRequest(String oldName, String newName, String newSurfaceName, String dimension) {
        try {
            // 创建数据包
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(oldName);
            buf.writeString(newName);
            buf.writeString(newSurfaceName != null ? newSurfaceName : ""); // 空字符串表示null
            buf.writeString(dimension);

            // 发送到服务端
            ClientNetworkCompat.get().sendToServer(Channel.C2S_RENAME_REQUEST, buf);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                "向服务端发送重命名请求: " + oldName + " -> " + newName + " (维度: " + dimension + ")");

        } catch (Exception e) {
            MinecraftClient.getInstance().player.sendMessage(
                net.minecraft.text.Text.of(I18nManager.translate("message.error.rename") + e.getMessage()), false);

            ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                "发送重命名请求失败: " + e.getMessage());
        }
    }

    /**
     * 注册客户端网络接收器（在 ClientNetworking 中调用）
     */
    public static void registerClientReceivers() {
        // 注册服务端响应接收器
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_RENAME_RESPONSE,
            (client, buf) -> {
                try {
                    String action = buf.readString();

                    if ("rename_list".equals(action)) {
                        // 处理可重命名域名列表响应
                        handleRenameListResponse(client, buf);
                    } else if ("rename_response".equals(action)) {
                        // 处理重命名结果响应
                        handleRenameResultResponse(client, buf);
                    }

                } catch (Exception e) {
                    ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                        "处理服务端响应时发生错误: " + e.getMessage());
                }
            });
    }

    /**
     * 处理域名列表响应
     */
    private static void handleRenameListResponse(MinecraftClient client, NetworkReader buf) {
        String dimension = buf.readString();
        int count = buf.readInt();

        // 读取域名列表
        java.util.List<areahint.data.AreaData> areas = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                String areaName = buf.readString();
                String signature = buf.readString();

                // 创建简化的AreaData对象（只包含必要信息）
                areahint.data.AreaData area = new areahint.data.AreaData();
                area.setName(areaName);
                area.setSignature(signature.isEmpty() ? null : signature);

                areas.add(area);
            } catch (Exception e) {
                ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
                    "读取域名信息时出错: " + e.getMessage());
            }
        }

        client.execute(() -> {
            if (client.player != null) {
                if (areas.isEmpty()) {
                    client.player.sendMessage(net.minecraft.text.Text.of(I18nManager.translate("message.error.area.dimension.rename")), false);
                } else {
                    // 启动交互式Rename流程
                    RenameManager.getInstance().startRename(areas, dimension);
                }
            }
        });
    }

    /**
     * 处理重命名结果响应
     */
    private static void handleRenameResultResponse(MinecraftClient client, NetworkReader buf) {
        boolean success = buf.readBoolean();
        // 与服务端结构化消息协议保持一致，审批提交提示也按原翻译片段显示。
        net.minecraft.text.MutableText message = areahint.network.TranslatableMessage.read(buf);

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(message.formatted(success
                    ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED), false);
            }
        });

        ClientDebugManager.sendDebugInfo(ClientDebugManager.DebugCategory.NETWORK,
            "收到服务端响应: " + (success ? "成功" : "失败") + " - " + message);
    }
}
