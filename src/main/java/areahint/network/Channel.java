package areahint.network;

/**
 * 网络通道标识：只保存通道名，具体变成哪个版本的 Identifier / ResourceLocation 由各版本的 NetworkCompat 实现决定。
 * 本文件刻意不引用任何 Minecraft 类型，因此不需要任何版本分支。
 */
public final class Channel {
    /** 命名空间；null 表示用模组自身的命名空间（Areashint.MOD_ID） */
    private final String namespace;
    private final String path;

    private Channel(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    /** 用通道名构造：命名空间取模组自身；通道名必须与协议里既有的一致，不许改 */
    public static Channel of(String path) {
        return new Channel(null, path);
    }

    /** 用「命名空间 + 通道名」构造：给历史上确实用了别的命名空间的通道留口子 */
    public static Channel of(String namespace, String path) {
        return new Channel(namespace, path);
    }

    /** 命名空间；null 表示用模组自身命名空间 */
    public String namespace() {
        return namespace;
    }

    /** 通道名（不含命名空间） */
    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Channel)) {
            return false;
        }
        Channel that = (Channel) other;
        return java.util.Objects.equals(namespace, that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return "Channel[" + (namespace == null ? "<mod>" : namespace) + ":" + path + "]";
    }

    // ===== 既有通道清单：字符串值与 Packets 里 new Identifier(MOD_ID, "…") 的第二参数逐字一致 =====

    // 以下四个在旧代码里是 "areas-hint:xxx" 形式的字符串常量，这里只取路径段
    public static final Channel S2C_AREA_DATA = of("s2c_area_data");
    public static final Channel S2C_CLIENT_COMMAND = of("s2c_client_command");
    public static final Channel S2C_DEBUG_COMMAND = of("s2c_debug_command");
    public static final Channel C2S_REQUEST_AREA_DATA = of("c2s_request_area_data");

    // 以下三个通道历史上用的是 "areashint:" 命名空间（注意：不是 MOD_ID 的 "areas-hint"），必须原样保留
    public static final Channel DIMENSIONAL_NAMES = of("areashint", "dimensional_names");
    public static final Channel C2S_REQUEST_WORLD_INFO = of("areashint", "request_world_info");
    public static final Channel S2C_WORLD_INFO = of("areashint", "world_info");

    public static final Channel AREA_DATA_CHANNEL = of("area_data");
    public static final Channel CLIENT_COMMAND_CHANNEL = of("client_command");
    public static final Channel C2S_RECOLOR_REQUEST = of("c2s_recolor_request");
    public static final Channel S2C_RECOLOR_RESPONSE = of("s2c_recolor_response");
    public static final Channel C2S_SUBTITLE_MUTATION = of("c2s_subtitle_mutation");
    public static final Channel S2C_SUBTITLE_RESPONSE = of("s2c_subtitle_response");
    public static final Channel C2S_RENAME_REQUEST = of("c2s_rename_request");
    public static final Channel S2C_RENAME_RESPONSE = of("s2c_rename_response");
    public static final Channel C2S_EASYADD_AREA_DATA = of("c2s_easyadd_area_data");
    public static final Channel S2C_EASYADD_RESPONSE = of("s2c_easyadd_response");
    public static final Channel C2S_SETHIGH_REQUEST = of("c2s_sethigh_request");
    public static final Channel S2C_SETHIGH_AREA_LIST = of("s2c_sethigh_area_list");
    public static final Channel S2C_SETHIGH_AREA_SELECTION = of("s2c_sethigh_area_selection");
    public static final Channel S2C_SETHIGH_RESPONSE = of("s2c_sethigh_response");
    public static final Channel C2S_TELEPORT_REQUEST = of("c2s_teleport_request");
    public static final Channel S2C_TELEPORT_RESPONSE = of("s2c_teleport_response");
    public static final Channel EXPAND_AREA_CHANNEL = of("expand_area");
    public static final Channel EXPAND_AREA_RESPONSE_CHANNEL = of("expand_area_response");
    public static final Channel SHRINK_AREA_CHANNEL = of("shrink_area");
    public static final Channel SHRINK_AREA_RESPONSE_CHANNEL = of("shrink_area_response");
    public static final Channel C2S_AREA_LOG = of("c2s_area_log");
    public static final Channel C2S_REQUEST_DELETABLE_AREAS = of("c2s_request_deletable_areas");
    public static final Channel S2C_DELETABLE_AREAS_LIST = of("s2c_deletable_areas_list");
    public static final Channel C2S_DELETE_AREA = of("c2s_delete_area");
    public static final Channel S2C_DELETE_RESPONSE = of("s2c_delete_response");
    public static final Channel ADDHINT_AREA_CHANNEL = of("addhint_area");
    public static final Channel ADDHINT_AREA_RESPONSE_CHANNEL = of("addhint_area_response");
    public static final Channel DELETEHINT_AREA_CHANNEL = of("deletehint_area");
    public static final Channel DELETEHINT_AREA_RESPONSE_CHANNEL = of("deletehint_area_response");
    public static final Channel SIGNATURE_AREA_CHANNEL = of("signature_area");
    public static final Channel SIGNATURE_AREA_RESPONSE_CHANNEL = of("signature_area_response");
    public static final Channel DIVIDE_AREA_CHANNEL = of("divide_area");
    public static final Channel DIVIDE_AREA_RESPONSE_CHANNEL = of("divide_area_response");
    public static final Channel C2S_DIMNAME_REQUEST = of("c2s_dimname_request");
    public static final Channel C2S_DIMCOLOR_REQUEST = of("c2s_dimcolor_request");
    public static final Channel C2S_FIRST_DIMNAME = of("c2s_first_dimname");
    public static final Channel REPLACEBUTTON_START = of("replacebutton_start");
    public static final Channel REPLACEBUTTON_CANCEL = of("replacebutton_cancel");
    public static final Channel REPLACEBUTTON_CONFIRM = of("replacebutton_confirm");
    public static final Channel C2S_LANGUAGE_SYNC = of("c2s_language_sync");
    public static final Channel C2S_AREA_MANAGEMENT_CAPABILITIES = of("c2s_area_management_capabilities");
    public static final Channel S2C_AREA_MANAGEMENT_CAPABILITIES = of("s2c_area_management_capabilities");
    public static final Channel C2S_DESCRIPTION_QUERY = of("c2s_description_query");
    public static final Channel C2S_DESCRIPTION_CURRENT_QUERY = of("c2s_description_current_query");
    public static final Channel S2C_DESCRIPTION_QUERY_RESPONSE = of("s2c_description_query_response");
    public static final Channel C2S_DESCRIPTION_LIST_REQUEST = of("c2s_description_list_request");
    public static final Channel S2C_DESCRIPTION_AREA_LIST = of("s2c_description_area_list");
    public static final Channel C2S_DESCRIPTION_EDIT_QUERY = of("c2s_description_edit_query");
    public static final Channel S2C_DESCRIPTION_EDIT_RESPONSE = of("s2c_description_edit_response");
    public static final Channel C2S_DESCRIPTION_WRITE = of("c2s_description_write");
    public static final Channel C2S_DESCRIPTION_DELETE = of("c2s_description_delete");
    public static final Channel S2C_DESCRIPTION_MUTATION_RESPONSE = of("s2c_description_mutation_response");
    public static final Channel C2S_APPROVAL_ACTION = of("c2s_approval_action");
    public static final Channel C2S_APPROVAL_CHUNK = of("c2s_approval_chunk");
    public static final Channel S2C_APPROVAL_DATA = of("s2c_approval_data");
    public static final Channel S2C_APPROVAL_RESULT = of("s2c_approval_result");
    // 游戏模式策略只发给主动请求的新客户端，位置通知不携带客户端指定的域名或模式。
    public static final Channel C2S_AREA_GAMEMODE_POLICY = of("c2s_area_gamemode_policy");
    public static final Channel S2C_AREA_GAMEMODE_POLICY = of("s2c_area_gamemode_policy");
    public static final Channel C2S_AREA_GAMEMODE_CHECK = of("c2s_area_gamemode_check");
    public static final Channel S2C_AREA_GAMEMODE_EDITOR = of("s2c_area_gamemode_editor");
    public static final Channel S2C_URBAN_AREA_EDITOR = of("s2c_urban_area_editor");

    /** 全部通道清单：1.20.5+ 的网络实现按它逐个注册 PayloadType（顺序无关） */
    public static final java.util.List<Channel> ALL = java.util.List.of(
            C2S_AREA_GAMEMODE_POLICY, S2C_AREA_GAMEMODE_POLICY, C2S_AREA_GAMEMODE_CHECK, S2C_AREA_GAMEMODE_EDITOR,
            S2C_URBAN_AREA_EDITOR,
            S2C_AREA_DATA, S2C_CLIENT_COMMAND, S2C_DEBUG_COMMAND, C2S_REQUEST_AREA_DATA,
            DIMENSIONAL_NAMES, C2S_REQUEST_WORLD_INFO, S2C_WORLD_INFO,
            AREA_DATA_CHANNEL, CLIENT_COMMAND_CHANNEL, C2S_RECOLOR_REQUEST, S2C_RECOLOR_RESPONSE,
            C2S_SUBTITLE_MUTATION, S2C_SUBTITLE_RESPONSE, C2S_RENAME_REQUEST, S2C_RENAME_RESPONSE,
            C2S_EASYADD_AREA_DATA, S2C_EASYADD_RESPONSE, C2S_SETHIGH_REQUEST, S2C_SETHIGH_AREA_LIST,
            S2C_SETHIGH_AREA_SELECTION, S2C_SETHIGH_RESPONSE, C2S_TELEPORT_REQUEST, S2C_TELEPORT_RESPONSE,
            EXPAND_AREA_CHANNEL, EXPAND_AREA_RESPONSE_CHANNEL, SHRINK_AREA_CHANNEL, SHRINK_AREA_RESPONSE_CHANNEL,
            C2S_AREA_LOG, C2S_REQUEST_DELETABLE_AREAS, S2C_DELETABLE_AREAS_LIST, C2S_DELETE_AREA,
            S2C_DELETE_RESPONSE, ADDHINT_AREA_CHANNEL, ADDHINT_AREA_RESPONSE_CHANNEL, DELETEHINT_AREA_CHANNEL,
            DELETEHINT_AREA_RESPONSE_CHANNEL, SIGNATURE_AREA_CHANNEL, SIGNATURE_AREA_RESPONSE_CHANNEL,
            DIVIDE_AREA_CHANNEL, DIVIDE_AREA_RESPONSE_CHANNEL, C2S_DIMNAME_REQUEST, C2S_DIMCOLOR_REQUEST,
            C2S_FIRST_DIMNAME, REPLACEBUTTON_START, REPLACEBUTTON_CANCEL, REPLACEBUTTON_CONFIRM,
            C2S_LANGUAGE_SYNC, C2S_AREA_MANAGEMENT_CAPABILITIES, S2C_AREA_MANAGEMENT_CAPABILITIES,
            C2S_DESCRIPTION_QUERY, C2S_DESCRIPTION_CURRENT_QUERY, S2C_DESCRIPTION_QUERY_RESPONSE,
            C2S_DESCRIPTION_LIST_REQUEST, S2C_DESCRIPTION_AREA_LIST, C2S_DESCRIPTION_EDIT_QUERY,
            S2C_DESCRIPTION_EDIT_RESPONSE, C2S_DESCRIPTION_WRITE, C2S_DESCRIPTION_DELETE,
            S2C_DESCRIPTION_MUTATION_RESPONSE, C2S_APPROVAL_ACTION, C2S_APPROVAL_CHUNK, S2C_APPROVAL_DATA, S2C_APPROVAL_RESULT);
}
