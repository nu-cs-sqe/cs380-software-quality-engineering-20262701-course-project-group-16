# SetupSession BVA

The application session owns the internal GameState. SetupView is an immutable
read model containing public player records and only the selected human's cards
after explicit reveal. It never retains the internal GameState. Cards are hidden
before starting/restarting, selecting a viewer, or reporting invalid input.

| ID | Method(s) | State/input | Expected output | Status / test |
| --- | --- | --- | --- | --- |
| SS-C01 | SetupSession(GameInitializer) | Null initializer | IllegalArgumentException | RED: rejectsNullInitializer |
| SS-C02 | SetupSession(); getView; hideCards | No match | Empty view; hiding is safe | noMatchHasNoDisplayAndHidingIsSafe |
| SS-N01 | revealCards | No match | IllegalStateException | RED: cannotRevealBeforeStarting |
| SS-N02 | selectViewer | No match | IllegalStateException | RED: cannotSelectBeforeStarting |
| SS-V01/02 | start; getView | 1 or 6 humans | Six seats, expected human/bot split, first human selected, cards hidden; pot30, remainder40, total6000 | RED: startsAtHumanCountBoundariesWithHiddenCards |
| SS-I01/02 | start; getView | 0 or 7 humans before match | IllegalArgumentException; no view | RED: invalidInitialStartDoesNotCreateAMatch |
| SS-I03/04/05/06 | start; getView | MIN_VALUE, 0, 7 or MAX_VALUE after reveal | Reject; preserve previous match and conceal cards | RED: failedRestartKeepsMatchButHidesCards |
| SS-P01 | selectViewer; revealCards; hideCards; getView | Two-human match, reveal seat1 then hide | Correct seat1 cards only; correct public positions/actor; immutable snapshots; hidden view after hide | RED: revealShowsOnlySelectedHumanCardsAndHideClearsThem |
| SS-P02/03 | selectViewer; getView | Same human or another human after reveal | Cards hidden until a new reveal | RED: everyViewerSelectionRequiresANewReveal |
| SS-P04/05/06/07 | selectViewer; getView | Null, blank, missing or bot ID | IllegalArgumentException; selected human unchanged, cards hidden | RED: rejectsInvalidViewerAndHidesPreviousCards |
| SS-S01 | start; getView | Start a new match after revealing another human's cards | New IDs, first human selected, cards hidden, earlier view unchanged | RED: newMatchResetsViewerAndHidesOldCards |

There are 20 cases. The forwarding placeholders are the RED scaffold; the state
and privacy behaviors will be implemented after observing the failures. SetupView
is a view model (course view exemption); its data and privacy are checked through
SetupSession tests. UI callbacks will use this session on the Swing event thread.
