package application;

import domain.model.PlayerProfile;
import domain.model.StoredProfile;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import security.Pbkdf2PasswordHasher;

/** Creates and authenticates local accounts; the application serializes writer access. */
public final class ProfileService {
    private final ProfileRepository repository;
    private final PasswordHasher hasher;
    private final Supplier<String> ids;

    public ProfileService(ProfileRepository repository) {
        this(repository, new Pbkdf2PasswordHasher(), () -> UUID.randomUUID().toString());
    }

    /** Injects storage, hashing and IDs for isolated coordination tests. */
    public ProfileService(ProfileRepository repository, PasswordHasher hasher,
            Supplier<String> ids) {
        requireCollaborators(repository, hasher, ids);
        this.repository = repository;
        this.hasher = hasher;
        this.ids = ids;
    }

    private static void requireCollaborators(ProfileRepository repository, PasswordHasher hasher,
            Supplier<String> ids) {
        if (repository == null || hasher == null || ids == null) {
            throw new IllegalArgumentException(
                    "Storage, password hashing and ID sources are required.");
        }
    }

    /** Saves identity and credential together before returning the created profile. */
    public PlayerProfile create(String displayName, char[] password)
            throws IOException, GeneralSecurityException {
        PlayerProfile created = new PlayerProfile(ids.get(), displayName);
        List<StoredProfile> profiles = new ArrayList<>(repository.load());
        requireAvailableId(profiles, created.getProfileId());
        profiles.add(new StoredProfile(created, hasher.hash(password)));
        repository.save(List.copyOf(profiles));
        return created;
    }

    private static void requireAvailableId(List<StoredProfile> profiles, String id) {
        if (profiles.stream().anyMatch(profile -> profile.getProfile().getProfileId().equals(id))) {
            throw new IllegalStateException("The generated profile ID already exists.");
        }
    }

    /** Verifies the selected profile's password; failure returns no authenticated identity. */
    public Optional<PlayerProfile> authenticate(String profileId, char[] password)
            throws IOException, GeneralSecurityException {
        requireLookupId(profileId);
        Optional<StoredProfile> stored = findStored(profileId);
        if (stored.isPresent()
                && hasher.matches(password, stored.orElseThrow().getPasswordHash())) {
            return stored.map(StoredProfile::getProfile);
        }
        return Optional.empty();
    }

    /** Public identity lookup only; this method does not authenticate access. */
    public Optional<PlayerProfile> find(String profileId) throws IOException {
        requireLookupId(profileId);
        return findStored(profileId).map(StoredProfile::getProfile);
    }

    private Optional<StoredProfile> findStored(String profileId) throws IOException {
        return repository.load().stream()
                .filter(profile -> profile.getProfile().getProfileId().equals(profileId))
                .findFirst();
    }

    private static void requireLookupId(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            throw new IllegalArgumentException("A nonblank profile ID is required.");
        }
    }

    /** Public immutable identities for selection; credentials are never returned. */
    public List<PlayerProfile> list() throws IOException {
        return repository.load().stream().map(StoredProfile::getProfile)
                .collect(Collectors.toUnmodifiableList());
    }
}
