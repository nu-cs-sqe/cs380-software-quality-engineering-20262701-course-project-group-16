package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Explicit snapshots for postflop contracts; expected results stay in individual tests. */
final class PostflopFixtures {
    static FoundationFixtures.Fixture street(GamePhase phase) {
        FoundationFixtures.Fixture fixture = FoundationFixtures.fixture();
        fixture.board(phase, phase == GamePhase.FLOP ? 3 : phase == GamePhase.TURN ? 4 : 5);
        for (int seat = 0; seat < 6; seat++) {
            fixture.player(seat, 960, PlayerStatus.ACTIVE, 0, 40);
        }
        fixture.actor = "p1";
        fixture.betting = new BettingProgress(0, 20, FoundationFixtures.allIds(), FoundationFixtures.allIds());
        return fixture;
    }

    static void contribution(FoundationFixtures.Fixture fixture, int seat, long round) {
        fixture.player(seat, 960 - round, PlayerStatus.ACTIVE, round, 40 + round);
    }

    static GameState build(FoundationFixtures.Fixture fixture, BettingHistory history) {
        return new GameState(fixture.config, fixture.hand, fixture.phase, fixture.players,
                fixture.positions, fixture.actor, fixture.community, fixture.deck, fixture.betting, history);
    }

    static GameState withHistory(GameState before, BettingHistory history) {
        return new GameState(before.getConfig(), before.getHandId(), before.getPhase(),
                before.getPlayerStates(), before.getPositions(), before.getCurrentActorId().orElse(null),
                before.getCommunityCards(), before.getRemainingDeck(), before.getBettingProgress(), history);
    }

    static void close(FoundationFixtures.Fixture fixture) {
        fixture.actor = null;
        fixture.betting = new BettingProgress(fixture.betting.getCurrentBet(),
                fixture.betting.getLastFullRaiseSize(), Set.of(), Set.of());
    }

    static List<Object> snapshot(GameState state) {
        List<Object> values = new ArrayList<>(List.of(state.getHandId(), state.getPhase(),
                state.getConfig().getStartingChips(), state.getConfig().getSmallBlind(),
                state.getConfig().getBigBlind(), state.getDealerSeat(), state.getSmallBlindSeat(),
                state.getBigBlindSeat(), state.getCurrentActorId(), state.getCommunityCards(),
                state.getRemainingDeck(), state.getCurrentBet(), state.getLastFullRaiseSize(),
                state.getBettingProgress().getPlayersToAct(), state.getBettingProgress().getPlayersAllowedToRaise(),
                state.getBettingHistory().getFinalPreflopAggressorId(), state.getBettingHistory().getFlopCBetAmount()));
        for (PlayerState player : state.getPlayerStates()) {
            values.add(List.of(player.getPlayer().getId(), player.getPlayer().getSeat(),
                    player.getPlayer().getType(), player.getPlayer().getChipStack(), player.getStatus(),
                    player.getHoleCards(), player.getRoundContribution(), player.getHandContribution()));
        }
        return values;
    }
}
