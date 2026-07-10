package areahint.permission;

/**
 * Areas Hint 权限节点常量。
 */
public final class PermissionNodes {
    public static final String HELP = "areahint.command.help";
    public static final String CHECK = "areahint.command.check";
    public static final String RELOAD = "areahint.command.reload";
    public static final String DEBUG = "areahint.command.debug";
    // 玩家个人客户端开关也允许 LuckPerms 显式拒绝，未定义节点时仍按权限等级0开放。
    public static final String ON = "areahint.command.on";
    public static final String OFF = "areahint.command.off";
    public static final String DIMENSIONALITY_NAME = "areahint.command.dimensionalityname";
    public static final String DIMENSIONALITY_COLOR = "areahint.command.dimensionalitycolor";
    // 首次维度命名会写入服务端数据，指令树和网络包必须共同使用以下节点。
    public static final String FIRST_DIM_NAME = "areahint.command.firstdimname";
    public static final String FIRST_DIM_NAME_SKIP = "areahint.command.firstdimname_skip";
    public static final String ADD = "areahint.command.add";
    public static final String DELETE = "areahint.command.delete";
    public static final String EASYADD = "areahint.command.easyadd";
    public static final String EXPANDAREA = "areahint.command.expandarea";
    public static final String SHRINKAREA = "areahint.command.shrinkarea";
    public static final String DIVIDEAREA = "areahint.command.dividearea";
    public static final String RECOLOR = "areahint.command.recolor";
    public static final String RENAME = "areahint.command.rename";
    public static final String SETHIGH = "areahint.command.sethigh";
    public static final String TELEPORT = "areahint.command.teleport";
    public static final String SETTP = "areahint.command.settp";
    public static final String ADDHINT = "areahint.command.addhint";
    public static final String DELETEHINT = "areahint.command.deletehint";
    public static final String ADD_DESCRIPTION = "areahint.command.adddescription";
    public static final String DELETE_DESCRIPTION = "areahint.command.deletedescription";
    public static final String ADD_DIMENSIONALITY_DESCRIPTION = "areahint.command.adddimensionalitydescription";
    public static final String DELETE_DIMENSIONALITY_DESCRIPTION = "areahint.command.deletedimensionalitydescription";
    public static final String ADDSIGNATURE = "areahint.command.addsignature";
    public static final String DELETESIGNATURE = "areahint.command.deletesignature";
    public static final String FREQUENCY = "areahint.command.frequency";
    public static final String HINT_RENDER = "areahint.command.hintrender";
    public static final String TITLE_STYLE = "areahint.command.titlestyle";
    public static final String TITLE_SIZE = "areahint.command.titlesize";
    public static final String ADD_SUBTITLE = "areahint.command.addsubtitle";
    public static final String DELETE_SUBTITLE = "areahint.command.deletesubtitle";
    public static final String REPLACE_SUBTITLE_COLOR = "areahint.command.replacesubtitlecolor";
    public static final String REPLACE_SUBTITLE_SIZE = "areahint.command.replacesubtitlesize";
    // 以下节点只限制个人客户端设置指令入口，不影响客户端正常配置同步。
    public static final String REPLACE_BUTTON = "areahint.command.replacebutton";
    public static final String LANGUAGE = "areahint.command.language";
    public static final String BOUND_VIZ = "areahint.command.boundviz";
    public static final String SERVER_LANGUAGE = "areahint.command.serverlanguage";

    private PermissionNodes() {
    }
}
