package areahint.soundevent;

import areahint.commandui.CommandUiScreen;
import areahint.config.ClientConfig;
import areahint.data.ConfigData;
import areahint.i18n.I18nManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Pattern;

/**
 * soundlevel 独立图形界面，在确认前仅维护音量草稿并校验精确输入。
 */
public final class SoundLevelScreen extends CommandUiScreen {
    private static final int CONTENT_MAX_WIDTH = 360;
    private static final int CONTENT_SIDE_MARGIN = 16;
    private static final int CONTROL_GAP = 6;
    private static final int PRESET_GAP = 4;
    private static final int INPUT_MAX_WIDTH = 70;
    private static final int FOOTER_BUTTON_WIDTH = 100;
    private static final float KEYBOARD_LEVEL_STEP = 0.01f;
    private static final float[] COMMON_LEVELS = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
    // 与配置面板保持一致，只把无符号且最多两位小数的范围内普通十进制保留为原编辑文本。
    private static final Pattern PLAIN_SOUND_LEVEL_PATTERN = Pattern.compile(
            "(?:0(?:\\.\\d{0,2})?|1(?:\\.0{0,2})?|\\.\\d{1,2})");

    private final float originalSoundLevel;
    private float draftSoundLevel;
    private String inputText;
    private boolean inputValid = true;
    private boolean syncingTextField;
    private int contentTop;
    private SoundLevelSlider soundLevelSlider;
    private TextFieldWidget soundLevelInput;
    private ButtonWidget confirmButton;

    public SoundLevelScreen(Screen parent) {
        super("commandui.soundlevel.title", parent);
        this.originalSoundLevel = ConfigData.normalizeSoundLevel(ClientConfig.getSoundLevel());
        this.draftSoundLevel = this.originalSoundLevel;
        this.inputText = ConfigData.formatSoundLevel(this.draftSoundLevel);
    }

    @Override
    protected void init() {
        int contentWidth = Math.max(1, Math.min(CONTENT_MAX_WIDTH, this.width - CONTENT_SIDE_MARGIN * 2));
        int contentX = (this.width - contentWidth) / 2;
        this.contentTop = calculateContentTop();

        int inputWidth = Math.min(INPUT_MAX_WIDTH, Math.max(1, contentWidth / 4));
        int sliderWidth = Math.max(1, contentWidth - inputWidth - CONTROL_GAP);
        int controlY = this.contentTop + 46;
        this.soundLevelSlider = new SoundLevelSlider(contentX, controlY, sliderWidth, BUTTON_HEIGHT);
        this.addDrawableChild(this.soundLevelSlider);

        this.soundLevelInput = new TextFieldWidget(this.textRenderer,
                contentX + sliderWidth + CONTROL_GAP, controlY, inputWidth, BUTTON_HEIGHT,
                Text.literal(translate("commandui.soundlevel.label", ConfigData.formatSoundLevel(this.draftSoundLevel))));
        this.soundLevelInput.setMaxLength(32);
        this.soundLevelInput.setText(this.inputText);
        this.soundLevelInput.setChangedListener(this::applySoundLevelInput);
        this.addDrawableChild(this.soundLevelInput);

        addPresetButtons(contentX, contentWidth, this.contentTop + 72);
        addFooterButtons(contentWidth);
        updateInputValidity(this.inputValid);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawCenteredTrimmed(context,
                translate("commandui.soundlevel.current", ConfigData.formatSoundLevel(this.originalSoundLevel)),
                this.contentTop, 0xAAAAAA);
        drawCenteredTrimmed(context,
                translate("commandui.soundlevel.label", ConfigData.formatSoundLevel(this.draftSoundLevel)),
                this.contentTop + 14, BRIGHT_GREEN);
        drawCenteredTrimmed(context,
                translate("commandui.soundlevel.range",
                        ConfigData.formatSoundLevel(ConfigData.SOUND_LEVEL_MIN),
                        ConfigData.formatSoundLevel(ConfigData.SOUND_LEVEL_MAX)),
                this.contentTop + 28, 0xAAAAAA);
        if (!this.inputValid) {
            drawCenteredTrimmed(context, translate("commandui.soundlevel.error.invalid"),
                    this.contentTop + 100, 0xFF5555);
        }
    }

    /**
     * 添加五个常用音量按钮，按钮宽度随当前窗口内容宽度平均分配。
     */
    private void addPresetButtons(int contentX, int contentWidth, int y) {
        int totalGap = PRESET_GAP * (COMMON_LEVELS.length - 1);
        int buttonWidth = Math.max(1, (contentWidth - totalGap) / COMMON_LEVELS.length);
        for (int index = 0; index < COMMON_LEVELS.length; index++) {
            float soundLevel = COMMON_LEVELS[index];
            String formattedLevel = ConfigData.formatSoundLevel(soundLevel);
            int x = contentX + index * (buttonWidth + PRESET_GAP);
            this.addDrawableChild(ButtonWidget.builder(
                            Text.literal(translate("soundlevel.button.level", formattedLevel)),
                            button -> setDraftSoundLevel(soundLevel))
                    .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
        }
    }

    /**
     * 添加确认和取消按钮，确认仅在精确输入为可保存值时启用。
     */
    private void addFooterButtons(int contentWidth) {
        int buttonWidth = Math.min(FOOTER_BUTTON_WIDTH, Math.max(1, (contentWidth - CONTROL_GAP) / 2));
        int totalWidth = buttonWidth * 2 + CONTROL_GAP;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.confirmButton = ButtonWidget.builder(Text.literal(t("commandui.button.confirm")), button -> confirm())
                .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build();
        this.addDrawableChild(this.confirmButton);
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> close())
                .dimensions(x + buttonWidth + CONTROL_GAP, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    /**
     * 解析精确输入并同步合法草稿，非法或未完成内容只保留在输入框中供玩家继续编辑。
     */
    private void applySoundLevelInput(String value) {
        if (this.syncingTextField) {
            return;
        }

        this.inputText = value;
        String trimmed = value.trim();
        if (isIncompleteInput(trimmed)) {
            updateInputValidity(false);
            return;
        }

        try {
            double parsedSoundLevel = Double.parseDouble(trimmed);
            if (!Double.isFinite(parsedSoundLevel)
                    || parsedSoundLevel < ConfigData.SOUND_LEVEL_MIN
                    || parsedSoundLevel > ConfigData.SOUND_LEVEL_MAX) {
                updateInputValidity(false);
                return;
            }

            this.draftSoundLevel = ConfigData.normalizeSoundLevel(parsedSoundLevel);
            if (this.soundLevelSlider != null) {
                this.soundLevelSlider.setSoundLevel(this.draftSoundLevel);
            }
            updateInputValidity(true);
            // 第三位小数、科学计数法、数值后缀等合法写法立即转成统一的普通十进制文本。
            if (!PLAIN_SOUND_LEVEL_PATTERN.matcher(value).matches()) {
                setTextFieldSilently(ConfigData.formatSoundLevel(this.draftSoundLevel));
            }
        } catch (NumberFormatException ignored) {
            // 小数点或指数等尚未完成的编辑状态继续保留，但确认按钮保持禁用。
            updateInputValidity(false);
        }
    }

    /**
     * 用滑条或常用按钮更新草稿，同时恢复精确输入为合法的统一文本。
     */
    private void setDraftSoundLevel(float soundLevel) {
        this.draftSoundLevel = ConfigData.normalizeSoundLevel(soundLevel);
        if (this.soundLevelSlider != null) {
            this.soundLevelSlider.setSoundLevel(this.draftSoundLevel);
        }
        setTextFieldSilently(ConfigData.formatSoundLevel(this.draftSoundLevel));
        updateInputValidity(true);
    }

    /**
     * 确认后复用声音管理器的唯一保存入口，成功提示与聊天指令保持一致。
     */
    private void confirm() {
        if (!this.inputValid) {
            return;
        }
        if (SoundEventManager.applySoundLevel(this.draftSoundLevel)) {
            SoundLevelChatUI.showSelectionSuccess(this.draftSoundLevel);
            close();
        }
    }

    /**
     * 静默同步输入框以避免 changed listener 在控件互相更新时递归触发。
     */
    private void setTextFieldSilently(String value) {
        this.inputText = value;
        if (this.soundLevelInput == null) {
            return;
        }
        this.syncingTextField = true;
        try {
            this.soundLevelInput.setText(value);
        } finally {
            this.syncingTextField = false;
        }
    }

    /**
     * 同步错误显示与确认按钮状态，保证无效文本永远不能写入配置。
     */
    private void updateInputValidity(boolean valid) {
        this.inputValid = valid;
        if (this.confirmButton != null) {
            this.confirmButton.active = valid;
        }
    }

    private int calculateContentTop() {
        // 矮窗口向上收拢内容，常规窗口则保持标题下方的稳定留白。
        return Math.max(32, Math.min(42, this.height - FOOTER_Y_OFFSET - 116));
    }

    private static boolean isIncompleteInput(String value) {
        return value.isEmpty() || ".".equals(value);
    }

    private double toSliderValue(float soundLevel) {
        float clamped = ConfigData.clampSoundLevel(soundLevel);
        return (clamped - ConfigData.SOUND_LEVEL_MIN)
                / (double) (ConfigData.SOUND_LEVEL_MAX - ConfigData.SOUND_LEVEL_MIN);
    }

    private void drawCenteredTrimmed(DrawContext context, String value, int y, int color) {
        String trimmed = this.textRenderer.trimToWidth(value, Math.max(1, this.width - CONTENT_SIDE_MARGIN * 2));
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(trimmed), this.width / 2, y, color);
    }

    private static String translate(String key, Object... args) {
        return I18nManager.translate(key, args);
    }

    /**
     * 0 到 1 的原版滑条值与配置音量范围之间按 0.01 精度互相转换。
     */
    private final class SoundLevelSlider extends SliderWidget {
        private SoundLevelSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Text.empty(), SoundLevelScreen.this.toSliderValue(draftSoundLevel));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.literal(ConfigData.formatSoundLevel(getSoundLevel())));
        }

        @Override
        protected void applyValue() {
            SoundLevelScreen.this.setDraftSoundLevel(getSoundLevel());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            float previousSoundLevel = getSoundLevel();
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled && (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT)) {
                // 原版滑条按像素计算键盘步长，宽控件会被 0.01 量化舍回原值，因此显式推进一个音量档。
                float direction = keyCode == GLFW.GLFW_KEY_LEFT ? -1.0f : 1.0f;
                SoundLevelScreen.this.setDraftSoundLevel(previousSoundLevel + direction * KEYBOARD_LEVEL_STEP);
            }
            return handled;
        }

        private float getSoundLevel() {
            double soundLevel = ConfigData.SOUND_LEVEL_MIN
                    + this.value * (ConfigData.SOUND_LEVEL_MAX - ConfigData.SOUND_LEVEL_MIN);
            return ConfigData.normalizeSoundLevel(soundLevel);
        }

        private void setSoundLevel(float soundLevel) {
            this.value = SoundLevelScreen.this.toSliderValue(ConfigData.normalizeSoundLevel(soundLevel));
            updateMessage();
        }
    }
}
