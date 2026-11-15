package areahint.rename;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Rename用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class RenameUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectScreen(List<AreaData> areas) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.rename")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.area.rename")));

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String signature = area.getSignature() != null ? area.getSignature() : I18nManager.translate("gui.message.general_16");

            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint rename select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("gui.message.rename") + signature)))
                    .withColor(ChatFormatting.GOLD));

            client.player.sendSystemMessage(areaButton);
        }

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示新域名名称输入界面
     */
    public static void showNewNameInputScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.area.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.name")));

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示联合域名输入界面
     */
    public static void showSurfaceNameInputScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.title.area.surface")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.area.surface")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.area.surface.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.hint.area.surface")));

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String oldName, String newName, String newSurfaceName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.confirm.rename")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.confirm.rename")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area_2") + oldName));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area_3") + newName));

        if (newSurfaceName != null && !newSurfaceName.trim().isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.surface") + newSurfaceName));
        } else {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.surface.name")));
        }

        // 显示确认和取消按钮
        MutableComponent confirmButton = Component.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename confirm"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.confirm.rename"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.rename"))))
                .withColor(ChatFormatting.RED));

        MutableComponent buttonRow = Component.empty()
            .append(confirmButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.confirm_2")));
    }

    /**
     * 显示错误消息
     */
    public static void showError(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.error.general_2") + message));
        }
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty("§a" + message));
        }
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.nullToEmpty("§7" + message));
        }
    }
}
