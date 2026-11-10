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
        requireRecord(profile, passwordHash, statistics);
        this.profile = profile;
        this.passwordHash = passwordHash;
        this.statistics = statistics;
    }

    private static void requireRecord(PlayerProfile profile, PasswordHash passwordHash,
            CompletedStatistics statistics) {
        if (profile == null || passwordHash == null || statistics == null) {
            throw new IllegalArgumentException("Identity, credential and statistics are required.");
        }
    }

    public PlayerProfile getProfile() { return profile; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public CompletedStatistics getStatistics() { return statistics; }
}
