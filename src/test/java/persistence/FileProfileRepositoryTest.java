package persistence;

import domain.model.PlayerProfile;
import domain.model.StoredProfile;
import domain.model.PasswordHash;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class FileProfileRepositoryTest {
    private static final String SALT = "AAECAwQFBgcICQoLDA0ODw==";
    private static final String HASH = "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=";

    @TempDir Path temporary;

    @Test
    @DisplayName("FP-C01: a missing catalog is empty and reading creates no directory")
    void missingCatalogIsEmptyWithoutCreatingDirectories() throws IOException {
        Path directory = temporary.resolve("not-created");
        assertTrue(new FileProfileRepository(directory).load().isEmpty());
        assertFalse(Files.exists(directory));
    }

    @Test
    @DisplayName("FP-C02: an explicitly saved empty catalog reloads")
    void savesAndReloadsAnEmptyCatalog() throws IOException {
        new FileProfileRepository(temporary).save(List.of());

        assertTrue(Files.isRegularFile(catalog()));
        assertTrue(new FileProfileRepository(temporary).load().isEmpty());
    }

    @Test
    @DisplayName("FP-C03: Unicode, spaces and property punctuation survive reload")
    void preservesOpaqueIdsAndUnicodeTextAcrossInstances() throws IOException {
        Path directory = temporary.resolve("store");
        List<StoredProfile> profiles = List.of(
                stored("../opaque/id", " 玩家一 "),
                stored("second", "Zoë 🃏\nline\t:=#!\\"),
                stored("third", "玩家一"));
        new FileProfileRepository(directory).save(profiles);

        List<StoredProfile> restored = new FileProfileRepository(directory).load();
        assertEquals(3, restored.size());
        for (int index = 0; index < profiles.size(); index++) {
            assertEquals(profiles.get(index).getProfile().getProfileId(), restored.get(index).getProfile().getProfileId());
            assertEquals(profiles.get(index).getProfile().getDisplayName(), restored.get(index).getProfile().getDisplayName());
        }
        assertThrows(UnsupportedOperationException.class,
                () -> restored.add(stored("extra", "Extra")));
        assertFalse(Files.exists(temporary.resolve("opaque")));
    }

    @Test
    @DisplayName("FP-C04: a new complete catalog replaces the old snapshot")
    void replacesTheCatalogWithoutLeavingTemporaryFiles() throws IOException {
        FileProfileRepository repository = new FileProfileRepository(temporary);
        repository.save(List.of(stored("one", "Old")));
        repository.save(List.of(stored("one", "New"),
                stored("two", "Second")));

        List<StoredProfile> restored = new FileProfileRepository(temporary).load();
        assertEquals(2, restored.size());
        assertEquals("New", restored.get(0).getProfile().getDisplayName());
        assertEquals("two", restored.get(1).getProfile().getProfileId());
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("FP-C05: invalid replacement data cannot overwrite a saved catalog")
    void rejectsInvalidReplacementDataBeforeWriting() throws IOException {
        FileProfileRepository repository = new FileProfileRepository(temporary);
        repository.save(List.of(stored("saved", "Original")));
        byte[] before = Files.readAllBytes(catalog());

        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        assertThrows(IllegalArgumentException.class,
                () -> repository.save(Arrays.asList((StoredProfile) null)));
        assertThrows(IllegalArgumentException.class, () -> repository.save(List.of(
                stored("duplicate", "First"),
                stored("duplicate", "Second"))));

        assertArrayEquals(before, Files.readAllBytes(catalog()));
        assertEquals("Original", repository.load().get(0).getProfile().getDisplayName());
        assertNoTemporaryFiles();
    }

    @ParameterizedTest(name = "{0}: rejects a corrupt catalog without changing it")
    @MethodSource("corruptCatalogs")
    void rejectsCorruptCatalogsWithoutResettingData(String caseId, String contents)
            throws IOException {
        Files.writeString(catalog(), contents, StandardCharsets.UTF_8);
        byte[] before = Files.readAllBytes(catalog());

        assertThrows(IOException.class, () -> new FileProfileRepository(temporary).load(), caseId);
        assertArrayEquals(before, Files.readAllBytes(catalog()), caseId);
    }

    static Stream<Arguments> corruptCatalogs() {
        return Stream.of(
                Arguments.of("FP-C06", ""),
                Arguments.of("FP-C07", "formatVersion=99\nprofileCount=0\n"),
                Arguments.of("FP-C08", "formatVersion=2\nprofileCount=-1\n"),
                Arguments.of("FP-C09", "formatVersion=2\nprofileCount=oops\n"),
                Arguments.of("FP-C10", "formatVersion=2\nprofileCount=2147483647\n"),
                Arguments.of("FP-C11", "formatVersion=2\n"),
                Arguments.of("FP-C12", "profileCount=0\n"),
                Arguments.of("FP-C13", header(1) + "profile.0.id=one\n"),
                Arguments.of("FP-C14", header(1) + "profile.0.id= \nprofile.0.name=Name\n"),
                Arguments.of("FP-C15", header(1) + "profile.0.id=one\nprofile.0.name= \n"),
                Arguments.of("FP-C16", header(2)
                        + "profile.0.id=same\nprofile.0.name=First\n"
                        + "profile.1.id=same\nprofile.1.name=Second\n"),
                Arguments.of("FP-C17", header(0) + "unexpected=value\n"),
                Arguments.of("FP-C18", header(1)
                        + "profile.0.id=one\nprofile.0.name=Name\nextra=value\n"),
                Arguments.of("FP-C19", header(0) + "bad=\\u12GG\n"),
                Arguments.of("FP-C20", header(1)
                        + "wrong.id=one\nprofile.0.name=Name\n"));
    }

    @ParameterizedTest(name = "{0}: rejects damaged credentials without changing the catalog")
    @MethodSource("damagedCredentials")
    void rejectsDamagedCredentials(String caseId, String contents) throws IOException {
        rejectsCorruptCatalogsWithoutResettingData(caseId, contents);
    }

    static Stream<Arguments> damagedCredentials() {
        String valid = header(1) + "profile.0.id=one\nprofile.0.name=Player\n";
        return Stream.of(
                Arguments.of("FP-C26", valid.replace("profile.0.salt=", "unknown.salt=")),
                Arguments.of("FP-C27", valid.replace("profile.0.hash=", "unknown.hash=")),
                Arguments.of("FP-C28", valid.replace(SALT, "invalid!")),
                Arguments.of("FP-C29", valid.replace(HASH,
                        java.util.Base64.getEncoder().encodeToString(new byte[31]))),
                Arguments.of("FP-C30", "formatVersion=1\nprofileCount=0\n"));
    }

    @Test
    @DisplayName("FP-C21: invalid UTF-8 is a read failure")
    void rejectsInvalidUtf8() throws IOException {
        Files.write(catalog(), new byte[] {(byte) 0xC3, 0x28});
        assertThrows(IOException.class, () -> new FileProfileRepository(temporary).load());
    }

    @Test
    @DisplayName("FP-C22: an unreadable catalog shape is not treated as empty")
    void reportsACatalogDirectoryAsAReadError() throws IOException {
        Files.createDirectory(catalog());
        Files.writeString(catalog().resolve("keep"), "keep");
        assertThrows(IOException.class, () -> new FileProfileRepository(temporary).load());
        assertEquals("keep", Files.readString(catalog().resolve("keep")));
    }

    @Test
    @DisplayName("FP-C23: a file cannot be used as the storage directory")
    void reportsAnInvalidDirectoryWithoutChangingIt() throws IOException {
        Path file = temporary.resolve("ordinary-file");
        Files.writeString(file, "keep");
        assertThrows(IOException.class, () -> new FileProfileRepository(file)
                .save(List.of(stored("one", "Name"))));
        assertEquals("keep", Files.readString(file));
    }

    @Test
    @DisplayName("FP-C24: failed atomic replacement preserves the target and cleans up")
    void preservesTheTargetAfterFailedReplacement() throws IOException {
        Files.createDirectory(catalog());
        Files.writeString(catalog().resolve("keep"), "keep");

        assertThrows(IOException.class, () -> new FileProfileRepository(temporary)
                .save(List.of(stored("one", "Name"))));
        assertEquals("keep", Files.readString(catalog().resolve("keep")));
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("FP-C25: requires a storage directory")
    void rejectsANullDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new FileProfileRepository(null));
    }

    private static StoredProfile stored(String id, String name) {
        return new StoredProfile(new PlayerProfile(id, name), new PasswordHash(SALT, HASH));
    }

    private Path catalog() { return temporary.resolve("profiles.properties"); }

    private static String header(int count) {
        StringBuilder result = new StringBuilder("formatVersion=2\nprofileCount=" + count + "\n");
        for (int index = 0; index < count; index++) {
            result.append("profile.").append(index).append(".salt=").append(SALT).append('\n');
            result.append("profile.").append(index).append(".hash=").append(HASH).append('\n');
        }
        return result.toString();
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (Stream<Path> files = Files.list(temporary)) {
            assertTrue(files.noneMatch(path -> path.toString().endsWith(".tmp")));
        }
    }
}
