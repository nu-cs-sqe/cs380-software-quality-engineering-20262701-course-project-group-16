package application;

import bot.MonteCarloEquityEstimator;
import bot.UniformDealSampler;
import domain.model.PokerHandEvaluator;
import domain.model.ActionRequest;
import domain.model.ActionType;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BotBackgroundIntegrationTest {
    @Test
    void bg01ComputesOffTheEventThreadAndDispatchesBackToIt()
            throws InterruptedException, InvocationTargetException {
        BotTurn turn = BotFixtures.started(1).getBotTurn(64).orElseThrow();
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean background = new AtomicBoolean();
        AtomicBoolean owner = new AtomicBoolean();
        List<ActionRequest> actions = new ArrayList<>();
        List<BotFailure> errors = new ArrayList<>();
        BotWorker worker = BotWorker.background((input, stop) -> {
            background.set(!SwingUtilities.isEventDispatchThread());
            return new BotComputer().decide(input, stop);
        }, SwingUtilities::invokeLater);
        try {
            SwingUtilities.invokeAndWait(() -> worker.start(turn, action -> {
                owner.set(SwingUtilities.isEventDispatchThread());
                actions.add(action);
                done.countDown();
            }, reason -> { errors.add(reason); done.countDown(); }));
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertTrue(background.get());
            assertTrue(owner.get());
            assertTrue(errors.isEmpty(), errors::toString);
            assertEquals(1, actions.size());
            assertTrue(turn.getLegalActions().getActions().contains(actions.get(0).getType()));
        } finally {
            SwingUtilities.invokeAndWait(worker::close);
        }
    }

    @Test
    void bg02ReplacementKeepsOnlyTheNewestQueuedJob()
            throws InterruptedException, InvocationTargetException {
        BotTurn turn = BotFixtures.started(1).getBotTurn(1).orElseThrow();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger calculations = new AtomicInteger();
        AtomicInteger accepted = new AtomicInteger();
        List<BotFailure> errors = new ArrayList<>();
        BotWorker worker = BotWorker.background((input, stop) -> {
            if (calculations.incrementAndGet() == 1) {
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS), "Release blocked worker");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            return new ActionRequest(input.getView().getViewerId(), ActionType.CALL);
        }, SwingUtilities::invokeLater);
        try {
            Runnable start = () -> worker.start(turn, action -> {
                accepted.incrementAndGet();
                done.countDown();
            }, reason -> { errors.add(reason); done.countDown(); });
            SwingUtilities.invokeAndWait(start);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> { start.run(); start.run(); });
            release.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertTrue(errors.isEmpty(), errors::toString);
            assertEquals(2, calculations.get());
            assertEquals(1, accepted.get());
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(worker::close);
        }
    }

    @Test
    void bg03RealAdaptiveTurnsReachAHumanWithoutBlockingTheOwner()
            throws InterruptedException, InvocationTargetException {
        AtomicReference<CountDownLatch> done = new AtomicReference<>(new CountDownLatch(1));
        List<SetupSession> sessions = new ArrayList<>();
        List<BotController> controllers = new ArrayList<>();
        AtomicBoolean owner = new AtomicBoolean(true);
        long started = System.nanoTime();
        SwingUtilities.invokeAndWait(() -> {
            SetupSession session = BotFixtures.started(1);
            sessions.add(session);
            BotWorker worker = BotWorker.background(seededComputer()::decide,
                    SwingUtilities::invokeLater);
            BotController controller = new BotController(session, worker, () -> {
                owner.compareAndSet(true, SwingUtilities.isEventDispatchThread());
                BotController current = controllers.get(0);
                if (current.getFailure().isPresent()) {
                    done.get().countDown();
                } else if (!current.isThinking() && session.getBotTurn(1).isEmpty()) {
                    done.get().countDown();
                }
            });
            controllers.add(controller);
            controller.advance();
        });
        try {
            awaitHuman(done, controllers.get(0));
            assertTrue(owner.get());
            SwingUtilities.invokeAndWait(() -> {
                SetupSession session = sessions.get(0);
                assertEquals(0, session.getView().orElseThrow().getCurrentActorSeat());
                assertEquals(3, session.getAcceptedActions().size());
                assertTrue(session.getView().orElseThrow().getHoleCards().isEmpty());
                long chips = session.getView().orElseThrow().getPot()
                        + session.getView().orElseThrow().getPlayers().stream()
                        .mapToLong(player -> player.getChipStack()).sum();
                assertEquals(6000, chips);
            });
            System.out.printf(java.util.Locale.ROOT,
                    "E06 live bot-to-human flow: %.3f ms%n",
                    (System.nanoTime() - started) / 1_000_000.0);
        } finally {
            SwingUtilities.invokeAndWait(() -> controllers.get(0).close());
        }
    }
    private static BotComputer seededComputer() {
        Random deals = new Random(380);
        MonteCarloEquityEstimator estimator = new MonteCarloEquityEstimator(
                new PokerHandEvaluator(), new UniformDealSampler(deals::nextInt));
        return new BotComputer(estimator::estimate, new Random(16)::nextDouble);
    }

    // The test drives explicit user retries; production never retries automatically.
    private static void awaitHuman(AtomicReference<CountDownLatch> done, BotController controller)
            throws InterruptedException, InvocationTargetException {
        int maximumRetries = 2;
        for (int retries = 0; retries <= maximumRetries; retries++) {
            assertTrue(done.get().await(5, TimeUnit.SECONDS), "Owner callback did not arrive");
            Optional<BotFailure> failure = readFailure(controller);
            if (failure.isEmpty()) {
                return;
            }
            assertEquals(Optional.of(BotFailure.TIMED_OUT), failure);
            assertTrue(retries < maximumRetries, "All explicitly retried jobs timed out");
            done.set(new CountDownLatch(1));
            SwingUtilities.invokeAndWait(controller::advance);
        }
    }

    private static Optional<BotFailure> readFailure(BotController controller)
            throws InterruptedException, InvocationTargetException {
        AtomicReference<Optional<BotFailure>> failure = new AtomicReference<>(Optional.empty());
        SwingUtilities.invokeAndWait(() -> failure.set(controller.getFailure()));
        return failure.get();
    }

}
