# Profile screen BVA

Swing profile/account views use the rubric's view exemption; their behavioral
contracts are still tested on the event thread before implementation. Production
startup uses persistent profiles; the existing single-argument GamePanel constructor
remains an injectable unprofiled component fixture for earlier regression tests.

| ID | Methods/events and system/input state | Expected output | Implemented? |
| --- | --- | --- | --- |
| UI-AC01-en/zh | New desktop, no accounts, choose language | Localized profiles tab, password masking, no match start before login | Yes - locally passing |
| UI-AC02-en/zh | Create/login two humans, start, switch private viewers | Correct profile names/seats, four bots, stable binding, hidden handoffs | Yes - locally passing |
| UI-AC03-en/zh | Real create/login/play/settle/reopen/login/new match | Saved lifetime totals displayed; new stacks/match totals reset | Yes - locally passing |
| UI-AC04-en/zh | Wrong password then correct login; duplicate selection | Localized rejection, no joined account; duplicate disabled | Yes - locally passing |
| UI-AC05 | Six joined profiles and seventh saved profile | Seventh login disabled; remove enables another selection | Yes - locally passing |
| UI-AC06-en/zh | Blank name or 5/16-character password | Localized validation; no account saved; input cleared | Yes - locally passing |
| UI-AC07 | Slow hash gate, event-thread heartbeat | UI remains responsive; mutation controls disabled; captured char arrays cleared | Yes - locally passing |
| UI-AC08-en/zh | Corrupt catalog, then reload after repair | Localized storage error; bytes preserved; retry succeeds | Yes - locally passing |
| UI-AC09-en/zh | Settlement write failure then retry | Error displayed; hand remains retryable; lifetime increments once | Yes - locally passing |
| UI-AC10 | Language/tab/focus change with credentials/private cards | Credentials/cards cleared; existing match retained | Yes - locally passing |
| UI-AC11 | Remove from next-match roster during ongoing match | Current binding retained; next match uses new roster | Yes - locally passing |
| UI-AC12-en/zh | Layout at normal window size | Form/action controls fit and remain reachable | Yes - locally passing |

Tests use JUnit TempDir catalogs, real services/session/filesystem and controlled
hashers where isolation matters. No real profile data or extra reports are created.

UI-AC13 verifies that markup-like names are escaped in roster/seat/handoff
presentation. UI-AC14 injects a hashing-provider failure and checks both localized
feedback and zeroed captured password buffers. These are regression hardening
following the original 20 RED cases, not claimed additional RED cycles.

UI-AC15 checks that two same-name profiles have distinct picker entries and that
login preserves the chosen account. Saved rows and next-match seats are numbered;
opaque persistent IDs remain backend identity, not user-chosen identifiers.

The original 20 cases were observed RED (20 expected stub failures), then all 20
GREEN after the selection fix. Three subsequent regressions also pass: 23 profile
screen cases total. Tests verify English/Chinese controls and real persistence;
UI/Main remain under the view exemption for scoped coverage/mutation. Full suite
1,219 passing; see [quality evidence](../quality.md#desktop-profile-flow--november-10-2026).
Native font/focus walkthrough remains pending because the desktop bridge failed.
No separate Git TDD checkpoints, commits or pushes are claimed.

Password-hint layout follow-up: the policy text now uses a read-only, word-wrapped
text area with two preferred rows and the surrounding label font. English/Chinese
resource text uses plain wording and an explicit line break; the hint can wrap
without JLabel ellipsis. Password validation itself is unchanged. This is a
presentation-only adjustment; the existing profile/layout checks are reused.

Registration length is now 6–15 characters in both hints and backend checks.
UI-AC06 uses 5/16-character passwords and checks the revised plain-text range;
fixtures use a valid 14-character sample. Passwords saved under the older policy
remain accepted for sign-in; see PW-C07/08.
