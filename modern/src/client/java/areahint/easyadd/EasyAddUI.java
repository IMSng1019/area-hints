package areahint.easyadd;

import areahint.util.TextEventCompat;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ClientAreaText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * EasyAdd用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class EasyAddUI {
    
    /**
     * 显示域名名称输入界面
     */
    public static void showNameInputScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.title.area.add")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.name_2")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.general_8")));
        
        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
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
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.surface")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.message.area.surface.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.name")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.hint.area.surface")));
        
        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示域名等级输入界面
     */
    public static void showLevelInputScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.level")));

        // 创建等级选择按钮
        MutableComponent level1Button = Component.literal(I18nManager.translate("dividearea.button.area"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd level 1"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.area_2"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent level2Button = Component.literal(I18nManager.translate("dividearea.button.area_3"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd level 2"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.area_3"))))
                .withColor(ChatFormatting.YELLOW));

        MutableComponent level3Button = Component.literal(I18nManager.translate("dividearea.button.area_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd level 3"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.area_4"))))
                .withColor(ChatFormatting.LIGHT_PURPLE));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));
        
        // 组合按钮显示
        MutableComponent buttonRow = Component.empty()
            .append(level1Button)
            .append(Component.nullToEmpty("  "))
            .append(level2Button)
            .append(Component.nullToEmpty("  "))
            .append(level3Button)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);
        
        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.level.description")));
    }
    
    /**
     * 显示上级域名选择界面
     */
    public static void showBaseSelectScreen(List<AreaData> availableParentAreas) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.area.parent_2")));

        for (AreaData area : availableParentAreas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            MutableComponent areaButton = Component.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(TextEventCompat.runCommand(
                        "/areahint easyadd base \"" + area.getName() + "\""))
                    .withHoverEvent(TextEventCompat.showText(
                        Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("easyadd.message.area.parent"))))
                    .withColor(ChatFormatting.GOLD));
            
            client.player.sendSystemMessage(areaButton);
        }
        
        // 显示取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(cancelButton);
    }

    /**
     * 显示坐标点记录后的界面
     */
    public static void showPointRecordedScreen(List<BlockPos> recordedPoints, BlockPos lastPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("addhint.message.record") + recordedPoints.size() + I18nManager.translate("easyadd.message.record.point")));

        // 显示操作选项
        MutableComponent continueButton = Component.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd continue"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent finishButton = Component.literal(I18nManager.translate("easyadd.button.record.finish"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd finish"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.coordinate.record.confirm"))))
                .withColor(ChatFormatting.AQUA));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));
        
        if (recordedPoints.size() >= 3) {
            // 有足够的点，显示完成选项
            MutableComponent buttonRow = Component.empty()
                .append(continueButton)
                .append(Component.nullToEmpty("  "))
                .append(finishButton)
                .append(Component.nullToEmpty("  "))
                .append(cancelButton);
            
            client.player.sendSystemMessage(buttonRow);
        } else {
            // 点数不够，只显示继续和取消
            MutableComponent buttonRow = Component.empty()
                .append(continueButton)
                .append(Component.nullToEmpty("  "))
                .append(cancelButton);
            
            client.player.sendSystemMessage(buttonRow);
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.record.finish")));
        }
    }
    
    /**
     * 显示确认保存界面
     */
    public static void showConfirmSaveScreen(AreaData areaData) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.title.area.confirm")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.name_2") + areaData.getName()));
        if (areaData.getSurfacename() != null && !areaData.getSurfacename().trim().isEmpty()) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.surface_2") + areaData.getSurfacename()));
        }
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.level") + areaData.getLevel()));
        
        if (areaData.getBaseName() != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.parent_2") + areaData.getBaseName()));
        } else {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.area.parent_3")));
        }
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.vertex") + areaData.getVertices().size() + I18nManager.translate("gui.message.general")));
        
        if (areaData.getAltitude() != null) {
            client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude_12") +
                areaData.getAltitude().getMin() + " ~ " + areaData.getAltitude().getMax()));
        }
        
        // 显示颜色信息（新增）
        String colorHex = areaData.getColor();
        String colorDisplay = colorHex != null ? colorHex : "#FFFFFF";
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.color_2") + colorDisplay));

        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_6") + ClientAreaText.signature(areaData)));
        
        // 显示确认和取消按钮
        MutableComponent saveButton = Component.literal(I18nManager.translate("easyadd.button.area.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd save"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.save.confirm"))))
                .withColor(ChatFormatting.GREEN));

        MutableComponent cancelButton = Component.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));
        
        MutableComponent buttonRow = Component.empty()
            .append(saveButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);
        
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.prompt.save.confirm")));
    }
    
    /**
     * 显示高度选择界面
     */
    public static void showAltitudeSelectionScreen(List<BlockPos> recordedPoints) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        // 计算当前坐标点的高度范围用于显示
        int minY = recordedPoints.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = recordedPoints.stream().mapToInt(BlockPos::getY).max().orElse(0);
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.title.altitude")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.prompt.altitude_3")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude.record") + minY + " ~ " + maxY));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        // 自动计算按钮
        MutableComponent autoButton = Component.literal(I18nManager.translate("easyadd.button.altitude"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd altitude auto"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude.record.expand_2"))))
                .withColor(ChatFormatting.AQUA));
        
        // 自定义按钮
        MutableComponent customButton = Component.literal(I18nManager.translate("command.button.altitude"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd altitude custom"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("easyadd.prompt.altitude_7"))))
                .withColor(ChatFormatting.LIGHT_PURPLE));
        
        // 不限制高度按钮
        MutableComponent unlimitedButton = Component.literal(I18nManager.translate("command.button.altitude_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd altitude unlimited"))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("command.message.area.altitude.coordinate"))))
                .withColor(ChatFormatting.YELLOW));
        
        // 取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));
        MutableComponent buttonRow = Component.empty()
            .append(autoButton)
            .append(Component.nullToEmpty("  "))
            .append(customButton)
            .append(Component.nullToEmpty("  "))
            .append(unlimitedButton)
            .append(Component.nullToEmpty("  "))
            .append(cancelButton);
        
        client.player.sendSystemMessage(buttonRow);
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.altitude.record.expand")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("command.message.area.altitude.boundary")));
    }
    
    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.title.area.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("dividearea.prompt.area.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("easyadd.message.area.color")));
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        
        // 第一行颜色按钮
        MutableComponent row1 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));
        
        // 第二行颜色按钮
        MutableComponent row2 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_27"), "#FF0000", "§c"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));
        
        // 第三行颜色按钮
        MutableComponent row3 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));
        
        // 第四行颜色按钮
        MutableComponent row4 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));
        // 闪烁效果按钮行
        MutableComponent row5 = Component.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Component.nullToEmpty("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        // 取消按钮
        MutableComponent cancelButton = Component.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand("/areahint easyadd cancel"))
                .withHoverEvent(TextEventCompat.showText(Component.nullToEmpty(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(ChatFormatting.RED));

        client.player.sendSystemMessage(row1);
        client.player.sendSystemMessage(row2);
        client.player.sendSystemMessage(row3);
        client.player.sendSystemMessage(row4);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(Component.nullToEmpty(I18nManager.translate("gui.message.general_4")));
        client.player.sendSystemMessage(row5);
        client.player.sendSystemMessage(Component.nullToEmpty(""));
        client.player.sendSystemMessage(cancelButton);
    }
    
    /**
     * 创建颜色选择按钮
     */
    private static MutableComponent createColorButton(String colorName, String colorValue, String minecraftColor) {
        String command = colorValue.equals("custom") ? 
            "/areahint easyadd color custom" : 
            "/areahint easyadd color " + colorValue;
        
        return Component.literal(minecraftColor + "[" + colorName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(command))
                .withHoverEvent(TextEventCompat.showText(
                    Component.nullToEmpty(I18nManager.translate("addhint.prompt.general") + colorName + I18nManager.translate("dividearea.message.area.color")))));
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
