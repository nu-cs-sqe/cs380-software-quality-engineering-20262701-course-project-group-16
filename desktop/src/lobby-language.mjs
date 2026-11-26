import { localeCatalog } from './locales.mjs';

export const lobbyLanguage = Object.fromEntries(localeCatalog.available.map(locale =>
  [locale.id, locale.lobby]));
