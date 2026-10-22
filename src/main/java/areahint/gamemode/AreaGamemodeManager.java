package areahint.gamemode;

import areahint.Areashint;
import areahint.config.ServerConfig;
import areahint.file.JsonHelper;
import areahint.data.AreaData;
import areahint.network.Channel;
import areahint.network.NetworkBuf;
import areahint.network.NetworkCompat;
import areahint.network.Packets;
import areahint.world.WorldFolderManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 所有状态只在服务端线程访问；客户端消息仅触发复核，绝不提供可信坐标、域名或目标模式。
public final class AreaGamemodeManager {
    private static final Map<String, CachedAreas> DIMENSIONS = new HashMap<>();
    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> SUBSCRIBERS = new HashMap<>();

    private AreaGamemodeManager() { }

    public static void register() {
        NetworkCompat.get().registerServerReceiver(Channel.C2S_AREA_GAMEMODE_POLICY, (server, player, reader) ->
            server.execute(() -> {
                if (!isConnected(server, player)) {
                    return;
                }
                Integer previous = SUBSCRIBERS.get(player.getUuid());
                if (previous != null && server.getTicks() - previous < 20) {
                    return;
                }
                SUBSCRIBERS.put(player.getUuid(), server.getTicks());
                sendPolicy(player);
            }));
        NetworkCompat.get().registerServerReceiver(Channel.C2S_AREA_GAMEMODE_CHECK, (server, player, reader) ->
            server.execute(() -> {
                if (ServerConfig.isForceUseAreahint() && isConnected(server, player)) {
                    // 同一玩家的通知合并，并延后一刻读取坐标，避开通知先于本刻移动包到达的时序。
                    PENDING.putIfAbsent(player.getUuid(), server.getTicks() + 1);
                }
            }));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> refreshPlayer(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            PLAYERS.remove(id);
            PENDING.remove(id);
            SUBSCRIBERS.remove(id);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> refreshPlayer(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
            (player, origin, destination) -> refreshPlayer(player));
        ServerTickEvents.END_SERVER_TICK.register(AreaGamemodeManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DIMENSIONS.clear();
            PLAYERS.clear();
            PENDING.clear();
            SUBSCRIBERS.clear();
        });
    }

    public static void reload(MinecraftServer server) {
        DIMENSIONS.clear();
        PLAYERS.clear();
        PENDING.clear();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (SUBSCRIBERS.containsKey(player.getUuid())) {
                sendPolicy(player);
            }
            apply(player);
        }
    }

    private static void sendPolicy(ServerPlayerEntity player) {
        NetworkBuf buffer = NetworkCompat.get().newBuffer();
        buffer.writeBoolean(ServerConfig.isForceUseAreahint());
        buffer.writeString(ServerConfig.getDefaultGamemode().getName());
        NetworkCompat.get().sendToPlayer(player, Channel.S2C_AREA_GAMEMODE_POLICY, buffer);
    }

    private static boolean isConnected(MinecraftServer server, ServerPlayerEntity player) {
        return server.getPlayerManager().getPlayer(player.getUuid()) == player;
    }

    private static void refreshPlayer(ServerPlayerEntity player) {
        PLAYERS.remove(player.getUuid());
        apply(player);
    }

    private static void tick(MinecraftServer server) {
        if (!ServerConfig.isForceUseAreahint()) {
            PENDING.clear();
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Integer due = PENDING.get(player.getUuid());
            if (due != null && due <= server.getTicks()) {
                PENDING.remove(player.getUuid());
                apply(player);
            } else if (server.getTicks() % 20 == 0) {
                // 每秒兜底复核一次，弥补跨边界同步延迟、客户端停用标题检测及服务端传送。
                apply(player);
            }
        }
    }

    private static void apply(ServerPlayerEntity player) {
        if (!ServerConfig.isForceUseAreahint()) {
            return;
        }
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        AreaGamemodeResolver resolver = resolverFor(player);
        GameMode desired = AreaGamemodeResolver.requiredMode(resolver == null ? null
            : resolver.findArea(player.getX(), player.getY(), player.getZ()), ServerConfig.getDefaultGamemode());
        PlayerState previous = PLAYERS.put(player.getUuid(), new PlayerState(dimension, desired));
        // 同一要求下不反复覆盖管理员的临时模式，仅在要求变化或新玩家实体进入时应用。
        if (previous == null || previous.mode != desired || !previous.dimension.equals(dimension)) {
            if (player.interactionManager.getGameMode() != desired) {
                player.changeGameMode(desired);
            }
        }
    }

    private static AreaGamemodeResolver resolverFor(ServerPlayerEntity player) {
        // 完整维度标识必须匹配，避免自定义命名空间下同名维度误读主世界或下界的文件。
        String type = switch (player.getWorld().getRegistryKey().getValue().toString()) {
            case "minecraft:overworld" -> Packets.DIMENSION_OVERWORLD;
            case "minecraft:the_nether" -> Packets.DIMENSION_NETHER;
            case "minecraft:the_end" -> Packets.DIMENSION_END;
            default -> null;
        };
        String filename = Packets.getFileNameForDimension(type);
        if (filename == null) {
            return null;
        }
        Path file = WorldFolderManager.getWorldDimensionFile(filename);
        try {
            FileTime modified = Files.exists(file) ? Files.getLastModifiedTime(file) : FileTime.fromMillis(0);
            long size = Files.exists(file) ? Files.size(file) : 0;
            CachedAreas cached = DIMENSIONS.get(filename);
            if (cached == null || !cached.modified.equals(modified) || cached.size != size) {
                AreaGamemodeResolver next;
                try {
                    List<AreaData> areas = Files.exists(file) ? JsonHelper.fromJson(Files.readString(file)) : List.of();
                    if (areas == null) {
                        throw new IllegalArgumentException("域名文件根节点必须是数组");
                    }
                    next = new AreaGamemodeResolver(areas);
                } catch (RuntimeException invalid) {
                    // 损坏文件只报告一次并继续使用上一份快照，直到磁盘内容发生变化再尝试读取。
                    Areashint.LOGGER.warn("域名游戏模式文件内容不合法，保留已有快照: {}", file, invalid);
                    next = cached == null ? new AreaGamemodeResolver(List.of()) : cached.resolver;
                }
                cached = new CachedAreas(modified, size, next);
                DIMENSIONS.put(filename, cached);
            }
            return cached.resolver;
        } catch (java.io.IOException e) {
            Areashint.LOGGER.warn("读取域名游戏模式文件失败: {}", file, e);
            // 暂时无法读取文件时保留最后一份服务端快照，避免已知域名意外回退默认模式。
            CachedAreas cached = DIMENSIONS.get(filename);
            return cached == null ? null : cached.resolver;
        }
    }

    private record CachedAreas(FileTime modified, long size, AreaGamemodeResolver resolver) { }
    private record PlayerState(String dimension, GameMode mode) { }
}
