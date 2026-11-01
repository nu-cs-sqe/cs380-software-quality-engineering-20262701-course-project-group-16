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
| DECK-02 | New deck, deal all cards | Exactly one of each rank/suit combination; zero remain | Yes - `DeckTest.newDeckContainsEveryCardOnce` (passing) |
| REMAIN-01 | Fresh, partially used, one remaining, or empty | Count follows each successful draw | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |

## deal(int count): valid boundaries

Each case starts with a fresh deck, consumes the stated prefix, and requests the next batch. The test then drains the rest and verifies that all 52 cards are conserved without duplicates.

| ID | Already dealt | Request | Expected result | Test implemented? |
|---|---|---|---|---|
| DEAL-V01 | 0 | 0 | Return 0; 52 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V02 | 0 | 1 | Return 1; 51 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V03 | 0 | 51 | Return 51; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V04 | 0 | 52 | Return 52; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V05 | 1 | 0 | Return 0; 51 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V06 | 1 | 1 | Return 1; 50 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V07 | 1 | 50 | Return 50; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V08 | 1 | 51 | Return 51; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V09 | 12 | 0 | Return 0; 40 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V10 | 12 | 1 | Return 1; 39 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V11 | 12 | 39 | Return 39; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V12 | 12 | 40 | Return 40; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V13 | 51 | 0 | Return 0; 1 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V14 | 51 | 1 | Return 1; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |
| DEAL-V15 | 52 | 0 | Return 0; 0 remain | Yes - `DeckTest.dealsValidNumberOfCards` (passing) |

| ID | State | Expected result | Test implemented? |
|---|---|---|---|
| DEAL-OWN-01 | A two-card returned hand, followed by drawing the rest | Caller cannot clear the hand; later dealing does not change it | Yes - `DeckTest.dealtHandIsReadOnlyAndIndependent` (passing) |

## deal(int count): invalid boundaries

The test compares the count and the entire remaining card sequence with an untouched control deck after rejection. This includes the empty-deck boundary and extreme integer values.

| ID | Already dealt | Request | Expected result | Test implemented? |
|---|---|---|---|---|
| DEAL-I01 | 0 | -1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I02 | 0 | Integer.MIN_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I03 | 0 | 53 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I04 | 0 | Integer.MAX_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I05 | 1 | -1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I06 | 1 | Integer.MIN_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I07 | 1 | 52 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I08 | 1 | Integer.MAX_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I09 | 12 | -1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I10 | 12 | Integer.MIN_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I11 | 12 | 41 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I12 | 12 | Integer.MAX_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I13 | 51 | -1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I14 | 51 | Integer.MIN_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I15 | 51 | 2 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I16 | 51 | Integer.MAX_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I17 | 52 | -1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I18 | 52 | Integer.MIN_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I19 | 52 | 1 | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |
| DEAL-I20 | 52 | Integer.MAX_VALUE | IllegalArgumentException; deck unchanged | Yes - `DeckTest.rejectsInvalidDealWithoutChangingState` |

## TDD status

RED commit `102465a` recorded the missing card storage and dealing behavior. This GREEN checkpoint stores all 52 cards, removes successful draws, and returns immutable independent hands. All 33 cases in the full suite pass. The current RED checkpoint checks the required exception type and unchanged deck for all invalid boundaries; explicit validation is not implemented yet.
