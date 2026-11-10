package application;

import domain.model.PlayerProfile;

/** In-memory authentication handle issued by one ProfileService; never serialized. */
public final class AuthenticatedProfile {
    private final ProfileService owner;
    private final PlayerProfile profile;

    AuthenticatedProfile(ProfileService owner, PlayerProfile profile) {
        this.owner = owner;
        this.profile = profile;
    }

    public PlayerProfile getProfile() { return profile; }
    boolean belongsTo(ProfileService service) { return owner == service; }
}
