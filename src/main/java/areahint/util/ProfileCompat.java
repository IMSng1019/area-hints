package areahint.util;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** Authlib 7 将档案改为 record，保持签名者和消息身份使用账号档案而非可变显示名称。 */
public final class ProfileCompat {
    private ProfileCompat() {
    }

    public static String name(GameProfile profile) {
        //? if >=1.21.9 {
        /*return profile.name();*/
        //?} else {
        return profile.getName();
        //?}
    }

    public static UUID id(GameProfile profile) {
        //? if >=1.21.9 {
        /*return profile.id();*/
        //?} else {
        return profile.getId();
        //?}
    }
}
