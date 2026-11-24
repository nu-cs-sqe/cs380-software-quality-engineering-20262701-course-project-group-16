export const SEAT_COUNT = 6;
export const MINIMUM_BUY_IN = 400n;
const DEFAULT_BUY_IN = 1000n;

export function emptySeats() {
  return Array.from({length: SEAT_COUNT}, () => ({id: '', name: '', buyIn: ''}));
}

export function occupy(seats, slot, {id, name}) {
  if (seats.some((seat, index) => seat.id === id && index !== slot)) {
    throw new Error('duplicate');
  }
  return {id, name, buyIn: seats[slot].buyIn};
}

export function readyPlayers(seats, positions = seats.map((_, index) => index)) {
  return seats.flatMap(({id, buyIn}, slot) => id && buyIn ? [{id, buyIn, seat:positions[slot]}] : []);
}

export function choosePosition(positions, seats, slot, position) {
  const previous = positions.indexOf(position);
  if (previous < 0 || (previous !== slot && seats[previous].id)) return positions;
  const next = [...positions];
  next[previous] = positions[slot];
  next[slot] = position;
  return next;
}

export function validBuyIn(amount, balance) {
  if (!/^\d+$/.test(amount)) return false;
  const chips = BigInt(amount);
  return chips >= MINIMUM_BUY_IN && chips <= BigInt(balance);
}

export function openingBuyIn(balance) {
  const funds = BigInt(balance);
  return (funds < MINIMUM_BUY_IN ? MINIMUM_BUY_IN : funds < DEFAULT_BUY_IN ? funds : DEFAULT_BUY_IN).toString();
}

export function minimumPlayerCount(seats) {
  return Math.max(1, seats.findLastIndex(seat => seat.id) + 1);
}

export { formatChips } from './locales.mjs';

export function availableProfiles(profiles, seats) {
  const names = new Set();
  const repeated = new Set();
  for (const {name} of profiles) {
    if (names.has(name)) repeated.add(name);
    names.add(name);
  }
  return profiles.filter(profile => !seats.some(seat => seat.id === profile.id))
    .map(({id, name}) => ({id, label: repeated.has(name) ? name + ' · ' + id : name}));
}

export function nextUnreadySeat(seats, count, current) {
  for (let offset = 1; offset < count; offset++) {
    const index = (current + offset) % count;
    if (!seats[index].buyIn) return index;
  }
  return current;
}
