package areahint.replacebutton;

import areahint.network.Channel;
import areahint.network.ClientNetworkCompat;
import areahint.network.NetworkBuf;

/**
 * ReplaceButton网络处理
 * 处理客户端接收服务器的replacebutton相关消息
 */
public class ReplaceButtonNetworking {

    /**
     * 注册客户端接收器
     */
    public static void registerClientReceivers() {
        // 接收开始按键更换的消息
        ClientNetworkCompat.get().registerClientReceiver(Channel.REPLACEBUTTON_START, (client, buf) -> {
            client.execute(() -> {
                ReplaceButtonManager.getInstance().startReplaceButton();
            });
        });

        // 接收取消按键更换的消息
        ClientNetworkCompat.get().registerClientReceiver(Channel.REPLACEBUTTON_CANCEL, (client, buf) -> {
            client.execute(() -> {
                ReplaceButtonManager.getInstance().cancel();
            });
        });

        // 接收确认按键更换的消息
        ClientNetworkCompat.get().registerClientReceiver(Channel.REPLACEBUTTON_CONFIRM, (client, buf) -> {
            client.execute(() -> {
                ReplaceButtonManager.getInstance().confirmNewKey();
            });
        });
    }

    /**
     * 发送开始按键更换请求到服务器
     */
    public static void sendStartRequest() {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        ClientNetworkCompat.get().sendToServer(Channel.REPLACEBUTTON_START, buf);
    }

    /**
     * 发送取消按键更换请求到服务器
     */
    public static void sendCancelRequest() {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        ClientNetworkCompat.get().sendToServer(Channel.REPLACEBUTTON_CANCEL, buf);
    }

    /**
     * 发送确认按键更换请求到服务器
     */
    public static void sendConfirmRequest() {
        NetworkBuf buf = ClientNetworkCompat.get().newBuffer();
        ClientNetworkCompat.get().sendToServer(Channel.REPLACEBUTTON_CONFIRM, buf);
    }
}
