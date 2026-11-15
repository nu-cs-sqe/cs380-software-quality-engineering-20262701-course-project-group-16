# OpponentModel BVA

The coordinator consumes accepted public events. Belief calculations have their
own [per-class analysis](OpponentBeliefs.md); required references follow the
[non-null caller contract](README.md#reference-contract-update--november-15).

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| OM-C01 | constructor/observe/snapshot | Injected updater; mutate caller registry; two accepted events | Copied registry; updater receives current belief; publish returned snapshot | Yes: omC01CoordinatesOneOpponentUpdateAndCopiesRegistry |
| OM-C02 | observe | Own choice, forced blind, departure fold, fresh/stale/replayed sequence | Skip nonstrategic observations; accept a fresh opponent choice exactly once | Yes: omC02SkipsOwnForcedDepartureAndReplayedDecisions |
| OM-C03 | observe/snapshot | Wrong match, unknown participant and unknown lookup | Reject without consuming sequence or changing beliefs | Yes: omC03RejectsForeignOrMissingInputWithoutConsumingSequence |
| OM-C04 | observe/snapshot | Updater throws, then retry same accepted event | Preserve prior and sequence on failure; publish retry once | Yes: omC04FailedUpdateAndRetryPublishOnlyOnce |
| OM-C05 | constructors | Blank IDs; 0/5/6 registered opponents; observer included in registry | Accept 0–5 distinct opponents; reject invalid identities, observer and sixth opponent | Yes: omC05ValidatesSixSeatRegistryAndCompositionBoundary |
| OM-C06 | observe/snapshot | Event sequence Long.MAX_VALUE, then replay | Accept once without overflow; replay causes no second update | Yes: omC06AcceptsLastSequenceWithoutOverflowAndDoesNotReplayIt |

## Integration and learning evaluation

| ID | Methods | State/input | Expected output | Implemented? |
| --- | --- | --- | --- | --- |
| OL-C01 | public constructor/observe/snapshot | Real initialization and engine reject CHECK, accept CALL | No rejected observation; learn accepted call once; exact completed-hand statistics remain separate; chips conserved | Yes |
| OL-C02 | public constructor/observe/snapshot | Two observers, different opponents, new match | Opponent/observer isolation; immutable prior snapshot; new match starts fresh | Yes |
| OL-C03 | observe/snapshot | Automatic departure facing a c-bet, replay, then voluntary fold | Departure/replay teach nothing; genuine fold counts once | Yes |
| LE-C01 | public constructor/observe/snapshot and belief queries | Four permutations of fixed 100-decision training/held-out compositions | Add-one posterior, reduced held-out multiclass Brier error versus disabled learning, smaller uncertainty | Yes |
| LE-C02 | observe/snapshot | Later aggressive evidence; unseen river context | Raise probability updates gradually; unseen context retains zero-data prior | Yes |

OM-C01–04/06 isolate coordination using an injected updater. OB tests exercise
real aggregation separately. OL tests use real model/value components; OL-C01
also uses real initialization, betting and PlayStats. LE tests are controlled
prediction experiments, not poker match or win-rate evidence.

Local TDD observed 13 context/uncertainty failures, 9 event/prediction failures,
13 belief/coordinator failures, 2 opportunity-consistency failures and 3 automatic-
event failures, each followed by implementation and GREEN. One all-in fixture was
later corrected as documented in OpponentBeliefs.md. Integration/evaluation cases
and mutation-driven assertions were added later; they are not claimed feature RED
cycles. No synthetic Git checkpoints or extra scratch reports were created.

## Instructor follow-up — November 15

Opponent capacity shares GameConfig.SEAT_COUNT, retaining OM-C05's 0/5/6
registration cases. Snapshot queries check registry membership before reading
a required stored belief; no absent-value sentinel travels to another component.

## Transactional successor (E06)

| ID | Method | Input/state | Expected | Implemented |
| --- | --- | --- | --- | --- |
| TM01 | after/snapshot | New event, successor and old snapshot | Successor learns; original and earlier snapshot unchanged | Yes |
| TM02 | after/observe | Copied sequence, duplicate, wrong match then retry | No double count; failure consumes nothing | Yes |

Cases map to TransactionalModelTest. Session prepares all bot successors before
publishing accepted state/statistics, so a rejected game action cannot teach.
