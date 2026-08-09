package areahint.soundevent;

import areahint.data.ConfigData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 图形声音选择流程控制器，统一配置草稿模式和立即保存模式。
 */
public final class SoundEventVisualController {
    private SoundEventVisualController() {
    }

    /**
     * 从指令可视化面板打开，选择后立即保存并返回原面板。
     */
    public static void openFromCommandUi(Screen parent) {
        open(parent, SoundEventManager::getCurrentSelection, selection -> {
            boolean applied = SoundEventManager.applySelection(selection);
            if (applied) {
                SoundEventChatUI.showSelectionSuccess(selection);
            }
            return applied;
        });
    }

    /**
     * 从配置面板打开，选择只写入草稿并返回配置面板。
     */
    public static void openForConfig(Screen parent, ConfigData draft, Runnable selectionChanged) {
        open(parent,
                () -> SoundEventCatalog.fromConfig(draft.getSoundEvent(), draft.getSoundPitch()),
                selection -> {
                    boolean applied = SoundEventManager.applyToDraft(draft, selection);
                    if (applied && selectionChanged != null) {
                        selectionChanged.run();
                    }
                    return applied;
                });
    }

    private static void open(Screen parent, Supplier<SoundEventSelection> currentSelection,
                             Predicate<SoundEventSelection> selectionAction) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        SelectionContext context = new SelectionContext(parent, currentSelection, selectionAction);
        client.setScreen(new SoundEventCategoryScreen(context));
    }

    /**
     * 在多层选择界面之间共享最终返回目标和选择动作。
     */
    static final class SelectionContext {
        private final Screen returnScreen;
        private final Supplier<SoundEventSelection> currentSelection;
        private final Predicate<SoundEventSelection> selectionAction;

        private SelectionContext(Screen returnScreen, Supplier<SoundEventSelection> currentSelection,
                                 Predicate<SoundEventSelection> selectionAction) {
            this.returnScreen = returnScreen;
            this.currentSelection = currentSelection;
            this.selectionAction = selectionAction;
        }

        SoundEventSelection currentSelection() {
            return this.currentSelection.get();
        }

        Screen returnScreen() {
            return this.returnScreen;
        }

        void select(SoundEventSelection selection) {
            if (this.selectionAction.test(selection)) {
                returnToOrigin();
            }
        }

        void returnToOrigin() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(this.returnScreen);
            }
        }
    }
}
