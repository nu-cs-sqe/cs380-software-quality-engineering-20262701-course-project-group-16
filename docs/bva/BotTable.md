# BotTable BVA

Match-scoped owner-thread registry; this internal class creates filtered worker
snapshots and prepares independent learning successors. Tests using the real
engine, estimator or model are integration tests, not isolated unit tests.

| ID | Methods | Input/state | Expected output | Implemented |
| --- | --- | --- | --- | --- |
| SB01 | start/capture/snapshot | One human, five bots; 1/64/10,000 trials; hidden other cards | Own/public cards only, matching legal limits, immutable beliefs and public pot scenario | Yes |
| SB02 | after/snapshot | Illegal action, accepted CALL, duplicate application | Learning only once after commit; prior snapshots unchanged; unknown observer rejected | Yes |
| SB04 | start/matches | Same hand/player fixtures reused for new match | New match identity rejects former job | Yes |
| SB05 | after/capture | Result, payment, next hand | No terminal job; learning retained within match | Yes |
| SB06 | afterDeparture | Current and out-of-turn departure FOLD | Automatic departure has no strategic context and is excluded from learned preferences | Yes |
| SB08 | capture | Short blind 5/10, nominal target 20; gross call 10 | Contested 10-chip pot; new-call refund capped at 10, excluding 5 already committed | Yes |
| BP01/BP02 | capture/after | Real adaptive hand; capped multiway CALL | Pay/continue with retained learning; exclude hero-ineligible side pot; conserve 6,000 chips | Yes |
| PL01–05 | capture via PotLayout | Zero/one chips, six levels, folded money, side pots, uncalled excess | Shared settlement layering and eligibility, no duplicated pot algorithm | Yes |

SB cases map to SessionBotIntegrationTest; PL cases map to PotLayoutTest.
The public SetupSession boundary rejects 0, -1 and 10,001 trials before capture.
The snapshot is an immediate-settlement-after-CALL scenario with no further
betting; future opponents may add chips, so its refund is conditional on that
scenario. It is not a guaranteed refund across later actions.

SB11 checks exact learned pre-action street/position/action contexts throughout a
real played hand; SB12 covers passive progression after immediate departure. SB14 injects a recorder
failure to check that prepared model successors are not published on failure and
that the same captured turn remains retryable. BP01/BP02 exercise live-policy
payment/continuation and capped all-in side-pot scenarios with real components.

Style follow-up: start delegates the model-construction loop to createOpponentModelsForBots,
keeping the entry point at one level of abstraction. Existing SB/BP integration
cases retain the same behavior and expected outputs.

I03 follow-up (November 16): afterDeparture uses PublicDecision's automatic-event
overload. It prepares model successors without constructing another actor's legal
choices or position. DEP-10 checks failed recording, unchanged learning, a
retryable revision and a fresh scenario that excludes the folded quitter.

Week 8 `reseat`: continuing bot identities retain opponent observations and
sequence; departed opponent IDs disappear and new bots/opponents begin with
fresh evidence. AccountTableIntegrationTest.survivingBotsRetainTheirLearningWhenAnotherSeatIsReplaced
and newBotsReplaceBustedBotsAndRetainOnlyActualAccountBindings cover the
real session boundary, including a subsequent accepted action (GREEN).
