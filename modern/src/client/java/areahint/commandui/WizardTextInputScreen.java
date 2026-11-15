package areahint.commandui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
    private final List<EditBox> fields = new ArrayList<>();
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
            EditBox field = new EditBox(this.font, x, firstY + i * FIELD_GAP + LABEL_GAP,
                INPUT_WIDTH, BUTTON_HEIGHT, Component.empty());
            field.setMaxLength(Math.max(1, spec.maxLength()));
            field.setValue(spec.initialValue() == null ? "" : spec.initialValue());
            if (spec.placeholderKey() != null && !spec.placeholderKey().isBlank()) {
                field.setHint(Component.literal(t(spec.placeholderKey())));
            }
            this.fields.add(field);
            this.addWidget(field);
        }

        if (!this.fields.isEmpty()) {
            this.setFocused(this.fields.get(0));
            this.fields.get(0).setFocused(true);
        }

        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int xButtons = (this.width - (buttonWidth * 2 + 4)) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.next")), button -> submit())
            .bounds(xButtons, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(xButtons + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    */
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        for (EditBox field : this.fields) {
            if (field.keyPressed(inputEvent)) {
            /*if (field.keyPressed(keyCode, scanCode, modifiers)) {
            */
                return true;
            }
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
        for (EditBox field : this.fields) {
            if (field.charTyped(inputEvent)) {
            /*if (field.charTyped(chr, modifiers)) {
            */
                return true;
            }
        }
        return super.charTyped(inputEvent);
        /*return super.charTyped(chr, modifiers);
        */
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int contentWidth = Math.min(360, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 14;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
        }
        if (this.errorKey != null && !this.errorKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.errorKey)), x, y + 18, contentWidth, 0xFF5555);
        }

        for (int i = 0; i < this.fields.size(); i++) {
            FieldSpec spec = this.fieldSpecs.get(i);
            EditBox field = this.fields.get(i);
            drawTrimmed(context, Component.literal(t(spec.labelKey())), field.getX(), field.getY() - LABEL_GAP,
                INPUT_WIDTH, BRIGHT_YELLOW);
            field.extractRenderState(context, mouseX, mouseY, delta);
        }
    }

    private void submit() {
        List<String> values = new ArrayList<>();
        for (EditBox field : this.fields) {
            values.add(field.getValue());
        }
        this.submitAction.accept(values);
    }
}
