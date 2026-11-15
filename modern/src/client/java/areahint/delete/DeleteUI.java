package areahint.delete;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ClientAreaText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Delete用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class DeleteUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectionScreen(List<AreaData> deletableAreas) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.area.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.prompt.area.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.delete.permission")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 为每个可删除的域名创建按钮
        for (AreaData area : deletableAreas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);

            // 构建悬停提示信息
            String hoverText = I18nManager.translate("gui.message.area") +
                I18nManager.translate("gui.message.name_2") + area.getName() + "\n" +
                I18nManager.translate("gui.message.level") + area.getLevel() + "\n" +
                I18nManager.translate("gui.message.general_7") + ClientAreaText.signature(area);

            if (area.getBaseName() != null) {
                hoverText += I18nManager.translate("gui.message.area.parent") + area.getBaseName();
            }

            hoverText += I18nManager.translate("gui.prompt.area");

            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint delete select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(hoverText)))
                    .withColor(ChatFormatting.GOLD));

            client.player.sendSystemMessage(areaButton);
        }

        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint delete cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.delete_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示确认删除界面（二级确认）
     */
    public static void showConfirmDeleteScreen(AreaData area) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String displayName = areahint.util.AreaDataConverter.getDisplayName(area);

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.title.confirm.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.error.area.confirm.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.error.general_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 显示域名详细信息
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.name_2") + area.getName()));

        if (area.getSurfacename() != null && !area.getSurfacename().trim().isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.surface_2") + area.getSurfacename()));
        }

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.level") + area.getLevel()));

        if (area.getBaseName() != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.parent_2") + area.getBaseName()));
        } else {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.parent_3")));
        }

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.vertex") + area.getVertices().size() + I18nManager.translate("gui.message.general")));

        if (area.getAltitude() != null) {
            String minAlt = area.getAltitude().getMin() != null ?
                String.valueOf(area.getAltitude().getMin()) : I18nManager.translate("command.message.general_10");
            String maxAlt = area.getAltitude().getMax() != null ?
                String.valueOf(area.getAltitude().getMax()) : I18nManager.translate("command.message.general_10");
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.message.altitude_12") + minAlt + " ~ " + maxAlt));
        }

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_6") + ClientAreaText.signature(area)));

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.error.delete")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        // 显示确认和取消按钮
        MutableComponent confirmButton = Component.literal(I18nManager.translate("gui.error.confirm.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint delete confirm"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("gui.error.area.confirm.delete_2") + displayName + I18nManager.translate("gui.message.general_3"))))
                .withColor(ChatFormatting.RED)
                .withBold(true));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("gui.button.cancel.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint delete cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("gui.message.cancel.delete"))))
                .withColor(ChatFormatting.GREEN)
                .withBold(true));

        MutableComponent buttonRow = Component.empty()
            .append(confirmButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);

        client.player.sendSystemMessage(buttonRow);
    }
}
