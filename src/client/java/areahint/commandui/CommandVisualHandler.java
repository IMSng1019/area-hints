package areahint.commandui;

import areahint.i18n.I18nManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 指令可视化入口，每个用户指令只需要实现打开逻辑即可接入主面板。
 */
public interface CommandVisualHandler {
    String id();

    String defaultCommand();

    String descriptionKey();

    String categoryKey();

    boolean hasVisualFlow();

    void open(Screen parent);

    default Text displayName() {
        String command = defaultCommand();
        if (command == null || command.isBlank()) {
            return Text.literal(I18nManager.translate("commandui.command." + id() + ".name"));
        }
        return Text.literal("/" + command);
    }

    default Text description() {
        return Text.literal(I18nManager.translate(descriptionKey()));
    }
}
