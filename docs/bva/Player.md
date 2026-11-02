# Player BVA

## Contract

`Player(String id, int seat, PlayerType type, long chipStack)` represents one
player. It is immutable; chip transfers and other game behavior are outside
this first increment.

- `id` must be non-null and contain at least one non-whitespace character
  according to Java 11's `String.isBlank()`. The supplied ID is retained.
- `seat` must be in `0..5`, with the team using a consistent seat order.
- `type` must be `PlayerType.HUMAN` or `PlayerType.BOT`, never null.
- `chipStack` is a nonnegative `long`. Zero is allowed to represent a player
  with no chips; initial-game stack requirements belong to game setup.
- An invalid constructor argument throws `IllegalArgumentException`.
- `getId()`, `getSeat()`, `getType()`, and `getChipStack()` return the supplied
  valid values. ID uniqueness across players belongs to game setup.

## Cases

Each case constructs a fresh player. Unless a row overrides a value, the input
state is `id="player-1"`, `seat=2`, `type=HUMAN`, and `chipStack=1000`.
Rejected inputs produce no constructed player. `PlayerType` is an enum and is
exempt from a separate BVA document under the course template.

| ID | Methods under test | State / input override | Expected result | Implemented? |
| --- | --- | --- | --- | --- |
| P-C01 | Constructor | `id=null` | `IllegalArgumentException` | Yes |
| P-C02 | Constructor | `id=""` | `IllegalArgumentException` | Yes |
| P-C03 | Constructor | `id=" "` | `IllegalArgumentException` | Yes |
| P-C04 | Constructor | `id="\t"` | `IllegalArgumentException` | Yes |
| P-C05 | Constructor | `id="\n"` | `IllegalArgumentException` | Yes |
| P-C06 | Constructor | ID is Unicode em space U+2003 | `IllegalArgumentException` | Yes |
| P-C07 | Constructor, `getId()` | `id="p"` (one nonblank character) | Constructed; getter returns `"p"` | Yes |
| P-C08 | Constructor, `getId()` | `id=" player-2 "` | Constructed; getter retains the surrounding spaces | Yes |
| P-C09 | Constructor | `seat=Integer.MIN_VALUE` | `IllegalArgumentException` | Yes |
| P-C10 | Constructor | `seat=-1` | `IllegalArgumentException` | Yes |
| P-C11 | Constructor | `seat=6` | `IllegalArgumentException` | Yes |
| P-C12 | Constructor | `seat=Integer.MAX_VALUE` | `IllegalArgumentException` | Yes |
| P-C13 | Constructor, `getSeat()` | `seat=0` | Constructed; getter returns `0` | Yes |
| P-C14 | Constructor, `getSeat()` | `seat=1` | Constructed; getter returns `1` | Yes |
| P-C15 | Constructor, `getSeat()` | `seat=4` | Constructed; getter returns `4` | Yes |
| P-C16 | Constructor, `getSeat()` | `seat=5` | Constructed; getter returns `5` | Yes |
| P-C17 | Constructor | `type=null` | `IllegalArgumentException` | Yes |
| P-C18 | Constructor, `getType()` | `type=HUMAN` | Constructed; getter returns `HUMAN` | Yes |
| P-C19 | Constructor, `getType()` | `type=BOT` | Constructed; getter returns `BOT` | Yes |
| P-C20 | Constructor | `chipStack=Long.MIN_VALUE` | `IllegalArgumentException` | Yes |
| P-C21 | Constructor | `chipStack=-1` | `IllegalArgumentException` | Yes |
| P-C22 | Constructor, `getChipStack()` | `chipStack=0` | Constructed; getter returns `0` | Yes |
| P-C23 | Constructor, `getChipStack()` | `chipStack=1` | Constructed; getter returns `1` | Yes |
| P-C24 | Constructor, `getChipStack()` | `chipStack=Long.MAX_VALUE-1` | Constructed; getter returns that exact value | Yes |
| P-C25 | Constructor, `getChipStack()` | `chipStack=Long.MAX_VALUE` | Constructed; getter returns that exact value | Yes |

## Test mapping

The case IDs are included in the parameterized test display names in
`PlayerTest`. Every public constructor/method is mapped above. Update a case to
implemented only after its test has been written, executed, and passed.
