import test from 'node:test';
import assert from 'node:assert/strict';
import { handPositions, replayAt, runoutEvents, streetHistory } from '../src/hand-history.mjs';

test('rewinding an award and a voluntary show restores the public information at that step', () => {
  const cards = [{ rank: 'ACE', suit: 'CLUBS' }, { rank: 'KING', suit: 'SPADES' }];
  const events = [
    { kind: 'OPEN', stacks: { a: '9007199254740993', b: '20' }, dealer: 0, dealt: ['a','b'] },
    { kind: 'SMALL_BLIND', id: 'a', amount: '10' },
    { kind: 'BIG_BLIND', id: 'b', amount: '20' },
    { kind: 'FOLD', id: 'a', amount: '0' },
    { kind: 'SHOWDOWN' },
    { kind: 'AWARD', payouts: { a: '0', b: '20' }, refunds: { a: '0', b: '10' },
      categories: {}, pots: [{ b: '20' }] },
    { kind: 'VOLUNTARILY_SHOWN', id: 'a', cards },
    { kind: 'MUCKED', id: 'b' },
  ];
  assert.deepEqual(replayAt(events, 7).shown, { a: cards });
  assert.equal(replayAt(events, 7).statuses.a, 'FOLD');
  assert.equal(replayAt(events, 7).visibility.b, 'MUCKED');
  assert.deepEqual(replayAt(events, 5).shown, {});
  assert.equal(replayAt(events, 5).stacks.b, 30n);
  assert.equal(replayAt(events, 4).pot, 30n);
  assert.deepEqual(replayAt(events, 4).payouts, {});
  assert.deepEqual(replayAt(events, 4).pots, []);
  assert.deepEqual(replayAt(events, 7).pots, [{ b: '20' }]);
  assert.deepEqual(replayAt(events, 1).blinds, { a: 'SMALL_BLIND' });
  assert.equal(replayAt(events, 0).dealer, 0);
  assert.equal(replayAt(events, 0).stacks.a, 9007199254740993n);
});

test('every all-in replay step conserves chips and reveals only the board dealt so far', () => {
  const pockets = {
    a: [{rank:'ACE',suit:'HEARTS'},{rank:'ACE',suit:'CLUBS'}],
    b: [{rank:'KING',suit:'DIAMONDS'},{rank:'KING',suit:'SPADES'}],
    c: [{rank:'QUEEN',suit:'HEARTS'},{rank:'QUEEN',suit:'SPADES'}],
  };
  const board = [
    {rank:'SEVEN',suit:'CLUBS'}, {rank:'FOUR',suit:'SPADES'}, {rank:'TWO',suit:'DIAMONDS'},
    {rank:'EIGHT',suit:'HEARTS'}, {rank:'NINE',suit:'CLUBS'},
  ];
  const pots = [{a:'150'},{b:'100'}];
  const events = [
    {kind:'OPEN', dealer:0, stacks:{a:'50',b:'100',c:'150'}, dealt:['a','b','c']},
    {kind:'SMALL_BLIND', id:'b', amount:'10'}, {kind:'BIG_BLIND', id:'c', amount:'20'},
    {kind:'RAISE', id:'a', amount:'50', target:'50', allIn:true},
    {kind:'RAISE', id:'b', amount:'90', target:'100', allIn:true},
    {kind:'CALL', id:'c', amount:'80', target:'100'},
    ...Object.entries(pockets).map(([id,cards]) => ({kind:'SHOWDOWN_REVEALED',id,cards})),
    ...['FLOP','TURN','RIVER'].map((street,i) => ({kind:'DEAL',street,board:board.slice(0,i+3)})),
    {kind:'SHOWDOWN', street:'SHOWDOWN'},
    {kind:'AWARD', street:'SHOWDOWN', pots, payouts:{a:'150',b:'100',c:'0'},
      refunds:{a:'0',b:'0',c:'0'}, categories:{a:'ONE_PAIR',b:'ONE_PAIR'}},
  ].map(event => ({street:'PREFLOP', ...event}));
  for (let index = 0; index < events.length; index++) {
    const frame = replayAt(events,index);
    assert.equal(Object.values(frame.stacks).reduce((sum,chips) => sum + chips, frame.pot),300n);
  }
  assert.equal(replayAt(events,3).pot,80n);
  assert.deepEqual(replayAt(events,5).shown,{});
  assert.deepEqual(replayAt(events,6).shown,{a:pockets.a});
  assert.deepEqual(replayAt(events,8).board,[]);
  assert.deepEqual(replayAt(events,8).shown,pockets);
  assert.deepEqual([9,10,11].map(i => replayAt(events,i).board.length),[3,4,5]);
  assert.deepEqual(replayAt(events,12).categories,{});
  assert.deepEqual(replayAt(events,13).pots,pots);
  assert.deepEqual(replayAt(events,13).stacks,{a:150n,b:100n,c:50n});
});

test('inline history groups blinds and preserves the pot at each street boundary', () => {
  const events = [
    {kind:'OPEN'}, {kind:'SMALL_BLIND',amount:'10'}, {kind:'BIG_BLIND',amount:'20'},
    {street:'PREFLOP',kind:'CALL',amount:'10'}, {street:'FLOP',kind:'DEAL',board:[]},
    {street:'FLOP',kind:'BET',amount:'40'}, {street:'FLOP',kind:'FOLD',amount:'0'},
    {street:'SHOWDOWN',kind:'AWARD',pots:[{a:'40'}],refunds:{a:'40'}},
    {street:'SHOWDOWN',kind:'VOLUNTARILY_SHOWN',cards:[]}
  ];
  const streets = streetHistory(events);
  assert.deepEqual(streets.flatMap(group => group.events),events);
  assert.deepEqual(streets.map(group => [group.street,group.pot]),
    [['blinds',30n],['PREFLOP',40n],['FLOP',80n],['SHOWDOWN',0n]]);
});

test('action positions rotate around occupied chairs, including heads-up', () => {
  const players = [0,1,2,3,4,5].map(seat => ({seat,id:String(seat)}));
  assert.deepEqual(handPositions(players,{dealer:4,dealt:['1','4']}),{'1':'BB','4':'BTN/SB'});
  assert.deepEqual(handPositions(players,{dealer:4,dealt:players.map(p => p.id)}),
    {'0':'BB','1':'UTG','2':'HJ','3':'CO','4':'BTN','5':'SB'});
});

test('a deferred folded show cannot enter the live runout presentation early', () => {
  const events = [{kind:'SHOWDOWN_REVEALED'}, {kind:'DEAL',board:[1,2,3]},
    {kind:'DEAL',board:[1,2,3,4]}, {kind:'DEAL',board:[1,2,3,4,5]},
    {kind:'SHOWDOWN'}, {kind:'VOLUNTARILY_SHOWN'}];
  for (const [count,size] of [0,3,4,5].entries()) {
    assert.deepEqual(runoutEvents(events,size),events.slice(0,count+1));
  }
});
