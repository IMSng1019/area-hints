package areahint.signature;

import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;
import areahint.network.TranslatableMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

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
                MutableComponent message = TranslatableMessage.read(buf);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendSystemMessage(
                            message.withStyle(success ? ChatFormatting.GREEN : ChatFormatting.RED));
                    }
                });
            }
        );
    }
}
