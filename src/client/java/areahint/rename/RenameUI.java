package areahint.rename;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import areahint.i18n.I18nManager;

import java.util.List;

/**
 * Rename用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class RenameUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectScreen(List<AreaData> areas) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.rename")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.area.rename")), false);

        for (AreaData area : areas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            String signature = area.getSignature() != null ? area.getSignature() : I18nManager.translate("gui.message.general_16");

            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint rename select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Text.of(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("gui.message.rename") + signature)))
                    .withColor(Formatting.GOLD));

            client.player.sendMessage(areaButton, false);
        }

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示新域名名称输入界面
     */
    public static void showNewNameInputScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.area.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.name")), false);

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示联合域名输入界面
     */
    public static void showSurfaceNameInputScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.title.area.surface")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.area.surface")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.area.surface.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.hint.area.surface")), false);

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel_3"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示确认界面
     */
    public static void showConfirmScreen(String oldName, String newName, String newSurfaceName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.confirm.rename")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.confirm.rename")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area_2") + oldName), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area_3") + newName), false);

        if (newSurfaceName != null && !newSurfaceName.trim().isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.surface") + newSurfaceName), false);
        } else {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.surface.name")), false);
        }

        // 显示确认和取消按钮
        MutableText confirmButton = Text.literal(I18nManager.translate("gui.button.general_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename confirm"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.confirm.rename"))))
                .withColor(Formatting.GREEN));

        MutableText cancelButton = Text.literal(I18nManager.translate("gui.error.general"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint rename cancel"))
                .withHoverEvent(TextEventCompat.showText(Text.of(I18nManager.translate("gui.message.cancel.rename"))))
                .withColor(Formatting.RED));

        MutableText buttonRow = Text.empty()
            .append(confirmButton)
            .append(Text.of("  "))
            .append(cancelButton);

        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.confirm_2")), false);
    }

    /**
     * 显示错误消息
     */
    public static void showError(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.error.general_2") + message), false);
        }
    }

    /**
     * 显示成功消息
     */
    public static void showSuccess(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of("§a" + message), false);
        }
    }

    /**
     * 显示信息消息
     */
    public static void showInfo(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.of("§7" + message), false);
        }
    }
}
