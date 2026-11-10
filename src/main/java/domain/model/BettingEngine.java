package domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies bets/raises, including short all-ins, and passive actions; advances closed rounds.
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
            throw new IllegalArgumentException(
                    "The deck must contain the burn card and all cards to deal.");
        }

        List<PlayerState> players = resetRound(before.getPlayerStates());
        Set<String> pending = new HashSet<>();
        players.stream().filter(PlayerState::canAct)
                .forEach(player -> pending.add(player.getPlayer().getId()));
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
                new BettingProgress(0, before.getConfig().getBigBlind(), pending, pending),
                before.getBettingHistory());
    }

    private static GamePhase nextStreet(GamePhase phase) {
        switch (phase) {
            case PREFLOP: return GamePhase.FLOP;
            case FLOP: return GamePhase.TURN;
            case TURN: return GamePhase.RIVER;
            case RIVER: return GamePhase.SHOWDOWN;
            default:
                throw new UnsupportedOperationException("Only a betting street has a next street.");
        }
    }

    private static void requireClosedRound(GameState before) {
        if (!before.getBettingProgress().getPlayersToAct().isEmpty()) {
            throw new IllegalArgumentException("Pending decisions must finish before advancing.");
        }
        if (before.getPlayerStates().stream().filter(PlayerState::isParticipating).count() < 2) {
            throw new IllegalArgumentException(
                    "An uncontested or empty hand cannot advance to another street.");
        }
        if (before.getPlayerStates().stream().filter(PlayerState::canAct)
                .anyMatch(player -> player.getRoundContribution() < before.getCurrentBet())) {
            throw new IllegalArgumentException("An active player still owes chips in this round.");
        }
    }

    private static List<PlayerState> resetRound(List<PlayerState> before) {
        List<PlayerState> players = new ArrayList<>();
        for (PlayerState player : before) {
            players.add(new PlayerState(player.getPlayer(), player.getStatus(),
                    player.getHoleCards(),
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
     * Wagers use round targets; an undersized bet/raise must commit the entire stack.
     * @throws IllegalArgumentException for invalid inputs/targets, out-of-turn actions,
     *         a check while chips are owed or a zero-cost call
     * @throws UnsupportedOperationException for non-betting phases
     */
    public BettingResult apply(GameState before, ActionRequest request) {
        requireCurrentActor(before, request);
        requireBettingStreet(before);
        int seat = before.getCurrentActorSeat().orElseThrow();
        PlayerState actor = before.getPlayerStates().get(seat);
        AcceptedAction accepted = resolveAction(before, actor, request);

        List<PlayerState> players = new ArrayList<>(before.getPlayerStates());
        players.set(seat, afterAction(actor, accepted));
        BettingProgress progress = BettingRound.after(before, players, accepted);
        GameState after = new GameState(before.getConfig(), before.getHandId(), before.getPhase(),
                players, before.getPositions(),
                nextPendingActor(players, progress.getPlayersToAct(), seat),
                before.getCommunityCards(), before.getRemainingDeck(), progress,
                before.getBettingHistory().after(accepted));
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

    private static void requireBettingStreet(GameState before) {
        switch (before.getPhase()) {
            case PREFLOP:
            case FLOP:
            case TURN:
            case RIVER:
                return;
            default:
                throw new UnsupportedOperationException("Player actions require a betting street.");
        }
    }

    private static AcceptedAction resolveAction(GameState state, PlayerState actor,
            ActionRequest request) {
        long chipsAdded = BettingAmounts.chipsAdded(state, actor, request);
        ResolvedActionType type = ResolvedActionType.valueOf(request.getType().name());
        return new AcceptedAction(state.getHandId(), actor.getPlayer().getId(),
                BettingStreet.valueOf(state.getPhase().name()), type, chipsAdded,
                actor.getRoundContribution() + chipsAdded,
                chipsAdded > 0 && chipsAdded == actor.getPlayer().getChipStack(),
                contextFor(state, actor, type));
    }

    private static StatsContext contextFor(GameState state, PlayerState actor,
            ResolvedActionType type) {
        if (state.getPhase() == GamePhase.PREFLOP) {
            PreflopRaiseType level = type == ResolvedActionType.RAISE
                    ? nextRaiseType(state.getBettingHistory().getPreflopRaiseCount())
                    : PreflopRaiseType.NONE;
            boolean opportunity = type != ResolvedActionType.CHECK
                    && state.getBettingHistory().getPreflopRaiseCount() == 1
                    && BettingAmounts.canWager(state, actor);
            return new StatsContext(level, opportunity, false, false);
        }
        boolean opening = false;
        boolean facing = false;
        if (state.getPhase() == GamePhase.FLOP) {
            String playerId = actor.getPlayer().getId();
            BettingHistory history = state.getBettingHistory();
            boolean isAggressor = history.getFinalPreflopAggressorId()
                    .filter(playerId::equals).isPresent();
            opening = isAggressor && state.getCurrentBet() == 0
                    && BettingAmounts.canWager(state, actor);
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
                action.getRoundContributionAfter(),
                before.getHandContribution() + action.getChipsAdded());
    }

    private static PreflopRaiseType nextRaiseType(long previousRaises) {
        if (previousRaises == 0) {
            return PreflopRaiseType.OPENING_RAISE;
        }
        if (previousRaises == 1) {
            return PreflopRaiseType.THREE_BET;
        }
        return previousRaises == 2 ? PreflopRaiseType.FOUR_BET : PreflopRaiseType.LATER_RAISE;
    }

    private static String nextPendingActor(List<PlayerState> players, Set<String> pending,
            int seat) {
        for (int distance = 1; distance <= players.size(); distance++) {
            PlayerState candidate = players.get((seat + distance) % players.size());
            if (candidate.canAct() && pending.contains(candidate.getPlayer().getId())) {
                return candidate.getPlayer().getId();
            }
        }
        return null;
    }
}
