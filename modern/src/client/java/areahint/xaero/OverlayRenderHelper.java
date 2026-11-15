package areahint.xaero;

import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** 两类地图共用的绘制门面，版本工厂提供具体后端，共享侧只管理成对的状态恢复。 */
public final class OverlayRenderHelper {
    /** 两张 Xaero 地图共用的域名名称放大倍数。 */
    public static final float AREA_NAME_SCALE = 4.0F;
    private static final OverlayBackend BACKEND = OverlayRendererFactory.create();
    private static final ThreadLocal<Deque<RenderState>> RENDER_STATES = ThreadLocal.withInitial(ArrayDeque::new);

    private OverlayRenderHelper() {
    }

    public static void beginOverlay() {
        RENDER_STATES.get().push(BACKEND.beginOverlay());
    }

    public static void endOverlay() {
        Deque<RenderState> states = RENDER_STATES.get();
        if (states.isEmpty()) {
            return;
        }
        RenderState state = states.pop();
        state.restore();
        if (states.isEmpty()) {
            RENDER_STATES.remove();
        }
    }

    public static void drawTriangles(Matrix4f matrix, List<float[]> triangles, int rgb, float alpha, float depth) {
        BACKEND.drawTriangles(matrix, triangles, rgb, alpha, depth);
    }

    public static void drawLines(Matrix4f matrix, List<float[]> lines, int rgb, float alpha, float depth) {
        BACKEND.drawLines(matrix, lines, rgb, alpha, depth);
    }

    /** 旧后端恢复绘制状态，新后端关闭目标附件作用域；保留同名类型并让其承担真实的跨版本恢复契约。 */
    public interface RenderState {
        void restore();
    }
}
