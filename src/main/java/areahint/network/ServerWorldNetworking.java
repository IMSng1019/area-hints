package areahint.network;

import areahint.Areashint;
import areahint.world.WorldFolderManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 服务端世界网络处理
 * 负责响应客户端的世界信息请求
 */
public class ServerWorldNetworking {
    
    // 网络包标识符
    public static final String C2S_REQUEST_WORLD_INFO = "areashint:request_world_info";
    public static final String S2C_WORLD_INFO = "areashint:world_info";
    
    /**
     * 初始化服务端世界网络处理
     */
    public static void init() {
        // 注册世界信息请求处理器
        NetworkCompat.get().registerServerReceiver(Channel.C2S_REQUEST_WORLD_INFO,
            ServerWorldNetworking::handleWorldInfoRequest);
        
        Areashint.LOGGER.info("服务端世界网络处理初始化完成");
    }
    
    /**
     * 处理客户端的世界信息请求
     */
    private static void handleWorldInfoRequest(net.minecraft.server.MinecraftServer server,
                                             ServerPlayerEntity player,
                                             NetworkReader reader) {
        try {
            // 在服务器主线程中处理
            server.execute(() -> {
                try {
                    String worldName = WorldFolderManager.getCurrentWorldName();
                    Areashint.LOGGER.info("从WorldFolderManager获取的世界名称: '{}'", worldName);
                    
                    if (worldName == null) {
                        worldName = "Server"; // 备用名称
                        Areashint.LOGGER.warn("世界名称为null，使用备用名称: '{}'", worldName);
                    }
                    
                    sendWorldInfoToClient(player, worldName);
                    
                    Areashint.LOGGER.info("已向玩家 {} 发送世界信息: '{}'", 
                        player.getName().getString(), worldName);
                    
                } catch (Exception e) {
                    Areashint.LOGGER.error("处理世界信息请求时发生错误", e);
                }
            });
            
        } catch (Exception e) {
            Areashint.LOGGER.error("解析世界信息请求时发生错误", e);
        }
    }
    
    /**
     * 向客户端发送世界信息
     * @param player 目标玩家
     * @param worldName 世界名称
     */
    public static void sendWorldInfoToClient(ServerPlayerEntity player, String worldName) {
        try {
            NetworkBuf buf = NetworkCompat.get().newBuffer();
            buf.writeString(worldName);
            
            NetworkCompat.get().sendToPlayer(player, Channel.S2C_WORLD_INFO, buf);
            
            Areashint.LOGGER.debug("世界信息发送给玩家: {} -> {}", player.getName().getString(), worldName);
            
        } catch (Exception e) {
            Areashint.LOGGER.error("发送世界信息到客户端时发生错误", e);
        }
    }
    
    /**
     * 向所有在线玩家广播世界信息
     * @param server 服务器实例
     * @param worldName 世界名称
     */
    public static void broadcastWorldInfoToAllClients(net.minecraft.server.MinecraftServer server, String worldName) {
        try {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                sendWorldInfoToClient(player, worldName);
            }
            
            Areashint.LOGGER.info("已向所有在线玩家广播世界信息: {}", worldName);
            
        } catch (Exception e) {
            Areashint.LOGGER.error("广播世界信息时发生错误", e);
        }
    }
} 