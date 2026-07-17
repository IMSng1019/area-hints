package areahint.network;

import areahint.Areashint;
import net.minecraft.util.Identifier;

/**
 * 网络数据包定义类
 * 定义客户端和服务端之间通信的数据包
 */
public class Packets {
    // 区域数据传输通道标识符
    public static final Identifier AREA_DATA_CHANNEL = new Identifier(Areashint.MOD_ID, "area_data");
    
    // 客户端命令通道标识符
    public static final Identifier CLIENT_COMMAND_CHANNEL = new Identifier(Areashint.MOD_ID, "client_command");
    
    // 维度类型常量
    public static final String DIMENSION_OVERWORLD = "overworld";
    public static final String DIMENSION_NETHER = "the_nether";
    public static final String DIMENSION_END = "the_end";
    
    // 服务端到客户端的包ID
    public static final String S2C_AREA_DATA = Areashint.MOD_ID + ":s2c_area_data";
    public static final String S2C_CLIENT_COMMAND = Areashint.MOD_ID + ":s2c_client_command";
    public static final String S2C_DEBUG_COMMAND = Areashint.MOD_ID + ":s2c_debug_command";
    
    // Recolor功能相关的包ID
    public static final Identifier C2S_RECOLOR_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_recolor_request");
    public static final Identifier S2C_RECOLOR_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_recolor_response");

    // Subtitle功能相关的包ID
    public static final Identifier C2S_SUBTITLE_MUTATION = new Identifier(Areashint.MOD_ID, "c2s_subtitle_mutation");
    public static final Identifier S2C_SUBTITLE_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_subtitle_response");
    
    // Rename功能相关的包ID
    public static final Identifier C2S_RENAME_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_rename_request");
    public static final Identifier S2C_RENAME_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_rename_response");
    
    // 客户端到服务端的包ID
    public static final String C2S_REQUEST_AREA_DATA = Areashint.MOD_ID + ":c2s_request_area_data";
    
    // EasyAdd功能相关的包ID
    public static final Identifier C2S_EASYADD_AREA_DATA = new Identifier(Areashint.MOD_ID, "c2s_easyadd_area_data");
    public static final Identifier S2C_EASYADD_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_easyadd_response");
    
    // SetHigh功能相关的包ID
    public static final Identifier C2S_SETHIGH_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_sethigh_request");
    public static final Identifier S2C_SETHIGH_AREA_LIST = new Identifier(Areashint.MOD_ID, "s2c_sethigh_area_list");
    public static final Identifier S2C_SETHIGH_AREA_SELECTION = new Identifier(Areashint.MOD_ID, "s2c_sethigh_area_selection");
    public static final Identifier S2C_SETHIGH_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_sethigh_response");

    // Teleport功能相关的包ID
    public static final Identifier C2S_TELEPORT_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_teleport_request");
    public static final Identifier S2C_TELEPORT_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_teleport_response");

    // ExpandArea功能相关的包ID
    public static final Identifier EXPAND_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "expand_area");
    public static final Identifier EXPAND_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "expand_area_response");
    
    // ShrinkArea功能相关的包ID
    public static final Identifier SHRINK_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "shrink_area");
    public static final Identifier SHRINK_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "shrink_area_response");

    // 日志功能相关的包ID
    public static final Identifier C2S_AREA_LOG = new Identifier(Areashint.MOD_ID, "c2s_area_log");

    // Delete功能相关的包ID
    public static final Identifier C2S_REQUEST_DELETABLE_AREAS = new Identifier(Areashint.MOD_ID, "c2s_request_deletable_areas");
    public static final Identifier S2C_DELETABLE_AREAS_LIST = new Identifier(Areashint.MOD_ID, "s2c_deletable_areas_list");
    public static final Identifier C2S_DELETE_AREA = new Identifier(Areashint.MOD_ID, "c2s_delete_area");
    public static final Identifier S2C_DELETE_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_delete_response");

    // AddHint功能相关的包ID
    public static final Identifier ADDHINT_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "addhint_area");
    public static final Identifier ADDHINT_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "addhint_area_response");

    // DeleteHint功能相关的包ID
    public static final Identifier DELETEHINT_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "deletehint_area");
    public static final Identifier DELETEHINT_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "deletehint_area_response");

    // Signature功能相关的包ID
    public static final Identifier SIGNATURE_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "signature_area");
    public static final Identifier SIGNATURE_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "signature_area_response");

    // DivideArea功能相关的包ID
    public static final Identifier DIVIDE_AREA_CHANNEL = new Identifier(Areashint.MOD_ID, "divide_area");
    public static final Identifier DIVIDE_AREA_RESPONSE_CHANNEL = new Identifier(Areashint.MOD_ID, "divide_area_response");

    // DimensionalName功能相关的包ID
    public static final Identifier C2S_DIMNAME_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_dimname_request");
    public static final Identifier C2S_DIMCOLOR_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_dimcolor_request");

    // 首次维度命名请求，服务端接收器会使用对应 LuckPerms 节点再次校验。
    public static final Identifier C2S_FIRST_DIMNAME = new Identifier(Areashint.MOD_ID, "c2s_first_dimname");

    // ReplaceButton功能相关的包ID
    public static final Identifier REPLACEBUTTON_START = new Identifier(Areashint.MOD_ID, "replacebutton_start");
    public static final Identifier REPLACEBUTTON_CANCEL = new Identifier(Areashint.MOD_ID, "replacebutton_cancel");
    public static final Identifier REPLACEBUTTON_CONFIRM = new Identifier(Areashint.MOD_ID, "replacebutton_confirm");

    // 语言同步包ID
    public static final Identifier C2S_LANGUAGE_SYNC = new Identifier(Areashint.MOD_ID, "c2s_language_sync");

    // Xaero 世界地图域名管理能力查询
    public static final Identifier C2S_AREA_MANAGEMENT_CAPABILITIES = new Identifier(Areashint.MOD_ID, "c2s_area_management_capabilities");
    public static final Identifier S2C_AREA_MANAGEMENT_CAPABILITIES = new Identifier(Areashint.MOD_ID, "s2c_area_management_capabilities");

    // Description功能相关的包ID
    public static final Identifier C2S_DESCRIPTION_QUERY = new Identifier(Areashint.MOD_ID, "c2s_description_query");
    public static final Identifier C2S_DESCRIPTION_CURRENT_QUERY = new Identifier(Areashint.MOD_ID, "c2s_description_current_query");
    public static final Identifier S2C_DESCRIPTION_QUERY_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_description_query_response");
    public static final Identifier C2S_DESCRIPTION_LIST_REQUEST = new Identifier(Areashint.MOD_ID, "c2s_description_list_request");
    public static final Identifier S2C_DESCRIPTION_AREA_LIST = new Identifier(Areashint.MOD_ID, "s2c_description_area_list");
    public static final Identifier C2S_DESCRIPTION_EDIT_QUERY = new Identifier(Areashint.MOD_ID, "c2s_description_edit_query");
    public static final Identifier S2C_DESCRIPTION_EDIT_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_description_edit_response");
    public static final Identifier C2S_DESCRIPTION_WRITE = new Identifier(Areashint.MOD_ID, "c2s_description_write");
    public static final Identifier C2S_DESCRIPTION_DELETE = new Identifier(Areashint.MOD_ID, "c2s_description_delete");
    public static final Identifier S2C_DESCRIPTION_MUTATION_RESPONSE = new Identifier(Areashint.MOD_ID, "s2c_description_mutation_response");

    /**
     * 将维度路径转换为维度类型字符串
     * @param dimensionPath 维度路径
     * @return 维度类型字符串，如果未知则返回null
     */
    public static String convertDimensionPathToType(String dimensionPath) {
        if (dimensionPath == null) {
            return null;
        }
        
        switch (dimensionPath) {
            case "overworld":
                return DIMENSION_OVERWORLD;
            case "the_nether":
                return DIMENSION_NETHER;
            case "the_end":
                return DIMENSION_END;
            default:
                return null;
        }
    }
    
    /**
     * 获取维度对应的文件名
     * @param dimensionType 维度类型
     * @return 文件名，如果未知则返回null
     */
    public static String getFileNameForDimension(String dimensionType) {
        if (dimensionType == null) {
            return null;
        }

        switch (dimensionType) {
            case DIMENSION_OVERWORLD:
                return Areashint.OVERWORLD_FILE;
            case DIMENSION_NETHER:
                return Areashint.NETHER_FILE;
            case DIMENSION_END:
                return Areashint.END_FILE;
            default:
                return null;
        }
    }

    public static String getDatabaseFolderForDimension(String dimensionType) {
        if (dimensionType == null) {
            return null;
        }

        switch (dimensionType) {
            case DIMENSION_OVERWORLD:
                return DIMENSION_OVERWORLD;
            case DIMENSION_NETHER:
                return DIMENSION_NETHER;
            case DIMENSION_END:
                return DIMENSION_END;
            default:
                return null;
        }
    }
}
