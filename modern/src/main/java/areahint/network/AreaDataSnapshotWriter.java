package areahint.network;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 客户端快照写入与服务端权威文件分开判断，避免集成服延迟快照覆盖已保存的新数据。
public final class AreaDataSnapshotWriter {
    private AreaDataSnapshotWriter() { }
    public static void write(Path cache, String snapshot, Path authoritativeFile) throws IOException {
        // 集成服的客户端缓存可能就是服务端文件，排队较早的同步快照绝不能回滚较新的原子保存。
        if (authoritativeFile != null && (cache.toAbsolutePath().normalize().equals(authoritativeFile.toAbsolutePath().normalize())
                || Files.exists(cache) && Files.exists(authoritativeFile) && Files.isSameFile(cache, authoritativeFile))) return;
        Files.writeString(cache, snapshot);
    }
}
