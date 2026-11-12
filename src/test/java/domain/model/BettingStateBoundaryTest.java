package domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BettingStateBoundaryTest {
    @Test
    void departedSeatsCannotRetainCardsOrCommitChipsInLaterHands() {
        Player player = new Player("p", 0, PlayerType.HUMAN, 1000);
        Card[] deck = FoundationFixtures.cards();
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerState(player, PlayerStatus.DEPARTED, List.of(deck[0], deck[1]), 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerState(player, PlayerStatus.DEPARTED, List.of(), 0, 1));
        PlayerState departed = new PlayerState(player, PlayerStatus.DEPARTED, List.of(), 0, 0);
        assertSame(player, departed.getPlayer());
        assertFalse(departed.canAct());
        assertFalse(departed.isParticipating());
        assertFalse(departed.isDealtSeat());
    }

    @Test
    void unfinishedPositionsRequireTwoDealtSeatsAndTheCorrectBigBlind() {
        FoundationFixtures.Fixture wrongBlind = FoundationFixtures.fixture();
        wrongBlind.positions = new HandPositions(0, 1, 3);
        assertThrows(IllegalArgumentException.class, wrongBlind::build);
        FoundationFixtures.Fixture lone = FoundationFixtures.fixture();
        lone.player(0, 6000, PlayerStatus.ACTIVE, 0, 0);
        for (int seat = 1; seat < 6; seat++) lone.player(seat, 0, PlayerStatus.ELIMINATED, 0, 0);
        lone.actor = null;
        lone.betting = new BettingProgress(0, 20, Set.of(), Set.of());
        assertThrows(IllegalArgumentException.class, lone::build);
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"SHOWDOWN", "COMPLETION"})
    void terminalSnapshotsRejectRaiseRightsEvenWithoutAnActor(GamePhase phase) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(phase, 5);
        fixture.betting = new BettingProgress(0, 20, Set.of(), Set.of("p0"));
        assertThrows(IllegalArgumentException.class, fixture::build);
    }

    @Test
    void paidTurnUncontestedSnapshotRetainsFourCommunityCards() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(GamePhase.COMPLETION, 4);
        assertEquals(4, fixture.build().getCommunityCards().size());
    }

    @Test
    void completedSnapshotAcceptsTheInclusiveRaiseCountSafetyBound() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(GamePhase.COMPLETION, 0);
        BettingHistory history = new BettingHistory("p0", 0, 5980);
        assertEquals(5980, PostflopFixtures.build(fixture, history)
                .getBettingHistory().getPreflopRaiseCount());
    }

    @Test
    void laterStreetCanRetainContinuationBetWithExactlyEnoughCommittedChips() {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(GamePhase.TURN, 4);
        fixture.player(0, 980, PlayerStatus.ACTIVE, 0, 20);
        BettingHistory history = new BettingHistory("p0", 20);
        assertEquals(20, PostflopFixtures.build(fixture, history)
                .getBettingHistory().getFlopCBetAmount().orElseThrow());
    }

    @Test
    void nominalShortBlindCannotSurvivePostflopOrRecordedAggression() {
        FoundationFixtures.Fixture flop = shortBlind(GamePhase.FLOP, PlayerStatus.ALL_IN);
        assertThrows(IllegalArgumentException.class, flop::build);
        FoundationFixtures.Fixture preflop = shortBlind(GamePhase.PREFLOP, PlayerStatus.ALL_IN);
        assertEquals(20, preflop.build().getCurrentBet());
        assertThrows(IllegalArgumentException.class, () ->
                PostflopFixtures.build(preflop, new BettingHistory("p0", 0)));
        FoundationFixtures.Fixture activeBlind = shortBlind(GamePhase.PREFLOP, PlayerStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class, activeBlind::build);
    }

    private static FoundationFixtures.Fixture shortBlind(GamePhase phase, PlayerStatus status) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(phase, phase == GamePhase.FLOP ? 3 : 0);
        Set<String> active = new HashSet<>(FoundationFixtures.allIds());
        if (status == PlayerStatus.ALL_IN) {
            fixture.player(0, 1985, PlayerStatus.ACTIVE, 0, 0);
            fixture.player(2, 0, status, 15, 15);
            active.remove("p2");
        } else {
            fixture.player(2, 985, status, 15, 15);
        }
        fixture.betting = new BettingProgress(20, 20, active, active);
        return fixture;
    }
}
