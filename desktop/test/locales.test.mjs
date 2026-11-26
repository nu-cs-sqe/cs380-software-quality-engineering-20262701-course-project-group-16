import assert from 'node:assert/strict';
import test from 'node:test';
import resources from '../src/locale-resources.json' with { type: 'json' };
import { createLocaleCatalog, localeCatalog } from '../src/locales.mjs';

test('the configured startup locales expose their native labels and message formats', () => {
  assert.equal(localeCatalog.get().tag, 'en');
  const english = localeCatalog.get('en');
  const chinese = localeCatalog.get('zh');
  assert.equal(english.label, 'English');
  assert.equal(chinese.label, '中文');
  assert.equal(chinese.tag, 'zh-CN');
  assert.equal(english.lobby.readyCount(2, 1), '2 ready · 1 computer');
  assert.equal(english.lobby.readyCount(2, 4), '2 ready · 4 computers');
  assert.equal(chinese.table.card('A', '黑桃'), '黑桃A');
});

test('a locale added through resources joins the selector and formats its own messages', () => {
  const french = {
    ...resources.locales.en,
    tag: 'fr-FR',
    label: 'Français',
    table: { ...resources.locales.en.table, card: '{0} de {1}' },
  };
  const catalog = createLocaleCatalog({
    ...resources, locales: { ...resources.locales, fr: french },
  });
  assert.ok(catalog.available.some(locale => locale.id === 'fr' && locale.label === 'Français'));
  assert.equal(catalog.get('fr').tag, 'fr-FR');
  assert.equal(catalog.get('fr').table.card('A', 'pique'), 'A de pique');
  assert.equal(catalog.get('unknown').id, resources.defaultLocale);
});

test('the English template includes every table validation message for new locales', () => {
  const operations = ['act', 'bot', 'disclose', 'end', 'handoff', 'hide', 'next', 'quit',
    'reveal', 'state', 'switchOwner', 'topup'];
  for (const locale of localeCatalog.available) {
    assert.deepEqual(Object.keys(locale.table.errors ?? {}).sort(), operations);
    assert.ok(operations.every(operation => locale.table.errors[operation].trim().length > 0));
  }
});
