package areahint.xaero.worldmap;

import areahint.i18n.I18nManager;
import areahint.management.client.AreaManagementClient;
import areahint.xaero.AreaOverlayColorResolver;
import areahint.xaero.AreaOverlayRepository;
import areahint.xaero.AreaOverlayRepository.OverlayArea;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xaero.map.element.MapElementReader;
import xaero.map.element.MapElementRenderLocation;
import xaero.map.gui.CursorBox;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

final class AreaWorldMapReader extends MapElementReader<OverlayArea, AreaWorldMapRenderContext, AreaWorldMapRenderer> {
    private final AreaWorldMapRenderContext context;

    AreaWorldMapReader(AreaWorldMapRenderContext context) {
        this.context = context;
    }

    @Override
    public boolean isHidden(OverlayArea area, AreaWorldMapRenderContext context) {
        return area == null || area.vertices().size() < 3;
    }

    @Override
    public double getRenderX(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return area.centerX();
    }

    @Override
    public double getRenderZ(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return area.centerZ();
    }

    @Override
    public int getInteractionBoxLeft(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return clampFloor(area.minX() - area.centerX());
    }

    @Override
    public int getInteractionBoxRight(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return clampCeil(area.maxX() - area.centerX());
    }

    @Override
    public int getInteractionBoxTop(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return clampFloor(area.minZ() - area.centerZ());
    }

    @Override
    public int getInteractionBoxBottom(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return clampCeil(area.maxZ() - area.centerZ());
    }

    @Override
    public int getRenderBoxLeft(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return getInteractionBoxLeft(area, context, partialTicks);
    }

    @Override
    public int getRenderBoxRight(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return getInteractionBoxRight(area, context, partialTicks);
    }

    @Override
    public int getRenderBoxTop(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return getInteractionBoxTop(area, context, partialTicks);
    }

    @Override
    public int getRenderBoxBottom(OverlayArea area, AreaWorldMapRenderContext context, float partialTicks) {
        return getInteractionBoxBottom(area, context, partialTicks);
    }

    @Override
    public int getLeftSideLength(OverlayArea area, MinecraftClient client) {
        return 0;
    }

    @Override
    public String getMenuName(OverlayArea area) {
        return area == null ? "" : area.displayName();
    }

    @Override
    public String getFilterName(OverlayArea area) {
        return getMenuName(area);
    }

    @Override
    public int getMenuTextFillLeftPadding(OverlayArea area) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(OverlayArea area) {
        return 0xFF000000 | AreaOverlayColorResolver.resolve(area, System.currentTimeMillis());
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    @Override
    public boolean isInteractable(int location, OverlayArea area) {
        return location == MapElementRenderLocation.WORLD_MAP;
    }

    @Override
    public boolean isHoveredOnMap(int location, OverlayArea area, double mouseMapX, double mouseMapZ,
                                  double mapScale, double optionalScale, double dimensionScale,
                                  AreaWorldMapRenderContext context, float partialTicks) {
        double actualX = mouseMapX * dimensionScale;
        double actualZ = mouseMapZ * dimensionScale;
        boolean hovered = area.contains(actualX, actualZ);
        if (hovered) {
            context.hoverX = actualX;
            context.hoverZ = actualZ;
        }
        return hovered;
    }

    @Override
    public boolean isOnScreen(OverlayArea area, double cameraX, double cameraZ, int width, int height,
                              double mapScale, double optionalScale, double dimensionScale,
                              AreaWorldMapRenderContext context, float partialTicks) {
        double minScreenX = (area.minX() / dimensionScale - cameraX) * mapScale + width / 2.0D;
        double maxScreenX = (area.maxX() / dimensionScale - cameraX) * mapScale + width / 2.0D;
        double minScreenZ = (area.minZ() / dimensionScale - cameraZ) * mapScale + height / 2.0D;
        double maxScreenZ = (area.maxZ() / dimensionScale - cameraZ) * mapScale + height / 2.0D;
        return maxScreenX >= 0.0D && minScreenX <= width && maxScreenZ >= 0.0D && minScreenZ <= height;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(OverlayArea area, IRightClickableElement target) {
        ArrayList<RightClickOption> options = new ArrayList<>();
        if (!isRightClickValid(area)) {
            return options;
        }
        String dimensionId = context.dimensionId;
        double hoverX = context.hoverX;
        double hoverZ = context.hoverZ;
        // 右键菜单打开后鼠标会离开原位置，因此必须在创建菜单项时冻结本次精确命中结果。
        List<OverlayArea> hits = List.copyOf(AreaOverlayRepository.getInstance()
            .queryPoint(dimensionId, hoverX, hoverZ));
        if (hits.isEmpty()) {
            return options;
        }
        options.add(new RightClickOption(I18nManager.translate("xaero.areahint.manage"), 0, target) {
            @Override
            public void onAction(net.minecraft.client.gui.screen.Screen screen) {
                AreaManagementClient.openForHits(screen, dimensionId, hits);
            }
        });
        return options;
    }

    @Override
    public boolean isRightClickValid(OverlayArea area) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || context.dimensionId == null) {
            return false;
        }
        return context.dimensionId.equals(client.world.getRegistryKey().getValue().toString());
    }

    @Override
    public CursorBox getTooltip(OverlayArea area, AreaWorldMapRenderContext context, boolean overMenu) {
        StringJoiner signatures = new StringJoiner(", ");
        for (String signature : area.signatures()) {
            signatures.add(signature);
        }
        String altitude = formatAltitude(area);
        List<String> lines = List.of(
            I18nManager.translate("xaero.areahint.tooltip.name", area.name()),
            I18nManager.translate("xaero.areahint.tooltip.surface", value(area.surfaceName())),
            I18nManager.translate("xaero.areahint.tooltip.level", area.level()),
            I18nManager.translate("xaero.areahint.tooltip.base", value(area.baseName())),
            I18nManager.translate("xaero.areahint.tooltip.signature", signatures.length() == 0 ? value(null) : signatures.toString()),
            I18nManager.translate("xaero.areahint.tooltip.altitude", altitude),
            I18nManager.translate("xaero.areahint.tooltip.color", area.color())
        );
        return new CursorBox(Text.literal(String.join("\n", lines)));
    }

    private static String formatAltitude(OverlayArea area) {
        String min = area.minY() == null ? I18nManager.translate("xaero.areahint.unlimited") : trimNumber(area.minY());
        String max = area.maxY() == null ? I18nManager.translate("xaero.areahint.unlimited") : trimNumber(area.maxY());
        return min + " ~ " + max;
    }

    private static String trimNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static String value(String value) {
        return value == null || value.isBlank() ? I18nManager.translate("xaero.areahint.none") : value;
    }

    private static int clampFloor(double value) {
        double floored = Math.floor(value);
        return floored <= Integer.MIN_VALUE ? Integer.MIN_VALUE
            : floored >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) floored;
    }

    private static int clampCeil(double value) {
        double ceiled = Math.ceil(value);
        return ceiled <= Integer.MIN_VALUE ? Integer.MIN_VALUE
            : ceiled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ceiled;
    }
}
