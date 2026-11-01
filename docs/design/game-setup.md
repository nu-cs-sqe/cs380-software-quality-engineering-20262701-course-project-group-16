# Game Setup Design

## Goal and status

Initialize a Texas Hold'em game so it is ready for the first preflop turn. This document separates the completed Member A components from the remaining setup integration.

Member A's Week 5 components are implemented: immutable cards with value equality, a full 52-card deck, controlled shuffling, validated dealing, and immutable returned hands. The six-player dealing sequence is tested. This does not make the full game playable; Member B and shared setup/UI integration are still required.

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
| `HandEvaluator` (interface only) | Define the Week 6 comparison contract | Kevin (Member A) |
| `Player` | Hold seat, human/bot identity, chips, and hole cards | Member B |
| `GameState` | Hold players, dealer/blind positions, contributions, and current actor | Member B |
| `GameSetup` | Validate configuration and coordinate initialization using the model | Member B, integrated together |
| Swing views | Read input and display setup state and private cards | Shared |

`Player`, `GameState`, and `GameSetup` are proposed names and responsibilities to review with Member B before implementation. Game rules and validation stay in Java application/model code rather than in Swing event handlers.

## Member A interface

| API | Contract |
|---|---|
| `Deck()` | Create all 52 distinct rank/suit combinations, initially unshuffled |
| `Deck(Random random)` | Same deck with a supplied shuffle source for controlled tests; reject null |
| `int remainingCards()` | Return the actual number of cards currently in the deck |
| `void shuffle()` | Reorder remaining cards without adding, removing, or duplicating them |
| `List<Card> deal(int count)` | Return an immutable independent list and remove exactly `count` cards; reject a negative count or a count above the remaining size with `IllegalArgumentException`, leaving the deck unchanged; zero returns an empty list |

Cards expose `getRank()` and `getSuit()`, reject null inputs, and compare by rank and suit. Equal card values have equal hash codes. Card rank and suit are readable by the setup/UI code. Dealt lists must not expose the deck's internal mutable storage. The deck handles cards only; it does not depend on `Player` or Swing.

For the six-player initial deal, the setup coordinator requests one card per seat in each of two rounds, beginning clockwise after the dealer. The setup coordinator associates those cards with players.

## Setup sequence

1. Validate the human-player count and create six occupied seats.
2. Assign each player's starting stack and select the dealer.
3. Set the small blind clockwise after the dealer and the big blind after the small blind; move posted chips from stacks into tracked contributions.
4. Create and shuffle the deck, then deal two rounds of hole cards.
5. Set the first preflop actor clockwise after the big blind.
6. Display seats, stacks, dealer/blind indicators, the current actor, and the appropriate private cards. Initialize the community-card area as empty.

## Decisions still to confirm together

The team selected 1,000 starting chips per player, fixed 5/10 blinds, a random first dealer, and no rebuys. The detailed rules are committed on `docs/project-setup` and still need to be combined with this branch.

- Confirm the representation of chip contributions with Member B.
- Member B's final class/method interfaces and the handoff of dealt cards.
- How the Swing interface hides/reveals cards when humans share a computer and starts the turn if the first actor is a bot.

Full betting rounds and hand evaluation are later work. `HandEvaluator.compare(List<Card>, List<Card>)` is an interface only: each input has seven non-null distinct cards, shared community cards may occur in both lists, and the result is positive/zero/negative for first stronger/tied/second stronger. Neither input may be modified. Invalid-size/duplicate inputs require `IllegalArgumentException`; null lists/cards require `NullPointerException`. Its planned BVA cases are in `docs/bva/HandEvaluator.md`; no evaluator behavior is implemented or claimed tested yet.

## Using the deck in setup

Create a new `Deck` for each hand, call `shuffle()`, then call `deal(1)` for each occupied seat clockwise after the dealer in two rounds. The setup controller owns seat ordering and associates the returned cards with its players; Deck has no dependency on Player, bots, chips, or Swing. With six players, the initial deal consumes 12 cards and leaves 40. If a caller requests cards that are unavailable, it receives `IllegalArgumentException` and the same deck remains usable.

Tests can use `new Deck(new Random(seed))` to repeat a shuffle. Game setup should normally use `new Deck()` rather than a fixed test seed.

## BVA and TDD checkpoints

- Document BVA for each public method of each non-view, non-enum class in `docs/bva/`, following its README. Card-value methods also need documented cases; enum classes are exempt.
- Use one focused failing test or parameterized group per behavior, then the minimum passing implementation. Record meaningful RED/GREEN checkpoints in Git.
- Cover deck membership/uniqueness, card value equality, dealing boundaries on fresh and partially depleted decks, and rejection without state changes.
- For shuffling, verify preservation of card membership and count; make randomness controllable for repeatable tests. Do not rely on the assertion that every shuffle must change the order.
- Verify that the initial deal yields six two-card hands, no duplicate cards, and 40 remaining cards.
- Keep the BVA implementation status linked to actual test methods. Do not mark uniqueness complete based on a size-only test.
