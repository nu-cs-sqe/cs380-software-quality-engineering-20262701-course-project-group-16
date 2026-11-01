# Card BVA Analysis

## Input domains

A card has one `Rank` and one `Suit`. Java enums restrict non-null inputs to the declared values; arbitrary numbers or strings cannot be passed to this constructor. The valid cases below sample the lowest/next-lowest and highest/next-highest ranks and all four suits. Null is the invalid input category.

`Rank` and `Suit` are enums and do not require separate BVA documents under the repository's BVA rules.

## Methods: Card(Rank rank, Suit suit), getRank(), getSuit()

Each case constructs a new card, then reads its rank and suit. A getter must return the supplied value, independently of the other field.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| CARD-01 | Construct with TWO, CLUBS | getRank() returns TWO; getSuit() returns CLUBS | Yes - `CardTest.cardRetainsRankAndSuit` (passing) |
| CARD-02 | Construct with THREE, DIAMONDS | getRank() returns THREE; getSuit() returns DIAMONDS | Yes - same passing parameterized test |
| CARD-03 | Construct with KING, HEARTS | getRank() returns KING; getSuit() returns HEARTS | Yes - same passing parameterized test |
| CARD-04 | Construct with ACE, SPADES | getRank() returns ACE; getSuit() returns SPADES | Yes - same passing parameterized test |

## Constructor validation: Card(Rank rank, Suit suit)

The required contract rejects null rank/suit values with `NullPointerException`. The cases below are implemented as tests for the RED checkpoint; constructor validation is not implemented yet.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| CARD-05 | Construct with null rank and SPADES | Reject with NullPointerException | Yes - `CardTest.cardRejectsNullRankOrSuit` (RED checkpoint) |
| CARD-06 | Construct with ACE and null suit | Reject with NullPointerException | Yes - `CardTest.cardRejectsNullRankOrSuit` (RED checkpoint) |
| CARD-07 | Construct with null rank and null suit | Reject with NullPointerException | Yes - `CardTest.cardRejectsNullRankOrSuit` (RED checkpoint) |

## TDD checkpoint status

The rank/suit RED checkpoint (`efb8581`) was followed by GREEN checkpoint `2219068`, which stores the supplied values in private final fields. The current RED checkpoint adds the three null-input cases before implementing constructor validation. The four valid-card cases and existing Deck test remain passing. Value equality is planned for a later checkpoint.
