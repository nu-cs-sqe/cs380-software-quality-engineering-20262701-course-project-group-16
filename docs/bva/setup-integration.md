# Setup integration BVA

These cases compose Member A's cards/deck with Member B's setup, state and views.
They specify the original card/deck-to-state boundary. GameInitializer and the
Swing SetupSession now compose these components in application code; see
[setup design](../design/game-setup.md). The later
[showdown fixtures](../design/showdown.md) verify a separate closed-state result
boundary, without claiming played-hand betting progression.

| ID | Methods | State/input | Expected output | Implemented |
| --- | --- | --- | --- | --- |
| IG-01 | GameSetup.create; Deck.shuffle/deal/remainingCards; PlayerState; GameState; GameState.viewFor | Two humans and four bots; dealer 0; a full seeded deck; two dealing rounds clockwise after dealer | Twelve distinct hole cards; 40 remaining before ownership transfer; PREFLOP state; actor 3; stacks 990/980; pot 30; total 6000; each human/bot view reveals only its own cards; original setup unchanged | Yes |
| IG-02 | Card.equals/hashCode; PlayerState; GameState | Two different Card objects with the same rank/suit placed in different hands | Reject the duplicate physical card by value; retain previous valid state | Yes |
