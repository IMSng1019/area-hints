package areahint.description;

import areahint.AreashintClient;
import areahint.i18n.I18nManager;
import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端域名描述网络处理。
 */
public final class DescriptionClientNetworking {
    private DescriptionClientNetworking() {
    }

    public static void registerClientReceivers() {
        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DESCRIPTION_QUERY_RESPONSE,
            (client, buf) -> {
                String title = buf.readString(32767);
                String author = buf.readString(32767);
                String description = buf.readString(32767);
                client.execute(() -> BookDescriptionScreenUtil.openDescriptionBook(title, author, description));
            });

        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DESCRIPTION_AREA_LIST,
            (client, buf) -> {
                String operation = buf.readString();
                String targetType = buf.readString();
                String dimension = buf.readString();
                int count = buf.readInt();
                List<DescriptionListEntry> entries = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    entries.add(new DescriptionListEntry(
                        buf.readString(32767),
                        buf.readString(32767),
                        buf.readInt(),
                        buf.readString(32767),
                        buf.readString(32767),
                        buf.readString(32767)
                    ));
                }
                client.execute(() -> DescriptionManager.getInstance().receiveList(operation, targetType, dimension, entries));
            });

        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DESCRIPTION_EDIT_RESPONSE,
            (client, buf) -> {
                String targetType = buf.readString(32767);
                String dimensionType = buf.readString(32767);
                String targetName = buf.readString(32767);
                boolean success = buf.readBoolean();
                String message = buf.readString(32767);
                String title = buf.readString(32767);
                String description = buf.readString(32767);
                client.execute(() -> DescriptionManager.getInstance().receiveExistingDescription(
                    targetType, dimensionType, targetName, success, message, title, description));
            });

        ClientNetworkCompat.get().registerClientReceiver(Channel.S2C_DESCRIPTION_MUTATION_RESPONSE,
            (client, buf) -> {
                boolean success = buf.readBoolean();
                String message = buf.readString(32767);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal(message).formatted(success ? Formatting.GREEN : Formatting.RED), false);
                    }
                    DescriptionManager.getInstance().reset();
                });
            });
    }

    public static void sendQuery(String targetType, String dimensionType, String targetName) {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(clean(targetType));
        buf.writeString(clean(dimensionType));
        buf.writeString(clean(targetName));
        ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_QUERY, buf);
    }

    public static void sendCurrentAreaQuery() {
        if (ClientNetworkCompat.get().canSend(Channel.C2S_DESCRIPTION_CURRENT_QUERY)) {
            ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_CURRENT_QUERY, ClientNetworkCompat.get().newBuffer());
        }
    }

    public static void sendListRequest(String operation, String targetType, String dimensionType, String query) {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(clean(operation));
        buf.writeString(clean(targetType));
        buf.writeString(clean(dimensionType));
        buf.writeString(clean(query));
        ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_LIST_REQUEST, buf);
    }

    public static void sendEditQuery(String targetType, String dimensionType, String targetName) {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(clean(targetType));
        buf.writeString(clean(dimensionType));
        buf.writeString(clean(targetName));
        ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_EDIT_QUERY, buf);
    }

    public static boolean sendWrite(String targetType, String dimensionType, String targetName, String description) {
        if (description == null || description.length() > DescriptionServerNetworking.MAX_DESCRIPTION_LENGTH) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal(I18nManager.translate("description.error.too_long")).formatted(Formatting.RED), false);
            }
            return false;
        }
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(clean(targetType));
        buf.writeString(clean(dimensionType));
        buf.writeString(clean(targetName));
        buf.writeString(description);
        ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_WRITE, buf);
        return true;
    }

    public static void sendDelete(String targetType, String dimensionType, String targetName) {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        buf.writeString(clean(targetType));
        buf.writeString(clean(dimensionType));
        buf.writeString(clean(targetName));
        ClientNetworkCompat.get().sendToServer(Channel.C2S_DESCRIPTION_DELETE, buf);
    }

    private static String clean(String value) {
        return value == null ? "" : value;
    }
}
