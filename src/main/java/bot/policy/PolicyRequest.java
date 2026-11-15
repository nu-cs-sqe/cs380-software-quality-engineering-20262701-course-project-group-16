package bot.policy;

import bot.learning.OpponentBeliefs;
import domain.model.ActionType;
import domain.model.BettingStreet;
import domain.model.GameView;
import domain.model.LegalActions;
import domain.model.PlayerStatus;
import domain.model.PlayerType;
import domain.model.PlayerView;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** One filtered decision and its application-owned public call valuation. */
public final class PolicyRequest {
    private final GameView view;
    private final LegalActions legal;
    private final CallValue call;
    private final Map<String, OpponentBeliefs> opponents;
    private final PlayerView hero;
    private final BettingStreet street;

    public PolicyRequest(GameView view, LegalActions legal, CallValue call,
            Map<String, OpponentBeliefs> opponents) {
        this.view = view;
        this.legal = legal;
        this.call = call;
        street = BettingStreet.valueOf(view.getPhase().name());
        hero = view.getPlayers().stream()
                .filter(player -> player.getId().equals(view.getViewerId()))
                .findFirst().orElseThrow();
        requireActingBot();
        requireLegalOptions();
        requireCallScenario();
        requireOpponents(opponents);
        this.opponents = Map.copyOf(opponents);
    }

    private void requireActingBot() {
        if (hero.getType() != PlayerType.BOT || hero.getStatus() != PlayerStatus.ACTIVE
                || view.getCurrentActorId().filter(hero.getId()::equals).isEmpty()
                || getParticipantCount() < 2) {
            throw new IllegalArgumentException(
                    "A contested betting decision requires its acting bot.");
        }
    }

    private void requireLegalOptions() {
        long owed = Math.min(hero.getChipStack(),
                Math.max(0, view.getCurrentBet() - hero.getRoundContribution()));
        if (legal.getActions().isEmpty() || legal.getCallCost() != owed) {
            throw new IllegalArgumentException(
                    "Supply this snapshot's authoritative legal options.");
        }
        legal.getWager().ifPresent(this::requireWager);
    }

    private void requireWager(LegalActions.Wager wager) {
        ActionType type = view.getCurrentBet() == 0 ? ActionType.BET : ActionType.RAISE;
        if (wager.getMaximumTarget() != hero.getRoundContribution() + hero.getChipStack()
                || wager.getType() != type
                || !view.getPlayersAllowedToRaise().contains(hero.getId())) {
            throw new IllegalArgumentException("Wager limits must belong to the acting bot.");
        }
    }

    private void requireCallScenario() {
        call.riskedChips(legal.getCallCost());
        long available = view.getPot() + legal.getCallCost() - call.getCallRefund();
        if (call.getEligiblePotAfterCall() > available) {
            throw new IllegalArgumentException("Eligible pots cannot exceed the public scenario.");
        }
    }

    private void requireOpponents(Map<String, OpponentBeliefs> beliefs) {
        for (String id : beliefs.keySet()) {
            if (id.equals(hero.getId())
                    || view.getPlayers().stream().noneMatch(player -> player.getId().equals(id))) {
                throw new IllegalArgumentException(
                        "Beliefs must identify other seats in this match.");
            }
        }
    }

    public GameView getView() { return view; }
    public LegalActions getLegalActions() { return legal; }
    public CallValue getCallValue() { return call; }
    public Map<String, OpponentBeliefs> getOpponents() { return opponents; }
    public PlayerView getHero() { return hero; }
    public BettingStreet getStreet() { return street; }

    public int getParticipantCount() {
        return (int) view.getPlayers().stream().filter(player ->
                player.getStatus() == PlayerStatus.ACTIVE
                || player.getStatus() == PlayerStatus.ALL_IN).count();
    }

    public List<PlayerView> getActiveOpponents() {
        return view.getPlayers().stream()
                .filter(player -> player.getStatus() == PlayerStatus.ACTIVE)
                .filter(player -> !player.getId().equals(hero.getId()))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Street-order position among players still able to respond; never uses private cards. */
    public boolean isInPosition() {
        Set<Integer> active = view.getPlayers().stream()
                .filter(player -> player.getStatus() == PlayerStatus.ACTIVE)
                .map(PlayerView::getSeat).collect(Collectors.toSet());
        return view.getPositions().isInPosition(hero.getSeat(), active,
                street == BettingStreet.PREFLOP);
    }
}
