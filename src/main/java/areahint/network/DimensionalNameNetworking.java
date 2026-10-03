package areahint.network;

import areahint.Areashint;
import areahint.data.DimensionalNameData;
import areahint.dimensional.DimensionalNameManager;
import com.google.gson.Gson;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 维度域名网络传输
 * 负责在服务端和客户端之间同步维度域名配置
 */
public class DimensionalNameNetworking {
    // 网络包标识符
    public static final String S2C_DIMENSIONAL_NAMES = "areashint:dimensional_names";
    
    private static final Gson GSON = new Gson();
    
    /**
     * 初始化网络处理
     */
    public static void init() {
        Areashint.LOGGER.info("维度域名网络处理初始化完成");
    }
    
    /**
     * 向所有在线客户端发送维度域名配置
     * @param server 服务器实例
     */
    public static void sendDimensionalNamesToAllClients(MinecraftServer server) {
        try {
            String jsonData = buildJsonData();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                sendDimensionalNamesToClient(player, jsonData);
            }
            Areashint.LOGGER.info("已向 {} 个在线客户端发送维度域名配置",
                server.getPlayerManager().getPlayerList().size());
        } catch (Exception e) {
            Areashint.LOGGER.error("发送维度域名配置到客户端失败", e);
        }
    }
    
    /**
     * 向指定客户端发送维度域名配置
     * @param player 目标玩家
     */
    public static void sendDimensionalNamesToClient(ServerPlayerEntity player) {
        try {
            sendDimensionalNamesToClient(player, buildJsonData());
        } catch (Exception e) {
            Areashint.LOGGER.error("发送维度域名配置到客户端失败: " + player.getName().getString(), e);
        }
    }

    private static String buildJsonData() {
        Map<String, String> names = DimensionalNameManager.getAllDimensionalNames();
        Map<String, String> colors = DimensionalNameManager.getAllDimensionalColors();
        Map<String, String> signatures = DimensionalNameManager.getAllDimensionalSignatures();
        List<DimensionalNameData> dataList = new ArrayList<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            dataList.add(new DimensionalNameData(entry.getKey(), entry.getValue(),
                colors.get(entry.getKey()),
                signatures.get(entry.getKey())));
        }
        return GSON.toJson(dataList);
    }
    
    /**
     * 向指定客户端发送维度域名配置（内部方法）
     * @param player 目标玩家
     * @param jsonData JSON数据
     */
    private static void sendDimensionalNamesToClient(ServerPlayerEntity player, String jsonData) {
        try {
            NetworkBuf buffer = NetworkCompat.get().newBuffer();
            buffer.writeString(jsonData);
            NetworkCompat.get().sendToPlayer(player, Channel.DIMENSIONAL_NAMES, buffer);
                
            Areashint.LOGGER.debug("已向客户端 {} 发送维度域名配置", player.getName().getString());
            
        } catch (Exception e) {
            Areashint.LOGGER.error("发送维度域名配置到客户端失败: " + player.getName().getString(), e);
        }
    }
    
    /**
     * 获取网络包标识符（供Packets类使用）
     * @return 网络包标识符
     */
    public static String getPacketId() {
        return S2C_DIMENSIONAL_NAMES;
    }
} 
