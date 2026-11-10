package domain.model;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable internal hand snapshot. Only the engine receives this object: interfaces and bots
 * receive viewFor(playerId). Chip transfers, legality checks and phase transitions belong to
 * engine operations which construct the next snapshot.
 */
public final class GameState {
    private final GameConfig config;
    private final String handId;
    private final GamePhase phase;
    private final List<PlayerState> players;
    private final HandPositions positions;
    private final Optional<String> currentActorId;
    private final List<Card> communityCards;
    private final List<Card> remainingDeck;
    private final BettingProgress betting;
    private final BettingHistory history;

    /**
     * Creates a validated snapshot. A null actor is accepted at this legacy boundary
     * and converted immediately to Optional.empty(); internal state never stores null.
     * The remaining deck is in deal order; burned/discarded cards need not appear in it.
     * All six seats remain after elimination. Contributions hold unawarded chips only.
     * @throws IllegalArgumentException for missing/inconsistent state or unsafe chip totals
     */
    public GameState(GameConfig config, String handId, GamePhase phase, List<PlayerState> players,
            HandPositions positions, String currentActorId, List<Card> communityCards,
            List<Card> remainingDeck, BettingProgress betting) {
        this(config, handId, phase, players, positions, currentActorId, communityCards,
                remainingDeck, betting, new BettingHistory());
    }

    /**
     * Creates the same immutable snapshot with explicit engine-recorded betting provenance.
     * Use this overload to preserve aggression/c-bet history in later snapshots.
     * @throws IllegalArgumentException for invalid state or inconsistent history
     */
    public GameState(GameConfig config, String handId, GamePhase phase, List<PlayerState> players,
            HandPositions positions, String currentActorId, List<Card> communityCards,
            List<Card> remainingDeck, BettingProgress betting, BettingHistory history) {
        if (config == null || handId == null || handId.isBlank() || phase == null
                || positions == null || betting == null || history == null) {
            throw new IllegalArgumentException(
                "Settings, hand identity, phase, positions and betting are required.");
        }
        this.config = config;
        this.handId = handId;
        this.phase = phase;
        this.players = copyPlayers(players);
        this.positions = positions;
        this.currentActorId = Optional.ofNullable(currentActorId);
        this.communityCards = copyCards(communityCards);
        this.remainingDeck = copyCards(remainingDeck);
        this.betting = betting;
        this.history = history;
        validatePositions();
        validateCards();
        validateChipTotal();
        validateBetting();
        validateHistory();
    }

    public BettingHistory getBettingHistory() {
        return history;
    }

    private void validateHistory() {
        Optional<String> aggressorId = history.getFinalPreflopAggressorId();
        if (aggressorId.isEmpty()) {
            return;
        }
        if (phase == GamePhase.SETUP) {
            throw new IllegalArgumentException("Betting history cannot precede dealing.");
        }
        PlayerState aggressor = players.stream()
                .filter(player -> player.getPlayer().getId().equals(aggressorId.orElseThrow()))
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Unknown preflop aggressor."));
        if (phase != GamePhase.COMPLETION && aggressor.getStatus() == PlayerStatus.ELIMINATED) {
            throw new IllegalArgumentException(
                    "An eliminated seat did not participate in this hand.");
        }
        if (history.getFlopCBetAmount().isPresent()) {
            long amount = history.getFlopCBetAmount().orElseThrow();
            if (phase == GamePhase.PREFLOP
                    || (phase == GamePhase.FLOP && aggressor.getRoundContribution() < amount)
                    || (phase != GamePhase.COMPLETION
                    && aggressor.getHandContribution() < amount)) {
                throw new IllegalArgumentException(
                        "The recorded flop c-bet is inconsistent with this hand.");
            }
        }
    }

    private static List<PlayerState> copyPlayers(List<PlayerState> players) {
        new SeatOrder(players);
        List<PlayerState> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparingInt(state -> state.getPlayer().getSeat()));
        return List.copyOf(sorted);
    }

    private static List<Card> copyCards(List<Card> cards) {
        if (cards == null || cards.stream().anyMatch(card -> card == null)) {
            throw new IllegalArgumentException("Card collections and elements must be non-null.");
        }
        return List.copyOf(cards);
    }

    private void validatePositions() {
        if (phase == GamePhase.COMPLETION) {
            return; // Last hand's positions remain historical after elimination and payout.
        }
        HandPositions expected;
        try {
            expected = new SeatOrder(players).positionsFor(positions.getDealerSeat());
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException(
                "An unfinished hand requires two remaining players.", exception);
        }
        if (positions.getSmallBlindSeat() != expected.getSmallBlindSeat()
                || positions.getBigBlindSeat() != expected.getBigBlindSeat()) {
            throw new IllegalArgumentException(
                "Positions must follow clockwise and heads-up rules.");
        }
    }

    private void validateCards() {
        int count = communityCards.size();
        boolean validBoard;
        switch (phase) {
            case SETUP:
            case PREFLOP:
                validBoard = count == 0;
                break;
            case FLOP:
                validBoard = count == 3;
                break;
            case TURN:
                validBoard = count == 4;
                break;
            case RIVER:
            case SHOWDOWN:
                validBoard = count == 5;
                break;
            case COMPLETION:
                validBoard = count == 0 || count == 3 || count == 4 || count == 5;
                break;
            default:
                throw new IllegalArgumentException("Unknown phase.");
        }
        if (!validBoard) {
            throw new IllegalArgumentException("Community card count does not match the phase.");
        }
        Set<Card> seen = new HashSet<>();
        addDistinct(seen, communityCards);
        addDistinct(seen, remainingDeck);
        for (PlayerState state : players) {
            List<Card> cards = state.getHoleCards();
            if ((phase == GamePhase.SETUP && !cards.isEmpty())
                    || (phase != GamePhase.SETUP && phase != GamePhase.COMPLETION
                    && state.getStatus() != PlayerStatus.ELIMINATED && cards.size() != 2)) {
                throw new IllegalArgumentException(
                    "Hole cards must match setup or dealt-hand status.");
            }
            addDistinct(seen, cards);
        }
    }

    private static void addDistinct(Set<Card> seen, List<Card> cards) {
        for (Card card : cards) {
            if (!seen.add(card)) {
                throw new IllegalArgumentException(
                    "A physical card cannot occur in two locations.");
            }
        }
    }

    private void validateBetting() {
        long maximum = players.stream().mapToLong(PlayerState::getRoundContribution)
                .max().orElseThrow();
        if ((betting.getCurrentBet() != maximum && !hasShortBlindTarget(maximum))
                || betting.getLastFullRaiseSize() < config.getBigBlind()) {
            throw new IllegalArgumentException("Current bet or last full raise is inconsistent.");
        }
        Set<String> eligible = players.stream().filter(PlayerState::canAct)
                .map(state -> state.getPlayer().getId()).collect(Collectors.toSet());
        if (!eligible.containsAll(betting.getPlayersToAct())
                || !eligible.containsAll(betting.getPlayersAllowedToRaise())
                || !eligible.containsAll(betting.getLastDecisionTargets().keySet())) {
            throw new IllegalArgumentException(
                "Only active players may have pending decisions or raise rights.");
        }
        boolean pending = !betting.getPlayersToAct().isEmpty();
        if (pending != currentActorId.isPresent()
                || currentActorId.filter(id -> !betting.getPlayersToAct().contains(id))
                        .isPresent()) {
            throw new IllegalArgumentException(
                "The current actor must be a pending active player.");
        }
        if ((phase == GamePhase.SHOWDOWN || phase == GamePhase.COMPLETION)
                && (pending || currentActorId.isPresent()
                || !betting.getPlayersAllowedToRaise().isEmpty())) {
            throw new IllegalArgumentException("A terminal phase has no betting decisions.");
        }
        if (phase == GamePhase.COMPLETION && getPot() != 0) {
            throw new IllegalArgumentException(
                "Completed hands must have awarded all committed chips.");
        }
    }

    private boolean hasShortBlindTarget(long maximum) {
        PlayerState bigBlind = players.get(positions.getBigBlindSeat());
        return (phase == GamePhase.SETUP || phase == GamePhase.PREFLOP)
                && maximum < config.getBigBlind()
                && betting.getCurrentBet() == config.getBigBlind()
                && bigBlind.getStatus() == PlayerStatus.ALL_IN
                && history.getFinalPreflopAggressorId().isEmpty();
    }

    private void validateChipTotal() {
        try {
            if (getTotalChips() != config.getInitialTotalChips()) {
                throw new IllegalArgumentException(
                    "Stacks plus commitments must conserve match chips.");
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                "Chip totals exceed the supported long range.", exception);
        }
    }

    public GameConfig getConfig() { return config; }
    public String getHandId() { return handId; }
    public GamePhase getPhase() { return phase; }
    public List<Player> getPlayers() {
        return players.stream().map(PlayerState::getPlayer)
                .collect(Collectors.toUnmodifiableList());
    }
    public List<PlayerState> getPlayerStates() { return players; }
    public HandPositions getPositions() { return positions; }
    public int getDealerSeat() { return positions.getDealerSeat(); }
    public int getSmallBlindSeat() { return positions.getSmallBlindSeat(); }
    public int getBigBlindSeat() { return positions.getBigBlindSeat(); }
    public Optional<String> getCurrentActorId() { return currentActorId; }
    public OptionalInt getCurrentActorSeat() {
        if (currentActorId.isEmpty()) {
            return OptionalInt.empty();
        }
        String actor = currentActorId.orElseThrow();
        return OptionalInt.of(players.stream()
                .filter(state -> state.getPlayer().getId().equals(actor))
                .findFirst().orElseThrow().getPlayer().getSeat());
    }
    public List<Card> getCommunityCards() { return communityCards; }
    public List<Card> getRemainingDeck() { return remainingDeck; }
    public BettingProgress getBettingProgress() { return betting; }
    public long getCurrentBet() { return betting.getCurrentBet(); }
    public long getLastFullRaiseSize() { return betting.getLastFullRaiseSize(); }

    /** @throws IllegalArgumentException for seats outside 0-5 */
    public long getCommittedChips(int seat) {
        if (seat < 0 || seat >= players.size()) {
            throw new IllegalArgumentException("Seat must be between zero and five.");
        }
        return players.get(seat).getHandContribution();
    }

    /** Derived from whole-hand contributions, which include the current round. */
    public long getPot() {
        long pot = 0;
        for (PlayerState state : players) {
            pot = Math.addExact(pot, state.getHandContribution());
        }
        return pot;
    }

    public long getTotalChips() {
        long total = getPot();
        for (PlayerState state : players) {
            total = Math.addExact(total, state.getPlayer().getChipStack());
        }
        return total;
    }

    /** @throws IllegalArgumentException if the viewer is not a player in this match */
    public GameView viewFor(String playerId) {
        if (playerId == null || players.stream()
                .noneMatch(state -> state.getPlayer().getId().equals(playerId))) {
            throw new IllegalArgumentException("A view requires an existing player ID.");
        }
        return new GameView(this, playerId);
    }
}
