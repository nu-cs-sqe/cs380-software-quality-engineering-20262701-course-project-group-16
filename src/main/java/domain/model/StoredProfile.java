package domain.model;

/** Identity, credential and completed lifetime data published in one local catalog. */
public final class StoredProfile {
    private final PlayerProfile profile;
    private final PasswordHash passwordHash;
    private final CompletedStatistics statistics;

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash) {
        this(profile, passwordHash, new CompletedStatistics());
    }

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash,
            CompletedStatistics statistics) {
        this.profile = profile;
        this.passwordHash = passwordHash;
        this.statistics = statistics;
    }

    public PlayerProfile getProfile() { return profile; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public CompletedStatistics getStatistics() { return statistics; }
}
