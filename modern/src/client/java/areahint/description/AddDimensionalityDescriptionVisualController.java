package areahint.description;

import areahint.commandui.CommandUiActions;
import areahint.commandui.CommandWizardScreen;
import areahint.commandui.WizardSelectionListScreen;
import areahint.i18n.I18nManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * adddimensionalitydescription 图形流程控制器，只把维度选择转换为现有描述 Manager 流程。
 */
public final class AddDimensionalityDescriptionVisualController {
    private static final int VISUAL_START_TIMEOUT_TICKS = 100;
    private static Screen parentScreen;
    private static boolean startingFromVisualCommand;
    private static int visualStartTicksRemaining;
    private static boolean registered;

    private AddDimensionalityDescriptionVisualController() {
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
        CommandUiActions.runCommandAndClose(parent, "areahint adddimensionalitydescription");
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

    public static void showLoading() {
        setScreen(new MessageScreen(parentScreen,
            "commandui.adddimensionalitydescription.title",
            I18nManager.translate("description.manager.loading.targets"),
            List.of(I18nManager.translate("description.ui.loading",
                I18nManager.translate("description.ui.target.dimension"))),
            DescriptionManager.getInstance()::cancel));
    }

    public static void showSelection(List<DescriptionListEntry> entries) {
        List<WizardSelectionListScreen.SelectionItem<DescriptionListEntry>> items = new ArrayList<>();
        for (DescriptionListEntry entry : entries) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(entry, title(entry), detail(entry)));
        }
        setScreen(new WizardSelectionListScreen<>(parentScreen,
            "commandui.adddimensionalitydescription.title",
            "description.ui.select.prompt",
            items,
            entry -> DescriptionManager.getInstance().selectVisualTarget(entry.id()),
            DescriptionManager.getInstance()::cancel));
    }

    public static void showOpeningBook(DescriptionListEntry entry) {
        setScreen(new MessageScreen(parentScreen,
            "commandui.adddimensionalitydescription.title",
            I18nManager.translate("description.ui.selected", title(entry)),
            List.of(I18nManager.translate("description.ui.book.instruction")),
            DescriptionManager.getInstance()::cancel));
    }

    public static void closeToGame() {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(null);
        }
    }

    private static String title(DescriptionListEntry entry) {
        if (entry.displayName() != null && !entry.displayName().trim().isEmpty()) {
            return entry.displayName();
        }
        return nullToText(entry.id());
    }

    private static String detail(DescriptionListEntry entry) {
        return I18nManager.translate("description.ui.hover.dimension") + nullToText(entry.id()) + "  "
            + I18nManager.translate("description.ui.hover.displayname") + title(entry);
    }

    private static String nullToText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("description.ui.none") : value;
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.setScreen(screen);
        }
    }

    /**
     * 简单等待页，避免网络列表返回前玩家只看到聊天提示。
     */
    private static final class MessageScreen extends CommandWizardScreen {
        private final String prompt;
        private final List<String> details;

        private MessageScreen(Screen parent, String titleKey, String prompt, List<String> details, Runnable cancelAction) {
            super(titleKey, parent, cancelAction);
            this.prompt = prompt;
            this.details = details == null ? List.of() : details;
        }

        @Override
        protected void init() {
            int buttonWidth = 90;
            int x = (this.width - buttonWidth) / 2;
            int y = this.height - FOOTER_Y_OFFSET;
            this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
                .bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
            super.extractRenderState(context, mouseX, mouseY, delta);
            int contentWidth = Math.min(430, this.width - 40);
            int x = (this.width - contentWidth) / 2;
            int y = Math.max(44, this.height / 2 - 26);
            if (this.prompt != null && !this.prompt.isBlank()) {
                drawTrimmed(context, Component.literal(this.prompt), x, y, contentWidth, 0xFFFFFF);
                y += 18;
            }
            for (String detail : this.details) {
                drawTrimmed(context, Component.literal(detail), x, y, contentWidth, 0xAAAAAA);
                y += 13;
            }
        }
    }
}
