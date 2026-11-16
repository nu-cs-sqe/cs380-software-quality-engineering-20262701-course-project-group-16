package domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoredProfileTest {
    @Test
    void spC02RetainsImmutableIdentityAndCredential() {
        PlayerProfile profile = new PlayerProfile("id", "玩家");
        PasswordHash hash = new PasswordHash("AAECAwQFBgcICQoLDA0ODw==",
                "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=");
        StoredProfile stored = new StoredProfile(profile, hash);
        assertSame(profile, stored.getProfile());
        assertSame(hash, stored.getPasswordHash());
    }
}
