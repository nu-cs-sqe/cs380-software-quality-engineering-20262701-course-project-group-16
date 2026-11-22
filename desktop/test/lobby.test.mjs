import test from 'node:test';
import assert from 'node:assert/strict';
import * as lobby from '../src/lobby-model.mjs';

test('only ready local seats enter the hand; unready and computer seats stay out', () => {
  const seats = lobby.emptySeats();
  const account = {id: 'a', name: 'Alice', balance: '900', history: ['private'], metrics: {}};
  seats[0] = lobby.occupy(seats, 0, account);
  seats[3] = lobby.occupy(seats, 3, {id: 'b', name: 'Bob'});
  seats[3] = {...seats[3], buyIn: '400'};
  assert.deepEqual(lobby.readyPlayers(seats), [{id:'b', buyIn:'400'}]);
  assert.deepEqual(seats[0], {id:'a', name:'Alice', buyIn:''});
  assert.equal(seats.length, 6);
});

test('one account can unlock its own seat but cannot occupy a second', () => {
  const seats = lobby.emptySeats();
  const account = {id:'a', name:'Alice'};
  seats[0] = {...lobby.occupy(seats, 0, account), buyIn:'429'};
  assert.equal(lobby.occupy(seats, 0, account).buyIn, '429');
  assert.throws(() => lobby.occupy(seats, 5, account), /duplicate/);
});

test('validates whole-chip buy-ins exactly through the user’s available funds', () => {
  for (const amount of ['', '399', '-400', '400.1', '1e3', '+401', '501']) {
    assert.equal(lobby.validBuyIn(amount, '500'), false, amount);
  }
  assert.equal(lobby.validBuyIn('400', '400'), true);
  assert.equal(lobby.validBuyIn('500', '500'), true);
  assert.equal(lobby.validBuyIn('9007199254740993', '9007199254740993'), true);
  assert.equal(lobby.validBuyIn('9007199254740993', '9007199254740992'), false);
});

test('only unoccupied slots can be removed when changing the local player count', () => {
  const seats = lobby.emptySeats();
  assert.equal(lobby.minimumPlayerCount(seats), 1);
  seats[2] = lobby.occupy(seats, 2, {id:'a', name:'Alice'});
  assert.equal(lobby.minimumPlayerCount(seats), 3);
});
