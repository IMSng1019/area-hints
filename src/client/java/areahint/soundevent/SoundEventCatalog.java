package areahint.soundevent;

import areahint.AreashintClient;
import areahint.data.ConfigData;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 客户端声音注册表目录，负责一次性扫描、分类和生成音符盒音阶项。
 */
public final class SoundEventCatalog {
    public static final String CATEGORY_NOTE_BLOCK = "note_block";
    public static final String CATEGORY_AMBIENT_WEATHER = "ambient_weather";
    public static final String CATEGORY_BLOCK = "block";
    public static final String CATEGORY_ENTITY = "entity";
    public static final String CATEGORY_ITEM = "item";
    public static final String CATEGORY_MUSIC = "music";
    public static final String CATEGORY_UI = "ui";
    public static final String CATEGORY_EVENT_PARTICLE = "event_particle";
    public static final String CATEGORY_OTHER = "other";

    private static final List<String> CATEGORY_ORDER = List.of(
            CATEGORY_NOTE_BLOCK,
            CATEGORY_AMBIENT_WEATHER,
            CATEGORY_BLOCK,
            CATEGORY_ENTITY,
            CATEGORY_ITEM,
            CATEGORY_MUSIC,
            CATEGORY_UI,
            CATEGORY_EVENT_PARTICLE,
            CATEGORY_OTHER
    );

    private static volatile Snapshot snapshot;

    private SoundEventCatalog() {
    }

    /**
     * 获取懒加载的声音目录快照。
     */
    public static Snapshot getSnapshot() {
        Snapshot current = snapshot;
        if (current != null) {
            return current;
        }
        synchronized (SoundEventCatalog.class) {
            current = snapshot;
            if (current == null) {
                current = buildSnapshot();
                snapshot = current;
            }
        }
        return current;
    }

    /**
     * 清除缓存，供客户端重新加载资源或开发环境刷新注册表时使用。
     */
    public static synchronized void clear() {
        snapshot = null;
    }

    /**
     * 解析当前客户端注册表中的声音事件。
     */
    public static Optional<SoundEvent> resolve(String soundId) {
        if (!ConfigData.isValidSoundEventId(soundId)
                || ConfigData.SOUND_EVENT_NONE.equalsIgnoreCase(soundId)) {
            return Optional.empty();
        }
        Identifier identifier = Identifier.tryParse(soundId);
        if (identifier == null) {
            return Optional.empty();
        }
        return Registries.SOUND_EVENT.getOrEmpty(identifier);
    }

    /**
     * 判断声音事件是否仍存在于当前客户端注册表。
     */
    public static boolean contains(String soundId) {
        return resolve(soundId).isPresent();
    }

    /**
     * 按配置中的 ID 和音高恢复一个选择项，用于界面显示当前值。
     */
    public static SoundEventSelection fromConfig(String soundId, float pitch) {
        if (ConfigData.SOUND_EVENT_NONE.equalsIgnoreCase(soundId)) {
            return SoundEventSelection.none();
        }
        Identifier identifier = Identifier.tryParse(soundId);
        if (identifier == null || !Registries.SOUND_EVENT.containsId(identifier)) {
            return SoundEventSelection.missing(soundId, pitch);
        }
        String category = classify(identifier.getPath());
        if (CATEGORY_NOTE_BLOCK.equals(category)) {
            String instrument = noteInstrument(identifier.getPath());
            int note = nearestNote(pitch);
            return SoundEventSelection.note(soundId, instrument, note, ConfigData.clampSoundPitch(pitch));
        }
        return SoundEventSelection.regular(soundId, category);
    }

    /**
     * 根据命令或界面传入的声音 ID 和音高创建可保存的选择项。
     */
    public static Optional<SoundEventSelection> createSelection(String soundId, float pitch) {
        if (!Float.isFinite(pitch) || pitch < ConfigData.SOUND_PITCH_MIN
                || pitch > ConfigData.SOUND_PITCH_MAX) {
            return Optional.empty();
        }
        if (ConfigData.SOUND_EVENT_NONE.equalsIgnoreCase(soundId)) {
            return Optional.of(SoundEventSelection.none());
        }
        Identifier identifier = Identifier.tryParse(soundId);
        if (identifier == null || !Registries.SOUND_EVENT.containsId(identifier)) {
            return Optional.empty();
        }
        String category = classify(identifier.getPath());
        if (CATEGORY_NOTE_BLOCK.equals(category)) {
            String instrument = noteInstrument(identifier.getPath());
            int note = nearestNote(pitch);
            return Optional.of(SoundEventSelection.note(soundId, instrument, note, notePitch(note)));
        }
        return Optional.of(SoundEventSelection.regular(soundId, category));
    }

    /**
     * 获取固定顺序的非空分类。
     */
    public static List<Category> getCategories() {
        return getSnapshot().categories();
    }

    /**
     * 通过分类 ID 查找分类。
     */
    public static Category getCategory(String categoryKey) {
        return getSnapshot().categoryMap().get(categoryKey);
    }

    /**
     * 计算指定音阶的原版音符盒音高。
     */
    public static float notePitch(int note) {
        int clampedNote = Math.max(0, Math.min(24, note));
        return (float) Math.pow(2.0, (clampedNote - 12) / 12.0);
    }

    /**
     * 从任意音高找到最接近的原版音阶。
     */
    public static int nearestNote(float pitch) {
        int nearest = 0;
        float nearestDistance = Float.MAX_VALUE;
        for (int note = 0; note <= 24; note++) {
            float distance = Math.abs(notePitch(note) - pitch);
            if (distance < nearestDistance) {
                nearest = note;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static Snapshot buildSnapshot() {
        Map<String, List<SoundEventSelection>> regularByCategory = new LinkedHashMap<>();
        Map<String, List<SoundEventSelection>> notesByInstrument = new LinkedHashMap<>();
        for (String category : CATEGORY_ORDER) {
            regularByCategory.put(category, new ArrayList<>());
        }

        List<Identifier> identifiers = new ArrayList<>(Registries.SOUND_EVENT.getIds());
        identifiers.sort(Comparator.naturalOrder());
        for (Identifier identifier : identifiers) {
            String category = classify(identifier.getPath());
            if (CATEGORY_NOTE_BLOCK.equals(category)) {
                String instrument = noteInstrument(identifier.getPath());
                notesByInstrument
                        // 使用完整注册表 ID 分组，避免不同模组注册同名乐器时被错误合并。
                        .computeIfAbsent(identifier.toString(), ignored -> new ArrayList<>())
                        .addAll(createNoteSelections(identifier.toString(), instrument));
            } else {
                regularByCategory.get(category).add(SoundEventSelection.regular(identifier.toString(), category));
            }
        }

        Map<String, Category> categoryMap = new LinkedHashMap<>();
        for (String category : CATEGORY_ORDER) {
            List<SoundEventSelection> sounds = sortedSelections(regularByCategory.get(category));
            List<InstrumentGroup> instruments = new ArrayList<>();
            if (CATEGORY_NOTE_BLOCK.equals(category)) {
                for (Map.Entry<String, List<SoundEventSelection>> entry : notesByInstrument.entrySet()) {
                    List<SoundEventSelection> notes = sortedSelections(
                            entry.getValue(), Comparator.comparingInt(SoundEventSelection::note));
                    instruments.add(new InstrumentGroup(entry.getKey(), notes));
                }
            }
            instruments.sort(Comparator.comparing(InstrumentGroup::soundId));
            if (!sounds.isEmpty() || !instruments.isEmpty()) {
                categoryMap.put(category, new Category(category, sounds, instruments));
            }
        }

        AreashintClient.LOGGER.info("已构建客户端声音目录: {} 个声音事件, {} 个分类",
                identifiers.size(), categoryMap.size());
        return new Snapshot(categoryMap);
    }

    private static List<SoundEventSelection> createNoteSelections(String soundId, String instrument) {
        List<SoundEventSelection> selections = new ArrayList<>();
        for (int note = 0; note <= 24; note++) {
            selections.add(SoundEventSelection.note(soundId, instrument, note, notePitch(note)));
        }
        return selections;
    }

    private static List<SoundEventSelection> sortedSelections(List<SoundEventSelection> selections) {
        return sortedSelections(selections, Comparator.comparing(SoundEventSelection::soundId));
    }

    private static List<SoundEventSelection> sortedSelections(
            List<SoundEventSelection> selections,
            Comparator<SoundEventSelection> comparator) {
        List<SoundEventSelection> sorted = new ArrayList<>(selections);
        sorted.sort(comparator.thenComparing(SoundEventSelection::soundId));
        return Collections.unmodifiableList(sorted);
    }

    private static String classify(String path) {
        if (path.startsWith("block.note_block.")) {
            return CATEGORY_NOTE_BLOCK;
        }
        if (path.startsWith("ambient.") || path.startsWith("weather.")) {
            return CATEGORY_AMBIENT_WEATHER;
        }
        if (path.startsWith("block.")) {
            return CATEGORY_BLOCK;
        }
        if (path.startsWith("entity.") || path.startsWith("player.")) {
            return CATEGORY_ENTITY;
        }
        if (path.startsWith("item.")) {
            return CATEGORY_ITEM;
        }
        if (path.startsWith("music.") || path.startsWith("music_disc.") || path.startsWith("record.")) {
            return CATEGORY_MUSIC;
        }
        if (path.startsWith("ui.")) {
            return CATEGORY_UI;
        }
        if (path.startsWith("event.") || path.startsWith("particle.")) {
            return CATEGORY_EVENT_PARTICLE;
        }
        return CATEGORY_OTHER;
    }

    private static String noteInstrument(String path) {
        String prefix = "block.note_block.";
        if (!path.startsWith(prefix)) {
            return path;
        }
        String instrument = path.substring(prefix.length());
        int separator = instrument.indexOf('.');
        return separator >= 0 ? instrument.substring(0, separator) : instrument;
    }

    /**
     * 分类快照，构建完成后只读共享。
     */
    public record Snapshot(Map<String, Category> categoryMap) {
        public Snapshot {
            categoryMap = Collections.unmodifiableMap(new LinkedHashMap<>(categoryMap));
        }

        public List<Category> categories() {
            return List.copyOf(categoryMap.values());
        }
    }

    /**
     * 一个声音分类及其普通声音或音符盒乐器组。
     */
    public record Category(String key, List<SoundEventSelection> sounds, List<InstrumentGroup> instruments) {
        public Category {
            sounds = List.copyOf(sounds);
            instruments = List.copyOf(instruments);
        }
    }

    /**
     * 一个音符盒乐器以及该乐器的 25 个音阶。
     */
    public record InstrumentGroup(String soundId, List<SoundEventSelection> notes) {
        public InstrumentGroup {
            notes = List.copyOf(notes);
        }
    }
}
