package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Creates a new six-seat match. Deal cards before accepting actions in its first hand. */
public final class GameSetup {
    private final GameConfig config;
    private final Random dealerRandom;

    public GameSetup() {
        this(new GameConfig(), new Random());
    }

    /** Inject a seeded/random test source to control the uniform occupied-seat selection. */
    public GameSetup(GameConfig config, Random random) {
        this.config = config;
        this.dealerRandom = random;
    }

    /**
     * Humans take seats 0..humanCount-1; bots fill the rest. Every call starts a new match,
     * with new participant IDs and a new first-hand ID. This method does not resume a match.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public GameState create(int humanCount) {
        requireHumanCount(humanCount);
        String matchId = UUID.randomUUID().toString();
        List<PlayerState> players = createPlayers(matchId, humanCount);
        int dealer = dealerRandom.nextInt(players.size());
        SeatOrder seats = new SeatOrder(players);
        HandPositions positions = seats.positionsFor(dealer);
        postBlind(players, positions.getSmallBlindSeat(), config.getSmallBlind());
        postBlind(players, positions.getBigBlindSeat(), config.getBigBlind());
        int actorSeat = seats.firstPreflopActor(dealer).orElseThrow();
        BettingProgress betting = initialBettingProgress(players);
        return GameState.create(config, matchId + ":hand:1", GamePhase.SETUP, players, positions,
                Optional.of(players.get(actorSeat).getPlayer().getId()),
                List.of(), List.of(), betting);
    }

    private void requireHumanCount(int humanCount) {
        if (humanCount < 1 || humanCount > config.getSeatCount()) {
            throw new IllegalArgumentException("Human count must be between one and six.");
        }
    }

    private BettingProgress initialBettingProgress(List<PlayerState> players) {
        Set<String> active = players.stream().filter(PlayerState::canAct)
                .map(state -> state.getPlayer().getId()).collect(Collectors.toSet());
        return new BettingProgress(config.getBigBlind(), config.getBigBlind(), active, active);
    }

    private List<PlayerState> createPlayers(String matchId, int humanCount) {
        List<PlayerState> players = new ArrayList<>();
        for (int seat = 0; seat < config.getSeatCount(); seat++) {
            PlayerType type = seat < humanCount ? PlayerType.HUMAN : PlayerType.BOT;
            Player player = new Player(matchId + ":player:" + seat, seat, type,
                    config.getStartingChips());
            players.add(new PlayerState(player, PlayerStatus.ACTIVE, List.of(), 0, 0));
        }
        return players;
    }

    private static void postBlind(List<PlayerState> players, int seat, long amount) {
        Player before = players.get(seat).getPlayer();
        long posted = Math.min(before.getChipStack(), amount);
        long remaining = before.getChipStack() - posted;
        Player after = new Player(before.getId(), seat, before.getType(), remaining);
        PlayerStatus status = remaining == 0 ? PlayerStatus.ALL_IN : PlayerStatus.ACTIVE;
        players.set(seat, new PlayerState(after, status, List.of(), posted, posted));
    }
}
