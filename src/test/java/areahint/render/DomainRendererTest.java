package areahint.render;

import areahint.data.AreaData;
import areahint.util.ColorUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainRendererTest {
    @Test
    void prebuiltIndexPreservesSurfaceNamesColorsAndFullParentChain() {
        AreaData root = area("根域", 1, null, null, "#FF5555");
        AreaData child = area("实际子域", 2, "根域", " 联合域 ", "#55FF55");
        AreaData leaf = area("叶域", 3, "实际子域", "  ", ColorUtil.FLASH_RAINBOW_ALL);

        // 联合域名保留原有空格，空白联合域名回退实际名称，闪烁颜色不写入静态颜色代码。
        assertBothOverloads("§c根域§r§f·§a 联合域 §r§f·叶域", leaf, List.of(root, child, leaf));
    }

    @Test
    void duplicateParentNamesKeepLastRecordForDisplay() {
        AreaData firstParent = area("同名父域", 1, null, "旧父域", "#AA0000");
        AreaData lastParent = area("同名父域", 1, null, "新父域", "#00AA00");
        AreaData child = area("子域", 2, "同名父域", null, "#5555FF");
        List<AreaData> areas = List.of(firstParent, child, lastParent);
        Map<String, AreaData> displayIndex = DomainRenderer.buildAreaIndex(areas);

        // 显示索引的后者覆盖语义与检测索引的先出现者优先语义不同，不能混用。
        assertSame(lastParent, displayIndex.get("同名父域"));
        assertBothOverloads("§2新父域§r§f·§9子域§r", child, areas);
    }

    @Test
    void missingParentLeavesCurrentDomainVisible() {
        AreaData child = area("孤立域", 2, "不存在的父域", null, null);

        assertBothOverloads("§f孤立域§r", child, List.of(child));
    }

    @Test
    void cyclicParentReferencesStopBeforeRepeatingDomain() {
        AreaData first = area("甲域", 2, "乙域", null, "#FF5555");
        AreaData second = area("乙域", 1, "甲域", null, "#55FF55");

        assertBothOverloads("§a乙域§r§f·§c甲域§r", first, List.of(first, second));
    }

    @Test
    void emptyCurrentDomainProducesEmptyTextWithEitherIndexForm() {
        assertBothOverloads("", null, List.of());
    }

    private static void assertBothOverloads(String expected, AreaData current, List<AreaData> areas) {
        assertEquals(expected, DomainRenderer.buildDomainDisplayText(current, areas));
        assertEquals(expected, DomainRenderer.buildDomainDisplayText(current, DomainRenderer.buildAreaIndex(areas)));
    }

    private static AreaData area(String name, int level, String parent, String surfaceName, String color) {
        AreaData area = new AreaData();
        area.setName(name);
        area.setLevel(level);
        area.setBaseName(parent);
        area.setSurfacename(surfaceName);
        area.setColor(color);
        return area;
    }
}
