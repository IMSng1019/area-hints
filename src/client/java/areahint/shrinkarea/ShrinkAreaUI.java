package areahint.shrinkarea;

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
 * ShrinkArea用户界面系统
 * 使用聊天消息和可点击组件实现交互（类似EasyAdd）
 */
public class ShrinkAreaUI {
    private final ShrinkAreaManager manager;
    private final MinecraftClient client;
    
    public ShrinkAreaUI(ShrinkAreaManager manager) {
        this.manager = manager;
        this.client = MinecraftClient.getInstance();
    }
    
    /**
     * 显示域名选择界面（使用聊天消息）
     */
    public void showAreaSelectionScreen() {
        if (client.player == null) return;
        
        List<AreaData> areas = manager.getAvailableAreas();
        
        client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.title.area.shrink_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.prompt.area.shrink")), false);
        
        if (manager.isAdmin()) {
            client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.message.area.shrink_3")), false);
        } else {
            client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.message.area.shrink_2")), false);
        }
        
        client.player.sendMessage(Text.of(""), false);
        
        if (areas.isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.error.area.shrink_6")), false);
            MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint shrinkarea cancel"))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("shrinkarea.message.cancel.shrink"))))
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
                        "/areahint shrinkarea select \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("shrinkarea.message.shrink") + displayName + I18nManager.translate("addhint.message.general") + signature + I18nManager.translate("shrinkarea.message.level") + area.getLevel())))
                    .withColor(Formatting.GOLD));
            
            client.player.sendMessage(areaButton, false);
        }
        
        client.player.sendMessage(Text.of(""), false);
        
        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint shrinkarea cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("shrinkarea.message.cancel.shrink"))))
                .withColor(Formatting.RED));
        
        client.player.sendMessage(cancelButton, false);
    }
    
    /**
     * 显示坐标点记录后的选项界面
     */
    public void showPointRecordedOptions(int vertexCount) {
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.record") + vertexCount + I18nManager.translate("shrinkarea.message.vertex.shrink")), false);
        
        // 显示操作选项
        MutableText continueButton = Text.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint shrinkarea continue"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("shrinkarea.message.vertex.record.continue"))))
                .withColor(Formatting.GREEN));
        
        MutableText saveButton = Text.literal(I18nManager.translate("expandarea.button.area.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint shrinkarea save"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("shrinkarea.message.area.save.shrink"))))
                .withColor(Formatting.AQUA));
        
        if (vertexCount >= 3) {
            // 有足够的点，显示保存选项
            MutableText buttonRow = Text.empty()
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
        
        client.player.sendMessage(Text.of(I18nManager.translate("shrinkarea.error.area.cancel.shrink")), false);
    }
}
