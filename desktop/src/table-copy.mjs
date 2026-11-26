import { localeCatalog } from './locales.mjs';

export const tableCopy = Object.fromEntries(localeCatalog.available.map(locale =>
  [locale.id, locale.table]));
export const metrics = { vpip: "VPIP", pfr: "PFR", gap: "GAP", threeBet: "3-Bet", af: "AF",
  afq: "AFq", cBet: "C-Bet", foldCBet: "F/C-Bet", wtsd: "WTSD", wsd: "W$SD" };
