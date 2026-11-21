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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Rotates the moving button and deals again without replacing match participants. */
final class NextHand {
    private final InitialDeal dealing;

    NextHand(Random random) { dealing = new InitialDeal(random); }

    GameState start(GameState previous) {
        return start(previous, Set.of());
    }

    GameState start(GameState previous, Set<String> departed) {
        if (previous.getPhase() != GamePhase.COMPLETION) {
            throw new IllegalStateException("Settle the previous hand first.");
        }
        List<PlayerState> players = emptyHands(previous, departed);
        SeatOrder seats = new SeatOrder(players);
        int dealer = seats.nextDealerAfter(previous.getDealerSeat()).orElseThrow();
        HandPositions positions = seats.positionsFor(dealer);
        postBlind(players, positions.getSmallBlindSeat(), previous.getConfig().getSmallBlind());
        postBlind(players, positions.getBigBlindSeat(), previous.getConfig().getBigBlind());
        return dealing.deal(table(previous, players, positions));
    }

    /** Deals a funded account roster, with seat identities retained or replaced explicitly. */
    GameState reseat(GameState previous, List<Player> roster, boolean rotate) {
        List<PlayerState> players = fundedSeats(roster);
        SeatOrder seats = new SeatOrder(players);
        int dealer = rotate
                ? seats.nextDealerAfter(previous.getDealerSeat()).orElseThrow()
                : previous.getDealerSeat();
        GameState funded = fundedTable(previous, players);
        HandPositions positions = seats.positionsFor(dealer);
        postBlind(players, positions.getSmallBlindSeat(), funded.getConfig().getSmallBlind());
        postBlind(players, positions.getBigBlindSeat(), funded.getConfig().getBigBlind());
        return dealing.deal(table(funded, players, positions));
    }

    private static List<PlayerState> fundedSeats(List<Player> roster) {
        List<PlayerState> players = new ArrayList<>();
        for (Player player : roster) {
            players.add(new PlayerState(player, player.getChipStack() == 0
                    ? PlayerStatus.ELIMINATED : PlayerStatus.ACTIVE, List.of(), 0, 0));
        }
        return players;
    }

    private static GameState fundedTable(GameState previous, List<PlayerState> players) {
        long total = 0;
        for (PlayerState player : players) {
            total = Math.addExact(total, player.getPlayer().getChipStack());
        }
        return GameState.create(previous.getConfig().withTableTotal(total), previous.getHandId(),
                GamePhase.COMPLETION, players, previous.getPositions(), Optional.empty(),
                List.of(), List.of(), new BettingProgress(0, previous.getConfig().getBigBlind(),
                        Set.of(), Set.of()));
    }

    private static List<PlayerState> emptyHands(GameState previous, Set<String> departed) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState player : previous.getPlayerStates()) {
            PlayerStatus status = departed.contains(player.getPlayer().getId())
                    ? PlayerStatus.DEPARTED : player.getStatus();
            players.add(new PlayerState(player.getPlayer(), status, List.of(), 0, 0));
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
        long currentBet = initialBetTarget(previous, players);
        Set<String> pending = pendingPlayers(players, currentBet);
        Optional<String> actor = firstPreflopActor(players, positions, pending);
        return GameState.create(previous.getConfig(), UUID.randomUUID().toString(), GamePhase.SETUP,
                players, positions, actor, List.of(), List.of(),
                new BettingProgress(currentBet, previous.getConfig().getBigBlind(), pending,
                        pending.size() > 1 ? pending : Set.of()));
    }

    private static long initialBetTarget(GameState previous, List<PlayerState> players) {
        long postedMaximum = players.stream().mapToLong(PlayerState::getRoundContribution)
                .max().orElseThrow();
        return players.stream().filter(PlayerState::canAct).count() > 1
                ? Math.max(postedMaximum, previous.getConfig().getBigBlind()) : postedMaximum;
    }

    private static Optional<String> firstPreflopActor(List<PlayerState> players,
            HandPositions positions, Set<String> pending) {
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        int seat = new SeatOrder(players)
                .firstPreflopActor(positions.getDealerSeat()).orElseThrow();
        return Optional.of(players.get(seat).getPlayer().getId());
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
