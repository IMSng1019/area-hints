package areahint.signature;

import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.TranslatableMessage;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

/**
 * Signature客户端网络通信
 */
public class SignatureClientNetworking {

    public static void sendToServer(String operation, String areaName, String dimension, String targetPlayerName) {
        try {
            NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
            buf.writeString(operation);
            buf.writeString(areaName);
            buf.writeString(dimension);
            buf.writeString(targetPlayerName);
            ClientNetworkCompat.get().sendToServer(Channel.SIGNATURE_AREA_CHANNEL, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void registerClientReceivers() {
        ClientNetworkCompat.get().registerClientReceiver(
            Channel.SIGNATURE_AREA_RESPONSE_CHANNEL,
            (client, buf) -> {
                boolean success = buf.readBoolean();
                MutableText message = TranslatableMessage.read(buf);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                            message.formatted(success ? Formatting.GREEN : Formatting.RED), false);
                    }
                });
            }
        );
    }
}
