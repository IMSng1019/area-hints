// 阶段 6 第三方隔离搬迁：BlueMap API（1.20.6 节点）的集成实现。
// 与 1.20.4 节点同源复制（机制 ② 的代价）；BlueMap API 版本不同时在本文件内单独适配。

package areahint.map;

import de.bluecolored.bluemap.api.markers.ExtrudeMarker;

/**
 * BlueMap 动态 marker 运行时状态。
 */
public final class BlueMapDynamicMarkerState {
    private final String markerId;
    private final ExtrudeMarker marker;
    private final String flashMode;
    private final long phaseOffsetMs;
    private final int lineWidth;
    private final float fillAlpha;
    private long lastPhaseBucket;
    private int lastAppliedColor;

    public BlueMapDynamicMarkerState(String markerId, ExtrudeMarker marker, String flashMode,
                                     long phaseOffsetMs, int lineWidth, float fillAlpha,
                                     long lastPhaseBucket, int lastAppliedColor) {
        this.markerId = markerId;
        this.marker = marker;
        this.flashMode = flashMode;
        this.phaseOffsetMs = phaseOffsetMs;
        this.lineWidth = lineWidth;
        this.fillAlpha = fillAlpha;
        this.lastPhaseBucket = lastPhaseBucket;
        this.lastAppliedColor = lastAppliedColor;
    }

    public String getMarkerId() {
        return markerId;
    }

    public ExtrudeMarker getMarker() {
        return marker;
    }

    public String getFlashMode() {
        return flashMode;
    }

    public long getPhaseOffsetMs() {
        return phaseOffsetMs;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public float getFillAlpha() {
        return fillAlpha;
    }

    public long getLastPhaseBucket() {
        return lastPhaseBucket;
    }

    public void setLastPhaseBucket(long lastPhaseBucket) {
        this.lastPhaseBucket = lastPhaseBucket;
    }

    public int getLastAppliedColor() {
        return lastAppliedColor;
    }

    public void setLastAppliedColor(int lastAppliedColor) {
        this.lastAppliedColor = lastAppliedColor;
    }
}
