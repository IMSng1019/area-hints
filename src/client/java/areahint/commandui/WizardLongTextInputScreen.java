package areahint.commandui;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

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
    private final SelectionManager selectionManager;
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
        this.selectionManager = new SelectionManager(
            () -> this.text,
            value -> this.text = value == null ? "" : value,
            SelectionManager.makeClipboardGetter(MinecraftClient.getInstance()),
            SelectionManager.makeClipboardSetter(MinecraftClient.getInstance()),
            value -> value != null && value.length() <= MAX_LENGTH
        );
    }

    @Override
    protected void init() {
        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 8;
        int x = (this.width - totalWidth) / 2;
        int y = this.height - FOOTER_Y_OFFSET;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.execute")), button -> submit())
            .dimensions(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .dimensions(x + buttonWidth + 8, y, buttonWidth, BUTTON_HEIGHT).build());
        this.selectionManager.putCursorAtEnd();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Screen.isSelectAll(keyCode)) {
            this.selectionManager.selectAll();
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            this.selectionManager.copy();
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            this.selectionManager.paste();
            return true;
        }
        if (Screen.isCut(keyCode)) {
            this.selectionManager.cut();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (Screen.hasShiftDown()) {
                this.selectionManager.insert("\n");
            } else {
                submit();
            }
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> this.selectionManager.delete(-1);
            case GLFW.GLFW_KEY_DELETE -> this.selectionManager.delete(1);
            case GLFW.GLFW_KEY_RIGHT -> this.selectionManager.moveCursor(1, Screen.hasShiftDown());
            case GLFW.GLFW_KEY_LEFT -> this.selectionManager.moveCursor(-1, Screen.hasShiftDown());
            case GLFW.GLFW_KEY_HOME -> this.selectionManager.moveCursorToStart(Screen.hasShiftDown());
            case GLFW.GLFW_KEY_END -> this.selectionManager.moveCursorToEnd(Screen.hasShiftDown());
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (SharedConstants.isValidChar(chr)) {
            this.selectionManager.insert(Character.toString(chr));
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(520, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        if (this.promptKey != null && !this.promptKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.promptKey)), x, y, contentWidth, 0xFFFFFF);
            y += 13;
        }
        if (this.detailKey != null && !this.detailKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.detailKey)), x, y, contentWidth, 0xAAAAAA);
            y += 13;
        }
        if (this.errorKey != null && !this.errorKey.isBlank()) {
            drawTrimmed(context, Text.literal(t(this.errorKey)), x, y, contentWidth, 0xFF5555);
        }

        int boxY = 76;
        int boxHeight = Math.max(70, this.height - 118);
        context.fill(x - 2, boxY - 2, x + contentWidth + 2, boxY + boxHeight + 2, 0x99202020);
        context.drawBorder(x - 2, boxY - 2, contentWidth + 4, boxHeight + 4, 0xCCAAAAAA);
        context.drawTextWrapped(this.textRenderer, Text.literal(this.text), x + 4, boxY + 4, contentWidth - 8, 0xFFFFFF);
    }

    private void submit() {
        if (this.submitAction != null) {
            markFlowHandled();
            if (this.client != null) {
                this.client.setScreen(null);
            }
            this.submitAction.accept(this.text);
        }
    }
}
