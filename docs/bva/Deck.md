# Deck BVA Analysis

## Contracts

- `Deck()` starts with all 52 different rank/suit combinations, unshuffled.
- `remainingCards()` returns the current remaining count, including zero.
- `deal(int count)` returns an immutable, independent list and removes those cards. Zero is allowed. Negative counts and counts above the remaining size must throw `IllegalArgumentException` before changing state.
- `Deck(Random random)` creates the same unshuffled deck with a supplied random source; null must throw `NullPointerException`.
- `shuffle()` reorders only remaining cards, preserving count and membership. Default and injected random sources are supported.

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

## Deck(Random random) and shuffle()

The seed is a test fixture, not a game setting. Equal fixed seeds must give repeatable results. The fixed 7/8 fixtures also detect a shuffle stub that does nothing or ignores the supplied source; this does not require every possible random shuffle to change order. Membership checks shuffle twice and verify that already-dealt cards are not reintroduced.

| ID | Method(s) | State/input | Expected result | Test implemented? |
|---|---|---|---|---|
| RANDOM-01 | Deck(Random) | null source | NullPointerException | Yes - `DeckTest.rejectsNullRandomSource` (passing) |
| RANDOM-02 | Deck(Random), shuffle | Two sources seeded 7 and another seeded 8 | Same seed repeats the order; fixed different fixtures differ from each other and the unshuffled deck | Yes - `DeckTest.shuffleUsesProvidedRandom` (passing) |
| SHUFFLE-01 | shuffle | 52 remaining | Preserve the 52 remaining cards | Yes - `DeckTest.shuffleOnlyChangesRemainingCardOrder` |
| SHUFFLE-02 | shuffle | 51 remaining | Preserve the 51 remaining cards | Yes - same parameterized test |
| SHUFFLE-03 | shuffle | 40 remaining | Preserve the 40 remaining cards | Yes - same parameterized test |
| SHUFFLE-04 | shuffle | One remaining | Preserve the single card | Yes - same parameterized test |
| SHUFFLE-05 | shuffle | Empty deck | Remain empty; no exception | Yes - same parameterized test |
| SHUFFLE-06 | Deck(), shuffle | Default random source | Shuffle without losing or duplicating cards | Yes - `DeckTest.defaultDeckCanBeShuffled` |

## Initial deal integration

| ID | State/input | Expected result | Test implemented? |
|---|---|---|---|
| DEAL-SETUP-01 | Shuffled deck; two rounds of one-card draws for six hands | Two cards per hand; 12 distinct dealt cards; 40 remaining; all 52 conserved | Yes - `DeckSetupTest.sixPlayersReceiveTwoUniqueCardsEach` |

This is an integration check of the already-tested deck operations. The setup controller still belongs to Member B; this test does not implement seating, dealer selection, chips, blinds, or the UI. It is not claimed as a new RED/GREEN pair.

## TDD status

Each implemented behavior has a recorded RED/GREEN pair. Valid dealing: RED `102465a`, GREEN `630deff`. Invalid requests: RED `a1490c5`, GREEN `3a53ede`. Controlled shuffle: RED `8e67799`, followed by this GREEN checkpoint. All 61 cases in the full suite pass. The deck is ready for the initial-deal integration check.
