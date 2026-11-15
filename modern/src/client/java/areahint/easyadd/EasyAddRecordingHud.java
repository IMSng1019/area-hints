package areahint.easyadd;

import areahint.i18n.I18nManager;
import areahint.keyhandler.UnifiedKeyHandler;
import areahint.render.HudRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * EasyAdd 图形录点阶段的 HUD，保持普通 Screen 关闭以允许玩家移动。
 */
public final class EasyAddRecordingHud {
    private static boolean registered;

    private EasyAddRecordingHud() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        HudRegistration.register("easyadd_recording", EasyAddRecordingHud::render);
        registered = true;
    }

    // 录点面板只提取当前帧 GUI 状态，独立标识避免与三种域名标题渲染器冲突。
    private static void render(GuiGraphicsExtractor context, net.minecraft.client.DeltaTracker tickCounter) {
    /*private static void render(DrawContext context, float tickDelta) {
    */
        EasyAddManager manager = EasyAddManager.getInstance();
        if (!manager.isVisualMode()
                || manager.getCurrentState() != EasyAddManager.EasyAddState.RECORDING_POINTS) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        List<BlockPos> points = manager.getRecordedPoints();
        BlockPos lastPos = points.isEmpty() ? null : points.get(points.size() - 1);
        String keyName = UnifiedKeyHandler.getRecordKeyDisplayName();
        String lastText = lastPos == null
            ? I18nManager.translate("commandui.easyadd.record.last.none")
            : I18nManager.translate("commandui.easyadd.record.last",
                lastPos.getX(), lastPos.getY(), lastPos.getZ());

        int width = context.guiWidth();
        int panelWidth = Math.min(340, width - 20);
        int panelHeight = 76;
        int x = (width - panelWidth) / 2;
        int y = 18;
        context.fill(x, y, x + panelWidth, y + panelHeight, 0x77000000);
        areahint.util.GuiDrawCompat.drawBorder(context, x, y, panelWidth, panelHeight, 0x88FFFFFF);

        context.centeredText(client.font,
            Component.literal(I18nManager.translate("commandui.easyadd.record.title")), width / 2, y + 7, 0xFFFFFFFF);
        context.text(client.font,
            Component.literal(I18nManager.translate("commandui.easyadd.record.count", points.size())), x + 10, y + 22, 0xFFFFFF00);
        context.text(client.font, Component.literal(lastText), x + 10, y + 34, 0xFFAAAAAA);
        context.text(client.font,
            Component.literal(trim(client, I18nManager.translate("commandui.easyadd.record.action_hint", keyName), panelWidth - 20)),
            x + 10, y + 46, 0xFF00FF00);
        context.text(client.font,
            Component.literal(trim(client, I18nManager.translate(points.size() >= 3
                ? "commandui.easyadd.record.ready"
                : "commandui.easyadd.record.need_more"), panelWidth - 20)), x + 10, y + 58, 0xFF55FFFF);
    }

    private static String trim(Minecraft client, String value, int maxWidth) {
        return client.font.plainSubstrByWidth(value, Math.max(0, maxWidth));
    }
}
