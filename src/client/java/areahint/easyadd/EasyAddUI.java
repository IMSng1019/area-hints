package areahint.easyadd;

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
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * EasyAdd用户界面系统
 * 使用聊天消息和可点击组件实现交互
 */
public class EasyAddUI {
    
    /**
     * 显示域名名称输入界面
     */
    public static void showNameInputScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.title.area.add")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.name_2")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.general_8")), false);
        
        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
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
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.surface")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.message.area.surface.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.name")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.hint.area.surface")), false);
        
        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示域名等级输入界面
     */
    public static void showLevelInputScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.level")), false);

        // 创建等级选择按钮
        MutableText level1Button = Text.literal(I18nManager.translate("dividearea.button.area"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd level 1"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_2"))))
                .withColor(Formatting.AQUA));

        MutableText level2Button = Text.literal(I18nManager.translate("dividearea.button.area_3"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd level 2"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_3"))))
                .withColor(Formatting.YELLOW));

        MutableText level3Button = Text.literal(I18nManager.translate("dividearea.button.area_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd level 3"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.prompt.area_4"))))
                .withColor(Formatting.LIGHT_PURPLE));

        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));
        
        // 组合按钮显示
        MutableText buttonRow = Text.empty()
            .append(level1Button)
            .append(Text.of("  "))
            .append(level2Button)
            .append(Text.of("  "))
            .append(level3Button)
            .append(Text.of("  "))
            .append(cancelButton);
        
        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.level.description")), false);
    }
    
    /**
     * 显示上级域名选择界面
     */
    public static void showBaseSelectScreen(List<AreaData> availableParentAreas) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.area.parent_2")), false);

        for (AreaData area : availableParentAreas) {
            String displayName = areahint.util.AreaDataConverter.getDisplayName(area);
            MutableText areaButton = Text.literal("§6[" + displayName + "]")
                .setStyle(Style.EMPTY
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/areahint easyadd base \"" + area.getName() + "\""))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.of(I18nManager.translate("addhint.prompt.general") + displayName + I18nManager.translate("easyadd.message.area.parent"))))
                    .withColor(Formatting.GOLD));
            
            client.player.sendMessage(areaButton, false);
        }
        
        // 显示取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(cancelButton, false);
    }

    /**
     * 显示坐标点记录后的界面
     */
    public static void showPointRecordedScreen(List<BlockPos> recordedPoints, BlockPos lastPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("addhint.message.record") + recordedPoints.size() + I18nManager.translate("easyadd.message.record.point")), false);

        // 显示操作选项
        MutableText continueButton = Text.literal(I18nManager.translate("addhint.button.record.continue"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd continue"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("dividearea.message.coordinate.record.continue"))))
                .withColor(Formatting.GREEN));

        MutableText finishButton = Text.literal(I18nManager.translate("easyadd.button.record.finish"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd finish"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.coordinate.record.confirm"))))
                .withColor(Formatting.AQUA));

        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));
        
        if (recordedPoints.size() >= 3) {
            // 有足够的点，显示完成选项
            MutableText buttonRow = Text.empty()
                .append(continueButton)
                .append(Text.of("  "))
                .append(finishButton)
                .append(Text.of("  "))
                .append(cancelButton);
            
            client.player.sendMessage(buttonRow, false);
        } else {
            // 点数不够，只显示继续和取消
            MutableText buttonRow = Text.empty()
                .append(continueButton)
                .append(Text.of("  "))
                .append(cancelButton);
            
            client.player.sendMessage(buttonRow, false);
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.record.finish")), false);
        }
    }
    
    /**
     * 显示确认保存界面
     */
    public static void showConfirmSaveScreen(AreaData areaData) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.title.area.confirm")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.name_2") + areaData.getName()), false);
        if (areaData.getSurfacename() != null && !areaData.getSurfacename().trim().isEmpty()) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.surface_2") + areaData.getSurfacename()), false);
        }
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.level") + areaData.getLevel()), false);
        
        if (areaData.getBaseName() != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.parent_2") + areaData.getBaseName()), false);
        } else {
            client.player.sendMessage(Text.of(I18nManager.translate("gui.message.area.parent_3")), false);
        }
        
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.vertex") + areaData.getVertices().size() + I18nManager.translate("gui.message.general")), false);
        
        if (areaData.getAltitude() != null) {
            client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.altitude_12") +
                areaData.getAltitude().getMin() + " ~ " + areaData.getAltitude().getMax()), false);
        }
        
        // 显示颜色信息（新增）
        String colorHex = areaData.getColor();
        String colorDisplay = colorHex != null ? colorHex : "#FFFFFF";
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.color_2") + colorDisplay), false);

        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_6") + ClientAreaText.signature(areaData)), false);
        
        // 显示确认和取消按钮
        MutableText saveButton = Text.literal(I18nManager.translate("easyadd.button.area.save"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd save"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.area.save.confirm"))))
                .withColor(Formatting.GREEN));

        MutableText cancelButton = Text.literal(I18nManager.translate("addhint.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));
        
        MutableText buttonRow = Text.empty()
            .append(saveButton)
            .append(Text.of("  "))
            .append(cancelButton);
        
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.prompt.save.confirm")), false);
    }
    
    /**
     * 显示高度选择界面
     */
    public static void showAltitudeSelectionScreen(List<BlockPos> recordedPoints) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        // 计算当前坐标点的高度范围用于显示
        int minY = recordedPoints.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = recordedPoints.stream().mapToInt(BlockPos::getY).max().orElse(0);
        
        client.player.sendMessage(Text.of(I18nManager.translate("command.title.altitude")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("command.prompt.altitude_3")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.altitude.record") + minY + " ~ " + maxY), false);
        client.player.sendMessage(Text.of(""), false);
        
        // 自动计算按钮
        MutableText autoButton = Text.literal(I18nManager.translate("easyadd.button.altitude"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd altitude auto"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.of(I18nManager.translate("easyadd.message.altitude.record.expand_2"))))
                .withColor(Formatting.AQUA));
        
        // 自定义按钮
        MutableText customButton = Text.literal(I18nManager.translate("command.button.altitude"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd altitude custom"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.of(I18nManager.translate("easyadd.prompt.altitude_7"))))
                .withColor(Formatting.LIGHT_PURPLE));
        
        // 不限制高度按钮
        MutableText unlimitedButton = Text.literal(I18nManager.translate("command.button.altitude_2"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd altitude unlimited"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Text.of(I18nManager.translate("command.message.area.altitude.coordinate"))))
                .withColor(Formatting.YELLOW));
        
        // 取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));
        MutableText buttonRow = Text.empty()
            .append(autoButton)
            .append(Text.of("  "))
            .append(customButton)
            .append(Text.of("  "))
            .append(unlimitedButton)
            .append(Text.of("  "))
            .append(cancelButton);
        
        client.player.sendMessage(buttonRow, false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.altitude.record.expand")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("command.message.area.altitude.boundary")), false);
    }
    
    /**
     * 显示颜色选择界面
     */
    public static void showColorSelectionScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.title.area.color")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("dividearea.prompt.area.color")), false);
        client.player.sendMessage(Text.of(I18nManager.translate("easyadd.message.area.color")), false);
        client.player.sendMessage(Text.of(""), false);
        
        // 第一行颜色按钮
        MutableText row1 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_24"), "#FFFFFF", "§f"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_23"), "#808080", "§7"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_18"), "#555555", "§8"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_31"), "#000000", "§0"));
        
        // 第二行颜色按钮
        MutableText row2 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_19"), "#AA0000", "§4"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_27"), "#FF0000", "§c"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_25"), "#FF55FF", "§d"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_17"), "#FFAA00", "§6"));
        
        // 第三行颜色按钮
        MutableText row3 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_30"), "#FFFF55", "§e"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_28"), "#55FF55", "§a"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_20"), "#00AA00", "§2"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_13"), "#55FFFF", "§b"));
        
        // 第四行颜色按钮
        MutableText row4 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_22"), "#00AAAA", "§3"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_29"), "#5555FF", "§9"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_21"), "#0000AA", "§1"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_26"), "#AA00AA", "§5"));
        // 闪烁效果按钮行
        MutableText row5 = Text.empty()
            .append(createColorButton(I18nManager.translate("gui.message.general_15"), "FLASH_BW_ALL", "§7"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_14"), "FLASH_RAINBOW_ALL", "§b"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_12"), "FLASH_BW_CHAR", "§8"))
            .append(Text.of("  "))
            .append(createColorButton(I18nManager.translate("gui.message.general_11"), "FLASH_RAINBOW_CHAR", "§d"));

        // 取消按钮
        MutableText cancelButton = Text.literal(I18nManager.translate("command.error.cancel"))
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/areahint easyadd cancel"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(I18nManager.translate("easyadd.message.cancel_2"))))
                .withColor(Formatting.RED));

        client.player.sendMessage(row1, false);
        client.player.sendMessage(row2, false);
        client.player.sendMessage(row3, false);
        client.player.sendMessage(row4, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(Text.of(I18nManager.translate("gui.message.general_4")), false);
        client.player.sendMessage(row5, false);
        client.player.sendMessage(Text.of(""), false);
        client.player.sendMessage(cancelButton, false);
    }
    
    /**
     * 创建颜色选择按钮
     */
    private static MutableText createColorButton(String colorName, String colorValue, String minecraftColor) {
        String command = colorValue.equals("custom") ? 
            "/areahint easyadd color custom" : 
            "/areahint easyadd color " + colorValue;
        
        return Text.literal(minecraftColor + "[" + colorName + "]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Text.of(I18nManager.translate("addhint.prompt.general") + colorName + I18nManager.translate("dividearea.message.area.color")))));
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
