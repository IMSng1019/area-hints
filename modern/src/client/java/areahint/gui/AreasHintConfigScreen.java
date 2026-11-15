package areahint.gui;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import areahint.render.VulkanModCompat;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AreasHintConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int COMPACT_BUTTON_WIDTH = 96;
    private static final int INPUT_WIDTH = 58;
    private static final int INPUT_GAP = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BOTTOM_BUTTON_WIDTH = 74;
    private static final int LIST_SCROLLBAR_GAP = 10;
    private static final int FREQUENCY_MIN = 1;
    private static final int FREQUENCY_MAX = 60;
    // 仅保留无符号、无额外前导零且最多两位小数的普通十进制输入状态。
    private static final Pattern PLAIN_SOUND_LEVEL_PATTERN = Pattern.compile(
            "(?:0(?:\\.\\d{0,2})?|1(?:\\.0{0,2})?|\\.\\d{1,2})");
    private static final String[] RENDER_MODES = {"CPU", "OpenGL", "Vulkan"};
    private static final String[] STYLE_MODES = {"full", "simple", "mixed"};
    private static final String[] SIZE_MODES = {"extra_large", "large", "medium_large", "medium", "medium_small", "small", "extra_small"};
    private static final String[] SUBTITLE_SIZE_MODES = {"auto", "extra_large", "large", "medium_large", "medium", "medium_small", "small", "extra_small"};
    private static final String[] TELEPORT_FORMATS = {"tp", "minecraft:tp", "teleport", "minecraft:teleport"};

    private final Screen parent;
    private ConfigData draft;
    private ConfigListWidget list;
    private boolean listeningForKey;
    private boolean frequencyUsesCustomInput;
    private boolean syncingTextField;
    private FrequencySlider frequencySlider;
    private EditBox frequencyInput;
    private SoundLevelSlider soundLevelSlider;
    private EditBox soundLevelInput;
    private Button titleSizeButton;
    private EditBox titleSizeInput;
    private Button subtitleSizeButton;
    private EditBox subtitleSizeInput;

    public AreasHintConfigScreen(Screen parent) {
        super(Component.literal(t("screen.areahint.config.title")));
        this.parent = parent;
        this.draft = ClientConfig.copy();
    }

    @Override
    protected void init() {
        this.listeningForKey = false;
        this.list = new ConfigListWidget(this.minecraft, this.width, this.height, 32, this.height - 32);
        this.addRenderableWidget(this.list);
        rebuildList();
        addFooterButtons();
    }

    private void rebuildList() {
        this.list.clearConfigEntries();
        this.list.addGroup("screen.areahint.config.group.general");
        this.list.addButton("screen.areahint.config.enabled", onOffButton(draft.isEnabled(), button -> {
            draft.setEnabled(!draft.isEnabled());
            button.setMessage(onOffText(draft.isEnabled()));
        }));
        this.frequencySlider = new FrequencySlider(0, 0, COMPACT_BUTTON_WIDTH, BUTTON_HEIGHT);
        this.frequencyInput = createFrequencyInput();
        this.list.addButton("screen.areahint.config.frequency", List.of(this.frequencySlider, this.frequencyInput));

        this.list.addGroup("screen.areahint.config.group.sound");
        // 重建控件前先把草稿量化到界面的 0.01 精度，避免显示值与最终保存值不一致。
        this.draft.setSoundLevel(ConfigData.normalizeSoundLevel(this.draft.getSoundLevel()));
        this.list.addButton("screen.areahint.config.sound_event", cycleButton(soundEventText(), button ->
                areahint.soundevent.SoundEventVisualController.openForConfig(
                        this, this.draft, () -> button.setMessage(soundEventText()))));
        this.soundLevelSlider = new SoundLevelSlider(0, 0, COMPACT_BUTTON_WIDTH, BUTTON_HEIGHT);
        this.soundLevelInput = createSoundLevelInput();
        this.list.addButton("screen.areahint.config.sound_level", List.of(this.soundLevelSlider, this.soundLevelInput));

        this.list.addGroup("screen.areahint.config.group.title");
        this.list.addButton("screen.areahint.config.hint_render", cycleButton(renderText(draft.getHintRender()), button -> {
            draft.setHintRender(nextRenderMode(draft.getHintRender()));
            button.setMessage(renderText(draft.getHintRender()));
        }));
        this.list.addButton("screen.areahint.config.title_style", cycleButton(styleText(draft.getTitleStyle()), button -> {
            draft.setTitleStyle(nextValue(draft.getTitleStyle(), STYLE_MODES));
            button.setMessage(styleText(draft.getTitleStyle()));
        }));
        this.titleSizeButton = cycleButton(sizeText(draft.getTitleSize()), button -> {
            draft.setTitleSize(nextValue(draft.getTitleSize(), SIZE_MODES));
            button.setMessage(sizeText(draft.getTitleSize()));
            if (titleSizeInput != null) {
                setTextFieldSilently(titleSizeInput, getTitleSizeInputText());
            }
            if ("auto".equals(draft.getSubtitleSize()) && subtitleSizeInput != null) {
                setTextFieldSilently(subtitleSizeInput, getSubtitleSizeInputText());
            }
        });
        this.titleSizeInput = createSizeInput(false);
        this.list.addButton("screen.areahint.config.title_size", List.of(this.titleSizeButton, this.titleSizeInput));
        this.subtitleSizeButton = cycleButton(subtitleSizeText(draft.getSubtitleSize()), button -> {
            draft.setSubtitleSize(nextValue(draft.getSubtitleSize(), SUBTITLE_SIZE_MODES));
            button.setMessage(subtitleSizeText(draft.getSubtitleSize()));
            if (subtitleSizeInput != null) {
                setTextFieldSilently(subtitleSizeInput, getSubtitleSizeInputText());
            }
        });
        this.subtitleSizeInput = createSizeInput(true);
        this.list.addButton("screen.areahint.config.subtitle_size", List.of(this.subtitleSizeButton, this.subtitleSizeInput));

        this.list.addGroup("screen.areahint.config.group.input");
        this.list.addButton("screen.areahint.config.record_key", cycleButton(recordKeyText(), button -> {
            listeningForKey = true;
            button.setMessage(Component.literal(t("screen.areahint.config.record_key.listening")));
        }));

        this.list.addGroup("screen.areahint.config.group.language");
        this.list.addButton("screen.areahint.config.language", cycleButton(languageText(draft.getLanguage()), button -> {
            List<String> languages = getLanguages();
            if (!languages.isEmpty()) {
                String current = draft.getLanguage();
                int index = languages.indexOf(current);
                draft.setLanguage(languages.get((index + 1 + languages.size()) % languages.size()));
                button.setMessage(languageText(draft.getLanguage()));
            }
        }));
        this.list.addButton("screen.areahint.config.language_locked", onOffButton(draft.isLanguageLocked(), button -> {
            draft.setLanguageLocked(!draft.isLanguageLocked());
            button.setMessage(onOffText(draft.isLanguageLocked()));
        }));

        this.list.addGroup("screen.areahint.config.group.advanced");
        this.list.addButton("screen.areahint.config.bound_viz", onOffButton(draft.isBoundVizEnabled(), button -> {
            draft.setBoundVizEnabled(!draft.isBoundVizEnabled());
            button.setMessage(onOffText(draft.isBoundVizEnabled()));
        }));
        this.list.addButton("screen.areahint.config.xaero_minimap_overlay", onOffButton(draft.isXaeroMinimapOverlayEnabled(), button -> {
            draft.setXaeroMinimapOverlayEnabled(!draft.isXaeroMinimapOverlayEnabled());
            button.setMessage(onOffText(draft.isXaeroMinimapOverlayEnabled()));
        }));
        this.list.addButton("screen.areahint.config.xaero_world_map_overlay", onOffButton(draft.isXaeroWorldMapOverlayEnabled(), button -> {
            draft.setXaeroWorldMapOverlayEnabled(!draft.isXaeroWorldMapOverlayEnabled());
            button.setMessage(onOffText(draft.isXaeroWorldMapOverlayEnabled()));
        }));
        this.list.addButton("screen.areahint.config.teleport_format", cycleButton(Component.literal(draft.getTeleportFormat()), button -> {
            draft.setTeleportFormat(nextValue(draft.getTeleportFormat(), TELEPORT_FORMATS));
            button.setMessage(Component.literal(draft.getTeleportFormat()));
        }));
    }

    private void addFooterButtons() {
        int y = this.height - 26;
        int totalWidth = BOTTOM_BUTTON_WIDTH * 3 + 8;
        int x = (this.width - totalWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("screen.areahint.config.reset")), button -> {
            this.draft = new ConfigData();
            this.frequencyUsesCustomInput = false;
            this.rebuildList();
        }).bounds(x, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("screen.areahint.config.cancel")), button -> this.onClose()).bounds(x + BOTTOM_BUTTON_WIDTH + 4, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("screen.areahint.config.done")), button -> {
            applyDraft();
            this.onClose();
        }).bounds(x + (BOTTOM_BUTTON_WIDTH + 4) * 2, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void applyDraft() {
        boolean recordKeyChanged = ClientConfig.getRecordKey() != draft.getRecordKey();
        AreashintClient.applyClientConfig(draft, recordKeyChanged);
        this.draft = ClientConfig.copy();
        this.listeningForKey = false;
        this.rebuildWidgets();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    */
        if (listeningForKey) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_UNKNOWN && keyCode > 0) {
                draft.setRecordKey(keyCode);
            }
            listeningForKey = false;
            rebuildList();
            return true;
        }
        if (parent instanceof areahint.commandui.CommandUiScreen
                && areahint.keyhandler.UnifiedKeyHandler.matchesRecordKey(keyCode, scanCode)) {
            // 从指令可视化进入设置时，绑定键关闭界面并丢弃未应用的草稿。
            areahint.keyhandler.UnifiedKeyHandler.suppressRecordKeyUntilRelease();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }
        if (list != null && list.keyPressed(inputEvent)) {
        /*if (list != null && list.keyPressed(keyCode, scanCode, modifiers)) {
        */
            return true;
        }
        return super.keyPressed(inputEvent);
        /*return super.keyPressed(keyCode, scanCode, modifiers);
        */
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent inputEvent) {
        int chr = inputEvent.codepoint();
    /*public boolean charTyped(char chr, int modifiers) {
    */
        if (list != null && list.charTyped(inputEvent)) {
        /*if (list != null && list.charTyped(chr, modifiers)) {
        */
            return true;
        }
        return super.charTyped(inputEvent);
        /*return super.charTyped(chr, modifiers);
        */
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 1.20.2 起 renderBackground 改为 4 参、1 参重载被移除，故按版本分支
        this.extractBackground(context, mouseX, mouseY, delta);
        /*this.extractBackground(context);*/
        context.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private Button cycleButton(Component message, Button.OnPress action) {
        return Button.builder(message, action).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    private Button onOffButton(boolean value, Button.OnPress action) {
        return cycleButton(onOffText(value), action);
    }

    private EditBox createFrequencyInput() {
        EditBox input = new EditBox(this.font, 0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Component.empty());
        input.setMaxLength(5);
        input.setValue(formatFrequency(draft.getFrequency()));
        input.setHint(Component.literal(t("screen.areahint.config.custom.placeholder")));
        input.setResponder(value -> applyFrequencyInput(input, value));
        return input;
    }

    private EditBox createSoundLevelInput() {
        EditBox input = new EditBox(this.font, 0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Component.empty());
        input.setMaxLength(5);
        input.setValue(ConfigData.formatSoundLevel(draft.getSoundLevel()));
        input.setHint(Component.literal(t("screen.areahint.config.custom.placeholder")));
        input.setResponder(value -> applySoundLevelInput(input, value));
        return input;
    }

    private EditBox createSizeInput(boolean subtitle) {
        EditBox input = new EditBox(this.font, 0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Component.empty());
        input.setMaxLength(5);
        input.setValue(subtitle ? getSubtitleSizeInputText() : getTitleSizeInputText());
        input.setHint(Component.literal(t("screen.areahint.config.custom.placeholder")));
        input.setResponder(value -> applySizeInput(input, value, subtitle));
        return input;
    }

    private void applyFrequencyInput(EditBox input, String value) {
        if (syncingTextField) {
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            double parsedFrequency = Double.parseDouble(trimmed);
            if (!Double.isFinite(parsedFrequency)) {
                return;
            }
            double frequency = Math.max(FREQUENCY_MIN, Math.min(FREQUENCY_MAX, parsedFrequency));
            draft.setFrequency(frequency);
            frequencyUsesCustomInput = true;
            if (frequencySlider != null) {
                frequencySlider.setFrequency(frequency);
            }
            String formattedFrequency = formatFrequency(frequency);
            if (!trimmed.endsWith(".") && !formattedFrequency.equals(trimmed)) {
                setTextFieldSilently(input, formattedFrequency);
            }
        } catch (NumberFormatException ignored) {
            // 允许用户临时输入小数点等未完成内容，等内容成为合法数字后再同步草稿。
        }
    }

    private void applySoundLevelInput(EditBox input, String value) {
        if (syncingTextField) {
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            double parsedSoundLevel = Double.parseDouble(trimmed);
            if (!Double.isFinite(parsedSoundLevel)
                    || parsedSoundLevel < ConfigData.SOUND_LEVEL_MIN
                    || parsedSoundLevel > ConfigData.SOUND_LEVEL_MAX) {
                // 非有限值和越界值仅保留在输入框供继续修改，不能钳制后写入配置草稿。
                return;
            }
            // 输入先按滑条的 0.01 精度规范化，确保草稿、滑条与文本显示始终对应同一个值。
            float soundLevel = ConfigData.normalizeSoundLevel(parsedSoundLevel);
            draft.setSoundLevel(soundLevel);
            if (soundLevelSlider != null) {
                soundLevelSlider.setSoundLevel(soundLevel);
            }
            String formattedSoundLevel = ConfigData.formatSoundLevel(soundLevel);
            // 普通十进制输入态暂时保留，其余合法写法立即同步为统一的 0.01 精度文本。
            if (!isPlainSoundLevelInput(value)) {
                setTextFieldSilently(input, formattedSoundLevel);
            }
        } catch (NumberFormatException ignored) {
            // 允许用户临时输入小数点等未完成内容，等内容成为合法数字后再同步草稿。
        }
    }

    private void applySizeInput(EditBox input, String value, boolean subtitle) {
        if (syncingTextField) {
            return;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        try {
            float scale = Float.parseFloat(trimmed);
            if (!Float.isFinite(scale) || scale < ConfigData.CUSTOM_SIZE_MIN || scale > ConfigData.CUSTOM_SIZE_MAX) {
                return;
            }
            String customSize = ConfigData.formatCustomSize(scale);
            if (subtitle) {
                draft.setSubtitleSize(customSize);
                if (subtitleSizeButton != null) {
                    subtitleSizeButton.setMessage(customText());
                }
            } else {
                draft.setTitleSize(customSize);
                if (titleSizeButton != null) {
                    titleSizeButton.setMessage(customText());
                }
                if ("auto".equals(draft.getSubtitleSize()) && subtitleSizeInput != null) {
                    setTextFieldSilently(subtitleSizeInput, getSubtitleSizeInputText());
                }
            }
        } catch (NumberFormatException ignored) {
            // 允许用户临时输入小数点等未完成内容，等内容成为合法数字后再同步草稿。
        }
    }

    private String getTitleSizeInputText() {
        return getSizeInputText(draft.getTitleSize());
    }

    private String getSubtitleSizeInputText() {
        if ("auto".equals(draft.getSubtitleSize())) {
            return ConfigData.formatCustomSize(getAutoSubtitleScale()).substring("custom:".length());
        }
        return getSizeInputText(draft.getSubtitleSize());
    }

    private String getSizeInputText(String size) {
        Float scale = ConfigData.getCustomSizeScale(size);
        if (scale != null) {
            return ConfigData.formatCustomSize(scale).substring("custom:".length());
        }

        Float presetScale = ConfigData.getPresetSizeScale(size);
        if (presetScale == null) {
            return "";
        }
        return ConfigData.formatCustomSize(presetScale).substring("custom:".length());
    }

    private float getAutoSubtitleScale() {
        return Math.max(ConfigData.CUSTOM_SIZE_MIN, ConfigData.getSizeScale(draft.getTitleSize()) * 0.8f);
    }

    private void setTextFieldSilently(EditBox input, String value) {
        syncingTextField = true;
        input.setValue(value);
        syncingTextField = false;
    }

    private String formatFrequency(double frequency) {
        return ConfigData.formatFrequency(frequency);
    }

    private Component onOffText(boolean value) {
        return Component.literal(value ? t("screen.areahint.config.value.on") : t("screen.areahint.config.value.off"));
    }

    private Component renderText(String value) {
        if ("Vulkan".equals(value) && !VulkanModCompat.isLoaded()) {
            return Component.literal(value + " (" + t("screen.areahint.config.vulkan_unavailable") + ")");
        }
        return Component.literal(value);
    }

    private Component styleText(String value) {
        return Component.literal(t("screen.areahint.config.value.style." + value));
    }

    private Component sizeText(String value) {
        Float customScale = ConfigData.getCustomSizeScale(value);
        if (customScale != null) {
            return customText();
        }
        return Component.literal(t("screen.areahint.config.value.size." + value));
    }

    private Component subtitleSizeText(String value) {
        Float customScale = ConfigData.getCustomSizeScale(value);
        if (customScale != null) {
            return customText();
        }
        if (!"auto".equals(value)) {
            return sizeText(value);
        }
        return Component.literal(t("screen.areahint.config.value.subtitle_size." + value));
    }

    private Component customText() {
        return Component.literal(t("screen.areahint.config.value.custom"));
    }

    private Component languageText(String language) {
        return Component.literal(I18nManager.getLanguageDisplayName(language) + " (" + language + ")");
    }

    private Component recordKeyText() {
        return InputConstants.Type.KEYSYM.getOrCreate(draft.getRecordKey()).getDisplayName();
    }

    private Component soundEventText() {
        areahint.soundevent.SoundEventSelection selection = areahint.soundevent.SoundEventCatalog.fromConfig(
                draft.getSoundEvent(), draft.getSoundPitch());
        return Component.literal(areahint.soundevent.SoundEventManager.getDisplayText(selection));
    }

    private String nextRenderMode(String current) {
        String next = current;
        do {
            next = nextValue(next, RENDER_MODES);
        } while ("Vulkan".equals(next) && !VulkanModCompat.isLoaded());
        return next;
    }

    private String nextValue(String current, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private List<String> getLanguages() {
        List<String> languages = new ArrayList<>(I18nManager.getAvailableLanguages());
        Collections.sort(languages);
        if (languages.isEmpty()) {
            languages.add(draft.getLanguage());
        }
        return languages;
    }

    private static String t(String key) {
        return I18nManager.translate(key);
    }

    private class FrequencySlider extends AbstractSliderButton {
        FrequencySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), AreasHintConfigScreen.this.toSliderValue(draft.getFrequency()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String valueText = String.valueOf(getFrequencyValue());
            if (frequencyUsesCustomInput) {
                valueText = t("screen.areahint.config.value.custom");
            }
            this.setMessage(Component.literal(valueText));
        }

        @Override
        protected void applyValue() {
            frequencyUsesCustomInput = false;
            draft.setFrequency(getFrequencyValue());
            if (frequencyInput != null) {
                setTextFieldSilently(frequencyInput, formatFrequency(draft.getFrequency()));
            }
            updateMessage();
        }

        private int getFrequencyValue() {
            return FREQUENCY_MIN + (int) Math.round(this.value * (FREQUENCY_MAX - FREQUENCY_MIN));
        }

        private void setFrequency(double frequency) {
            this.value = AreasHintConfigScreen.this.toSliderValue(frequency);
            updateMessage();
        }

    }

    private double toSliderValue(double frequency) {
        double clamped = Math.max(FREQUENCY_MIN, Math.min(FREQUENCY_MAX, frequency));
        return (clamped - FREQUENCY_MIN) / (double) (FREQUENCY_MAX - FREQUENCY_MIN);
    }

    private static boolean isPlainSoundLevelInput(String value) {
        return PLAIN_SOUND_LEVEL_PATTERN.matcher(value).matches();
    }

    private double toSoundLevelSliderValue(float soundLevel) {
        float clamped = ConfigData.clampSoundLevel(soundLevel);
        return (clamped - ConfigData.SOUND_LEVEL_MIN)
                / (double) (ConfigData.SOUND_LEVEL_MAX - ConfigData.SOUND_LEVEL_MIN);
    }

    private class SoundLevelSlider extends AbstractSliderButton {
        SoundLevelSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                    AreasHintConfigScreen.this.toSoundLevelSliderValue(draft.getSoundLevel()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(ConfigData.formatSoundLevel(getSoundLevelValue())));
        }

        @Override
        protected void applyValue() {
            float soundLevel = getSoundLevelValue();
            this.value = AreasHintConfigScreen.this.toSoundLevelSliderValue(soundLevel);
            // 拖动仅同步配置草稿和输入框，试听与持久化仍由原有交互和完成按钮负责。
            draft.setSoundLevel(soundLevel);
            if (soundLevelInput != null) {
                setTextFieldSilently(soundLevelInput, ConfigData.formatSoundLevel(soundLevel));
            }
            updateMessage();
        }

        private float getSoundLevelValue() {
            double soundLevel = ConfigData.SOUND_LEVEL_MIN
                    + this.value * (ConfigData.SOUND_LEVEL_MAX - ConfigData.SOUND_LEVEL_MIN);
            return ConfigData.normalizeSoundLevel(soundLevel);
        }

        private void setSoundLevel(float soundLevel) {
            this.value = AreasHintConfigScreen.this.toSoundLevelSliderValue(
                    ConfigData.normalizeSoundLevel(soundLevel));
            updateMessage();
        }
    }

    private class ConfigListWidget extends CompatEntryListWidget<ConfigListWidget.Entry> {
        private AbstractWidget focusedConfigWidget;
        private EditBox focusedTextField;

        ConfigListWidget(Minecraft client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 24);
            // 1.20.5 起列表控件不再自绘背景、setRenderBackground 被移除，故只在 1.20.4 及以前调用
            /*this.setRenderBackground(false);
            */
        }

        void addGroup(String key) {
            this.addEntry(new GroupEntry(key));
        }

        void addButton(String key, AbstractWidget widget) {
            this.addEntry(new ButtonEntry(key, List.of(widget)));
        }

        void addButton(String key, List<AbstractWidget> widgets) {
            this.addEntry(new ButtonEntry(key, widgets));
        }

        void clearConfigEntries() {
            super.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(420, AreasHintConfigScreen.this.width - 40);
        }

        @Override
        // 1.20.5 起 getScrollbarPositionX 改名为 getScrollbarX，方法体共用
        /*protected int getScrollbarPositionX() {
        */
        protected int scrollBarX() {
            // 原版列表会按默认宽度计算滚动条位置，宽屏下会压到右侧配置按钮中间。
            return Math.min(this.width - 6, this.getRowRight() + LIST_SCROLLBAR_GAP);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent clickEvent, boolean doubled) {
            double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
        /*public boolean mouseClicked(double mouseX, double mouseY, int button) {
        */
            AbstractWidget widget = findConfigWidgetAt(mouseX, mouseY);
            if (widget != null && widget.mouseClicked(clickEvent, doubled)) {
            /*if (widget != null && widget.mouseClicked(mouseX, mouseY, button)) {
            */
                // 原版列表的行命中只知道“点中了某一行”，这里再明确把点击交给行内按钮或滑块。
                this.focusedConfigWidget = widget;
                setFocusedTextField(widget instanceof EditBox ? (EditBox) widget : null);
                return true;
            }

            setFocusedTextField(null);
            return super.mouseClicked(clickEvent, doubled);
            /*return super.mouseClicked(mouseX, mouseY, button);
            */
        }

        @Override
        public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent clickEvent) {
            double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
        /*public boolean mouseReleased(double mouseX, double mouseY, int button) {
        */
            if (this.focusedConfigWidget != null) {
                AbstractWidget widget = this.focusedConfigWidget;
                this.focusedConfigWidget = null;
                if (widget.mouseReleased(clickEvent)) {
                /*if (widget.mouseReleased(mouseX, mouseY, button)) {
                */
                    return true;
                }
            }
            return super.mouseReleased(clickEvent);
            /*return super.mouseReleased(mouseX, mouseY, button);
            */
        }

        @Override
        public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent clickEvent, double deltaX, double deltaY) {
            double mouseX = clickEvent.x(), mouseY = clickEvent.y(); int button = clickEvent.button();
        /*public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        */
            if (this.focusedConfigWidget != null
                    && this.focusedConfigWidget.mouseDragged(clickEvent, deltaX, deltaY)) {
                    /*&& this.focusedConfigWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    */
                return true;
            }
            return super.mouseDragged(clickEvent, deltaX, deltaY);
            /*return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            */
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
            int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
        /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        */
            if (this.focusedTextField != null && this.focusedTextField.keyPressed(inputEvent)) {
            /*if (this.focusedTextField != null && this.focusedTextField.keyPressed(keyCode, scanCode, modifiers)) {
            */
                return true;
            }
            return super.keyPressed(inputEvent);
            /*return super.keyPressed(keyCode, scanCode, modifiers);
            */
        }

        @Override
        public boolean charTyped(net.minecraft.client.input.CharacterEvent inputEvent) {
            int chr = inputEvent.codepoint();
        /*public boolean charTyped(char chr, int modifiers) {
        */
            if (this.focusedTextField != null && this.focusedTextField.charTyped(inputEvent)) {
            /*if (this.focusedTextField != null && this.focusedTextField.charTyped(chr, modifiers)) {
            */
                return true;
            }
            return super.charTyped(inputEvent);
            /*return super.charTyped(chr, modifiers);
            */
        }

        private AbstractWidget findConfigWidgetAt(double mouseX, double mouseY) {
            for (areahint.gui.AreasHintConfigScreen.ConfigListWidget.Entry entry : this.children()) {
                for (AbstractWidget widget : entry.getConfigWidgets()) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        return widget;
                    }
                }
            }

            return null;
        }

        private void setFocusedTextField(EditBox textField) {
            if (this.focusedTextField != null && this.focusedTextField != textField) {
                this.focusedTextField.setFocused(false);
            }
            this.focusedTextField = textField;
            if (this.focusedTextField != null) {
                this.focusedTextField.setFocused(true);
            }
            AreasHintConfigScreen.this.setFocused(this.focusedTextField);
        }

        abstract class Entry extends ContainerObjectSelectionList.Entry<areahint.gui.AreasHintConfigScreen.ConfigListWidget.Entry> {
            List<AbstractWidget> getConfigWidgets() {
                return List.of();
            }
        }

        private class GroupEntry extends areahint.gui.AreasHintConfigScreen.ConfigListWidget.Entry {
            private final String key;

            private GroupEntry(String key) {
                this.key = key;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                context.text(AreasHintConfigScreen.this.font, Component.literal(t(key)), x + 4, y + 7, 0xFFFFFF55);
            }

            @Override
            public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return List.of();
            }

            @Override
            public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
                return List.of();
            }
        }

        private class ButtonEntry extends areahint.gui.AreasHintConfigScreen.ConfigListWidget.Entry {
            private final String key;
            private final List<AbstractWidget> widgets;

            private ButtonEntry(String key, List<AbstractWidget> widgets) {
                this.key = key;
                this.widgets = widgets;
            }

            @Override
            public void extractContent(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int x = getContentX(), y = getContentY(), entryWidth = getWidth(), entryHeight = getContentHeight();
            /*public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            */
                context.text(AreasHintConfigScreen.this.font, Component.literal(t(key)), x + 4, y + 6, 0xFFFFFFFF);
                int widgetX = x + entryWidth - 4;
                for (int i = this.widgets.size() - 1; i >= 0; i--) {
                    AbstractWidget widget = this.widgets.get(i);
                    widgetX -= widget.getWidth();
                    widget.setPosition(widgetX, y + 2);
                    widget.extractRenderState(context, mouseX, mouseY, tickDelta);
                    widgetX -= INPUT_GAP;
                }
            }

            @Override
            public List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
                return this.widgets;
            }

            @Override
            public List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
                return this.widgets;
            }

            @Override
            List<AbstractWidget> getConfigWidgets() {
                return this.widgets;
            }
        }
    }
}
