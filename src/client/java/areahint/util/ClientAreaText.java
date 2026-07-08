package areahint.util;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;

/**
 * 客户端域名文本格式化工具，统一处理可选字段的空值显示。
 */
public final class ClientAreaText {
    private ClientAreaText() {
    }

    public static String signature(AreaData area) {
        return signature(area == null ? null : area.getSignature());
    }

    public static String signature(String signature) {
        return optional(signature);
    }

    public static String areaDetail(AreaData area) {
        if (area == null) {
            return I18nManager.translate("commandui.common.none");
        }
        return I18nManager.translate("commandui.common.area.detail",
            area.getName(), area.getLevel(), area.getColor(), signature(area));
    }

    private static String optional(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("commandui.common.none") : value;
    }
}
