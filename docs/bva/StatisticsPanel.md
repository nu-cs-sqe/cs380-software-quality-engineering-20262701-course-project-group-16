# Bilingual completed-statistics UI BVA

GamePanel owns the existing session; StatisticsPanel renders immutable snapshots
in a non-editable, horizontally scrollable table. Every label/format lives in the
translation resources. No cards or mutable collectors reach the table.

| ID | Methods | State/input | Expected output | Implemented test |
| --- | --- | --- | --- | --- |
| UI-ST01-en/zh | GamePanel construction; refresh; settlement/next-hand handlers; StatisticsPanel.render | Before start, unpaid result, paid hand, next hand, second settlement, language change, new match | Buttons enabled only at valid stages; hands0→1→2 then reset0; old data remains pending before payment; totals survive continuation/locale changes; correct N/A/percent/column labels; table not editable | Yes; StatisticsGamePanelTest.paidHandsAppearInStatisticsAndContinueWithoutResettingTotals (two locale invocations) |
| UI-ST02-en/zh | tab selection; hidePrivateCards | Revealed cards, then select Statistics and return | Private cards concealed in session and screen | Yes; same test |

Both new locale cases failed before the UI implementation. Two additional
locale layout cases failed when the embedded table header had no parent/height;
explicitly attaching the header fixes the 1,100 × 740 layout. See
StatisticsGamePanelTest.tableHeadersStayVisibleAtTheDefaultWindowSize. Existing screen,
privacy, played-result and localization completeness/signature tests remain
part of the full build. Native operating-system focus delivery and cross-platform
font checks still need the team's manual walkthrough.

## Profile lifetime rows

The view also renders authenticated profiles with their saved StatisticsView
values (UI-AC01/03/04). UI-AC03 verifies a real settled hand after application
restart, profile names, read-only cells and separate fresh-match totals. Empty/
unauthenticated selection has zero rows; unavailable rates retain the existing
locale formatting. UI-AC09 covers failed-save feedback/retry; UI-AC13 escapes
markup-like names. See [profile-screen BVA](ProfileScreen.md).
