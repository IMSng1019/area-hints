package areahint.vizedit;

import areahint.Areashint;
import areahint.approval.ApprovalManager;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import com.google.gson.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.IOException;
import java.util.*;

// 独立分片通道兼容旧版 32767 字节 C2S 上限，每名玩家只缓存一个有界传输。
public final class VizEditNetworking {
    public static final int CHUNK_SIZE = 8000;
    public static final int MAX_CHUNKS = 1024;
    public static final int MAX_MESSAGE_LENGTH = 8_000_000;
    public static final long TRANSFER_TIMEOUT = 30_000L;
    private static final Map<UUID, Incoming> INCOMING = new HashMap<>();
    private static final Map<UUID, LinkedHashMap<String, Completed>> COMPLETED = new HashMap<>();

    private VizEditNetworking() { }

    public static void register() {
        NetworkCompat.get().registerServerReceiver(Channel.C2S_VIZEDIT_ACTION, (server, player, reader) -> {
            try {
                String json = reader.readString(CHUNK_SIZE);
                server.execute(() -> handle(player, json));
            } catch (RuntimeException exception) {
                server.execute(() -> failure(player, "", "", "vizedit.error.invalid"));
            }
        });
        NetworkCompat.get().registerServerReceiver(Channel.C2S_VIZEDIT_CHUNK, (server, player, reader) -> {
            try {
                String id = reader.readString(128);
                int index = reader.readInt();
                int count = reader.readInt();
                String chunk = reader.readString(CHUNK_SIZE);
                server.execute(() -> receive(player, id, index, count, chunk));
            } catch (RuntimeException exception) {
                server.execute(() -> {
                    INCOMING.remove(player.getUuid());
                    failure(player, "", "", "vizedit.error.invalid");
                });
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player.getUuid()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> clear(player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            INCOMING.clear();
            COMPLETED.clear();
            VizEditServerService.clearAll();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, Incoming>> iterator = INCOMING.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (now - entry.getValue().started <= TRANSFER_TIMEOUT) continue;
                iterator.remove();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) failure(player, entry.getValue().id, "", "vizedit.error.timeout");
            }
            COMPLETED.values().forEach(cache -> cache.values().removeIf(entry -> now - entry.time > 5 * 60_000L));
            COMPLETED.values().removeIf(Map::isEmpty);
        });
    }

    private static void clear(UUID player) {
        INCOMING.remove(player);
        COMPLETED.remove(player);
        VizEditServerService.clear(player);
    }

    private static boolean connected(ServerPlayerEntity player) {
        return player.getServer() != null && player.getServer().getPlayerManager().getPlayer(player.getUuid()) == player;
    }

    private static void handle(ServerPlayerEntity player, String json) {
        if (!connected(player)) return;
        String requestId = "";
        String sessionId = "";
        try {
            if (json.length() > MAX_MESSAGE_LENGTH) throw VizEditChangeService.error("size");
            JsonObject action = JsonParser.parseString(json).getAsJsonObject();
            requestId = VizEditChangeService.text(action, "requestId", false);
            sessionId = VizEditChangeService.text(action, "sessionId", true);
            String operation = VizEditChangeService.text(action, "action", false);
            if ("open".equals(operation)) {
                sendData(player, VizEditServerService.open(player, action));
            } else if ("submit".equals(operation)) {
                LinkedHashMap<String, Completed> cache = COMPLETED.computeIfAbsent(player.getUuid(), key -> new LinkedHashMap<>());
                Completed previous = cache.get(requestId);
                if (previous != null) {
                    if (!previous.input.equals(json)) throw VizEditChangeService.error("invalid");
                    sendResult(player, previous.result);
                    return;
                }
                JsonObject result = VizEditServerService.submit(player, action);
                // 成功结果先缓存再发送，确认包丢失后的同请求重试不会产生第二次新建或重复审批。
                cache.put(requestId, new Completed(json, result.deepCopy(), System.currentTimeMillis()));
                while (cache.size() > 8) cache.remove(cache.keySet().iterator().next());
                sendResult(player, result);
            } else if ("close".equals(operation)) {
                VizEditServerService.close(player.getUuid(), sessionId);
            } else throw VizEditChangeService.error("invalid");
        } catch (VizEditChangeService.EditException exception) {
            failure(player, requestId, sessionId, exception.getMessage());
        } catch (ApprovalManager.ApprovalException exception) {
            failure(player, requestId, sessionId, exception.getMessage());
        } catch (IOException exception) {
            Areashint.LOGGER.error("保存可视化域名编辑失败", exception);
            failure(player, requestId, sessionId, "vizedit.error.storage");
        } catch (RuntimeException exception) {
            Areashint.LOGGER.warn("拒绝无效可视化编辑消息: {}", player.getGameProfile().getName(), exception);
            failure(player, requestId, sessionId, "vizedit.error.invalid");
        }
    }

    private static void receive(ServerPlayerEntity player, String id, int index, int count, String chunk) {
        if (!connected(player)) return;
        if (!VizEditServerService.canOpen(player)) { failure(player, id, "", "vizedit.error.denied"); return; }
        if (id.isBlank() || count < 1 || count > MAX_CHUNKS || index < 0 || index >= count) {
            INCOMING.remove(player.getUuid());
            failure(player, id, "", "vizedit.error.size");
            return;
        }
        Incoming incoming = INCOMING.get(player.getUuid());
        if (incoming == null || !incoming.id.equals(id)) {
            if (index != 0) { failure(player, id, "", "vizedit.error.invalid"); return; }
            incoming = new Incoming(id, count);
            INCOMING.put(player.getUuid(), incoming);
        }
        if (incoming.chunks.length != count || System.currentTimeMillis() - incoming.started > TRANSFER_TIMEOUT
                || incoming.chunks[index] != null && !incoming.chunks[index].equals(chunk)) {
            INCOMING.remove(player.getUuid());
            failure(player, id, "", "vizedit.error.timeout");
            return;
        }
        if (incoming.chunks[index] == null) {
            incoming.chunks[index] = chunk;
            incoming.length += chunk.length();
            incoming.received++;
        }
        if (incoming.length > MAX_MESSAGE_LENGTH) {
            INCOMING.remove(player.getUuid());
            failure(player, id, "", "vizedit.error.size");
        } else if (incoming.received == count) {
            INCOMING.remove(player.getUuid());
            handle(player, String.join("", incoming.chunks));
        }
    }

    private static void sendData(ServerPlayerEntity player, JsonObject data) {
        String json = data.toString();
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < json.length();) {
            int end = Math.min(json.length(), start + CHUNK_SIZE);
            if (end < json.length() && Character.isHighSurrogate(json.charAt(end - 1))) end--;
            chunks.add(json.substring(start, end));
            start = end;
        }
        String id = UUID.randomUUID().toString();
        for (int index = 0; index < chunks.size(); index++) {
            NetworkBuf buffer = NetworkCompat.get().newBuffer();
            buffer.writeString(id);
            buffer.writeInt(index);
            buffer.writeInt(chunks.size());
            buffer.writeString(chunks.get(index));
            NetworkCompat.get().sendToPlayer(player, Channel.S2C_VIZEDIT_DATA, buffer);
        }
    }

    private static void failure(ServerPlayerEntity player, String requestId, String sessionId, String key) {
        JsonObject result = new JsonObject();
        result.addProperty("requestId", requestId);
        result.addProperty("sessionId", sessionId);
        result.addProperty("success", false);
        result.addProperty("messageKey", key);
        result.addProperty("outcome", "error");
        sendResult(player, result);
    }

    private static void sendResult(ServerPlayerEntity player, JsonObject result) {
        NetworkBuf buffer = NetworkCompat.get().newBuffer();
        buffer.writeString(result.toString());
        NetworkCompat.get().sendToPlayer(player, Channel.S2C_VIZEDIT_RESULT, buffer);
    }

    private record Completed(String input, JsonObject result, long time) { }

    private static final class Incoming {
        private final String id;
        private final String[] chunks;
        private final long started = System.currentTimeMillis();
        private int length;
        private int received;
        private Incoming(String id, int count) { this.id = id; chunks = new String[count]; }
    }
}
