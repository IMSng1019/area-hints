package areahint.commandui;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import areahint.util.GuiInputCompat;

import java.util.function.Consumer;

/**
 * 通用长文本输入页，主要用于 JSON 或说明文本的粘贴录入。
 */
public class WizardLongTextInputScreen extends CommandWizardScreen {
    private static final int MAX_LENGTH = 30000;
    private final String promptKey;
    private final String detailKey;
    private final String errorKey;
    private final Consumer<String> submitAction;
    private final TextFieldHelper selectionManager;
    private String text;

    public WizardLongTextInputScreen(Screen parent, String titleKey, String promptKey, String detailKey,
                                     String initialValue, String errorKey, Consumer<String> submitAction,
                                     Runnable cancelAction) {
        super(titleKey, parent, cancelAction);
        this.promptKey = promptKey;
        this.detailKey = detailKey;
        this.text = initialValue == null ? "" : initialValue;
        this.errorKey = errorKey;
        this.submitAction = submitAction;
        this.selectionManager = new TextFieldHelper(
            () -> this.text,
            value -> this.text = value == null ? "" : value,
            TextFieldHelper.createClipboardGetter(Minecraft.getInstance()),
            TextFieldHelper.createClipboardSetter(Minecraft.getInstance()),
            value -> value != null && value.length() <= MAX_LENGTH
        );
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.execute")), button -> submit())
            .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
        this.selectionManager.setCursorToEnd();
        int textWidth = Math.min(520, this.width - 40);
        int textLeft = (this.width - textWidth) / 2;
        areahint.util.TextInputCompat.area(textLeft + 4, 80, textLeft + textWidth - 4, 76 + Math.max(70, this.height - 118) - 4);
        areahint.util.TextInputCompat.start(this);
    }

    @Override
    public void removed() {
        areahint.util.TextInputCompat.stop(this);
        super.removed();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = areahint.util.PlatformInputCompat.secondaryCode(inputEvent), modifiers = inputEvent.modifiers();
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    */
        if (GuiInputCompat.isSelectAll(keyCode, scanCode, modifiers)) {
            this.selectionManager.selectAll();
            return true;
        }
        if (GuiInputCompat.isCopy(keyCode, scanCode, modifiers)) {
            this.selectionManager.copy();
            return true;
        }
        if (GuiInputCompat.isPaste(keyCode, scanCode, modifiers)) {
            this.selectionManager.paste();
            return true;
        }
        if (GuiInputCompat.isCut(keyCode, scanCode, modifiers)) {
            this.selectionManager.cut();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (GuiInputCompat.hasShift(modifiers)) {
                this.selectionManager.insertText("\n");
            } else {
                submit();
            }
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_BACKSPACE -> this.selectionManager.removeCharsFromCursor(-1);
            case InputConstants.KEY_DELETE -> this.selectionManager.removeCharsFromCursor(1);
            case InputConstants.KEY_RIGHT -> this.selectionManager.moveByChars(1, GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_LEFT -> this.selectionManager.moveByChars(-1, GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_HOME -> this.selectionManager.setCursorToStart(GuiInputCompat.hasShift(modifiers));
            case InputConstants.KEY_END -> this.selectionManager.setCursorToEnd(GuiInputCompat.hasShift(modifiers));
            default -> {
                return super.keyPressed(inputEvent);
                /*return super.keyPressed(keyCode, scanCode, modifiers);
                */
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent inputEvent) {
        int chr = inputEvent.codepoint();
    /*public boolean charTyped(char chr, int modifiers) {
    */
        if (GuiInputCompat.isValidChar(chr)) {
            this.selectionManager.insertText(Character.toString(chr));
            return true;
        }
        return super.charTyped(inputEvent);
        /*return super.charTyped(chr, modifiers);
        */
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(520, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 13;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
            y += 13;
        }
        if (this.errorKey != null && !this.errorKey.isBlank()) {
            drawTrimmed(context, Component.literal(t(this.errorKey)), x, y, contentWidth, 0xFF5555);
        }

        int boxY = 76;
        int boxHeight = Math.max(70, this.height - 118);
        areahint.util.TextInputCompat.area(x + 4, boxY + 4, x + contentWidth - 4, boxY + boxHeight - 4);
        context.fill(x - 2, boxY - 2, x + contentWidth + 2, boxY + boxHeight + 2, 0x99202020);
        areahint.util.GuiDrawCompat.drawBorder(context, x - 2, boxY - 2, contentWidth + 4, boxHeight + 4, 0xCCAAAAAA);
        // 1.21.2 起 drawTextWrapped 改名为 drawWrappedText 并新增阴影参数（原方法无阴影，传 false）
        /*context.drawTextWrapped(this.textRenderer, Text.literal(this.text), x + 4, boxY + 4, contentWidth - 8, 0xFFFFFFFF);
        */
        context.textWithWordWrap(this.font, Component.literal(this.text), x + 4, boxY + 4, contentWidth - 8, 0xFFFFFFFF, false);
    }

    private void submit() {
        if (this.submitAction != null) {
            markFlowHandled();
            if (this.minecraft != null) {
                areahint.util.ClientScreenCompat.setScreen(this.minecraft, null);
            }
            this.submitAction.accept(this.text);
        }
    }
}
