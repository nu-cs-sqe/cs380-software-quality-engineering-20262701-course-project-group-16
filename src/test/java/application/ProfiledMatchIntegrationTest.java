package application;

import domain.model.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.FileProfileRepository;
import static org.junit.jupiter.api.Assertions.*;

class ProfiledMatchIntegrationTest {
    private static final char[] PASSWORD = "PokerPlayer123".toCharArray();
    @TempDir Path directory;

    @Test
    void pmC01CompletedShowdownSurvivesRestartWhileNewMatchesResetStacks()
            throws IOException, GeneralSecurityException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile profile = profiles.create("玩家", PASSWORD);
        AuthenticatedProfile account = profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        session.startProfiles(profiles, List.of(account));
        String playerId = session.getView().orElseThrow().getPlayers().get(0).getId();
        assertNotEquals(profile.getProfileId(), playerId);
        assertEquals(Map.of(playerId, profile.getProfileId()), session.getProfileIds());
        assertThrows(UnsupportedOperationException.class, () -> session.getProfileIds().clear());
        for (int i = 0; i < 5; i++) act(session, ActionType.CALL);
        act(session, ActionType.CHECK);
        for (int i = 0; i < 18; i++) act(session, ActionType.CHECK);
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
        session.settleHand();
        assertEquals(1, session.getStatistics().get(playerId).getHandsPlayed());
        assertEquals(1, session.getLifetimeStatistics().get(playerId).getHandsPlayed());
        assertEquals(100, session.getLifetimeStatistics().get(playerId).getRates().get(0).orElseThrow());
        String completed = session.getView().orElseThrow().getHandId();
        session.nextHand();
        assertEquals(Map.of(playerId, profile.getProfileId()), session.getProfileIds());
        assertEquals(1, session.getLifetimeStatistics().get(playerId).getHandsPlayed());
        ProfileService reopened = new ProfileService(new FileProfileRepository(directory));
        AuthenticatedProfile login = reopened.login(profile.getProfileId(), PASSWORD).orElseThrow();
        assertEquals(1, reopened.getLifetimeStatistics(login).getHandsPlayed());
        assertEquals(100, reopened.getLifetimeStatistics(login).getRates().get(9).orElseThrow());
        assertTrue(new FileProfileRepository(directory).load().get(0).getStatistics()
                .getCompletedHandIds().contains(completed));
        SetupSession nextMatch = session();
        nextMatch.startProfiles(reopened, List.of(login));
        String nextId = nextMatch.getView().orElseThrow().getPlayers().get(0).getId();
        assertNotEquals(playerId, nextId);
        assertEquals(1000, nextMatch.getView().orElseThrow().getPlayers().get(0).getChipStack());
        assertEquals(0, nextMatch.getStatistics().get(nextId).getHandsPlayed());
        assertEquals(1, nextMatch.getLifetimeStatistics().get(nextId).getHandsPlayed());
        for (int i = 0; i < 5; i++) act(nextMatch, ActionType.FOLD);
        nextMatch.settleHand();
        assertEquals(2, reopened.getLifetimeStatistics(login).getHandsPlayed());
    }

    @Test
    void pmC02FailedSaveDoesNotPayChipsOrPublishStatsAndRetryCountsOnce()
            throws IOException, GeneralSecurityException {
        FileProfileRepository disk = new FileProfileRepository(directory);
        FailingRepository repo = new FailingRepository(disk);
        ProfileService profiles = new ProfileService(repo);
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account = profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        session.startProfiles(profiles, List.of(account));
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        SetupView before = session.getView().orElseThrow();
        byte[] saved = Files.readAllBytes(directory.resolve("profiles.properties"));
        repo.fail = true;
        assertSame(repo.failure, assertThrows(UncheckedIOException.class, session::settleHand).getCause());
        assertEquals(before.getPhase(), session.getView().orElseThrow().getPhase());
        assertEquals(before.getPot(), session.getView().orElseThrow().getPot());
        assertTrue(session.getSettlement().isEmpty());
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
        assertArrayEquals(saved, Files.readAllBytes(directory.resolve("profiles.properties")));
        assertThrows(IllegalStateException.class, session::nextHand);
        repo.fail = false;
        session.settleHand();
        assertEquals(1, profiles.getLifetimeStatistics(account).getHandsPlayed());
        assertThrows(IllegalStateException.class, session::settleHand);
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
    }

    @Test
    void pmC03RejectedRestartPreservesBindingAndUnprofiledRestartClearsIt()
            throws IOException, GeneralSecurityException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account = profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        assertTrue(session.getProfileIds().isEmpty());
        assertTrue(session.getLifetimeStatistics().isEmpty());
        session.startProfiles(profiles, List.of(account));
        Map<String, String> before = session.getProfileIds();
        session.revealCards();
        assertEquals(2, session.getView().orElseThrow().getHoleCards().size());
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(profiles, List.of()));
        assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> session.startProfiles(profiles, List.of(account, account)));
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(null, List.of(account)));
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(profiles, null));
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(profiles,
                java.util.Collections.nCopies(7, account)));
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(profiles,
                java.util.Arrays.asList((AuthenticatedProfile) null)));
        assertThrows(IllegalArgumentException.class,
                () -> session.startProfiles(new ProfileService(new FileProfileRepository(directory)), List.of(account)));
        assertEquals(before, session.getProfileIds());
        session.start(1);
        assertTrue(session.getProfileIds().isEmpty());
        assertTrue(session.getLifetimeStatistics().isEmpty());
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
    }

    @Test
    void pmC04RejectsInitializerHumanCountMismatchWithoutPublishingAMatch()
            throws IOException, GeneralSecurityException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account = profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        GameState twoHumans = new GameSetup().create(2);
        SetupSession session = new SetupSession(new GameInitializer(count -> twoHumans,
                java.util.function.UnaryOperator.identity()));
        assertThrows(IllegalArgumentException.class,
                () -> session.startProfiles(profiles, List.of(account)));
        assertTrue(session.getView().isEmpty());
        assertTrue(session.getProfileIds().isEmpty());
    }

    @Test
    void pmC05DurableSaveFollowedByReportedFailureRetriesWithoutDoubleCounting()
            throws IOException, GeneralSecurityException {
        FailingRepository repo = new FailingRepository(new FileProfileRepository(directory));
        ProfileService profiles = new ProfileService(repo);
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account = profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        session.startProfiles(profiles, List.of(account));
        for (int i = 0; i < 5; i++) act(session, ActionType.FOLD);
        repo.failAfterSave = true;
        assertThrows(UncheckedIOException.class, session::settleHand);
        assertTrue(session.getSettlement().isEmpty());
        assertTrue(session.getStatistics().values().stream().allMatch(s -> s.getHandsPlayed() == 0));
        ProfileService reopened = new ProfileService(new FileProfileRepository(directory));
        AuthenticatedProfile login = reopened.login(profile.getProfileId(), PASSWORD).orElseThrow();
        assertEquals(1, reopened.getLifetimeStatistics(login).getHandsPlayed());
        // A retry must not write again: the failing adapter remains armed.
        session.settleHand();
        assertEquals(1, profiles.getLifetimeStatistics(account).getHandsPlayed());
        assertEquals(1, session.getStatistics().get(session.getProfileIds().keySet().iterator().next())
                .getHandsPlayed());
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
    }

    private static SetupSession session() {
        Random dealer = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return 0; }
        };
        Random deck = new Random() {
            private static final long serialVersionUID = 1L;
            @Override public int nextInt(int bound) { return bound - 1; }
        };
        return new SetupSession(new GameInitializer(new GameConfig(), dealer, deck));
    }

    private static void act(SetupSession session, ActionType type) {
        SetupView view = session.getView().orElseThrow();
        session.submitAction(new ActionRequest(view.getPlayers()
                .get(view.getCurrentActorSeat()).getId(), type));
    }

    private static final class FailingRepository implements ProfileRepository {
        private final ProfileRepository delegate;
        private final IOException failure = new IOException("Disk full");
        private boolean fail;
        private boolean failAfterSave;
        private FailingRepository(ProfileRepository delegate) { this.delegate = delegate; }
        @Override public List<StoredProfile> load() throws IOException { return delegate.load(); }
        @Override public void save(List<StoredProfile> profiles) throws IOException {
            if (fail) { throw failure; }
            delegate.save(profiles);
            if (failAfterSave) { throw failure; }
        }
    }
}
