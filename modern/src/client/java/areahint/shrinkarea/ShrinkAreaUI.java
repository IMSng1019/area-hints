package areahint.shrinkarea;

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
 * ShrinkArea用户界面系统
 * 使用聊天消息和可点击组件实现交互（类似EasyAdd）
 */
public class ShrinkAreaUI {
    private final ShrinkAreaManager manager;
    private final Minecraft client;
    
    public ShrinkAreaUI(ShrinkAreaManager manager) {
        this.manager = manager;
        this.client = Minecraft.getInstance();
    }
    
    /**
     * 显示域名选择界面（使用聊天消息）
     */
    public void showAreaSelectionScreen() {
        if (client.player == null) return;
        
        List<AreaData> areas = manager.getAvailableAreas();
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.title.area.shrink_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.prompt.area.shrink")));
        
        if (manager.isAdmin()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.area.shrink_3")));
        } else {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.area.shrink_2")));
        }
        
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        if (areas.isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.error.area.shrink_6")));
            MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand("/areahint shrinkarea cancel"))
                    .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.cancel.shrink"))))
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
                        "/areahint shrinkarea select \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("shrinkarea.message.shrink") + displayName + I18nManager.translate("addhint.message.general") + signature + I18nManager.translate("shrinkarea.message.level") + area.getLevel())))
                    .withColor(ChatFormatting.GOLD));
            
            client.player.sendSystemMessage(areaButton);
        }
        
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint shrinkarea cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.cancel.shrink"))))
                .withColor(ChatFormatting.RED));
        
        client.player.sendSystemMessage(cancelButton);
    }
    
    /**
     * 显示坐标点记录后的选项界面
     */
    public void showPointRecordedOptions(int vertexCount) {
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.record") + vertexCount + I18nManager.translate("shrinkarea.message.vertex.shrink")));
        
        // 显示操作选项
        MutableComponent continueButton = Component.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint shrinkarea continue"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.vertex.record.continue"))))
                .withColor(ChatFormatting.GREEN));
        
        MutableComponent saveButton = Component.literal(I18nManager.translate("expandarea.button.area.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint shrinkarea save"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("shrinkarea.message.area.save.shrink"))))
                .withColor(ChatFormatting.AQUA));
        
        if (vertexCount >= 3) {
            // 有足够的点，显示保存选项
            MutableComponent buttonRow = Component.empty()
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
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("shrinkarea.error.area.cancel.shrink")));
    }
}
