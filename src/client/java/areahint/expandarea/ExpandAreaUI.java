package areahint.expandarea;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
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
 * ExpandArea用户界面系统
 * 使用聊天消息和可点击组件实现交互（类似EasyAdd）
 */
public class ExpandAreaUI {
    private final ExpandAreaManager manager;
    private final MinecraftClient client;
    
    public ExpandAreaUI(ExpandAreaManager manager) {
        this.manager = manager;
        this.client = MinecraftClient.getInstance();
    }
    
    /**
     * 显示域名选择界面（使用聊天消息）
     */
    public void showAreaSelection(List<AreaData> areas) {
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("expandarea.title.area.expand")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("expandarea.prompt.area.expand")), false);
        client.player.sendMessage(Text.of(""), false);
        
        if (areas.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("expandarea.error.area.expand_4")), false);
            MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint expandarea cancel"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("expandarea.message.cancel.expand"))))
                    .withColor(Formatting.RED));
            client.player.sendMessage(cancelButton, false);
            return;
        }
        
        // 显示域名列表按钮
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String signature = ClientAreaText.signature(area);
            
            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/areahint expandarea select \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("expandarea.message.expand") + displayName + I18nManager.translate("addhint.message.general") + signature)))
                    .withColor(Formatting.GOLD));
            
            client.player.sendMessage(areaButton, false);
        }
        
        client.player.sendMessage(Text.of(""), false);
        
        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint expandarea cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("expandarea.message.cancel.expand"))))
                .withColor(Formatting.RED));
        
        client.player.sendMessage(cancelButton, false);
    }
    
    /**
     * 显示记录界面（显示提示消息）
     */
    public void showRecordingInterface() {
        if (client.player == null) return;

        client.player.sendMessage(Text.of(I18nManager.translate("expandarea.message.vertex.record_3")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.general_2") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("easyadd.message.record_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("expandarea.message.vertex.record_2")), false);
    }
    
    /**
     * 显示坐标点记录后的选项界面
     */
    public void showPointRecordedOptions(int vertexCount) {
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.record") + vertexCount + I18nManager.translate("dividearea.message.coordinate")), false);
        
        // 显示操作选项
        net.minecraft.text.MutableText continueButton = Text.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(net.minecraft.text.Style.EMPTY
                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint expandarea continue"))
                .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(net.minecraft.util.Formatting.GREEN));
        
        net.minecraft.text.MutableText saveButton = Text.literal(I18nManager.translate("expandarea.button.area.save"))
            .setStyle(net.minecraft.text.Style.EMPTY
                .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND, "/areahint expandarea save"))
                .withHoverEvent(new net.minecraft.text.HoverEvent(net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("expandarea.message.area.save.expand"))))
                .withColor(net.minecraft.util.Formatting.AQUA));
        
        if (vertexCount >= 3) {
            // 有足够的点，显示保存选项
            net.minecraft.text.MutableText buttonRow = Text.empty()
                .append(continueButton)
                .append(Text.of("  "))
                .append(saveButton);
            
            client.player.sendMessage(buttonRow, false);
        } else {
            // 点数不够，只显示继续
            client.player.sendMessage(continueButton, false);
            client.player.sendMessage(Text.of(I18nManager.translate("expandarea.message.area.save")), false);
        }
    }
    
    
    /**
     * 显示取消确认消息
     */
    public void showCancelMessage() {
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("expandarea.error.area.cancel.expand")), false);
    }
}
