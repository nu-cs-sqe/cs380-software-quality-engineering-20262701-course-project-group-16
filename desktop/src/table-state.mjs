import { formatChips, localeCatalog } from './locales.mjs';

export function sameTurn(first, second) {
  return first.handId === second.handId && first.revision === second.revision
    && first.actor === second.actor;
}

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
      return ticket.generation === generation && sameTurn(ticket, snapshot);
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

export { formatChips as chipText };

export function seatLabel(status, streetAction, active, winner) {
  if (winner) return 'winner';
  if (active) return 'acting';
  if (status === 'ACTIVE' || status === 'FOLDED') return streetAction ?? status;
  return status;
}

export function seatedNames(game, lang) {
  const { table, lobby } = localeCatalog.get(lang);
  const localized = game.players.map(player => player.type === 'BOT'
    ? { ...player, name: table.botName(player.seat + 1) } : player);
  const players = localized.map(player => {
    if (!localized.some(other => other.id !== player.id && other.name === player.name)) return player;
    const seat = lobby.physicalSeat(player.seat + 1);
    return { ...player, name: player.name + ' (' + seat + ')' };
  });
  const action = game.lastAction;
  const lastAction = action && { ...action, name: players.find(p => p.id === action.playerId)?.name ?? action.name };
  return { ...game, players, lastAction };
}

export function needsSettlement(game, lastAttemptedHand) {
  return Boolean(game.result) && !game.paid && game.handId !== lastAttemptedHand;
}

export function remainingHumans(players) {
  return players.filter(player => player.type === 'HUMAN' && player.status !== 'DEPARTED');
}

export function continuationNotice(game) {
  if (game.canNext) return undefined;
  const humans = remainingHumans(game.players);
  if (!humans.length) return 'noHumans';
  const needsFunding = humans.some(player => {
    const funds = game.funds[player.accountId];
    return funds?.needsTopUp || funds?.needsRecovery;
  });
  return needsFunding ? 'fundNextHand' : 'cannotNext';
}

export function wagerPreview(target, limits, committed, stack) {
  if (!validTarget(target, limits.min, limits.max)) return undefined;
  const cost = BigInt(target) - BigInt(committed);
  return {cost: cost.toString(), remaining: (BigInt(stack) - cost).toString()};
}

const QUARTERS_PER_POT = 4n;
const POT_PRESETS = [
  { label: 'halfPot', quarters: 2n },
  { label: 'threeQuarterPot', quarters: 3n },
  { label: 'fullPot', quarters: QUARTERS_PER_POT },
];

export function wagerPresets(limits, pot, committed) {
  const call = BigInt(limits.call);
  const potAfterCall = BigInt(pot) + call;
  const calledTo = BigInt(committed) + call;
  const minimum = BigInt(limits.min);
  const allIn = BigInt(limits.max);
  const usedTargets = new Set([allIn]);
  const presets = [];
  for (const { label, quarters } of POT_PRESETS) {
    const target = calledTo + potAfterCall * quarters / QUARTERS_PER_POT;
    if (target >= minimum && target <= allIn && !usedTargets.has(target)) {
      presets.push({ label, target: String(target) });
      usedTargets.add(target);
    }
  }
  presets.push({ label: 'ALL_IN', target: limits.max });
  return presets;
}

// Native sliders use floating-point numbers. Keep chip amounts exact and limit
// only the number of slider positions; the text field accepts every legal chip.
const MAX_SLIDER_STEPS = 1000n;

function roundedQuotient(numerator, denominator) {
  return (numerator + denominator / 2n) / denominator;
}

export function wagerScale(min, max) {
  const minimum = BigInt(min);
  const span = BigInt(max) - minimum;
  const steps = span < MAX_SLIDER_STEPS ? span : MAX_SLIDER_STEPS;
  return {
    steps: Number(steps),
    targetAt(position) {
      if (steps === 0n) return min;
      const offset = roundedQuotient(BigInt(position) * span, steps);
      return String(minimum + offset);
    },
    positionOf(target) {
      if (span === 0n || !validTarget(target, min, max)) return 0;
      const offset = BigInt(target) - minimum;
      return Number(roundedQuotient(offset * steps, span));
    },
  };
}
