package application;

import domain.model.StoredProfile;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import localization.Localizations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistence.SqliteProfileRepository;
import static org.junit.jupiter.api.Assertions.*;

class RecoveryIntegrationTest {
    private static final String PASSWORD = "PokerPlayer123";
    private static final ZoneId LOCAL = ZoneId.of("America/New_York");
    private static final Instant BEFORE_MIDNIGHT = Instant.parse("2026-11-22T04:59:59Z");
    @TempDir Path directory;

    @Test
    void countsOnlySuccessfulClaimsAndResetsAtLocalMidnight()
            throws IOException, GeneralSecurityException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        AuthenticatedProfile account = create(profiles);
        RecoveryService recovery = service(profiles, repository, BEFORE_MIDNIGHT);
        setBalance(repository, 0);
        RecoveryChallenge first = recovery.begin();
        assertEquals(RecoveryOutcome.WRONG_ANSWERS,
                recovery.claim(account, first, answers(first, 2)));
        assertEquals(0, profiles.getChipBalance(account));
        assertEquals(RecoveryOutcome.AWARDED, recovery.claim(account, first, answers(first, 1)));
        assertEquals(200, profiles.getChipBalance(account));
        verifyReplayAcrossMidnight(profiles, repository, account, first);
        for (int attempt = 0; attempt < 2; attempt++) {
            setBalance(repository, 199);
            assertEquals(RecoveryOutcome.AWARDED, claimNew(recovery, account));
        }
        setBalance(repository, 399);
        assertEquals(RecoveryOutcome.DAILY_LIMIT, claimNew(recovery, account));
        RecoveryService tomorrow = service(profiles, repository, BEFORE_MIDNIGHT.plusSeconds(1));
        assertEquals(RecoveryOutcome.AWARDED, claimNew(tomorrow, account));
        assertEquals(599, profiles.getChipBalance(account));
        assertEquals(RecoveryOutcome.FUNDED, claimNew(tomorrow, account));
        assertEquals(4, profiles.getOverview(account).getHistory().size());
        ChipHistoryEntry latest = profiles.getOverview(account).getHistory().get(0);
        assertEquals(ChipHistoryEntry.Type.RECOVERY_REWARD, latest.getType());
        assertEquals(200, latest.getChange());
        assertEquals(599, latest.getBalance());
        assertTrue(latest.getHandId().isEmpty());
        assertTrue(latest.getSessionId().isEmpty());
        assertTrue(latest.getSessionNet().isEmpty());
        assertEquals(0, profiles.getLifetimeStatistics(account).getHandsPlayed());
    }

    @Test
    void failedWriteRollsBackAndOnlyTheOriginalAccountCanReplayTheClaim()
            throws IOException, GeneralSecurityException, SQLException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        AuthenticatedProfile account = create(profiles);
        setBalance(repository, 0);
        RecoveryService recovery = service(profiles, repository, BEFORE_MIDNIGHT);
        RecoveryChallenge challenge = recovery.begin();
        execute("CREATE TRIGGER drop_claim BEFORE INSERT ON recovery_claims "
                + "BEGIN SELECT RAISE(IGNORE); END");
        assertThrows(IOException.class, () -> recovery.claim(account, challenge,
                answers(challenge, 0)));
        assertEquals(0, profiles.getChipBalance(account));
        assertTrue(profiles.getOverview(account).getHistory().isEmpty());
        execute("DROP TRIGGER drop_claim");
        assertEquals(RecoveryOutcome.AWARDED,
                recovery.claim(account, challenge, answers(challenge, 0)));
        AuthenticatedProfile other = create(profiles);
        assertThrows(IOException.class, () -> recovery.claim(other, challenge,
                answers(challenge, 0)));
        ProfileService anotherService = new ProfileService(repository);
        RecoveryService foreign = service(anotherService, repository, BEFORE_MIDNIGHT);
        assertThrows(IllegalArgumentException.class,
                () -> foreign.claim(account, challenge, answers(challenge, 0)));
        assertEquals(1, profiles.getOverview(account).getHistory().size());
        assertEquals(2_000, profiles.getChipBalance(other));
    }

    @Test
    void exactBuyInIsPracticeAndRecoveryUpgradePreservesSavedChips()
            throws IOException, GeneralSecurityException, SQLException {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        ProfileService profiles = new ProfileService(repository);
        AuthenticatedProfile account = create(profiles);
        setBalance(repository, BuyInLimits.MINIMUM);
        execute("DROP TABLE recovery_claims");
        execute("PRAGMA user_version = 3");
        RecoveryService recovery = service(profiles, repository, BEFORE_MIDNIGHT);
        assertEquals(RecoveryOutcome.FUNDED, claimNew(recovery, account));
        assertEquals(BuyInLimits.MINIMUM, profiles.getChipBalance(account));
        assertTrue(profiles.getOverview(account).getHistory().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> repository.grantRecovery(
                " ", "id", BEFORE_MIDNIGHT.atZone(LOCAL).toLocalDate(), BEFORE_MIDNIGHT));
        assertThrows(IllegalArgumentException.class, () -> repository.grantRecovery(
                account.getProfile().getProfileId(), " ",
                BEFORE_MIDNIGHT.atZone(LOCAL).toLocalDate(), BEFORE_MIDNIGHT));
    }

    @Test
    void bankHasTenDistinctLocalizedQuestionsAndValidatesAnswerShape() {
        Localizations messages = Localizations.load();
        assertEquals(10, RecoveryQuestion.BANK.size());
        Set<String> questionIds = new java.util.HashSet<>();
        for (RecoveryQuestion question : RecoveryQuestion.BANK) {
            assertTrue(questionIds.add(question.getId()));
            String key = "recovery.question." + question.getId();
            for (String language : messages.languages()) {
                assertFalse(messages.text(language, key).isBlank());
                for (int choice = 0; choice < RecoveryQuestion.CHOICES; choice++) {
                    assertFalse(messages.text(language, key + "." + choice).isBlank());
                }
            }
        }
        RecoveryChallenge quiz = new RecoveryChallenge(RecoveryQuestion.BANK.subList(0, 5));
        assertEquals(5, quiz.score(answers(quiz, 0)));
        assertThrows(UnsupportedOperationException.class, () -> quiz.getQuestions().clear());
        assertThrows(IllegalArgumentException.class, () -> quiz.score(List.of()));
        assertThrows(IllegalArgumentException.class, () -> quiz.score(List.of(-1, 0, 0, 0, 0)));
        assertThrows(IllegalArgumentException.class, () -> quiz.score(List.of(3, 0, 0, 0, 0)));
    }

    @Test
    void challengesSampleDistinctQuestionsAndUseThePokerAnswerKey() {
        SqliteProfileRepository repository = new SqliteProfileRepository(directory);
        RecoveryService recovery = service(new ProfileService(repository), repository,
                BEFORE_MIDNIGHT);
        List<RecoveryQuestion> first = recovery.begin().getQuestions();
        List<RecoveryQuestion> second = recovery.begin().getQuestions();
        assertEquals(5, Set.copyOf(first).size());
        assertEquals(5, Set.copyOf(second).size());
        assertNotEquals(first, second);
        Map<String, Integer> key = Map.of("blinds", 1, "fold", 2, "private", 0,
                "board", 1, "allIn", 2, "refund", 0, "sidePot", 1, "tie", 2, "quit", 0,
                "topUp", 1);
        for (RecoveryQuestion question : RecoveryQuestion.BANK) {
            assertEquals(key.get(question.getId()).intValue(), question.correctChoice(),
                    question.getId());
        }
    }

    private void execute(String sql) throws SQLException {
        try (var connection = DriverManager.getConnection(
                "jdbc:sqlite:" + directory.resolve("profiles.db"));
                var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void verifyReplayAcrossMidnight(ProfileService profiles,
            SqliteProfileRepository repository, AuthenticatedProfile account,
            RecoveryChallenge challenge) throws IOException {
        RecoveryService tomorrow = service(profiles, repository, BEFORE_MIDNIGHT.plusSeconds(1));
        assertEquals(RecoveryOutcome.ALREADY_AWARDED,
                tomorrow.claim(account, challenge, answers(challenge, 0)));
        assertEquals(200, profiles.getChipBalance(account));
    }

    private static RecoveryOutcome claimNew(RecoveryService recovery, AuthenticatedProfile account)
            throws IOException {
        RecoveryChallenge challenge = recovery.begin();
        return recovery.claim(account, challenge, answers(challenge, 0));
    }

    private static RecoveryService service(ProfileService profiles, SqliteProfileRepository repo,
            Instant now) {
        return new RecoveryService(profiles, repo, Clock.fixed(now, LOCAL), 17);
    }

    private static AuthenticatedProfile create(ProfileService profiles)
            throws IOException, GeneralSecurityException {
        String id = profiles.create("Player", PASSWORD.toCharArray()).getProfileId();
        return profiles.login(id, PASSWORD.toCharArray()).orElseThrow();
    }

    private static void setBalance(SqliteProfileRepository repository, long balance)
            throws IOException {
        StoredProfile before = repository.load().get(0);
        repository.save(List.of(new StoredProfile(before.getProfile(), before.getPasswordHash(),
                before.getStatistics(), balance)));
    }

    static List<Integer> answers(RecoveryChallenge challenge, int mistakes) {
        List<Integer> choices = new ArrayList<>();
        for (RecoveryQuestion question : challenge.getQuestions()) {
            int correct = question.correctChoice();
            choices.add(mistakes-- > 0 ? (correct + 1) % RecoveryQuestion.CHOICES : correct);
        }
        return List.copyOf(choices);
    }
}
