package application;

import domain.model.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifetimeStatisticsTest {
    private static final char[] PASSWORD = "PokerPlayer123".toCharArray();
    private static final PasswordHash HASH = new PasswordHash("AAECAwQFBgcICQoLDA0ODw==",
            "7xdxRO7JQgy8EJPSqLNEqSvFBtDU7JwCjdGfgyTYweY=");

    @Test
    void lsC01OnlyVerifiedHandlesCanReadLifetimeStatistics()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        ProfileService service = service(repo);
        PlayerProfile profile = service.create("Player", PASSWORD);
        assertTrue(service.login(profile.getProfileId(), new char[0]).isEmpty());
        AuthenticatedProfile account = service.login("profile", PASSWORD).orElseThrow();
        assertEquals(profile.getDisplayName(), account.getProfile().getDisplayName());
        assertEquals(0, service.getLifetimeStatistics(account).getHandsPlayed());
        assertThrows(IllegalArgumentException.class, () -> service.getLifetimeStatistics(null));
        ProfileService other = service(repo);
        assertThrows(IllegalArgumentException.class, () -> other.getLifetimeStatistics(account));
        repo.data.clear();
        assertThrows(IllegalArgumentException.class, () -> service.getLifetimeStatistics(account));
    }

    @Test
    void lsC02PublishesAllProfilesTogetherAndDoesNotWriteDuplicateHands()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        ProfileService service = service(repo);
        service.create("Player", PASSWORD);
        AuthenticatedProfile account = service.login("profile", PASSWORD).orElseThrow();
        CompletedStatistics hand = hand("one");
        service.saveCompleted(Map.of("match-player", account), Map.of("match-player", hand));
        int writes = repo.writes;
        assertEquals(1, service.getLifetimeStatistics(account).getHandsPlayed());
        service.saveCompleted(Map.of("match-player", account), Map.of("match-player", hand));
        assertEquals(writes, repo.writes);
        assertEquals(1, service.getLifetimeStatistics(account).getHandsPlayed());
        assertEquals(HASH.getHashBase64(), repo.data.get(0).getPasswordHash().getHashBase64());
    }

    @Test
    void lsC03FailedWriteIsRetryableWithoutPublishingTotals()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        ProfileService service = service(repo);
        service.create("Player", PASSWORD);
        AuthenticatedProfile account = service.login("profile", PASSWORD).orElseThrow();
        repo.fail = true;
        assertSame(repo.failure, assertThrows(IOException.class, () -> service.saveCompleted(
                Map.of("p", account), Map.of("p", hand("one")))));
        assertEquals(0, service.getLifetimeStatistics(account).getHandsPlayed());
        repo.fail = false;
        service.saveCompleted(Map.of("p", account), Map.of("p", hand("one")));
        assertEquals(1, service.getLifetimeStatistics(account).getHandsPlayed());
    }

    @Test
    void lsC04InvalidBindingOrMissingHandCannotPartiallySave()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        ProfileService service = service(repo);
        service.create("Player", PASSWORD);
        AuthenticatedProfile account = service.login("profile", PASSWORD).orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.saveCompleted(Map.of("p", account), Map.of()));
        assertEquals(0, service.getLifetimeStatistics(account).getHandsPlayed());
        AuthenticatedProfile foreign = service(repo).login("profile", PASSWORD).orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> service.saveCompleted(Map.of("p", foreign), Map.of("p", hand("one"))));
        repo.data.clear();
        assertThrows(IllegalArgumentException.class,
                () -> service.saveCompleted(Map.of("p", account), Map.of("p", hand("one"))));
    }

    @Test
    void lsC05WritesTwoAccountsAtomicallyAndPreservesUnboundAccounts()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        java.util.ArrayDeque<String> ids = new java.util.ArrayDeque<>(List.of("a", "b", "c"));
        ProfileService service = new ProfileService(repo, new FixedHasher(), ids::removeFirst);
        for (int i = 0; i < 3; i++) service.create("Player", PASSWORD);
        AuthenticatedProfile a = service.login("a", PASSWORD).orElseThrow();
        AuthenticatedProfile b = service.login("b", PASSWORD).orElseThrow();
        AuthenticatedProfile c = service.login("c", PASSWORD).orElseThrow();
        Map<String, AuthenticatedProfile> accounts = new java.util.LinkedHashMap<>();
        accounts.put("p0", a);
        accounts.put("p1", b);
        int writes = repo.writes;
        assertThrows(IllegalArgumentException.class,
                () -> service.saveCompleted(accounts, Map.of("p0", hand("first"))));
        assertEquals(writes, repo.writes);
        assertEquals(0, service.getLifetimeStatistics(a).getHandsPlayed());
        assertEquals(0, service.getLifetimeStatistics(b).getHandsPlayed());
        service.saveCompleted(accounts, Map.of("p0", hand("first"), "p1", hand("first")));
        assertEquals(writes + 1, repo.writes);
        assertEquals(1, service.getLifetimeStatistics(a).getHandsPlayed());
        assertEquals(1, service.getLifetimeStatistics(b).getHandsPlayed());
        assertEquals(0, service.getLifetimeStatistics(c).getHandsPlayed());
    }

    @Test
    void lsC06SixDistinctAccountsAreAcceptedAndSevenDistinctAccountsRejected()
            throws IOException, GeneralSecurityException {
        MemoryRepository repo = new MemoryRepository();
        java.util.ArrayDeque<String> ids = new java.util.ArrayDeque<>(
                List.of("a", "b", "c", "d", "e", "f", "g"));
        ProfileService service = new ProfileService(repo, new FixedHasher(), ids::removeFirst);
        List<AuthenticatedProfile> accounts = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            PlayerProfile profile = service.create("Player", PASSWORD);
            accounts.add(service.login(profile.getProfileId(), PASSWORD).orElseThrow());
        }
        assertDoesNotThrow(() -> ProfileMatch.requireAccounts(service, accounts.subList(0, 6)));
        assertThrows(IllegalArgumentException.class,
                () -> ProfileMatch.requireAccounts(service, accounts));
        GameState state = new GameSetup().create(6);
        ProfileMatch binding = new ProfileMatch(service, accounts.subList(0, 6), state);
        assertEquals(6, binding.profileIds().size());
        assertEquals("f", binding.profileIds().get(state.getPlayers().get(5).getId()));
    }

    private static CompletedStatistics hand(String id) {
        PlayStats stats = new PlayStats();
        stats.beginHand(id, true);
        stats.completeHand(id, false, false, 0, 0);
        return stats.completedSnapshot();
    }

    private static ProfileService service(ProfileRepository repository) {
        return new ProfileService(repository, new FixedHasher(), () -> "profile");
    }

    private static final class FixedHasher implements PasswordHasher {
        @Override public PasswordHash hash(char[] password) { return HASH; }
        @Override public boolean matches(char[] password, PasswordHash hash) {
            return java.util.Arrays.equals(PASSWORD, password);
        }
    }

    private static final class MemoryRepository implements ProfileRepository {
        private final List<StoredProfile> data = new ArrayList<>();
        private final IOException failure = new IOException("Disk unavailable");
        private boolean fail;
        private int writes;
        @Override public List<StoredProfile> load() { return new ArrayList<>(data); }
        @Override public void save(List<StoredProfile> profiles) throws IOException {
            if (fail) { throw failure; }
            data.clear();
            data.addAll(profiles);
            writes++;
        }
    }
}
