# Game rules

This project uses [Texas Hold'em rules](https://www.pokerstars.com/poker/learn/lesson/texas-holdem-rules/)
with the match settings below. The intended full game is no-limit Hold'em;
the current engine support and desktop controls are described separately below.
All chips are virtual and have no cash value.

## Table and match

- Six fixed seats, with 1–6 human players and bots filling the rest. Humans share
  one computer and explicitly reveal their own cards at each handoff.
- Each player starts a new match with 1,000 chips: 6,000 chips total. Blinds are
  fixed at 10/20, with no ante, blind increases or rebuys. These agreed defaults
  replace the earlier 5/10 proposal. Tests may inject other valid settings.
- A hand is one deal and its betting rounds. Seats and chip ownership stay
  fixed during a match. Humans can leave; new players cannot join or replace
  them. Ordinary folding ends participation in the current hand only.
- Leaving immediately folds an active human, including an out-of-turn departure.
  Another pending player keeps their turn while betting remains open. An
  already-all-in human keeps their current-hand eligibility and payouts. Leaving
  after folding or after the hand's result creates no additional fold.
  Departed seats keep their chips and completed statistics, but receive no
  further cards, blinds or turns.
- The whole game may be stopped at any time by closing the window. There is no
  saved unfinished-match feature. Starting again abandons the previous match and
  starts everyone at the initial stack; it does not resume that match. The desktop
  Profiles screen creates local password accounts and signs in 1–6 humans before
  starting. Profile-bound matches retain completed lifetime statistics. Removing
  an account from the next-match roster leaves the ongoing match unchanged.
- The match carries stacks between settled hands and eliminates zero-chip
  players from further deals. A match winner must own all 6,000 chips after
  settlement. An already-all-in quitter who wins the full total still wins the
  match, and a completed winner keeps that result after leaving. With at least
  two funded seats that have not left, Next hand rotates the dealer and deals
  again. With fewer than two such seats and no full-total owner, play ends
  without a match winner. Departed stacks stay with their owners. Settle hand
  pays the current hand before the match outcome is determined. Tests using
  other valid settings require the full configured chip total for victory.

## Deal and betting order

Use a standard 52-card deck without jokers. Shuffle a fresh deck and deal two
private cards per player, one at a time clockwise after the dealer in two rounds.
With six players, 12 cards are dealt and 40 remain. Eliminated seats get no cards. Select the first dealer
randomly. The dealer moves clockwise among remaining players on later hands. Heads-up,
the dealer is the small blind and acts first preflop. The small and big blinds
are otherwise the next two seats clockwise; the first
preflop actor follows the big blind. After the flop, action starts with the
first eligible seat after the dealer.

Each betting round requires all eligible decisions, including the big blind's
option to check when nobody raises. Skip folded, all-in and eliminated players.
Deal the flop (three cards), turn (one), and river (one), burning one card before
each street. Players use their private cards and the five shared cards.

The intended full rules allow folding, checking when owing zero, calling,
betting and raising subject to stack and minimum-raise rules. The current
engine supports FOLD/CHECK/CALL and full BET/RAISE on every betting street.
BET opens a postflop round for at least the big blind. RAISE specifies the total
round commitment, and its increase must be at least the previous full increment.
The engine deducts only the difference from the actor's existing contribution;
the target cannot exceed that contribution plus their remaining chips.
A short stack can call all-in for its remaining chips. Blinds are capped at the
available stack. With multiple active opponents, a short big blind still leaves
the normal minimum call; a lone active player need only match the opposing
all-in amount. Once that player is matched, run out the board without another
decision. A bet/raise below the full minimum is legal only as an exact all-in
when the player has raising rights. Short wagers retain the last full increment.
A player who already acted can raise again once cumulative increases since their
own last decision reach that increment; players who have not acted keep rights.
Calling an intermediate increase resets that caller's level. Checking before a
short opening does not reopen rights until a full cumulative bet is faced.
These follow [Poker TDA rules 45/49](https://www.pokertda.com/view-poker-tda-rules/).
Swing shows the engine's capped call cost and legal bet or raise targets.
Enter the round's total contribution target to bet or raise.
Out-of-turn or invalid actions preserve chips, cards, actor and accepted-action history.

## Hand result and chips

Compare the best five of seven cards among eligible players. From strongest to
weakest: straight flush, four of a kind, full house, flush, straight, three of
a kind, two pair, one pair, high card. Compare category ranks and kickers;
an ace may be low in A–2–3–4–5. Equal best hands tie. Folded players cannot win;
all-in players remain eligible. If everyone else folds, the sole remaining
player wins uncontested. If further betting is impossible, finish the board
without inventing player actions.

Comparison returns winner(s) with **AWAITING_SETTLEMENT**. Choose **Settle hand**
to distribute the pot, then **Next hand** to continue. Settlement forms separate
pots from contribution levels; folded chips stay in those pots, while only
eligible live contributors can win. Split tied pots separately, giving odd
chips clockwise after the dealer, and return uncalled excess as a refund.
These follow the [Poker TDA payout rules](https://www.pokertda.com/view-poker-tda-rules/).
Payouts conserve the 6,000-chip total; paid hands have zero pot and zero
commitments. A refund alone is not a contested showdown win.

Accepted voluntary actions feed pending statistics once. Blinds, queries and
rejected actions do not create voluntary observations. Settlement completes each
player's hand statistics once; the Statistics tab shows accumulated match totals.
Next hand keeps those totals, while starting a new match resets them. Closing
the game discards the match. Backend profile-bound matches additionally retain
completed lifetime counters and completed-hand IDs locally, separately from
match totals; new matches do not reset lifetime data. Save failures leave hand
settlement retryable without paying chips or incrementing statistics twice.
No unfinished hand is serialized. The existing desktop Statistics tab still
shows match totals; Profiles displays the selected signed-in account's completed
lifetime statistics. Accounts reopen on this device after password verification.
