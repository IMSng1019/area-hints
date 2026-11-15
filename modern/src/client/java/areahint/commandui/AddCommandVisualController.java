package areahint.commandui;

import areahint.data.AreaData;
import areahint.i18n.I18nManager;
import areahint.util.ColorUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * /areahint add 的图形化流程，只负责收集参数并生成原命令需要的 JSON。
 */
public final class AddCommandVisualController {
    private AddCommandVisualController() {
    }

    public static void open(Screen parent) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            new WizardOptionScreen.OptionSpec("commandui.add.mode.form",
                "commandui.add.mode.form.detail", -1, () -> openBasic(parent, new AddAreaDraft(), null)),
            new WizardOptionScreen.OptionSpec("commandui.add.mode.paste_json",
                "commandui.add.mode.paste_json.detail", -1, () -> openRawJson(parent, null))
        );
        setScreen(new WizardOptionScreen(parent, titleKey(),
            "commandui.add.mode.prompt",
            "commandui.add.mode.detail",
            options,
            null));
    }

    private static void openRawJson(Screen parent, String errorKey) {
        setScreen(new WizardLongTextInputScreen(parent, titleKey(),
            "commandui.add.prompt",
            "commandui.add.detail",
            "",
            errorKey,
            text -> {
                String json = text == null ? "" : text.trim();
                if (json.isEmpty()) {
                    openRawJson(parent, "commandui.add.error.empty");
                    return;
                }
                CommandUiActions.runCommand("areahint add " + json);
            },
            null));
    }

    private static void openBasic(Screen parent, AddAreaDraft draft, String errorKey) {
        setScreen(new WizardTextInputScreen(parent, titleKey(),
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.add.name.label", "commandui.add.name.placeholder",
                    draft.name, 80),
                new WizardTextInputScreen.FieldSpec("commandui.add.surface.label", "commandui.add.surface.placeholder",
                    draft.surfaceName, 80),
                new WizardTextInputScreen.FieldSpec("commandui.add.level.label", "commandui.add.level.placeholder",
                    draft.levelText(), 8)
            ),
            "commandui.add.basic.prompt",
            "commandui.add.basic.detail",
            errorKey,
            values -> {
                String name = values.get(0).trim();
                if (name.isEmpty()) {
                    openBasic(parent, draft, "commandui.add.error.name_empty");
                    return;
                }
                try {
                    int level = Integer.parseInt(values.get(2).trim());
                    if (level < 1) {
                        openBasic(parent, draft, "commandui.add.error.level");
                        return;
                    }
                    draft.name = name;
                    draft.surfaceName = values.get(1).trim();
                    draft.level = level;
                    openOptional(parent, draft);
                } catch (NumberFormatException e) {
                    openBasic(parent, draft, "commandui.add.error.level");
                }
            },
            null));
    }

    private static void openOptional(Screen parent, AddAreaDraft draft) {
        setScreen(new WizardTextInputScreen(parent, titleKey(),
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.add.base.label", "commandui.add.base.placeholder",
                    draft.baseName, 80),
                new WizardTextInputScreen.FieldSpec("commandui.add.signature.label", "commandui.add.signature.placeholder",
                    draft.signature, 32)
            ),
            "commandui.add.optional.prompt",
            "commandui.add.optional.detail",
            null,
            values -> {
                draft.baseName = values.get(0).trim();
                draft.signature = values.get(1).trim();
                openVertices(parent, draft, null);
            },
            null));
    }

    private static void openVertices(Screen parent, AddAreaDraft draft, String errorKey) {
        setScreen(new WizardLongTextInputScreen(parent, titleKey(),
            "commandui.add.vertices.prompt",
            "commandui.add.vertices.detail",
            draft.verticesText,
            errorKey,
            text -> {
                String verticesText = text == null ? "" : text.trim();
                List<AreaData.Vertex> vertices = parseVertices(verticesText);
                if (vertices == null) {
                    openVertices(parent, draft, "commandui.add.vertices.error.format");
                    return;
                }
                if (vertices.isEmpty()) {
                    openVertices(parent, draft, "commandui.add.vertices.error.empty");
                    return;
                }
                if (vertices.size() < 3) {
                    openVertices(parent, draft, "commandui.add.vertices.error.count");
                    return;
                }
                draft.verticesText = verticesText;
                draft.vertices = vertices;
                draft.secondVertices = buildSecondVertices(vertices);
                openAltitudeMode(parent, draft);
            },
            null));
    }

    private static void openAltitudeMode(Screen parent, AddAreaDraft draft) {
        List<WizardOptionScreen.OptionSpec> options = List.of(
            new WizardOptionScreen.OptionSpec("commandui.add.altitude.unlimited",
                "commandui.add.altitude.unlimited.detail", -1, () -> {
                    draft.altitudeMin = null;
                    draft.altitudeMax = null;
                    openColor(parent, draft);
                }),
            new WizardOptionScreen.OptionSpec("commandui.add.altitude.custom",
                "commandui.add.altitude.custom.detail", -1, () -> openCustomAltitude(parent, draft, null))
        );
        setScreen(new WizardOptionScreen(parent, titleKey(),
            "commandui.add.altitude.prompt",
            "commandui.add.altitude.detail",
            options,
            null));
    }

    private static void openCustomAltitude(Screen parent, AddAreaDraft draft, String errorKey) {
        setScreen(new WizardTextInputScreen(parent, titleKey(),
            List.of(
                new WizardTextInputScreen.FieldSpec("commandui.add.altitude.min.label",
                    "commandui.add.altitude.min.placeholder", draft.altitudeMinText(), 12),
                new WizardTextInputScreen.FieldSpec("commandui.add.altitude.max.label",
                    "commandui.add.altitude.max.placeholder", draft.altitudeMaxText(), 12)
            ),
            "commandui.add.altitude.custom.prompt",
            "commandui.add.altitude.custom.range_detail",
            errorKey,
            values -> {
                try {
                    double min = Double.parseDouble(values.get(0).trim());
                    double max = Double.parseDouble(values.get(1).trim());
                    if (!Double.isFinite(min) || !Double.isFinite(max)) {
                        openCustomAltitude(parent, draft, "commandui.add.altitude.error.number");
                        return;
                    }
                    if (max < min) {
                        openCustomAltitude(parent, draft, "commandui.add.altitude.error.order");
                        return;
                    }
                    draft.altitudeMin = min;
                    draft.altitudeMax = max;
                    openColor(parent, draft);
                } catch (NumberFormatException e) {
                    openCustomAltitude(parent, draft, "commandui.add.altitude.error.number");
                }
            },
            null));
    }

    private static void openColor(Screen parent, AddAreaDraft draft) {
        setScreen(new WizardOptionScreen(parent, titleKey(),
            "commandui.add.color.prompt",
            "commandui.color.detail",
            CommandUiData.colorOptions(color -> {
                draft.color = color;
                openConfirm(parent, draft);
            }, () -> openCustomColor(parent, draft, null)),
            null));
    }

    private static void openCustomColor(Screen parent, AddAreaDraft draft, String errorKey) {
        setScreen(new WizardTextInputScreen(parent, titleKey(),
            List.of(new WizardTextInputScreen.FieldSpec("commandui.color.custom.label",
                "commandui.color.custom.placeholder", draft.color, 16)),
            "commandui.color.custom.prompt",
            "commandui.color.custom.detail",
            errorKey,
            values -> {
                String color = normalizeStrictColor(values.isEmpty() ? "" : values.get(0));
                if (color == null) {
                    openCustomColor(parent, draft, "commandui.color.error.invalid");
                    return;
                }
                draft.color = color;
                openConfirm(parent, draft);
            },
            null));
    }

    private static void openConfirm(Screen parent, AddAreaDraft draft) {
        String json = draft.toCommandJson();
        List<String> details = new ArrayList<>();
        details.add(I18nManager.translate("commandui.add.confirm.name", draft.name));
        details.add(I18nManager.translate("commandui.add.confirm.level", draft.level));
        details.add(I18nManager.translate("commandui.add.confirm.surface", nullText(draft.surfaceName)));
        details.add(I18nManager.translate("commandui.add.confirm.base", nullText(draft.baseName)));
        details.add(I18nManager.translate("commandui.add.confirm.signature", nullText(draft.signature)));
        details.add(I18nManager.translate("commandui.add.confirm.vertices", draft.vertices.size()));
        details.add(I18nManager.translate("commandui.add.confirm.altitude", draft.altitudeText()));
        details.add(I18nManager.translate("commandui.add.confirm.color", draft.color));
        setScreen(new WizardConfirmScreen(parent, titleKey(),
            I18nManager.translate("commandui.add.confirm.prompt"),
            details,
            "commandui.button.execute",
            () -> CommandUiActions.runCommand("areahint add " + json),
            null));
    }

    private static List<AreaData.Vertex> parseVertices(String input) {
        List<AreaData.Vertex> vertices = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return vertices;
        }

        String[] lines = input.trim().split("\\R|;");
        for (String line : lines) {
            String cleaned = line.trim();
            if (cleaned.isEmpty()) {
                continue;
            }
            cleaned = cleaned.replace("(", "").replace(")", "").replace("[", "").replace("]", "");
            String[] parts = cleaned.split("[,，\\s]+");
            if (parts.length < 2) {
                return null;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double z = Double.parseDouble(parts[1]);
                if (!Double.isFinite(x) || !Double.isFinite(z)) {
                    return null;
                }
                vertices.add(new AreaData.Vertex(x, z));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return vertices;
    }

    private static List<AreaData.Vertex> buildSecondVertices(List<AreaData.Vertex> vertices) {
        double minX = vertices.get(0).getX();
        double maxX = vertices.get(0).getX();
        double minZ = vertices.get(0).getZ();
        double maxZ = vertices.get(0).getZ();
        for (AreaData.Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.getX());
            maxX = Math.max(maxX, vertex.getX());
            minZ = Math.min(minZ, vertex.getZ());
            maxZ = Math.max(maxZ, vertex.getZ());
        }

        List<AreaData.Vertex> secondVertices = new ArrayList<>();
        secondVertices.add(new AreaData.Vertex(minX, maxZ));
        secondVertices.add(new AreaData.Vertex(maxX, maxZ));
        secondVertices.add(new AreaData.Vertex(maxX, minZ));
        secondVertices.add(new AreaData.Vertex(minX, minZ));
        return secondVertices;
    }

    private static JsonArray verticesToJson(List<AreaData.Vertex> vertices) {
        JsonArray array = new JsonArray();
        for (AreaData.Vertex vertex : vertices) {
            JsonObject vertexJson = new JsonObject();
            vertexJson.addProperty("x", vertex.getX());
            vertexJson.addProperty("z", vertex.getZ());
            array.add(vertexJson);
        }
        return array;
    }

    private static JsonObject altitudeToJson(Double min, Double max) {
        JsonObject altitude = new JsonObject();
        if (max == null) {
            altitude.add("max", JsonNull.INSTANCE);
        } else {
            altitude.addProperty("max", max);
        }
        if (min == null) {
            altitude.add("min", JsonNull.INSTANCE);
        } else {
            altitude.addProperty("min", min);
        }
        return altitude;
    }

    private static String normalizeStrictColor(String colorInput) {
        if (colorInput == null || colorInput.trim().isEmpty()) {
            return null;
        }
        String trimmed = colorInput.trim();
        if (ColorUtil.isFlashColor(trimmed)) {
            return trimmed;
        }
        String namedColor = ColorUtil.getColorHex(trimmed);
        if (namedColor != null) {
            return namedColor;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }
        return normalized.matches("^#[0-9A-F]{6}$") ? normalized : null;
    }

    private static void setScreen(Screen screen) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            areahint.util.ClientScreenCompat.setScreen(client, screen);
        }
    }

    private static String titleKey() {
        return "commandui.add.title";
    }

    private static String nullText(String value) {
        return value == null || value.trim().isEmpty() ? I18nManager.translate("commandui.common.none") : value;
    }

    private static final class AddAreaDraft {
        private String name = "";
        private String surfaceName = "";
        private int level = 1;
        private String baseName = "";
        private String signature = "";
        private String verticesText = "";
        private List<AreaData.Vertex> vertices = new ArrayList<>();
        private List<AreaData.Vertex> secondVertices = new ArrayList<>();
        private Double altitudeMin;
        private Double altitudeMax;
        private String color = "#FFFFFF";

        private String levelText() {
            return Integer.toString(this.level);
        }

        private String altitudeMinText() {
            return this.altitudeMin == null ? "" : formatDouble(this.altitudeMin);
        }

        private String altitudeMaxText() {
            return this.altitudeMax == null ? "" : formatDouble(this.altitudeMax);
        }

        private String altitudeText() {
            if (this.altitudeMin == null && this.altitudeMax == null) {
                return I18nManager.translate("commandui.add.altitude.unlimited");
            }
            return I18nManager.translate("commandui.add.altitude.range",
                formatDouble(this.altitudeMin), formatDouble(this.altitudeMax));
        }

        private String toCommandJson() {
            JsonObject area = new JsonObject();
            area.addProperty("name", this.name);
            area.add("vertices", verticesToJson(this.vertices));
            area.add("second-vertices", verticesToJson(this.secondVertices));
            area.add("altitude", altitudeToJson(this.altitudeMin, this.altitudeMax));
            area.addProperty("level", this.level);
            addNullableString(area, "base-name", this.baseName);
            addNullableString(area, "signature", this.signature);
            area.addProperty("color", this.color);
            addNullableString(area, "surfacename", this.surfaceName);
            return area.toString();
        }

        private static void addNullableString(JsonObject object, String key, String value) {
            if (value == null || value.trim().isEmpty()) {
                object.add(key, JsonNull.INSTANCE);
            } else {
                object.addProperty(key, value.trim());
            }
        }

        private static String formatDouble(Double value) {
            if (value == null) {
                return "";
            }
            if (value.doubleValue() == Math.rint(value.doubleValue())) {
                return Long.toString(Math.round(value.doubleValue()));
            }
            return value.toString();
        }
    }
}
