package areahint.soundevent;

import areahint.util.TextEventCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;

/**
 * 使用聊天可点击文本显示常用域名切换声音音量。
 */
public final class SoundLevelChatUI {
    private static final float[] COMMON_LEVELS = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};

    private SoundLevelChatUI() {
    }

    /**
     * 显示当前音量、操作提示和常用音量按钮。
     */
    public static void showMenu() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(t("soundlevel.chat.title")));
        client.player.sendSystemMessage(Component.literal(t("soundlevel.chat.current",
                ConfigData.formatSoundLevel(ClientConfig.getSoundLevel()))));
        client.player.sendSystemMessage(Component.literal(t("soundlevel.chat.prompt")));

        MutableComponent buttonRow = Component.empty();
        for (int index = 0; index < COMMON_LEVELS.length; index++) {
            if (index > 0) {
                buttonRow.append(Component.literal("  "));
            }
            buttonRow.append(createLevelButton(COMMON_LEVELS[index]));
        }
        client.player.sendSystemMessage(buttonRow);
    }

    /**
     * 显示音量保存成功消息。
     */
    public static void showSelectionSuccess(float soundLevel) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(t("soundlevel.message.selected",
                    ConfigData.formatSoundLevel(soundLevel))));
        }
    }

    private static MutableComponent createLevelButton(float soundLevel) {
        String formattedLevel = ConfigData.formatSoundLevel(soundLevel);
        // 指令和显示共用稳定格式，避免按钮发送包含浮点尾数的参数。
        String command = "/areahint soundlevel " + formattedLevel;
        return Component.literal(t("soundlevel.button.level", formattedLevel)).setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(command))
                .withHoverEvent(TextEventCompat.showText(
                        Component.literal(t("soundlevel.hover.level", formattedLevel))))
                .withColor(ChatFormatting.AQUA));
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }
}
