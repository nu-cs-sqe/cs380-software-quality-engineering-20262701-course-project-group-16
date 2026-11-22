export function handoffGate() {
  let generation = 0;
  return {
    begin({ handId, revision, actor }) {
      return { handId, revision, actor, generation: ++generation };
    },
    conceal() {
      generation++;
    },
    accepts(ticket, snapshot) {
      return ticket.generation === generation && ticket.handId === snapshot.handId
        && ticket.revision === snapshot.revision && ticket.actor === snapshot.actor;
    },
  };
}

export function publicSnapshot(snapshot) {
  return { ...snapshot, hole: [] };
}

export function validTarget(value, min, max) {
  if (!/^\d+$/.test(value)) return false;
  const target = BigInt(value);
  return target >= BigInt(min) && target <= BigInt(max);
}

export function chipText(value, lang) {
  return BigInt(value).toLocaleString(lang === 'zh' ? 'zh-CN' : 'en-US');
}

export function seatedNames(game, lang) {
  const localized = game.players.map(p => p.type === 'BOT' && lang === 'zh'
    ? { ...p, name: '电脑 ' + (p.seat + 1) } : p);
  const players = localized.map(player => {
    if (!localized.some(other => other.id !== player.id && other.name === player.name)) return player;
    const seat = (lang === 'zh' ? '座位 ' : 'Seat ') + (player.seat + 1);
    return { ...player, name: player.name + ' (' + seat + ')' };
  });
  const action = game.lastAction;
  const lastAction = action && { ...action, name: players.find(p => p.id === action.playerId)?.name ?? action.name };
  return { ...game, players, lastAction };
}
