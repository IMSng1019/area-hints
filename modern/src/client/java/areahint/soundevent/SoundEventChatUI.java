package areahint.soundevent;

import areahint.util.TextEventCompat;

import areahint.i18n.I18nManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * 使用聊天可点击文本实现的声音选择流程，适合直接输入指令。
 */
public final class SoundEventChatUI {
    private static final int PAGE_SIZE = 8;

    private SoundEventChatUI() {
    }

    /**
     * 显示声音分类入口。
     */
    public static void showCategories() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.title")));
        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.current",
                SoundEventManager.getDisplayText(SoundEventManager.getCurrentSelection()))));
        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.category_prompt")));
        client.player.sendSystemMessage(createButton(t("soundevent.button.none"),
                "/areahint replacesoundevent none", ChatFormatting.GRAY,
                t("soundevent.hover.none")));

        for (SoundEventCatalog.Category category : SoundEventCatalog.getCategories()) {
            int count = category.sounds().size();
            if (SoundEventCatalog.CATEGORY_NOTE_BLOCK.equals(category.key())) {
                count = category.instruments().size();
            }
            String label = t("soundevent.category." + category.key());
            client.player.sendSystemMessage(createButton(t("soundevent.button.category", label, count),
                    "/areahint replacesoundevent category " + category.key() + " 0",
                    ChatFormatting.AQUA, t("soundevent.hover.category", label)));
        }

        client.player.sendSystemMessage(createButton(t("soundevent.button.cancel"),
                "/areahint replacesoundevent cancel", ChatFormatting.RED,
                t("soundevent.hover.cancel")));
    }

    /**
     * 显示普通分类的分页声音按钮，音符盒分类改为显示乐器按钮。
     */
    public static void showCategory(String categoryKey, int page) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        SoundEventCatalog.Category category = SoundEventCatalog.getCategory(categoryKey);
        if (category == null) {
            showError("soundevent.error.invalid_category", categoryKey);
            showCategories();
            return;
        }

        if (SoundEventCatalog.CATEGORY_NOTE_BLOCK.equals(categoryKey)) {
            showInstrumentPage(category, page);
            return;
        }

        List<SoundEventSelection> sounds = category.sounds();
        int safePage = clampPage(page, sounds.size());
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sounds.size());
        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.list_title",
                t("soundevent.category." + categoryKey), safePage + 1, pageCount(sounds.size()))));
        for (int index = start; index < end; index++) {
            SoundEventSelection selection = sounds.get(index);
            client.player.sendSystemMessage(createSoundButton(selection));
        }
        showNavigation(categoryKey, safePage, pageCount(sounds.size()), false, null);
    }

    /**
     * 显示指定音符盒乐器的音阶分页按钮。
     */
    public static void showInstrument(String soundId, int page) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        SoundEventCatalog.Category category = SoundEventCatalog.getCategory(SoundEventCatalog.CATEGORY_NOTE_BLOCK);
        if (category == null) {
            showError("soundevent.error.invalid_category", SoundEventCatalog.CATEGORY_NOTE_BLOCK);
            return;
        }
        Optional<SoundEventCatalog.InstrumentGroup> instrument = category.instruments().stream()
                .filter(group -> group.soundId().equals(soundId))
                .findFirst();
        if (instrument.isEmpty()) {
            showError("soundevent.error.invalid_sound", soundId);
            showCategory(SoundEventCatalog.CATEGORY_NOTE_BLOCK, 0);
            return;
        }

        List<SoundEventSelection> notes = instrument.get().notes();
        int safePage = clampPage(page, notes.size());
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, notes.size());
        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.note_title",
                instrument.get().soundId(), safePage + 1, pageCount(notes.size()))));
        for (int index = start; index < end; index++) {
            client.player.sendSystemMessage(createSoundButton(notes.get(index)));
        }
        showNavigation(SoundEventCatalog.CATEGORY_NOTE_BLOCK, safePage, pageCount(notes.size()), true,
                instrument.get().soundId());
    }

    /**
     * 显示选择成功消息。
     */
    public static void showSelectionSuccess(SoundEventSelection selection) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(t("soundevent.message.selected",
                    SoundEventManager.getDisplayText(selection))));
        }
    }

    /**
     * 显示流程取消消息。
     */
    public static void showCancelled() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(t("soundevent.message.cancelled")));
        }
    }

    /**
     * 显示错误消息。
     */
    public static void showError(String key, Object... args) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(t(key, args)));
        }
    }

    private static void showInstrumentPage(SoundEventCatalog.Category category, int page) {
        Minecraft client = Minecraft.getInstance();
        List<SoundEventCatalog.InstrumentGroup> instruments = category.instruments();
        int safePage = clampPage(page, instruments.size());
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, instruments.size());
        client.player.sendSystemMessage(Component.literal(t("soundevent.chat.instrument_title",
                safePage + 1, pageCount(instruments.size()))));
        for (int index = start; index < end; index++) {
            SoundEventCatalog.InstrumentGroup instrument = instruments.get(index);
            client.player.sendSystemMessage(createButton(t("soundevent.button.instrument", instrument.soundId()),
                    "/areahint replacesoundevent instrument " + instrument.soundId() + " 0",
                    ChatFormatting.YELLOW, t("soundevent.hover.instrument", instrument.soundId())));
        }
        showNavigation(SoundEventCatalog.CATEGORY_NOTE_BLOCK, safePage, pageCount(instruments.size()), false, null);
    }

    private static MutableComponent createSoundButton(SoundEventSelection selection) {
        String label = selection.isNoteBlock()
                ? t("soundevent.button.note", selection.note(), formatPitch(selection.pitch()))
                : selection.soundId();
        // 指令参数使用 Float 的可逆字符串，避免聊天显示取整改变原版音符盒音高。
        String command = "/areahint replacesoundevent select " + selection.soundId() + " " + Float.toString(selection.pitch());
        String hover = selection.isNoteBlock()
                ? t("soundevent.hover.note", selection.soundId(), selection.note(), formatPitch(selection.pitch()))
                : t("soundevent.hover.sound", selection.soundId());
        return createButton("[" + label + "]", command, ChatFormatting.GREEN, hover);
    }

    private static void showNavigation(String categoryKey, int page, int pages,
                                       boolean notePage, String soundId) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        MutableComponent navigation = Component.empty();
        if (page > 0) {
            navigation.append(createButton(t("soundevent.button.previous"),
                    notePage
                            ? "/areahint replacesoundevent instrument " + soundId + " " + (page - 1)
                            : "/areahint replacesoundevent category " + categoryKey + " " + (page - 1),
                    ChatFormatting.LIGHT_PURPLE, t("soundevent.hover.previous")));
            navigation.append(Component.literal("  "));
        }
        if (page + 1 < pages) {
            navigation.append(createButton(t("soundevent.button.next"),
                    notePage
                            ? "/areahint replacesoundevent instrument " + soundId + " " + (page + 1)
                            : "/areahint replacesoundevent category " + categoryKey + " " + (page + 1),
                    ChatFormatting.LIGHT_PURPLE, t("soundevent.hover.next")));
            navigation.append(Component.literal("  "));
        }
        navigation.append(createButton(t("soundevent.button.back"),
                notePage
                        ? "/areahint replacesoundevent category " + categoryKey + " 0"
                        : "/areahint replacesoundevent",
                ChatFormatting.AQUA, t("soundevent.hover.back")));
        navigation.append(Component.literal("  "));
        navigation.append(createButton(t("soundevent.button.cancel"),
                "/areahint replacesoundevent cancel", ChatFormatting.RED, t("soundevent.hover.cancel")));
        client.player.sendSystemMessage(navigation);
    }

    private static MutableComponent createButton(String label, String command, ChatFormatting color, String hoverText) {
        return Component.literal(label).setStyle(Style.EMPTY
                .withClickEvent(TextEventCompat.runCommand(command))
                .withHoverEvent(TextEventCompat.showText(Component.literal(hoverText)))
                .withColor(color));
    }

    private static int clampPage(int page, int itemCount) {
        int pages = pageCount(itemCount);
        return Math.max(0, Math.min(page, pages - 1));
    }

    private static int pageCount(int itemCount) {
        return Math.max(1, (itemCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private static String formatPitch(float pitch) {
        return String.format(java.util.Locale.ROOT, "%.3f", pitch)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private static String t(String key, Object... args) {
        return I18nManager.translate(key, args);
    }
}
