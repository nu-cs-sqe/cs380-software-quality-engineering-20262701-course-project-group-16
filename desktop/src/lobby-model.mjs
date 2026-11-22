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

export function readyPlayers(seats) {
  return seats.filter(seat => seat.id && seat.buyIn).map(({id, buyIn}) => ({id, buyIn}));
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

export function formatChips(value, lang) {
  return BigInt(value).toLocaleString(lang === 'zh' ? 'zh-CN' : 'en-US');
}
