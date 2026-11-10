package areahint.network;

import areahint.Areashint;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// 大型域名文件只向明确声明支持的新客户端分片同步，旧客户端仍使用原来的单包格式。
public final class AreaDataChunkNetworking {
    public static final int CHUNK_SIZE = 24_000;
    public static final int MAX_LENGTH = 8_000_000;
    private static final Set<UUID> READY = new HashSet<>();

    private AreaDataChunkNetworking() { }

    public static void register() {
        NetworkCompat.get().registerServerReceiver(Channel.C2S_AREA_DATA_CHUNK_READY, (server, player, reader) -> {
            try {
                int protocol = reader.readInt();
                if (protocol != 1 || reader.isReadable()) return;
                server.execute(() -> {
                    if (server.getPlayerManager().getPlayer(player.getUuid()) != player) return;
                    if (READY.add(player.getUuid())) ServerNetworking.sendAllAreaDataToClient(player);
                });
            } catch (RuntimeException exception) {
                Areashint.LOGGER.warn("忽略无效域名分片握手: {}", player.getGameProfile().getName());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> READY.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> READY.clear());
    }

    public static void send(ServerPlayerEntity player, String dimension, String document) {
        if (!READY.contains(player.getUuid())) {
            Areashint.LOGGER.warn("客户端尚不支持大型域名分片同步，跳过 {} 的 {} 数据", player.getGameProfile().getName(), dimension);
            return;
        }
        if (document.length() > MAX_LENGTH) {
            Areashint.LOGGER.error("域名文件超过分片同步上限，保留正式文件并跳过同步: {} ({} 字符)", dimension, document.length());
            return;
        }
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < document.length();) {
            int end = Math.min(document.length(), start + CHUNK_SIZE);
            // UTF-16 高低代理项不能分开发送，保证自定义属性和域名中的表情原样恢复。
            if (end < document.length() && Character.isHighSurrogate(document.charAt(end - 1))) end--;
            chunks.add(document.substring(start, end));
            start = end;
        }
        String id = UUID.randomUUID().toString();
        for (int index = 0; index < chunks.size(); index++) {
            NetworkBuf buffer = NetworkCompat.get().newBuffer();
            buffer.writeString(id);
            buffer.writeString(dimension);
            buffer.writeInt(index);
            buffer.writeInt(chunks.size());
            buffer.writeString(chunks.get(index));
            NetworkCompat.get().sendToPlayer(player, Channel.S2C_AREA_DATA_CHUNK, buffer);
        }
    }
}
