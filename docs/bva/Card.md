# Card BVA Analysis

## Input domains

A card has one `Rank` and one `Suit`. Java enums restrict non-null inputs to the declared values; arbitrary numbers or strings cannot be passed to this constructor. The valid cases below sample the lowest/next-lowest and highest/next-highest ranks and all four suits. Rank and suit are required caller-supplied values.

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

The former programmer-null constructor cases are retired under the non-null
caller contract; domain and Java Object.equals cases remain current.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|

## Methods: equals(Object other), hashCode()

Equality is based on rank and suit rather than object identity. The cases are categorical boundaries, not numeric ranges. Compare both directions when both objects are cards. Equal card values must have equal hash codes so sets can detect duplicates; unequal cards are allowed to share a hash code.

| ID | Method(s) | State/input | Expected output | Test implemented? |
|---|---|---|---|---|
| CARD-08 | equals | A card compared with itself | true | Yes - `CardTest.cardEqualityUsesRankAndSuit` |
| CARD-09 | equals | Separate cards with ACE/SPADES | true in both directions | Yes - same parameterized test |
| CARD-10 | equals | ACE/SPADES versus KING/SPADES | false in both directions | Yes - same parameterized test |
| CARD-11 | equals | ACE/SPADES versus ACE/HEARTS | false in both directions | Yes - same parameterized test |
| CARD-12 | equals | ACE/SPADES versus KING/HEARTS | false in both directions | Yes - same parameterized test |
| CARD-13 | equals | ACE/SPADES versus null | false | Yes - same parameterized test |
| CARD-14 | equals | ACE/SPADES versus a String | false | Yes - same parameterized test |
| CARD-15 | hashCode, equals | A set receives two separate ACE/SPADES cards and one ACE/HEARTS | Equal cards have equal hashes; set contains two distinct card values | Yes - `CardTest.equalCardsHaveSameHashAndDeduplicateInASet` |

## TDD checkpoint status

Rank/suit storage was completed in GREEN checkpoint `2219068`, and null validation in GREEN checkpoint `acbd186`. Equality RED checkpoint `82175f2` recorded failures for separate same-value cards and hash-based duplicate detection. This GREEN checkpoint overrides `equals` and `hashCode` using rank and suit. Those 15 cases describe the historical checkpoint. The three constructor-null
cases are now retired; the remaining 12 Card cases are current.
