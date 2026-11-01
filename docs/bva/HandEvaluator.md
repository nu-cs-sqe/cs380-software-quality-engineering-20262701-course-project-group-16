# HandEvaluator contract cases

Week 5 defines the interface only. There is no evaluator implementation or evaluator test suite yet; these cases are planned for Week 6. Unlike Card/Deck, this file does not claim passing implementation coverage.

## Method: compare(List<Card> firstCards, List<Card> secondCards)

Each valid input has exactly seven distinct card values. The two inputs can overlap because the community cards are shared. Result signs follow the interface Javadoc. A later implementation must preserve both input lists.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| EVAL-01 | Null first or second list | NullPointerException | No |
| EVAL-02 | Null element in either list | NullPointerException | No |
| EVAL-03 | Either list has 0, 6, or 8 cards | IllegalArgumentException | No |
| EVAL-04 | Exactly seven entries but a repeated card value in either list | IllegalArgumentException | No |
| EVAL-05 | Seven valid cards per list; first is stronger | Positive result; inputs unchanged | No |
| EVAL-06 | Same five-card strength, including playing the board | Zero; inputs unchanged | No |
| EVAL-07 | Second holding is stronger | Negative result; inputs unchanged | No |
| EVAL-08 | Shared community cards occur in both valid lists | Allowed; compare normal hand strength | No |
| EVAL-09 | Every hand category, kickers, and ace-low straight boundaries | Standard best-five-of-seven ranking | No |

Separate Week 6 tests will expand the grouped input categories into uniquely identified cases before implementing them.
