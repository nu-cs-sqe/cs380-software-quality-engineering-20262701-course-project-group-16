import assert from 'node:assert/strict';
import test from 'node:test';
import { DatabaseSync } from 'node:sqlite';
import { mkdtemp, rm } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { _electron as electron } from 'playwright';
import { lobbyLanguage } from '../src/lobby-language.mjs';
import { tableCopy } from '../src/table-copy.mjs';

const desktop = fileURLToPath(new URL('../', import.meta.url));
const password = 'river-seat-24';
const button = (page, name) => page.getByRole('button', {name, exact:true});
const recoveryAnswers = {
  blinds: 1, fold: 2, private: 0, board: 1, allIn: 2,
  refund: 0, sidePot: 1, tie: 2, quit: 0, topUp: 1,
};

async function open(data, timeZone = '') {
  const javaOptions = '-Dcs380.profileDirectory=' + data
    + (timeZone ? ' -Duser.timezone=' + timeZone : '');
  const app = await electron.launch({
    executablePath:process.env.RIVER_ROOM_EXECUTABLE,
    args:[...(process.env.RIVER_ROOM_EXECUTABLE ? [] : [desktop]), '--user-data-dir=' + path.join(data,'window')],
    env:{...process.env, RIVER_ROOM_DATA:data, JAVA_TOOL_OPTIONS:javaOptions}, timeout:30000
  });
  const page = await app.firstWindow();
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows()[0].focus());
  await page.getByRole('heading',{name:'Table lobby'}).waitFor();
  return {app, page, errors};
}

async function register(page, name, copy) {
  const form = page.locator('.rr-account');
  await button(form.getByRole('group'),copy.register).click();
  await form.getByLabel(copy.displayName).fill(name);
  await form.getByLabel(copy.password,{exact:true}).fill(password);
  await form.locator('form button[type=submit]').click();
  await form.locator('.rr-balance').waitFor();
}

async function ready(page, amount, copy) {
  const form = page.locator('.rr-account');
  await form.getByLabel(copy.buyIn).fill(amount);
  await button(form,copy.markReady).click();
  await form.locator('.rr-balance').waitFor({state:'detached'});
}

async function join(page, seat, name, amount, copy = lobbyLanguage.en) {
  await page.getByRole('button',{name:new RegExp(copy.slot(seat) + '.*' + copy.waiting)}).click();
  await register(page,name,copy);
  await ready(page,amount,copy);
}

async function startTable(page, names) {
  await button(page,String(names.length)).click();
  for (const [index,name] of names.entries()) await join(page,index + 1,name,'500');
  await button(page,'Start game').click();
  await page.getByTestId('poker-table').waitFor();
}

async function selectLanguage(page, lang) {
  if (lang === 'zh') await button(page,'中文').click();
  await page.getByRole('heading',{name:lobbyLanguage[lang].title}).waitFor();
  assert.equal(await page.locator('html').getAttribute('lang'),lang === 'zh' ? 'zh-CN' : 'en');
}

function withDatabase(data, task) {
  const database = new DatabaseSync(path.join(data,'profiles.db'));
  try {
    return task(database);
  } finally {
    database.close();
  }
}

async function signIn(page, id, copy) {
  const form = page.locator('.rr-account');
  await button(form.getByRole('group'),copy.signIn).click();
  await form.getByRole('combobox').selectOption(id);
  await form.getByLabel(copy.password,{exact:true}).fill(password);
  await button(form.locator('form'),copy.signIn).click();
  await form.locator('.rr-balance').waitFor();
}

async function lowBalanceAccount(page, data, name, copy) {
  await register(page,name,copy);
  await button(page,copy.leave).click();
  const id = withDatabase(data, database => {
    const account = database.prepare('SELECT id FROM profiles WHERE display_name=?').get(name);
    const update = database.prepare('UPDATE profiles SET chip_balance=250 WHERE id=?');
    assert.equal(update.run(account.id).changes,1);
    return account.id;
  });
  await signIn(page,id,copy);
  await expectBalance(page,250,copy);
  return id;
}

async function expectBalance(page, balance, copy) {
  const amount = page.locator('.rr-balance strong');
  assert.equal((await amount.innerText()).trim(),balance + ' ' + copy.chips);
}

async function answerRecoveryQuiz(page, copy, outcome) {
  const quiz = page.getByRole('region',{name:copy.recovery});
  const questions = quiz.locator('fieldset');
  await questions.nth(4).waitFor();
  assert.equal(await questions.count(),5);
  for (let index = 0; index < 5; index++) {
    const choices = questions.nth(index).getByRole('radio');
    const answer = recoveryAnswers[await choices.first().getAttribute('name')];
    assert.ok(Number.isInteger(answer));
    await choices.nth(index === 0 ? (answer + 1) % 3 : answer).check();
  }
  await button(quiz,copy.submit).click();
  await quiz.getByText(copy.results[outcome],{exact:true}).waitFor();
}

async function foldToSettlement(page, copy, lang) {
  const paid = button(page,copy.stats);
  const reveal = page.getByRole('button',{
    name:lang === 'zh' ? /查看手牌/ : /Reveal hand/, disabled:false
  });
  const hole = page.getByTestId('private-hand');
  for (let step = 0; step < 2; step++) {
    await paid.or(reveal).first().waitFor({timeout:60000});
    if (await paid.isVisible()) break;
    assert.equal(await hole.count(),0);
    await reveal.click();
    await hole.waitFor();
    await button(page,copy.FOLD).click();
    await hole.waitFor({state:'detached'});
  }
  await paid.waitFor({timeout:60000});
}

async function openSeatFunds(page, name, copy) {
  await button(page,copy.manage).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByRole('button',{name:new RegExp(name)}).click();
  await button(dialog,copy.authorize(name)).click();
  return dialog;
}

async function tableState(page) {
  return page.evaluate(() => window.poker.request('state'));
}

for (const lang of ['en','zh']) {
  test(`recovery funds a top-up and survives a packaged restart (${lang})`,
    {timeout:180000}, async () => {
      const data = await mkdtemp(path.join(os.tmpdir(),'river-room-recovery-'));
      const lobby = lobbyLanguage[lang];
      const table = tableCopy[lang];
      const name = 'Robin Recovery';
      let room;
      try {
        room = await open(data);
        const {page} = room;
        await selectLanguage(page,lang);
        await button(page,'2').click();
        const id = await lowBalanceAccount(page,data,name,lobby);
        await button(page,lobby.recoverLink).click();
        await answerRecoveryQuiz(page,lobby,'AWARDED');
        await button(page,lobby.back).click();
        await expectBalance(page,450,lobby);
        await button(page,lobby.history).click();
        const reward = page.locator('.rr-history article').filter({hasText:lobby.reward});
        await reward.getByText('+200',{exact:true}).waitFor();
        assert.equal(await reward.count(),1);
        await button(page,lobby.dashboard).click();
        await ready(page,'400',lobby);
        await join(page,2,'Sam', '500',lobby);
        await button(page,lobby.start).click();
        await page.getByTestId('poker-table').waitFor();
        await foldToSettlement(page,table,lang);

        const paid = (await tableState(page)).funds[id];
        assert.equal(paid.available,'50');
        let dialog = await openSeatFunds(page,name,table);
        await dialog.getByLabel(table.amount).fill('50');
        await button(dialog,table.topup).click();
        await dialog.waitFor({state:'detached'});
        const funded = (await tableState(page)).funds[id];
        assert.equal(funded.tableChips,String(BigInt(paid.tableChips) + 50n));
        assert.equal(funded.available,'0');
        assert.equal(funded.balance,paid.balance);

        dialog = await openSeatFunds(page,name,table);
        await button(dialog,table.leave).click();
        await button(dialog,table.leave).click();
        await dialog.waitFor({state:'detached'});
        const departed = (await tableState(page)).funds[id];
        assert.equal(departed.tableChips,'0');
        assert.equal(departed.available,paid.balance);
        await button(page,table.end).click();
        await button(page,table.paidReturn).click();
        await page.getByRole('heading',{name:lobby.title}).waitFor();
        assert.deepEqual(room.errors,[]);

        await room.app.close();
        room = await open(data);
        await selectLanguage(room.page,lang);
        await signIn(room.page,id,lobby);
        await expectBalance(room.page,paid.balance,lobby);
        await button(room.page,lobby.history).click();
        const history = room.page.locator('.rr-history article');
        await history.filter({hasText:lobby.reward}).getByText('+200',{exact:true}).waitFor();
        await history.getByText(lobby.hand,{exact:true}).waitFor();
        assert.equal(await history.count(),2);
        assert.deepEqual(room.errors,[]);
      } finally {
        if (room) await room.app.close().catch(()=>{});
        await rm(data,{recursive:true,force:true});
      }
    });
}

function zone(offset) {
  return 'GMT' + (offset < 0 ? '-' : '+') + Math.abs(offset) + ':00';
}

test('the packaged recovery quiz blocks a fourth reward and pays on the next local date',
  {timeout:90000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-daily-cap-'));
    const copy = lobbyLanguage.en;
    // Use test JVM zones 24 hours apart and leave at least an hour before local midnight.
    const now = new Date();
    const offset = now.getUTCHours() === 11 ? -13 : -12;
    const localDay = shift => new Date(now.getTime() + shift * 60 * 60 * 1000)
      .toISOString().slice(0,10);
    const today = localDay(offset);
    let room;
    try {
      room = await open(data,zone(offset));
      const id = await lowBalanceAccount(room.page,data,'Daily Cap',copy);
      withDatabase(data, database => {
        const seed = database.prepare('INSERT INTO recovery_claims VALUES (?, ?, ?, ?, 200, 450)');
        for (let index = 1; index <= 3; index++) {
          seed.run('fixture-' + index,id,today,now.toISOString());
        }
      });
      await button(room.page,copy.recoverLink).click();
      await answerRecoveryQuiz(room.page,copy,'DAILY_LIMIT');
      await button(room.page,copy.back).click();
      await expectBalance(room.page,250,copy);

      await room.app.close();
      room = await open(data,zone(offset + 24));
      await signIn(room.page,id,copy);
      await button(room.page,copy.recoverLink).click();
      await answerRecoveryQuiz(room.page,copy,'AWARDED');
      await button(room.page,copy.back).click();
      await expectBalance(room.page,450,copy);
      const dates = withDatabase(data, database => database.prepare(
        'SELECT local_date FROM recovery_claims WHERE profile_id=? ORDER BY local_date'
      ).all(id).map(row => row.local_date));
      assert.deepEqual(dates,[today,today,today,localDay(offset + 24)]);
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });

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
      await button(page,'Hand results').waitFor();
      assert.equal(await page.locator('.rr-arena').isVisible(),false);
      assert.equal(await page.locator('.rr-final-board').count(),0);
      const headers = await page.getByRole('columnheader').allTextContents();
      assert.deepEqual(headers.slice(0,2), ['Player','Net']);
      await button(page,'Session statistics').click();
      await page.getByText('Statistics guide',{exact:true}).click();
      await page.getByText('Voluntarily put chips in pot',{exact:true}).waitFor();
      assert.equal(await page.getByRole('columnheader',{name:'VPIP'}).count(),1);
      const state = await page.evaluate(() => window.poker.request('state'));
      assert.equal(state.paid,true);
      const awarded = Object.values(state.result.payouts).reduce((total, value) => total + BigInt(value), 0n);
      assert.equal(await page.getByTestId('awarded-chips').innerText(),awarded.toLocaleString('en-US'));
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

test('bots fill unready seats but cannot deal again after the last human leaves',
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
      const settled = await page.evaluate(() => window.poker.request('state'));
      const robin = settled.players.find(player => player.name === 'Robin').accountId;
      await button(page,'Table options').click();
      const dialog = page.getByRole('dialog');
      await dialog.getByRole('button',{name:/Robin/}).click();
      await button(dialog,'Continue as Robin').click();
      await button(dialog,'Leave seat').click();
      await button(dialog,'Leave seat').click();
      await page.getByText('All human players have left. End the table to return to the lobby.')
        .waitFor();
      assert.equal(await button(page,'Deal next hand').isDisabled(),true);
      const departed = await page.evaluate(() => window.poker.request('state'));
      assert.equal(departed.funds[robin].tableChips,'0');
      assert.equal(departed.funds[robin].balance,settled.funds[robin].balance);
      await button(page,'中文').click();
      await page.getByText('所有真人玩家均已离桌。请结束牌桌并返回大厅。').waitFor();
      await button(page,'English').click();
      await button(page,'End session').click();
      await button(page,'Return to lobby').click();
      await page.getByRole('heading',{name:'Table lobby'}).waitFor();
      assert.deepEqual(room.errors,[]);
      await room.app.close();
      room = await open(data);
      const account = await room.page.evaluate(({id,password}) =>
        window.poker.request('login',{id,password}),{id:robin,password});
      assert.equal(account.balance,settled.funds[robin].balance);
      assert.equal(account.history.filter(row => row.type === 'HAND_SETTLEMENT').length,1);
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
          await page.getByLabel('Raise to').fill('4000');
          assert.equal(await button(page,'Raise').isDisabled(),true);
          await page.getByLabel('Raise to').fill('40');
          const actor = game.players.find(player => player.seat === game.actor);
          const extra = 40n - BigInt(actor.round);
          const preview = await page.locator('#rr-wager-preview').innerText();
          assert.ok(preview.includes('Add now ' + extra));
          assert.ok(preview.includes('Stack after ' + (BigInt(actor.stack) - extra)));
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
      assert.equal(await page.locator('.rr-final-board [role=img]').count(),5);
      await page.getByText('Review final table',{exact:true}).click();
      assert.equal(await page.locator('.rr-arena').isVisible(),true);
      assert.equal(await page.locator('.rr-arena .rr-cardbacks').count(),0);
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
