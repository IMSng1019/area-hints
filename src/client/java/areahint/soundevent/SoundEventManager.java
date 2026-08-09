package areahint.soundevent;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 域名切换声音管理器，统一处理试听、保存和配置显示。
 */
public final class SoundEventManager {
    private static final Set<String> MISSING_SOUND_WARNINGS = new HashSet<>();

    private SoundEventManager() {
    }

    /**
     * 获取当前配置对应的选择项。
     */
    public static SoundEventSelection getCurrentSelection() {
        return SoundEventCatalog.fromConfig(ClientConfig.getSoundEvent(), ClientConfig.getSoundPitch());
    }

    /**
     * 试听声音但不修改配置。
     */
    public static boolean preview(SoundEventSelection selection) {
        if (selection == null || selection.isNone()) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client != null ? client.player : null;
        if (player == null) {
            return false;
        }

        Optional<SoundEvent> soundEvent = SoundEventCatalog.resolve(selection.soundId());
        if (soundEvent.isEmpty()) {
            warnMissing(selection.soundId());
            return false;
        }

        player.playSound(soundEvent.get(), SoundCategory.PLAYERS, 1.0f,
                ConfigData.clampSoundPitch(selection.pitch()));
        return true;
    }

    /**
     * 保存选择并立即试听一次。
     */
    public static boolean applySelection(SoundEventSelection selection) {
        if (selection == null) {
            return false;
        }
        if (!selection.isNone() && !SoundEventCatalog.contains(selection.soundId())) {
            warnMissing(selection.soundId());
            return false;
        }

        ClientConfig.setSoundSelection(selection.soundId(), selection.pitch());
        if (!selection.isNone()) {
            preview(selection);
        }
        return true;
    }

    /**
     * 将选择写入配置草稿并试听，不触发配置文件保存。
     */
    public static boolean applyToDraft(ConfigData draft, SoundEventSelection selection) {
        if (draft == null || selection == null) {
            return false;
        }
        if (!selection.isNone() && !SoundEventCatalog.contains(selection.soundId())) {
            warnMissing(selection.soundId());
            return false;
        }

        draft.setSoundEvent(selection.soundId());
        draft.setSoundPitch(selection.pitch());
        if (!selection.isNone()) {
            preview(selection);
        }
        return true;
    }

    /**
     * 播放当前配置的声音，供域名状态追踪器调用。
     */
    public static void playConfiguredSound() {
        String soundId = ClientConfig.getSoundEvent();
        if (ConfigData.SOUND_EVENT_NONE.equals(soundId)) {
            return;
        }
        preview(new SoundEventSelection(soundId, ClientConfig.getSoundPitch(), "configured", null,
                SoundEventSelection.NO_NOTE));
    }

    /**
     * 获取配置项在界面上的简短显示文本。
     */
    public static String getDisplayText(SoundEventSelection selection) {
        if (selection == null || selection.isNone()) {
            return I18nManager.translate("soundevent.value.none");
        }
        if (selection.isNoteBlock()) {
            return I18nManager.translate("soundevent.value.note",
                    selection.soundId(), selection.note(), formatPitch(selection.pitch()));
        }
        if ("missing".equals(selection.categoryKey())) {
            return I18nManager.translate("soundevent.value.missing", selection.soundId());
        }
        return selection.soundId();
    }

    /**
     * 重置本会话中已记录的缺失声音警告。
     */
    public static void resetWarnings() {
        MISSING_SOUND_WARNINGS.clear();
    }

    private static void warnMissing(String soundId) {
        if (soundId == null || !MISSING_SOUND_WARNINGS.add(soundId)) {
            return;
        }
        AreashintClient.LOGGER.warn("客户端声音事件不存在，已跳过播放: {}", soundId);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                    I18nManager.translate("soundevent.error.missing", soundId)), false);
        }
    }

    private static String formatPitch(float pitch) {
        return String.format(java.util.Locale.ROOT, "%.2f", pitch);
    }
}
