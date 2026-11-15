package areahint.keyhandler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyKeyCodecTest {
    @Test
    void savedDefaultXCannotBecomeKeypadEnter() {
        assertEquals(27, LegacyKeyCodec.toSdl(88), "旧配置 88 必须仍然绑定 X");
        assertEquals(88, LegacyKeyCodec.toLegacy(27));
        assertEquals(335, LegacyKeyCodec.toLegacy(88), "SDL 的 88 是小键盘回车，不能写成旧 X 键");
        assertNotEquals(88, LegacyKeyCodec.toSdl(88));
    }

    @Test
    void capturesLettersDigitsAndFunctionKeysBackIntoLegacyConfig() {
        int[][] examples = {{65, 4}, {90, 29}, {48, 39}, {49, 30}, {57, 38}, {290, 58}, {301, 69}, {302, 104}, {313, 115}};
        for (int[] pair : examples) {
            assertEquals(pair[1], LegacyKeyCodec.toSdl(pair[0]));
            assertEquals(pair[0], LegacyKeyCodec.toLegacy(pair[1]));
        }
    }

    @Test
    void editAndModifierKeysKeepTheirPhysicalMeaning() {
        int[][] examples = {{256, 41}, {257, 40}, {258, 43}, {259, 42}, {261, 76}, {262, 79}, {263, 80},
            {340, 225}, {344, 229}, {341, 224}, {345, 228}, {342, 226}, {346, 230}, {343, 227}, {347, 231}};
        for (int[] pair : examples) {
            assertEquals(pair[1], LegacyKeyCodec.toSdl(pair[0]));
            assertEquals(pair[0], LegacyKeyCodec.toLegacy(pair[1]));
        }
    }

    @Test
    void unsupportedValuesCannotAliasAnotherKey() {
        for (int code : new int[] {-1, 0, 161, 162, 314, 999, Integer.MAX_VALUE}) {
            assertEquals(LegacyKeyCodec.UNSUPPORTED, LegacyKeyCodec.toSdl(code), "不支持的旧键码=" + code);
        }
        for (int code : new int[] {-1, 0, 3, 512, Integer.MAX_VALUE}) {
            assertEquals(LegacyKeyCodec.UNSUPPORTED, LegacyKeyCodec.toLegacy(code), "不支持的 SDL 键码=" + code);
        }
    }

    @Test
    void keypadDecimalRemainsThePhysicalPeriodKey() {
        assertEquals(99, LegacyKeyCodec.toSdl(330));
        assertEquals(330, LegacyKeyCodec.toLegacy(99));
        assertEquals(LegacyKeyCodec.UNSUPPORTED, LegacyKeyCodec.toLegacy(220), "进制切换键不能冒充小键盘小数点");
        assertEquals(84, LegacyKeyCodec.toSdl(331));
        assertEquals(86, LegacyKeyCodec.toSdl(333));
    }

    @Test
    void pollingOnlyUsesUniquePersistableKeys() {
        int[] keys = LegacyKeyCodec.supportedLegacyCodes();
        assertTrue(keys.length >= 100);
        int previous = -1;
        for (int key : keys) {
            assertTrue(key > previous, "轮询顺序沿用旧键码且不能重复");
            assertEquals(key, LegacyKeyCodec.toLegacy(LegacyKeyCodec.toSdl(key)));
            previous = key;
        }
        int first = keys[0];
        keys[0] = Integer.MAX_VALUE;
        assertEquals(first, LegacyKeyCodec.supportedLegacyCodes()[0], "外部调用不能破坏转换表");
    }
}
