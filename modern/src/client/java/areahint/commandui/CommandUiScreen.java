package areahint.commandui;

import areahint.i18n.I18nManager;
import areahint.keyhandler.UnifiedKeyHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 指令可视化界面基类，统一返回、关闭和绑定键关闭行为。
 */
public abstract class CommandUiScreen extends Screen {
    protected static final int BUTTON_HEIGHT = 20;
    protected static final int FOOTER_Y_OFFSET = 26;
    protected static final int BRIGHT_YELLOW = 0xFFFFFF00;
    protected static final int BRIGHT_GREEN = 0xFF00FF00;
    private static final int IN_GAME_BACKGROUND = 0x55000000;
    private static final int TOOLTIP_MAX_WIDTH = 260;
    protected final Screen parent;

    protected CommandUiScreen(String titleKey, Screen parent) {
        super(Component.literal(t(titleKey)));
        this.parent = parent;
    }

    public static boolean closeIfCommandUiOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.screen instanceof CommandUiScreen screen) {
            screen.closeToGameFromBoundKey();
            return true;
        }
        return false;
    }

    protected static String t(String key) {
        return I18nManager.translate(key);
    }

    protected void onDiscard() {
        // 子界面可在这里丢弃自己的临时流程状态。
    }

    protected void closeToGameFromBoundKey() {
        onDiscard();
        UnifiedKeyHandler.suppressRecordKeyUntilRelease();
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
    /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    */
        if (UnifiedKeyHandler.matchesRecordKey(keyCode, scanCode)) {
            if (UnifiedKeyHandler.isRecordKeySuppressedUntilRelease()) {
                return true;
            }
            closeToGameFromBoundKey();
            return true;
        }
        return super.keyPressed(inputEvent);
        /*return super.keyPressed(keyCode, scanCode, modifiers);
        */
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent inputEvent) {
        int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
    /*public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
    */
        if (UnifiedKeyHandler.releaseSuppressedRecordKeyIfMatches(keyCode, scanCode)) {
            return true;
        }
        return super.keyReleased(inputEvent);
        /*return super.keyReleased(keyCode, scanCode, modifiers);
        */
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        renderSoftBackground(context, mouseX, mouseY, delta);
        context.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    protected void drawTrimmed(GuiGraphicsExtractor context, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        String trimmed = this.font.plainSubstrByWidth(value, Math.max(0, maxWidth));
        // 向导历史调用传入 RGB，补全不透明 alpha；显式 ARGB 保留原透明度，兼容新版文字渲染。
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;
        context.text(this.font, Component.literal(trimmed), x, y, color);
    }

    protected void drawWrappedTooltip(GuiGraphicsExtractor context, Component text, int mouseX, int mouseY) {
        int maxWidth = Math.max(80, Math.min(TOOLTIP_MAX_WIDTH, this.width - 32));
        context.setTooltipForNextFrame(this.font, this.font.split(text, maxWidth), mouseX, mouseY);
    }

    private void renderSoftBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (this.minecraft != null && this.minecraft.level != null) {
            // 降低图形面板遮罩的不透明度，保留游戏画面作为背景参照。
            context.fill(0, 0, this.width, this.height, IN_GAME_BACKGROUND);
            return;
        }
        // 1.20.2 起 renderBackground 改为 4 参、1 参重载被移除，故按版本分支
        this.extractBackground(context, mouseX, mouseY, delta);
        /*this.extractBackground(context);*/
    }
}
