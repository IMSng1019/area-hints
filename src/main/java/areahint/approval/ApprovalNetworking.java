package areahint.approval;

import areahint.Areashint;
import areahint.config.ServerConfig;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// 审批通道独立于正式域名同步，普通玩家只能取得自己的记录，管理员只取得待审批列表。
public final class ApprovalNetworking {
    private static final int CHUNK_SIZE = 24000;
    // 1.20.4 的 C2S 自定义包上限为 32767 字节，8000 字符保证中文及包头也不会超限。
    private static final int REQUEST_CHUNK_SIZE = 8000;
    private static final Set<UUID> SUBSCRIBERS = new HashSet<>();
    private static final Map<UUID, Incoming> INCOMING = new HashMap<>();

    private ApprovalNetworking() { }

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
            (handler, server) -> {
                SUBSCRIBERS.remove(handler.player.getUuid());
                INCOMING.remove(handler.player.getUuid());
            });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SUBSCRIBERS.clear();
            INCOMING.clear();
        });
        NetworkCompat.get().registerServerReceiver(Channel.C2S_APPROVAL_ACTION, (server, player, reader) -> {
            String json;
            try {
                json = reader.readString(32767);
            } catch (Exception e) {
                return;
            }
            server.execute(() -> handleAction(player, json));
        });
        NetworkCompat.get().registerServerReceiver(Channel.C2S_APPROVAL_CHUNK, (server, player, reader) -> {
            try {
                String id = reader.readString(128);
                int index = reader.readInt();
                int count = reader.readInt();
                String chunk = reader.readString(REQUEST_CHUNK_SIZE);
                server.execute(() -> receiveChunk(player, id, index, count, chunk));
            } catch (RuntimeException e) {
                server.execute(() -> INCOMING.remove(player.getUuid()));
            }
        });
    }

    private static void handleAction(ServerPlayerEntity player, String json) {
        if (player.getServer() == null || player.getServer().getPlayerManager().getPlayer(player.getUuid()) != player) {
            return;
        }
        String operation = "";
        String requestId = "";
        try {
            JsonObject action = JsonParser.parseString(json).getAsJsonObject();
            operation = ApprovalManager.string(action, "action");
            if (action.has("id")) {
                requestId = ApprovalManager.string(action, "id");
            }
            // 只有主动请求过审批协议的客户端才接收广播，旧版客户端不会收到未知数据包。
            SUBSCRIBERS.add(player.getUuid());
            switch (operation) {
                case "sync", "list" -> sendSnapshot(player);
                case "decision" -> ApprovalManager.decide(player, action);
                case "draft" -> ApprovalManager.saveDraft(player, action);
                case "resubmit" -> ApprovalManager.resubmit(player, action);
                default -> sendResult(player, false, "approval.error.invalid", operation, "");
            }
        } catch (ApprovalManager.ApprovalException e) {
            sendResult(player, false, e.getMessage(), operation, requestId);
            sendSnapshot(player);
        } catch (java.io.IOException e) {
            Areashint.LOGGER.error("保存审批操作失败", e);
            sendResult(player, false, "approval.error.storage", operation, requestId);
        } catch (RuntimeException e) {
            Areashint.LOGGER.warn("拒绝无效审批消息: {}", player.getGameProfile().getName(), e);
            sendResult(player, false, "approval.error.invalid", operation, requestId);
        }
    }

    // 每名玩家最多缓存一个有时限的消息，完整重组后才进入与单包相同的授权和版本检查。
    private static void receiveChunk(ServerPlayerEntity player, String id, int index, int count, String chunk) {
        if (count < 1 || count > 1024 || index < 0 || index >= count) {
            INCOMING.remove(player.getUuid());
            return;
        }
        if (player.getServer() == null || player.getServer().getPlayerManager().getPlayer(player.getUuid()) != player) {
            INCOMING.remove(player.getUuid());
            return;
        }
        Incoming incoming = INCOMING.get(player.getUuid());
        if (incoming == null || !incoming.id.equals(id) || System.currentTimeMillis() - incoming.started > 30_000L) {
            if (index != 0) {
                INCOMING.remove(player.getUuid());
                return;
            }
            incoming = new Incoming(id, count);
            INCOMING.put(player.getUuid(), incoming);
        }
        if (incoming.chunks.length != count) {
            INCOMING.remove(player.getUuid());
            return;
        }
        if (incoming.chunks[index] == null) {
            incoming.chunks[index] = chunk;
            incoming.length += chunk.length();
            incoming.received++;
        }
        if (incoming.length > 8_000_000) {
            INCOMING.remove(player.getUuid());
            sendResult(player, false, "approval.error.invalid", "", "");
            return;
        }
        if (incoming.received == count) {
            INCOMING.remove(player.getUuid());
            handleAction(player, String.join("", incoming.chunks));
        }
    }

    private static final class Incoming {
        private final String id;
        private final String[] chunks;
        private final long started = System.currentTimeMillis();
        private int length;
        private int received;

        private Incoming(String id, int count) {
            this.id = id;
            this.chunks = new String[count];
        }
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        JsonObject data = new JsonObject();
        data.add("config", ServerConfig.toJson());
        data.addProperty("operator", ApprovalManager.isOperator(player));
        data.add("requests", ApprovalManager.GSON.toJsonTree(ApprovalManager.visibleRequests(player)));
        String json = ApprovalManager.GSON.toJson(data);
        String transfer = UUID.randomUUID().toString();
        var chunks = new ArrayList<String>();
        for (int start = 0; start < json.length();) {
            int end = Math.min(json.length(), start + CHUNK_SIZE);
            // UTF-16 代理对必须在同一包中编码，避免意见中的表情在分包边界损坏。
            if (end < json.length() && Character.isHighSurrogate(json.charAt(end - 1))) {
                end--;
            }
            chunks.add(json.substring(start, end));
            start = end;
        }
        int count = chunks.size();
        // 大列表按字符切片传输，单包始终低于各版本既有字符串上限。
        for (int index = 0; index < count; index++) {
            NetworkBuf buffer = NetworkCompat.get().newBuffer();
            buffer.writeString(transfer);
            buffer.writeInt(index);
            buffer.writeInt(count);
            buffer.writeString(chunks.get(index));
            NetworkCompat.get().sendToPlayer(player, Channel.S2C_APPROVAL_DATA, buffer);
        }
    }

    public static void sendResult(ServerPlayerEntity player, boolean success, String messageKey, String action, String id) {
        JsonObject result = new JsonObject();
        result.addProperty("success", success);
        result.addProperty("messageKey", messageKey);
        result.addProperty("action", action);
        result.addProperty("id", id);
        NetworkBuf buffer = NetworkCompat.get().newBuffer();
        buffer.writeString(result.toString());
        NetworkCompat.get().sendToPlayer(player, Channel.S2C_APPROVAL_RESULT, buffer);
    }

    public static void broadcast(ApprovalRequest request, boolean notifyApplicant) {
        var server = Areashint.getServer();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
            boolean applicant = online.getUuidAsString().equals(request.applicantId);
            if (SUBSCRIBERS.contains(online.getUuid()) && (applicant || ApprovalManager.isOperator(online))) {
                try {
                    sendSnapshot(online);
                } catch (RuntimeException e) {
                    // 通知失败不回滚已持久化的申请或结果，玩家下次上线仍可主动取得完整记录。
                    Areashint.LOGGER.warn("发送审批同步失败: {}", online.getGameProfile().getName(), e);
                }
            }
            if (applicant && notifyApplicant) {
                online.sendMessage(Text.translatable("approval.message.reply_received"), false);
            }
        }
    }
}
