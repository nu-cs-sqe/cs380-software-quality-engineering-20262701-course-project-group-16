package application;

import domain.model.*;
import bot.MonteCarloEquityEstimator;
import bot.UniformDealSampler;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        assertThrows(IllegalArgumentException.class, () -> session.startProfiles(profiles,
                java.util.Collections.nCopies(7, account)));
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

    @Test
    void pmC06RaisedProfileHandPersistsExactStatisticsAfterRestart()
            throws IOException, GeneralSecurityException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account =
                profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        session.startProfiles(profiles, List.of(account));
        String playerId = session.getView().orElseThrow().getPlayers().get(0).getId();
        wager(session, ActionType.RAISE, 40); // Seat 3 opens.
        act(session, ActionType.CALL); // Seat 4.
        act(session, ActionType.CALL); // Seat 5.
        wager(session, ActionType.RAISE, 80); // Profile at seat 0 three-bets.
        while (session.getView().orElseThrow().getPhase() == GamePhase.PREFLOP) {
            act(session, session.getAvailableActions().contains(ActionType.CALL)
                    ? ActionType.CALL : ActionType.CHECK);
        }
        for (int index = 0; index < 5; index++) {
            act(session, ActionType.CHECK);
        }
        wager(session, ActionType.BET, 40); // Final preflop aggressor makes a flop c-bet.
        for (int index = 0; index < 5; index++) {
            act(session, ActionType.CALL);
        }
        while (session.getResult().isEmpty()) {
            act(session, ActionType.CHECK);
        }
        assertEquals(720, session.getView().orElseThrow().getPot());
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
        session.settleHand();
        StatisticsView current = session.getStatistics().get(playerId);
        assertEquals(1, current.getHandsPlayed());
        for (int metric : new int[]{0, 1, 3, 6}) {
            assertEquals(100, current.getRates().get(metric).orElseThrow());
        }
        ProfileService reopened = new ProfileService(new FileProfileRepository(directory));
        AuthenticatedProfile login = reopened.login(profile.getProfileId(), PASSWORD).orElseThrow();
        StatisticsView persisted = reopened.getLifetimeStatistics(login);
        assertEquals(current.getHandsPlayed(), persisted.getHandsPlayed());
        assertEquals(current.getRates(), persisted.getRates());
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
        session.nextHand();
        assertEquals(Map.of(playerId, profile.getProfileId()), session.getProfileIds());
        assertEquals(1, profiles.getLifetimeStatistics(account).getHandsPlayed());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pmC07AdaptiveProfileHandPreservesLearningAndRetriesSettlement(boolean afterSave)
            throws IOException, GeneralSecurityException {
        FailingRepository repository = new FailingRepository(new FileProfileRepository(directory));
        ProfileService profiles = new ProfileService(repository);
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account =
                profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = new SetupSession(BotFixtures.initializer());
        session.startProfiles(profiles, List.of(account));
        Map<String, String> bindings = session.getProfileIds();
        String player = bindings.keySet().iterator().next();
        String observer = session.getView().orElseThrow().getPlayers().get(5).getId();
        playAdaptiveHand(session);
        Map<String, bot.learning.OpponentBeliefs> beliefs = session.getBotBeliefs(observer);
        assertTrue(beliefs.values().stream().anyMatch(b -> b.getObservedDecisions() > 0));
        SetupView before = session.getView().orElseThrow();
        repository.fail = !afterSave;
        repository.failAfterSave = afterSave;
        assertThrows(UncheckedIOException.class, session::settleHand);
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(before.getPlayers(), session.getView().orElseThrow().getPlayers());
        assertEquals(before.getPot(), session.getView().orElseThrow().getPot());
        assertEquals(0, session.getStatistics().get(player).getHandsPlayed());
        assertEquals(afterSave ? 1 : 0, profiles.getLifetimeStatistics(account).getHandsPlayed());
        assertEquals(beliefs, session.getBotBeliefs(observer));
        repository.fail = false;
        session.settleHand();
        assertEquals(1, session.getStatistics().get(player).getHandsPlayed());
        assertEquals(1, profiles.getLifetimeStatistics(account).getHandsPlayed());
        assertThrows(IllegalStateException.class, session::settleHand);
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
        ProfileService reopened = new ProfileService(new FileProfileRepository(directory));
        AuthenticatedProfile login = reopened.login(profile.getProfileId(), PASSWORD).orElseThrow();
        assertEquals(session.getStatistics().get(player).getRates(),
                reopened.getLifetimeStatistics(login).getRates());
        session.nextHand();
        assertEquals(bindings, session.getProfileIds());
        assertEquals(beliefs, session.getBotBeliefs(observer));
        assertEquals(1, session.getLifetimeStatistics().get(player).getHandsPlayed());
    }

    @Test
    void pmC08ProfileRestartRejectsOldBotTurnsAndResetsLearning()
            throws IOException, GeneralSecurityException {
        ProfileService profiles = new ProfileService(new FileProfileRepository(directory));
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account =
                profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = new SetupSession(BotFixtures.initializer());
        session.startProfiles(profiles, List.of(account));
        BotTurn old = session.getBotTurn(32).orElseThrow();
        session.submitAction(BotFixtures.passive(session));
        session.startProfiles(profiles, List.of(account));
        Map<String, String> bindings = session.getProfileIds();
        session.revealCards();
        assertFalse(session.applyBotDecision(old, new ActionRequest(
                old.getView().getViewerId(), ActionType.CALL)));
        assertTrue(session.getAcceptedActions().isEmpty());
        assertEquals(2, session.getView().orElseThrow().getHoleCards().size());
        BotTurn current = session.getBotTurn(32).orElseThrow();
        assertTrue(session.isCurrentBotTurn(current));
        assertTrue(session.getBotBeliefs(current.getView().getViewerId()).values().stream()
                .allMatch(b -> b.getObservedDecisions() == 0));
        assertEquals(profile.getProfileId(), bindings.values().iterator().next());
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void w8E01AsyncFailureAndRetryPublishOnlyFromTheOwner(boolean afterSave)
            throws IOException, GeneralSecurityException {
        FailingRepository repo = new FailingRepository(new FileProfileRepository(directory));
        ProfileService profiles = new ProfileService(repo);
        PlayerProfile profile = profiles.create("Player", PASSWORD);
        AuthenticatedProfile account =
                profiles.login(profile.getProfileId(), PASSWORD).orElseThrow();
        SetupSession session = session();
        session.startProfiles(profiles, List.of(account));
        for (int index = 0; index < 5; index++) {
            act(session, ActionType.FOLD);
        }
        verifyAsyncRetry(session, profiles, account, repo, afterSave);
    }

    private static void verifyAsyncRetry(SetupSession session, ProfileService profiles,
            AuthenticatedProfile account, FailingRepository repo, boolean afterSave)
            throws IOException {
        Queue<Runnable> worker = new ArrayDeque<>();
        Queue<Runnable> owner = new ArrayDeque<>();
        List<Boolean> outcomes = new ArrayList<>();
        SetupView original = session.getView().orElseThrow();
        String player = original.getPlayers().get(0).getId();
        repo.fail = !afterSave;
        repo.failAfterSave = afterSave;
        assertThrows(RejectedExecutionException.class, () -> session.settleHand(task -> {
            throw new RejectedExecutionException("Worker unavailable");
        }, owner::add, outcomes::add));
        assertTrue(outcomes.isEmpty());

        session.settleHand(worker::add, owner::add, outcomes::add);
        assertThrows(IllegalStateException.class,
                () -> session.settleHand(worker::add, owner::add, outcomes::add));
        assertThrows(IllegalStateException.class, session::settleHand);
        assertThrows(IllegalStateException.class, () -> session.start(1));
        assertThrows(IllegalStateException.class,
                () -> session.startProfiles(profiles, List.of(account)));
        assertThrows(IllegalStateException.class, () -> session.quit(player));
        assertThrows(IllegalStateException.class, session::nextHand);
        assertEquals(1, worker.size());

        worker.remove().run();
        assertTrue(outcomes.isEmpty());
        owner.remove().run();
        assertEquals(List.of(false), outcomes);
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(original.getPot(), session.getView().orElseThrow().getPot());
        assertEquals(0, session.getStatistics().get(player).getHandsPlayed());
        assertEquals(afterSave ? 1 : 0, profiles.getLifetimeStatistics(account).getHandsPlayed());

        repo.fail = false;
        session.settleHand(worker::add, owner::add, outcomes::add);
        worker.remove().run();
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(1, profiles.getLifetimeStatistics(account).getHandsPlayed());
        owner.remove().run();
        assertEquals(List.of(false, true), outcomes);
        assertTrue(session.getSettlement().isPresent());
        assertEquals(1, session.getStatistics().get(player).getHandsPlayed());
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
    }

    @Test
    void w8E01UnprofiledHandPublishesWithoutWorkerOrDeferredCallback() {
        SetupSession session = session();
        session.start(1);
        for (int index = 0; index < 5; index++) {
            act(session, ActionType.FOLD);
        }
        List<Boolean> outcomes = new ArrayList<>();
        session.settleHand(task -> fail("No profile work"), task -> fail("No callback to queue"),
                outcomes::add);
        assertEquals(List.of(true), outcomes);
        assertTrue(session.getSettlement().isPresent());
    }

    private static void playAdaptiveHand(SetupSession session) {
        Random deals = new Random(380);
        Random decisions = new Random(16);
        MonteCarloEquityEstimator equity = new MonteCarloEquityEstimator(new PokerHandEvaluator(),
                new UniformDealSampler(deals::nextInt));
        BotComputer computer = new BotComputer(equity::estimate, decisions::nextDouble);
        int actions = 0;
        while (session.getResult().isEmpty() && actions++ < 200) {
            session.getBotTurn(32).ifPresentOrElse(turn -> assertTrue(
                    session.applyBotDecision(turn, computer.decide(turn, () -> false))),
                    () -> session.submitAction(BotFixtures.passive(session)));
            SetupView view = session.getView().orElseThrow();
            assertEquals(6000, view.getPot() + view.getPlayers().stream()
                    .mapToLong(Player::getChipStack).sum());
        }
        assertTrue(session.getResult().isPresent());
        assertTrue(actions < 200);
    }

    private static void wager(SetupSession session, ActionType type, long target) {
        SetupView view = session.getView().orElseThrow();
        String actor = view.getPlayers().get(view.getCurrentActorSeat()).getId();
        session.submitAction(new ActionRequest(actor, type, target));
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
