package areahint.soundevent;

import areahint.AreashintClient;

import java.util.Optional;

/**
 * 域名切换音效的客户端动作入口。
 * <p>
 * 音效指令由服务端节点解析后通过 S2C_CLIENT_COMMAND 回传，客户端只负责本地配置读写和聊天界面展示，
 * 与音量、渲染模式、语言等客户端本地功能保持同一套“服务端解析、客户端执行”的模式。
 */
public final class SoundEventClientActions {
    private SoundEventClientActions() {
    }

    /**
     * 执行服务端回传的音效动作。
     *
     * @param action 去掉 areahint: 前缀后的动作字符串
     */
    public static void handleClientCommand(String action) {
        try {
            AreashintClient.LOGGER.info("处理音效命令: {}", action);
            if (action.equals("soundevent_open")) {
                SoundEventChatUI.showCategories();
            } else if (action.startsWith("soundevent_category:")) {
                // 动作格式为 soundevent_category:<页码>:<分类>，分类本身不含冒号
                String[] payload = splitPayload(action, "soundevent_category:");
                if (payload != null) {
                    SoundEventChatUI.showCategory(payload[1], Integer.parseInt(payload[0]));
                }
            } else if (action.startsWith("soundevent_instrument:")) {
                // 动作格式为 soundevent_instrument:<页码>:<声音ID>，声音ID可能自带命名空间冒号
                String[] payload = splitPayload(action, "soundevent_instrument:");
                if (payload != null) {
                    SoundEventChatUI.showInstrument(payload[1], Integer.parseInt(payload[0]));
                }
            } else if (action.startsWith("soundevent_select:")) {
                // 动作格式为 soundevent_select:<音高>:<声音ID>，字符串部分允许包含冒号
                String[] payload = splitPayload(action, "soundevent_select:");
                if (payload != null) {
                    applySelection(payload[1], Float.parseFloat(payload[0]));
                }
            } else if (action.equals("soundevent_none")) {
                applyNone();
            } else if (action.equals("soundevent_cancel")) {
                SoundEventChatUI.showCancelled();
            } else if (action.equals("soundlevel_open")) {
                SoundLevelChatUI.showMenu();
            } else if (action.startsWith("soundlevel_set:")) {
                applySoundLevel(action.substring("soundlevel_set:".length()));
            } else {
                AreashintClient.LOGGER.warn("未知的音效命令: {}", action);
            }
        } catch (Exception e) {
            AreashintClient.LOGGER.error("处理音效命令时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 按“数值:文本”的约定拆分动作参数，文本部分允许继续包含冒号。
     *
     * @param action 完整动作字符串
     * @param prefix 动作前缀
     * @return 长度为 2 的参数数组，格式不符时返回 null
     */
    private static String[] splitPayload(String action, String prefix) {
        String[] parts = action.substring(prefix.length()).split(":", 2);
        if (parts.length < 2) {
            AreashintClient.LOGGER.warn("音效命令缺少参数: {}", action);
            return null;
        }
        return parts;
    }

    /**
     * 应用声音选择并给出成功或错误提示。
     *
     * @param soundId 声音ID
     * @param pitch 音高
     */
    private static void applySelection(String soundId, float pitch) {
        Optional<SoundEventSelection> selection = SoundEventCatalog.createSelection(soundId, pitch);
        if (selection.isEmpty()) {
            SoundEventChatUI.showError("soundevent.error.invalid");
            return;
        }
        if (!SoundEventManager.applySelection(selection.get())) {
            SoundEventChatUI.showError("soundevent.error.invalid_sound", soundId);
            return;
        }
        SoundEventChatUI.showSelectionSuccess(selection.get());
    }

    /**
     * 关闭域名切换音效并给出提示。
     */
    private static void applyNone() {
        SoundEventSelection selection = SoundEventSelection.none();
        SoundEventManager.applySelection(selection);
        SoundEventChatUI.showSelectionSuccess(selection);
    }

    /**
     * 保存音量并给出成功提示。
     *
     * @param value 服务端回传的音量字符串
     */
    private static void applySoundLevel(String value) {
        float soundLevel = Float.parseFloat(value);
        if (SoundEventManager.applySoundLevel(soundLevel)) {
            SoundLevelChatUI.showSelectionSuccess(soundLevel);
        }
    }
}
