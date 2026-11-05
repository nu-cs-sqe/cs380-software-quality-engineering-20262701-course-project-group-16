# Setup localization

The supported setup-to-result flow uses English and Simplified Chinese.
A language selector is available before starting and remains available afterward.
English is the explicit startup default, independent of the JVM locale. Selecting
a language rebuilds the screen with translated labels and dynamic messages,
updates the native window title, and conceals private cards. Match identity,
selected viewer and input survive the change. Existing validation errors are
translated again rather than retaining text from the previous language.

`localization.Localizations` loads `ui/languages.properties` and the configured
UTF-8 bundles directly. This avoids ResourceBundle fallback to the host language.
All labels, handoff messages, validation, roles, seat names, chip/pot/deck counts
and preview notices use message keys. Standard poker rank letters and suit
symbols are shared card notation. MessageFormat uses the selected locale for
numeric arguments, including call amounts and result messages.

## Add another language

1. Copy `src/main/resources/ui/messages.properties` into a new UTF-8 resource,
   such as `src/main/resources/ui/messages_fr_FR.properties`.
2. Translate every value, including `languageName` (the selector's native name).
   Keep all keys and MessageFormat argument indexes/types. Literal apostrophes
   follow MessageFormat escaping rules; use two apostrophes when needed.
3. Add the BCP 47 tag to the ordered `languages` list in
   `src/main/resources/ui/languages.properties`, then add its `bundle.<tag>` path.
   The `default` tag must be in that list. No Java source changes are required.
4. Run the localization/UI tests and manually inspect the translated screen.

Catalog loading rejects empty/duplicate/malformed language entries, unmatched
configuration, missing resources, missing/extra/blank translation values,
invalid format patterns and incompatible argument indexes/types. Validation scans
actual unquoted placeholders and retains every format kind used for a repeated
index. Translations may reorder arguments and repeated format kinds while keeping
the same required indexes and kinds. Quoted braces are literal text; doubled
apostrophes follow MessageFormat rules. Choice outputs are checked recursively
when MessageFormat would reparse them, so nested placeholders and their format
kinds are included. Argument counts include indexes used only inside choice
branches. Formatting
rejects unsupported language tags, unknown message keys and incorrect argument
counts/types. Available languages are immutable to consumers. Tests add a third
German language entirely through supplied resources and verify locale-specific
number output. Coverage also rejects the undefined `und` language and verifies
that a resource read failure retains its IOException cause and closes the stream.

## Reuse for later result displays

Future UI consumers can share a loaded catalog, retain the chosen language tag,
and call `text(languageTag, key, arguments...)`. The API also exposes
`languages()`, `defaultLanguage()`, `languageName(tag)` and `locale(tag)` for
selection and formatting. `result.awaitingSettlement` is supplied in both
bundles for consumers that need a pending result message.

The played screen now uses the same catalog for fold/check/call controls, public
board cards, phase/status labels, and winner/tie/uncontested results awaiting
settlement. Language changes translate the current result without replaying
any actions. Payouts and additional betting behavior remain unimplemented.
See [played-hand flow](played-hand.md) and
[bilingual played-screen cases](../bva/PlayedGamePanel.md).

See [catalog BVA](../bva/Localizations.md) and
[UI localization cases](../bva/GamePanelLocalization.md) for executable coverage
and the RED checkpoint. Headless Swing tests cover component behavior; native
window focus and font/layout checks remain manual desktop verification.
