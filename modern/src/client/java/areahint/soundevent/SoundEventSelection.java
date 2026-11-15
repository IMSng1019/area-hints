package areahint.soundevent;

import areahint.data.ConfigData;

/**
 * 一个可供玩家选择的声音项，普通声音和音符盒声音统一使用该模型。
 */
public record SoundEventSelection(
        String soundId,
        float pitch,
        String categoryKey,
        String instrumentId,
        int note
) {
    public static final int NO_NOTE = -1;

    /**
     * 创建关闭声音的特殊选择项。
     */
    public static SoundEventSelection none() {
        return new SoundEventSelection(ConfigData.SOUND_EVENT_NONE, 1.0f, "none", null, NO_NOTE);
    }

    /**
     * 创建普通声音选择项。
     */
    public static SoundEventSelection regular(String soundId, String categoryKey) {
        return new SoundEventSelection(soundId, 1.0f, categoryKey, null, NO_NOTE);
    }

    /**
     * 创建指定乐器和音阶的音符盒声音项。
     */
    public static SoundEventSelection note(String soundId, String instrumentId, int note, float pitch) {
        return new SoundEventSelection(soundId, pitch, "note_block", instrumentId, note);
    }

    /**
     * 创建配置中存在但当前注册表缺失的声音项。
     */
    public static SoundEventSelection missing(String soundId, float pitch) {
        return new SoundEventSelection(soundId, pitch, "missing", null, NO_NOTE);
    }

    /**
     * 判断该项是否代表关闭声音。
     */
    public boolean isNone() {
        return ConfigData.SOUND_EVENT_NONE.equals(this.soundId);
    }

    /**
     * 判断该项是否为音符盒声音。
     */
    public boolean isNoteBlock() {
        return this.note >= 0 && this.instrumentId != null;
    }
}
