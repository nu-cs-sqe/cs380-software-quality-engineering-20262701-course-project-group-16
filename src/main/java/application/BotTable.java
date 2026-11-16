package application;

import bot.EquityPot;
import bot.EquityRequest;
import bot.learning.DecisionContext;
import bot.learning.DecisionOrigin;
import bot.learning.OpponentBeliefs;
import bot.learning.OpponentModel;
import bot.learning.PublicDecision;
import bot.policy.BotPersonality;
import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import domain.model.ActionType;
import domain.model.BettingEngine;
import domain.model.GameState;
import domain.model.GameView;
import domain.model.LegalActions;
import domain.model.Player;
import domain.model.PlayerType;
import domain.model.PotLayout;
import domain.model.ResolvedActionType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Owner-confined match models; successors prepare learning before session publication. */
final class BotTable {
    private final String matchId;
    private final long sequence;
    private final Set<String> players;
    private final Map<String, OpponentModel> models;

    private BotTable(String matchId, long sequence, Set<String> players,
            Map<String, OpponentModel> models) {
        this.matchId = matchId;
        this.sequence = sequence;
        this.players = Set.copyOf(players);
        this.models = Map.copyOf(models);
    }

    static BotTable start(GameState state) {
        String match = UUID.randomUUID().toString();
        Set<String> players = state.getPlayers().stream().map(Player::getId)
                .collect(Collectors.toSet());
        return new BotTable(match, 0, players,
                createOpponentModelsForBots(match, state.getPlayers(), players));
    }

    private static Map<String, OpponentModel> createOpponentModelsForBots(String match,
            List<Player> roster,
            Set<String> players) {
        Map<String, OpponentModel> models = new LinkedHashMap<>();
        for (Player player : roster) {
            if (player.getType() == PlayerType.BOT) {
                Set<String> opponents = players.stream().filter(id -> !id.equals(player.getId()))
                        .collect(Collectors.toSet());
                models.put(player.getId(), new OpponentModel(match, player.getId(), opponents));
            }
        }
        return models;
    }

    BotTable after(AcceptedAction action, LegalActions before, boolean position) {
        long next = Math.incrementExact(sequence);
        Set<ResolvedActionType> choices = before.getActions().stream()
                .map(type -> ResolvedActionType.valueOf(type.name())).collect(Collectors.toSet());
        DecisionContext context = new DecisionContext(action.getStreet(), choices, position);
        return after(new PublicDecision(matchId, next, action, context,
                DecisionOrigin.PLAYER_CHOICE));
    }

    BotTable afterDeparture(AcceptedAction action) {
        return after(new PublicDecision(matchId, Math.incrementExact(sequence), action,
                DecisionOrigin.DEPARTURE));
    }

    private BotTable after(PublicDecision event) {
        Map<String, OpponentModel> updated = new LinkedHashMap<>();
        models.forEach((id, model) -> updated.put(id, model.after(event)));
        return new BotTable(matchId, event.getSequence(), players, updated);
    }

    Map<String, OpponentBeliefs> snapshot(String bot) {
        if (!models.containsKey(bot)) {
            throw new IllegalArgumentException("Choose a bot in the current match.");
        }
        Map<String, OpponentBeliefs> result = new LinkedHashMap<>();
        players.stream().filter(id -> !id.equals(bot))
                .forEach(id -> result.put(id, models.get(bot).snapshot(id)));
        return Map.copyOf(result);
    }

    BotTurn capture(GameState state, LegalActions legal, long revision, int trials) {
        String actor = state.getCurrentActorId().orElseThrow();
        GameView view = state.viewFor(actor);
        GameState called = afterCall(state, actor, legal.getCallCost());
        PotLayout layout = PotLayout.from(called);
        long newRefund = Math.min(legal.getCallCost(), layout.getRefunds().get(actor));
        EquityRequest scenario = EquityRequest.fromView(view, equityPots(layout), trials);
        int seat = state.getCurrentActorSeat().orElseThrow();
        BotPersonality personality = BotPersonality.values()[seat % BotPersonality.values().length];
        return new BotTurn(matchId, revision, view, legal, scenario, newRefund,
                snapshot(actor), personality);
    }

    private static GameState afterCall(GameState state, String actor, long cost) {
        return cost == 0 ? state : new BettingEngine()
                .apply(state, new ActionRequest(actor, ActionType.CALL)).getState();
    }

    private static List<EquityPot> equityPots(PotLayout layout) {
        List<EquityPot> pots = new ArrayList<>();
        for (int index = 0; index < layout.getLayers().size(); index++) {
            PotLayout.Layer layer = layout.getLayers().get(index);
            pots.add(new EquityPot("pot-" + index, layer.getAmount(), layer.getEligiblePlayers()));
        }
        return List.copyOf(pots);
    }

    boolean matches(String match) { return matchId.equals(match); }
}
