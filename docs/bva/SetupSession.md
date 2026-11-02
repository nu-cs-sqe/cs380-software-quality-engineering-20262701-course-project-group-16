# SetupSession BVA

The application session owns the internal GameState. SetupView is an immutable
read model containing public player records and only the selected human's cards
after explicit reveal. It never retains the internal GameState. Cards are hidden
before starting/restarting, selecting a viewer, or reporting invalid input.

| ID | Method(s) | State/input | Expected output | Status / test |
| --- | --- | --- | --- | --- |
| SS-C01 | SetupSession(GameInitializer) | Null initializer | IllegalArgumentException | Passing: rejectsNullInitializer |
| SS-C02 | SetupSession(); getView; hideCards | No match | Empty view; hiding is safe | Passing: noMatchHasNoDisplayAndHidingIsSafe |
| SS-N01 | revealCards | No match | IllegalStateException | Passing: cannotRevealBeforeStarting |
| SS-N02 | selectViewer | No match | IllegalStateException | Passing: cannotSelectBeforeStarting |
| SS-V01 | start; getView | 1 human | Six seats, expected human/bot split, first human selected, cards hidden; pot30, remainder40, total6000 | Passing: startsAtHumanCountBoundariesWithHiddenCards |
| SS-V02 | start; getView | 6 humans | Six seats, expected human/bot split, first human selected, cards hidden; pot30, remainder40, total6000 | Passing: startsAtHumanCountBoundariesWithHiddenCards |
| SS-I01 | start; getView | 0 humans before match | IllegalArgumentException; no view | Passing: invalidInitialStartDoesNotCreateAMatch |
| SS-I02 | start; getView | 7 humans before match | IllegalArgumentException; no view | Passing: invalidInitialStartDoesNotCreateAMatch |
| SS-I03 | start; getView | MIN_VALUE after reveal | Reject; preserve previous match and conceal cards | Passing: failedRestartKeepsMatchButHidesCards |
| SS-I04 | start; getView | 0 after reveal | Reject; preserve previous match and conceal cards | Passing: failedRestartKeepsMatchButHidesCards |
| SS-I05 | start; getView | 7 after reveal | Reject; preserve previous match and conceal cards | Passing: failedRestartKeepsMatchButHidesCards |
| SS-I06 | start; getView | MAX_VALUE after reveal | Reject; preserve previous match and conceal cards | Passing: failedRestartKeepsMatchButHidesCards |
| SS-P01 | selectViewer; revealCards; hideCards; getView | Two-human match, reveal seat1 then hide | Correct seat1 cards only; correct public positions/actor; immutable snapshots; hidden view after hide | Passing: revealShowsOnlySelectedHumanCardsAndHideClearsThem |
| SS-P02 | selectViewer; getView | Same human after reveal | Cards hidden until a new reveal | Passing: everyViewerSelectionRequiresANewReveal |
| SS-P03 | selectViewer; getView | Another human after reveal | Cards hidden until a new reveal | Passing: everyViewerSelectionRequiresANewReveal |
| SS-P04 | selectViewer; getView | Null viewer ID | IllegalArgumentException; selected human unchanged, cards hidden | Passing: rejectsInvalidViewerAndHidesPreviousCards |
| SS-P05 | selectViewer; getView | Blank viewer ID | IllegalArgumentException; selected human unchanged, cards hidden | Passing: rejectsInvalidViewerAndHidesPreviousCards |
| SS-P06 | selectViewer; getView | Missing viewer ID | IllegalArgumentException; selected human unchanged, cards hidden | Passing: rejectsInvalidViewerAndHidesPreviousCards |
| SS-P07 | selectViewer; getView | Bot viewer ID | IllegalArgumentException; selected human unchanged, cards hidden | Passing: rejectsInvalidViewerAndHidesPreviousCards |
| SS-S01 | start; getView | Start a new match after revealing another human's cards | New IDs, first human selected, cards hidden, earlier view unchanged | Passing: newMatchResetsViewerAndHidesOldCards |

There are 20 passing cases. RED commit `bc5accd` ran 20 cases with 19 expected
assertion failures for missing initialization, validation and privacy behavior.
The empty-session/hide case already passed. The GREEN session passes all 20;
the session checkpoint passed 590 tests. The subsequent Swing checkpoint passes
602 tests with zero failures, errors or skips.

SetupView is a view model (course view exemption); its data and privacy are
checked through these session tests. UI callbacks use the session on the Swing
event thread. Real setup/deck/state components run with a fixed dealer stub and
seeded shuffle source; no game components are mocked.
