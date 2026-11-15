package areahint.expandarea;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.AreaDataConverter;
import areahint.util.ClientAreaText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * ExpandArea用户界面系统
 * 使用聊天消息和可点击组件实现交互（类似EasyAdd）
 */
public class ExpandAreaUI {
    private final ExpandAreaManager manager;
    private final Minecraft client;
    
    public ExpandAreaUI(ExpandAreaManager manager) {
        this.manager = manager;
        this.client = Minecraft.getInstance();
    }
    
    /**
     * 显示域名选择界面（使用聊天消息）
     */
    public void showAreaSelection(List<AreaData> areas) {
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.title.area.expand")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.prompt.area.expand")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        if (areas.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.error.area.expand_4")));
            MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand("/areahint expandarea cancel"))
                    .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("expandarea.message.cancel.expand"))))
                    .withColor(ChatFormatting.RED));
            client.player.sendSystemMessage(cancelButton);
            return;
        }
        
        // 显示域名列表按钮
        for (AreaData area : areas) {
            String displayName = AreaDataConverter.getDisplayName(area);
            String signature = ClientAreaText.signature(area);
            
            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint expandarea select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("expandarea.message.expand") + displayName + I18nManager.translate("addhint.message.general") + signature)))
                    .withColor(ChatFormatting.GOLD));
            
            client.player.sendSystemMessage(areaButton);
        }
        
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint expandarea cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("expandarea.message.cancel.expand"))))
                .withColor(ChatFormatting.RED));
        
        client.player.sendSystemMessage(cancelButton);
    }
    
    /**
     * 显示记录界面（显示提示消息）
     */
    public void showRecordingInterface() {
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.message.vertex.record_3")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.general_2") + areahint.keyhandler.UnifiedKeyHandler.getRecordKeyDisplayName() + I18nManager.translate("easyadd.message.record_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.message.vertex.record_2")));
    }
    
    /**
     * 显示坐标点记录后的选项界面
     */
    public void showPointRecordedOptions(int vertexCount) {
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.record") + vertexCount + I18nManager.translate("dividearea.message.coordinate")));
        
        // 显示操作选项
        net.minecraft.network.chat.MutableComponent continueButton = Component.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(net.minecraft.network.chat.Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint expandarea continue"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(net.minecraft.ChatFormatting.GREEN));
        
        net.minecraft.network.chat.MutableComponent saveButton = Component.literal(I18nManager.translate("expandarea.button.area.save"))
            .setStyle(net.minecraft.network.chat.Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint expandarea save"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("expandarea.message.area.save.expand"))))
                .withColor(net.minecraft.ChatFormatting.AQUA));
        
        if (vertexCount >= 3) {
            // 有足够的点，显示保存选项
            net.minecraft.network.chat.MutableComponent buttonRow = Component.empty()
                .append(continueButton)
                .append(Component.nullToEmpty("  "))
                .append(saveButton);
            
            client.player.sendSystemMessage(buttonRow);
        } else {
            // 点数不够，只显示继续
            client.player.sendSystemMessage(continueButton);
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.message.area.save")));
        }
    }
    
    
    /**
     * 显示取消确认消息
     */
    public void showCancelMessage() {
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("expandarea.error.area.cancel.expand")));
    }
}
