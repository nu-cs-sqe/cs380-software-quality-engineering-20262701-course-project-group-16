package domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies unraised preflop actions and postflop checks/folds, and advances closed rounds.
 * The application records statistics after receiving an accepted result.
 */
public final class BettingEngine {
    /**
     * Advances PREFLOP/FLOP/TURN/RIVER by one street without accepting a player action.
     * Burns one card before each community-card street. New rounds reset round commitments,
     * preserve hand commitments and create decisions only if at least two players can act.
     * SHOWDOWN retains the river accounting and has no decisions, dealing or payouts.
     * @throws IllegalArgumentException for null input, pending or unpaid active decisions,
     *         fewer than two participants or too few cards for the burn and deal
     * @throws UnsupportedOperationException for SETUP, SHOWDOWN or COMPLETION
     */
    public GameState advanceStreet(GameState before) {
        if (before == null) {
            throw new IllegalArgumentException("A state is required for street advancement.");
        }
        GamePhase next = nextStreet(before.getPhase());
        requireClosedRound(before);
        if (next == GamePhase.SHOWDOWN) {
            return toShowdown(before);
        }
        int consumed = next == GamePhase.FLOP ? 4 : 2;
        if (before.getRemainingDeck().size() < consumed) {
            throw new IllegalArgumentException("The deck must contain the burn card and all cards to deal.");
        }

        List<PlayerState> players = resetRound(before.getPlayerStates());
        Set<String> pending = new HashSet<>();
        players.stream().filter(PlayerState::canAct).forEach(player -> pending.add(player.getPlayer().getId()));
        if (pending.size() < 2) {
            pending.clear();
        }
        String actor = pending.isEmpty() ? null : players.get(new SeatOrder(players)
                .firstPostflopActor(before.getDealerSeat()).orElseThrow()).getPlayer().getId();
        List<Card> board = new ArrayList<>(before.getCommunityCards());
        board.addAll(before.getRemainingDeck().subList(1, consumed));
        return new GameState(before.getConfig(), before.getHandId(), next, players,
                before.getPositions(), actor, board,
                before.getRemainingDeck().subList(consumed, before.getRemainingDeck().size()),
                new BettingProgress(0, before.getConfig().getBigBlind(), pending, pending), before.getBettingHistory());
    }

    private static GamePhase nextStreet(GamePhase phase) {
        switch (phase) {
            case PREFLOP: return GamePhase.FLOP;
            case FLOP: return GamePhase.TURN;
            case TURN: return GamePhase.RIVER;
            case RIVER: return GamePhase.SHOWDOWN;
            default: throw new UnsupportedOperationException("Only a betting street has a next street.");
        }
    }

    private static void requireClosedRound(GameState before) {
        if (!before.getBettingProgress().getPlayersToAct().isEmpty()) {
            throw new IllegalArgumentException("Pending decisions must finish before advancing.");
        }
        if (before.getPlayerStates().stream().filter(PlayerState::isParticipating).count() < 2) {
            throw new IllegalArgumentException("An uncontested or empty hand cannot advance to another street.");
        }
        if (before.getPlayerStates().stream().filter(PlayerState::canAct)
                .anyMatch(player -> player.getRoundContribution() < before.getCurrentBet())) {
            throw new IllegalArgumentException("An active player still owes chips in this round.");
        }
    }

    private static List<PlayerState> resetRound(List<PlayerState> before) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState player : before) {
            players.add(new PlayerState(player.getPlayer(), player.getStatus(), player.getHoleCards(),
                    0, player.getHandContribution()));
        }
        return players;
    }

    private static GameState toShowdown(GameState before) {
        return new GameState(before.getConfig(), before.getHandId(), GamePhase.SHOWDOWN,
                before.getPlayerStates(), before.getPositions(), null, before.getCommunityCards(),
                before.getRemainingDeck(), new BettingProgress(before.getCurrentBet(),
                before.getLastFullRaiseSize(), Set.of(), Set.of()), before.getBettingHistory());
    }

    /**
     * Returns an immutable next state and accepted action without modifying the input.
     * With no pending decisions or only one participant, the next actor is absent.
     * Street advancement and pot settlement are separate operations, not performed here.
     * A call adds at most the available stack; exhausting it preserves CALL semantics.
     * @throws IllegalArgumentException for null inputs, out-of-turn actions,
     *         a check while chips are owed or a zero-cost call
     * @throws UnsupportedOperationException for non-betting phases, raised preflop rounds,
     *         bets, raises or postflop calls
     */
    public BettingResult apply(GameState before, ActionRequest request) {
        requireCurrentActor(before, request);
        requireSupportedDecision(before, request.getType());
        int seat = before.getCurrentActorSeat().orElseThrow();
        PlayerState actor = before.getPlayerStates().get(seat);
        AcceptedAction accepted = resolveAction(before, actor, request.getType());

        List<PlayerState> players = new ArrayList<>(before.getPlayerStates());
        players.set(seat, afterAction(actor, accepted));
        BettingProgress progress = afterDecision(before, players, request.getPlayerId());
        GameState after = new GameState(before.getConfig(), before.getHandId(), before.getPhase(),
                players, before.getPositions(), nextPendingActor(players, progress.getPlayersToAct(), seat),
                before.getCommunityCards(), before.getRemainingDeck(), progress, before.getBettingHistory());
        return new BettingResult(after, accepted);
    }

    private static void requireCurrentActor(GameState before, ActionRequest request) {
        if (before == null || request == null) {
            throw new IllegalArgumentException("A state and action request are required.");
        }
        if (!request.getPlayerId().equals(before.getCurrentActorId().orElse(null))) {
            throw new IllegalArgumentException("Only the current actor may act.");
        }
    }

    private static void requireSupportedDecision(GameState before, ActionType type) {
        switch (before.getPhase()) {
            case PREFLOP:
                if (before.getCurrentBet() != before.getConfig().getBigBlind()) {
                    throw new UnsupportedOperationException("Raised preflop rounds are not implemented yet.");
                }
                return;
            case FLOP:
            case TURN:
            case RIVER:
                if (type == ActionType.CHECK || type == ActionType.FOLD) {
                    return;
                }
                throw new UnsupportedOperationException("Postflop calling, betting and raising are not implemented yet.");
            default:
                throw new UnsupportedOperationException("Player actions require a betting street.");
        }
    }

    private static AcceptedAction resolveAction(GameState state, PlayerState actor, ActionType type) {
        long owed = state.getCurrentBet() - actor.getRoundContribution();
        switch (type) {
            case FOLD:
                return accepted(state, actor, ResolvedActionType.FOLD, 0);
            case CHECK:
                if (owed != 0) {
                    throw new IllegalArgumentException("A player must call or fold when chips are owed.");
                }
                return accepted(state, actor, ResolvedActionType.CHECK, 0);
            case CALL:
                if (owed == 0) {
                    throw new IllegalArgumentException("Nothing is owed; use a check instead of a call.");
                }
                return accepted(state, actor, ResolvedActionType.CALL,
                        Math.min(owed, actor.getPlayer().getChipStack()));
            default:
                throw new UnsupportedOperationException("Bets and raises are not implemented yet.");
        }
    }

    private static AcceptedAction accepted(GameState state, PlayerState actor,
            ResolvedActionType type, long chipsAdded) {
        return new AcceptedAction(state.getHandId(), actor.getPlayer().getId(),
                BettingStreet.valueOf(state.getPhase().name()), type, chipsAdded,
                actor.getRoundContribution() + chipsAdded,
                chipsAdded > 0 && chipsAdded == actor.getPlayer().getChipStack(),
                contextFor(state, actor));
    }

    private static StatsContext contextFor(GameState state, PlayerState actor) {
        boolean opening = false;
        boolean facing = false;
        if (state.getPhase() == GamePhase.FLOP) {
            String playerId = actor.getPlayer().getId();
            BettingHistory history = state.getBettingHistory();
            boolean isAggressor = history.getFinalPreflopAggressorId().filter(playerId::equals).isPresent();
            opening = isAggressor && state.getCurrentBet() == 0
                    && state.getBettingProgress().getPlayersAllowedToRaise().contains(playerId)
                    && state.getPlayerStates().stream().filter(PlayerState::canAct).count() >= 2;
            long cBet = history.getFlopCBetAmount().orElse(0);
            facing = cBet > 0 && !isAggressor && state.getCurrentBet() == cBet
                    && actor.getRoundContribution() < state.getCurrentBet();
        }
        return new StatsContext(PreflopRaiseType.NONE, false, opening, facing);
    }

    private static PlayerState afterAction(PlayerState before, AcceptedAction action) {
        Player player = before.getPlayer();
        Player after = new Player(player.getId(), player.getSeat(), player.getType(),
                player.getChipStack() - action.getChipsAdded());
        PlayerStatus status = before.getStatus();
        if (action.getType() == ResolvedActionType.FOLD) {
            status = PlayerStatus.FOLDED;
        } else if (action.becameAllIn()) {
            status = PlayerStatus.ALL_IN;
        }
        return new PlayerState(after, status, before.getHoleCards(),
                action.getRoundContributionAfter(), before.getHandContribution() + action.getChipsAdded());
    }

    private static BettingProgress afterDecision(GameState before, List<PlayerState> players, String actorId) {
        Set<String> pending = new HashSet<>(before.getBettingProgress().getPlayersToAct());
        Set<String> raising = new HashSet<>(before.getBettingProgress().getPlayersAllowedToRaise());
        pending.remove(actorId);
        raising.remove(actorId);
        if (players.stream().filter(PlayerState::isParticipating).count() <= 1 || pending.isEmpty()) {
            pending.clear();
            raising.clear();
        }
        return new BettingProgress(before.getCurrentBet(), before.getLastFullRaiseSize(), pending, raising);
    }

    private static String nextPendingActor(List<PlayerState> players, Set<String> pending, int seat) {
        for (int distance = 1; distance <= players.size(); distance++) {
            PlayerState candidate = players.get((seat + distance) % players.size());
            if (candidate.canAct() && pending.contains(candidate.getPlayer().getId())) {
                return candidate.getPlayer().getId();
            }
        }
        return null;
    }
}
