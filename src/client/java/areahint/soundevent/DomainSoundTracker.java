package areahint.soundevent;

import areahint.config.ClientConfig;
import areahint.data.AreaData;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * 追踪玩家当前显示的域名身份，并保证一次状态变化只播放一次声音。
 */
public final class DomainSoundTracker {
    private static String currentDomainKey;

    private DomainSoundTracker() {
    }

    /**
     * 接收域名检测器发布的状态。
     * @param area 当前普通域名，null 表示位于维度域名
     * @param dimension 当前维度
     */
    public static synchronized void update(AreaData area, Identifier dimension) {
        if (dimension == null || !ClientConfig.isEnabled()) {
            currentDomainKey = null;
            return;
        }

        String dimensionId = dimension.toString();
        String nextKey = area == null
                ? "dimension:" + dimensionId
                : "area:" + dimensionId + ":" + String.valueOf(area.getName());
        if (Objects.equals(currentDomainKey, nextKey)) {
            return;
        }

        currentDomainKey = nextKey;
        SoundEventManager.playConfiguredSound();
    }

    /**
     * 断线或关闭模组时静默清理状态。
     */
    public static synchronized void reset() {
        currentDomainKey = null;
    }
}
