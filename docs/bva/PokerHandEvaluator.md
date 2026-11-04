# PokerHandEvaluator BVA

## Current checkpoint

Week 6 starts with input validation for the existing HandEvaluator API. The
concrete class validates both holdings without modifying either input. Hand
ranking is still a placeholder: valid holdings return zero, and the evaluator is
not connected to the application.

## Method: compare(List<Card> firstCards, List<Card> secondCards)

Each valid holding must contain exactly seven non-null, distinct card values.
Overlap between the two holdings is allowed for shared community cards. Every
rejection case also verifies that both lists retain their original contents and
order. These preservation assertions pass along with exception validation.
No-argument construction has no input boundary and is exercised by every case.

| ID | Method | System state / input | Expected output | Test implemented / status |
| --- | --- | --- | --- | --- |
| PHE-N01 | compare | Null first list; other list has seven valid cards | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullList |
| PHE-N02 | compare | Null second list; other list has seven valid cards | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullList |
| PHE-N03 | compare | Seven entries per list; first contains null at index 0 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-N04 | compare | Seven entries per list; first contains null at index 3 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-N05 | compare | Seven entries per list; first contains null at index 6 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-N06 | compare | Seven entries per list; second contains null at index 0 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-N07 | compare | Seven entries per list; second contains null at index 3 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-N08 | compare | Seven entries per list; second contains null at index 6 | NullPointerException; both lists unchanged | Yes, GREEN: rejectsNullCard |
| PHE-S01 | compare | First list has 0 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-S02 | compare | First list has 6 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-S03 | compare | First list has 8 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-S04 | compare | Second list has 0 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-S05 | compare | Second list has 6 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-S06 | compare | Second list has 8 cards; other list has seven valid cards | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsCardCountOutsideSeven |
| PHE-D01 | compare | Seven entries per list; first repeats a card using a separate equal-value Card | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsDuplicateCardValues |
| PHE-D02 | compare | Seven entries per list; second repeats a card using a separate equal-value Card | IllegalArgumentException; both lists unchanged | Yes, GREEN: rejectsDuplicateCardValues |

## Verification

The RED tests were committed as `f5fd72e`. They were rerun before implementation:
all 16 failed because exceptions were not thrown. The GREEN implementation now
rejects null lists/cards, sizes other than seven and duplicate card values within
each holding. Both inputs remain unchanged after rejection.

The full clean build passes all 617 tests, including the 16 validation cases,
with zero failures, errors or skips. JUnit display names match the IDs above.
Tests use real Cards and the evaluator, with no mocks or random input.

Next checkpoint: add focused RED tests for valid seven-card comparisons, then
implement ranking in stages. Successful-comparison immutability, every hand
category, kickers, ace-low straights and ties remain planned. This validation
checkpoint does not claim that hand evaluation or winner selection is complete.
