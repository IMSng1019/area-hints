package areahint.network;

import areahint.AreashintClient;
import areahint.file.FileManager;
import areahint.world.ClientWorldFolderManager;
import net.minecraft.client.MinecraftClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 域名文件异步落盘管线。
 * <p>
 * 服务端每次同步都会给客户端推送整个维度的域名文件，原实现直接在客户端主线程写盘，
 * 文件较大时会在主线程产生明显卡顿。这里改为：
 * 主线程只登记待写内容并立即返回，真实写盘在单线程队列里串行执行，
 * 写完之后再回到主线程刷新快照并重新加载当前维度的检测数据。
 * 同一维度连续到达的多个版本只保留最新一份，内存占用因此有明确上界。
 */
public final class ClientAreaDataWriter {
    // 待写快照：同一维度只保留最新内容，避免连续同步导致写盘任务堆积
    private static final Map<String, PendingWrite> PENDING_WRITES = new ConcurrentHashMap<>();

    // 落盘线程：单线程串行执行，队列里最多只有一个任务在等待
    private static volatile ExecutorService writeExecutor;

    private ClientAreaDataWriter() {
    }

    /**
     * 登记一次域名数据落盘请求，主线程只做登记并立即返回。
     * @param client 客户端实例，用于把写完后的回调切回主线程
     * @param dimensionName 维度名称（overworld、the_nether、the_end）
     * @param fileContent 服务端下发的域名文件原文
     */
    public static void submit(MinecraftClient client, String dimensionName, String fileContent) {
        String fileName = Packets.getFileNameForDimension(dimensionName);
        if (fileName == null) {
            AreashintClient.LOGGER.warn("接收到未知维度的区域数据: {}", dimensionName);
            return;
        }

        PENDING_WRITES.put(dimensionName, new PendingWrite(fileName, fileContent));
        AreashintClient.LOGGER.info("已接收 {} 的区域数据（{} 字节），已排入异步写盘队列",
            dimensionName, byteLength(fileContent));

        // 写盘任务本身只做文件写入，不触碰任何客户端状态
        writeTask(client, dimensionName);
    }

    /**
     * 断开连接时把仍在排队的域名文件补齐落盘，避免最后一次同步丢失。
     * <p>
     * 这里必须走同一个单线程执行器并等待它完成：否则主线程会与在途的写盘任务同时写同一个文件。
     * 提交顺序保证在途任务先结束，因此等待完成后文件内容一定是最后一份完整数据。
     */
    public static void flushPendingWrites() {
        if (PENDING_WRITES.isEmpty()) {
            return;
        }
        try {
            writeExecutor().submit(() -> {
                for (Map.Entry<String, PendingWrite> entry : PENDING_WRITES.entrySet()) {
                    writeSnapshot(entry.getValue(), entry.getKey());
                }
            }).get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // 超时或执行失败时写盘任务仍在后台继续，这里只记录日志，不阻塞退出流程
            AreashintClient.LOGGER.warn("断开连接时等待区域数据落盘未完成: {}", e.getMessage());
        }
    }

    /**
     * 写出单个待写快照，成功后才释放登记（失败保留登记，等待下一次同步覆盖）。
     */
    private static void writeSnapshot(PendingWrite write, String dimensionName) {
        try {
            FileManager.checkFolderExist();
            Files.writeString(ClientWorldFolderManager.getWorldDimensionFile(write.fileName()),
                write.content(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PENDING_WRITES.remove(dimensionName, write);
        } catch (Exception e) {
            AreashintClient.LOGGER.error("写入区域数据失败: {}（维度 {}）", e.getMessage(), dimensionName);
        }
    }

    private static void writeTask(MinecraftClient client, String submittedDimension) {
        // 每个维度只用一个写盘任务，后续版本被覆盖后由同一个任务写出最新内容
        CompletableFuture.runAsync(() -> {
            PendingWrite write = PENDING_WRITES.get(submittedDimension);
            if (write == null) {
                return;
            }
            try {
                FileManager.checkFolderExist();
                Path filePath = ClientWorldFolderManager.getWorldDimensionFile(write.fileName());
                Files.writeString(filePath, write.content(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception e) {
                AreashintClient.LOGGER.error("保存接收到的区域数据时出错: {}（维度 {}）",
                    e.getMessage(), submittedDimension);
                return;
            }


            // 写盘成功后回到主线程刷新依赖该文件的组件
            client.execute(() -> applyLoadedData(client, submittedDimension, write));
        }, writeExecutor());
    }

    /**
     * 在主线程应用刚写完的维度数据：先释放待写登记，再刷新 Xaero 快照与当前维度检测数据。
     */
    private static void applyLoadedData(MinecraftClient client, String dimensionName, PendingWrite written) {
        PENDING_WRITES.compute(dimensionName, (key, current) -> current == written ? null : current);

        areahint.xaero.AreaOverlayRepository.getInstance().refreshDimension(dimensionName);
        AreashintClient.LOGGER.info("已保存 {} 的区域数据", dimensionName);

        if (client.world != null && dimensionName.equals(Packets.convertDimensionPathToType(
                client.world.getDimensionKey().getValue().getPath()))) {
            AreashintClient.LOGGER.info("重新加载当前维度的区域数据: {}", written.fileName());
            AreashintClient.getAreaDetector().loadAreaData(written.fileName());
            areahint.boundviz.BoundVizManager.getInstance().reload();
        }
    }

    /**
     * 统计 UTF-8 字节数，仅用于日志，不参与任何判定。
     */
    private static int byteLength(String content) {
        return content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * 惰性创建落盘线程，未使用该功能时不会产生线程。
     */
    private static ExecutorService writeExecutor() {
        ExecutorService executor = writeExecutor;
        if (executor != null) {
            return executor;
        }
        synchronized (ClientAreaDataWriter.class) {
            if (writeExecutor == null) {
                writeExecutor = Executors.newSingleThreadExecutor(task -> {
                    Thread thread = new Thread(task, "AreaHint-AreaDataWriter");
                    thread.setDaemon(true);
                    return thread;
                });
            }
            return writeExecutor;
        }
    }

    /**
     * 待写快照；同一维度被新版本覆盖时旧对象自然被替换。
     */
    private record PendingWrite(String fileName, String content) {
    }
}
