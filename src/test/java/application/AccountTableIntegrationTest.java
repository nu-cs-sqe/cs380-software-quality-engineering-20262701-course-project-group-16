package application;

import domain.model.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountTableIntegrationTest {
    @Test
    void buyInsUseAccountTotalsAndSettledChangesAreSavedExactlyOnce()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 500);
        SetupSession session = fixture.start();
        assertEquals(5500, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
        assertEquals(10, session.getView().orElseThrow().getRoundContribution(1));
        assertEquals(20, session.getView().orElseThrow().getRoundContribution(2));
        assertEquals(500, session.getTableFunds().get("account-1").getTableChips());
        assertEquals(1000, session.getTableFunds().get("account-0").getAvailableChips());
        finish(session);
        Map<String, Long> before = Map.copyOf(fixture.store.balances);
        HandCompletion preview = session.prepareHandCompletion();
        assertFalse(preview.getSessionId().isBlank());
        assertEquals(session.getView().orElseThrow().getHandId(), preview.getHandId());
        assertEquals(0, preview.getAccounts().values().stream()
                .mapToLong(HandCompletion.AccountChange::getNetChange).sum()
                + preview.getBotChanges().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(before, fixture.store.balances);
        session.settleHand();
        assertEquals(1, fixture.store.saved.size());
        for (HandCompletion.AccountChange change : preview.getAccounts().values()) {
            assertEquals(change.getEndingStack() - change.getStartingStack(),
                    change.getNetChange());
            assertEquals(before.get(change.getAccountId()) + change.getNetChange(),
                    fixture.store.balances.get(change.getAccountId()));
            assertEquals(1, change.getStatistics().getCounts().get(Statistic.HANDS));
        }
        assertTrue(session.getMatchWinner().isEmpty());
        assertThrows(IllegalStateException.class, session::settleHand);
        String sessionId = preview.getSessionId();
        session.nextHand();
        finish(session);
        assertEquals(sessionId, session.prepareHandCompletion().getSessionId());
        assertNotEquals(preview.getHandId(), session.prepareHandCompletion().getHandId());
        session.settleHand();
        assertEquals(2, fixture.store.saved.size());
        assertEquals(5500, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum());
    }

    @Test
    void failedOrAmbiguousSavePreservesUnpaidStateAndRetryDoesNotDoubleCredit()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        String hand = session.getView().orElseThrow().getHandId();
        long pot = session.getView().orElseThrow().getPot();
        fixture.store.failBefore = true;
        assertThrows(UncheckedIOException.class, session::settleHand);
        assertEquals(pot, session.getView().orElseThrow().getPot());
        assertEquals(0, session.getStatistics().values().iterator().next().getHandsPlayed());
        assertTrue(fixture.store.saved.isEmpty());
        fixture.store.failBefore = false;
        fixture.store.failAfter = true;
        assertThrows(UncheckedIOException.class, session::settleHand);
        Map<String, Long> credited = Map.copyOf(fixture.store.balances);
        assertTrue(session.getSettlement().isEmpty());
        fixture.store.failAfter = false;
        session.settleHand();
        assertEquals(credited, fixture.store.balances);
        assertEquals(1, fixture.store.saved.size());
        assertEquals(hand, fixture.store.saved.get(0).getHandId());
        assertEquals(1, session.getStatistics().values().iterator().next().getHandsPlayed());
    }

    @Test
    void topUpsOnlyMoveExistingFundsBetweenHandsAndRejectInvalidAmounts()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        assertThrows(IllegalStateException.class, () -> session.addTableChips("account-0", 1));
        finish(session);
        session.settleHand();
        long available = session.getTableFunds().get("account-0").getAvailableChips();
        long table = session.getTableFunds().get("account-0").getTableChips();
        assertEquals(1000, available);
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("unknown", 1));
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("account-0", 0));
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("account-0", -1));
        assertThrows(IllegalArgumentException.class,
                () -> session.addTableChips("account-0", available + 1));
        session.addTableChips("account-0", available);
        assertEquals(0, session.getTableFunds().get("account-0").getAvailableChips());
        assertEquals(table + available,
                session.getTableFunds().get("account-0").getTableChips());
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("account-0", 1));
        session.refreshTableBalances();
        assertEquals(2000, session.getTableFunds().get("account-0").getTableChips());
        assertEquals(1, fixture.store.saved.size());
        session.nextHand();
        assertEquals(7000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
    }

    @Test
    void departedHumansAreReplacedBetweenHandsAndBindingsDoNotReachTheBot()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        String departed = session.getView().orElseThrow().getPlayers().get(0).getId();
        session.quit(departed);
        assertEquals(1000, session.getTableFunds().get("account-0").getTableChips());
        assertEquals(1000, session.getTableFunds().get("account-0").getAvailableChips());
        finish(session);
        session.settleHand();
        long balance = fixture.store.balances.get("account-0");
        assertEquals(balance, session.getTableFunds().get("account-0").getAvailableChips());
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("account-0", 20));
        session.nextHand();
        SetupView view = session.getView().orElseThrow();
        assertEquals(PlayerType.BOT, view.getPlayers().get(0).getType());
        assertNotEquals(departed, view.getPlayers().get(0).getId());
        assertEquals(40, view.getRemainingCardCount());
        assertEquals(1, session.getProfileIds().size());
        assertEquals(1, session.getStatistics().get(departed).getHandsPlayed());
        finish(session);
        HandCompletion completion = session.prepareHandCompletion();
        assertEquals(Set.of("account-1"), completion.getAccounts().keySet());
        session.settleHand();
        assertEquals(balance, fixture.store.balances.get("account-0"));
        assertEquals(2, session.getStatistics().get(
                view.getPlayers().get(1).getId()).getHandsPlayed());
    }

    @Test
    void invalidBalancesAndAccountsCannotReplaceTheExistingHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        String hand = session.getView().orElseThrow().getHandId();
        fixture.store.balances.put("account-1", 0L);
        assertThrows(IllegalArgumentException.class, () ->
                session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
        fixture.store.balances.clear();
        assertThrows(IllegalArgumentException.class, () ->
                session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void aBustedHumanCanUseARecoveredBalanceWithoutRestartingTheTable()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(400, 400, 400, 400, 400, 400);
        SetupSession session = fixture.start();
        assertThrows(IllegalStateException.class, session::endTable);
        assertThrows(IllegalStateException.class, session::refreshTableBalances);
        assertFalse(session.canRecoverAccount("account-0"));
        assertTrue(session.canRecoverAccount("unseated"));
        assertThrows(IllegalStateException.class,
                () -> session.applyRecoveredBalance("account-0", 600));
        session.applyRecoveredBalance("unseated", 600);
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.RAISE, 400));
        BotFixtures.finish(session);
        session.settleHand();
        assertTrue(session.getMatchWinner().isEmpty());
        String broke = session.getTableFunds().entrySet().stream()
                .filter(entry -> entry.getValue().needsRecovery()).findFirst().orElseThrow()
                .getKey();
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips(broke, 1));
        assertFalse(session.canStartNextHand());
        assertThrows(IllegalStateException.class, session::nextHand);
        fixture.store.balances.put(broke, 399L);
        session.refreshTableBalances();
        assertTrue(session.getTableFunds().get(broke).needsRecovery());
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips(broke, 399));
        fixture.store.balances.put(broke, 400L);
        assertTrue(session.canRecoverAccount(broke));
        session.applyRecoveredBalance(broke, 400);
        assertTrue(session.getTableFunds().get(broke).needsTopUp());
        session.addTableChips(broke, 400);
        assertEquals(400, session.getTableFunds().get(broke).getTableChips());
        assertEquals(400, session.getTableFunds().get(broke).getBalance());
        assertTrue(session.canStartNextHand());
        session.nextHand();
        assertEquals(1, fixture.store.saved.size());
        assertEquals(2800, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
        BotFixtures.finish(session);
        session.settleHand();
        String funded = session.getView().orElseThrow().getPlayers().stream()
                .filter(player -> player.getChipStack() > 0).findFirst().orElseThrow().getId();
        session.selectViewer(funded);
        session.revealCards();
        assertFalse(session.getView().orElseThrow().getHoleCards().isEmpty());
        session.endTable();
        session.applyRecoveredBalance(broke, 900);
        assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
        assertThrows(IllegalStateException.class, session::refreshTableBalances);
        assertEquals(0, session.getTableFunds().get(broke).getTableChips());
        assertEquals(session.getTableFunds().get(broke).getBalance(),
                session.getTableFunds().get(broke).getAvailableChips());
        assertFalse(session.canStartNextHand());
        assertTrue(session.getMatchWinner().isEmpty());
        assertThrows(IllegalStateException.class, session::nextHand);
        assertThrows(IllegalStateException.class, () -> session.addTableChips(broke, 1));
    }

    @Test
    void restoredBalancesCannotInvalidateChipsAlreadyReservedForTheTable()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        Map<String, TableFunds> before = session.getTableFunds();
        fixture.store.balances.put("account-0", 1L);
        assertThrows(IllegalArgumentException.class, session::refreshTableBalances);
        assertEquals(before.get("account-0").getBalance(),
                session.getTableFunds().get("account-0").getBalance());
    }

    @Test
    void returnedCompletionsAreDetachedAndReportPreBlindStacksAndSessionNet()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        assertThrows(IllegalStateException.class, session::prepareHandCompletion);
        assertThrows(IllegalStateException.class, session::endTable);
        long firstStack = session.getTableFunds().get("account-1").getTableChips();
        assertEquals(1000, firstStack);
        finish(session);
        HandCompletion completion = session.prepareHandCompletion();
        assertEquals(1000, completion.getAccounts().get("account-1").getStartingStack());
        assertEquals(2000, completion.getAccounts().get("account-1").getBalanceBefore());
        assertThrows(UnsupportedOperationException.class, () -> completion.getAccounts().clear());
        assertThrows(UnsupportedOperationException.class, () -> completion.getBotChanges().clear());
        session.settleHand();
        assertEquals(completion.getAccounts().get("account-1").getNetChange(),
                session.getSessionNetChanges().get("account-1"));
        assertEquals(completion.getAccounts().get("account-1").getNetChange(),
                session.getTableFunds().get("account-1").getSessionNetChange());
        session.addTableChips("account-1", 100);
        assertEquals(completion.getAccounts().get("account-1").getNetChange(),
                session.getSessionNetChanges().get("account-1"));
        session.nextHand();
        assertEquals(1000, completion.getAccounts().get("account-1").getStartingStack());
    }

    @Test
    void newBotsReplaceBustedBotsAndRetainOnlyActualAccountBindings()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.RAISE, 1000));
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.CALL));
        BotFixtures.finish(session);
        session.settleHand();
        Map<Integer, String> busted = new LinkedHashMap<>();
        for (Player player : session.getView().orElseThrow().getPlayers()) {
            if (player.getType() == PlayerType.BOT && player.getChipStack() == 0) {
                busted.put(player.getSeat(), player.getId());
            }
        }
        assertFalse(busted.isEmpty());
        session.nextHand();
        SetupView view = session.getView().orElseThrow();
        busted.forEach((seat, id) -> {
            Player replacement = view.getPlayers().get(seat);
            assertNotEquals(id, replacement.getId());
            assertEquals(PlayerType.BOT, replacement.getType());
            assertEquals(1000, replacement.getChipStack() + view.getHandContribution(seat));
        });
        assertEquals(1, session.getProfileIds().size());
        session.getBotTurn(8).ifPresent(turn ->
                assertEquals(5, turn.getBeliefs().size()));
    }

    @Test
    void survivingBotsRetainTheirLearningWhenAnotherSeatIsReplaced()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        List<Player> original = session.getView().orElseThrow().getPlayers();
        session.submitAction(BotFixtures.passive(session));
        session.submitAction(BotFixtures.passive(session));
        String observed = original.get(3).getId();
        String observer = original.get(2).getId();
        String quitter = original.get(0).getId();
        session.quit(quitter);
        finish(session);
        session.settleHand();
        long count = session.getBotBeliefs(observer).get(observed).getObservedDecisions();
        assertTrue(count > 0);
        session.nextHand();
        assertEquals(count,
                session.getBotBeliefs(observer).get(observed).getObservedDecisions());
        assertFalse(session.getBotBeliefs(observer).containsKey(quitter));
        session.submitAction(BotFixtures.passive(session));
    }

    @Test
    void preparedSettlementSavesInBackgroundAndPublishesOnlyOnTheOwner()
            throws IOException, GeneralSecurityException, InterruptedException,
            ExecutionException, TimeoutException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        long decisions = session.getAcceptedActions().size();
        PreparedSettlement prepared = session.prepareSettlement();
        long pot = session.getView().orElseThrow().getPot();
        assertSame(prepared, session.prepareSettlement());
        assertSame(prepared.getHandCompletion().orElseThrow(), session.prepareHandCompletion());
        assertThrows(IllegalStateException.class, () -> session.publishSettlement(prepared));
        assertThrows(IllegalStateException.class, () -> session.start(1));
        assertThrows(IllegalStateException.class,
                () -> session.startProfiles(fixture.profiles, fixture.accounts));
        assertThrows(IllegalStateException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertThrows(IllegalStateException.class,
                () -> session.quit(session.getView().orElseThrow().getPlayers().get(0).getId()));
        CompletableFuture.runAsync(prepared::save).get(3, TimeUnit.SECONDS);
        assertTrue(prepared.isSaved());
        assertEquals(pot, session.getView().orElseThrow().getPot());
        assertTrue(session.getSettlement().isEmpty());
        assertEquals(1, fixture.store.saved.size());
        Fixture otherFixture = new Fixture(2000);
        SetupSession other = otherFixture.start();
        finish(other);
        PreparedSettlement foreign = other.prepareSettlement();
        foreign.save();
        assertNotEquals(prepared.getHandCompletion().orElseThrow().getSessionId(),
                foreign.getHandCompletion().orElseThrow().getSessionId());
        assertThrows(IllegalStateException.class, () -> session.publishSettlement(foreign));
        prepared.save();
        assertEquals(1, fixture.store.saved.size());
        session.publishSettlement(prepared);
        assertEquals(0, session.getView().orElseThrow().getPot());
        assertEquals(1, fixture.store.saved.size());
        assertThrows(IllegalStateException.class, () -> session.publishSettlement(prepared));
        session.nextHand();
        assertTrue(session.getBotTurn(8).orElseThrow().getRevision() > decisions);
        assertThrows(IllegalStateException.class, () -> session.publishSettlement(prepared));
    }

    @Test
    void topUpsLeaveRoomForBotsAfterAPlayerDeparts()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 10000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        long reserved = session.getTableFunds().get("account-0").getTableChips();
        long available = session.getTableFunds().get("account-0").getAvailableChips();
        assertEquals(available, session.getTableFunds().get("account-0").getMaximumTopUp());
        assertThrows(IllegalArgumentException.class,
                () -> session.addTableChips("account-0", available + 1));
        assertEquals(reserved, session.getTableFunds().get("account-0").getTableChips());
        session.addTableChips("account-0", available);
        assertEquals(Long.MAX_VALUE - 10000,
                session.getTableFunds().get("account-0").getTableChips());
        session.quit(session.getView().orElseThrow().getPlayers().get(0).getId());
        session.nextHand();
        assertTrue(session.getProfileIds().isEmpty());
        assertEquals(6000, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
        assertEquals(1, fixture.store.saved.size());
    }

    @Test
    void invalidOrUnavailableBalanceReadsPreserveTheCurrentHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        String hand = session.getView().orElseThrow().getHandId();
        fixture.store.failRead = true;
        assertThrows(UncheckedIOException.class, session::refreshTableBalances);
        assertThrows(UncheckedIOException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
        fixture.store.failRead = false;
        fixture.store.balances.clear();
        assertThrows(IllegalArgumentException.class, session::refreshTableBalances);
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void tableFundingBoundariesRejectNegativeOrUnreservedAmounts() {
        assertThrows(IllegalArgumentException.class, () -> new TableFunds(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new TableFunds(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TableFunds(1, 2, 0));
        assertTrue(new TableFunds(0, 0, 0).needsRecovery());
        assertFalse(new TableFunds(0, 0, 0).needsTopUp());
        assertFalse(new TableFunds(1, 1, 0).needsRecovery());
        assertFalse(new TableFunds(1, 1, 0).needsTopUp());
        assertFalse(new TableFunds(1, 0, 0).needsTopUp());
        assertTrue(new TableFunds(1, 0, 0).needsRecovery());
        GameConfig stakes = new GameConfig();
        assertThrows(IllegalArgumentException.class,
                () -> assertEquals(0, stakes.withTableTotal(0).getInitialTotalChips()));
        assertThrows(IllegalArgumentException.class,
                () -> assertEquals(-1, stakes.withTableTotal(-1).getInitialTotalChips()));
        assertEquals(1, stakes.withTableTotal(1).getInitialTotalChips());
        assertEquals(Long.MAX_VALUE,
                stakes.withTableTotal(Long.MAX_VALUE).getInitialTotalChips());
    }

    @Test
    void accountSelectionAndFailedRestartsPreservePrivacyAndThePriorHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000, 2000, 2000, 2000, 2000);
        SetupSession session = fixture.start();
        assertEquals(3, session.getView().orElseThrow().getViewerSeat());
        session.revealCards();
        assertEquals(2, session.getView().orElseThrow().getHoleCards().size());
        String hand = session.getView().orElseThrow().getHandId();
        fixture.store.failRead = true;
        assertThrows(UncheckedIOException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
        fixture.store.failRead = false;
        assertThrows(IllegalArgumentException.class, () -> session.startTable(fixture.profiles,
                List.of(fixture.accounts.get(0), fixture.accounts.get(0)), fixture.store));
        Fixture other = new Fixture(2000);
        assertThrows(IllegalArgumentException.class,
                () -> session.startTable(fixture.profiles, other.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void refreshedBalancesRetainTopUpsWhenAnotherAccountHasLeft()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        session.quit(session.getView().orElseThrow().getPlayers().get(1).getId());
        session.addTableChips("account-0", 100);
        long reserved = session.getTableFunds().get("account-0").getTableChips();
        session.refreshTableBalances();
        assertEquals(reserved, session.getTableFunds().get("account-0").getTableChips());
        assertEquals(0, session.getTableFunds().get("account-1").getTableChips());
    }

    @Test
    void accountOperationsRequireAnAccountTable() {
        SetupSession session = new SetupSession(BotFixtures.initializer());
        assertTrue(session.getTableFunds().isEmpty());
        assertTrue(session.getSessionNetChanges().isEmpty());
        assertThrows(IllegalStateException.class, session::prepareHandCompletion);
        assertThrows(IllegalStateException.class, session::refreshTableBalances);
        assertThrows(IllegalStateException.class, session::endTable);
        assertThrows(IllegalStateException.class, () -> session.addTableChips("unknown", 1));
        session.start(1);
        assertThrows(IllegalStateException.class, session::prepareHandCompletion);
    }

    @Test
    void aDepartedWinnersStackDoesNotReduceAnotherAccountsTopUpLimit()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 20000, 2000);
        SetupSession session = fixture.start();
        for (int count = 0; count < 4; count++) {
            session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.FOLD));
        }
        session.submitAction(BotFixtures.passive(session));
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.FOLD));
        session.settleHand();
        Player leaving = session.getView().orElseThrow().getPlayers().get(1);
        assertTrue(leaving.getChipStack() > 1000);
        session.quit(leaving.getId());
        long amount = session.getTableFunds().get("account-0").getMaximumTopUp();
        session.addTableChips("account-0", amount);
        assertEquals(Long.MAX_VALUE - 20000,
                session.getTableFunds().get("account-0").getTableChips());
        session.nextHand();
        assertEquals(PlayerType.BOT, session.getView().orElseThrow().getPlayers().get(1).getType());
    }

    @Test
    void minimumBuyInRejectsAnUnderfundedSelectedAccount()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        String hand = session.getView().orElseThrow().getHandId();
        fixture.store.balances.put("account-1", 399L);
        assertThrows(IllegalArgumentException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void bustedBalancesBelowTheMinimumRequireRecovery() {
        assertTrue(new TableFunds(399, 0, 0).needsRecovery());
        assertFalse(new TableFunds(399, 0, 0).needsTopUp());
        assertFalse(new TableFunds(400, 0, 0).needsRecovery());
        assertTrue(new TableFunds(400, 0, 0).needsTopUp());
        assertFalse(new TableFunds(399, 399, 0).needsRecovery());
    }

    @Test
    void selectedBuyInsCanExceedTwoThousandWithoutSpendingTheOutsideBalance()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(10000, 500);
        SetupSession session = fixture.start();
        session.startTable(fixture.profiles, fixture.accounts, fixture.store,
                Map.of("account-0", 3000L, "account-1", 400L));
        assertEquals(3000, session.getTableFunds().get("account-0").getTableChips());
        assertEquals(7000, session.getTableFunds().get("account-0").getAvailableChips());
        assertEquals(400, session.getTableFunds().get("account-1").getTableChips());
        assertEquals(7400, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
        assertTrue(fixture.store.saved.isEmpty());
        finish(session);
        HandCompletion completion = session.prepareHandCompletion();
        assertEquals(3000, completion.getAccounts().get("account-0").getStartingStack());
        assertEquals(400, completion.getAccounts().get("account-1").getStartingStack());
        session.settleHand();
        assertEquals(10000, fixture.store.balances.get("account-0"));
        session.nextHand();
        assertEquals(390, session.getTableFunds().get("account-1").getTableChips());
        assertFalse(session.getTableFunds().get("account-1").needsRecovery());
    }

    @Test
    void invalidSelectedAmountsPreserveTheCurrentHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(500);
        SetupSession session = fixture.start();
        String hand = session.getView().orElseThrow().getHandId();
        for (long amount : new long[] {-1, 0, 399, 501}) {
            assertThrows(IllegalArgumentException.class, () -> session.startTable(fixture.profiles,
                    fixture.accounts, fixture.store, Map.of("account-0", amount)));
            assertEquals(hand, session.getView().orElseThrow().getHandId());
        }
        assertThrows(IllegalArgumentException.class, () -> session.startTable(fixture.profiles,
                fixture.accounts, fixture.store, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> session.startTable(fixture.profiles,
                fixture.accounts, fixture.store, Map.of("account-0", 400L, "unknown", 400L)));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void selectedBuyInsRejectTotalOverflowBeforeReplacingTheCurrentHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 10000, 10000);
        SetupSession session = fixture.start();
        String hand = session.getView().orElseThrow().getHandId();
        assertThrows(IllegalArgumentException.class, () -> session.startTable(fixture.profiles,
                fixture.accounts, fixture.store,
                Map.of("account-0", Long.MAX_VALUE - 10000, "account-1", 10000L)));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
        session.startTable(fixture.profiles, fixture.accounts, fixture.store,
                Map.of("account-0", 3000L, "account-1", 400L));
        assertEquals(3000,
                session.getTableFunds().get("account-0").getTableChips());
    }

    @Test
    void winningsAndLaterTopUpsCanStayAboveTwoThousand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(10000, 10000, 10000, 10000, 10000, 10000);
        SetupSession session = fixture.start();
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.RAISE, 1000));
        BotFixtures.finish(session);
        session.settleHand();
        String winner = session.getTableFunds().entrySet().stream()
                .filter(entry -> entry.getValue().getTableChips() == 6000)
                .findFirst().orElseThrow().getKey();
        assertEquals(15000, session.getTableFunds().get(winner).getBalance());
        assertEquals(9000, session.getTableFunds().get(winner).getMaximumTopUp());
        session.addTableChips(winner, 1000);
        assertEquals(7000, session.getTableFunds().get(winner).getTableChips());
        String broke = session.getTableFunds().entrySet().stream()
                .filter(entry -> entry.getValue().getTableChips() == 0)
                .findFirst().orElseThrow().getKey();
        assertEquals(400, session.getTableFunds().get(broke).getMinimumTopUp());
        session.addTableChips(broke, 400);
        session.nextHand();
        assertEquals(7000, session.getTableFunds().get(winner).getTableChips());
        assertEquals(7400, session.getView().orElseThrow().getPlayers().stream()
                .mapToLong(Player::getChipStack).sum() + session.getView().orElseThrow().getPot());
        assertTrue(session.getMatchWinner().isEmpty());
    }

    @Test
    void openingFundsReserveRoomForPotentialWinnings()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE);
        SetupSession session = new SetupSession(BotFixtures.initializer());
        session.start(1);
        String hand = session.getView().orElseThrow().getHandId();
        assertThrows(IllegalArgumentException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store));
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void openingFundsReserveRoomForReplacementBots()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 2000, 400, 400, 400, 400, 400);
        SetupSession session = new SetupSession(BotFixtures.initializer());
        session.start(1);
        Map<String, Long> choices = Map.of("account-0", Long.MAX_VALUE - 2000,
                "account-1", 400L, "account-2", 400L, "account-3", 400L,
                "account-4", 400L, "account-5", 400L);
        assertThrows(IllegalArgumentException.class,
                () -> session.startTable(fixture.profiles, fixture.accounts,
                        fixture.store, choices));
    }

    @Test
    void reportedTopUpMaximumIsAcceptedWithinTheSharedFundingBudget()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 12000, 10000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        assertEquals(2000, session.getTableFunds().get("account-1").getMaximumTopUp());
        assertThrows(IllegalArgumentException.class,
                () -> session.addTableChips("account-1", 2001));
        session.addTableChips("account-1", 2000);
        assertEquals(2990, session.getTableFunds().get("account-1").getTableChips());
        session.nextHand();
        assertEquals(2990, session.getTableFunds().get("account-1").getTableChips());
    }

    @Test
    void pendingSettlementRejectionsConcealAnyRevealedCards()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        session.prepareSettlement();
        List<Runnable> changes = List.of(() -> session.start(1),
                () -> session.startProfiles(fixture.profiles, fixture.accounts),
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store),
                () -> session.startTable(fixture.profiles, fixture.accounts, fixture.store,
                        Map.of("account-0", 1000L)),
                session::nextHand, session::endTable, session::refreshTableBalances,
                () -> session.addTableChips("account-0", 400));
        for (Runnable change : changes) {
            session.revealCards();
            assertEquals(2, session.getView().orElseThrow().getHoleCards().size());
            assertThrows(IllegalStateException.class, change::run);
            assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
        }
    }

    @Test
    void blankAccountIdsCannotSelectABotForFunding()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        assertThrows(IllegalArgumentException.class, () -> session.addTableChips("", 1));
    }

    @Test
    void spentReleasedFundsDoNotBlockRemainingAccountsBalanceRefresh()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        session.quit(session.getView().orElseThrow().getPlayers().get(0).getId());
        fixture.store.balances.put("account-0", 0L);
        assertDoesNotThrow(session::refreshTableBalances);
        assertEquals(0, session.getTableFunds().get("account-0").getBalance());
        assertEquals(0, session.getTableFunds().get("account-0").getTableChips());
        assertEquals(1990, session.getTableFunds().get("account-1").getBalance());
    }

    @Test
    void unsafeFutureFundingStopsBeforeStartingAnotherHand()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(Long.MAX_VALUE - 10000);
        SetupSession session = fixture.start();
        session.startTable(fixture.profiles, fixture.accounts, fixture.store,
                Map.of("account-0", Long.MAX_VALUE - 10000));
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.RAISE, 1000));
        session.submitAction(BotFixtures.passive(session));
        session.submitAction(BotFixtures.passive(session));
        session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.FOLD));
        BotFixtures.finish(session);
        session.settleHand();
        assertFalse(session.canStartNextHand());
        assertThrows(IllegalStateException.class, session::nextHand);
        session.endTable();
        assertEquals(Long.MAX_VALUE - 10000,
                session.getTableFunds().get("account-0").getAvailableChips());
    }

    @Test
    void refreshRejectsAnUnpayableBalanceWithoutPublishingIt()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        String hand = session.getView().orElseThrow().getHandId();
        fixture.store.balances.put("account-0", Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, session::refreshTableBalances);
        assertEquals(2000, session.getTableFunds().get("account-0").getBalance());
        assertEquals(hand, session.getView().orElseThrow().getHandId());
    }

    @Test
    void fundingMaximumUsesTheSelectedAccountsActualStack()
            throws IOException, GeneralSecurityException {
        Fixture fixture = new Fixture(2000, 2000);
        SetupSession session = fixture.start();
        finish(session);
        session.settleHand();
        assertEquals(990, session.getTableFunds().get("account-1").getTableChips());
        assertEquals(1000, session.getTableFunds().get("account-1").getMaximumTopUp());
        session.addTableChips("account-1", 1000);
        assertEquals(1990, session.getTableFunds().get("account-1").getTableChips());
        assertEquals(0, session.getTableFunds().get("account-1").getAvailableChips());
    }

    private static void finish(SetupSession session) {
        int decisions = 0;
        while (session.getResult().isEmpty() && decisions++ < 40) {
            session.submitAction(new ActionRequest(BotFixtures.actor(session), ActionType.FOLD));
        }
        assertTrue(session.getResult().isPresent());
    }

    private static final class Fixture {
        final MapStore store = new MapStore();
        final List<AuthenticatedProfile> accounts = new ArrayList<>();
        final ProfileService profiles;

        Fixture(long... balances) throws IOException, GeneralSecurityException {
            ProfileRepository repository = new MemoryProfiles();
            PasswordHasher hasher = new FixedHasher();
            profiles = new ProfileService(repository, hasher,
                    () -> "account-" + accounts.size());
            for (long balance : balances) {
                String id = profiles.create("Player", new char[6]).getProfileId();
                accounts.add(profiles.login(id, new char[6]).orElseThrow());
                store.balances.put(id, balance);
            }
        }

        SetupSession start() {
            SetupSession session = new SetupSession(BotFixtures.initializer(), new BettingEngine(),
                    new ShowdownResolver(), HandStatistics::start, new java.util.Random(380));
            session.startTable(profiles, accounts, store);
            return session;
        }
    }

    private static final class MemoryProfiles implements ProfileRepository {
        private List<StoredProfile> records = List.of();
        public List<StoredProfile> load() { return records; }
        public void save(List<StoredProfile> profiles) { records = List.copyOf(profiles); }
    }

    private static final class FixedHasher implements PasswordHasher {
        public PasswordHash hash(char[] password) {
            return new PasswordHash("AAAAAAAAAAAAAAAAAAAAAA==",
                    "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        }
        public boolean matches(char[] password, PasswordHash hash) { return true; }
    }

    private static final class MapStore implements TableAccountStore {
        final Map<String, Long> balances = new LinkedHashMap<>();
        final List<HandCompletion> saved = new ArrayList<>();
        boolean failBefore;
        boolean failAfter;
        boolean failRead;

        public Map<String, Long> getBalances(Set<String> accounts) throws IOException {
            if (failRead) throw new IOException("Read unavailable");
            Map<String, Long> result = new LinkedHashMap<>();
            accounts.stream().filter(balances::containsKey)
                    .forEach(id -> result.put(id, balances.get(id)));
            return Map.copyOf(result);
        }

        public void save(HandCompletion hand) throws IOException {
            if (failBefore) throw new IOException("Before commit");
            if (saved.stream().anyMatch(old -> old.getHandId().equals(hand.getHandId()))) return;
            Map<String, Long> next = new LinkedHashMap<>(balances);
            hand.getAccounts().forEach((id, change) ->
                    next.put(id, Math.addExact(next.get(id), change.getNetChange())));
            balances.clear();
            balances.putAll(next);
            saved.add(hand);
            if (failAfter) throw new IOException("After commit");
        }
    }
}
