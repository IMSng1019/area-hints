package areahint.util;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

// 统一聊天按钮的事件构建，1.21.5 起使用具体记录类型，命令字符串和悬停文本保持调用方原样。
public final class TextEventCompat {
    private TextEventCompat() {
    }

    // 点击执行命令；旧版 Action 构造器与新版 RunCommand 的行为一致。
    public static ClickEvent runCommand(String command) {
        /*return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        */
        return new ClickEvent.RunCommand(command);
    }

    // 点击仅填入命令，不改变原来需要玩家确认发送的交互。
    public static ClickEvent suggestCommand(String command) {
        /*return new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
        */
        return new ClickEvent.SuggestCommand(command);
    }

    // 悬停展示原始文本，继续保留现有翻译、格式和拼接内容。
    public static HoverEvent showText(Component text) {
        /*return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
        */
        return new HoverEvent.ShowText(text);
    }
}
