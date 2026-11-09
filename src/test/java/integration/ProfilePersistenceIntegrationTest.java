package integration;

import application.ProfileService;
import domain.model.PlayerProfile;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.FileProfileRepository;
import security.Pbkdf2PasswordHasher;

import static org.junit.jupiter.api.Assertions.*;

class ProfilePersistenceIntegrationTest {
    private static final char[] PASSWORD = "correct horse battery staple".toCharArray();

    @TempDir Path directory;

    @Test
    @DisplayName("PI-C01: a new service reopens saved profiles with unchanged IDs")
    void reopensProfilesAcrossServiceAndRepositoryInstances() throws IOException, GeneralSecurityException {
        ProfileService first = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile english = first.create("Estella", PASSWORD.clone());
        PlayerProfile chinese = first.create("玩家一", PASSWORD.clone());
        UUID.fromString(english.getProfileId());
        UUID.fromString(chinese.getProfileId());
        assertNotEquals(english.getProfileId(), chinese.getProfileId());

        ProfileService reopened = new ProfileService(new FileProfileRepository(directory), new Pbkdf2PasswordHasher(),
                () -> { throw new AssertionError("Opening a profile must not generate an ID"); });
        assertEquals(2, reopened.list().size());
        assertTrue(reopened.authenticate(english.getProfileId(),
                "incorrect password".toCharArray()).isEmpty());
        assertTrue(reopened.authenticate("missing", PASSWORD.clone()).isEmpty());
        String catalog = Files.readString(directory.resolve("profiles.properties"));
        assertFalse(catalog.contains("correct horse battery staple"));
        var records = new FileProfileRepository(directory).load();
        assertNotEquals(records.get(0).getPasswordHash().getSaltBase64(),
                records.get(1).getPasswordHash().getSaltBase64());
        assertEquals("Estella", reopened.authenticate(english.getProfileId(), PASSWORD.clone())
                .orElseThrow().getDisplayName());
        assertEquals(chinese.getProfileId(), reopened.find(chinese.getProfileId())
                .orElseThrow().getProfileId());
        assertEquals("玩家一", reopened.authenticate(chinese.getProfileId(), PASSWORD.clone())
                .orElseThrow().getDisplayName());
    }

    @Test
    @DisplayName("PI-C02: a corrupt existing catalog is never overwritten by creation")
    void doesNotOverwriteACorruptCatalogWhenCreating() throws IOException, GeneralSecurityException {
        Path catalog = directory.resolve("profiles.properties");
        Files.writeString(catalog, "corrupt");
        ProfileService service = new ProfileService(new FileProfileRepository(directory), new Pbkdf2PasswordHasher(),
                () -> "new");

        assertThrows(IOException.class, () -> service.create("Player", PASSWORD.clone()));
        assertEquals("corrupt", Files.readString(catalog));
    }

    @Test
    @DisplayName("PI-C03: loaded data remains unchanged when another profile is created")
    void preservesEarlierProfilesAndDetachedSnapshots() throws IOException, GeneralSecurityException {
        ProfileService first = new ProfileService(new FileProfileRepository(directory), new Pbkdf2PasswordHasher(), () -> "one");
        PlayerProfile original = first.create("First", PASSWORD.clone());
        List<PlayerProfile> snapshot = first.list();
        ProfileService second = new ProfileService(new FileProfileRepository(directory), new Pbkdf2PasswordHasher(), () -> "two");
        second.create("Second", PASSWORD.clone());

        assertEquals(1, snapshot.size());
        assertEquals("one", original.getProfileId());
        assertEquals("First", original.getDisplayName());
        assertEquals(2, first.list().size());
        assertTrue(second.find("missing").isEmpty());
    }
    @Test
    @DisplayName("PI-C04: invalid passwords cannot create a passwordless or partial account")
    void rejectsInvalidPasswordsWithoutSavingAnAccount() throws IOException {
        ProfileService service = new ProfileService(new FileProfileRepository(directory));

        assertThrows(IllegalArgumentException.class, () -> service.create("Player", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.create("Player", "a".repeat(14).toCharArray()));

        assertTrue(service.list().isEmpty());
        assertFalse(Files.exists(directory.resolve("profiles.properties")));
    }
}
