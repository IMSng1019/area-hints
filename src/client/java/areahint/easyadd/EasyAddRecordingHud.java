package areahint.easyadd;

import areahint.i18n.I18nManager;
import areahint.keyhandler.UnifiedKeyHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

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
        HudRenderCallback.EVENT.register(EasyAddRecordingHud::render);
        registered = true;
    }

    private static void render(DrawContext context, float tickDelta) {
        EasyAddManager manager = EasyAddManager.getInstance();
        if (!manager.isVisualMode()
                || manager.getCurrentState() != EasyAddManager.EasyAddState.RECORDING_POINTS) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
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

        int width = context.getScaledWindowWidth();
        int panelWidth = Math.min(340, width - 20);
        int panelHeight = 76;
        int x = (width - panelWidth) / 2;
        int y = 18;
        context.fill(x, y, x + panelWidth, y + panelHeight, 0x77000000);
        context.drawBorder(x, y, panelWidth, panelHeight, 0x88FFFFFF);

        context.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal(I18nManager.translate("commandui.easyadd.record.title")), width / 2, y + 7, 0xFFFFFF);
        context.drawTextWithShadow(client.textRenderer,
            Text.literal(I18nManager.translate("commandui.easyadd.record.count", points.size())), x + 10, y + 22, 0xFFFF00);
        context.drawTextWithShadow(client.textRenderer, Text.literal(lastText), x + 10, y + 34, 0xAAAAAA);
        context.drawTextWithShadow(client.textRenderer,
            Text.literal(trim(client, I18nManager.translate("commandui.easyadd.record.action_hint", keyName), panelWidth - 20)),
            x + 10, y + 46, 0x00FF00);
        context.drawTextWithShadow(client.textRenderer,
            Text.literal(trim(client, I18nManager.translate(points.size() >= 3
                ? "commandui.easyadd.record.ready"
                : "commandui.easyadd.record.need_more"), panelWidth - 20)), x + 10, y + 58, 0x55FFFF);
    }

    private static String trim(MinecraftClient client, String value, int maxWidth) {
        return client.textRenderer.trimToWidth(value, Math.max(0, maxWidth));
    }
}
