package areahint.xaero;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

class AsyncOverlayCacheTest {
    @Test
    void requestReturnsFallbackWithoutRunningGeometryOnCaller() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        AtomicInteger calculations = new AtomicInteger();

        assertEquals("fallback", cache.request("world", "A", cancelled -> {
            calculations.incrementAndGet();
            return "mesh-A";
        }, "fallback"));
        assertEquals(0, calculations.get());
        assertEquals(1, executor.size());

        executor.runNext();
        assertEquals("mesh-A", cache.request("world", "A", cancelled -> fail("重复计算"), "fallback"));
        assertEquals(1, calculations.get());
    }

    @Test
    void rapidChangesRetainOnlyLatestPendingRequestPerDimension() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<Integer, Integer> cache = new AsyncOverlayCache<>(executor, 4, (key, failure) -> fail(failure));
        List<Integer> calculated = new ArrayList<>();
        for (int key = 0; key < 500; key++) {
            int value = key;
            cache.request("world", key, cancelled -> {
                calculated.add(value);
                return value;
            }, -1);
        }

        assertEquals(1, executor.size());
        executor.runNext();
        assertEquals(List.of(499), calculated);
        assertEquals(499, cache.request("world", 499, cancelled -> fail("重复计算"), -1));
    }

    @Test
    void repeatedFramesShareOnePendingCalculation() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        AtomicInteger calculations = new AtomicInteger();
        for (int i = 0; i < 100; i++) {
            cache.request("world", "A", cancelled -> {
                calculations.incrementAndGet();
                return "mesh-A";
            }, "fallback");
        }
        assertEquals(1, executor.size());
        executor.runNext();
        assertEquals(1, calculations.get());
    }

    @Test
    void revisitingCachedAreaCancelsAnObsoletePendingCalculation() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        cache.request("world", "A", cancelled -> "mesh-A", "fallback");
        executor.runNext();
        cache.request("world", "B", cancelled -> fail("过时任务不应执行"), "fallback");

        assertEquals("mesh-A", cache.request("world", "A", cancelled -> fail("缓存未复用"), "fallback"));
        executor.runNext();
        assertEquals("mesh-A", cache.request("world", "A", cancelled -> fail("缓存未复用"), "fallback"));
    }

    @Test
    void dimensionsHaveIndependentRequestsAndCacheEntries() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        cache.request("overworld", "A", cancelled -> "overworld-A", "fallback");
        cache.request("nether", "A", cancelled -> "nether-A", "fallback");
        assertEquals(1, executor.size());
        executor.runNext();

        assertEquals("overworld-A", cache.request("overworld", "A", cancelled -> fail("重复计算"), "fallback"));
        assertEquals("nether-A", cache.request("nether", "A", cancelled -> fail("重复计算"), "fallback"));
    }

    @Test
    void latestRequestCancelsRunningTaskAndCannotBeBlockedByItsComputation() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ExecutorService caller = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<BooleanSupplier> cancellation = new AtomicReference<>();
        try {
            AsyncOverlayCache<String, String> cache = cache(worker, 4);
            cache.request("world", "old", cancelled -> {
                cancellation.set(cancelled);
                started.countDown();
                await(release);
                return "stale-mesh";
            }, "fallback");
            assertTrue(started.await(3, TimeUnit.SECONDS));

            assertEquals("fallback", caller.submit(() -> cache.request("world", "new",
                    cancelled -> "new-mesh", "fallback")).get(3, TimeUnit.SECONDS));
            assertTrue(cancellation.get().getAsBoolean());
            release.countDown();
            worker.submit(() -> {}).get(3, TimeUnit.SECONDS);

            assertEquals("new-mesh", cache.request("world", "new", cancelled -> fail("重复计算"), "fallback"));
            assertEquals("fallback", cache.request("world", "old", cancelled -> "fresh-old", "fallback"));
        } finally {
            release.countDown();
            worker.shutdownNow();
            caller.shutdownNow();
        }
    }

    @Test
    void clearRejectsRunningOldWorldResultEvenWhenKeyIsReused() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<BooleanSupplier> cancellation = new AtomicReference<>();
        try {
            AsyncOverlayCache<String, String> cache = cache(worker, 4);
            cache.request("world", "A", cancelled -> {
                cancellation.set(cancelled);
                started.countDown();
                await(release);
                return "old-world";
            }, "fallback");
            assertTrue(started.await(3, TimeUnit.SECONDS));
            cache.clear();
            assertTrue(cancellation.get().getAsBoolean());
            cache.request("world", "A", cancelled -> "new-world", "fallback");
            release.countDown();
            worker.submit(() -> {}).get(3, TimeUnit.SECONDS);

            assertEquals("new-world", cache.request("world", "A", cancelled -> fail("重复计算"), "fallback"));
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void invalidateDropsOnlyItsDimensionIncludingPendingWork() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        cache.request("overworld", "A", cancelled -> "overworld-A", "fallback");
        cache.request("nether", "A", cancelled -> "nether-A", "fallback");
        executor.runNext();
        cache.request("overworld", "B", cancelled -> fail("已失效任务不应执行"), "fallback");
        cache.invalidate("overworld");
        executor.runNext();

        assertEquals("nether-A", cache.request("nether", "A", cancelled -> fail("其他维度缓存应保留"), "fallback"));
        assertEquals("fallback", cache.request("overworld", "A", cancelled -> "fresh-A", "fallback"));
        executor.runNext();
        assertEquals("fresh-A", cache.request("overworld", "A", cancelled -> fail("重复计算"), "fallback"));
    }

    @Test
    void clearDropsPendingWorkBeforeItStarts() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 4);
        cache.request("world", "A", cancelled -> fail("清理前任务不应执行"), "fallback");
        cache.clear();
        executor.runNext();
        cache.request("world", "A", cancelled -> "fresh", "fallback");
        executor.runNext();
        assertEquals("fresh", cache.request("world", "A", cancelled -> fail("重复计算"), "fallback"));
    }

    @Test
    void leastRecentlyUsedEntryIsEvictedAndCanBeComputedAgain() {
        ManualExecutor executor = new ManualExecutor();
        AsyncOverlayCache<String, String> cache = cache(executor, 2);
        for (String key : List.of("A", "B")) {
            cache.request("world", key, cancelled -> key, "fallback");
            executor.runNext();
        }
        assertEquals("A", cache.request("world", "A", cancelled -> fail("缓存未复用"), "fallback"));
        cache.request("world", "C", cancelled -> "C", "fallback");
        executor.runNext();

        assertEquals("A", cache.request("world", "A", cancelled -> fail("最近访问项不应淘汰"), "fallback"));
        assertEquals("fallback", cache.request("world", "B", cancelled -> "new-B", "fallback"));
        executor.runNext();
        assertEquals("new-B", cache.request("world", "B", cancelled -> fail("重复计算"), "fallback"));
    }

    @Test
    void failureIsReportedOnceUntilCurrentKeyChanges() {
        ManualExecutor executor = new ManualExecutor();
        List<String> failures = new ArrayList<>();
        AsyncOverlayCache<String, String> cache = new AsyncOverlayCache<>(executor, 4,
                (key, failure) -> failures.add(key));
        AtomicInteger calculations = new AtomicInteger();
        cache.request("world", "broken", cancelled -> {
            calculations.incrementAndGet();
            throw new IllegalStateException("broken");
        }, "fallback");
        executor.runNext();
        for (int i = 0; i < 100; i++) {
            assertEquals("fallback", cache.request("world", "broken", cancelled -> fail("失败项不应逐帧重试"), "fallback"));
        }
        assertEquals(0, executor.size());
        assertEquals(1, calculations.get());
        assertEquals(List.of("broken"), failures);

        cache.request("world", "other", cancelled -> "other", "fallback");
        executor.runNext();
        cache.request("world", "broken", cancelled -> "recovered", "fallback");
        executor.runNext();
        assertEquals("recovered", cache.request("world", "broken", cancelled -> fail("重复计算"), "fallback"));
    }

    @Test
    void failureHandlerRunsOutsideCacheLockAndDoesNotStopOtherDimensions() {
        ManualExecutor executor = new ManualExecutor();
        AtomicReference<AsyncOverlayCache<String, String>> reference = new AtomicReference<>();
        AsyncOverlayCache<String, String> cache = new AsyncOverlayCache<>(executor, 4, (key, failure) -> {
            try {
                CompletableFuture.runAsync(() -> reference.get().invalidate("world")).get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new AssertionError("失败回调持有缓存锁", e);
            }
        });
        reference.set(cache);
        cache.request("world", "broken", cancelled -> { throw new IllegalArgumentException(); }, "fallback");
        cache.request("nether", "A", cancelled -> "nether-A", "fallback");
        executor.runNext();
        assertEquals("nether-A", cache.request("nether", "A", cancelled -> fail("其他维度任务丢失"), "fallback"));
    }

    @Test
    void executorRejectionReturnsFallbackAndDoesNotRetryEveryFrame() {
        AtomicInteger submissions = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        Executor rejected = task -> {
            submissions.incrementAndGet();
            throw new RejectedExecutionException("stopped");
        };
        AsyncOverlayCache<String, String> cache = new AsyncOverlayCache<>(rejected, 4,
                (key, failure) -> failures.incrementAndGet());
        for (int i = 0; i < 10; i++) {
            assertEquals("fallback", cache.request("world", "A", cancelled -> fail("不应执行"), "fallback"));
        }
        assertEquals(1, submissions.get());
        assertEquals(1, failures.get());
    }

    private static AsyncOverlayCache<String, String> cache(Executor executor, int maxEntries) {
        return new AsyncOverlayCache<>(executor, maxEntries, (key, failure) -> fail(failure));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("测试等待超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> queued = new ArrayDeque<>();

        @Override
        public void execute(Runnable task) {
            queued.add(task);
        }

        int size() {
            return queued.size();
        }

        void runNext() {
            assertFalse(queued.isEmpty(), "后台任务应已排队");
            queued.remove().run();
        }
    }
}
