package areahint.network;

import areahint.AreashintClient;
import areahint.world.ClientWorldFolderManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * 客户端世界网络处理
 * 负责请求和接收服务端的世界信息
 */
public class ClientWorldNetworking {
    
    // 网络包标识符
    public static final String C2S_REQUEST_WORLD_INFO = "areashint:request_world_info";
    public static final String S2C_WORLD_INFO = "areashint:world_info";
    
    /**
     * 初始化客户端世界网络处理
     */
    public static void init() {
        // 注册世界信息接收处理器
        ClientPlayNetworking.registerGlobalReceiver(
            new Identifier(S2C_WORLD_INFO),
            ClientWorldNetworking::handleWorldInfo
        );
        
        AreashintClient.LOGGER.info("客户端世界网络处理初始化完成");
    }
    
    /**
     * 向服务端请求世界信息
     */
    public static void requestWorldInfo() {
        try {
            if (ClientPlayNetworking.canSend(new Identifier(C2S_REQUEST_WORLD_INFO))) {
                PacketByteBuf buf = PacketByteBufs.create();
                // 请求包不需要任何数据
                ClientPlayNetworking.send(new Identifier(C2S_REQUEST_WORLD_INFO), buf);
                
                AreashintClient.LOGGER.info("已向服务端请求世界信息");
            } else {
                AreashintClient.LOGGER.warn("无法发送世界信息请求包，服务端可能不支持");
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("请求世界信息时发生错误", e);
        }
    }
    
    /**
     * 处理接收到的世界信息
     * @param client Minecraft客户端实例
     * @param handler 网络处理器
     * @param buf 数据包缓冲区
     * @param responseSender 响应发送器
     */
    private static void handleWorldInfo(MinecraftClient client,
                                       ClientPlayNetworkHandler handler,
                                       PacketByteBuf buf,
                                       PacketSender responseSender) {
        try {
            String worldName = buf.readString();
            
            // 在主线程中处理世界信息
            client.execute(() -> {
                try {
                    if (client.getNetworkHandler() != handler) {
                        AreashintClient.LOGGER.debug("已忽略失效连接发送的世界信息: {}", worldName);
                        return;
                    }
                    AreashintClient.LOGGER.info("收到服务端世界信息: '{}'", worldName);
                    AreashintClient.LOGGER.info("开始完成世界文件夹初始化...");
                    
                    // 完成世界文件夹初始化
                    ClientWorldFolderManager.finalizeWorldInitialization(worldName);

                    // 正式目录确定后先回放登录阶段暂存的域名数据，避免检测器读取空目录
                    ClientNetworking.flushPendingAreaData(client, handler);
                    
                    // 重新加载当前区域数据
                    reloadCurrentAreaData();
                    
                } catch (Exception e) {
                    AreashintClient.LOGGER.error("处理世界信息时发生错误", e);
                }
            });
            
        } catch (Exception e) {
            AreashintClient.LOGGER.error("解析世界信息包时发生错误", e);
        }
    }
    
    /**
     * 重新加载当前区域数据
     * 在世界文件夹初始化完成后调用
     */
    private static void reloadCurrentAreaData() {
        try {
            // 获取当前客户端实例
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null) {
                // 强制重新加载区域数据
                String dimensionId = client.world.getDimensionKey().getValue().toString();
                String dimensionFileName = getDimensionFileName(dimensionId);
                
                AreashintClient.LOGGER.info("网络同步完成，重新加载区域数据: {}", dimensionFileName);
                
                // 调用AreashintClient的重新检测方法
                AreashintClient.forceRedetectCurrentArea();
                
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("重新加载区域数据时发生错误", e);
        }
    }
    
    /**
     * 根据维度ID获取文件名
     * @param dimensionId 维度ID
     * @return 文件名
     */
    private static String getDimensionFileName(String dimensionId) {
        if (dimensionId.contains("overworld")) {
            return areahint.Areashint.OVERWORLD_FILE;
        } else if (dimensionId.contains("nether")) {
            return areahint.Areashint.NETHER_FILE;
        } else if (dimensionId.contains("end")) {
            return areahint.Areashint.END_FILE;
        }
        return areahint.Areashint.OVERWORLD_FILE; // 默认返回主世界
    }
}
