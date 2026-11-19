package domain.model;

/** Identity, credential and completed lifetime data published in one local catalog. */
public final class StoredProfile {
    private static final long INITIAL_CHIP_BALANCE = 2_000;
    private final PlayerProfile profile;
    private final PasswordHash passwordHash;
    private final CompletedStatistics statistics;
    private final long chipBalance;

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash) {
        this(profile, passwordHash, new CompletedStatistics());
    }

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash,
            CompletedStatistics statistics) {
        this(profile, passwordHash, statistics, INITIAL_CHIP_BALANCE);
    }

    public StoredProfile(PlayerProfile profile, PasswordHash passwordHash,
            CompletedStatistics statistics, long chipBalance) {
        if (chipBalance < 0) {
            throw new IllegalArgumentException("A chip balance cannot be negative.");
        }
        this.profile = profile;
        this.passwordHash = passwordHash;
        this.statistics = statistics;
        this.chipBalance = chipBalance;
    }

    public PlayerProfile getProfile() { return profile; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public CompletedStatistics getStatistics() { return statistics; }
    public long getChipBalance() { return chipBalance; }
}
