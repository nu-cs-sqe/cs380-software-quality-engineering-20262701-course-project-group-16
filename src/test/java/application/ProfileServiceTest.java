package application;

import domain.model.PlayerProfile;
import domain.model.PasswordHash;
import domain.model.StoredProfile;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceTest {
    private static final char[] PASSWORD = "correct horse battery staple".toCharArray();
    private static final String SALT = "AAECAwQFBgcICQoLDA0ODw==";
    private static final String HASH = "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=";

    @Test
    @DisplayName("PS-C01: creates and persists an automatically assigned identity")
    void createsAndPersistsAnAssignedIdentity() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        repository.data.add(stored("older", "Kevin"));
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "new-id");

        PlayerProfile created = service.create("Estella 玩家", PASSWORD.clone());

        assertEquals("new-id", created.getProfileId());
        assertEquals("Estella 玩家", created.getDisplayName());
        assertEquals(List.of("older", "new-id"), ids(service.list()));
        assertEquals("Estella 玩家", service.find("new-id").orElseThrow().getDisplayName());
    }

    @Test
    @DisplayName("PS-C02: same-name profiles retain distinct generated IDs")
    void allowsSameNamesWithDistinctIds() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        ArrayDeque<String> generated = new ArrayDeque<>(List.of("one", "two"));
        ProfileService service = new ProfileService(repository, new FakeHasher(), generated::removeFirst);

        service.create("玩家", PASSWORD.clone());
        service.create("玩家", PASSWORD.clone());

        assertEquals(List.of("one", "two"), ids(service.list()));
        assertEquals("玩家", service.find("one").orElseThrow().getDisplayName());
        assertEquals("玩家", service.find("two").orElseThrow().getDisplayName());
    }

    @Test
    @DisplayName("PS-C03: an ID collision never replaces the existing profile")
    void rejectsAnIdCollisionWithoutReplacingExistingData() {
        MemoryRepository repository = new MemoryRepository();
        repository.data.add(stored("same-id", "Original"));
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "same-id");

        assertThrows(IllegalStateException.class, () -> service.create("Replacement", PASSWORD.clone()));
        assertEquals(1, repository.data.size());
        assertEquals("Original", repository.data.get(0).getProfile().getDisplayName());
    }

    @ParameterizedTest(name = "{0}: rejects an invalid name before storage")
    @MethodSource("invalidNames")
    void rejectsInvalidNames(String caseId, String name) {
        MemoryRepository repository = new MemoryRepository();
        repository.failRead = true;
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "id");

        assertThrows(IllegalArgumentException.class, () -> service.create(name, PASSWORD.clone()), caseId);
        assertTrue(repository.data.isEmpty());
    }

    static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("PS-C04", (String) null),
                Arguments.of("PS-C05", ""),
                Arguments.of("PS-C06", " "),
                Arguments.of("PS-C07", "\t"),
                Arguments.of("PS-C08", "\n"),
                Arguments.of("PS-C09", "\u2003"));
    }

    @ParameterizedTest(name = "{0}: rejects an invalid generated ID before storage")
    @MethodSource("invalidGeneratedIds")
    void rejectsInvalidGeneratedIds(String caseId, String id) {
        MemoryRepository repository = new MemoryRepository();
        repository.failRead = true;
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> id);

        assertThrows(IllegalArgumentException.class, () -> service.create("Estella", PASSWORD.clone()), caseId);
        assertTrue(repository.data.isEmpty());
    }

    static Stream<Arguments> invalidGeneratedIds() {
        return Stream.of(
                Arguments.of("PS-C10", (String) null),
                Arguments.of("PS-C11", ""),
                Arguments.of("PS-C12", " "));
    }

    @Test
    @DisplayName("PS-C13: listing exposes an immutable independent snapshot")
    void listsAnImmutableIndependentSnapshot() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        repository.data.add(stored("one", "First"));
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "two");
        List<PlayerProfile> snapshot = service.list();

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.add(new PlayerProfile("extra", "Extra")));
        service.create("Second", PASSWORD.clone());

        assertEquals(List.of("one"), ids(snapshot));
        assertEquals(List.of("one", "two"), ids(service.list()));
    }

    @Test
    @DisplayName("PS-C14: lookup uses an exact ID, not the display name")
    void findsByExactIdentityOnly() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        repository.data.add(stored(" profile-1 ", "Estella"));
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "unused");

        assertEquals("Estella", service.find(" profile-1 ").orElseThrow().getDisplayName());
        assertTrue(service.find("profile-1").isEmpty());
        assertTrue(service.find("Estella").isEmpty());
        assertTrue(service.find("missing").isEmpty());
    }

    @ParameterizedTest(name = "{0}: rejects an invalid lookup ID")
    @MethodSource("invalidLookupIds")
    void rejectsInvalidLookupIds(String caseId, String id) {
        MemoryRepository repository = new MemoryRepository();
        repository.failRead = true;
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "unused");

        assertThrows(IllegalArgumentException.class, () -> service.find(id), caseId);
    }

    static Stream<Arguments> invalidLookupIds() {
        return Stream.of(
                Arguments.of("PS-C15", (String) null),
                Arguments.of("PS-C16", ""),
                Arguments.of("PS-C17", " "),
                Arguments.of("PS-C18", "\t"),
                Arguments.of("PS-C19", "\n"),
                Arguments.of("PS-C20", "\u2003"));
    }

    @ParameterizedTest(name = "PS-C21: propagates read failure from {0}")
    @ValueSource(strings = {"create", "find", "list"})
    void propagatesReadFailures(String operation) {
        MemoryRepository repository = new MemoryRepository();
        repository.failRead = true;
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "id");

        IOException actual = assertThrows(IOException.class, () -> {
            switch (operation) {
                case "create": service.create("Player", PASSWORD.clone()); break;
                case "find": service.find("id"); break;
                default: service.list();
            }
        });
        assertSame(repository.failure, actual);
        assertTrue(repository.data.isEmpty());
    }

    @Test
    @DisplayName("PS-C22: failed save preserves data and retry creates only one profile")
    void preservesDataAfterFailedSaveAndSupportsRetry() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        repository.data.add(stored("older", "Existing"));
        repository.failWrite = true;
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "new");

        assertSame(repository.failure,
                assertThrows(IOException.class, () -> service.create("New", PASSWORD.clone())));
        assertEquals(List.of("older"), ids(service.list()));
        assertTrue(service.find("new").isEmpty());

        repository.failWrite = false;
        service.create("New", PASSWORD.clone());
        assertEquals(List.of("older", "new"), ids(service.list()));
    }

    @Test
    @DisplayName("PS-C23: requires a repository and ID source")
    void requiresCollaborators() {
        MemoryRepository repository = new MemoryRepository();
        Supplier<String> ids = () -> "id";

        assertThrows(IllegalArgumentException.class, () -> new ProfileService(null));
        assertThrows(IllegalArgumentException.class, () -> new ProfileService(null, new FakeHasher(), ids));
        assertThrows(IllegalArgumentException.class, () -> new ProfileService(repository, new FakeHasher(), null));
    }


    @Test
    @DisplayName("PS-A01: authenticates only the selected identity with its password")
    void authenticatesOnlyMatchingCredentials() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "id");
        service.create("Estella", PASSWORD.clone());

        assertEquals("Estella", service.authenticate("id", PASSWORD.clone())
                .orElseThrow().getDisplayName());
        assertTrue(service.authenticate("id", "incorrect password".toCharArray()).isEmpty());
        assertTrue(service.authenticate("missing", PASSWORD.clone()).isEmpty());
        assertTrue(service.authenticate("id", null).isEmpty());
        assertEquals(1, service.list().size());
    }

    @Test
    @DisplayName("PS-A02: password derivation failure cannot create a partial account")
    void derivationFailureDoesNotCreateAnAccount() throws IOException, GeneralSecurityException {
        MemoryRepository repository = new MemoryRepository();
        GeneralSecurityException failure = new GeneralSecurityException("Unavailable");
        PasswordHasher unavailable = new UnavailableHasher(failure);
        ProfileService service = new ProfileService(repository, unavailable, () -> "id");

        assertSame(failure, assertThrows(GeneralSecurityException.class,
                () -> service.create("Player", PASSWORD.clone())));
        assertTrue(service.list().isEmpty());
        assertTrue(service.authenticate("missing", PASSWORD.clone()).isEmpty());
        repository.data.add(stored("existing", "Player"));
        assertSame(failure, assertThrows(GeneralSecurityException.class,
                () -> service.authenticate("existing", PASSWORD.clone())));
    }

    @Test
    @DisplayName("PS-A03: authentication validates identity and propagates storage errors")
    void validatesAuthenticationInputAndPreservesStorageFailures() {
        MemoryRepository repository = new MemoryRepository();
        ProfileService service = new ProfileService(repository, new FakeHasher(), () -> "id");
        assertThrows(IllegalArgumentException.class, () -> service.authenticate(null, PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> service.authenticate(" ", PASSWORD));
        repository.failRead = true;
        assertSame(repository.failure, assertThrows(IOException.class,
                () -> service.authenticate("id", PASSWORD.clone())));
        assertThrows(IllegalArgumentException.class,
                () -> new ProfileService(repository, null, () -> "id"));
    }

    private static StoredProfile stored(String id, String name) {
        return new StoredProfile(new PlayerProfile(id, name), new PasswordHash(SALT, HASH));
    }

    private static final class UnavailableHasher implements PasswordHasher {
        private final GeneralSecurityException failure;

        private UnavailableHasher(GeneralSecurityException failure) {
            this.failure = failure;
        }

        @Override public PasswordHash hash(char[] password) throws GeneralSecurityException {
            throw failure;
        }

        @Override public boolean matches(char[] password, PasswordHash credential)
                throws GeneralSecurityException {
            throw failure;
        }
    }

    private static final class FakeHasher implements PasswordHasher {
        @Override public PasswordHash hash(char[] password) {
            return new PasswordHash(SALT, HASH);
        }
        @Override public boolean matches(char[] password, PasswordHash credential) {
            return Arrays.equals(PASSWORD, password) && credential.getHashBase64().equals(HASH);
        }
    }

    private static List<String> ids(List<PlayerProfile> profiles) {
        return profiles.stream().map(PlayerProfile::getProfileId)
                .collect(java.util.stream.Collectors.toList());
    }

    private static final class MemoryRepository implements ProfileRepository {
        private final List<StoredProfile> data = new ArrayList<>();
        private final IOException failure = new IOException("Storage unavailable");
        private boolean failRead;
        private boolean failWrite;

        @Override public List<StoredProfile> load() throws IOException {
            if (failRead) { throw failure; }
            return new ArrayList<>(data);
        }

        @Override public void save(List<StoredProfile> profiles) throws IOException {
            if (failWrite) { throw failure; }
            data.clear();
            data.addAll(profiles);
        }
    }
}
