import resources from './locale-resources.json' with { type: 'json' };

function format(pattern, values) {
  return pattern.replace(/\{(\d+)\}/g, (_, index) => String(values[Number(index)]));
}

function compileMessages(messages, pluralRules) {
  return Object.fromEntries(Object.entries(messages).map(([key, message]) => {
    if (typeof message === 'string') {
      return [key, /\{\d+\}/.test(message) ? (...values) => format(message, values) : message];
    }
    if (Object.hasOwn(message, 'pluralArg')) {
      const { pluralArg, other } = message;
      return [key, (...values) => format(message[pluralRules.select(values[pluralArg])] ?? other,
        values)];
    }
    return [key, compileMessages(message, pluralRules)];
  }));
}

export function createLocaleCatalog({ defaultLocale, locales }) {
  const available = Object.entries(locales).map(([id, { tag, label, room, lobby, table }]) => {
    const plurals = new Intl.PluralRules(tag);
    return {
      id, tag, label, room,
      lobby: compileMessages(lobby, plurals),
      table: compileMessages(table, plurals),
    };
  });
  const byId = new Map(available.map(locale => [locale.id, locale]));
  if (!byId.has(defaultLocale)) throw new Error('Configure a default River Room locale.');
  return { available, get: id => byId.get(id) ?? byId.get(defaultLocale) };
}

export const DEFAULT_LOCALE = resources.defaultLocale;
export const localeCatalog = createLocaleCatalog(resources);

export function formatChips(value, lang) {
  return BigInt(value).toLocaleString(localeCatalog.get(lang).tag);
}
