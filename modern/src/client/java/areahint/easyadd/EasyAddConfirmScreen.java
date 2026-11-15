package areahint.easyadd;

import areahint.commandui.CommandWizardScreen;
import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * EasyAdd 图形确认页，展示即将保存的域名 JSON 关键字段。
 */
public class EasyAddConfirmScreen extends CommandWizardScreen {
    private final AreaData areaData;
    private final List<String> summaryLines;

    public EasyAddConfirmScreen(Screen parent, AreaData areaData) {
        super("commandui.easyadd.confirm.title", parent, () -> EasyAddManager.getInstance().cancelEasyAdd());
        this.areaData = areaData;
        this.summaryLines = buildSummary(areaData);
    }

    @Override
    protected void init() {
        int y = this.height - FOOTER_Y_OFFSET;
        int buttonWidth = 90;
        int totalWidth = buttonWidth * 2 + 4;
        int x = (this.width - totalWidth) / 2;
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.save")), button -> {
            // 发送失败时保留确认页和取消回调，允许玩家重试保存或主动放弃本次创建。
            if (!EasyAddManager.getInstance().confirmSave()) {
                return;
            }
            markFlowHandled();
            if (this.minecraft != null) {
                areahint.util.ClientScreenCompat.setScreen(this.minecraft, null);
            }
        }).bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal(t("commandui.button.cancel")), button -> cancelAndCloseToGame())
            .bounds(x + buttonWidth + 4, y, buttonWidth, BUTTON_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        int contentWidth = Math.min(560, this.width - 40);
        int x = (this.width - contentWidth) / 2;
        int y = 34;
        drawTrimmed(context, Component.literal(I18nManager.translate("commandui.easyadd.confirm.prompt")),
            x, y, contentWidth, 0xFFFFFF);
        y += 18;

        int maxLines = Math.max(1, (this.height - 72) / 12);
        for (int i = 0; i < this.summaryLines.size() && i < maxLines; i++) {
            drawTrimmed(context, Component.literal(this.summaryLines.get(i)), x, y + i * 12, contentWidth, 0xAAAAAA);
        }
    }

    private static List<String> buildSummary(AreaData areaData) {
        List<String> lines = new ArrayList<>();
        lines.add("name: " + nullText(areaData.getName()));
        lines.add("surfacename: " + nullText(areaData.getSurfacename()));
        lines.add("level: " + areaData.getLevel());
        lines.add("base-name: " + nullText(areaData.getBaseName()));
        lines.add("signature: " + nullText(areaData.getSignature()));
        lines.add("color: " + nullText(areaData.getColor()));
        lines.add("vertices: " + vertices(areaData.getVertices()));
        lines.add("second-vertices: " + vertices(areaData.getSecondVertices()));
        lines.add("altitude: " + altitude(areaData.getAltitude()));
        return lines;
    }

    private static String vertices(List<AreaData.Vertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return "[]";
        }
        List<String> values = new ArrayList<>();
        for (AreaData.Vertex vertex : vertices) {
            values.add("{x:" + trim(vertex.getX()) + ",z:" + trim(vertex.getZ()) + "}");
        }
        return "[" + String.join(",", values) + "]";
    }

    private static String altitude(AreaData.AltitudeData altitude) {
        if (altitude == null) {
            return "null";
        }
        return "{max:" + nullText(altitude.getMax()) + ",min:" + nullText(altitude.getMin()) + "}";
    }

    private static String nullText(Object value) {
        return value == null ? "null" : value.toString();
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
