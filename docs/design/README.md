# System design

The [foundation contract](shared-contract.md) defines settings, stable seats, immutable
hand state, action messages and filtered player views. Game rules, bot decisions and
the interface remain separate components.

```mermaid
classDiagram
    GameSetup --> GameConfig
    GameSetup --> SeatOrder
    GameSetup --> GameState
    SeatOrder --> HandPositions
    GameState --> GameConfig
    GameState --> PlayerState
    PlayerState --> Player
    PlayerState --> Card
    GameState --> BettingProgress
    GameState --> BettingHistory
    GameState --> GameView : viewFor
    GameView --> PlayerView
    AcceptedAction --> StatsContext
    AcceptedAction --> StatsAction : toStatsAction
    PlayStats --> StatsAction
    BettingEngine --> ActionRequest
    BettingEngine --> GameState
    BettingEngine --> BettingResult
    BettingResult --> GameState
    BettingResult --> AcceptedAction
    PokerHandEvaluator ..|> HandEvaluator
    ShowdownResolver --> HandEvaluator
    ShowdownResolver --> ShowdownResult
```

Member A supplies the shared `domain.model.Card`, rank/suit enums, deck creation,
shuffling and dealing primitives. The existing HandEvaluator interface now has one PokerHandEvaluator implementation,
used by ShowdownResolver to return immutable results awaiting settlement. See
[showdown design](showdown.md). The betting engine accepts `ActionRequest`,
validates it against current state, then produces a new snapshot and
`AcceptedAction`. Bots and the interface receive
`GameView`; profiles and saved statistics stay separate from live match state.

Implemented here: initialization, immutable contracts, unraised preflop
FOLD/CHECK/CALL (including all-in calls), and postflop CHECK/FOLD.
`BettingEngine.apply` returns a `BettingResult`
with a new state and accepted action; it does not mutate earlier snapshots or
update statistics itself. Pending active players act clockwise, retaining the
big blind's option. Folded, all-in and eliminated seats do not act; all-in seats
still count as participants. When no decisions remain, the actor is absent and
the pot/phase/cards are retained for a separate advancement or settlement step.
Calls add at most the caller's available stack. Stack exhaustion sets ALL_IN
and `becameAllIn=true`, while the accepted action stays CALL. Short calls retain
the current bet, last full raise size and other players' pending decisions.
`BettingEngine.advanceStreet` explicitly advances a closed round through
FLOP/TURN/RIVER/SHOWDOWN. It rejects pending or unpaid ACTIVE decisions and hands
with fewer than two participants. It burns one card before each community-card
street, deals in order and resets round accounting while preserving hand
commitments. New decisions require at least two ACTIVE players; an all-in runout
can advance without player actions. SHOWDOWN retains the completed river's
accounting and leaves all pots unawarded.
Postflop accepted actions report FLOP/TURN/RIVER rather than PREFLOP. The
immutable `BettingHistory` records the final preflop aggressor and an actual
flop continuation bet's original target. The engine uses that provenance,
amount owed, current betting level and legal betting rights to emit opportunity
context; `PlayStats` does not reconstruct the betting tree. The original
GameState constructor creates empty history, while the explicit-history
constructor validates it and engine operations preserve it across snapshots.
Two integration demonstrations connect real setup/dealing, preflop actions,
postflop checks/folds and advancement through SHOWDOWN without paying pots.
Betting/raising, raised-round action handling, postflop CALL, pot
settlement, bot strategy and profile storage follow later. See the
[engine BVA](../bva/BettingEngine.md).
Best-five evaluation and closed-state winner/tie resolution are also implemented
without payouts. SetupSession now connects the played betting flow to ShowdownResolver and Swing;
see [played-hand application flow](played-hand.md).
The Swing setup preview uses SetupSession and
SetupView to display initialization and private-card handoffs; see [setup design](game-setup.md).
CI builds and tests pushes and pull requests.

[Localization](localization.md) covers English/Chinese discovery, selection and
private-card handoffs. [Quality checks](../quality.md) document Checkstyle,
SpotBugs, scoped coverage and mutation results.

## Method design

Each new or changed method should do one named task. Keep orchestration readable
by extracting independent validation, dealing, state transitions and rendering
steps into private helpers. Keep constructors short and collaborators injectable
where coordination needs isolated tests. Prefer explicit absence to nullable
internal values, while retaining validation at public boundaries. Splitting a
method should improve its responsibilities, not merely move lines elsewhere.
Apply these method-design rules to future changes.


## Standalone equity (E03)

`EquityRequest.fromView` copies only the viewer's two cards, the public board and
ordered ACTIVE/ALL_IN participant IDs. Folded/eliminated seats are excluded.
`EquityPot` describes an awardable public scenario: amount, identity and eligible
players. The engine/application must supply this eligibility and remove uncalled
refunds. E03 does not reconstruct settlement layers or action legality. Scenarios
may describe a prospective legal call rather than the currently displayed pot.

`UniformDealSampler` builds a fresh unseen 52-card universe from rank/suit values,
removes known cards, then samples remaining board cards and two cards per live
opponent without replacement. All live opponents are sampled, including those
ineligible for a particular pot. This preserves card blocking across pots.
The sampled board is shared by all seven-card holdings. Hidden folded cards and
unobserved burns are marginalized under this uniform model; actual hidden cards
and physical deck order are never accessed. Uniform hands are a baseline design
choice, not learned ranges or a GTO claim.

`MonteCarloEquityEstimator` reuses `PokerHandEvaluator`. Each trial compares the
hero with each opponent once; each pot then uses only its eligible contenders.
A win contributes 1, a loss 0 and a tie with k eligible opponents 1/(k+1).
The returned map contains mean shares, including 0 for hero-ineligible pots.
Expected payout is the amount-weighted sum of these shares. Equity divides this
expectation by the total hero-eligible awardable amount. Neither value includes
refunds, future betting, integer odd-chip allocation or net profit; action EV
requires calling costs and later strategy. Values stay numeric and unrounded.

The ports `HandEvaluator`, `DealSampler` and `EquityEstimator` permit isolated
unit tests. Integration tests separately cover real comparisons, a six-way royal
flush, exact enumeration of all 990 heads-up river hands, and hidden-data
invariance. Fixed seeds reproduce results; they do not establish a general error
bound. Error and observational latency are recorded in standard JUnit output.

Requests are immutable, have checked chip totals and bound trials to 1–10,000.
That initial cap limits work, not wall-clock latency. Cancellation or interruption
is checked between trials and throws without returning a partial result; retries
start fresh totals. RNG-backed estimator instances belong to one worker. E06 must
run them off Swing's event thread, attach match/hand/state identity and discard
stale results before submitting actions. E04 adds smoothed opponent beliefs; E05 chooses varied legal actions using
the shared engine contract.
None of those later integrations is completed by this calculator.


## Opponent learning (E04)

Each bot owns a match-scoped `OpponentModel`, registered with its own participant
ID and up to five opponent IDs. On a serialized owner thread, the application
passes **committed** `AcceptedAction` events wrapped in `PublicDecision`, with
match identity and a positive sequence increasing across all hands. Events may
skip numbers; they must arrive in engine order. No ActionRequest or rejected
StatsAction is accepted by this API. Learning happens only after application
publication succeeds, so a failed engine/statistics transaction cannot train it.

A voluntary decision includes `DecisionContext`: public street, actual available
choices and whether the player acts in position. The engine/application supplies
that pre-action snapshot; the model does not reconstruct turn or betting rules.
Raise eligibility and three-bet classification remain separate. A three-bet flag
requires a legal RAISE; a c-bet opportunity requires a legal BET. Short/all-in
choices keep their resolved CALL/BET/RAISE meaning. Metadata does not include
opponents' hole cards, the physical deck, hidden equity or profile passwords.

Forced posts and departure folds have no strategic preference meaning and may
omit decision context entirely. The explicit origin distinguishes a voluntary
fold from quit-as-fold, including a non-current departure with no legal turn
choices. Already-all-in departure behavior still belongs to Kevin's K04 contract;
this model neither folds an all-in player nor invents a decision. Own actions,
forced posts and automatic departures advance the event sequence but do not
change beliefs. Replayed/stale sequences are ignored. A wrong match or unknown
participant is rejected before publication. A failed updater publishes neither
new beliefs nor the sequence, so the same event can be retried once.

`OpponentBeliefs` contains immutable predictions and tendency estimates, with
mutable registries confined to the model owner. Snapshot readers never mutate
counters. Contexts match by street, legal-choice set and position, keeping a
bounded collection of at most 248 contexts per opponent (4 streets × 2 position
states × 31 nonempty subsets of five choices). Queries for unseen contexts create
a prior result without storing new evidence. There is no per-event history or
unfinished-match persistence; creating a new match creates a new model. Profile
lifetime statistics and exact PlayStats reporting remain independent.

For K available actions, `ActionPrediction` uses a Dirichlet prior with one
pseudo-count per choice. After N decisions, action a has posterior predictive
probability (count[a]+1)/(N+K). It reports raw counts, posterior marginal standard
deviations sqrt(p[a]×(1−p[a])/(N+K+1)) and evidence weight N/(N+K). Illegal choices
have no entry. With only one available choice, prediction is deterministically
one with zero uncertainty, independent of strategic skill.

`TendencyEstimate` tracks decision-level three-bets, flop c-bets and folds to
actual flop c-bets. It consumes explicit engine opportunity flags, including
calls/folds/checks that decline an eligible action. With S successes in N
opportunities, the Beta(1,1) posterior mean is (S+1)/(N+2), standard deviation
sqrt(p×(1−p)/(N+3)), and evidence weight N/(N+2). Four-bets are not three-bets;
ordinary flop bets/folds do not create c-bet opportunities. These decision-level
beliefs are not the completed-hand percentage definitions in PlayStats.

Zero observations mean a prior, **not a measured 50% rate**. Evidence weight is
data weight relative to the prior, not a confidence interval or a claim that the
model is correct. Uncertainty assumes exchangeable observations within each
coarse context. Sparse contexts, selection effects and changing opponents limit
that assumption. Counts are cumulative; new evidence changes beliefs gradually,
without forgetting older play. A future recency model needs its own validation.

The small contextual probability model is our project design. Research on
[Bayesian poker opponent modeling](https://poker.cs.ualberta.ca/publications/UAI05.pdf)
motivates separating uncertain opponent strategy from game dynamics; this is
not a reproduction of that paper's full model or a GTO claim. The prior formulas
follow [Beta-binomial updating](https://stat210a.berkeley.edu/fall-2025/reader/bayes-estimation.html)
and [Dirichlet distributions](https://mc-stan.org/docs/2_31/functions-reference/dirichlet-distribution.html).
An action-frequency model does not identify hidden-card likelihoods. E03's hand
sampling remains uniform; weighted hand-range inference requires a separately
specified and validated likelihood model.

Unit tests isolate context matching, posterior arithmetic and coordinator
failure/retry behavior. Real-component integrations cover rejected versus
accepted engine actions, observer/opponent isolation, match reset and departure
exclusion. Controlled held-out evaluation compares learned and disabled-learning
multiclass Brier error (sum of squared action-probability errors; lower is better).
Four permutations of fixed train/held-out compositions test order invariance;
they are not four independent playing-strength measurements. E05/E06 still need
to use these snapshots in varied legal decisions, wire committed engine events,
run simulations off Swing's event thread and reject stale calculated actions.
