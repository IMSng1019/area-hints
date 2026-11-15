package areahint.keyhandler;

import java.util.Arrays;
import java.util.stream.IntStream;

/** 保留配置中的历史 GLFW 键码，平台转换不修改 recordKey 字段含义。 */
public final class LegacyKeyCodec {
    public static final int UNSUPPORTED = -1;
    private static final int[] LEGACY_TO_SDL = new int[349];
    private static final int[] SDL_TO_LEGACY = new int[512];
    private static final int[] SUPPORTED_LEGACY;

    static {
        Arrays.fill(LEGACY_TO_SDL, UNSUPPORTED);
        Arrays.fill(SDL_TO_LEGACY, UNSUPPORTED);
        // 字母、数字、功能键及小键盘均按物理键对应，不能把旧整数直接解释为 SDL 扫描码。
        bindRange(65, 4, 26);
        bindRange(49, 30, 9);
        bind(48, 39);
        bindRange(290, 58, 12);
        bindRange(302, 104, 12);
        bindRange(321, 89, 9);
        bind(320, 98);
        int[][] specialKeys = {
            {32, 44}, {39, 52}, {44, 54}, {45, 45}, {46, 55}, {47, 56}, {59, 51}, {61, 46},
            {91, 47}, {92, 49}, {93, 48}, {96, 53},
            {256, 41}, {257, 40}, {258, 43}, {259, 42}, {260, 73}, {261, 76}, {262, 79},
            {263, 80}, {264, 81}, {265, 82}, {266, 75}, {267, 78}, {268, 74}, {269, 77},
            {280, 57}, {281, 71}, {282, 83}, {283, 70}, {284, 72},
            // GLFW 小键盘小数点对应 SDL KP_PERIOD(99)，不能误绑到进制切换键 KP_DECIMAL(220)。
            {330, 99}, {331, 84}, {332, 85}, {333, 86}, {334, 87}, {335, 88}, {336, 103},
            {340, 225}, {341, 224}, {342, 226}, {343, 227}, {344, 229}, {345, 228}, {346, 230}, {347, 231}, {348, 101}
        };
        for (int[] pair : specialKeys) bind(pair[0], pair[1]);
        // GLFW 的 WORLD_1/WORLD_2 与 F25 没有确定的 SDL 对应，保留为不支持而非猜测另一个键。
        SUPPORTED_LEGACY = IntStream.range(0, LEGACY_TO_SDL.length).filter(code -> LEGACY_TO_SDL[code] != UNSUPPORTED).toArray();
    }

    private LegacyKeyCodec() { }

    public static int toSdl(int legacyCode) {
        return legacyCode >= 0 && legacyCode < LEGACY_TO_SDL.length ? LEGACY_TO_SDL[legacyCode] : UNSUPPORTED;
    }

    public static int toLegacy(int sdlCode) {
        return sdlCode >= 0 && sdlCode < SDL_TO_LEGACY.length ? SDL_TO_LEGACY[sdlCode] : UNSUPPORTED;
    }

    public static int[] supportedLegacyCodes() { return SUPPORTED_LEGACY.clone(); }

    private static void bindRange(int legacyStart, int sdlStart, int length) {
        for (int index = 0; index < length; index++) bind(legacyStart + index, sdlStart + index);
    }

    private static void bind(int legacy, int sdl) {
        if (LEGACY_TO_SDL[legacy] != UNSUPPORTED || SDL_TO_LEGACY[sdl] != UNSUPPORTED) {
            throw new IllegalStateException("按键转换表不能让两个物理键共用同一配置值");
        }
        LEGACY_TO_SDL[legacy] = sdl;
        SDL_TO_LEGACY[sdl] = legacy;
    }
}
