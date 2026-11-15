package areahint.log;

import areahint.Areashint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * 异步日志管理器
 * 负责异步写入日志到文件，支持文件大小限制和自动分割
 * 采用常开写入句柄 + 有界队列，避免每条日志都执行 open/write/close
 */
public class AsyncLogManager {
    // 日志文件最大大小（10MB）
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024;

    // 写入队列上限，超出后丢弃并计数告警，避免内存无界增长
    private static final int MAX_QUEUE_SIZE = 10000;

    // 丢弃日志的告警间隔（每丢弃多少条告警一次）
    private static final long DROP_WARN_INTERVAL = 1000L;

    // 日期格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd HH:mm:ss");

    // 异步执行器（单线程消费 + 有界队列，保证写入顺序且不会交错）
    private final ExecutorService executorService;

    // 日志文件夹路径
    private final Path logFolder;

    // 日志类型（server或client）
    private final String logType;

    // 当前日志文件路径
    private Path currentLogFile;

    // 当前日志文件序号
    private int currentFileNumber = 1;

    // 当前日期（用于检测日期变化）
    private String currentDate;

    // 日志保留天数
    private final int retentionDays;

    // 是否已关闭
    private volatile boolean shutdown = false;

    // 常开写入句柄，仅在切分文件、日期变化或关闭时重建
    private BufferedWriter writer;

    // 当前文件已写入字节数，替代每次调用 Files.size
    private long currentFileBytes = 0L;

    // 写入句柄的同步锁，保证写入与关闭不会并发操作同一个句柄
    private final Object writerLock = new Object();

    // 是否已就“文件日志不可用”告警过，保证只记一次警告
    private boolean warnedUnavailable = false;

    // 日期变化后待执行的旧日志清理标记
    private boolean cleanupPending = false;

    // 因队列满被丢弃的日志条数
    private final AtomicLong droppedCount = new AtomicLong();

    /**
     * 构造函数
     * @param logFolder 日志文件夹路径
     * @param logType 日志类型（server或client）
     * @param retentionDays 日志保留天数
     */
    public AsyncLogManager(Path logFolder, String logType, int retentionDays) {
        this.logFolder = logFolder;
        this.logType = logType;
        this.retentionDays = retentionDays;
        // 单线程消费保证写入顺序，有界队列在拥塞时直接拒绝而不是无界堆积
        this.executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_QUEUE_SIZE),
            r -> {
                Thread thread = new Thread(r, "AsyncLogManager-" + logType);
                thread.setDaemon(true);
                return thread;
            });

        // 初始化日志文件
        initLogFile();

        // 清理旧日志
        try {
            executorService.execute(this::cleanOldLogs);
        } catch (RejectedExecutionException e) {
            // 队列异常时跳过清理，不影响日志写入
        }
    }

    /**
     * 初始化日志文件
     */
    private void initLogFile() {
        // 获取当前日期
        currentDate = LocalDate.now().format(DATE_FORMATTER);

        // 查找当前日期的最大文件序号
        currentFileNumber = findMaxFileNumber(currentDate);

        // 创建或打开日志文件（句柄常开）
        currentLogFile = getLogFilePath(currentDate, currentFileNumber);
        openCurrentFile();

        // 如果文件已存在且超过大小限制，创建新文件
        if (currentFileBytes >= MAX_LOG_FILE_SIZE) {
            currentFileNumber++;
            currentLogFile = getLogFilePath(currentDate, currentFileNumber);
            openCurrentFile();
        }
    }

    /**
     * 查找指定日期的最大文件序号
     * @param date 日期字符串
     * @return 最大文件序号
     */
    private int findMaxFileNumber(String date) {
        try (Stream<Path> files = Files.list(logFolder)) {
            return files
                .filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.startsWith(date + "_") && name.endsWith(".log"))
                .map(name -> {
                    try {
                        String numberPart = name.substring(date.length() + 1, name.length() - 4);
                        return Integer.parseInt(numberPart);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compareTo)
                .orElse(1);
        } catch (IOException e) {
            Areashint.LOGGER.error("查找最大文件序号失败", e);
            return 1;
        }
    }

    /**
     * 获取日志文件路径
     * @param date 日期字符串
     * @param fileNumber 文件序号
     * @return 日志文件路径
     */
    private Path getLogFilePath(String date, int fileNumber) {
        return logFolder.resolve(date + "_" + fileNumber + ".log");
    }

    /**
     * 打开当前日志文件的常开写入句柄，并初始化字节计数
     */
    private void openCurrentFile() {
        synchronized (writerLock) {
            closeWriterInternal();

            try {
                // 确保日志文件夹存在（可能被外部删除）
                if (Files.notExists(logFolder)) {
                    Files.createDirectories(logFolder);
                    Areashint.LOGGER.info("创建日志文件夹: {}", logFolder);
                }

                // 已存在的文件需要沿用原有长度，新文件从 0 开始计数
                boolean exists = Files.exists(currentLogFile);
                currentFileBytes = exists ? Files.size(currentLogFile) : 0L;

                // 以追加方式打开常开 UTF-8 写入句柄
                writer = Files.newBufferedWriter(currentLogFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                if (!exists) {
                    Areashint.LOGGER.info("创建日志文件: {}", currentLogFile);
                }

                // 句柄重建成功，允许后续再次告警
                warnedUnavailable = false;
            } catch (IOException e) {
                // 目录或文件不可写时丢弃日志，句柄保持为空
                writer = null;
                currentFileBytes = 0L;
                warnUnavailableOnce(e);
            }
        }
    }

    /**
     * 刷新并关闭常开写入句柄
     */
    private void closeWriter() {
        synchronized (writerLock) {
            closeWriterInternal();
        }
    }

    /**
     * 关闭句柄本体（调用方需持有 writerLock）
     */
    private void closeWriterInternal() {
        if (writer == null) {
            return;
        }

        // 先取出引用再置空，避免关闭失败后残留旧句柄
        BufferedWriter target = writer;
        writer = null;

        try {
            target.flush();
            target.close();
        } catch (IOException e) {
            warnUnavailableOnce(e);
        }
    }

    /**
     * 就文件日志不可用告警一次
     * @param e 触发原因
     */
    private void warnUnavailableOnce(Exception e) {
        if (warnedUnavailable) {
            return;
        }

        warnedUnavailable = true;
        Areashint.LOGGER.warn("文件日志不可用，已丢弃后续文件日志: {}", currentLogFile, e);
    }

    /**
     * 异步写入日志
     * @param message 日志消息
     */
    public void log(String message) {
        if (shutdown) {
            return;
        }

        // 与原实现保持一致：null 消息写入为字符串 "null"
        String payload = message != null ? message : "null";

        try {
            executorService.execute(() -> writeMessage(payload));
        } catch (RejectedExecutionException e) {
            // 关闭过程中的拒绝不计入丢弃统计
            if (shutdown) {
                return;
            }

            // 队列已满时丢弃并计数告警，保证内存有界
            long dropped = droppedCount.incrementAndGet();
            if (dropped == 1 || dropped % DROP_WARN_INTERVAL == 0) {
                Areashint.LOGGER.warn("日志写入队列已满（上限 {}），已丢弃 {} 条日志", MAX_QUEUE_SIZE, dropped);
            }
        }
    }

    /**
     * 消费一条日志：处理日期切分、大小切分并写入常开句柄
     * @param message 日志消息
     */
    private void writeMessage(String message) {
        // 单线程消费，加锁仅用于与关闭流程互斥
        synchronized (writerLock) {
            // 检查日期是否变化
            String today = LocalDate.now().format(DATE_FORMATTER);
            if (!today.equals(currentDate)) {
                currentDate = today;
                currentFileNumber = 1;
                currentLogFile = getLogFilePath(currentDate, currentFileNumber);

                // 切换到新日期的文件
                openCurrentFile();

                // 写入完成后再清理旧日志，保持原有先后顺序
                cleanupPending = true;
            }

            if (writer == null) {
                return;
            }

            // 检查文件大小（使用字节计数，不再重复读取文件大小）
            if (currentFileBytes >= MAX_LOG_FILE_SIZE) {
                currentFileNumber++;
                currentLogFile = getLogFilePath(currentDate, currentFileNumber);
                openCurrentFile();

                if (writer == null) {
                    return;
                }
            }

            // 添加时间戳
            String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
            String logLine = "[" + timestamp + "] " + message + "\n";

            try {
                writer.write(logLine);
                // 每条日志立即刷盘，保持与原实现相同的落盘时机
                writer.flush();
                currentFileBytes += logLine.getBytes(StandardCharsets.UTF_8).length;
            } catch (IOException e) {
                // 写入失败按不可写处理，丢弃该条日志并只告警一次
                closeWriterInternal();
                warnUnavailableOnce(e);
            }

            if (cleanupPending) {
                cleanupPending = false;
                cleanOldLogs();
            }
        }
    }

    /**
     * 清理旧日志文件
     */
    private void cleanOldLogs() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

            try (Stream<Path> files = Files.list(logFolder)) {
                files.filter(Files::isRegularFile)
                    .filter(file -> {
                        String fileName = file.getFileName().toString();
                        if (!fileName.endsWith(".log")) {
                            return false;
                        }

                        try {
                            // 提取日期部分
                            String datePart = fileName.substring(0, 8);
                            LocalDate fileDate = LocalDate.parse(datePart, DATE_FORMATTER);
                            return fileDate.isBefore(cutoffDate);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                            Areashint.LOGGER.info("删除旧日志文件: {}", file);
                        } catch (IOException e) {
                            Areashint.LOGGER.error("删除旧日志文件失败: {}", file, e);
                        }
                    });
            }
        } catch (IOException e) {
            Areashint.LOGGER.error("清理旧日志失败", e);
        }
    }

    /**
     * 关闭日志管理器
     * 先排空队列中的日志，再关闭写入句柄
     */
    public void shutdown() {
        shutdown = true;
        executorService.shutdown();
        try {
            // 等待队列中的日志全部写完，避免关服丢日志
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            // 队列排空后关闭句柄，保证缓冲区内容落盘
            closeWriter();
        }

        long dropped = droppedCount.get();
        if (dropped > 0) {
            Areashint.LOGGER.warn("本会话共丢弃 {} 条文件日志（队列上限 {}）", dropped, MAX_QUEUE_SIZE);
        }
    }
}
