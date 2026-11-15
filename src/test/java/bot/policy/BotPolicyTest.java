package bot.policy;

import bot.learning.*;
import domain.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class BotPolicyTest {
    private final BotPolicy policy = new BotPolicy();

    @Test
    void bp01NeverPaysWithZeroEquityOrFoldsAFreeCheck() {
        assertEquals(Map.of(ActionType.FOLD, 1.0), probabilities(PolicyFixtures.table(), 0));
        assertEquals(Map.of(ActionType.FOLD, 1.0), probabilities(PolicyFixtures.table(), 0.01));
        PolicyFixtures.Fixture free = PolicyFixtures.table().street(GamePhase.FLOP);
        assertEquals(Map.of(ActionType.CHECK, 1.0), probabilities(free, 0));
        free.raiseRights = false;
        assertEquals(Map.of(ActionType.CHECK, 1.0), probabilities(free, 1));
    }

    @Test
    void bp02NeverFoldsCertainEquityAndSupportsShortAllInCalls() {
        PolicyFixtures.Fixture shortStack = PolicyFixtures.table();
        shortStack.stacks[3] = 15;
        ActionDistribution distribution = decide(shortStack, 1);
        assertEquals(Map.of(ActionType.CALL, 1.0), distribution.getProbabilities());
        BettingResult accepted = new BettingEngine().apply(shortStack.state(),
                distribution.sample(0));
        assertEquals(15, accepted.getAcceptedAction().getChipsAdded());
        assertTrue(accepted.getAcceptedAction().becameAllIn());
        assertEquals(ResolvedActionType.CALL, accepted.getAcceptedAction().getType());
        assertFalse(decide(PolicyFixtures.table(), 1).getProbabilities()
                .containsKey(ActionType.FOLD));
    }

    @Test
    void bp03VariesFoldAndCallNearBreakEvenAndRespectsClosedRaiseRights() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        fixture.raiseRights = false;
        ActionDistribution distribution = decide(fixture, 0.4);
        assertEquals(Set.of(ActionType.FOLD, ActionType.CALL),
                distribution.getProbabilities().keySet());
        assertTrue(distribution.getProbabilities().values().stream().allMatch(p -> p > 0));
        assertEquals(1, distribution.getProbabilities().values().stream()
                .mapToDouble(Double::doubleValue).sum(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(BotPersonality.class)
    void bp04EveryPersonalityProducesLegalActionsAndRoundTargets(BotPersonality personality) {
        for (GamePhase street : List.of(GamePhase.PREFLOP, GamePhase.FLOP,
                GamePhase.TURN, GamePhase.RIVER)) {
            PolicyFixtures.Fixture fixture = PolicyFixtures.table();
            if (street != GamePhase.PREFLOP) fixture.street(street);
            for (double equity : new double[] {0, 0.1, 0.4, 0.7, 1}) {
                PolicyRequest request = fixture.request(equity);
                ActionDistribution distribution = policy.distribution(request, personality);
                for (int index = 0; index < 101; index++) {
                    ActionRequest action = distribution.sample(index / 101.0);
                    assertEquals("p3", action.getPlayerId());
                    assertTrue(request.getLegalActions().getActions().contains(action.getType()));
                    action.getTargetAmount().ifPresent(target ->
                            assertTrue(request.getLegalActions().getWager()
                                    .orElseThrow().allows(target)));
                }
            }
        }
    }

    @Test
    void bp05SizesRaiseToTargetsAndClampsFullAndShortAllIns() {
        assertEquals(58, last(decide(PolicyFixtures.table(), 0.9))
                .getTargetAmount().orElseThrow());
        PolicyFixtures.Fixture smallBlind = PolicyFixtures.table();
        smallBlind.hero = 1;
        smallBlind.actor = 1;
        ActionRequest raise = last(decide(smallBlind, 0.9));
        assertEquals(50, raise.getTargetAmount().orElseThrow());
        assertEquals(40, new BettingEngine().apply(smallBlind.state(), raise)
                .getAcceptedAction().getChipsAdded());
        for (long stack : new long[] {25, 35, 40, 50}) {
            PolicyFixtures.Fixture fixture = PolicyFixtures.table();
            fixture.stacks[3] = stack;
            ActionRequest allIn = last(decide(fixture, 1));
            assertEquals(stack, allIn.getTargetAmount().orElseThrow());
            assertTrue(new BettingEngine().apply(fixture.state(), allIn)
                    .getAcceptedAction().becameAllIn());
        }
    }

    @Test
    void bp06ClampsToMinimumAndUsesOnlyEligiblePotForOdds() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        GameView view = fixture.state().viewFor("p3");
        PolicyRequest narrow = new PolicyRequest(view, fixture.legal(),
                new CallValue(1, 20, 0), Map.of());
        assertEquals(40, last(policy.distribution(narrow, BotPersonality.BALANCED))
                .getTargetAmount().orElseThrow());
        PolicyRequest poorCall = new PolicyRequest(view, fixture.legal(),
                new CallValue(0.5, 20, 0), Map.of());
        assertEquals(Map.of(ActionType.FOLD, 1.0),
                policy.distribution(poorCall, BotPersonality.BALANCED).getProbabilities());
        PolicyRequest freeCall = new PolicyRequest(view, fixture.legal(),
                new CallValue(0, 0, 20), Map.of());
        assertEquals(Map.of(ActionType.CALL, 1.0),
                policy.distribution(freeCall, BotPersonality.BALANCED).getProbabilities());
    }

    @Test
    void bp07PersonalitiesVarySizingAndAggressionModestly() {
        PolicyRequest request = PolicyFixtures.table().request(0.9);
        ActionDistribution cautious = policy.distribution(request, BotPersonality.CAUTIOUS);
        ActionDistribution balanced = policy.distribution(request, BotPersonality.BALANCED);
        ActionDistribution aggressive = policy.distribution(request, BotPersonality.AGGRESSIVE);
        assertEquals(45, last(cautious).getTargetAmount().orElseThrow());
        assertEquals(58, last(balanced).getTargetAmount().orElseThrow());
        assertEquals(70, last(aggressive).getTargetAmount().orElseThrow());
        assertTrue(cautious.getProbabilities().get(ActionType.RAISE)
                < balanced.getProbabilities().get(ActionType.RAISE));
        assertTrue(balanced.getProbabilities().get(ActionType.RAISE)
                < aggressive.getProbabilities().get(ActionType.RAISE));
    }

    @Test
    void bp08PositionAndStackPressureChangeChoicesWithoutHiddenInformation() {
        PolicyFixtures.Fixture out = PolicyFixtures.table().street(GamePhase.FLOP);
        PolicyFixtures.Fixture position = PolicyFixtures.table().street(GamePhase.FLOP);
        position.dealer = 3;
        double before = decide(out, 0.6).getProbabilities().get(ActionType.BET);
        assertTrue(decide(position, 0.6).getProbabilities().get(ActionType.BET) > before);
        PolicyFixtures.Fixture deep = PolicyFixtures.table();
        PolicyFixtures.Fixture shortStack = PolicyFixtures.table();
        deep.raiseRights = false;
        shortStack.raiseRights = false;
        shortStack.stacks[3] = 20;
        assertTrue(decide(shortStack, 0.4).getProbabilities().get(ActionType.FOLD)
                > decide(deep, 0.4).getProbabilities().get(ActionType.FOLD));
    }

    @Test
    void bp09LearnedAggressionIsConfidenceWeightedAndStreetSpecific() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        fixture.raiseRights = false;
        double prior = decide(fixture, 0.4).getProbabilities().get(ActionType.FOLD);
        OpponentBeliefs aggressive = learned(ResolvedActionType.RAISE);
        OpponentBeliefs passive = learned(ResolvedActionType.CALL);
        assertTrue(probability(fixture.request(0.4, Map.of("p1", aggressive)), ActionType.FOLD)
                > prior);
        assertTrue(probability(fixture.request(0.4, Map.of("p1", passive)), ActionType.FOLD)
                < prior);
        assertEquals(prior, probability(fixture.request(0.4,
                Map.of("p1", new OpponentBeliefs())), ActionType.FOLD));
        fixture.street(GamePhase.FLOP);
        assertEquals(probabilities(fixture, 0.4), policy.distribution(
                fixture.request(0.4, Map.of("p1", aggressive)), BotPersonality.BALANCED)
                .getProbabilities());
    }

    @Test
    void bp10SeededSamplesAreRepeatableAndMatchPublishedProbabilities() {
        ActionDistribution distribution = decide(PolicyFixtures.table(), 0.4);
        Random first = new Random(380);
        Random second = new Random(380);
        Map<ActionType, Integer> counts = new EnumMap<>(ActionType.class);
        int samples = 10000;
        for (int index = 0; index < samples; index++) {
            ActionType selected = distribution.sample(first.nextDouble()).getType();
            assertEquals(selected, distribution.sample(second.nextDouble()).getType());
            counts.merge(selected, 1, Integer::sum);
        }
        distribution.getProbabilities().forEach((type, expected) ->
                assertEquals(expected, counts.get(type) / (double) samples, 0.02));
        assertTrue(counts.size() > 1);
    }

    @Test
    void bp11PaidPositionImprovesCallingNearBreakEven() {
        PolicyFixtures.Fixture out = raisedFlop();
        PolicyFixtures.Fixture position = raisedFlop();
        position.dealer = 3;
        out.raiseRights = false;
        position.raiseRights = false;
        double equity = 20.0 / 260;
        assertTrue(decide(position, equity).getProbabilities().get(ActionType.CALL)
                > decide(out, equity).getProbabilities().get(ActionType.CALL));
    }

    @Test
    void bp12CountsAllInContendersButUsesHeadsUpFairShare() {
        PolicyFixtures.Fixture multiway = PolicyFixtures.table().street(GamePhase.FLOP);
        PolicyFixtures.Fixture headsUp = PolicyFixtures.table().street(GamePhase.FLOP);
        for (int seat : new int[] {1, 2, 4, 5}) headsUp.eliminated(seat);
        assertTrue(decide(multiway, 0.4).getProbabilities().containsKey(ActionType.BET));
        assertEquals(Map.of(ActionType.CHECK, 1.0), probabilities(headsUp, 0.4));
    }

    @Test
    void bp13CommittingTheWholeStackReducesAggressiveWeight() {
        PolicyFixtures.Fixture deep = PolicyFixtures.table();
        PolicyFixtures.Fixture shortStack = PolicyFixtures.table();
        shortStack.stacks[3] = 40;
        assertTrue(decide(shortStack, 1).getProbabilities().get(ActionType.RAISE)
                < decide(deep, 1).getProbabilities().get(ActionType.RAISE));
    }

    @Test
    void bp14EquivalentChipExposureIgnoresAccountingOffsets() {
        PolicyFixtures.Fixture original = raisedFlop();
        PolicyFixtures.Fixture shifted = raisedFlop();
        for (int seat = 0; seat < 6; seat++) shifted.round[seat] += 10;
        ActionDistribution before = decide(original, 0.9);
        ActionDistribution after = decide(shifted, 0.9);
        assertEquals(before.getProbabilities(), after.getProbabilities());
        assertEquals(last(before).getTargetAmount().orElseThrow() + 10,
                last(after).getTargetAmount().orElseThrow());
    }

    private static PolicyFixtures.Fixture raisedFlop() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table().street(GamePhase.FLOP);
        fixture.round[1] = 20;
        return fixture;
    }

    @Test
    void bp15PersonalityChangesPaidCallPreferencesNearBreakEven() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        fixture.raiseRights = false;
        PolicyRequest request = fixture.request(0.4);
        double cautious = policy.distribution(request, BotPersonality.CAUTIOUS)
                .getProbabilities().get(ActionType.CALL);
        double balanced = policy.distribution(request, BotPersonality.BALANCED)
                .getProbabilities().get(ActionType.CALL);
        double aggressive = policy.distribution(request, BotPersonality.AGGRESSIVE)
                .getProbabilities().get(ActionType.CALL);
        assertTrue(cautious < balanced);
        assertTrue(balanced < aggressive);
    }

    @Test
    void bp16CertainEquityNeverFoldsEvenAtTheAcceptedCostBoundary() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table();
        PolicyRequest request = new PolicyRequest(fixture.state().viewFor("p3"), fixture.legal(),
                new CallValue(1, 20, 0), Map.of());
        for (BotPersonality personality : BotPersonality.values()) {
            assertFalse(policy.distribution(request, personality)
                    .getProbabilities().containsKey(ActionType.FOLD));
        }
    }

    @Test
    void bp17FairShareIsNotValueAndCautiousBotsAvoidThinValue() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table().street(GamePhase.FLOP);
        assertEquals(Map.of(ActionType.CHECK, 1.0), probabilities(fixture, 1.0 / 6));
        PolicyRequest thinValue = fixture.request(0.25);
        assertEquals(Map.of(ActionType.CHECK, 1.0),
                policy.distribution(thinValue, BotPersonality.CAUTIOUS).getProbabilities());
        assertTrue(policy.distribution(thinValue, BotPersonality.AGGRESSIVE)
                .getProbabilities().containsKey(ActionType.BET));
    }

    @Test
    void bp18LearnedAggressionReducesThinBettingAndBalancedPlayKeepsCallMajority() {
        PolicyFixtures.Fixture fixture = PolicyFixtures.table().street(GamePhase.FLOP);
        double prior = decide(fixture, 0.6).getProbabilities().get(ActionType.BET);
        OpponentBeliefs aggressive = learned(ResolvedActionType.BET, BettingStreet.FLOP);
        double learned = policy.distribution(fixture.request(0.6, Map.of("p1", aggressive)),
                BotPersonality.BALANCED).getProbabilities().get(ActionType.BET);
        assertTrue(learned < prior);
        double raises = decide(PolicyFixtures.table(), 0.9).getProbabilities()
                .get(ActionType.RAISE);
        assertTrue(raises > 0.2 && raises < 0.5);
    }

    private Map<ActionType, Double> probabilities(PolicyFixtures.Fixture fixture, double equity) {
        return decide(fixture, equity).getProbabilities();
    }

    private ActionDistribution decide(PolicyFixtures.Fixture fixture, double equity) {
        return policy.distribution(fixture.request(equity), BotPersonality.BALANCED);
    }

    private double probability(PolicyRequest request, ActionType type) {
        return policy.distribution(request, BotPersonality.BALANCED)
                .getProbabilities().get(type);
    }

    private static ActionRequest last(ActionDistribution distribution) {
        return distribution.sample(Math.nextDown(1.0));
    }

    private static OpponentBeliefs learned(ResolvedActionType type) {
        return learned(type, BettingStreet.PREFLOP);
    }

    private static OpponentBeliefs learned(ResolvedActionType type, BettingStreet street) {
        OpponentModel model = new OpponentModel("learn", "p3", Set.of("p1"));
        StatsContext stats = new StatsContext(type == ResolvedActionType.RAISE
                ? PreflopRaiseType.OPENING_RAISE : PreflopRaiseType.NONE, false, false, false);
        Set<ResolvedActionType> choices = street == BettingStreet.PREFLOP
                ? Set.of(ResolvedActionType.FOLD, ResolvedActionType.CALL, ResolvedActionType.RAISE)
                : Set.of(ResolvedActionType.BET, ResolvedActionType.CHECK);
        DecisionContext context = new DecisionContext(street, choices, false);
        for (int index = 1; index <= 100; index++) {
            AcceptedAction action = new AcceptedAction("h" + index, "p1",
                    street, type, 40, 40, false, stats);
            model.observe(new PublicDecision("learn", index, action, context,
                    DecisionOrigin.PLAYER_CHOICE));
        }
        return model.snapshot("p1");
    }
}
