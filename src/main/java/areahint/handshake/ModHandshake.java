package areahint.handshake;

import areahint.Areashint;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.util.Identifier;

// 登录握手独立于游玩阶段自定义载荷，所有受支持版本都在玩家进入世界前完成校验。
public final class ModHandshake {
    public static final String DOWNLOAD_URL = "https://modrinth.com/mod/area-hints";
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_VERSION_LENGTH = 128;
    //? if <1.21 {
    public static final Identifier CHANNEL = new Identifier(Areashint.MOD_ID, "required_mod_handshake");
    //?} else {
    /*public static final Identifier CHANNEL = Identifier.of(Areashint.MOD_ID, "required_mod_handshake");*/
    //?}

    private ModHandshake() { }

    public static String localVersion() {
        return FabricLoader.getInstance().getModContainer(Areashint.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("无法读取域名模组版本"))
                .getMetadata().getVersion().getFriendlyString();
    }

    public static boolean isCompatible(String clientVersion, String serverVersion) {
        try {
            // 使用语义版本比较，正确处理 4.10 高于 4.9、预发布低于正式版等情况。
            return SemanticVersion.parse(clientVersion).compareTo(SemanticVersion.parse(serverVersion)) >= 0;
        } catch (VersionParsingException | IllegalArgumentException e) {
            return false;
        }
    }
}
