# BVA index

Every listed case identifies methods, input/system state, expected output and implementation
status. Tables map case IDs to JUnit methods (parameterized methods may cover
several IDs). Enums are excluded under the course rule; view models also have
privacy and immutability cases.

## Reference contract update — November 15

Required application-owned references and collection elements are non-null caller
contracts. Current BVA focuses on game/domain boundaries; obsolete programmer-null
cases and their current table rows have been retired without renumbering the
remaining case IDs. Historical RED/GREEN and test counts remain dated checkpoints.
Intentional actor/aggressor absence uses Optional.empty(), hidden cards use empty
lists, and unavailable statistics use OptionalDouble. Those are still tested.
Missing or malformed external localization resources remain validated and tested.
Constructor/composition null checks and their programmer-null cases are also
retired. Java Object.equals compatibility still includes its required absent-object
case. No new null-input tests were introduced. See the [shared API contract](../design/README.md#instructor-follow-up-november-15).

| Class | Analysis |
| --- | --- |
| `Card` | [Card.md](Card.md) |
| `Deck` | [Deck.md](Deck.md) |
| `HandEvaluator` (interface contract) | [HandEvaluator.md](HandEvaluator.md) |
| `PokerHandEvaluator` | [PokerHandEvaluator.md](PokerHandEvaluator.md) |
| `ShowdownResolver` | [ShowdownResolver.md](ShowdownResolver.md) |
| `ShowdownResult` | [ShowdownResult.md](ShowdownResult.md) |
| `Localizations` | [Localizations.md](Localizations.md) |
| `ProfilePanel` / profile-enabled `GamePanel` | [ProfileScreen.md](ProfileScreen.md) |
| `GamePanel` localization behavior | [GamePanelLocalization.md](GamePanelLocalization.md) |
| `Player` | [Player.md](Player.md) |
| `PlayerProfile` | [PlayerProfile.md](PlayerProfile.md) |
| `ProfileRepository` | [ProfileRepository.md](ProfileRepository.md) |
| `CompletedStatistics` | [CompletedStatisticsSnapshot.md](CompletedStatisticsSnapshot.md) |
| `AuthenticatedProfile` | [AuthenticatedProfile.md](AuthenticatedProfile.md) |
| `ProfileMatch` | [ProfileMatch.md](ProfileMatch.md) |
| `StatisticsProperties` | [StatisticsProperties.md](StatisticsProperties.md) |
| `StoredProfile` | [StoredProfile.md](StoredProfile.md) |
| `PasswordHash` | [PasswordHash.md](PasswordHash.md) |
| `PasswordHasher` | [PasswordHasher.md](PasswordHasher.md) |
| `PasswordDeriver` | [PasswordDeriver.md](PasswordDeriver.md) |
| `Pbkdf2PasswordHasher` | [Pbkdf2PasswordHasher.md](Pbkdf2PasswordHasher.md) |
| `ProfileService` | [ProfileService.md](ProfileService.md) |
| `FileProfileRepository` | [FileProfileRepository.md](FileProfileRepository.md) |
| `GameConfig` | [GameConfig.md](GameConfig.md) |
| `ActionRequest` | [ActionRequest.md](ActionRequest.md) |
| `AcceptedAction` | [AcceptedAction.md](AcceptedAction.md) |
| `PlayerState` | [PlayerState.md](PlayerState.md) |
| `BettingProgress` | [BettingProgress.md](BettingProgress.md) |
| `BettingHistory` | [BettingHistory.md](BettingHistory.md) |
| `BettingEngine` (actions and street advancement) | [BettingEngine.md](BettingEngine.md), [full bets/raises](FullBetting.md), [short all-ins/reopening](ShortAllInBetting.md) |
| `LegalActions` | [LegalActions.md](LegalActions.md) |
| `BettingResult` | [BettingResult.md](BettingResult.md) |
| `HandPositions` | [HandPositions.md](HandPositions.md) |
| `SeatOrder` | [SeatOrder.md](SeatOrder.md) |
| `GameState` | [GameState.md](GameState.md) |
| `GameSetup` | [GameSetup.md](GameSetup.md) |
| `GameInitializer` | [GameInitializer.md](GameInitializer.md) |
| `SetupSession` | [SetupSession.md](SetupSession.md), [played-hand lifecycle](SessionBetting.md), [departure](Departure.md) |
| `GamePanel` played flow | [PlayedGamePanel.md](PlayedGamePanel.md) |
| `GameView` | [GameView.md](GameView.md) |
| `PlayerView` | [PlayerView.md](PlayerView.md) |
| `HandStatistics` / session delivery | [HandStatistics.md](HandStatistics.md) |
| `PlayStats` | [PlayStats.md](PlayStats.md), [transactional copy](CompletedStatistics.md) |
| `PotSettlement` / `Settlement` | [PotSettlement.md](PotSettlement.md) |
| `MatchStatistics` | [MatchStatistics.md](MatchStatistics.md), [lifetime integration](LifetimeStatistics.md) |
| `StatisticsView` / `NextHand` | [CompletedStatistics.md](CompletedStatistics.md) |
| `GamePanel` / `StatisticsPanel` completion UI | [StatisticsPanel.md](StatisticsPanel.md) |
| `StatsAction` | [StatsAction.md](StatsAction.md) |
| `StatsContext` | [StatsContext.md](StatsContext.md) |
| `EquityPot` | [EquityPot.md](EquityPot.md) |
| `EquityRequest` | [EquityRequest.md](EquityRequest.md) |
| `DealSampler` | [DealSampler.md](DealSampler.md) |
| `UniformDealSampler` | [UniformDealSampler.md](UniformDealSampler.md) |
| `EquityEstimator` | [EquityEstimator.md](EquityEstimator.md) |
| `MonteCarloEquityEstimator` | [MonteCarloEquityEstimator.md](MonteCarloEquityEstimator.md) |
| `EquityEstimate` | [EquityEstimate.md](EquityEstimate.md) |
| `DecisionContext` | [DecisionContext.md](DecisionContext.md) |
| `TendencyEstimate` | [TendencyEstimate.md](TendencyEstimate.md) |
| `PublicDecision` | [PublicDecision.md](PublicDecision.md) |
| `ActionPrediction` | [ActionPrediction.md](ActionPrediction.md) |
| `OpponentBeliefs` | [OpponentBeliefs.md](OpponentBeliefs.md) |
| `OpponentModel` | [OpponentModel.md](OpponentModel.md) |
| `PotLayout` (including Layer) | [PotLayout.md](PotLayout.md) |
| `BotTurn` | [BotTurn.md](BotTurn.md) |
| `BotTable` | [BotTable.md](BotTable.md) |
| `BotComputer` | [BotComputer.md](BotComputer.md) |
| `BotWorker` / `BotScheduler` port | [BotWorker.md](BotWorker.md) |
| `BotController` / `BotSession` port | [BotController.md](BotController.md) |

[Combined setup integration cases](setup-integration.md) cover the card/deck-to-state boundary.
[Showdown integration](../design/showdown.md) covers the initialized-card-to-result boundary.

---

# Overall Rule
This folder should contain the BVA analysis. There should be one .md for each non-view and non-enum class, and there should be BVA analysis for each public method.

# What to Include in each BVA Analysis File (like `MyVector.md`)

You are encouraged to document your intermediate analysis results for Steps 1-3.
However, you are only required to document Step 4.

For each test case, you may choose **any easy-to-read format** you like. Regardless of the format, you are required to specify the following for each test case:
1. A unique ID
2. The method(s) under test
3. The state of the system under test
4. The expected output
5. Whether it has been implemented

Here are 2 formats as references. You can use any of them or a revised version of them.

## Format 1:

### Method under test: `abc()`

|              | State of the System | Expected output | Implemented?              |
|--------------|---------------------|-----------------|---------------------------|
| Test Case 1  |                     |                 | :x: or :white_check_mark: |
| Test Case 2  |                     |                 | :x: or :white_check_mark: |


### Method under test: `efg()`

|              | State of the System | Expected output | Implemented?              |
|--------------|---------------------|-----------------|---------------------------|
| Test Case 3  |                     |                 | :x: or :white_check_mark: |
| Test Case 4  |                     |                 | implemented in Test Case 1 |

## Format 2:

### Method under test: `abc()`
- **TC1: NAME OF THE TEST CASE** ( :x: or :white_check_mark: )
    - **State of the system**: abc
    - **Expected output**: abc

- **TC2: NAME OF THE TEST CASE** ( :x: or :white_check_mark: )
    - **State of the system**: abc
    - **Expected output**: abc

### Method under test: `efg()`
- **TC3: NAME OF THE TEST CASE** ( :x: or :white_check_mark: )
    - **State of the system**: abc
    - **Expected output**: abc

- **TC4: NAME OF THE TEST CASE** ( implemented in TC1 )
    - **State of the system**: abc
    - **Expected output**: abc

## E05 decision policy

- [CallValue](CallValue.md)
- [PolicyRequest](PolicyRequest.md)
- [ActionDistribution](ActionDistribution.md)
- [BotPolicy](BotPolicy.md)

BotPersonality is an enum; its configurations are tested through BotPolicy.
The aggression query extension is covered in [OpponentBeliefs](OpponentBeliefs.md).
