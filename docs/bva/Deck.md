# Deck BVA Analysis

## Contracts

- `Deck()` starts with all 52 different rank/suit combinations, unshuffled.
- `remainingCards()` returns the current remaining count, including zero.
- `deal(int count)` returns an immutable, independent list and removes those cards. Zero is allowed. Negative counts and counts above the remaining size must throw `IllegalArgumentException` before changing state.
- `shuffle()` will reorder only remaining cards. Controlled randomness will be added in a later checkpoint.

## Constructor and remainingCards()

| ID | State | Expected result | Test implemented? |
|---|---|---|---|
| DECK-01 | New deck | 52 remaining cards | Yes - `DeckTest.newDeckHas52Cards` |
| DECK-02 | New deck, deal all cards | Exactly one of each rank/suit combination; zero remain | Yes - `DeckTest.newDeckContainsEveryCardOnce` (RED) |
| REMAIN-01 | Fresh, partially used, one remaining, or empty | Count follows each successful draw | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |

## deal(int count): valid boundaries

Each case starts with a fresh deck, consumes the stated prefix, and requests the next batch. The test then drains the rest and verifies that all 52 cards are conserved without duplicates.

| ID | Already dealt | Request | Expected result | Test implemented? |
|---|---|---|---|---|
| DEAL-V01 | 0 | 0 | Return 0; 52 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V02 | 0 | 1 | Return 1; 51 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V03 | 0 | 51 | Return 51; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V04 | 0 | 52 | Return 52; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V05 | 1 | 0 | Return 0; 51 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V06 | 1 | 1 | Return 1; 50 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V07 | 1 | 50 | Return 50; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V08 | 1 | 51 | Return 51; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V09 | 12 | 0 | Return 0; 40 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V10 | 12 | 1 | Return 1; 39 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V11 | 12 | 39 | Return 39; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V12 | 12 | 40 | Return 40; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V13 | 51 | 0 | Return 0; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V14 | 51 | 1 | Return 1; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |
| DEAL-V15 | 52 | 0 | Return 0; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (RED) |

| ID | State | Expected result | Test implemented? |
|---|---|---|---|
| DEAL-OWN-01 | A two-card returned hand, followed by drawing the rest | Caller cannot clear the hand; later dealing does not change it | Yes - `DeckTest.dealtHandIsReadOnlyAndIndependent` (RED) |

## Invalid requests planned next

For remaining sizes 52, 51, 40, 1, and 0, test -1, `Integer.MIN_VALUE`, remaining + 1, and `Integer.MAX_VALUE`. Each must reject the request without changing count, cards, or order. Tests are not implemented yet.

## TDD status

The initial count-only behavior already passes. This RED checkpoint adds real-card and valid-dealing tests before replacing the fixed count and the `deal` compilation stub.
