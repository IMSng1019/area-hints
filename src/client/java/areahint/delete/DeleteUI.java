package areahint.delete;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ClientAreaText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Delete用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class DeleteUI {

    /**
     * 显示域名选择界面
     */
    public static void showAreaSelectionScreen(List<AreaData> deletableAreas) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.area.delete")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.prompt.area.delete")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.delete.permission")), false);
        client.player.sendMessage(Text.of(""), false);

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

            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/areahint delete select \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(hoverText)))
                    .withColor(Formatting.GOLD));

            client.player.sendMessage(areaButton, false);
        }

        client.player.sendMessage(Text.of(""), false);

        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint delete cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("gui.message.cancel.delete_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示确认删除界面（二级确认）
     */
    public static void showConfirmDeleteScreen(AreaData area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String displayName = areahint.util.AreaDataConverter.getDisplayName(area);

        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.title.confirm.delete")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.error.area.confirm.delete")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.error.general_2")), false);
        client.player.sendMessage(Text.of(""), false);

        // 显示域名详细信息
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.name_2") + area.getName()), false);

        if (area.getSurfacename() != null && !area.getSurfacename().trim().isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.surface_2") + area.getSurfacename()), false);
        }

        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.level") + area.getLevel()), false);

        if (area.getBaseName() != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.parent_2") + area.getBaseName()), false);
        } else {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.parent_3")), false);
        }

        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.vertex") + area.getVertices().size() + I18nManager.translate("gui.message.general")), false);

        if (area.getAltitude() != null) {
            String minAlt = area.getAltitude().getMin() != null ?
                String.valueOf(area.getAltitude().getMin()) : I18nManager.translate("command.message.general_10");
            String maxAlt = area.getAltitude().getMax() != null ?
                String.valueOf(area.getAltitude().getMax()) : I18nManager.translate("command.message.general_10");
            client.player.sendMessage(Text.of(I18nManager.translate("command.message.altitude_12") + minAlt + " ~ " + maxAlt), false);
        }

        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_6") + ClientAreaText.signature(area)), false);

        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.error.delete")), false);
        client.player.sendMessage(Text.of(""), false);

        // 显示确认和取消按钮
        MutableText confirmButton = Text.literal(I18nManager.translate("gui.error.confirm.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint delete confirm"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.of(I18nManager.translate("gui.error.area.confirm.delete_2") + displayName + I18nManager.translate("gui.message.general_3"))))
                .withColor(Formatting.RED)
                .withBold(true));

        MutableText cancelButton = Text.literal(I18nManager.translate("gui.button.cancel.delete"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint delete cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("gui.message.cancel.delete"))))
                .withColor(Formatting.GREEN)
                .withBold(true));

        MutableText buttonRow = Text.empty()
            .append(confirmButton)
            .append(Text.of("  "))
            .append(cancelButton);

        client.player.sendMessage(buttonRow, false);
    }
}
