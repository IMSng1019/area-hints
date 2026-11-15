package areahint.handshake;

import areahint.Areashint;
import areahint.config.ServerConfig;
import areahint.i18n.ServerI18nManager;
import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class ServerModHandshake {
    private ServerModHandshake() { }

    public static void register() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            if (!ServerConfig.isForceUseAreahint()) {
                return;
            }
            FriendlyByteBuf query = FriendlyByteBufs.create();
            query.writeVarInt(ModHandshake.PROTOCOL_VERSION);
            query.writeUtf(ModHandshake.localVersion(), ModHandshake.MAX_VERSION_LENGTH);
            // Fabric 会等待查询的应答；无模组客户端会返回不支持，仍停留在登录阶段。
            sender.sendPacket(ModHandshake.CHANNEL, query);
        });

        ServerLoginNetworking.registerGlobalReceiver(ModHandshake.CHANNEL,
                (server, handler, understood, response, synchronizer, sender) -> {
                    boolean accepted = false;
                    if (understood && response != null) {
                        try {
                            int protocol = response.readVarInt();
                            String clientVersion = response.readUtf(ModHandshake.MAX_VERSION_LENGTH);
                            accepted = protocol == ModHandshake.PROTOCOL_VERSION
                                    && !response.isReadable()
                                    && ModHandshake.isCompatible(clientVersion, ModHandshake.localVersion());
                        } catch (RuntimeException malformed) {
                            Areashint.LOGGER.warn("客户端域名模组握手内容不合法，拒绝登录");
                        }
                    }
                    if (!accepted) {
                        // 将拒绝操作交给服务端线程，并阻止登录状态在任务执行之前继续推进。
                        synchronizer.waitFor(server.submit(() -> handler.disconnect(requiredModMessage())));
                    }
                });
    }

    private static Component requiredModMessage() {
        // 使用服务端已翻译的字面文本，未安装模组的客户端也能看到提示和服务端版本。
        String message = ServerI18nManager.translate("message.handshake.required", ModHandshake.localVersion());
        Style linkStyle = Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true);
        /*linkStyle = linkStyle.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ModHandshake.DOWNLOAD_URL));
        */
        linkStyle = linkStyle.withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(ModHandshake.DOWNLOAD_URL)));
        return Component.literal(message).append(Component.literal(ModHandshake.DOWNLOAD_URL).setStyle(linkStyle));
    }
}
