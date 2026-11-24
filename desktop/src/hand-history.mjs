// Consume only the prefix selected by the viewer. Chip amounts cross the bridge as decimal strings.
export function replayAt(events, index) {
  const frame = { stacks: {}, pot: 0n, board: [], shown: {}, visibility: {}, statuses: {},
    payouts: {}, refunds: {}, categories: {}, pots: [], blinds: {}, dealt: [] };
  for (const event of events.slice(0, index + 1)) {
    frame.street = event.street;
    if (event.kind === 'OPEN') {
      frame.dealer = event.dealer;
      frame.dealt = event.dealt;
      for (const [id, chips] of Object.entries(event.stacks)) frame.stacks[id] = BigInt(chips);
    } else if (event.amount !== undefined) {
      const chips = BigInt(event.amount);
      frame.stacks[event.id] -= chips;
      frame.pot += chips;
      frame.statuses[event.id] = event.allIn ? 'ALL_IN' : event.kind;
      if (event.kind === 'SMALL_BLIND' || event.kind === 'BIG_BLIND') {
        frame.blinds[event.id] = event.kind;
      }
    } else if (event.kind === 'DEAL') {
      frame.board = event.board;
      for (const [id, state] of Object.entries(frame.statuses)) {
        if (state !== 'FOLD' && state !== 'ALL_IN') delete frame.statuses[id];
      }
    } else if (event.kind === 'AWARD') {
      frame.payouts = event.payouts;
      frame.refunds = event.refunds;
      frame.categories = { ...event.categories };
      frame.pots = event.pots;
      for (const id of Object.keys(frame.stacks)) {
        const chips = BigInt(event.payouts[id]) + BigInt(event.refunds[id]);
        frame.stacks[id] += chips;
        frame.pot -= chips;
      }
    } else if (event.cards || event.kind === 'MUCKED') {
      frame.visibility[event.id] = event.kind;
      if (event.cards) frame.shown[event.id] = event.cards;
      if (event.category) frame.categories[event.id] = event.category;
    }
  }
  return frame;
}

// Group adjacent events only: an all-in exposure can precede the remaining streets.
export function streetHistory(events) {
  const groups = [];
  let pot = 0n;
  for (const event of events) {
    const street = ['OPEN', 'SMALL_BLIND', 'BIG_BLIND'].includes(event.kind) ? 'blinds' : event.street;
    if (groups.at(-1)?.street !== street) groups.push({street, events:[], pot});
    const group = groups.at(-1);
    if (event.amount !== undefined) pot += BigInt(event.amount);
    group.events.push(event);
    if (event.kind === 'AWARD') pot = 0n;
    group.pot = pot;
  }
  return groups;
}

export function runoutEvents(events, boardSize) {
  const end = events.findIndex(event => (event.kind === 'DEAL' && event.board.length > boardSize)
    || event.kind === 'SHOWDOWN');
  return end < 0 ? events : events.slice(0,end);
}

// Positions follow dealt seats clockwise. A player's physical chair never changes.
export function handPositions(players, opening) {
  if (!opening) return {};
  const dealt = players.filter(p => opening.dealt.includes(p.id))
    .sort((a,b) => a.seat - b.seat);
  const button = dealt.findIndex(p => p.seat === opening.dealer);
  const order = [...dealt.slice(button), ...dealt.slice(0,button)];
  const names = {
    2: ['BTN/SB','BB'], 3: ['BTN','SB','BB'], 4: ['BTN','SB','BB','CO'],
    5: ['BTN','SB','BB','HJ','CO'], 6: ['BTN','SB','BB','UTG','HJ','CO'],
  }[order.length] || [];
  return Object.fromEntries(order.map((player,i) => [player.id,names[i]]));
}
