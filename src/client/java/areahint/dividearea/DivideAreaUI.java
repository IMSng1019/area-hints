package areahint.dividearea;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

public class DivideAreaUI {
    private final DivideAreaManager manager;
    private final MinecraftClient client;

    public DivideAreaUI(DivideAreaManager manager) {
        this.manager = manager;
        this.client = MinecraftClient.getInstance();
    }

    public void showAreaSelection(List<AreaData> areas) {
        if (client.player == null) return;
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.title.area.divide")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.prompt.area.divide")), false);
        client.player.sendMessage(Text.of(""), false);

        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            MutableText btn = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/areahint dividearea select \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("dividearea.message.divide_4") + displayName + I18nManager.translate("addhint.message.general") + ClientAreaText.signature(area))))
                    .withColor(Formatting.GOLD));
            client.player.sendMessage(btn, false);
        }

        client.player.sendMessage(Text.of(""), false);
        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    public void showRecordingInterface() {
        if (client.player == null) return;
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.vertex.record.divide_3")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.prompt.general_3") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("dividearea.message.record_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.vertex.record.divide_2")), false);

        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(Formatting.RED));
        client.player.sendMessage(cancelBtn, false);
    }

    public void showPointRecordedOptions(int count) {
        if (client.player == null) return;
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.record") + count + I18nManager.translate("dividearea.message.coordinate")), false);

        MutableText continueBtn = Text.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint dividearea continue"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(Formatting.GREEN));

        MutableText saveBtn = Text.literal(I18nManager.translate("dividearea.button.record.finish"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint dividearea save"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.coordinate.record.finish"))))
                .withColor(Formatting.AQUA));

        MutableText cancelBtn = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint dividearea cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.cancel.divide"))))
                .withColor(Formatting.RED));

        if (count >= 2) {
            client.player.sendMessage(Text.empty().append(continueBtn).append(Text.of("  ")).append(saveBtn).append(Text.of("  ")).append(cancelBtn), false);
        } else {
            client.player.sendMessage(Text.empty().append(continueBtn).append(Text.of("  ")).append(cancelBtn), false);
            client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.divide_2")), false);
        }
    }

    public void showCancelMessage() {
        if (client.player == null) return;
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.error.area.cancel.divide")), false);
    }
}
