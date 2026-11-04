# HandEvaluator contract cases

Week 5 defined this interface. Week 6 implements it once in
`PokerHandEvaluator`, shared by showdown comparison. Neither input is modified.
The concrete [BVA table](PokerHandEvaluator.md) expands these groups into 16
validation and 37 ranking cases with their actual RED/GREEN checkpoints.

## Method: compare(List<Card> firstCards, List<Card> secondCards)

Exactly seven non-null distinct cards are required in each holding. Overlap
between holdings is permitted for the shared board. Positive/zero/negative
means first stronger/tied/second stronger. No suit breaks a rank tie.

| ID | State/input | Expected output | Implemented / mapping |
| --- | --- | --- | --- |
| EVAL-01 | Null first or second list | NullPointerException | Yes: PHE-N01–N02 |
| EVAL-02 | Null element in either list | NullPointerException | Yes: PHE-N03–N08 |
| EVAL-03 | Either list has 0, 6 or 8 cards | IllegalArgumentException | Yes: PHE-S01–S06 |
| EVAL-04 | Seven entries with repeated card values | IllegalArgumentException | Yes: PHE-D01–D02 |
| EVAL-05 | First valid holding is stronger | Positive; inputs unchanged | Yes: positive RANK-C/K/E rows |
| EVAL-06 | Same best-five strength, including playing the board | Zero; inputs unchanged | Yes: RANK-T01–T03 |
| EVAL-07 | Second valid holding is stronger | Negative; inputs unchanged | Yes: RANK-E01/E03/E08 and every reversed comparison |
| EVAL-08 | Shared board cards appear in both holdings | Normal ranking; no cross-list duplicate rejection | Yes: RANK-T01/T03 and showdown tests |
| EVAL-09 | Categories, kickers, ace-low and best-five boundaries | Traditional high-hand Hold'em strength | Yes: RANK-C01–C08, K01–K18, E01–E08 |
