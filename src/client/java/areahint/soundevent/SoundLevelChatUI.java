package areahint.soundevent;

import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.player.sendMessage(Text.literal(t("soundlevel.chat.title")), false);
        client.player.sendMessage(Text.literal(t("soundlevel.chat.current",
                ConfigData.formatSoundLevel(ClientConfig.getSoundLevel()))), false);
        client.player.sendMessage(Text.literal(t("soundlevel.chat.prompt")), false);

        MutableText buttonRow = Text.empty();
        for (int index = 0; index < COMMON_LEVELS.length; index++) {
            if (index > 0) {
                buttonRow.append(Text.literal("  "));
            }
            buttonRow.append(createLevelButton(COMMON_LEVELS[index]));
        }
        client.player.sendMessage(buttonRow, false);
    }

    /**
     * 显示音量保存成功消息。
     */
    public static void showSelectionSuccess(float soundLevel) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(t("soundlevel.message.selected",
                    ConfigData.formatSoundLevel(soundLevel))), false);
        }
    }

    private static MutableText createLevelButton(float soundLevel) {
        String formattedLevel = ConfigData.formatSoundLevel(soundLevel);
        // 指令和显示共用稳定格式，避免按钮发送包含浮点尾数的参数。
        String command = "/areahint soundlevel " + formattedLevel;
        return Text.literal(t("soundlevel.button.level", formattedLevel)).setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(t("soundlevel.hover.level", formattedLevel))))
                .withColor(Formatting.AQUA));
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }
}
