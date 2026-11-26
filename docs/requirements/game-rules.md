# Game rules

This project uses [Texas Hold'em rules](https://www.pokerstars.com/poker/learn/lesson/texas-holdem-rules/)
with the match settings below. The intended full game is no-limit Hold'em;
the current engine support and desktop controls are described separately below.
All chips are virtual and have no cash value.

## Fixed-stack Swing setup

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
- Stacks carry between settled hands. Zero-chip and departed seats sit out of
  future deals, and departed stacks stay with their owners. After settlement,
  Next hand rotates the dealer and deals when at least two funded seats remain.

## Account tables: Week 8 desktop rules

River Room uses funded account tables with persistent chip changes from each
settled hand.

- Each human signs in to a local account. Storage supplies the initial 2,000
  virtual chips. Players choose a buy-in of at least 400, up to their saved
  balance, with no fixed upper cap. The desktop lets each signed-in account
  choose an amount in the roster and defaults to min(1,000, balance) when blank.
- Six seats remain. Bots fill unused seats. Between paid hands, departed humans
  and busted bots are replaced with separately funded 1,000-chip bots.
- A new hand requires at least two funded seats, including one human. If all
  remaining humans are out of table chips, they can top up or end the paid table.
  A table with no remaining humans can be ended.
- Human chips carry between hands, including winnings above any starting amount.
  A stack reduced below 400 can keep playing. Between-hand top-ups use existing
  account funds and must leave at least 400 on the table; there is no fixed upper
  cap. A busted account with less than 400 needs recovery before rebuying.
  A signed-in account below 400 can earn 200 chips for four correct answers
  out of five, up to three successful rewards per local calendar day.
  A seated account can claim only between paid hands; practice is always available.
- Account totals include table chips. Funding reserves existing money; each paid
  hand changes the total only by final stack minus the stack before blinds.
  Save every affected human's balance, history and statistics atomically, once.
- Leaving retains account ownership and the hand's all-in eligibility. Matched
  money from departed contributors remains awardable to surviving eligible hands.
  Only uncalled excess is refunded. Departures cannot block settlement.
- Play continues while the table can fund the next hand. End the table between
  settled hands to release reservations. Closing discards the unfinished hand.

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
Payouts conserve the configured funded total; paid hands have zero pot and zero
commitments. A refund alone is not a contested showdown win.

Accepted voluntary actions feed pending statistics once. Blinds, queries and
rejected actions do not create voluntary observations. Settlement completes each
player's hand statistics once; the Statistics tab shows accumulated match totals.
Next hand keeps those totals, while starting a new match resets them. Closing
the game discards the match. Backend profile-bound matches additionally retain
completed lifetime counters and completed-hand IDs locally, separately from
match totals; new matches do not reset lifetime data. Save failures leave hand
settlement retryable without paying chips or incrementing statistics twice.
During the profile save the table stays responsive, but hand progression and
account actions are disabled. The payout appears after the save succeeds. If
saving fails, fix the storage problem and choose **Settle hand** again.
No unfinished hand is serialized. The existing desktop Statistics tab still
shows match totals; Profiles displays the selected signed-in account's completed
lifetime statistics. Accounts reopen on this device after password verification.

## River Room showdown and public hand history

A fold immediately gives up every pot claim. Both cards stay hidden by default.
A folded owner can privately inspect them and choose to show both or muck. By
default the table publishes a voluntary show after the hand concludes. The host
can enable immediate folded-card exposure before starting a home game. An
uncontested winner may keep both cards hidden, show them, or explicitly muck
after winning. None of these choices changes that payout.

River Room defaults to tabling all live hands at an ordinary contested river.
The lobby also offers an ordered show/muck house rule. In either mode the
last river bettor or raiser shows first.
After a checked river, start with the first live seat clockwise after the dealer.
Continue clockwise. Under optional mucking, showing both cards claims the pot, including when playing
the board. Mucking withdraws the claim. If everyone else mucks before anyone
tables a hand, the last claimant wins without exposing either card. Once all
betting decisions are complete in a contested all-in hand, expose both cards of
every live participant before dealing any remaining streets. This includes a
short-stack all-in when the deeper stacks have finished their side-pot betting.
The order and all-in policy follow [Poker TDA rules 16–18](https://www.pokertda.com/view-poker-tda-rules/).

By default bots keep folded and uncontested hands private. Under optional
river mucking a bot tables against a public hand it can beat or tie and mucks when
beaten. The lobby's optional show policy tells bots to show whenever permitted.
Mandatory all-in exposure applies to every live human and bot.

River Room saves the chip settlement before offering the public replay. The
timeline starts before the blinds and records committed actions, board cards,
disclosures, the actual shares of each pot and uncalled refunds. The winner's
category appears when the hand has been shown and at least five cards are
available. Private inspections add no public cards. The open table retains all
its completed replays. Closing the table discards them. Completed account
statistics and chip transactions retain their existing persistence.

River Room binds authenticated accounts to the physical chairs selected before
the opening deal. Empty lobby chairs become bots when play starts. The first
dealer is random, with later positions following the occupied seats clockwise.
The six fixed chairs keep their occupants throughout the table, subject to the
existing departure and replacement rules. The lobby and history display chairs
1–6, corresponding to engine indices 0–5.

The completed table shows paid stacks, individual awards and the best public
five for a selected winner. Results and replay are read-only views of that
same hand. Opening them cannot pay chips or deal cards. A folded owner may
change a private muck to a voluntary show until the next deal. A showdown muck
that withdrew a pot claim stays final. A public disclosure is permanent.
