package application;

import domain.model.BettingProgress;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.HandPositions;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.SeatOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Rotates the moving button and deals again without replacing match participants. */
final class NextHand {
    private final InitialDeal dealing;

    NextHand(Random random) { dealing = new InitialDeal(random); }

    GameState start(GameState previous) {
        if (previous.getPhase() != GamePhase.COMPLETION) {
            throw new IllegalStateException("Settle the previous hand first.");
        }
        List<PlayerState> players = emptyHands(previous);
        SeatOrder seats = new SeatOrder(players);
        int dealer = seats.nextDealerAfter(previous.getDealerSeat()).orElseThrow();
        HandPositions positions = seats.positionsFor(dealer);
        postBlind(players, positions.getSmallBlindSeat(), previous.getConfig().getSmallBlind());
        postBlind(players, positions.getBigBlindSeat(), previous.getConfig().getBigBlind());
        return dealing.deal(table(previous, players, positions));
    }

    private static List<PlayerState> emptyHands(GameState previous) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState player : previous.getPlayerStates()) {
            players.add(new PlayerState(player.getPlayer(), player.getStatus(), List.of(), 0, 0));
        }
        return players;
    }

    private static void postBlind(List<PlayerState> players, int seat, long blind) {
        Player old = players.get(seat).getPlayer();
        long amount = Math.min(old.getChipStack(), blind);
        long chips = old.getChipStack() - amount;
        Player player = new Player(old.getId(), seat, old.getType(), chips);
        players.set(seat, new PlayerState(player, chips == 0 ? PlayerStatus.ALL_IN
                : PlayerStatus.ACTIVE, List.of(), amount, amount));
    }

    private static GameState table(GameState previous, List<PlayerState> players,
            HandPositions positions) {
        long currentBet = players.stream().mapToLong(PlayerState::getRoundContribution)
                .max().orElseThrow();
        Set<String> pending = pendingPlayers(players, currentBet);
        String actor = pending.isEmpty() ? null : players.get(new SeatOrder(players)
                .firstPreflopActor(positions.getDealerSeat()).orElseThrow()).getPlayer().getId();
        return new GameState(previous.getConfig(), UUID.randomUUID().toString(), GamePhase.SETUP,
                players, positions, actor, List.of(), List.of(),
                new BettingProgress(currentBet, previous.getConfig().getBigBlind(), pending,
                        pending.size() > 1 ? pending : Set.of()));
    }

    private static Set<String> pendingPlayers(List<PlayerState> players, long currentBet) {
        List<PlayerState> active = players.stream().filter(PlayerState::canAct)
                .collect(Collectors.toList());
        if (active.size() == 1 && active.get(0).getRoundContribution() >= currentBet) {
            return Set.of();
        }
        return active.stream().map(player -> player.getPlayer().getId())
                .collect(Collectors.toSet());
    }
}
