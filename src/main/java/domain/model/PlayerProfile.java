package domain.model;

/**
 * Immutable human profile identity, independent of match seats, stacks and pending hands.
 * Creation services generate unique IDs; loading supplies the previously stored ID.
 */
public final class PlayerProfile {
    private final String profileId;
    private final String displayName;

    /**
     * Retains opaque identity and display text without trimming or normalization.
     * Display names need not be unique; registration and uniqueness belong to a service.
     *
     * @param profileId existing nonblank identifier, supplied by creation or loading
     * @param displayName nonblank human-readable name in any supported script
     * @throws IllegalArgumentException if either input is null or blank
     */
    public PlayerProfile(String profileId, String displayName) {
        this.profileId = requireNonBlank(profileId, "Profile ID");
        this.displayName = requireNonBlank(displayName, "Display name");
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must contain a non-whitespace character.");
        }
        return value;
    }

    /** Returns the stable profile ID exactly as supplied. */
    public String getProfileId() { return profileId; }

    /** Returns the display name exactly as supplied. */
    public String getDisplayName() { return displayName; }
}
