# Card BVA Analysis

## Input domains

A card has one `Rank` and one `Suit`. Java enums restrict non-null inputs to the declared values; arbitrary numbers or strings cannot be passed to this constructor. The valid cases below sample the lowest/next-lowest and highest/next-highest ranks and all four suits. Null is the invalid input category.

`Rank` and `Suit` are enums and do not require separate BVA documents under the repository's BVA rules.

## Methods: Card(Rank rank, Suit suit), getRank(), getSuit()

Each case constructs a new card, then reads its rank and suit. A getter must return the supplied value, independently of the other field.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| CARD-01 | Construct with TWO, CLUBS | getRank() returns TWO; getSuit() returns CLUBS | Yes - `CardTest.cardRetainsRankAndSuit` (RED checkpoint) |
| CARD-02 | Construct with THREE, DIAMONDS | getRank() returns THREE; getSuit() returns DIAMONDS | Yes - same parameterized test |
| CARD-03 | Construct with KING, HEARTS | getRank() returns KING; getSuit() returns HEARTS | Yes - same parameterized test |
| CARD-04 | Construct with ACE, SPADES | getRank() returns ACE; getSuit() returns SPADES | Yes - same parameterized test |

## Constructor validation cases planned for a later checkpoint

The planned contract rejects null rank/suit values with `NullPointerException`. These validation cases are not yet implemented or verified.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| CARD-05 | Construct with null rank and SPADES | Reject with NullPointerException | No |
| CARD-06 | Construct with ACE and null suit | Reject with NullPointerException | No |
| CARD-07 | Construct with null rank and null suit | Reject with NullPointerException | No |

## TDD checkpoint status

The rank/suit test is written before the storage behavior. `Card` currently has only the constructor and getter stubs required to compile the test. Passing behavior and value equality will be implemented in later checkpoints.
