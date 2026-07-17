package areahint.gui;

import areahint.AreashintClient;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import areahint.render.VulkanModCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private TextFieldWidget frequencyInput;
    private ButtonWidget titleSizeButton;
    private TextFieldWidget titleSizeInput;
    private ButtonWidget subtitleSizeButton;
    private TextFieldWidget subtitleSizeInput;

    public AreasHintConfigScreen(Screen parent) {
        super(Text.literal(t("screen.areahint.config.title")));
        this.parent = parent;
        this.draft = ClientConfig.copy();
    }

    @Override
    protected void init() {
        this.listeningForKey = false;
        this.list = new ConfigListWidget(this.client, this.width, this.height, 32, this.height - 32);
        this.addDrawableChild(this.list);
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
            button.setMessage(Text.literal(t("screen.areahint.config.record_key.listening")));
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
        this.list.addButton("screen.areahint.config.teleport_format", cycleButton(Text.literal(draft.getTeleportFormat()), button -> {
            draft.setTeleportFormat(nextValue(draft.getTeleportFormat(), TELEPORT_FORMATS));
            button.setMessage(Text.literal(draft.getTeleportFormat()));
        }));
    }

    private void addFooterButtons() {
        int y = this.height - 26;
        int totalWidth = BOTTOM_BUTTON_WIDTH * 3 + 8;
        int x = (this.width - totalWidth) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("screen.areahint.config.reset")), button -> {
            this.draft = new ConfigData();
            this.frequencyUsesCustomInput = false;
            this.rebuildList();
        }).dimensions(x, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("screen.areahint.config.cancel")), button -> this.close()).dimensions(x + BOTTOM_BUTTON_WIDTH + 4, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("screen.areahint.config.done")), button -> {
            applyDraft();
            this.close();
        }).dimensions(x + (BOTTOM_BUTTON_WIDTH + 4) * 2, y, BOTTOM_BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void applyDraft() {
        boolean recordKeyChanged = ClientConfig.getRecordKey() != draft.getRecordKey();
        AreashintClient.applyClientConfig(draft, recordKeyChanged);
        this.draft = ClientConfig.copy();
        this.listeningForKey = false;
        this.clearAndInit();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
            if (this.client != null) {
                this.client.setScreen(null);
            }
            return true;
        }
        if (list != null && list.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (list != null && list.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    private ButtonWidget cycleButton(Text message, ButtonWidget.PressAction action) {
        return ButtonWidget.builder(message, action).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
    }

    private ButtonWidget onOffButton(boolean value, ButtonWidget.PressAction action) {
        return cycleButton(onOffText(value), action);
    }

    private TextFieldWidget createFrequencyInput() {
        TextFieldWidget input = new TextFieldWidget(this.textRenderer, 0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Text.empty());
        input.setMaxLength(5);
        input.setText(formatFrequency(draft.getFrequency()));
        input.setPlaceholder(Text.literal(t("screen.areahint.config.custom.placeholder")));
        input.setChangedListener(value -> applyFrequencyInput(input, value));
        return input;
    }

    private TextFieldWidget createSizeInput(boolean subtitle) {
        TextFieldWidget input = new TextFieldWidget(this.textRenderer, 0, 0, INPUT_WIDTH, BUTTON_HEIGHT, Text.empty());
        input.setMaxLength(5);
        input.setText(subtitle ? getSubtitleSizeInputText() : getTitleSizeInputText());
        input.setPlaceholder(Text.literal(t("screen.areahint.config.custom.placeholder")));
        input.setChangedListener(value -> applySizeInput(input, value, subtitle));
        return input;
    }

    private void applyFrequencyInput(TextFieldWidget input, String value) {
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

    private void applySizeInput(TextFieldWidget input, String value, boolean subtitle) {
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

    private void setTextFieldSilently(TextFieldWidget input, String value) {
        syncingTextField = true;
        input.setText(value);
        syncingTextField = false;
    }

    private String formatFrequency(double frequency) {
        return ConfigData.formatFrequency(frequency);
    }

    private Text onOffText(boolean value) {
        return Text.literal(value ? t("screen.areahint.config.value.on") : t("screen.areahint.config.value.off"));
    }

    private Text renderText(String value) {
        if ("Vulkan".equals(value) && !VulkanModCompat.isLoaded()) {
            return Text.literal(value + " (" + t("screen.areahint.config.vulkan_unavailable") + ")");
        }
        return Text.literal(value);
    }

    private Text styleText(String value) {
        return Text.literal(t("screen.areahint.config.value.style." + value));
    }

    private Text sizeText(String value) {
        Float customScale = ConfigData.getCustomSizeScale(value);
        if (customScale != null) {
            return customText();
        }
        return Text.literal(t("screen.areahint.config.value.size." + value));
    }

    private Text subtitleSizeText(String value) {
        Float customScale = ConfigData.getCustomSizeScale(value);
        if (customScale != null) {
            return customText();
        }
        if (!"auto".equals(value)) {
            return sizeText(value);
        }
        return Text.literal(t("screen.areahint.config.value.subtitle_size." + value));
    }

    private Text customText() {
        return Text.literal(t("screen.areahint.config.value.custom"));
    }

    private Text languageText(String language) {
        return Text.literal(I18nManager.getLanguageDisplayName(language) + " (" + language + ")");
    }

    private Text recordKeyText() {
        return InputUtil.Type.KEYSYM.createFromCode(draft.getRecordKey()).getLocalizedText();
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

    private class FrequencySlider extends SliderWidget {
        FrequencySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), AreasHintConfigScreen.this.toSliderValue(draft.getFrequency()));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String valueText = String.valueOf(getFrequencyValue());
            if (frequencyUsesCustomInput) {
                valueText = t("screen.areahint.config.value.custom");
            }
            this.setMessage(Text.literal(valueText));
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

    private class ConfigListWidget extends ElementListWidget<ConfigListWidget.Entry> {
        private ClickableWidget focusedConfigWidget;
        private TextFieldWidget focusedTextField;

        ConfigListWidget(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 24);
            this.setRenderBackground(false);
        }

        void addGroup(String key) {
            this.addEntry(new GroupEntry(key));
        }

        void addButton(String key, ClickableWidget widget) {
            this.addEntry(new ButtonEntry(key, List.of(widget)));
        }

        void addButton(String key, List<ClickableWidget> widgets) {
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
        protected int getScrollbarPositionX() {
            // 原版列表会按默认宽度计算滚动条位置，宽屏下会压到右侧配置按钮中间。
            return Math.min(this.width - 6, this.getRowRight() + LIST_SCROLLBAR_GAP);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            ClickableWidget widget = findConfigWidgetAt(mouseX, mouseY);
            if (widget != null && widget.mouseClicked(mouseX, mouseY, button)) {
                // 原版列表的行命中只知道“点中了某一行”，这里再明确把点击交给行内按钮或滑块。
                this.focusedConfigWidget = widget;
                setFocusedTextField(widget instanceof TextFieldWidget ? (TextFieldWidget) widget : null);
                return true;
            }

            setFocusedTextField(null);
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.focusedConfigWidget != null) {
                ClickableWidget widget = this.focusedConfigWidget;
                this.focusedConfigWidget = null;
                if (widget.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }

            return super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (this.focusedConfigWidget != null
                    && this.focusedConfigWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }

            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.focusedTextField != null && this.focusedTextField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            if (this.focusedTextField != null && this.focusedTextField.charTyped(chr, modifiers)) {
                return true;
            }
            return super.charTyped(chr, modifiers);
        }

        private ClickableWidget findConfigWidgetAt(double mouseX, double mouseY) {
            for (Entry entry : this.children()) {
                for (ClickableWidget widget : entry.getConfigWidgets()) {
                    if (widget.isMouseOver(mouseX, mouseY)) {
                        return widget;
                    }
                }
            }

            return null;
        }

        private void setFocusedTextField(TextFieldWidget textField) {
            if (this.focusedTextField != null && this.focusedTextField != textField) {
                this.focusedTextField.setFocused(false);
            }
            this.focusedTextField = textField;
            if (this.focusedTextField != null) {
                this.focusedTextField.setFocused(true);
            }
            AreasHintConfigScreen.this.setFocused(this.focusedTextField);
        }

        abstract class Entry extends ElementListWidget.Entry<Entry> {
            List<ClickableWidget> getConfigWidgets() {
                return List.of();
            }
        }

        private class GroupEntry extends Entry {
            private final String key;

            private GroupEntry(String key) {
                this.key = key;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(AreasHintConfigScreen.this.textRenderer, Text.literal(t(key)), x + 4, y + 7, 0xFFFF55);
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() {
                return List.of();
            }

            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
                return List.of();
            }
        }

        private class ButtonEntry extends Entry {
            private final String key;
            private final List<ClickableWidget> widgets;

            private ButtonEntry(String key, List<ClickableWidget> widgets) {
                this.key = key;
                this.widgets = widgets;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(AreasHintConfigScreen.this.textRenderer, Text.literal(t(key)), x + 4, y + 6, 0xFFFFFF);
                int widgetX = x + entryWidth - 4;
                for (int i = this.widgets.size() - 1; i >= 0; i--) {
                    ClickableWidget widget = this.widgets.get(i);
                    widgetX -= widget.getWidth();
                    widget.setPosition(widgetX, y + 2);
                    widget.render(context, mouseX, mouseY, tickDelta);
                    widgetX -= INPUT_GAP;
                }
            }

            @Override
            public List<? extends net.minecraft.client.gui.Element> children() {
                return this.widgets;
            }

            @Override
            public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
                return this.widgets;
            }

            @Override
            List<ClickableWidget> getConfigWidgets() {
                return this.widgets;
            }
        }
    }
}
