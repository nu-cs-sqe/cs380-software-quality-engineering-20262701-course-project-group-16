import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import {
  chipText, continuationNotice, handoffGate, publicSnapshot, seatedNames, needsSettlement,
} from './table-state.mjs';
import { tableCopy } from './table-copy.mjs';
import { BettingControls, CardBacks, Results, SettlementSummary, TableSurface } from './TableSurface.jsx';
import TableOptions from './TableOptions.jsx';
import './table.css';

function HandoffCurtain({ game, actor, copy, lang, busy, error, reveal, viewTable }) {
  const dialog = useRef();
  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    element.querySelector('[data-public-table]').focus();
    return () => element.close();
  }, []);
  return <dialog ref={dialog} className="rr-curtain" aria-labelledby="rr-pass-name"
    data-testid="handoff" onCancel={event => { event.preventDefault(); viewTable(); }}>
    <div className="rr-curtain-panel">
      <div className="rr-curtain-cards"><CardBacks /></div>
      <small>{copy.ready}</small>
      <h2 id="rr-pass-name">{copy.pass(actor.name)}</h2>
      <p>{copy.privacy}</p>
      <div className="rr-curtain-rule"><span />♢<span /></div>
      <button type="button" className="rr-primary" disabled={busy} onClick={reveal}>
        {copy.confirm(actor.name)}</button>
      <button data-public-table type="button" className="rr-link" onClick={viewTable}>
        {lang === 'zh' ? '查看公共牌桌' : 'View public table'}</button>
      <div className="rr-curtain-position">{copy.hand} {String(game.handNumber).padStart(2, '0')}
        <i />{copy.seat} {actor.seat + 1}</div>
      {error && <p role="alert" className="rr-error">{error}</p>}
    </div>
  </dialog>;
}

function LastAction({ game, copy, lang }) {
  const action = game.lastAction;
  if (!action) return null;
  const amount = ['RAISE', 'BET'].includes(action.action) ? action.target : action.amount;
  return <div className="rr-last-action" role="status">
    <span>{lang === 'zh' ? '上一步' : 'Last action'}</span><strong>{action.name}</strong>
    {copy.past[action.action]} {BigInt(amount) > 0n && chipText(amount, lang)}
    {action.allIn && ' · ' + copy.ALL_IN}
  </div>;
}

function PaidControls({ game, copy, busy, execute, openOptions }) {
  const notice = continuationNotice(game);
  return <>
    <button type="button" className="rr-secondary" disabled={busy}
      onClick={() => openOptions('end')}>{copy.end}</button>
    <button type="button" className="rr-primary" disabled={busy || !game.canNext}
      onClick={() => execute('next')}>{copy.next}<span aria-hidden="true">　→</span></button>
    {notice && <small>{copy[notice]}</small>}
    {notice === 'fundNextHand' && <button type="button" className="rr-secondary" disabled={busy}
      onClick={() => openOptions('seats')}>{copy.fundSeats}</button>}
  </>;
}

function TurnStrip({ game, actor, hole, copy, lang, busy, execute, conceal, openOptions, saveFailed }) {
  if (game.result) {
    const winners = game.players.filter(player => game.result.winners.includes(player.id));
    return <section className="rr-actions rr-outcome">
      <div className="rr-outcome-icon" aria-hidden="true">♠</div>
      <div className="rr-turn-copy"><small>{game.result.uncontested ? copy.uncontested : copy[game.phase]}</small>
        <h2>{winners.map(p => p.name).join(' / ')}</h2>
        <p>{winners.length > 1 ? copy.split : copy.won}</p></div>
      <div className="rr-outcome-controls">{game.paid ?
        <PaidControls game={game} copy={copy} busy={busy} execute={execute}
          openOptions={openOptions} /> : saveFailed ? <>
        <small className="rr-error" role="alert">{copy.saveError}</small>
        <button type="button" className="rr-primary" disabled={busy}
          onClick={() => execute('settle')}>{copy.retrySave}</button></> :
        <small role="status">{copy.saving}</small>}</div>
    </section>;
  }
  if (!actor) return <section className="rr-actions"><p>{copy.waiting}</p></section>;
  if (actor.type === 'BOT') return <section className="rr-actions rr-bot-turn" role="status">
    <div className="rr-think" aria-hidden="true"><i /><i /><i /></div>
    <div className="rr-turn-copy"><small>{copy.botTurn}</small><h2>{copy.thinking(actor.name)}</h2></div>
  </section>;
  if (hole.length) return <BettingControls key={game.handId + ':' + game.revision}
    player={actor} hole={hole} game={game} copy={copy} lang={lang} conceal={conceal}
    submit={(action, target) => execute('act', { id: actor.accountId, action, ...(target && { target }) })} />;
  return <section className="rr-actions rr-handoff" data-testid="handoff">
    <div className="rr-handoff-icon"><CardBacks /></div>
    <div className="rr-turn-copy"><small>{copy.ready}</small><h2>{copy.pass(actor.name)}</h2><p>{copy.privacy}</p></div>
    <button type="button" className="rr-primary" disabled={busy}
      onClick={() => execute('reveal', { id: actor.accountId })}>
      <span aria-hidden="true">♢</span>{copy.confirm(actor.name)}</button>
  </section>;
}

export default function Table({ lang, request, initialGame, onEnd }) {
  const [snapshot, setGame] = useState(() => publicSnapshot(initialGame));
  const game = useMemo(() => seatedNames(snapshot, lang), [snapshot, lang]);
  const [hole, setHole] = useState([]);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState();
  const [failedSaveHand, setFailedSaveHand] = useState('');
  const lastAutoSave = useRef('');
  const [options, setOptions] = useState('');
  const [screen, setScreen] = useState(() =>
    initialGame.players.some(p => p.type === 'HUMAN' && p.seat === initialGame.actor) ? 'curtain' : 'board');
  const latest = useRef(snapshot);
  const gate = useRef(handoffGate());
  const privatePending = useRef(false);
  const locked = useRef(false);
  const hiding = useRef(Promise.resolve());
  const mounted = useRef(false);
  const api = useRef({ request, onEnd });
  api.current = { request, onEnd };
  const copy = tableCopy[lang] || tableCopy.en;
  const error = failure ? (lang === 'zh' ? copy.errors[failure.op] : failure.detail) : '';
  const actor = game.players.find(p => p.seat === game.actor);

  const adopt = useCallback(snapshot => {
    if (!mounted.current) return;
    const previous = latest.current;
    if (previous.handId !== snapshot.handId || previous.revision !== snapshot.revision
        || previous.actor !== snapshot.actor) {
      const humanTurn = snapshot.players.some(p => p.type === 'HUMAN' && p.seat === snapshot.actor);
      setScreen(humanTurn ? 'transition' : 'board');
    }
    latest.current = publicSnapshot(snapshot);
    setGame(latest.current);
  }, []);

  const clearPrivate = useCallback(() => {
    gate.current.conceal();
    privatePending.current = false;
    setHole([]);
  }, []);

  const conceal = useCallback(() => {
    const shouldHide = privatePending.current;
    flushSync(() => {
      clearPrivate();
      setOptions('');
      const turn = latest.current;
      if (turn.players.some(p => p.type === 'HUMAN' && p.seat === turn.actor)) setScreen('curtain');
    });
    if (shouldHide) {
      hiding.current = api.current.request('hide').catch(err => {
        if (mounted.current) setFailure({ op: 'hide', detail: err.message });
      });
    }
  }, [clearPrivate]);

  useEffect(() => {
    mounted.current = true;
    const visibility = () => { if (document.hidden) conceal(); };
    window.addEventListener('table-conceal', conceal);
    window.addEventListener('blur', conceal);
    document.addEventListener('visibilitychange', visibility);
    return () => {
      mounted.current = false;
      gate.current.conceal();
      window.removeEventListener('table-conceal', conceal);
      window.removeEventListener('blur', conceal);
      document.removeEventListener('visibilitychange', visibility);
      if (privatePending.current) void api.current.request('hide').catch(() => {});
    };
  }, [conceal]);

  const refresh = useCallback(async () => {
    try { adopt(await api.current.request('state')); }
    catch (err) { if (mounted.current) setFailure({ op: 'state', detail: err.message }); }
  }, [adopt]);

  const execute = useCallback(async (op, fields = {}) => {
    if (locked.current) return false;
    locked.current = true;
    const turn = latest.current;
    const ticket = gate.current.begin(turn);
    if (op === 'reveal') privatePending.current = true;
    else if (op === 'act') flushSync(clearPrivate);
    else clearPrivate();
    if (op === 'settle') setFailedSaveHand('');
    setBusy(true);
    setFailure(undefined);
    try {
      await hiding.current;
      if (op === 'reveal' && !gate.current.accepts(ticket, latest.current)) return false;
      const response = await api.current.request(op, { handId: turn.handId, revision: turn.revision, ...fields });
      if (!mounted.current) return false;
      if (op === 'end') {
        api.current.onEnd();
      } else if (op === 'reveal') {
        if (gate.current.accepts(ticket, latest.current) && gate.current.accepts(ticket, response)
            && !document.hidden) {
          adopt(response);
          setHole(response.hole);
          setScreen('play');
        }
      } else {
        adopt(response);
      }
      return true;
    } catch (err) {
      if (mounted.current) {
        clearPrivate();
        await refresh();
        if (op === 'settle') setFailedSaveHand(turn.handId);
        else setFailure({ op, detail: err.message });
      }
      return false;
    } finally {
      locked.current = false;
      if (mounted.current) setBusy(false);
    }
  }, [adopt, clearPrivate, refresh]);

  useEffect(() => {
    if (busy || locked.current || !needsSettlement(game, lastAutoSave.current)) return;
    lastAutoSave.current = game.handId;
    void execute('settle');
  }, [busy, execute, game]);

  useEffect(() => {
    if (actor?.type !== 'BOT' || busy || options || error || game.result) return;
    const timer = setTimeout(() => execute('bot'), 680);
    return () => clearTimeout(timer);
  }, [actor, busy, error, execute, game, options]);

  useEffect(() => {
    if (screen !== 'transition' || options) return;
    const timer = setTimeout(() => setScreen('curtain'), 750);
    return () => clearTimeout(timer);
  }, [screen, game.handId, game.revision, options]);

  function openOptions(page) {
    conceal();
    setScreen('board');
    setOptions(page);
  }

  const settled = game.paid && game.result;
  const surface = <TableSurface game={game} copy={copy} lang={lang} privateView={hole.length > 0} />;
  return <main className="rr-game" data-testid="poker-table">
    <header className="rr-table-heading">
      <div><div className="rr-kicker"><i />{copy.stakes} · {game.players.length} {copy.seats}</div>
        <h1>{copy.title}</h1></div>
      <div className="rr-header-facts"><div><small>{copy.hand}</small>
        <strong>{String(game.handNumber).padStart(2, '0')}</strong></div>
        <div><small>{copy.blinds}</small><strong>{chipText(game.smallBlind, lang)} / {chipText(game.bigBlind, lang)}</strong></div>
        <button type="button" className="rr-options" disabled={busy} onClick={() => openOptions('seats')}
          aria-label={copy.manage} title={copy.manage}><i /><i /><i /></button>
      </div>
    </header>
    {!settled && surface}
    {error && !options && <div className="rr-alert" role="alert">{error}
      <button type="button" onClick={() => setFailure(undefined)}>{failure.op === 'bot' ? copy.retry : copy.dismiss}</button></div>}
    {!settled && <LastAction game={game} copy={copy} lang={lang} />}
    {actor?.type === 'HUMAN' && screen === 'curtain' && !options
      ? <HandoffCurtain game={game} actor={actor} copy={copy} lang={lang} busy={busy} error={error}
          viewTable={() => setScreen('board')} reveal={() => execute('reveal', { id: actor.accountId })} />
      : screen === 'transition' && actor?.type === 'HUMAN'
        ? <section className="rr-actions"><div className="rr-turn-copy"><small>{copy.ready}</small>
          <h2>{copy.pass(actor.name)}</h2></div></section>
        : <TurnStrip game={game} actor={actor} hole={hole} copy={copy} lang={lang} busy={busy}
      execute={execute} conceal={conceal} openOptions={openOptions} saveFailed={failedSaveHand === game.handId} />}
    {settled && <>
      <SettlementSummary game={game} copy={copy} lang={lang} />
      <Results key={game.handId} game={game} copy={copy} lang={lang} />
      <details className="rr-final-table"><summary>{copy.finalTable}</summary>
        {surface}<LastAction game={game} copy={copy} lang={lang} />
      </details>
    </>}
    {options && <TableOptions game={game} copy={copy} lang={lang} request={request} initialPage={options}
      execute={execute} refresh={refresh} busy={busy} error={error} close={() => setOptions('')} />}
  </main>;
}
