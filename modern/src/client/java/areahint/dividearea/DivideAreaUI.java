package areahint.dividearea;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import java.util.List;

public class DivideAreaUI {
    private final DivideAreaManager manager;
    private final Minecraft client;

    public DivideAreaUI(DivideAreaManager manager) {
        this.manager = manager;
        this.client = Minecraft.getInstance();
    }

    public void showAreaSelection(List<AreaData> areas) {
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.title.area.divide")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.area.divide")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableComponent btn = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint dividearea select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("dividearea.message.divide_4") + displayName + I18nManager.translate("addhint.message.general") + ClientAreaText.signature(area))))
                    .withColor(ChatFormatting.GOLD));
            client.player.sendSystemMessage(btn);
        }

        client.player.sendSystemMessage(Component.nullToEmpty(""));
        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint dividearea cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    public void showRecordingInterface() {
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.vertex.record.divide_3")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.general_3") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("dividearea.message.record_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.vertex.record.divide_2")));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint dividearea cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(ChatFormatting.RED));
        client.player.sendSystemMessage(cancelBtn);
    }

    public void showPointRecordedOptions(int count) {
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.record") + count + I18nManager.translate("dividearea.message.coordinate")));

        MutableComponent continueBtn = Component.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint dividearea continue"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent saveBtn = Component.literal(I18nManager.translate("dividearea.button.record.finish"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint dividearea save"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.coordinate.record.finish"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent cancelBtn = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint dividearea cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(ChatFormatting.RED));

        if (count >= 2) {
            client.player.sendSystemMessage(Component.empty().append(continueBtn).append(Component.nullToEmpty("  ")).append(saveBtn).append(Component.nullToEmpty("  ")).append(cancelBtn));
        } else {
            client.player.sendSystemMessage(Component.empty().append(continueBtn).append(Component.nullToEmpty("  ")).append(cancelBtn));
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.divide_2")));
        }
    }

    public void showCancelMessage() {
        if (client.player == null) return;
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.error.area.cancel.divide")));
    }
}
