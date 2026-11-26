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
  try {
    const page = await app.firstWindow();
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.getByRole('heading',{name:'Table lobby'}).waitFor();
    await app.evaluate(({app,BrowserWindow}) => {
      app.focus({steal:true});
      BrowserWindow.getAllWindows()[0].focus();
    });
    await page.waitForFunction(() => document.hasFocus() && !document.hidden);
    return {app, page, errors};
  } catch (error) {
    await app.close();
    throw error;
  }
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
  const paid = button(page,copy.details);
  const reveal = page.getByRole('button',{
    name:lang === 'zh' ? /查看手牌/ : /Reveal hand/, disabled:false
  });
  const hole = page.getByTestId('private-hand');
  for (let step = 0; step < 2; step++) {
    await paid.or(reveal).first().waitFor({timeout:60000});
    if (await paid.isVisible()) break;
    assert.equal(await hole.count(),0);
    await reveal.click();
    try {
      await hole.waitFor();
    } catch (cause) {
      const publicState = await tableState(page);
      const focused = await page.evaluate(() => document.hasFocus());
      const errors = await page.getByRole('alert').allTextContents();
      throw new Error('Private handoff failed at seat ' + publicState.actor + ', phase '
        + publicState.phase + ', focused=' + focused + ', alerts=' + errors.join(' | '), {cause});
    }
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

for (const language of ['en', 'zh']) {
  test(`startup renders buy-in validation and grouped balances (${language})`,
    {timeout:60000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(), 'river-room-language-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      const copy = lobbyLanguage[language];
      await selectLanguage(page, language);
      await page.locator('[data-position="4"]').click();
      await register(page, 'Casey', copy);
      assert.equal((await page.locator('.rr-balance strong').innerText()).trim(),
        '2,000 ' + copy.chips);
      const amount = page.getByLabel(copy.buyIn);
      for (const invalid of ['399', '2001', '400.5']) {
        await amount.fill(invalid);
        await button(page, copy.markReady).click();
        await page.getByRole('alert').filter({hasText:copy.errors.buyIn}).waitFor();
        assert.equal(await button(page, copy.start).isDisabled(), true);
        assert.equal(await page.getByTestId('poker-table').count(), 0);
      }
      const expectedBuyIn = language === 'en' ? '1000' : '400';
      await amount.fill(language === 'en' ? '' : expectedBuyIn);
      await button(page, copy.markReady).click();
      assert.equal(await button(page, copy.start).isEnabled(), true);
      await button(page, copy.start).click();
      await page.getByTestId('poker-table').waitFor();
      const game = await tableState(page);
      const player = game.players[4];
      assert.equal(player.name, 'Casey');
      assert.equal(game.funds[player.accountId].tableChips, expectedBuyIn);
      assert.equal(game.players.filter(player => player.type === 'BOT').length, 5);
      assert.deepEqual(room.errors, []);
    } finally {
      if (room) await room.app.close().catch(() => {});
      await rm(data, {recursive:true, force:true});
    }
  });
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
      await page.getByRole('heading', { name: lobbyLanguage.zh.title }).waitFor();
      await button(page,'English').click();
      await page.getByRole('heading', { name: lobbyLanguage.en.title }).waitFor();
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
        if (turn === 4) {
          withDatabase(data, database => database.exec(
            "CREATE TRIGGER reject_hand BEFORE INSERT ON settled_hands "
            + "BEGIN SELECT RAISE(ABORT, 'test save failure'); END"));
        }
        await button(page,'Fold').click();
        await hole.waitFor({state:'detached'});
      }
      const retry = button(page, tableCopy.en.retrySave);
      await retry.waitFor();
      await assertHistoryUnavailable(page);
      const unpaid = await tableState(page);
      assert.equal(unpaid.paid, false);
      assert.equal(unpaid.events.some(event => event.kind === 'AWARD'), false);
      const {hands} = await page.evaluate(({ handId, revision }) =>
        window.poker.request('history', { handId, revision }), unpaid);
      assert.deepEqual(hands, []);
      assert.equal(withDatabase(data, database => database.prepare(
        'SELECT COUNT(*) AS count FROM settled_hands WHERE hand_id = ?').get(unpaid.handId).count), 0);
      withDatabase(data, database => database.exec('DROP TRIGGER reject_hand'));
      await retry.click();
      await button(page,tableCopy.en.details).waitFor();
      const finalState = await tableState(page);
      assert.equal(finalState.handId, unpaid.handId);
      assert.equal(finalState.events.filter(event => event.kind === 'AWARD').length, 1);
      assert.equal(withDatabase(data, database => database.prepare(
        'SELECT COUNT(*) AS count FROM settled_hands WHERE hand_id = ?').get(unpaid.handId).count), 1);
      const folder = finalState.players.find(p => finalState.folded.includes(p.id)
        && p.accountId !== finalState.privateOwner);
      await button(page,tableCopy.en.sharedCards).click();
      const cards = page.getByRole('dialog', {name:tableCopy.en.sharedCards});
      await button(cards,folder.name).click();
      await cards.getByLabel('Account password').fill(password);
      await button(cards,tableCopy.en.signInSeat).click();
      await button(cards,'Show both cards').click();
      await cards.waitFor({state:'detached'});
      await button(page,'View Hand History').click();
      const review = page.getByRole('dialog', {name:'View Hand History'});
      const slider = review.getByRole('slider');
      await slider.focus();
      await slider.press('End');
      await review.getByRole('img').nth(1).waitFor();
      await button(review,'Previous action').click();
      assert.equal(await review.getByRole('img').count(),0);
      await button(review,'Close').click();
      assert.equal(await page.locator('.rr-arena').isVisible(),true);
      await button(page, tableCopy.en.details).click();
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
      await button(page,tableCopy.en.backToTable).click();
      assert.deepEqual(await tableState(page),state);
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
      await assertHistoryUnavailable(page);
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

test('solo seat selection, private cards and continuation after the last human leaves',
  {timeout:150000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-bots-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      await page.locator('[data-position="4"]').click();
      await register(page,'Robin',lobbyLanguage.en);
      await ready(page,'400',lobbyLanguage.en);
      await button(page,'Start game').click();
      await page.getByTestId('poker-table').waitFor();
      const first = await tableState(page);
      assert.equal(first.players.filter(p=>p.type === 'BOT').length,5);
      assert.equal(first.players[4].name,'Robin');
      assert.equal(await page.locator('[data-position]').count(),0);
      assert.match(await page.getByTestId('seat-4').getAttribute('class'),/pos-4/);
      assert.equal(await button(page,tableCopy.en.sharedCards).count(),0);
      assert.equal(await page.locator('input[type=password]').count(),0);
      await page.getByTestId('private-hand').waitFor();
      await button(page,tableCopy.en.hide).click();
      await page.getByTestId('private-hand').waitFor({state:'detached'});
      await button(page,tableCopy.en.inspect).click();
      await page.getByTestId('private-hand').waitFor();
      const paid = button(page,tableCopy.en.details);
      const fold = button(page,'Fold');
      await paid.or(fold).first().waitFor({timeout:60000});
      if (await fold.isVisible()) {
        await assertHistoryUnavailable(page);
        const handBox = await page.locator('.rr-owner-hand').boundingBox();
        const controlBox = await page.locator('.rr-bet-panel').boundingBox();
        const rowBox = await page.locator('.rr-action-buttons').boundingBox();
        assert.ok(controlBox.x >= handBox.x + handBox.width);
        assert.ok(Math.abs(rowBox.width - controlBox.width) < 1);
        await fold.click();
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
      const paid = button(page,tableCopy.en.details);
      const streets = new Set();
      for (let turn=0; turn<40; turn++) {
        await paid.or(reveal).first().waitFor();
        if (await paid.isVisible()) break;
        await reveal.click();
        await page.getByTestId('private-hand').waitFor();
        const game = await page.evaluate(() => window.poker.request('state'));
        const first = !streets.has(game.phase);
        streets.add(game.phase);
        if (first) {
          await verifyStreetLayout(page, game);
          if (game.phase === 'FLOP') {
            await room.app.evaluate(({ BrowserWindow }) =>
              BrowserWindow.getAllWindows()[0].setSize(900, 650));
            await page.waitForFunction(() => innerWidth === 900 && innerHeight <= 650);
            assert.deepEqual(await room.app.evaluate(({ BrowserWindow }) =>
              BrowserWindow.getAllWindows()[0].getSize()), [900, 650]);
            await verifyStreetLayout(page, game);
          }
        }
        const previous = game.lastAction;
        if (previous && game.streetActions[previous.playerId]) {
          const seat = game.players.find(player => player.id === previous.playerId);
          const badge = page.getByTestId('seat-' + seat.seat).locator('.rr-seat-status');
          assert.ok((await badge.innerText()).startsWith(tableCopy.en[previous.action]));
        }
        if (first && game.phase === 'PREFLOP') {
          await verifyOpeningRaiseControls(page, game);
          await button(page,'Raise').click();
          await verifyAcceptedRaise(page, game, 40n);
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
      await page.setViewportSize({width:1500,height:900});
      assert.equal(Object.keys(result.shown).length,6);
      await verifyCompletedHistory(page);
      assert.equal(await page.locator('.rr-seat.is-winner').count(),result.result.winners.length);
      for (const id of result.result.winners) {
        const focus = page.locator('.rr-winning-focus select');
        if (await focus.count()) await focus.selectOption(id);
        const selected = await page.locator('.rr-arena .rr-card[data-winning]').evaluateAll(cards =>
          cards.map(card => card.getAttribute('aria-label')).sort());
        const rank = {ACE:'A',KING:'K',QUEEN:'Q',JACK:'J',TEN:'10',NINE:'9',EIGHT:'8',
          SEVEN:'7',SIX:'6',FIVE:'5',FOUR:'4',THREE:'3',TWO:'2'};
        assert.deepEqual(selected, result.winningCards[id].map(card =>
          tableCopy.en.card(rank[card.rank], tableCopy.en[card.suit])).sort());
        const player = result.players.find(p => p.id === id);
        const seat = page.getByTestId('seat-' + player.seat);
        assert.equal(await seat.locator('.rr-award').innerText(),
          '+' + BigInt(result.result.payouts[id]).toLocaleString('en-US'));
        assert.equal(await seat.locator('.rr-stack-value').innerText(),
          BigInt(result.result.after[id]).toLocaleString('en-US'));
      }
      assert.deepEqual([...streets],['PREFLOP','FLOP','TURN','RIVER']);
      assert.equal(result.board.length,5);
      await button(page, 'View Hand History').click();
      const review = page.getByRole('dialog', {name:'View Hand History'});
      const timeline = review.getByRole('slider', {name:'Timeline'});
      await timeline.waitFor();
      assert.equal(await review.getByRole('img').count(), 0);
      await button(review, 'Next action').click();
      await review.locator('.rr-event-list li').last().getByText(/Small blind/).waitFor();
      await timeline.focus();
      await timeline.press('End');
      await review.getByText('Pot awarded', {exact:false}).waitFor();
      assert.equal(await review.getByRole('img').count(), 17);
      await timeline.press('Home');
      assert.equal(await review.getByRole('img').count(), 0);
      await button(review, 'Close').click();
      assert.equal(await page.getByTestId('community-cards').getByRole('img').count(),5);
      assert.equal(await page.locator('.rr-arena').isVisible(),true);
      assert.equal(await page.locator('.rr-arena .rr-cardbacks').count(),0);
      assert.equal(result.paid,true);
      assert.ok(result.result.winners.length > 0);
      assert.equal(Object.values(result.funds).reduce((total,fund) => total+BigInt(fund.balance),0n),12000n);
      await button(page,'中文').click();
      await button(page,tableCopy.zh.details).click();
      await button(page,'本场统计').click();
      assert.equal(await page.getByRole('columnheader',{name:'手数'}).count(),1);
      await button(page,tableCopy.zh.backToTable).click();
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

test('optional shared showdown hands the device to each claimant without asking for a password',
  {timeout:150000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-ordered-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      const copy = tableCopy.en;
      await page.getByText(lobbyLanguage.en.disclosureRules,{exact:true}).click();
      await page.getByLabel(lobbyLanguage.en.manualShowdown).check();
      await startTable(page,['Amy','Bea','Cam','Dan','Eve','Fox']);
      const handoff = page.getByRole('button',{name:/Reveal hand/, disabled:false});
      const choose = button(page,copy.showdownChoice);
      for (let turn = 0; turn < 24; turn++) {
        await handoff.or(choose).first().waitFor();
        if (await choose.isVisible()) break;
        await handoff.click();
        try {
          await page.getByTestId('private-hand').waitFor({timeout:10000});
        } catch (cause) {
          const state = await tableState(page);
          const controls = await page.getByRole('button').allTextContents();
          const alerts = await page.getByRole('alert').allTextContents();
          throw new Error(JSON.stringify({turn, phase:state.phase, actor:state.actor,
            revision:state.revision, focused:await page.evaluate(() => document.hasFocus()),
            controls, alerts, rendererErrors:room.errors}), {cause});
        }
        const check = button(page,copy.CHECK);
        if (await check.isVisible()) await check.click();
        else await page.getByRole('button',{name:/^Call /}).click();
        await page.getByTestId('private-hand').waitFor({state:'detached'});
      }
      // Advance one disclosure at the bridge while the renderer holds the older turn.
      const before = await tableState(page);
      const outgoing = before.players.find(player => player.id === before.showActor);
      const handedOff = await page.evaluate(({ handId, revision, id }) =>
        window.poker.request('handoff', { handId, revision, id }), {
        handId: before.handId, revision: before.revision, id: outgoing.accountId,
      });
      const after = await page.evaluate(({ handId, revision, id }) =>
        window.poker.request('disclose', { handId, revision, id, choice: 'MUCK' }), {
        handId: handedOff.handId, revision: handedOff.revision, id: outgoing.accountId,
      });
      const next = after.players.find(player => player.id === after.showActor);
      await choose.click();
      await page.getByRole('heading', { name: copy.pass(next.name) }).waitFor();
      assert.equal(await page.getByRole('dialog', { name: copy.sharedCards }).count(), 0);
      await page.getByRole('alert').filter({
        hasText:'The turn has changed. Pass the table to the current player.',
      }).waitFor();

      for (let decision = 1; decision < 5; decision++) {
        await choose.click();
        const dialog = page.getByRole('dialog',{name:copy.sharedCards});
        assert.equal(await dialog.locator('input[type=password]').count(),0);
        await button(dialog,copy.inspect).click();
        await dialog.getByTestId('private-hand').waitFor();
        await button(dialog,copy.muck).click();
        await dialog.waitFor({state:'detached'});
      }
      await button(page,copy.details).waitFor();
      const game = await tableState(page);
      assert.deepEqual(game.shown,{});
      assert.deepEqual(game.winningCards,{});
      assert.equal(game.result.winners.length,1);
      assert.equal(game.events.filter(event => event.kind === 'MUCKED').length,5);
      assert.equal(game.events.filter(event => event.kind === 'AWARD').length,1);
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });

test('unequal all-ins distribute every side pot on the same table and in the details view',
  {timeout:120000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-side-pots-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      const copy = tableCopy.en;
      await button(page,'6').click();
      for (const [i,name] of ['Mo','Cal','Ben','Tess','River','Jude'].entries()) {
        await join(page,i+1,name,String(400+100*i));
      }
      await button(page,'Start game').click();
      const handoff = page.getByRole('button',{name:/Reveal hand/,disabled:false});
      for (let i = 0; i < 6; i++) {
        await handoff.click();
        await page.getByTestId('private-hand').waitFor();
        const raise = button(page,copy.RAISE);
        if (await raise.isVisible()) {
          await button(page,copy.max).click();
          await raise.click();
        } else {
          assert.equal(await page.getByRole('slider').count(),0);
          const call = page.getByRole('button',{name:/^Call /});
          const state = await tableState(page);
          const actor = state.players.find(p => p.seat === state.actor);
          assert.equal((await call.innerText()).includes(copy.ALL_IN),
            state.legal.call === actor.stack);
          await call.click();
        }
      }
      await button(page,copy.details).waitFor();
      const game = await tableState(page);
      const amounts = [2400,500,400,300,200];
      assert.deepEqual(game.result.pots.map(shares =>
        Object.values(shares).reduce((sum,chips) => sum + Number(chips),0)),amounts);
      const total = chips => Object.values(chips).reduce((sum,value) => sum + BigInt(value),0n);
      const committed = game.events.reduce((sum,event) => sum + BigInt(event.amount || '0'),0n);
      assert.equal(total(game.result.payouts),3800n);
      assert.equal(total(game.result.refunds),committed - 3800n);
      assert.equal(total(game.result.after),3900n);
      await button(page,copy.details).click();
      assert.equal(await page.locator('.rr-results .rr-pot-breakdown dt').count(),5);
      await button(page,copy.backToTable).click();
      assert.deepEqual(await tableState(page),game);
      assert.equal(await page.locator('.rr-table-history .rr-pot-breakdown dt').count(),5);
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });

async function assertHistoryUnavailable(page) {
  assert.equal(await page.locator('.rr-table-history').count(), 0);
  assert.equal(await button(page, tableCopy.en.history).count(), 0);
}

async function verifyOpeningRaiseControls(page, game) {
  const amount = page.getByRole('textbox', { name: 'Raise to' });
  const slider = page.getByRole('slider', { name: 'Wager size' });
  await slider.press('End');
  assert.equal(await amount.inputValue(), game.legal.max);
  await slider.press('Home');
  assert.equal(await amount.inputValue(), game.legal.min);
  await button(page, 'Pot').click();
  assert.equal(await amount.inputValue(), '70');
  await button(page, 'Min').click();
  assert.equal(await amount.inputValue(), game.legal.min);
  await amount.fill('4000');
  assert.equal(await button(page, 'Raise').isDisabled(), true);
  await amount.fill('40');
  const actor = game.players.find(player => player.seat === game.actor);
  const extra = 40n - BigInt(actor.round);
  const preview = await page.locator('#rr-wager-preview').innerText();
  assert.ok(preview.includes('Add now ' + extra));
  assert.ok(preview.includes('Stack after ' + (BigInt(actor.stack) - extra)));
}

async function verifyAcceptedRaise(page, before, target) {
  await page.waitForFunction(async revision =>
    (await window.poker.request('state')).revision > revision, before.revision);
  const after = await tableState(page);
  const actor = before.players.find(p => p.seat === before.actor);
  const accepted = after.players.find(player => player.id === actor.id);
  const added = target - BigInt(actor.round);
  assert.equal(after.lastAction.playerId, actor.id);
  assert.equal(after.lastAction.action, 'RAISE');
  assert.equal(after.lastAction.target, String(target));
  assert.equal(after.lastAction.amount, String(added));
  assert.equal(accepted.round, String(target));
  assert.equal(BigInt(accepted.stack), BigInt(actor.stack) - added);
  assert.equal(BigInt(after.pot), BigInt(before.pot) + added);
}

async function verifyCompletedHistory(page) {
  const history = page.locator('.rr-table-history');
  assert.equal(await history.isVisible(), true);
  const groups = history.locator('.rr-history-streets > details > summary');
  const streets = await groups.evaluateAll(nodes =>
    nodes.map(node => node.firstChild.textContent.trim()));
  assert.deepEqual(streets, ['Blinds', 'Preflop', 'Flop', 'Turn', 'River', 'Showdown']);
  const historyBox = await history.boundingBox();
  const outcomeBox = await page.locator('.rr-outcome').boundingBox();
  assert.ok(historyBox.y >= outcomeBox.y + outcomeBox.height);
}

async function verifyStreetLayout(page, game) {
  await assertHistoryUnavailable(page);
  const actions = await page.locator('.rr-action-buttons button').allTextContents();
  const expected = ['FOLD', 'CHECK', 'CALL', 'BET', 'RAISE']
    .filter(action => game.legal.actions.includes(action));
  assert.deepEqual(actions.map(label => label.split(' ')[0]),
    expected.map(action => tableCopy.en[action]));
  if (game.lastAction) {
    assert.equal(await page.locator('.rr-last-action').isVisible(), true);
  }
  const board = page.getByTestId('community-cards');
  assert.equal(await board.locator('.rr-card').count(), game.board.length);
  assert.equal(await board.locator('.rr-slot').count(), 5 - game.board.length);
  assert.equal(await page.locator('.rr-street-progress [aria-current=step]').innerText(),
    tableCopy.en[game.phase]);
  assert.equal(await page.getByTestId('private-hand').getByRole('img').count(), 2);
  const active = page.getByTestId('seat-' + game.actor);
  assert.match(await active.locator('.rr-seat-status').innerText(), /To act/);

  const problems = await page.evaluate(() => {
    const errors = [];
    const rect = node => node.getBoundingClientRect();
    const overlaps = (a, b) => a.left < b.right && a.right > b.left
      && a.top < b.bottom && a.bottom > b.top;
    const center = rect(document.querySelector('.rr-center'));
    const seats = [...document.querySelectorAll('.rr-seat')].map(rect);
    if (seats.some((seat, index) => overlaps(seat, center)
        || seats.slice(index + 1).some(other => overlaps(seat, other)))) {
      errors.push('seat overlaps the board or another seat');
    }
    const controls = document.querySelectorAll('.rr-private .rr-card, .rr-action-buttons button, '
      + '.rr-disclosure-option, .rr-sizing input, .rr-sizing button, .rr-wager-preview');
    const row = rect(document.querySelector('.rr-action-buttons'));
    const buttons = [...document.querySelectorAll('.rr-action-buttons button')].map(rect);
    const [firstButton] = buttons;
    const equalSizeAndAlignment = buttons.every(button =>
      button.height >= 42 && button.height === firstButton.height
      && button.y === firstButton.y && Math.abs(button.width - firstButton.width) <= 1);
    const fillsRow = Math.abs(firstButton.left - row.left) <= 1
      && Math.abs(buttons.at(-1).right - row.right) <= 1;
    if (!equalSizeAndAlignment || !fillsRow) {
      errors.push('uneven action buttons');
    }
    for (const node of controls) {
      const box = rect(node);
      if (box.bottom > innerHeight || box.top < 0 || box.left < 0 || box.right > innerWidth) {
        errors.push('private card or betting control outside viewport');
      }
    }
    const inks = { spades: 'rgb(31, 41, 55)', hearts: 'rgb(220, 38, 38)',
      diamonds: 'rgb(37, 99, 235)', clubs: 'rgb(21, 128, 61)' };
    for (const card of document.querySelectorAll('.rr-card')) {
      const suit = card.getAttribute('aria-label').split(' ').at(-1);
      const marks = [...card.querySelectorAll('*')];
      if (getComputedStyle(card).backgroundColor !== 'rgb(255, 255, 255)'
          || marks.some(node => getComputedStyle(node).color !== inks[suit])) {
        errors.push('incorrect face or suit ink on ' + suit);
      }
    }
    return errors;
  });
  assert.deepEqual(problems, [], game.phase);
}

for (const immediate of [false,true]) {
  test('the closing folder appears at the chosen time during an all-in runout (immediate='
    + immediate + ')', {timeout:120000}, async () => {
    const data = await mkdtemp(path.join(os.tmpdir(),'river-room-all-in-'));
    let room;
    try {
      room = await open(data);
      const {page} = room;
      if (immediate) {
        await page.getByText(lobbyLanguage.en.disclosureRules,{exact:true}).click();
        await page.getByLabel(lobbyLanguage.en.earlyCards).check();
      }
      await startTable(page,['Ari','Bo','Cleo','Dev','Eli','Flo']);
      const reveal = page.getByRole('button',{name:/Reveal hand/, disabled:false});
      let folder;
      for (const action of ['raise','call','fold','fold','fold','show']) {
        await reveal.click();
        await page.getByTestId('private-hand').waitFor();
        if (action === 'raise') {
          await button(page,'Max').click();
          await button(page,'Raise').click();
        } else if (action === 'call') {
          await page.getByRole('button',{name:/^Call /}).click();
        } else {
          if (action === 'show') {
            const state = await tableState(page);
            folder = state.players.find(p => p.seat === state.actor);
            await page.getByLabel(immediate ? tableCopy.en.foldShow : tableCopy.en.foldDefer).check();
          }
          await button(page,'Fold').click();
        }
        await page.getByTestId('private-hand').waitFor({state:'detached'});
      }
      await page.getByRole('heading',{name:tableCopy.en.runout}).waitFor();
      await assertHistoryUnavailable(page);
      for (const size of [0,3,4,5]) {
        assert.equal(await page.getByTestId('community-cards').locator('.rr-card').count(),size);
        assert.equal(await page.locator('.rr-exposed .rr-card').count(),immediate ? 6 : 4);
        assert.equal(await page.getByTestId('seat-' + folder.seat).locator('.rr-card').count(),
          immediate ? 2 : 0);
        await button(page,tableCopy.en.nextEvent).click();
      }
      await button(page,tableCopy.en.details).waitFor();
      const settled = await tableState(page);
      assert.equal(settled.shown[folder.id].length,2);
      assert.equal(settled.result.payouts[folder.id],'0');
      await button(page,'中文').click();
      const copy = tableCopy.zh;
      await button(page,copy.history).click();
      const review = page.getByRole('dialog',{name:copy.history});
      await button(review,copy.FLOP).click();
      assert.equal(await review.getByRole('img').count(),immediate ? 9 : 7);
      const timeline = review.getByRole('slider',{name:copy.timeline});
      await timeline.focus();
      await timeline.press('End');
      assert.equal(await review.getByRole('img').count(),11);
      await review.locator('.rr-pot-breakdown dt').first().waitFor();
      await button(review,copy.close).click();
      assert.deepEqual(room.errors,[]);
    } finally {
      if (room) await room.app.close().catch(()=>{});
      await rm(data,{recursive:true,force:true});
    }
  });
}
