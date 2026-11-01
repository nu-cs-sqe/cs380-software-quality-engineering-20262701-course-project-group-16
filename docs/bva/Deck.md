# Deck BVA Analysis

## Constructor: Deck()

| ID | Initial state | Expected result | Implemented? |
|---|---|---|---|
| DECK-01 | A new deck is constructed | Initial remaining-card count is 52 | Yes — `DeckTest.newDeckHas52Cards()` |
| DECK-02 | A new deck is constructed | Contains all 52 distinct rank/suit combinations | No |

## Method: remainingCards()

| ID | State of the system | Expected result | Implemented? |
|---|---|---|---|
| REMAIN-01 | Immediately after constructing a new deck | Returns 52 | Yes — covered by `DeckTest.newDeckHas52Cards()` |

## Method: deal(int count)

Each case starts with a fresh deck containing 52 cards.

| ID | Count | Expected result | Implemented? |
|---|---|---|---|
| DEAL-01 | -1 | Reject; deck unchanged | No |
| DEAL-02 | 0 | Return no cards; 52 remain | No |
| DEAL-03 | 1 | Return 1 card; 51 remain | No |
| DEAL-04 | 51 | Return 51 cards; 1 remains | No |
| DEAL-05 | 52 | Return 52 cards; deck empty | No |
| DEAL-06 | 53 | Reject; deck unchanged | No |