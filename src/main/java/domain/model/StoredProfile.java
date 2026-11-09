package domain.model;

/** Immutable identity and credential published together in one local account catalog. */
public final class StoredProfile {
    private final PlayerProfile profile;
    private final PasswordHash passwordHash;

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash) {
        requireIdentityAndCredential(profile, passwordHash);
        this.profile = profile;
        this.passwordHash = passwordHash;
    }

    private static void requireIdentityAndCredential(PlayerProfile profile,
            PasswordHash passwordHash) {
        if (profile == null || passwordHash == null) {
            throw new IllegalArgumentException("A stored identity and credential are required.");
        }
    }

    public PlayerProfile getProfile() { return profile; }
    public PasswordHash getPasswordHash() { return passwordHash; }
}
