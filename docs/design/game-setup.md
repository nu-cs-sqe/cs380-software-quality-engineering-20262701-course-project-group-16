# Game Setup Design

## Goal and status

Initialize a Texas Hold'em game so it is ready for the first preflop turn. This is a working design for team review, not a claim that setup has been implemented.

The current implementation only reports an initial deck size of 52. It does not yet store cards, shuffle, or deal.

## Requirements

- Accept 1-6 human players and fill the remaining seats with bots, for six players total.
- Assign starting chips, select a dealer, and post the small and big blinds.
- Create and shuffle a standard 52-card deck with no jokers.
- Deal two distinct hole cards to each player; 12 cards are dealt and 40 remain.
- Identify the first preflop acting player and display the initialized game in a basic Swing interface.
- Show private hole cards only to the appropriate human player and preserve the total number of chips across stacks and contributions.

The setup user story and acceptance criteria are tracked in [issue #1](https://github.com/nu-cs-sqe/cs380-software-quality-engineering-20262701-course-project-group-16/issues/1).

## Classes and responsibilities

The project uses Java 11. Core model classes belong in `domain.model`; their tests use the matching package under `src/test/java`.

| Class or enum | Responsibility | Planned owner |
|---|---|---|
| `Rank`, `Suit` (enums) | Represent the 13 ranks and four suits | Kevin (Member A) |
| `Card` | Immutable rank/suit value; equality and hashing identify the same card | Kevin (Member A) |
| `Deck` | Own the remaining cards, shuffle them, and remove cards when dealing | Kevin (Member A) |
| `Player` | Hold seat, human/bot identity, chips, and hole cards | Member B |
| `GameState` | Hold players, dealer/blind positions, contributions, and current actor | Member B |
| `GameSetup` | Validate configuration and coordinate initialization using the model | Member B, integrated together |
| Swing views | Read input and display setup state and private cards | Shared |

`Player`, `GameState`, and `GameSetup` are proposed names and responsibilities to review with Member B before implementation. Game rules and validation stay in Java application/model code rather than in Swing event handlers.

## Member A interface

| API | Contract |
|---|---|
| `Deck()` | Create all 52 distinct rank/suit combinations |
| `int remainingCards()` | Return the actual number of cards currently in the deck |
| `void shuffle()` | Reorder remaining cards without adding, removing, or duplicating them |
| `List<Card> deal(int count)` | Return and remove exactly `count` cards; reject a negative count or a count above the remaining size with `IllegalArgumentException`, leaving the deck unchanged; zero returns an empty list |

Card rank and suit must be readable by the setup/UI code. Dealt lists must not expose the deck's internal mutable storage. The deck handles cards only; it does not depend on `Player` or Swing.

For the six-player initial deal, the setup coordinator requests one card per seat in each of two rounds, beginning clockwise after the dealer. The setup coordinator associates those cards with players.

## Setup sequence

1. Validate the human-player count and create six occupied seats.
2. Assign each player's starting stack and select the dealer.
3. Set the small blind clockwise after the dealer and the big blind after the small blind; move posted chips from stacks into tracked contributions.
4. Create and shuffle the deck, then deal two rounds of hole cards.
5. Set the first preflop actor clockwise after the big blind.
6. Display seats, stacks, dealer/blind indicators, the current actor, and the appropriate private cards. Initialize the community-card area as empty.

## Decisions still to confirm together

- Starting stack, small-blind amount, and big-blind amount; starting stacks must cover the initial blinds.
- Initial dealer selection policy and the representation of chip contributions.
- Member B's final class/method interfaces and the handoff of dealt cards.
- How the Swing interface hides/reveals cards when humans share a computer and starts the turn if the first actor is a bot.

Full betting rounds and hand evaluation are later work. This checkpoint adds documentation only.

## BVA and TDD checkpoints

- Document BVA for each public method of each non-view, non-enum class in `docs/bva/`, following its README. Card-value methods also need documented cases; enum classes are exempt.
- Use one focused failing test or parameterized group per behavior, then the minimum passing implementation. Record meaningful RED/GREEN checkpoints in Git.
- Cover deck membership/uniqueness, card value equality, dealing boundaries on fresh and partially depleted decks, and rejection without state changes.
- For shuffling, verify preservation of card membership and count; make randomness controllable for repeatable tests. Do not rely on the assertion that every shuffle must change the order.
- Verify that the initial deal yields six two-card hands, no duplicate cards, and 40 remaining cards.
- Keep the BVA implementation status linked to actual test methods. Do not mark uniqueness complete based on a size-only test.
