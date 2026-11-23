import test from 'node:test';
import assert from 'node:assert/strict';
import {
  handoffGate, validTarget, publicSnapshot, chipText, seatedNames, needsSettlement, wagerPreview,
  continuationNotice,
} from '../src/table-state.mjs';

const turn = { handId: 'hand-7', revision: 2, actor: 3 };
const hole = [{ rank: 'ACE', suit: 'HEARTS' }, { rank: 'KING', suit: 'SPADES' }];

test('a late reveal cannot restore private cards after focus loss or an action', () => {
  const gate = handoffGate();
  const ticket = gate.begin(turn);
  gate.conceal();
  assert.equal(gate.accepts(ticket, turn), false);
  const next = gate.begin(turn);
  assert.equal(gate.accepts(next, turn), true);
  assert.equal(gate.accepts(ticket, turn), false);
});

test('a response for another hand, revision or actor cannot reveal cards', () => {
  const gate = handoffGate();
  const ticket = gate.begin(turn);
  for (const changed of [{ handId: 'hand-8' }, { revision: 3 }, { actor: 4 }]) {
    assert.equal(gate.accepts(ticket, { ...turn, ...changed }), false);
  }
});

test('public snapshots discard the private hand', () => {
  const state = { ...turn, hole, pot: '100' };
  assert.deepEqual(publicSnapshot(state), { ...turn, hole: [], pot: '100' });
  assert.equal(state.hole.length, 2);
});

test('raise-to validates inclusive integer boundaries beyond Number precision', () => {
  const min = '9007199254740993';
  const max = '9007199254740995';
  for (const good of [min, '9007199254740994', max]) assert.equal(validTarget(good, min, max), true);
  for (const bad of ['9007199254740992', '9007199254740996', '', '-1', '1.5', '1e18', ' 42']) {
    assert.equal(validTarget(bad, min, max), false);
  }
  assert.equal(chipText(max, 'en'), '9,007,199,254,740,995');
  assert.equal(chipText('-1520', 'zh'), '-1,520');
});

test('duplicate display names are disambiguated by physical seat, including public actions', () => {
  const game = { players: [
    { id: 'a', type: 'HUMAN', seat: 0, name: 'Alex' },
    { id: 'b', type: 'HUMAN', seat: 2, name: 'Alex' },
    { id: 'c', type: 'BOT', seat: 1, name: 'Bot 2' },
  ], lastAction: { playerId: 'b', name: 'Alex' } };
  const view = seatedNames(game, 'en');
  assert.deepEqual(view.players.map(p => p.name), ['Alex (Seat 1)', 'Alex (Seat 3)', 'Bot 2']);
  assert.equal(view.lastAction.name, 'Alex (Seat 3)');
  assert.equal(seatedNames(game, 'zh').players[2].name, '电脑 2');
  assert.equal(game.players[0].name, 'Alex');
});

test('results trigger one automatic settlement per hand, including across state refreshes', () => {
  const finished = { handId: 'one', paid: false, result: { winners: ['p1'] } };
  assert.equal(needsSettlement({ ...finished, result: '' }, ''), false);
  assert.equal(needsSettlement(finished, ''), true);
  assert.equal(needsSettlement({ ...finished, revision: 7 }, 'one'), false);
  assert.equal(needsSettlement({ ...finished, paid: true }, ''), false);
  assert.equal(needsSettlement({ ...finished, handId: 'two' }, 'one'), true);
});

test('a blocked next hand explains whether humans have left or need funding', () => {
  const player = { accountId: 'p1', type: 'HUMAN', status: 'ELIMINATED' };
  const bots = [{ type: 'BOT', status: 'ACTIVE' }];
  const game = { canNext: false, players: [player, ...bots], funds: {} };
  assert.equal(continuationNotice({ ...game,
    players: [{ ...player, status: 'DEPARTED' }, ...bots],
    funds: { p1: { needsRecovery: true } }
  }), 'noHumans');
  assert.equal(continuationNotice({
    ...game, funds: { p1: { needsTopUp: true } }
  }), 'fundNextHand');
  assert.equal(continuationNotice({
    ...game, funds: { p1: { needsRecovery: true } }
  }), 'fundNextHand');
  assert.equal(continuationNotice(game), 'cannotNext');
  assert.equal(continuationNotice({ ...game, canNext: true }), undefined);
});

test('a raise previews only the extra chips and handles all-ins and oversized integers', () => {
  const limits = {min:'40', max:'500'};
  assert.deepEqual(wagerPreview('60', limits, '10', '490'), {cost:'50', remaining:'440'});
  assert.deepEqual(wagerPreview('500', limits, '10', '490'), {cost:'490', remaining:'0'});
  assert.equal(wagerPreview('', limits, '10', '490'), undefined);
  assert.equal(wagerPreview('501', limits, '10', '490'), undefined);
  const max = '9007199254740993';
  assert.deepEqual(wagerPreview(max, {min:max,max}, '1', max),
    {cost:'9007199254740992', remaining:'1'});
});
