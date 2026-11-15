package areahint.xaero;

import org.joml.Matrix4f;

import java.util.List;

/** 地图几何的版本中立接口，Minecraft 缓冲区、着色器和 GPU 附件类型只允许出现在版本实现内。 */
public interface OverlayBackend {
    /** 捕获调用方状态并准备覆盖层，返回 endOverlay 要执行的恢复动作。 */
    OverlayRenderHelper.RenderState beginOverlay();

    void drawTriangles(Matrix4f matrix, List<float[]> triangles, int rgb, float alpha, float depth);

    void drawLines(Matrix4f matrix, List<float[]> lines, int rgb, float alpha, float depth);
}
