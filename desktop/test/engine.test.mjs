import assert from 'node:assert/strict';
import test from 'node:test';
import { spawn } from 'node:child_process';
import { Engine } from '../electron/engine.cjs';

const fake = `
const lines = require('node:readline').createInterface({input: process.stdin});
lines.on('line', line => {
  const {op, name} = JSON.parse(line);
  if (op === 'exit') process.exit(1);
  else if (op === 'garble') console.log('oops');
  else if (op === 'reject') console.log(JSON.stringify({ok:false,error:'Invalid action'}));
  else console.log(JSON.stringify({ok:true,data:name}));
});`;
function engine() {
  return new Engine(spawn(process.execPath, ['-e', fake], {stdio:['pipe','pipe','ignore']}));
}
test('answers stay paired with their requests, including after a rejected action', async () => {
  const port = engine();
  try {
    const results = await Promise.allSettled([
      port.request({op:'echo',name:'first'}), port.request({op:'reject'}),
      port.request({op:'echo',name:'third'})
    ]);
    assert.equal(results[0].value,'first');
    assert.match(results[1].reason.message,/Invalid action/);
    assert.equal(results[2].value,'third');
  } finally { await port.close(); }
});
test('closing the window drains already submitted work', async () => {
  const port = engine();
  const active = port.request({op:'echo',name:'saved'});
  const closing = port.close();
  await assert.rejects(port.request({op:'echo'}));
  assert.equal(await active,'saved');
  await closing;
});
for (const op of ['exit','garble']) {
  test('a broken engine rejects pending and future requests: '+op, async () => {
    const port = engine();
    const results = await Promise.allSettled([port.request({op}),port.request({op:'echo'})]);
    assert.ok(results.every(result=>result.status==='rejected'));
    await assert.rejects(port.request({op:'echo'}));
    await port.close();
  });
}
