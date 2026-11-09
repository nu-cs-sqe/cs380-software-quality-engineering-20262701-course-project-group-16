# Played-hand application flow

SetupSession extends the existing setup coordinator; it owns the only current
GameState. Swing receives SetupView snapshots, never the remaining deck or other
players' hole cards. The view includes the hand ID, phase, public board, stacks,
statuses and commitments. Its existing int actor accessor returns -1 when no
decision remains. All session calls run on the Swing event thread.

```mermaid
sequenceDiagram
    participant UI as GamePanel
    participant Session as SetupSession
    participant Engine as BettingEngine
    participant Resolver as ShowdownResolver
    participant Stats as HandStatistics
    UI->>Session: submitAction(ActionRequest)
    Session->>Engine: apply(currentState, request)
    Engine-->>Session: next state + accepted action
    loop Closed rounds with at least two participants
        Session->>Engine: advanceStreet(candidate)
    end
    opt Showdown or one remaining participant
        Session->>Resolver: resolve(candidate)
        Resolver-->>Session: result awaiting settlement
    end
    Session->>Stats: record accepted action in pending collector
    Session-->>UI: commit state/result/action once
    UI->>Session: playBotTurns()
    Session-->>UI: stop at human decision or result
    UI->>Session: getView(), getResult(), getAvailableActions()
```

## Session contract

| API | Behavior |
| --- | --- |
| `start(humanCount)` | Initialize a separate hand, hide cards, prepare fresh pending collectors, clear history/result only after a successful start. It returns at the initial decision; the UI then calls playBotTurns. |
| `submitAction(request)` | Delegate legality/deductions to BettingEngine. Advance closed rounds, resolve a terminal comparison, then record the pending observation, publish the candidate state and append exactly one accepted action. Any validation/comparison/recording failure preserves the old state/result/log; private cards are hidden. |
| `playBotTurns()` | Temporary legal CHECK if available, otherwise CALL; stop before a human decision or after a result. Return an immutable list of newly committed bot actions. Does not inspect hole cards. |
| `getAvailableActions()` | Ask the pure engine about FOLD/CHECK/CALL using disposable snapshots; expose only supported legal actions. Queries never update the session/log/statistics. Empty before start or after a result. |
| `getResult()` | Optional immutable ShowdownResult. It remains the comparison snapshot; getSettlement reports subsequent payment. |
| `getAcceptedActions()` | Immutable current-hand event snapshot, including human/bot actions and existing statistics context. No forced blind posts or invented runout actions; failed requests do not append. |

The coordinator keeps its candidate state local until advancement and comparison
succeed. Pending statistics receive the accepted observation before publishing
state/result/log. A failed final comparison can therefore be retried without losing the
actor or counting the action twice. A result rejects further requests. Successful
restart creates a new hand/log/collector; failed restart retains the old result,
history and collector. A package-private statistics factory supports capturing
real deliveries and failure injection in tests. Public constructors wire the
real adapter. Recorders must reject before mutation; PlayStats validates before
appending. No completed-hand totals are changed.

## Transitions and privacy

BettingEngine owns betting/turn/street rules. The session requests advancement
only when decisions are closed. Multiple all-in participants can run out the
board without fake checks. A sole participant is resolved immediately without
dealing extra cards. The actor can still owe a response against an all-in
opponent, so one ACTIVE player alone is not a reason to skip that decision.

Accepted requests and rejected requests hide private cards. A human actor becomes
the selected viewer, with an explicit reveal still required. Starting the UI
executes initial bot turns and selects the next human. Selecting another human
disables action controls until the actor is selected again. Focus loss and locale
changes retain the existing concealment behavior. Selection is shared-computer
handoff, not authentication.

The screen displays public cards, phase, player status, chips, legal actions and
translated winner/tie/uncontested messages. It retains the selected language at
restart and can translate a pending result without replaying actions.

## Controlled demonstration and limits

With six 1,000-chip stacks, dealer 0 and blinds 10/20, use seat 3 CALL, seat 4 FOLD,
seat 5 CALL, seat 0 CALL, seat 1 CALL, seat 2 CHECK. Five participants reach the flop
with pot 100 and stacks 5,900. Check in order1/2/3/5/0 on each street. The final
check produces a real evaluator result awaiting settlement after 21 actions.
Tests use legal deterministic shuffle choices to produce both a shared-board
flush tie and a sole seat 5 six-high straight, without replacing game components.

The engine/session now accept full BET/RAISE and legal FOLD/CHECK/CALL on every
street. For example, a round contribution of 10 and raise target of 100 adds 90
chips. Full wagers reset other ACTIVE players' decisions and raise rights.
Undersized all-in wagers and cumulative reopening remain K02. The screen and
getAvailableActions still offer FOLD/CHECK/CALL only; engine-owned numeric limits
and bet/raise controls remain K05/UI work. Equity bots and saved profiles are later
work. The action log and pending PlayStats receive the same accepted actions. Settlement
then consumes the ledger once to publish completed match totals.

See [session BVA](../bva/SessionBetting.md) and
[bilingual Swing cases](../bva/PlayedGamePanel.md). Headless UI behavior and
offscreen layout checks do not establish native focus delivery or cross-platform
font correctness; those still require the team's desktop walkthrough.

## Paid hands and cumulative statistics

1. `settleHand()` requires an unpaid result. PotSettlement prepares immutable
   awards, refunds and a COMPLETION state from contribution layers. Each pot is
   compared among its eligible contenders, not just the overall hand winner.
2. MatchStatistics copies completed PlayStats collectors and replays only this
   hand's accepted log. Board presence plus preflop folds determines who saw the
   flop; terminal live participants determine contested showdown participation.
   Actual contested payouts and separate refunds supply completion facts.
3. Paid state, settlement and all player totals are published together. Rejected
   or duplicate settlement changes none of them. StatisticsView snapshots contain
   completed numbers only; UI cannot mutate a collector or inspect private cards.
4. `nextHand()` keeps identities/stacks/totals, moves the button, posts capped
   blinds, deals only surviving seats and handles automatic all-in runout.
   With a short BB, multiple active players still owe the nominal big blind;
   the lone opposing active player only matches the actual all-in commitment.
   A matched lone active player gets no extra fold/check decision. A finished
   match rejects another hand. Starting a new match resets every player's totals.

The existing comparison object remains historical and AWAITING_SETTLEMENT by
contract; use getSettlement for actual paid status. GamePanel shows paid/match
winner status and exposes separate settlement/continuation controls. Its tabs
hide private cards. Statistics persist only for this open match; disk profiles
and short all-in wager/reopening behavior remain later checkpoints.
