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
