package application;

import domain.model.AcceptedAction;
import domain.model.BettingStreet;
import domain.model.Card;
import domain.model.GameConfig;
import domain.model.GamePhase;
import domain.model.GameState;
import domain.model.PlayerState;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.PokerHandEvaluator;
import domain.model.ResolvedActionType;
import domain.model.Settlement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Owns disclosure and the public journal for one deal. The undealt deck never enters the log. */
public final class ReviewedHand {
    public enum Visibility { HIDDEN, VOLUNTARILY_SHOWN, SHOWDOWN_REVEALED, MUCKED }
    private final Map<String, List<Card>> pockets = new LinkedHashMap<>();
    private final Map<String, Visibility> visibility = new LinkedHashMap<>();
    private final Set<String> bots = new HashSet<>();
    private final Set<String> folded = new HashSet<>();
    private final Set<String> forfeited = new HashSet<>();
    private final Set<String> deferred = new LinkedHashSet<>();
    private final Set<String> winners = new HashSet<>();
    private final Deque<String> pending = new ArrayDeque<>();
    private final HandHistory history;
    private final boolean immediate;
    private final boolean showBots;
    private final boolean tableLiveHands;
    private final PokerHandEvaluator evaluator = new PokerHandEvaluator();
    private List<Card> board = List.of();
    private Optional<String> riverAggressor = Optional.empty();
    private boolean finishedBetting;
    private Map<String, Object> runout = Map.of();

    ReviewedHand(GameState opening, boolean immediate, boolean showBots) {
        this(opening, immediate, showBots, false);
    }

    ReviewedHand(GameState opening, boolean immediate, boolean showBots, boolean tableLiveHands) {
        this.tableLiveHands = tableLiveHands;
        this.immediate = immediate;
        this.showBots = showBots;
        history = new HandHistory(opening);
        registerDealtSeats(opening.getPlayerStates());
        advance(opening, opening.getPhase() == GamePhase.SHOWDOWN);
    }

    private void registerDealtSeats(List<PlayerState> seats) {
        for (PlayerState seat : seats) {
            String id = seat.getPlayer().getId();
            if (seat.isDealtSeat()) {
                pockets.put(id, seat.getHoleCards());
                visibility.put(id, Visibility.HIDDEN);
            }
            if (seat.getPlayer().getType() == PlayerType.BOT) {
                bots.add(id);
            }
        }
    }

    void accepted(AcceptedAction action, GameState next, boolean terminal, boolean showFold) {
        history.action(action);
        recordFold(action, showFold);
        recordRiverAggressor(action);
        advance(next, terminal);
    }

    private void recordFold(AcceptedAction action, boolean showFold) {
        String id = action.getPlayerId();
        if (action.getType() == ResolvedActionType.FOLD) {
            folded.add(id);
            if (showFold || (bots.contains(id) && showBots)) {
                optional(id, true);
            }
        }
    }

    private void recordRiverAggressor(AcceptedAction action) {
        if (action.getStreet() == BettingStreet.RIVER && (action.getType() == ResolvedActionType.BET
                || action.getType() == ResolvedActionType.RAISE)) {
            riverAggressor = Optional.of(action.getPlayerId());
        }
    }

    private void advance(GameState next, boolean terminal) {
        List<PlayerState> live = next.getPlayerStates().stream()
                .filter(PlayerState::isParticipating).collect(Collectors.toList());
        boolean revealAllIn = terminal && isContestedAllIn(live);
        if (revealAllIn) {
            exposeForRunout(live, next.getCommunityCards().size());
        }
        board = next.getCommunityCards();
        history.deal(board);
        if (terminal) {
            startShowdown(next, live, revealAllIn);
        }
    }

    private static boolean isContestedAllIn(List<PlayerState> live) {
        return live.size() > 1 && live.stream()
                .anyMatch(seat -> seat.getStatus() == PlayerStatus.ALL_IN);
    }

    private void exposeForRunout(List<PlayerState> live, int finalBoardSize) {
        for (PlayerState seat : live) {
            expose(seat.getPlayer().getId(), Visibility.SHOWDOWN_REVEALED);
        }
        if (board.size() < finalBoardSize) {
            runout = Map.of("from", board.size(), "shown", shown());
        }
    }

    private void startShowdown(GameState state, List<PlayerState> live, boolean exposedAllIn) {
        finishedBetting = true;
        history.showdown();
        if (!exposedAllIn && live.size() > 1) {
            startRiverDisclosure(state, live);
        }
        finishChoices();
    }

    private void startRiverDisclosure(GameState state, List<PlayerState> live) {
        int first = riverAggressor.flatMap(id -> live.stream()
                .filter(seat -> id.equals(seat.getPlayer().getId())).findFirst())
                .map(seat -> seat.getPlayer().getSeat()).orElse(state.getDealerSeat() + 1);
        live.sort(Comparator.comparingInt(seat ->
                Math.floorMod(seat.getPlayer().getSeat() - first, GameConfig.SEAT_COUNT)));
        for (PlayerState seat : live) {
            String id = seat.getPlayer().getId();
            if (tableLiveHands) {
                expose(id, Visibility.SHOWDOWN_REVEALED);
            } else {
                pending.addLast(id);
            }
        }
    }

    public Optional<String> nextToShow() {
        return pending.isEmpty() ? Optional.empty() : Optional.of(pending.getFirst());
    }

    public Map<String, Visibility> visibility() { return Map.copyOf(visibility); }

    public Map<String, List<Card>> shown() {
        Map<String, List<Card>> publicCards = new LinkedHashMap<>();
        visibility.forEach((id, state) -> {
            if (state == Visibility.VOLUNTARILY_SHOWN || state == Visibility.SHOWDOWN_REVEALED) {
                publicCards.put(id, pockets.get(id));
            }
        });
        return Map.copyOf(publicCards);
    }

    public List<Map<String, Object>> events() { return history.events(); }
    public Set<String> folded() { return Set.copyOf(folded); }
    public boolean immediate() { return immediate; }
    public Map<String, Object> runout() { return runout; }

    /** The desktop adapter authorizes the active seat owner before this private read. */
    public List<Card> ownerCards(String id) {
        requireDealtHuman(id);
        return pockets.get(id);
    }

    private void requireDealtHuman(String id) {
        if (bots.contains(id) || !pockets.containsKey(id)) {
            throw new IllegalArgumentException("Choose a dealt human seat.");
        }
    }

    void decide(String id, boolean show) {
        requireDealtHuman(id);
        boolean optionalMuck = visibility.get(id) == Visibility.MUCKED && !forfeited.contains(id);
        if (visibility.get(id) != Visibility.HIDDEN && !optionalMuck) {
            throw new IllegalArgumentException("This seat cannot make a disclosure decision.");
        }
        if (nextToShow().filter(id::equals).isPresent()) {
            recordShowdownChoice(pending.removeFirst(), show);
            finishChoices();
        } else if (folded.contains(id) || showdownChoicesComplete()) {
            optional(id, show);
        } else {
            throw new IllegalArgumentException("Wait for this seat's showdown turn.");
        }
    }

    private void optional(String id, boolean show) {
        if (!show) {
            deferred.remove(id);
            if (visibility.get(id) != Visibility.MUCKED) {
                muck(id);
            }
        } else if (immediate || showdownChoicesComplete()) {
            expose(id, Visibility.VOLUNTARILY_SHOWN);
        } else {
            deferred.add(id);
        }
    }

    private boolean showdownChoicesComplete() {
        return finishedBetting && pending.isEmpty();
    }

    private void finishChoices() {
        resolveBotChoices();
        if (pending.isEmpty()) {
            publishOptionalShows();
        }
    }

    private void resolveBotChoices() {
        while (!pending.isEmpty()) {
            String id = pending.getFirst();
            if (pending.size() == 1 && !anyTabled()) {
                // The sole claimant keeps the pot and can still choose to show later.
                pending.removeFirst();
                return;
            }
            if (!bots.contains(id)) {
                return;
            }
            boolean show = showBots || canBeatTabled(id);
            recordShowdownChoice(pending.removeFirst(), show);
        }
    }

    private void publishOptionalShows() {
        deferred.forEach(id -> expose(id, Visibility.VOLUNTARILY_SHOWN));
        deferred.clear();
        if (showBots) {
            visibility.forEach((id, state) -> {
                if (bots.contains(id) && state == Visibility.HIDDEN) {
                    expose(id, Visibility.VOLUNTARILY_SHOWN);
                }
            });
        }
    }

    private boolean anyTabled() { return visibility.containsValue(Visibility.SHOWDOWN_REVEALED); }

    private boolean canBeatTabled(String id) {
        return visibility.entrySet().stream()
                .filter(entry -> entry.getValue() == Visibility.SHOWDOWN_REVEALED)
                .allMatch(entry -> evaluator.compare(holding(id), holding(entry.getKey())) >= 0);
    }

    private List<Card> holding(String id) {
        List<Card> cards = new ArrayList<>(board);
        cards.addAll(pockets.get(id));
        return cards;
    }

    private void recordShowdownChoice(String id, boolean show) {
        if (show) {
            expose(id, Visibility.SHOWDOWN_REVEALED);
        } else {
            forfeited.add(id);
            muck(id);
        }
    }

    private void muck(String id) {
        visibility.put(id, Visibility.MUCKED);
        history.muck(id);
    }

    private void expose(String id, Visibility state) {
        visibility.put(id, state);
        Optional<String> ranking = winners.contains(id) ? category(id) : Optional.empty();
        history.show(id, state.name(), pockets.get(id), ranking);
    }

    private Optional<String> category(String id) {
        List<Card> cards = holding(id);
        return cards.size() >= PokerHandEvaluator.HAND_SIZE
                ? Optional.of(evaluator.category(cards)) : Optional.empty();
    }

    Set<String> withdrawnClaims() {
        if (!pending.isEmpty()) {
            throw new IllegalStateException("Finish the showdown decisions before paying the pot.");
        }
        return Set.copyOf(forfeited);
    }

    void paid(Settlement settlement) {
        settlement.getPayouts().forEach((id, amount) -> {
            if (amount > 0) {
                winners.add(id);
            }
        });
        history.award(settlement, categories());
    }

    public Map<String, List<Card>> winningCards() {
        Map<String, List<Card>> best = new LinkedHashMap<>();
        categories().keySet().forEach(id -> best.put(id, evaluator.winningCards(holding(id))));
        return Map.copyOf(best);
    }

    public boolean showAfterHand(String id) {
        requireDealtHuman(id);
        return deferred.contains(id);
    }

    public Map<String, String> categories() {
        Map<String, String> categories = new LinkedHashMap<>();
        shown().keySet().stream().filter(winners::contains).forEach(id ->
                category(id).ifPresent(name -> categories.put(id, name)));
        return Map.copyOf(categories);
    }
}
