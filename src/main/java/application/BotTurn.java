package application;

import bot.EquityRequest;
import bot.learning.OpponentBeliefs;
import bot.policy.BotPersonality;
import domain.model.GameView;
import domain.model.LegalActions;
import java.util.Map;

/** Immutable worker input. Only the session constructs paired, validated snapshots. */
public final class BotTurn {
    private final String matchId;
    private final long revision;
    private final GameView view;
    private final LegalActions legal;
    private final EquityRequest scenario;
    private final long callRefund;
    private final Map<String, OpponentBeliefs> beliefs;
    private final BotPersonality personality;

    BotTurn(String matchId, long revision, GameView view, LegalActions legal,
            EquityRequest scenario, long callRefund, Map<String, OpponentBeliefs> beliefs,
            BotPersonality personality) {
        this.matchId = matchId;
        this.revision = revision;
        this.view = view;
        this.legal = legal;
        this.scenario = scenario;
        this.callRefund = callRefund;
        this.beliefs = Map.copyOf(beliefs);
        this.personality = personality;
    }

    public String getMatchId() { return matchId; }
    public long getRevision() { return revision; }
    public GameView getView() { return view; }
    public LegalActions getLegalActions() { return legal; }
    public EquityRequest getScenario() { return scenario; }
    public long getCallRefund() { return callRefund; }
    public Map<String, OpponentBeliefs> getBeliefs() { return beliefs; }
    public BotPersonality getPersonality() { return personality; }
}
