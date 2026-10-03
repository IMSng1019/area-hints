package areahint.network;

/**
 * 网络通道标识：只保存通道名，具体变成哪个版本的 Identifier / ResourceLocation 由各版本的 NetworkCompat 实现决定。
 * 本文件刻意不引用任何 Minecraft 类型，因此不需要任何版本分支。
 */
public final class Channel {
    private final String path;

    private Channel(String path) {
        this.path = path;
    }

    /** 用通道名构造：通道名必须与协议里既有的一致，不许改 */
    public static Channel of(String path) {
        return new Channel(path);
    }

    /** 通道名（不含命名空间） */
    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Channel && path.equals(((Channel) other).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return "Channel[" + path + "]";
    }

    // ===== 既有通道清单：字符串值与 Packets 里 new Identifier(MOD_ID, "…") 的第二参数逐字一致 =====

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
}
