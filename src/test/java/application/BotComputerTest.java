package application;

import bot.BotEstimateValues;
import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotComputerTest {
    @Test
    void bc01UsesOnlyCapturedInputsAndSuppliedRandomness() {
        SetupSession session = BotFixtures.started(1);
        BotTurn turn = session.getBotTurn(64).orElseThrow();
        AtomicInteger estimates = new AtomicInteger();
        BotComputer computer = new BotComputer((request, cancelled) -> {
            assertSame(turn.getScenario(), request);
            assertFalse(cancelled.getAsBoolean());
            estimates.incrementAndGet();
            return BotEstimateValues.fixed(request, 0.9);
        }, () -> Math.nextDown(1.0));
        ActionRequest action = computer.decide(turn, () -> false);
        assertEquals(turn.getView().getViewerId(), action.getPlayerId());
        assertEquals(ActionType.RAISE, action.getType());
        assertTrue(turn.getLegalActions().getWager().orElseThrow()
                .allows(action.getTargetAmount().orElseThrow()));
        assertEquals(1, estimates.get());
        assertEquals(30, session.getView().orElseThrow().getPot());
        assertTrue(session.getAcceptedActions().isEmpty());
    }

    @Test
    void bc02CancellationBeforeAndAfterSimulationPublishesNothing() {
        BotTurn turn = BotFixtures.started(1).getBotTurn(64).orElseThrow();
        AtomicBoolean cancelled = new AtomicBoolean(true);
        AtomicInteger estimates = new AtomicInteger();
        AtomicInteger draws = new AtomicInteger();
        BotComputer computer = new BotComputer((request, stop) -> {
            estimates.incrementAndGet();
            cancelled.set(true);
            return BotEstimateValues.fixed(request, 0.9);
        }, () -> { draws.incrementAndGet(); return 0.5; });
        assertThrows(CancellationException.class, () -> computer.decide(turn, cancelled::get));
        assertEquals(0, estimates.get());
        cancelled.set(false);
        assertThrows(CancellationException.class, () -> computer.decide(turn, cancelled::get));
        assertEquals(1, estimates.get());
        assertEquals(0, draws.get());
    }

    @Test
    void bc03FailuresRemainFailuresWithoutAnInventedFallback() {
        BotTurn turn = BotFixtures.started(1).getBotTurn(1).orElseThrow();
        BotComputer broken = new BotComputer((request, stop) -> {
            throw new IllegalStateException("estimator failed");
        }, () -> 0.5);
        assertThrows(IllegalStateException.class, () -> broken.decide(turn, () -> false));
        BotComputer badDraw = new BotComputer((request, stop) ->
                BotEstimateValues.fixed(request, 0.9), () -> 1);
        assertThrows(IllegalArgumentException.class, () -> badDraw.decide(turn, () -> false));
    }
}
