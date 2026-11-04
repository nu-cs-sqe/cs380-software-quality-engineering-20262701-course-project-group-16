# HandEvaluator contract cases

Week 5 defined the interface. Week 6 now has input validation in
PokerHandEvaluator with 16 passing BVA/TDD cases. Hand ranking is still a
placeholder; no passing ranking coverage is claimed.
See [the concrete evaluator BVA](PokerHandEvaluator.md) for unique case IDs and
observed test results.

## Method: compare(List<Card> firstCards, List<Card> secondCards)

Each valid input has exactly seven distinct card values. The two inputs can overlap because the community cards are shared. Result signs follow the interface Javadoc. A later implementation must preserve both input lists.

| ID | State/input | Expected output | Test implemented? |
|---|---|---|---|
| EVAL-01 | Null first or second list | NullPointerException | Yes, GREEN: PHE-N01–N02 |
| EVAL-02 | Null element in either list | NullPointerException | Yes, GREEN: PHE-N03–N08 |
| EVAL-03 | Either list has 0, 6, or 8 cards | IllegalArgumentException | Yes, GREEN: PHE-S01–S06 |
| EVAL-04 | Exactly seven entries but a repeated card value in either list | IllegalArgumentException | Yes, GREEN: PHE-D01–D02 |
| EVAL-05 | Seven valid cards per list; first is stronger | Positive result; inputs unchanged | No |
| EVAL-06 | Same five-card strength, including playing the board | Zero; inputs unchanged | No |
| EVAL-07 | Second holding is stronger | Negative result; inputs unchanged | No |
| EVAL-08 | Shared community cards occur in both valid lists | Allowed; compare normal hand strength | No |
| EVAL-09 | Every hand category, kickers, and ace-low straight boundaries | Standard best-five-of-seven ranking | No |

The first four groups are expanded into 16 uniquely identified cases. Remaining
comparison/ranking cases will be expanded before their implementation.
