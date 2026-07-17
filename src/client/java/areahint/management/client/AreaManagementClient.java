package areahint.management.client;

import areahint.AreashintClient;
import areahint.commandui.CommandVisualLaunchContext;
import areahint.commandui.CommandVisualRegistry;
import areahint.commandui.WizardSelectionListScreen;
import areahint.i18n.I18nManager;
import areahint.network.Packets;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Xaero 世界地图域名管理请求、过期响应过滤和界面入口。
 */
public final class AreaManagementClient {
    private static final long REQUEST_TIMEOUT_MILLIS = 10_000L;
    private static final AtomicLong REQUEST_IDS = new AtomicLong();
    private static PendingRequest pending;

    private AreaManagementClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(Packets.S2C_AREA_MANAGEMENT_CAPABILITIES,
            (client, handler, buf, responseSender) -> {
                long requestId = buf.readLong();
                String dimensionId = buf.readString();
                String areaName = buf.readString();
                boolean valid = buf.readBoolean();
                int size = buf.readInt();
                List<String> operations = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    operations.add(buf.readString());
                }
                client.execute(() -> handleResponse(requestId, dimensionId, areaName, valid, operations));
            });
    }

    public static void openForHits(Screen parent, String dimensionId, List<OverlayArea> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }
        if (hits.size() == 1) {
            requestCapabilities(parent, dimensionId, hits.get(0));
            return;
        }

        List<WizardSelectionListScreen.SelectionItem<OverlayArea>> items = new ArrayList<>();
        for (OverlayArea area : hits) {
            items.add(new WizardSelectionListScreen.SelectionItem<>(area, area.displayName(),
                I18nManager.translate("xaero.areahint.selection.detail", area.name(), area.level())));
        }
        MinecraftClient.getInstance().setScreen(new WizardSelectionListScreen<>(parent,
            "xaero.areahint.selection.title", "xaero.areahint.selection.prompt", items,
            area -> requestCapabilities(parent, dimensionId, area), null, true));
    }

    public static void tick() {
        CommandVisualLaunchContext.tick();
        PendingRequest request = pending;
        if (request == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        String dimension = client != null && client.world != null
            ? client.world.getRegistryKey().getValue().toString() : null;
        if (System.currentTimeMillis() > request.expiresAtMillis()
            || !request.dimensionId().equals(dimension)) {
            cancelPending(request.requestId(), true);
        }
    }

    public static void clear() {
        pending = null;
        CommandVisualLaunchContext.clear();
    }

    private static void requestCapabilities(Screen parent, String dimensionId, OverlayArea area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || area == null) {
            return;
        }
        String currentDimension = client.world.getRegistryKey().getValue().toString();
        if (!currentDimension.equals(dimensionId)) {
            return;
        }

        long requestId = REQUEST_IDS.incrementAndGet();
        pending = new PendingRequest(requestId, dimensionId, area.name(), parent,
            System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(requestId);
        buf.writeString(dimensionId);
        buf.writeString(area.name());
        ClientPlayNetworking.send(Packets.C2S_AREA_MANAGEMENT_CAPABILITIES, buf);
        client.setScreen(new LoadingScreen(parent, requestId, area.displayName()));
    }

    private static void handleResponse(long requestId, String dimensionId, String areaName,
                                       boolean valid, List<String> operations) {
        PendingRequest request = pending;
        MinecraftClient client = MinecraftClient.getInstance();
        if (request == null || request.requestId() != requestId
            || !request.dimensionId().equals(dimensionId) || !request.areaName().equals(areaName)
            || client == null || client.world == null
            || !dimensionId.equals(client.world.getRegistryKey().getValue().toString())
            || System.currentTimeMillis() > request.expiresAtMillis()) {
            return;
        }

        OverlayArea area = AreaOverlayRepository.getInstance().find(dimensionId, areaName);
        pending = null;
        if (!valid || area == null) {
            client.setScreen(request.parent());
            return;
        }
        client.setScreen(new AreaManagementScreen(request.parent(), area, dimensionId, requestId, operations));
    }

    private static void cancelPending(long requestId, boolean returnToParent) {
        PendingRequest request = pending;
        if (request == null || request.requestId() != requestId) {
            return;
        }
        pending = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (returnToParent && client != null && client.currentScreen instanceof LoadingScreen loading
            && loading.requestId == requestId) {
            client.setScreen(request.parent());
        }
    }

    private record PendingRequest(long requestId, String dimensionId, String areaName,
                                  Screen parent, long expiresAtMillis) {
    }

    private static final class LoadingScreen extends Screen {
        private final Screen parent;
        private final long requestId;
        private final String areaName;

        private LoadingScreen(Screen parent, long requestId, String areaName) {
            super(Text.literal(I18nManager.translate("xaero.areahint.loading.title")));
            this.parent = parent;
            this.requestId = requestId;
            this.areaName = areaName;
        }

        @Override
        protected void init() {
            int width = 90;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(I18nManager.translate("commandui.button.cancel")),
                button -> close()).dimensions((this.width - width) / 2, this.height - 32, width, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(I18nManager.translate("xaero.areahint.loading.prompt", areaName)),
                this.width / 2, this.height / 2 - 4, 0xAAAAAA);
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public void close() {
            cancelPending(requestId, false);
            if (this.client != null) {
                this.client.setScreen(parent);
            }
        }
    }
}
