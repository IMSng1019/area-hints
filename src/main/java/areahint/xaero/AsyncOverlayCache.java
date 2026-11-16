package areahint.xaero;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** 按维度合并最新几何请求，渲染端只读取已经完成的不可变结果。 */
public final class AsyncOverlayCache<K, V> {
    private final Object lock = new Object();
    private final Executor executor;
    private final int maxEntries;
    private final BiConsumer<K, Throwable> onFailure;
    private final Map<String, Request<K, V>> current = new HashMap<>();
    private final LinkedHashMap<String, Request<K, V>> pending = new LinkedHashMap<>();
    private final LinkedHashMap<CacheKey<K>, V> completed = new LinkedHashMap<>(16, 0.75F, true);
    private boolean draining;

    /** 执行器由调用方提供后台线程，不能使用在调用线程立即执行任务的执行器。 */
    public AsyncOverlayCache(Executor executor, int maxEntries, BiConsumer<K, Throwable> onFailure) {
        this.executor = Objects.requireNonNull(executor);
        this.onFailure = Objects.requireNonNull(onFailure);
        if (maxEntries < 1) {
            throw new IllegalArgumentException("缓存容量必须大于零");
        }
        this.maxEntries = maxEntries;
    }

    /** 未命中时立即返回替代网格；同一维度只保留最新请求，不等待正在运行的几何计算。 */
    public V request(String lane, K key, Function<BooleanSupplier, V> compute, V fallback) {
        Objects.requireNonNull(lane);
        Objects.requireNonNull(key);
        Objects.requireNonNull(compute);
        boolean submit = false;
        V result;
        synchronized (lock) {
            CacheKey<K> cacheKey = new CacheKey<>(lane, key);
            result = completed.get(cacheKey);
            Request<K, V> previous = current.get(lane);
            boolean sameRequest = previous != null && previous.key.equals(cacheKey);
            if (sameRequest && (result != null || !previous.finished || previous.failed)) {
                return result != null ? result : fallback;
            }

            // 即使目标已命中缓存，也必须取消该维度之前尚未完成的计算。
            if (previous != null) {
                previous.cancelled = true;
            }
            pending.remove(lane);
            Request<K, V> request = new Request<>(cacheKey, result == null ? compute : null);
            request.finished = result != null;
            current.put(lane, request);
            if (result == null) {
                pending.put(lane, request);
                if (!draining) {
                    draining = true;
                    submit = true;
                }
            }
        }
        if (submit) {
            submitDrain();
        }
        return result != null ? result : fallback;
    }

    /** 域名快照更新时只失效对应维度，其他地图正在使用的缓存仍可复用。 */
    public void invalidate(String lane) {
        Objects.requireNonNull(lane);
        synchronized (lock) {
            Request<K, V> request = current.remove(lane);
            if (request != null) {
                request.cancelled = true;
            }
            pending.remove(lane);
            completed.keySet().removeIf(key -> key.lane.equals(lane));
        }
    }

    /** 断开或切换世界后使全部旧任务失效；执行器保留供下一次连接使用。 */
    public void clear() {
        synchronized (lock) {
            current.values().forEach(request -> request.cancelled = true);
            current.clear();
            pending.clear();
            completed.clear();
        }
    }

    private void submitDrain() {
        try {
            executor.execute(this::drain);
        } catch (RuntimeException failure) {
            List<Request<K, V>> rejected;
            synchronized (lock) {
                draining = false;
                rejected = new ArrayList<>(pending.values());
                pending.clear();
                for (Request<K, V> request : rejected) {
                    request.finished = true;
                    request.failed = true;
                    request.compute = null;
                }
            }
            // 拒绝提交同样只报告一次，避免执行器停机后每帧重新提交并刷日志。
            for (Request<K, V> request : rejected) {
                reportFailure(request.key.key, failure);
            }
        }
    }

    private void drain() {
        while (true) {
            Request<K, V> request;
            synchronized (lock) {
                if (pending.isEmpty()) {
                    draining = false;
                    return;
                }
                var iterator = pending.entrySet().iterator();
                request = iterator.next().getValue();
                iterator.remove();
            }

            V value = null;
            Throwable failure = null;
            try {
                if (!request.isCancelled()) {
                    value = Objects.requireNonNull(request.compute.apply(request::isCancelled), "几何计算结果不能为空");
                }
            } catch (CancellationException ignored) {
                // 主动取消不输出错误；同键的意外取消也不应触发逐帧重试。
            } catch (RuntimeException | LinkageError error) {
                failure = error;
            }

            boolean report = false;
            synchronized (lock) {
                request.finished = true;
                request.compute = null;
                // 请求对象身份同时校验世界清理、快照更新和同名域名的再次进入。
                if (!request.isCancelled() && current.get(request.key.lane) == request) {
                    request.failed = value == null;
                    if (value != null) {
                        completed.put(request.key, value);
                        while (completed.size() > maxEntries) {
                            var iterator = completed.entrySet().iterator();
                            iterator.next();
                            iterator.remove();
                        }
                    }
                    report = failure != null;
                }
            }
            if (report) {
                reportFailure(request.key.key, failure);
            }
        }
    }

    private void reportFailure(K key, Throwable failure) {
        try {
            onFailure.accept(key, failure);
        } catch (RuntimeException ignored) {
            // 外部日志回调失败不能阻止后续维度任务继续执行。
        }
    }

    private record CacheKey<K>(String lane, K key) {
    }

    private static final class Request<K, V> {
        private final CacheKey<K> key;
        private Function<BooleanSupplier, V> compute;
        private volatile boolean cancelled;
        private boolean finished;
        private boolean failed;

        private Request(CacheKey<K> key, Function<BooleanSupplier, V> compute) {
            this.key = key;
            this.compute = compute;
        }

        private boolean isCancelled() {
            return cancelled || Thread.currentThread().isInterrupted();
        }
    }
}
