package areahint.command;

/**
 * 指令根名称常量。
 * <p>
 * {@link #SERVER_ROOT} 是模组的主指令，全部子命令由服务端注册；
 * {@link #CLIENT_ROOT} 专门承载纯客户端子命令（音效选择、音量设置）。
 * 两者必须分开：Fabric 会把客户端注册的整条根指令标记为“客户端命令”，
 * 一旦客户端子命令挂在主指令下，主指令的全部服务端子命令都会在客户端被拦截而无法执行。
 */
public final class AreasHintCommandRoot {
    /** 主指令根，服务端命令树。 */
    public static final String SERVER_ROOT = "areahint";

    /** 纯客户端子命令根，不参与服务端命令树。 */
    public static final String CLIENT_ROOT = "areahintc";

    private AreasHintCommandRoot() {
    }
}
