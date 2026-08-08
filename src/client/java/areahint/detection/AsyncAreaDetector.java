package areahint.detection;

import areahint.AreashintClient;
import areahint.data.AreaData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步区域检测器
 * 将射线法检测放到后台线程执行，主线程只消费结果
 */
public class AsyncAreaDetector {
    private final AreaDetector areaDetector;
    private final ExecutorService executor;
    private final AtomicReference<DetectionResult> latestResult = new AtomicReference<>();
    private final AtomicLong generation = new AtomicLong();

    // 上次检测的坐标（用于移动阈值判断）
    private double lastX = Double.NaN, lastY = Double.NaN, lastZ = Double.NaN;
    private static final double MOVE_THRESHOLD_SQ = 0.25; // 0.5格的平方距离

    // 防止重复提交
    private volatile boolean detecting = false;
    private volatile long activeTaskGeneration = Long.MIN_VALUE;

    public AsyncAreaDetector(AreaDetector areaDetector) {
        this.areaDetector = areaDetector;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "AreaHint-Detection");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 提交异步检测任务
     * 如果玩家移动距离小于阈值则跳过
     */
    public void submitDetection(double x, double y, double z) {
        long taskGeneration = generation.get();
        DetectionResult pendingResult = latestResult.get();

        // 只有当前代次的待处理结果才能阻止同坐标再次检测
        double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
        if (!Double.isNaN(lastX) && dx * dx + dy * dy + dz * dz < MOVE_THRESHOLD_SQ
                && pendingResult != null && pendingResult.generation == taskGeneration) {
            return;
        }

        if (detecting) return;

        detecting = true;
        activeTaskGeneration = taskGeneration;
        lastX = x; lastY = y; lastZ = z;

        executor.submit(() -> {
            try {
                AreaData rawArea = areaDetector.findAreaRaw(x, y, z);
                String formatted = (rawArea != null)
                        ? areaDetector.formatAreaNameFromData(rawArea) : null;
                DetectionResult completedResult = new DetectionResult(rawArea, formatted, taskGeneration);
                // 单线程队列中旧任务仍可能晚于 reset 后的新任务完成，只保留代次更新的结果
                latestResult.accumulateAndGet(completedResult, (current, completed) ->
                        current == null || completed.generation >= current.generation ? completed : current);
            } catch (Exception e) {
                AreashintClient.LOGGER.error("异步区域检测出错", e);
            } finally {
                // reset 后可能已有新任务开始，旧任务不能清除新任务的运行标记
                if (activeTaskGeneration == taskGeneration) {
                    detecting = false;
                }
            }
        });
    }

    /**
     * 消费最新检测结果（主线程调用）
     * 返回后清除，避免重复处理
     */
    public DetectionResult pollResult() {
        DetectionResult result = latestResult.getAndSet(null);
        return result != null && result.generation == generation.get() ? result : null;
    }

    /**
     * 查看最新结果但不清除
     */
    public DetectionResult peekResult() {
        DetectionResult result = latestResult.get();
        return result != null && result.generation == generation.get() ? result : null;
    }

    /**
     * 重置状态（维度切换/断开连接时调用）
     */
    public void reset() {
        generation.incrementAndGet();
        latestResult.set(null);
        lastX = Double.NaN;
        lastY = Double.NaN;
        lastZ = Double.NaN;
        activeTaskGeneration = Long.MIN_VALUE;
        detecting = false;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 检测结果容器
     */
    public static class DetectionResult {
        public final AreaData areaData;
        public final String formattedName;
        private final long generation;

        private DetectionResult(AreaData areaData, String formattedName, long generation) {
            this.areaData = areaData;
            this.formattedName = formattedName;
            this.generation = generation;
        }
    }
}
