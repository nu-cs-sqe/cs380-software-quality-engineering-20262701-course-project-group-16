package application;

import domain.model.GameState;
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
import java.util.List;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Set;

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
        BettingResult accepted = engine.apply(requireOpenHand(), request);
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
            throw new IllegalStateException("This hand already has a result awaiting settlement.");
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
     * Runs a temporary legal CHECK/CALL bot policy until a human decision or result.
     * Uses no opponent cards/equity. Each returned action was committed once.
     * @throws IllegalStateException if no match exists
     */
    public List<AcceptedAction> playBotTurns() {
        GameState match = requireMatch();
        List<AcceptedAction> played = new ArrayList<>();
        while (match.getCurrentActorSeat().isPresent()) {
            Player actor = match.getPlayers()
                    .get(match.getCurrentActorSeat().orElseThrow());
            if (actor.getType() == PlayerType.HUMAN) {
                break;
            }
            ActionType type = getAvailableActions().contains(ActionType.CHECK)
                    ? ActionType.CHECK : ActionType.CALL;
            played.add(submitAction(new ActionRequest(actor.getId(), type)));
            match = requireMatch();
        }
        selectActingHuman();
        return List.copyOf(played);
    }

    /**
     * Immutable supported actions, or empty before start/after a result.
     * The pure engine validates discarded snapshots; queries commit no observations.
     */
    public Set<ActionType> getAvailableActions() {
        if (state.isEmpty() || result.isPresent()) {
            return Set.of();
        }
        GameState match = state.orElseThrow();
        Set<ActionType> available = EnumSet.noneOf(ActionType.class);
        for (ActionType type : List.of(ActionType.FOLD, ActionType.CHECK, ActionType.CALL)) {
            try {
                engine.apply(match,
                        new ActionRequest(match.getCurrentActorId().orElseThrow(), type));
                available.add(type);
            } catch (IllegalArgumentException | UnsupportedOperationException unavailable) {
                // Legality belongs to the engine; the discarded result is never committed.
            }
        }
        return Set.copyOf(available);
    }

    /** Comparison only; no pots paid or completed-hand statistics aggregated. */
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
        publishStart(started, recorder);
    }

    private void publishStart(GameState started, Consumer<AcceptedAction> recorder) {
        statistics = Optional.of(recorder);
        state = Optional.of(started);
        result = Optional.empty();
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
                        && player.getType() == PlayerType.HUMAN)
                .findFirst().orElseThrow(() ->
                        new IllegalArgumentException("Select a human in this match."));
        viewerSeat = viewer.getSeat();
    }

    /** @throws IllegalStateException if no match exists */
    public void revealCards() {
        requireMatch();
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
        if (match.getCurrentActorSeat().isPresent()) {
            int actor = match.getCurrentActorSeat().orElseThrow();
            if (match.getPlayers().get(actor).getType() == PlayerType.HUMAN
                    && viewerSeat != actor) {
                hideCards();
                viewerSeat = actor;
            }
        }
    }

    private Optional<ShowdownResult> compareIfFinished(GameState candidate) {
        if (candidate.getPhase() == GamePhase.SHOWDOWN
                || participantCount(candidate) == 1) {
            return Optional.of(resolver.resolve(candidate));
        }
        return Optional.empty();
    }

    private GameState requireMatch() {
        return state.orElseThrow(() -> new IllegalStateException("Start a match first."));
    }
}
