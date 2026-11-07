# Game rules

This project uses [Texas Hold'em rules](https://www.pokerstars.com/poker/learn/lesson/texas-holdem-rules/)
with the match settings below. The intended full game is no-limit Hold'em;
the Week 6 playable subset is listed separately to show what is available now.
All chips are virtual and have no cash value.

## Table and match

- Six fixed seats, with 1–6 human players and bots filling the rest. Humans share
  one computer and explicitly reveal their own cards at each handoff.
- Each player starts a new match with 1,000 chips: 6,000 chips total. Blinds are
  fixed at 10/20, with no ante, blind increases or rebuys. These agreed defaults
  replace the earlier 5/10 proposal. Tests may inject other valid settings.
- A hand is one deal and its betting rounds. A match is a series of hands with
  the same players. Players cannot individually leave, join or be replaced
  during a match. Folding ends participation in that hand, not the match.
- The whole game may be stopped at any time by closing the window. There is no
  login/logout or saved-match feature. Starting again abandons the previous
  match and starts everyone at the initial stack; it does not resume that match.
- The intended full match carries stacks between settled hands, eliminates
  zero-chip players from further deals, and ends when one player holds all
  6,000 chips. There is no fixed hand count. Settlement, further hands and
  automatic match completion remain future work.

## Deal and betting order

Use a standard 52-card deck without jokers. Shuffle a fresh deck and deal two
private cards per player, one at a time clockwise after the dealer in two rounds.
With six players, 12 cards are dealt and 40 remain. Select the first dealer
randomly. The small and big blinds are the next two seats clockwise; the first
preflop actor follows the big blind. After the flop, action starts with the
first eligible seat after the dealer.

Each betting round requires all eligible decisions, including the big blind's
option to check when nobody raises. Skip folded, all-in and eliminated players.
Deal the flop (three cards), turn (one), and river (one), burning one card before
each street. Players use their private cards and the five shared cards.

The intended full rules allow folding, checking when owing zero, calling,
betting and raising subject to stack and minimum-raise rules. The current
engine supports unraised preflop FOLD/CHECK/CALL and postflop CHECK/FOLD.
A short stack can call all-in for its remaining chips. Unsupported bets/raises
and postflop calls are rejected; they are not playable yet. Out-of-turn or
invalid actions preserve chips, cards, actor and accepted-action history.

## Hand result and chips

Compare the best five of seven cards among eligible players. From strongest to
weakest: straight flush, four of a kind, full house, flush, straight, three of
a kind, two pair, one pair, high card. Compare category ranks and kickers;
an ace may be low in A–2–3–4–5. Equal best hands tie. Folded players cannot win;
all-in players remain eligible. If everyone else folds, the sole remaining
player wins uncontested. If further betting is impossible, finish the board
without inventing player actions.

Week 6 returns winner(s) with **AWAITING_SETTLEMENT**. Committed chips remain in
the pot, and stacks plus pot still total 6,000. No chips are awarded yet.
Future settlement must split tied pots, enforce each player's side-pot
eligibility, conserve chips, and finish the hand before starting the next one.
See the full reference above for betting/payout rules beyond the current subset.

Accepted voluntary actions feed pending statistics once. Blinds, queries and
rejected actions do not create voluntary observations. A comparison is not a
settled hand: lifetime statistics remain unchanged until settlement supplies
verified completion facts. Stopping or starting a new match discards pending
statistics. Profile storage remains future work.
