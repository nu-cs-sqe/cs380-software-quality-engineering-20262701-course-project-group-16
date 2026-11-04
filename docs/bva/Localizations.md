# Localizations BVA

The catalog is a non-view service. All cases use real UTF-8 property streams and
MessageFormat; no localization framework is mocked. A small in-memory resource
loader isolates malformed configuration without altering application resources.

| ID | Public method(s) | Input / state | Expected result | Implemented / test |
| --- | --- | --- | --- | --- |
| LC-01 | load(); languages(); defaultLanguage(); languageName(); locale(); text() | Bundled English and Chinese resources | Explicit English default, ordered native names, correct Chinese locale, dynamic labels and pending-settlement messages in both languages | Yes: bundledLanguagesUseExplicitDefaultAndFormatSupportedMessages |
| LC-02 | languages() | Attempt to add another language to loaded catalog | UnsupportedOperationException; configuration cannot be changed by callers | Yes: bundledLanguagesUseExplicitDefaultAndFormatSupportedMessages |
| LC-03 | load(ClassLoader); languages(); defaultLanguage(); languageName(); text() | Three configured languages, German default and resources | Third locale discovered without Java changes; translated greeting; 1234.5 formatted as 1,234.5 in English and 1.234,5 in German | Yes: thirdLanguageIsDiscoveredAndNumbersUseItsLocaleFromResourcesOnly |
| LC-04 | load(ClassLoader) | Null loader | IllegalArgumentException | Yes: nullResourceLoaderIsRejected |
| LC-05 | load(ClassLoader) | Missing catalog | IllegalArgumentException before UI is shown | Yes: missingCatalogIsRejected |
| LC-06a–k | load(ClassLoader) | Empty catalog; empty list; duplicate tag; trailing empty tag; unsupported default; malformed tag; missing bundle mapping; missing default; empty path; nonexistent bundle; unknown configuration key | IllegalArgumentException for each malformed configuration | Yes: malformedCatalogIsRejectedBeforeDisplayingTheScreen (11 cases) |
| LC-07a–h | load(ClassLoader) | Translation missing key; extra key; blank message; blank native name; malformed pattern; changed argument index; removed number format; date substituted for number | IllegalArgumentException rather than partial or incompatible translations | Yes: missingExtraBlankOrIncompatibleTranslationIsRejected (8 cases) |
| LC-08 | load(ClassLoader) | Invalid MessageFormat pattern in default bundle | IllegalArgumentException | Yes: malformedDefaultMessageIsRejected |
| LC-09a–e | text(); locale(); languageName() | Null, empty, unconfigured, region-alias or whitespace language | IllegalArgumentException; no system/default-language fallback | Yes: unconfiguredLanguageIsRejectedInsteadOfFallingBack (5 cases) |
| LC-10a–c | text() | Null, empty or missing message key | IllegalArgumentException | Yes: unknownMessageKeyIsRejected (3 cases) |
| LC-11 | text() | Missing or surplus arguments; null argument array; text passed to number format | IllegalArgumentException; no unresolved placeholders | Yes: wrongArgumentCountOrTypeIsRejectedInsteadOfLeavingPlaceholders |

There are 33 catalog cases. RED checkpoint `132e255` compiled the complete test
source set and ran 50 focused catalog/UI cases: 40 expected assertion failures
and 10 existing UI cases passing. The catalog was a compile-only stub at RED.
The first GREEN focused run passed all 50 cases.
