package application;

import domain.model.GameState;
import domain.model.LegalActions;
import domain.model.GamePhase;
import domain.model.Player;
import domain.model.PlayerState;
import domain.model.PlayerType;
import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.BettingResult;
import domain.model.ShowdownResolver;
import domain.model.ShowdownResult;
import domain.model.Settlement;
import domain.model.PotSettlement;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Set;
import java.util.Map;
import java.util.Random;

/** Owns one played hand and explicit private-card handoffs. Use on the UI thread. */
public final class SetupSession {
    private final GameInitializer initializer;
    private final BettingEngine engine;
    private final ShowdownResolver resolver;
    private final Function<GameState, Consumer<AcceptedAction>> statisticsFactory;
    private Optional<Consumer<AcceptedAction>> statistics = Optional.empty();
    private final List<AcceptedAction> acceptedActions = new ArrayList<>();
    private Optional<GameState> state = Optional.empty();
    private Optional<ShowdownResult> result = Optional.empty();
    private Optional<Settlement> settlement = Optional.empty();
    private Optional<MatchStatistics> totals = Optional.empty();
    private final PotSettlement payoutService = new PotSettlement();
    private final NextHand continuation = new NextHand(new Random());
    private final Set<String> departed = new HashSet<>();
    private int viewerSeat;
    private boolean cardsRevealed;

    public SetupSession() {
        this(new GameInitializer());
    }

    /** @throws IllegalArgumentException if initializer is null */
    public SetupSession(GameInitializer initializer) {
        this(initializer, new BettingEngine(), new ShowdownResolver());
    }

    /** @throws IllegalArgumentException if any collaborator is null */
    public SetupSession(GameInitializer initializer, BettingEngine engine,
            ShowdownResolver resolver) {
        this(initializer, engine, resolver, HandStatistics::start);
    }

    // Recorders must reject invalid observations before modifying their pending collection.
    SetupSession(GameInitializer initializer, BettingEngine engine, ShowdownResolver resolver,
            Function<GameState, Consumer<AcceptedAction>> statisticsFactory) {
        if (initializer == null || engine == null || resolver == null
                || statisticsFactory == null) {
            throw new IllegalArgumentException("The session collaborators are required.");
        }
        this.initializer = initializer;
        this.engine = engine;
        this.resolver = resolver;
        this.statisticsFactory = statisticsFactory;
    }

    /**
     * Accepts one request, advances closed rounds and compares a finished hand.
     * Rejection preserves the hand/result/log but conceals private cards.
     * Successful actions enter pending statistics once, after comparison succeeds.
     * @return the one committed action; transitions never invent player actions
     * @throws IllegalStateException before start or after a result
     * @throws IllegalArgumentException for null, illegal or out-of-turn requests
     * @throws UnsupportedOperationException for actions outside the supported engine scope
     */
    public AcceptedAction submitAction(ActionRequest request) {
        hideCards();
        GameState hand = requireOpenHand();
        if (request == null) {
            throw new IllegalArgumentException("An action request is required.");
        }
        if (hasDeparted(request.getPlayerId()) && request.getType() != ActionType.FOLD) {
            throw new IllegalArgumentException("A departed player can only fold.");
        }
        BettingResult accepted = engine.apply(hand, request);
        GameState candidate = advanceClosedRounds(accepted.getState());
        Optional<ShowdownResult> comparison = compareIfFinished(candidate);
        AcceptedAction action = accepted.getAcceptedAction();
        statistics.orElseThrow().accept(action);
        publishAction(candidate, comparison, action);
        return action;
    }

    private GameState requireOpenHand() {
        GameState match = requireMatch();
        if (result.isPresent()) {
            throw new IllegalStateException("This hand already has a result.");
        }
        return match;
    }

    private void publishAction(GameState candidate, Optional<ShowdownResult> comparison,
            AcceptedAction action) {
        state = Optional.of(candidate);
        result = comparison;
        acceptedActions.add(action);
        selectActingHuman();
    }

    /**
     * Runs queued departure folds and temporary CHECK/CALL bots until a human turn or result.
     * Uses no opponent cards/equity. Each returned action was committed once.
     * @throws IllegalStateException if no match exists
     */
    public List<AcceptedAction> playBotTurns() {
        GameState match = requireMatch();
        List<AcceptedAction> played = new ArrayList<>();
        while (match.getCurrentActorSeat().isPresent()) {
            Player actor = match.getPlayers()
                    .get(match.getCurrentActorSeat().orElseThrow());
            boolean quitting = hasDeparted(actor.getId());
            if (actor.getType() == PlayerType.HUMAN && !quitting) {
                break;
            }
            ActionType type = quitting ? ActionType.FOLD : passiveBotAction();
            played.add(submitAction(new ActionRequest(actor.getId(), type)));
            match = requireMatch();
        }
        selectActingHuman();
        return List.copyOf(played);
    }

    private ActionType passiveBotAction() {
        return getLegalActions().getActions().contains(ActionType.CHECK)
                ? ActionType.CHECK : ActionType.CALL;
    }

    /** Immutable engine options, or empty if no manual or bot decision is available. */
    public LegalActions getLegalActions() {
        if (state.isEmpty()) {
            return LegalActions.none();
        }
        GameState match = state.orElseThrow();
        return match.getCurrentActorId().filter(this::hasDeparted).isPresent()
                ? LegalActions.none() : engine.getLegalActions(match);
    }

    public Set<ActionType> getAvailableActions() {
        return getLegalActions().getActions();
    }

    /**
     * Retires a human seat from later hands. A current actor folds immediately; another
     * active seat folds at its next turn. All-in eligibility survives until settlement.
     * @throws IllegalArgumentException if the human is unknown or already departed
     * @throws IllegalStateException if no match exists
     */
    public void quit(String playerId) {
        hideCards();
        GameState match = requireMatch();
        if (hasDeparted(playerId) || match.getPlayers().stream()
                .noneMatch(player -> player.getId().equals(playerId)
                        && player.getType() == PlayerType.HUMAN)) {
            throw new IllegalArgumentException("Choose a human player who has not left.");
        }
        if (match.getCurrentActorId().filter(playerId::equals).isPresent()) {
            submitAction(new ActionRequest(playerId, ActionType.FOLD));
        }
        departed.add(playerId);
        selectActingHuman();
    }

    public boolean hasDeparted(String playerId) { return departed.contains(playerId); }

    /** Comparison snapshot; getSettlement reports whether payment has since completed. */
    public Optional<ShowdownResult> getResult() { return result; }

    /** Immutable current-hand log, cleared only by a successful restart; excludes blinds. */
    public List<AcceptedAction> getAcceptedActions() { return List.copyOf(acceptedActions); }

    /**
     * Starts a new match with cards hidden. A rejected restart preserves the previous match.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public void start(int humanCount) {
        hideCards();
        GameState started = initializer.start(humanCount);
        Consumer<AcceptedAction> recorder =
                Objects.requireNonNull(statisticsFactory.apply(started));
        MatchStatistics fresh = MatchStatistics.start(started);
        departed.clear();
        publishStart(started, recorder);
        totals = Optional.of(fresh);
    }

    private void publishStart(GameState started, Consumer<AcceptedAction> recorder) {
        statistics = Optional.of(recorder);
        state = Optional.of(started);
        result = Optional.empty();
        settlement = Optional.empty();
        acceptedActions.clear();
        viewerSeat = 0; // Setup assigns at least one human, starting at seat zero.
    }

    /**
     * Selects a human viewer and conceals cards, even if selection is rejected.
     * @throws IllegalStateException if no match exists
     * @throws IllegalArgumentException if the ID does not identify a human in this match
     */
    public void selectViewer(String playerId) {
        hideCards();
        GameState match = requireMatch();
        Player viewer = match.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId)
                        && canView(player))
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Select a human in this match."));
        viewerSeat = viewer.getSeat();
    }

    /** @throws IllegalStateException if no match exists */
    public void revealCards() {
        if (!canView(requireMatch().getPlayers().get(viewerSeat))) {
            throw new IllegalStateException("Select a remaining human player.");
        }
        cardsRevealed = true;
    }

    /** Conceals private cards; also safe before the first match. */
    public void hideCards() {
        cardsRevealed = false;
    }

    /** Returns immutable display data; empty until a match has been started. */
    public Optional<SetupView> getView() {
        return state.map(match -> new SetupView(match, viewerSeat, cardsRevealed));
    }

    private GameState advanceClosedRounds(GameState candidate) {
        while (candidate.getCurrentActorId().isEmpty() && participantCount(candidate) > 1
                && candidate.getPhase() != GamePhase.SHOWDOWN) {
            candidate = engine.advanceStreet(candidate);
        }
        return candidate;
    }

    private static long participantCount(GameState candidate) {
        return candidate.getPlayerStates().stream().filter(PlayerState::isParticipating).count();
    }

    private void selectActingHuman() {
        GameState match = requireMatch();
        List<Player> players = match.getPlayers();
        int actor = match.getCurrentActorSeat().orElse(-1);
        if (actor >= 0 && canView(players.get(actor))) {
            changeViewer(actor);
        } else if (!canView(players.get(viewerSeat))) {
            players.stream().filter(this::canView).findFirst()
                    .ifPresent(player -> changeViewer(player.getSeat()));
        }
    }

    private boolean canView(Player player) {
        return player.getType() == PlayerType.HUMAN && !hasDeparted(player.getId());
    }

    private void changeViewer(int seat) {
        if (seat != viewerSeat) {
            hideCards();
            viewerSeat = seat;
        }
    }

    private Optional<ShowdownResult> compareIfFinished(GameState candidate) {
        if (candidate.getPhase() == GamePhase.SHOWDOWN
                || participantCount(candidate) == 1) {
            return Optional.of(resolver.resolve(candidate));
        }
        return Optional.empty();
    }

    /** Pays a finished hand and atomically publishes completed statistics, exactly once. */
    public Settlement settleHand() {
        hideCards();
        GameState hand = requireMatch();
        Settlement paid = payoutService.settle(hand);
        MatchStatistics completed = totals.orElseThrow().complete(hand, paid, acceptedActions);
        state = Optional.of(paid.getState());
        totals = Optional.of(completed);
        settlement = Optional.of(paid);
        return paid;
    }

    /** Continues a settled match with stable identities, carried stacks and fresh private cards. */
    public void nextHand() {
        hideCards();
        if (!canStartNextHand()) {
            throw new IllegalStateException("A paid hand and two remaining players are required.");
        }
        GameState started = advanceClosedRounds(continuation.start(requireMatch(), departed));
        Optional<ShowdownResult> comparison = compareIfFinished(started);
        Consumer<AcceptedAction> recorder =
                Objects.requireNonNull(statisticsFactory.apply(started));
        publishStart(started, recorder);
        result = comparison;
        selectActingHuman();
    }

    public Optional<Settlement> getSettlement() { return settlement; }

    /** Immutable completed totals for the current match; no private cards or pending rates. */
    public Map<String, StatisticsView> getStatistics() {
        return totals.map(MatchStatistics::snapshot).orElseGet(Map::of);
    }

    /** A new hand requires payment and at least two funded seats that have not left. */
    public boolean canStartNextHand() {
        return settlement.isPresent() && fundedPlayers().size() > 1;
    }

    /** A paid match with one remaining funded seat has a winner. */
    public Optional<Player> getMatchWinner() {
        if (settlement.isEmpty()) {
            return Optional.empty();
        }
        List<Player> remaining = fundedPlayers();
        return remaining.size() == 1 ? Optional.of(remaining.get(0)) : Optional.empty();
    }

    private List<Player> fundedPlayers() {
        return requireMatch().getPlayers().stream()
                .filter(player -> player.getChipStack() > 0 && !hasDeparted(player.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    private GameState requireMatch() {
        return state.orElseThrow(() -> new IllegalStateException("Start a match first."));
    }
}
