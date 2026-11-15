package areahint.replacebutton;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandWizardScreen;
import areahint.commandui.WizardConfirmScreen;
import areahint.config.ClientConfig;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * replacebutton 图形流程控制器，只负责把 Screen 输入转交给现有 replacebutton 子命令和 Manager。
 */
public final class ReplaceButtonVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private ReplaceButtonVisualController() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!startingFromVisualCommand) {
                return;
            }
            visualStartTicksRemaining--;
            if (visualStartTicksRemaining <= 0) {
                clear();
            }
        });
        registered = true;
    }

    public static void openFromCommandUi(Screen parent) {
        parentScreen = parent;
        startingFromVisualCommand = true;
        visualStartTicksRemaining = VISUAL_START_TIMEOUT_TICKS;
        CommandUiActions.runCommandAndClose(parent, "areahint replacebutton");
    }

    public static boolean consumeVisualStartRequest() {
        boolean requested = startingFromVisualCommand;
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        return requested;
    }

    public static void clear() {
        startingFromVisualCommand = false;
        visualStartTicksRemaining = 0;
        parentScreen = null;
    }

    public static void showWaitingForKeyScreen(String errorText) {
        setScreen(new WaitingForKeyScreen(parentScreen, errorText));
    }

    public static void showConfirmScreen(String keyName) {
        setScreen(new WizardConfirmScreen(parentScreen,
            "commandui.replacebutton.title",
            I18nManager.translate("commandui.replacebutton.confirm", keyName),
            List.of(I18nManager.translate("commandui.replacebutton.confirm.detail")),
            "commandui.button.confirm",
            () -> CommandUiActions.runCommand("areahint replacebutton confirm"),
            () -> CommandUiActions.runCommand("areahint replacebutton cancel")));
    }

    private static void cancelWithCommand() {
        closeToGame();
        CommandUiActions.runCommand("areahint replacebutton cancel");
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    private static void closeToGame() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    /**
     * 等待按键页直接捕获键盘事件，避免打开 Screen 时原 tick 监听器无法收到按键。
     */
    private static final class WaitingForKeyScreen extends CommandWizardScreen {
        private final String errorText;

        private WaitingForKeyScreen(Screen parent, String errorText) {
            super("commandui.replacebutton.title", parent, ReplaceButtonVisualController::cancelWithCommand);
            this.errorText = errorText;
        }

        @Override
        protected void init() {
            int buttonWidth = 90;
            int x = (this.width - buttonWidth) / 2;
            int y = this.height - FOOTER_Y_OFFSET;
            this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")),
                    button -> cancelAndCloseToGame())
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        }

        @Override
        public boolean keyPressed(net.minecraft.client.input.KeyEvent inputEvent) {
            int keyCode = inputEvent.key(), scanCode = inputEvent.scancode(), modifiers = inputEvent.modifiers();
        /*public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        */
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelAndCloseToGame();
                return true;
            }

            ReplaceButtonManager.getInstance().handleKeyPress(keyCode, ReplaceButtonKeyListener.getKeyName(keyCode));
            return true;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            int contentWidth = Math.min(430, this.width - 40);
            int x = (this.width - contentWidth) / 2;
            int y = 42;

            drawTrimmed(context, Component.literal(t("commandui.replacebutton.waiting.prompt")),
                x, y, contentWidth, 0xFFFFFF);
            y += 18;
            drawTrimmed(context, Component.literal(I18nManager.translate("commandui.replacebutton.current",
                    ReplaceButtonKeyListener.getKeyName(ClientConfig.getRecordKey()))),
                x, y, contentWidth, 0xAAAAAA);
            y += 14;
            drawTrimmed(context, Component.literal(t("commandui.replacebutton.waiting.detail")),
                x, y, contentWidth, 0xAAAAAA);
            y += 14;
            drawTrimmed(context, Component.literal(t("commandui.replacebutton.waiting.tip")),
                x, y, contentWidth, 0x777777);

            if (errorText != null && !errorText.isBlank()) {
                y += 20;
                drawTrimmed(context, Component.literal(errorText), x, y, contentWidth, 0xFF5555);
            }
        }
    }
}
