import assert from 'node:assert/strict';
import test from 'node:test';
import { mkdtemp, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { _electron as electron } from 'playwright';

const desktop = fileURLToPath(new URL('../', import.meta.url));
const password = 'river-seat-24';
const button = (page, name) => page.getByRole('button', {name, exact:true});

async function open(data) {
  const app = await electron.launch({
    executablePath:process.env.RIVER_ROOM_EXECUTABLE,
    args:[...(process.env.RIVER_ROOM_EXECUTABLE ? [] : [desktop]), '--user-data-dir=' + path.join(data,'window')],
    env:{...process.env, RIVER_ROOM_DATA:data, JAVA_TOOL_OPTIONS:'-Dcs380.profileDirectory=' + data}, timeout:30000
  });
  const page = await app.firstWindow();
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows()[0].focus());
  await page.getByRole('heading',{name:'Table lobby'}).waitFor();
  return {app, page, errors};
}

async function join(page, seat, name, amount) {
  await page.getByRole('button',{name:new RegExp('Player ' + seat + '.*Open slot')}).click();
  const form = page.locator('.rr-account');
  await form.getByRole('group').getByRole('button',{name:'Create account'}).click();
  await form.getByLabel('Display name').fill(name);
  await form.getByLabel('Password',{exact:true}).fill(password);
  await form.locator('form button[type=submit]').click();
  await form.getByLabel('Opening buy-in').fill(amount);
  await button(form,'Ready to play').click();
}

async function startTable(page, names) {
  await button(page,String(names.length)).click();
  for (const [index,name] of names.entries()) await join(page,index + 1,name,'500');
  await button(page,'Start game').click();
  await page.getByTestId('poker-table').waitFor();
}

test('six shared seats play a hand, hide private cards and keep the paid result after restart',
  {timeout:150000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-spec-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      await button(page,'中文').click();
      assert.equal(await page.locator('html').getAttribute('lang'),'zh-CN');
      await button(page,'English').click();
      const names = ['Alice','Bryn','Chris','Dani','Emery','Finley'];
      await startTable(page,names);
      const hole = page.getByTestId('private-hand');
      const reveal = page.getByRole('button',{name:/Reveal hand/, disabled:false});
      for (let turn=0; turn<5; turn++) {
        await reveal.waitFor();
        assert.equal(await hole.count(),0);
        await reveal.click();
        await hole.waitFor();
        if (turn === 0) {
          await room.app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows()[0].blur());
          await hole.waitFor({state:'detached'});
          await room.app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows()[0].focus());
          await reveal.click();
          await hole.waitFor();
        }
        await button(page,'Fold').click();
        await hole.waitFor({state:'detached'});
      }
      await button(page,'Session statistics').click();
      assert.equal(await page.getByRole('columnheader',{name:'VPIP'}).count(),1);
      const state = await page.evaluate(() => window.poker.request('state'));
      assert.equal(state.paid,true);
      assert.deepEqual(state.hole,[]);
      const accounts = state.players.map(player => player.accountId);
      const balances = Object.values(state.funds).map(funds=>BigInt(funds.balance));
      assert.equal(balances.reduce((sum,value)=>sum+value,0n),12000n);
      await button(page,'Table options').click();
      const dialog = page.getByRole('dialog');
      await dialog.getByRole('button',{name:/Alice/}).click();
      await button(dialog,'Continue as Alice').click();
      await dialog.getByLabel('Chips to add').fill('25');
      await button(dialog,'Add chips').click();
      await dialog.waitFor({state:'detached'});
      const funded = await page.evaluate(() => window.poker.request('state'));
      assert.equal(BigInt(funded.funds[accounts[0]].tableChips),
        BigInt(state.funds[accounts[0]].tableChips) + 25n);
      assert.equal(funded.funds[accounts[0]].balance,state.funds[accounts[0]].balance);
      await button(page,'Deal next hand').click();
      await reveal.waitFor();
      const second = await page.evaluate(() => window.poker.request('state'));
      assert.equal(second.handNumber,2);
      assert.notEqual(second.handId,state.handId);
      assert.deepEqual(room.errors,[]);
      await room.app.close();
      room = await open(data);
      const account = await room.page.evaluate(({id,password}) =>
        window.poker.request('login',{id,password}),{id:accounts[0],password});
      assert.equal(account.history.filter(row=>row.type==='HAND_SETTLEMENT').length,1);
      assert.equal(account.balance,state.funds[accounts[0]].balance);
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });

test('unready local seats are filled by bots and the game reaches settlement',
  {timeout:150000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-bots-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      await button(page,'2').click();
      await join(page,1,'Robin','400');
      await button(page,'Start game').click();
      await page.getByTestId('poker-table').waitFor();
      const first = await page.evaluate(() => window.poker.request('state'));
      assert.equal(first.players.filter(p=>p.type === 'BOT').length,5);
      const paid = button(page,'Session statistics');
      const reveal = page.getByRole('button',{name:/Reveal hand/, disabled:false});
      for (let step=0; step<8; step++) {
        await paid.or(reveal).first().waitFor({timeout:60000});
        if (await paid.isVisible()) break;
        await reveal.click();
        await button(page,'Fold').click();
      }
      await button(page,'Deal next hand').waitFor();
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });

test('a raised six-player hand reaches showdown, saves the pot and returns to the lobby',
  {timeout:150000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-showdown-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      await startTable(page,['Gale','Hayden','Indy','Jess','Kai','Lou']);
      const reveal = page.getByRole('button',{name:/Reveal hand/, disabled:false});
      const paid = button(page,'Hand results');
      const streets = new Set();
      for (let turn=0; turn<32; turn++) {
        await paid.or(reveal).first().waitFor();
        if (await paid.isVisible()) break;
        await reveal.click();
        await page.getByTestId('private-hand').waitFor();
        const game = await page.evaluate(() => window.poker.request('state'));
        const first = !streets.has(game.phase);
        streets.add(game.phase);
        if (first && game.phase === 'PREFLOP') {
          await page.getByLabel('Raise to').fill('40');
          await button(page,'Raise').click();
        } else if (first && game.phase === 'FLOP') {
          await page.getByLabel('Bet amount').fill('20');
          await button(page,'Bet').click();
        } else if (await button(page,'Check').isVisible()) {
          await button(page,'Check').click();
        } else {
          await page.getByRole('button',{name:/^Call /}).click();
        }
        await page.getByTestId('private-hand').waitFor({state:'detached'});
      }
      await paid.waitFor();
      const result = await page.evaluate(() => window.poker.request('state'));
      assert.deepEqual([...streets],['PREFLOP','FLOP','TURN','RIVER']);
      assert.equal(result.board.length,5);
      assert.equal(result.paid,true);
      assert.ok(result.result.winners.length > 0);
      assert.equal(Object.values(result.funds).reduce((total,fund) => total+BigInt(fund.balance),0n),12000n);
      await button(page,'中文').click();
      await button(page,'本场统计').click();
      assert.equal(await page.getByRole('columnheader',{name:'手数'}).count(),1);
      await button(page,'English').click();
      await button(page,'End session').click();
      await button(page,'Return to lobby').click();
      await page.getByRole('heading',{name:'Table lobby'}).waitFor();
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });
