package areahint.management.client;

import areahint.commandui.CommandVisualHandler;
import areahint.commandui.CommandVisualLaunchContext;
import areahint.commandui.CommandVisualRegistry;
import areahint.i18n.I18nManager;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 只展示服务端已授权操作的域名管理面板。
 */
final class AreaManagementScreen extends Screen {
    private static final Map<String, List<String>> GROUPS = createGroups();

    private final Screen parent;
    private final OverlayArea area;
    private final String dimensionId;
    private final long requestId;
    private final Set<String> allowed;
    private ManagementList list;

    AreaManagementScreen(Screen parent, OverlayArea area, String dimensionId,
                         long requestId, List<String> allowedOperations) {
        super(Text.literal(I18nManager.translate("xaero.areahint.management.title", area.displayName())));
        this.parent = parent;
        this.area = area;
        this.dimensionId = dimensionId;
        this.requestId = requestId;
        this.allowed = Set.copyOf(allowedOperations == null ? List.of() : allowedOperations);
    }

    @Override
    protected void init() {
        this.list = new ManagementList(this.client, this.width, this.height, 48, this.height - 34);
        this.addDrawableChild(this.list);
        for (Map.Entry<String, List<String>> group : GROUPS.entrySet()) {
            List<String> visible = group.getValue().stream().filter(allowed::contains).toList();
            if (visible.isEmpty()) {
                continue;
            }
            this.list.addGroup(group.getKey());
            for (String operation : visible) {
                this.list.addOperation(operation);
            }
        }
        int width = 90;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(I18nManager.translate("commandui.button.close")),
            button -> close()).dimensions((this.width - width) / 2, this.height - 28, width, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
            Text.literal(I18nManager.translate("xaero.areahint.management.area", area.name(), area.level())),
            this.width / 2, 28, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        CommandVisualLaunchContext.clear();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private void launch(String operation) {
        CommandVisualHandler handler = CommandVisualRegistry.getById(operation);
        if (handler == null) {
            return;
        }
        if (CommandVisualLaunchContext.requiresAreaTarget(operation)) {
            CommandVisualLaunchContext.begin(operation, area.name(), dimensionId, requestId);
        } else {
            CommandVisualLaunchContext.clear();
        }
        handler.open(this);
    }

    private static Map<String, List<String>> createGroups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        groups.put("xaero.areahint.group.teleport", List.of("tcp", "udp"));
        groups.put("xaero.areahint.group.properties", List.of("rename", "recolor", "sethigh"));
        groups.put("xaero.areahint.group.text", List.of("adddescription", "replacedescription", "deletedescription",
            "addsubtitle", "replacesubtitle", "deletesubtitle", "replacesubtitlecolor", "replacesubtitlesize",
            "addsignature", "deletesignature"));
        groups.put("xaero.areahint.group.geometry", List.of("expandarea", "shrinkarea", "dividearea", "addhint", "deletehint"));
        groups.put("xaero.areahint.group.danger", List.of("delete"));
        return groups;
    }

    private final class ManagementList extends ElementListWidget<ManagementList.Entry> {
        private ManagementList(MinecraftClient client, int width, int height, int top, int bottom) {
            super(client, width, bottom - top, top, 26);
            this.setRenderBackground(false);
        }

        private void addGroup(String key) {
            this.addEntry(new GroupEntry(key));
        }

        private void addOperation(String operation) {
            this.addEntry(new OperationEntry(operation));
        }

        @Override
        public int getRowWidth() {
            return Math.min(500, AreaManagementScreen.this.width - 36);
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.width - 6;
        }

        private abstract class Entry extends ElementListWidget.Entry<Entry> {
        }

        private final class GroupEntry extends Entry {
            private final String key;

            private GroupEntry(String key) {
                this.key = key;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawTextWithShadow(AreaManagementScreen.this.textRenderer,
                    Text.literal(I18nManager.translate(key)), x + 6, y + 8, 0xFFFF55);
            }

            @Override public List<? extends Element> children() { return List.of(); }
            @Override public List<? extends Selectable> selectableChildren() { return List.of(); }
        }

        private final class OperationEntry extends Entry {
            private final String operation;
            private final ButtonWidget button;

            private OperationEntry(String operation) {
                this.operation = operation;
                this.button = ButtonWidget.builder(Text.literal(operationName(operation)),
                    ignored -> AreaManagementScreen.this.launch(operation)).size(190, 20).build();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                this.button.setPosition(x + 6, y + 3);
                this.button.render(context, mouseX, mouseY, tickDelta);
                CommandVisualHandler handler = CommandVisualRegistry.getById(operation);
                if (handler != null) {
                    String detail = handler.description().getString();
                    int detailX = x + 206;
                    context.drawTextWithShadow(AreaManagementScreen.this.textRenderer,
                        Text.literal(trim(detail, entryWidth - 212)), detailX, y + 9, 0xAAAAAA);
                }
            }

            @Override public boolean mouseClicked(double mouseX, double mouseY, int button) { return this.button.mouseClicked(mouseX, mouseY, button); }
            @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { return this.button.mouseReleased(mouseX, mouseY, button); }
            @Override public List<? extends Element> children() { return List.of(this.button); }
            @Override public List<? extends Selectable> selectableChildren() { return List.of(this.button); }
        }
    }

    private String operationName(String operation) {
        String key = "commandui." + operation + ".title";
        String translated = I18nManager.translate(key);
        return key.equals(translated) ? "/areahint " + operation : translated;
    }

    private String trim(String text, int maxWidth) {
        if (maxWidth <= 0 || this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        return this.textRenderer.trimToWidth(text, maxWidth - this.textRenderer.getWidth("...")) + "...";
    }
}
