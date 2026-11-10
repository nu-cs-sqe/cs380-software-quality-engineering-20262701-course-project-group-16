# GamePanel localization and privacy cases

GamePanel is a Swing view under the course view exemption. These component tests
exercise real SetupSession and GameInitializer instances on the event thread.
The existing setup/privacy tests remain; only the former English-only selector
expectation was replaced because the current requirement supports two languages.

| ID | Event / state | Expected behavior | Implemented / test |
| --- | --- | --- | --- |
| UI-L01 | Chinese JVM default; new panel; choose Chinese then English | Explicit English initial choice; both native language names offered before starting; switching back returns English without JVM fallback | Yes: defaultLanguageIsEnglishWithAnExplicitChoiceRegardlessOfSystemLocale |
| UI-L02a–b | Choose English/Chinese before starting a two-human match | Selected language used for start/title, seat identities, chips, pot, deck, first actor and private handoff; only selected human cards can be revealed; public seats stay concealed | Yes: selectedLanguageAppliesToSetupAndPrivateHandoff |
| UI-L03a–b | Either language; reveal; submit 0, 7, nonnumeric, empty and overflowing count; then restart with 1 | Localized errors; rejected start preserves match and hides hand; restart conceals cards, clears error and keeps language | Yes: selectedLanguageSurvivesRestartAndInvalidInputWithCardsHidden |
| UI-L04a–b | Either language; reveal second human; switch language; reject count; switch back | Every language change hides hand, preserves match/viewer and retranslates existing validation error | Yes: languageChangeHidesCardsPreservesMatchAndRetranslatesErrors |

The UI class has 17 cases including the original setup/privacy cases. Together
with LocalizationsTest, the first GREEN run passed 50 cases. The native frame
uses the panel's localized `windowTitle` client property and listens for changes;
headless tests verify that property in both languages. Native focus delivery and
cross-platform font/layout rendering still need a manual desktop check.

Coverage hardening now runs 52 focused cases (35 catalog and 17 UI); all pass.

Complete placeholder validation now runs 69 focused cases (52 catalog and 17 UI).

## ShowdownPanel result-display checkpoint

This separate Swing view consumes the existing `GameView` and `ShowdownResult`;
it does not execute betting actions or alter the session. UI classes use the
course view exemption, but the following behavior cases document this boundary.
All fixtures use the real resolver, state, cards and filtered view on the Swing
event thread. The supplied view includes the viewer's private cards; expected
result text contains public player names only.

| ID | Public method / state | Expected behavior | Test / status |
| --- | --- | --- | --- |
| UI-R01 | constructor; no result | Hidden panel and empty result messages | startsHiddenWithoutAStaleWinner; passing at RED |
| UI-R02a–b | showResult; one winner; English/Chinese | Localized Human 1 winner and awaiting-settlement status; unchanged pot/chips | rendersRealResultWithLocalizedPublicNames, SINGLE; GREEN |
| UI-R03a–b | showResult; two tied players; English/Chinese | Both public names in seat order with tie message | rendersRealResultWithLocalizedPublicNames, TWO_WAY; GREEN |
| UI-R04a–b | showResult; six-way board tie; English/Chinese | All six names, including bots, without hole cards or payouts | rendersRealResultWithLocalizedPublicNames, SIX_WAY; GREEN |
| UI-R05a–b | showResult; sole participant before complete board; English/Chinese | Uncontested Bot 6 message; awaiting settlement | rendersRealResultWithLocalizedPublicNames, UNCONTESTED; GREEN |
| UI-R06 | showResult again with another language | Replace all translated messages, preserving the supplied result | rerenderingInAnotherLanguageReplacesEveryResultMessage; GREEN |
| UI-R07 | clearResult, then show next hand | Clear old messages/hide, then display the new winner only | clearingAndShowingAnotherHandRemovesThePreviousWinner; GREEN |
| UI-R08 | showResult; different hand IDs | IllegalArgumentException; keep previous valid display | rejectsResultFromAnotherHandWithoutReplacingTheDisplay; GREEN |
| UI-R09 | showResult; winner absent from supplied view | IllegalArgumentException; do not show an unrelated player/raw ID | rejectsWinnerIdsMissingFromTheSuppliedPublicView; GREEN |
| UI-R10a–b | showResult; null view/result | NullPointerException; keep previous valid display | rejectsNullViewOrResult; GREEN |
| UI-R11a–c | showResult; null/empty/unconfigured locale | IllegalArgumentException; keep previous valid display | rejectsUnsupportedLanguageWithoutReplacingTheDisplay; GREEN |

Observed RED `2493bf3`: 18 invocations, 17 expected assertion failures, one
initial-state pass, zero errors/skips. GREEN: all 18 cases pass; full project
build passes 753 tests with zero failures/errors/skips. Checkstyle and SpotBugs
main/test checks have no findings or analysis errors. The test-helper cast
finding was fixed in source using a checked local Throwable; no suppression was
added. English/Chinese six-way ties and a Chinese uncontested result were
rendered with Java 11 and visually inspected at 680×200.

The panel retains no GameView/ShowdownResult and never reads hole cards. The
existing 17 GamePanel cases still verify private-card handoff/hiding behavior.
The application must clear the result between hands and render it again using
the selected language. Attaching it to Estella's session result is a later
integration checkpoint; these tests do not claim a played result screen.

The November 10 profile flow adds 23 headless component cases mapped in
[ProfileScreen.md](ProfileScreen.md), including registration/sign-in/lifetime
feedback in both languages, password/card concealment, storage retries and
same-name account selection. New messages remain resource-backed and catalog-
validated; no locale switch is implemented as a language-specific Java branch.
