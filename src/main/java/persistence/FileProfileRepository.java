package persistence;

import application.ProfileRepository;
import domain.model.PlayerProfile;
import domain.model.StoredProfile;
import domain.model.PasswordHash;
import domain.model.CompletedStatistics;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Versioned UTF-8 identity catalog. The application owns single-writer coordination. */
public final class FileProfileRepository implements ProfileRepository {
    private final StatisticsProperties statistics = new StatisticsProperties();
    private final Path directory;
    private final Path catalog;

    /** Selects an application-data directory without reading or writing it. */
    public FileProfileRepository(Path directory) {
        if (directory == null) {
            throw new IllegalArgumentException("A profile directory is required.");
        }
        this.directory = directory;
        this.catalog = directory.resolve("profiles.properties");
    }

    @Override
    public List<StoredProfile> load() throws IOException {
        try (Reader input = Files.newBufferedReader(catalog, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(input);
            return readProfiles(properties);
        } catch (NoSuchFileException absent) {
            return List.of();
        } catch (IllegalArgumentException invalid) {
            throw new IOException("The local profile catalog is invalid.", invalid);
        }
    }

    private List<StoredProfile> readProfiles(Properties properties) {
        int count = requireSchema(properties);
        boolean legacy = "2".equals(properties.getProperty("formatVersion"));
        List<StoredProfile> profiles = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long expectedProperties = 2L;
        for (int index = 0; index < count; index++) {
            StoredProfile profile = readProfile(properties, "profile." + index, legacy);
            requireUnique(profile, seen);
            profiles.add(profile);
            expectedProperties += 4L
                    + (legacy ? 0 : statistics.propertyCount(profile.getStatistics()));
        }
        if (properties.size() != expectedProperties) {
            throw new IllegalArgumentException("Unexpected or missing profile properties.");
        }
        return List.copyOf(profiles);
    }

    private StoredProfile readProfile(Properties properties, String prefix, boolean legacy) {
        return new StoredProfile(
                new PlayerProfile(properties.getProperty(prefix + ".id"),
                        properties.getProperty(prefix + ".name")),
                new PasswordHash(properties.getProperty(prefix + ".salt"),
                        properties.getProperty(prefix + ".hash")),
                legacy ? new CompletedStatistics() : statistics.read(properties, prefix));
    }

    private static int requireSchema(Properties properties) {
        String version = properties.getProperty("formatVersion");
        if (!"2".equals(version) && !"3".equals(version)) {
            throw new IllegalArgumentException("Unsupported profile catalog version.");
        }
        int count = Integer.parseInt(properties.getProperty("profileCount"));
        if (count < 0) {
            throw new IllegalArgumentException("Profile records do not match the catalog count.");
        }
        return count;
    }

    @Override
    public void save(List<StoredProfile> profiles) throws IOException {
        writeCatalog(writeProfiles(profiles));
    }

    private Properties writeProfiles(List<StoredProfile> profiles) {
        if (profiles == null) {
            throw new IllegalArgumentException("A profile collection is required.");
        }
        Properties properties = new Properties();
        properties.setProperty("formatVersion", "3");
        properties.setProperty("profileCount", Integer.toString(profiles.size()));
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < profiles.size(); index++) {
            StoredProfile profile = profiles.get(index);
            requireUnique(profile, seen);
            String prefix = "profile." + index;
            properties.setProperty(prefix + ".id", profile.getProfile().getProfileId());
            properties.setProperty(prefix + ".name", profile.getProfile().getDisplayName());
            properties.setProperty(prefix + ".salt", profile.getPasswordHash().getSaltBase64());
            properties.setProperty(prefix + ".hash", profile.getPasswordHash().getHashBase64());
            statistics.write(properties, prefix, profile.getStatistics());
        }
        return properties;
    }

    private static void requireUnique(StoredProfile profile, Set<String> seen) {
        if (profile == null || !seen.add(profile.getProfile().getProfileId())) {
            throw new IllegalArgumentException("Profiles must be non-null with unique IDs.");
        }
    }

    private void writeCatalog(Properties properties) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "profiles-", ".tmp");
        try {
            writeTemporary(temporary, properties);
            Files.move(temporary, catalog, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeTemporary(Path temporary, Properties properties) throws IOException {
        try (Writer output = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            properties.store(output, "CS380 local password-protected profiles");
        }
    }
}
