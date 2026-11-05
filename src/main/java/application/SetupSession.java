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
import java.util.Set;

/** Owns one played hand and explicit private-card handoffs. Use on the UI thread. */
public final class SetupSession {
    private final GameInitializer initializer;
    private final BettingEngine engine;
    private final ShowdownResolver resolver;
    private final List<AcceptedAction> acceptedActions = new ArrayList<>();
    private GameState state;
    private ShowdownResult result;
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
    public SetupSession(GameInitializer initializer, BettingEngine engine, ShowdownResolver resolver) {
        if (initializer == null || engine == null || resolver == null) {
            throw new IllegalArgumentException("Initializer, betting engine and resolver are required.");
        }
        this.initializer = initializer;
        this.engine = engine;
        this.resolver = resolver;
    }

    /**
     * Accepts one request, advances closed rounds and compares a finished hand.
     * Rejection preserves the hand/result/log but conceals private cards.
     * @return the one committed action; transitions never invent player actions
     * @throws IllegalStateException before start or after a result
     * @throws IllegalArgumentException for null, illegal or out-of-turn requests
     * @throws UnsupportedOperationException for actions outside the supported engine scope
     */
    public AcceptedAction submitAction(ActionRequest request) {
        hideCards();
        requireMatch();
        if (result != null) {
            throw new IllegalStateException("This hand already has a result awaiting settlement.");
        }
        BettingResult accepted = engine.apply(state, request);
        GameState candidate = advanceClosedRounds(accepted.getState());
        ShowdownResult comparison = candidate.getPhase() == GamePhase.SHOWDOWN
                || participantCount(candidate) == 1 ? resolver.resolve(candidate) : null;
        state = candidate;
        result = comparison;
        acceptedActions.add(accepted.getAcceptedAction());
        selectActingHuman();
        return accepted.getAcceptedAction();
    }

    /**
     * Runs a temporary legal CHECK/CALL bot policy until a human decision or result.
     * Uses no opponent cards/equity. Each returned action was committed once.
     * @throws IllegalStateException if no match exists
     */
    public List<AcceptedAction> playBotTurns() {
        requireMatch();
        List<AcceptedAction> played = new ArrayList<>();
        while (state.getCurrentActorSeat().isPresent()) {
            Player actor = state.getPlayers().get(state.getCurrentActorSeat().orElseThrow());
            if (actor.getType() == PlayerType.HUMAN) {
                break;
            }
            ActionType type = getAvailableActions().contains(ActionType.CHECK)
                    ? ActionType.CHECK : ActionType.CALL;
            played.add(submitAction(new ActionRequest(actor.getId(), type)));
        }
        selectActingHuman();
        return List.copyOf(played);
    }

    /**
     * Immutable supported actions, or empty before start/after a result.
     * The pure engine validates discarded snapshots; queries commit no observations.
     */
    public Set<ActionType> getAvailableActions() {
        if (state == null || result != null) {
            return Set.of();
        }
        Set<ActionType> available = EnumSet.noneOf(ActionType.class);
        for (ActionType type : List.of(ActionType.FOLD, ActionType.CHECK, ActionType.CALL)) {
            try {
                engine.apply(state, new ActionRequest(state.getCurrentActorId().orElseThrow(), type));
                available.add(type);
            } catch (IllegalArgumentException | UnsupportedOperationException unavailable) {
                // Legality belongs to the engine; the discarded result is never committed.
            }
        }
        return Set.copyOf(available);
    }

    /** Comparison only; no pots paid or completed-hand statistics aggregated. */
    public Optional<ShowdownResult> getResult() { return Optional.ofNullable(result); }

    /** Immutable current-hand log, cleared only by a successful restart; excludes blinds. */
    public List<AcceptedAction> getAcceptedActions() { return List.copyOf(acceptedActions); }

    /**
     * Starts a new match with cards hidden. A rejected restart preserves the previous match.
     * @throws IllegalArgumentException unless humanCount is between one and six
     */
    public void start(int humanCount) {
        hideCards();
        GameState started = initializer.start(humanCount);
        state = started;
        result = null;
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
        requireMatch();
        Player viewer = state.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId) && player.getType() == PlayerType.HUMAN)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Select a human in this match."));
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
        return state == null ? Optional.empty() : Optional.of(new SetupView(state, viewerSeat, cardsRevealed));
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
        if (state.getCurrentActorSeat().isPresent()) {
            int actor = state.getCurrentActorSeat().orElseThrow();
            if (state.getPlayers().get(actor).getType() == PlayerType.HUMAN && viewerSeat != actor) {
                hideCards();
                viewerSeat = actor;
            }
        }
    }

    private void requireMatch() {
        if (state == null) {
            throw new IllegalStateException("Start a match first.");
        }
    }
}
