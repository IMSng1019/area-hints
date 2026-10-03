package areahint.subtitle;

import areahint.AreashintClient;
import areahint.data.AreaData;
import areahint.file.JsonHelper;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.NetworkReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * 副字幕客户端网络处理。
 * <p>
 * 负责接收服务端下发的可操作域名列表，并把玩家确认后的副字幕修改请求发送回服务端。
 */
public class SubtitleNetworking {
    public static void registerClientReceivers() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_SUBTITLE_RESPONSE,
            (client, buf) -> {
                try {
                    String action = buf.readString();
                    if ("subtitle_response".equals(action)) {
                        handleMutationResponse(client, buf);
                    } else {
                        handleAreaList(client, action, buf);
                    }
                } catch (Exception e) {
                    AreashintClient.LOGGER.error("处理副字幕网络响应时发生错误", e);
                }
            });
    }

    public static void sendMutation(String mutation, String areaName, String value, String dimension) {
        sendMutation(mutation, areaName, value, dimension, "");
    }

    public static void sendMutation(String mutation, String areaName, String value, String dimension, String extraValue) {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(mutation);
        buf.writeString(areaName != null ? areaName : "");
        buf.writeString(value != null ? value : "");
        buf.writeString(dimension != null ? dimension : "");
        buf.writeString(extraValue != null ? extraValue : "");
        ClientNetworkCompat.get().sendToServer(Channel.C2S_SUBTITLE_MUTATION, buf);
    }

    private static void handleAreaList(MinecraftClient client, String action, NetworkReader buf) {
        String dimension = buf.readString();
        int count = buf.readInt();
        List<AreaData> areas = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            AreaData area = JsonHelper.fromJsonSingle(buf.readString());
            if (area != null) {
                areas.add(area);
            }
        }

        client.execute(() -> {
            SubtitleManager manager = SubtitleManager.getInstance();
            switch (action) {
                case "addsubtitle_interactive":
                    manager.startAddSubtitle(areas, dimension);
                    break;
                case "deletesubtitle_interactive":
                    manager.startDeleteSubtitle(areas, dimension);
                    break;
                case "replacesubtitlecolor_interactive":
                    manager.startReplaceSubtitleColor(areas, dimension);
                    break;
                default:
                    AreashintClient.LOGGER.warn("未知副字幕列表响应: " + action);
                    break;
            }
        });
    }

    private static void handleMutationResponse(MinecraftClient client, NetworkReader buf) {
        boolean success = buf.readBoolean();
        String message = buf.readString();

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.of((success ? "§a" : "§c") + message), false);
            }
        });
    }
}
