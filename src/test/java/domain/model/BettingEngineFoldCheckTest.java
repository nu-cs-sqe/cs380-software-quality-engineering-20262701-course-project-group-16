package domain.model;

import application.GameInitializer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BettingEngineFoldCheckTest {
    private final BettingEngine engine = new BettingEngine();

    @Test
    @DisplayName("BE-FOLD01: fold without spending chips or changing the input")
    void foldRetainsCardsAndChipsAndRemovesDecisionRights() {
        GameState before = opening().build();
        List<Object> original = snapshot(before);

        BettingResult result = act(before, 3, ActionType.FOLD);
        GameState after = result.getState();

        assertNotSame(before, after);
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(3).getStatus());
        assertEquals(before.getPlayers().get(3).getId(), after.getPlayers().get(3).getId());
        assertEquals(before.getPlayers().get(3).getType(), after.getPlayers().get(3).getType());
        assertEquals(3, after.getPlayers().get(3).getSeat());
        assertEquals(before.getPlayerStates().get(3).getHoleCards(), after.getPlayerStates().get(3).getHoleCards());
        for (int seat = 0; seat < 6; seat++) {
            assertEquals(before.getPlayers().get(seat).getChipStack(), after.getPlayers().get(seat).getChipStack());
            assertEquals(before.getCommittedChips(seat), after.getCommittedChips(seat));
            assertEquals(before.getPlayerStates().get(seat).getRoundContribution(),
                    after.getPlayerStates().get(seat).getRoundContribution());
            if (seat != 3) {
                assertEquals(before.getPlayerStates().get(seat).getStatus(),
                        after.getPlayerStates().get(seat).getStatus());
            }
        }
        assertEquals(Set.of("p0", "p1", "p2", "p4", "p5"), after.getBettingProgress().getPlayersToAct());
        assertEquals(Set.of("p0", "p1", "p2", "p4", "p5"), after.getBettingProgress().getPlayersAllowedToRaise());
        assertEquals(4, after.getCurrentActorSeat().orElseThrow());
        assertEquals(30, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getHandId(), after.getHandId());
        assertEquals(before.getPositions(), after.getPositions());
        assertEquals(20, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        assertObservation(result, before, 3, ResolvedActionType.FOLD, 0, 0);
        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-FOLD02: blind seat {0} retains its committed chips")
    @ValueSource(ints = {1, 2})
    void foldingFromABlindDoesNotRefundItsContribution(int seat) {
        GameState before = beforeBlindDecision(seat);
        List<Object> original = snapshot(before);
        long committed = seat == 1 ? 10 : 20;

        BettingResult result = act(before, seat, ActionType.FOLD);
        GameState after = result.getState();

        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(seat).getStatus());
        assertEquals(1000 - committed, after.getPlayers().get(seat).getChipStack());
        assertEquals(committed, after.getPlayerStates().get(seat).getRoundContribution());
        assertEquals(committed, after.getCommittedChips(seat));
        assertEquals(before.getPot(), after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertFalse(after.getBettingProgress().getPlayersToAct().contains("p" + seat));
        assertFalse(after.getBettingProgress().getPlayersAllowedToRaise().contains("p" + seat));
        assertObservation(result, before, seat, ResolvedActionType.FOLD, 0, committed);
        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-CHECK01: the big blind checks at zero cost and closes pending decisions")
    void bigBlindChecksWithoutAddingChipsOrDealingTheFlop() {
        GameState before = beforeBlindDecision(2);
        List<Object> original = snapshot(before);

        BettingResult result = act(before, 2, ActionType.CHECK);
        GameState after = result.getState();

        assertNotSame(before, after);
        assertEquals(980, after.getPlayers().get(2).getChipStack());
        assertEquals(PlayerStatus.ACTIVE, after.getPlayerStates().get(2).getStatus());
        assertEquals(20, after.getPlayerStates().get(2).getRoundContribution());
        assertEquals(20, after.getCommittedChips(2));
        assertEquals(120, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getCurrentActorSeat().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertEquals(20, after.getCurrentBet());
        assertEquals(20, after.getLastFullRaiseSize());
        assertEquals(before.getCommunityCards(), after.getCommunityCards());
        assertEquals(before.getRemainingDeck(), after.getRemainingDeck());
        assertObservation(result, before, 2, ResolvedActionType.CHECK, 0, 20);
        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-CHECK02: checking while owing {0} chips is invalid")
    @ValueSource(longs = {1, 10, 20})
    void checkIsRejectedWhenAnyChipsAreOwed(long owed) {
        FoundationFixtures.Fixture fixture = opening();
        long contributed = 20 - owed;
        fixture.player(3, 1000 - contributed, PlayerStatus.ACTIVE, contributed, contributed);
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 3, ActionType.CHECK));

        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-CALL04: a player owing zero must check rather than call")
    void zeroCostCallIsRejectedWithoutChangingTheState() {
        GameState before = beforeBlindDecision(2);
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 2, ActionType.CALL));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-ORDER02: reject an out-of-turn {0}")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK"})
    void foldAndCheckMustComeFromTheCurrentActor(ActionType type) {
        GameState before = opening().build();
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class, () -> act(before, 4, type));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-ORDER03: an unknown player cannot submit {0}")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK"})
    void anUnknownPlayerCannotAct(ActionType type) {
        GameState before = opening().build();
        List<Object> original = snapshot(before);

        assertThrows(IllegalArgumentException.class,
                () -> engine.apply(before, new ActionRequest("unknown-player", type)));

        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-TURN01: skip folded and all-in seats and wrap from seat 5 to seat 0")
    void nextActorSkipsFoldedAndAllInSeats() {
        FoundationFixtures.Fixture fixture = opening();
        fixture.player(4, 1000, PlayerStatus.FOLDED, 0, 0);
        fixture.player(5, 0, PlayerStatus.ALL_IN, 20, 20);
        fixture.player(0, 1980, PlayerStatus.ACTIVE, 0, 0);
        fixture.betting = new BettingProgress(20, 20, Set.of("p0", "p1", "p2", "p3"),
                Set.of("p0", "p1", "p2", "p3"));
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        GameState after = act(before, 3, ActionType.FOLD).getState();

        assertEquals(0, after.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p0", "p1", "p2"), after.getBettingProgress().getPlayersToAct());
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(4).getStatus());
        assertEquals(PlayerStatus.ALL_IN, after.getPlayerStates().get(5).getStatus());
        assertEquals(50, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-TURN02: retain eliminated seat numbers while skipping them")
    void nextActorSkipsAnEliminatedSeat() {
        FoundationFixtures.Fixture fixture = opening();
        eliminate(fixture, 4);
        fixture.player(0, 2000, PlayerStatus.ACTIVE, 0, 0);
        fixture.betting = new BettingProgress(20, 20, Set.of("p0", "p1", "p2", "p3", "p5"),
                Set.of("p0", "p1", "p2", "p3", "p5"));
        GameState before = fixture.build();

        GameState after = act(before, 3, ActionType.FOLD).getState();

        assertEquals(5, after.getCurrentActorSeat().orElseThrow());
        assertEquals(PlayerStatus.ELIMINATED, after.getPlayerStates().get(4).getStatus());
        assertEquals(4, after.getPlayers().get(4).getSeat());
        assertEquals(6, after.getPlayers().size());
        assertEquals(6000, after.getTotalChips());
    }

    @Test
    @DisplayName("BE-TURN03: the last opponent folds; stop decisions without awarding the pot")
    void lastFoldLeavesOneParticipantAndNoFurtherActor() {
        FoundationFixtures.Fixture fixture = opening();
        for (int seat = 1; seat <= 4; seat++) {
            eliminate(fixture, seat);
        }
        fixture.player(0, 2990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(5, 2980, PlayerStatus.ACTIVE, 20, 20);
        fixture.positions = new HandPositions(0, 0, 5);
        fixture.actor = "p0";
        fixture.betting = new BettingProgress(20, 20, Set.of("p0", "p5"), Set.of("p0", "p5"));
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        GameState after = act(before, 0, ActionType.FOLD).getState();

        assertEquals(1, after.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(0).getStatus());
        assertEquals(2990, after.getPlayers().get(0).getChipStack());
        assertEquals(2980, after.getPlayers().get(5).getChipStack());
        assertEquals(30, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-TURN04: an all-in opponent remains live, so an unmatched caller still acts")
    void anAllInOpponentDoesNotMakeTheRemainingActivePlayerAnUncontestedWinner() {
        FoundationFixtures.Fixture fixture = opening();
        fixture.player(2, 0, PlayerStatus.ALL_IN, 20, 20);
        fixture.player(0, 1980, PlayerStatus.ACTIVE, 0, 0);
        fixture.betting = new BettingProgress(20, 20, Set.of("p0", "p1", "p3", "p4", "p5"),
                Set.of("p0", "p1", "p3", "p4", "p5"));
        GameState state = fixture.build();
        for (int seat : new int[]{3, 4, 5, 0}) {
            state = act(state, seat, ActionType.FOLD).getState();
        }

        assertEquals(1, state.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of("p1"), state.getBettingProgress().getPlayersToAct());
        assertEquals(2, state.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(1, state.getPlayerStates().stream().filter(PlayerState::canAct).count());
        assertEquals(PlayerStatus.ALL_IN, state.getPlayerStates().get(2).getStatus());

        GameState afterCall = act(state, 1, ActionType.CALL).getState();

        assertTrue(afterCall.getCurrentActorId().isEmpty());
        assertEquals(40, afterCall.getPot());
        assertEquals(6000, afterCall.getTotalChips());
    }

    @ParameterizedTest(name = "BE-CLOSED01: reject {0} after all decisions finish")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK", "CALL"})
    void noActionIsAcceptedAfterTheRoundHasNoActor(ActionType type) {
        GameState closed = act(beforeBlindDecision(2), 2, ActionType.CHECK).getState();
        List<Object> original = snapshot(closed);

        assertThrows(IllegalArgumentException.class, () -> act(closed, 2, type));

        assertEquals(original, snapshot(closed));
    }

    @ParameterizedTest(name = "BE-LIMIT01: raised-round {0} awaits authoritative raise context")
    @EnumSource(value = ActionType.class, names = {"FOLD", "CHECK", "CALL"})
    void raisedRoundsRemainExplicitlyUnsupported(ActionType type) {
        FoundationFixtures.Fixture fixture = opening();
        fixture.player(0, 940, PlayerStatus.ACTIVE, 60, 60);
        fixture.betting = new BettingProgress(60, 40, Set.of("p1", "p2", "p3", "p4", "p5"),
                Set.of("p1", "p2", "p3", "p4", "p5"));
        GameState before = fixture.build();
        List<Object> original = snapshot(before);

        assertThrows(UnsupportedOperationException.class, () -> act(before, 3, type));

        assertEquals(original, snapshot(before));
    }

    @ParameterizedTest(name = "BE-LIMIT02: {0} is outside the fold/check/call increment")
    @EnumSource(value = ActionType.class, names = {"BET", "RAISE"})
    void betsAndRaisesRemainUnsupported(ActionType type) {
        GameState before = opening().build();
        List<Object> original = snapshot(before);

        assertThrows(UnsupportedOperationException.class,
                () -> engine.apply(before, new ActionRequest("p3", type, 60)));

        assertEquals(original, snapshot(before));
    }

    @Test
    @DisplayName("BE-SEQUENCE01: real dealing plus call/fold/check preserves the big blind option")
    void controlledPreflopSequenceEndsAtPot100WithFiveLiveHands() {
        Random dealer = new SeatZeroRandom();
        GameState initial = new GameInitializer(new GameConfig(), dealer, new Random(7)).start(2);
        List<Object> original = snapshot(initial);
        GameState state = initial;
        int[] seats = {3, 4, 5, 0, 1};
        ActionType[] actions = {ActionType.CALL, ActionType.FOLD, ActionType.CALL,
                ActionType.CALL, ActionType.CALL};
        for (int index = 0; index < seats.length; index++) {
            assertEquals(seats[index], state.getCurrentActorSeat().orElseThrow());
            state = act(state, seats[index], actions[index]).getState();
            assertEquals(6000, state.getTotalChips());
        }

        assertEquals(2, state.getCurrentActorSeat().orElseThrow());
        assertEquals(Set.of(state.getPlayers().get(2).getId()), state.getBettingProgress().getPlayersToAct());
        assertEquals(100, state.getPot());

        GameState after = act(state, 2, ActionType.CHECK).getState();

        assertTrue(after.getCurrentActorId().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersToAct().isEmpty());
        assertTrue(after.getBettingProgress().getPlayersAllowedToRaise().isEmpty());
        assertEquals(5, after.getPlayerStates().stream().filter(PlayerState::isParticipating).count());
        assertEquals(PlayerStatus.FOLDED, after.getPlayerStates().get(4).getStatus());
        for (int seat = 0; seat < 6; seat++) {
            long expected = seat == 4 ? 0 : 20;
            assertEquals(expected, after.getPlayerStates().get(seat).getRoundContribution());
            assertEquals(expected, after.getCommittedChips(seat));
            assertEquals(1000 - expected, after.getPlayers().get(seat).getChipStack());
            assertEquals(initial.getPlayerStates().get(seat).getHoleCards(), after.getPlayerStates().get(seat).getHoleCards());
        }
        assertEquals(100, after.getPot());
        assertEquals(6000, after.getTotalChips());
        assertEquals(GamePhase.PREFLOP, after.getPhase());
        assertTrue(after.getCommunityCards().isEmpty());
        assertEquals(initial.getRemainingDeck(), after.getRemainingDeck());
        assertEquals(original, snapshot(initial));
    }

    private BettingResult act(GameState state, int seat, ActionType type) {
        return engine.apply(state, new ActionRequest(state.getPlayers().get(seat).getId(), type));
    }

    private GameState beforeBlindDecision(int seat) {
        GameState state = opening().build();
        for (int caller : new int[]{3, 4, 5, 0}) {
            state = act(state, caller, ActionType.CALL).getState();
        }
        return seat == 2 ? act(state, 1, ActionType.CALL).getState() : state;
    }

    private static FoundationFixtures.Fixture opening() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.deal();
        fixture.player(1, 990, PlayerStatus.ACTIVE, 10, 10);
        fixture.player(2, 980, PlayerStatus.ACTIVE, 20, 20);
        fixture.betting = new BettingProgress(20, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        return fixture;
    }

    private static void eliminate(FoundationFixtures.Fixture fixture, int seat) {
        fixture.deck.addAll(fixture.players.get(seat).getHoleCards());
        fixture.player(seat, 0, PlayerStatus.ELIMINATED, 0, 0);
    }

    private static void assertObservation(BettingResult result, GameState before, int seat,
            ResolvedActionType type, long added, long contribution) {
        AcceptedAction action = result.getAcceptedAction();
        assertEquals(before.getHandId(), action.getHandId());
        assertEquals(before.getPlayers().get(seat).getId(), action.getPlayerId());
        assertEquals(BettingStreet.PREFLOP, action.getStreet());
        assertEquals(type, action.getType());
        assertEquals(added, action.getChipsAdded());
        assertEquals(contribution, action.getRoundContributionAfter());
        assertFalse(action.becameAllIn());
        assertEquals(PreflopRaiseType.NONE, action.getStatsContext().getPreflopRaiseType());
        assertFalse(action.getStatsContext().hasThreeBetOpportunity());
        assertFalse(action.getStatsContext().hasFlopCBetOpportunity());
        assertFalse(action.getStatsContext().hasFacedFlopCBetOpportunity());
        assertTrue(action.toStatsAction().isAccepted());
    }

    private static List<Object> snapshot(GameState state) {
        List<Object> values = new ArrayList<>(List.of(state.getHandId(), state.getPhase(),
                state.getConfig().getStartingChips(), state.getConfig().getSmallBlind(),
                state.getConfig().getBigBlind(), state.getDealerSeat(), state.getSmallBlindSeat(),
                state.getBigBlindSeat(), state.getCurrentActorId(), state.getCommunityCards(),
                state.getRemainingDeck(), state.getCurrentBet(), state.getLastFullRaiseSize(),
                state.getBettingProgress().getPlayersToAct(), state.getBettingProgress().getPlayersAllowedToRaise()));
        for (PlayerState player : state.getPlayerStates()) {
            values.add(List.of(player.getPlayer().getId(), player.getPlayer().getSeat(),
                    player.getPlayer().getType(), player.getPlayer().getChipStack(), player.getStatus(),
                    player.getHoleCards(), player.getRoundContribution(), player.getHandContribution()));
        }
        return values;
    }

    private static final class SeatZeroRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
