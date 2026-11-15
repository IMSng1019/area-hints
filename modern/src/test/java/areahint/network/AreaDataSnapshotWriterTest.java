package areahint.network;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import static org.junit.jupiter.api.Assertions.*;

class AreaDataSnapshotWriterTest {
    @TempDir(factory = WorkspaceTempFactory.class) Path folder;

    // 在当前版本的 Gradle 缓存中隔离临时文件，避免 Windows 用户目录短名称导致清理阶段拒绝访问。
    public static final class WorkspaceTempFactory implements TempDirFactory {
        @Override public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws IOException {
            Path parent = Path.of(".gradle", "vizedit-test-tmp").toAbsolutePath();
            Files.createDirectories(parent);
            return Files.createTempDirectory(parent, "snapshot-");
        }
    }

    @Test void delayedIntegratedClientSnapshotCannotRollBackAuthoritativeFile() throws Exception {
        Path authoritative = folder.resolve("overworld.json");
        Files.writeString(authoritative, "[{\"name\":\"new\"}]");
        Path clientAlias = folder.resolve("child/../overworld.json").normalize();
        AreaDataSnapshotWriter.write(clientAlias, "[{\"name\":\"old\"}]", authoritative);
        assertEquals("[{\"name\":\"new\"}]", Files.readString(authoritative));
    }

    @Test void remoteCacheStillReceivesSnapshotWithoutChangingServerFile() throws Exception {
        Path authoritative = folder.resolve("server.json");
        Path cache = folder.resolve("client.json");
        Files.writeString(authoritative, "[{\"name\":\"new\"}]");
        AreaDataSnapshotWriter.write(cache, "[{\"name\":\"synced\"}]", null);
        assertEquals("[{\"name\":\"synced\"}]", Files.readString(cache));
        assertEquals("[{\"name\":\"new\"}]", Files.readString(authoritative));
    }
}
