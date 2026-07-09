package areahint.commandui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用文本输入页，支持单输入框和多输入框表单。
 */
public class WizardTextInputScreen extends CommandWizardScreen {
    public record FieldSpec(String labelKey, String placeholderKey, String initialValue, int maxLength) {
    }

    private static final int INPUT_WIDTH = 220;
    private static final int LABEL_GAP = 14;
    private static final int FIELD_GAP = 38;

    private final List<FieldSpec> fieldSpecs;
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final String promptKey;
    private final String detailKey;
    private final String errorKey;
    private final Consumer<List<String>> submitAction;

    public WizardTextInputScreen(Screen parent, String titleKey, List<FieldSpec> fieldSpecs,
                                 String promptKey, String detailKey, String errorKey,
                                 Consumer<List<String>> submitAction, Runnable cancelAction) {
        super(titleKey, parent, cancelAction);
        this.fieldSpecs = fieldSpecs;
        this.promptKey = promptKey;
        this.detailKey = detailKey;
        this.errorKey = errorKey;
        this.submitAction = submitAction;
    }

    @Override
    protected void init() {
        this.fields.clear();
        int firstY = Math.max(54, this.height / 2 - this.fieldSpecs.size() * FIELD_GAP / 2 - 18);
        int x = (this.width - INPUT_WIDTH) / 2;

        for (int i = 0; i < this.fieldSpecs.size(); i++) {
            FieldSpec spec = this.fieldSpecs.get(i);
            TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, firstY + i * FIELD_GAP + LABEL_GAP,
                INPUT_WIDTH, BUTTON_HEIGHT, Text.empty());
            field.setMaxLength(Math.max(1, spec.maxLength()));
            field.setText(spec.initialValue() == null ? "" : spec.initialValue());
            if (spec.placeholderKey() != null && !spec.placeholderKey().isBlank()) {
                field.setPlaceholder(Text.literal(t(spec.placeholderKey())));
            }
            this.fields.add(field);
            this.addSelectableChild(field);
        }

        if (!this.fields.isEmpty()) {
            this.setFocused(this.fields.get(0));
            this.fields.get(0).setFocused(true);
        }

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int xButtons = (this.width - (buttonWidth * 2 + 4)) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.next")), button -> submit())
            .dimensions(xButtons, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .dimensions(xButtons + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        for (TextFieldWidget field : this.fields) {
            if (field.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (TextFieldWidget field : this.fields) {
            if (field.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int contentWidth = Math.min(360, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 14;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
        }
        if (this.errorKey != null && !this.errorKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.errorKey)), x, y + 18, contentWidth, 0xFF5555);
        }

        for (int i = 0; i < this.fields.size(); i++) {
            FieldSpec spec = this.fieldSpecs.get(i);
            TextFieldWidget field = this.fields.get(i);
            drawTrimmed(context, Text.literal(t(spec.labelKey())), field.getX(), field.getY() - LABEL_GAP,
                INPUT_WIDTH, BRIGHT_YELLOW);
            field.render(context, mouseX, mouseY, delta);
        }
    }

    private void submit() {
        List<String> values = new ArrayList<>();
        for (TextFieldWidget field : this.fields) {
            values.add(field.getText());
        }
        this.submitAction.accept(values);
    }
}
