package areahint.vizedit;

import java.awt.geom.Rectangle2D;

// 所有绘制与拾取使用同一组互逆变换，地图缩放和 GUI 比例不得各自再换算一次。
public interface VizEditCanvas {
    double screenX(double worldX);
    double screenY(double worldZ);
    double worldX(double screenX);
    double worldZ(double screenY);
    double pixelsPerBlock();
    void pan(double dx, double dy);
    void zoom(double amount, double x, double y);
    void frame(Rectangle2D bounds, int left, int top, int width, int height);
    default boolean isTerrain() { return false; }
    default boolean isCurrentDimension() { return true; }

    // 独立网格不依赖 Xaero，中心和缩放保存在编辑会话中以便往返恢复。
    final class Grid implements VizEditCanvas {
        private double centerX;
        private double centerZ;
        private double scale = 2;
        private double anchorX;
        private double anchorY;

        public void viewport(int left, int top, int width, int height) {
            anchorX = left + width / 2.0;
            anchorY = top + height / 2.0;
        }
        public void center(double x, double z) { centerX = x; centerZ = z; }
        @Override public double screenX(double x) { return anchorX + (x - centerX) * scale; }
        @Override public double screenY(double z) { return anchorY + (z - centerZ) * scale; }
        @Override public double worldX(double x) { return centerX + (x - anchorX) / scale; }
        @Override public double worldZ(double y) { return centerZ + (y - anchorY) / scale; }
        @Override public double pixelsPerBlock() { return scale; }
        @Override public void pan(double dx, double dy) { centerX -= dx / scale; centerZ -= dy / scale; }
        @Override public void zoom(double amount, double x, double y) {
            double beforeX = worldX(x), beforeZ = worldZ(y);
            scale = Math.max(0.002, Math.min(64, scale * Math.pow(1.25, amount)));
            centerX += beforeX - worldX(x);
            centerZ += beforeZ - worldZ(y);
        }
        @Override public void frame(Rectangle2D bounds, int left, int top, int width, int height) {
            viewport(left, top, width, height);
            centerX = bounds.getCenterX(); centerZ = bounds.getCenterY();
            scale = Math.max(0.002, Math.min(16, Math.min(width / Math.max(16, bounds.getWidth() * 1.3),
                height / Math.max(16, bounds.getHeight() * 1.3))));
        }
    }
}
