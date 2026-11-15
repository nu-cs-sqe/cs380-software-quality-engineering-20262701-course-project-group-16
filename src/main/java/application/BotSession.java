package application;

import domain.model.AcceptedAction;
import domain.model.ActionRequest;
import java.util.List;
import java.util.Optional;

/** Owner-thread application port used by the live bot-turn coordinator. */
public interface BotSession {
    Optional<BotTurn> getBotTurn(int trials);
    boolean isCurrentBotTurn(BotTurn turn);
    boolean applyBotDecision(BotTurn turn, ActionRequest action);
    void invalidateBotTurns();
    List<AcceptedAction> playDepartureTurns();
}
