import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { chipText, continuationNotice, seatedNames } from './table-state.mjs';
import { useTableRequests } from './use-table-requests.mjs';
import { localeCatalog } from './locales.mjs';
import BettingControls from './BettingControls.jsx';
import { CardBacks, Results, SettlementSummary, TableSurface } from './TableSurface.jsx';
import { CardDecision, HandReview, TableHistory } from './HandReview.jsx';
import { replayAt, runoutEvents } from './hand-history.mjs';
import OwnerHand from './OwnerHand.jsx';
import TableOptions from './TableOptions.jsx';
import './table.css';

const RUNOUT_STREETS = [
  { phase: 'PREFLOP', boardSize: 0 },
  { phase: 'FLOP', boardSize: 3 },
  { phase: 'TURN', boardSize: 4 },
  { phase: 'RIVER', boardSize: 5 },
];
const RUNOUT_STEP_DELAY_MS = 1200;

function HandoffCurtain({ game, actor, copy, busy, error, reveal, viewTable }) {
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
        {copy.viewPublicTable}</button>
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
    <span>{copy.lastAction}</span><strong>{action.name}</strong>
    {copy.past[action.action]} {BigInt(amount) > 0n && chipText(amount, lang)}
    {action.allIn && ' · ' + copy.ALL_IN}
  </div>;
}

function PaidControls({ game, copy, busy, execute, openOptions, showDetails }) {
  const notice = continuationNotice(game);
  return <>

    <button type="button" className="rr-primary" disabled={busy || !game.canNext}
      onClick={() => execute('next')}>{copy.next}<span aria-hidden="true">　→</span></button>
    <button type="button" className="rr-secondary" disabled={busy}
      onClick={showDetails}>{copy.details}</button>
    <button type="button" className="rr-link" disabled={busy}
      onClick={() => openOptions('end')}>{copy.end}</button>
    {notice && <small>{copy[notice]}</small>}
    {notice === 'fundNextHand' && <button type="button" className="rr-secondary" disabled={busy}
      onClick={() => openOptions('seats')}>{copy.fundSeats}</button>}
  </>;
}

function TurnStrip({ game, actor, hole, copy, lang, busy, execute, conceal, openOptions, saveFailed, solo, showDetails }) {
  if (game.showActor) {
    const showing = game.players.find(player => player.id === game.showActor);
    return <section className="rr-actions"><div className="rr-turn-copy"><small>{copy.SHOWDOWN}</small>
      <h2>{copy.pass(showing.name)}</h2></div><button type="button" className="rr-primary"
        hidden={Boolean(solo && game.privateOwner === showing.accountId)} disabled={busy} onClick={async () => {
          if (await execute('handoff', { id: showing.accountId })) openOptions('showdown');
        }}>{copy.showdownChoice}</button></section>;
  }
  if (game.result) {
    const winners = game.players.filter(player => game.result.winners.includes(player.id));
    return <section className="rr-actions rr-outcome">
      <div className="rr-outcome-icon" aria-hidden="true">♠</div>
      <div className="rr-turn-copy"><small>{game.result.uncontested ? copy.uncontested : copy[game.phase]}</small>
        <h2>{winners.map(p => p.name).join(' / ')}</h2>
        <p>{game.result.pots.length > 1 ? copy.multiplePots : winners.length > 1 ? copy.split : copy.won}</p></div>
      <div className="rr-outcome-controls">{game.paid ?
        <PaidControls game={game} copy={copy} busy={busy} execute={execute}
          openOptions={openOptions} showDetails={showDetails} /> : saveFailed ? <>
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
  if (solo || hole.length) {
    const locked = busy || (solo && game.privateOwner !== actor.accountId);
    return <BettingControls key={game.handId + ':' + game.revision}
      player={actor} hole={hole} solo={solo} busy={locked}
      game={game} copy={copy} lang={lang} conceal={conceal}
      submit={fields => execute('act', { id: actor.accountId, ...fields })} />;
  }
  return <section className="rr-actions rr-handoff" data-testid="handoff">
    <div className="rr-handoff-icon"><CardBacks /></div>
    <div className="rr-turn-copy"><small>{copy.ready}</small><h2>{copy.pass(actor.name)}</h2><p>{copy.privacy}</p></div>
    <button type="button" className="rr-primary" disabled={busy}
      onClick={() => execute('reveal', { id: actor.accountId })}>
      <span aria-hidden="true">♢</span>{copy.confirm(actor.name)}</button>
  </section>;
}

function Runout({ game, copy, lang, onFinish }) {
  const streets = RUNOUT_STREETS.filter(street => street.boardSize >= game.runout.from);
  const [step, setStep] = useState(0);
  const { boardSize, phase } = streets[step];
  const lastStep = step === streets.length - 1;
  const advance = useCallback(() => {
    if (lastStep) {
      onFinish(game.handId);
    } else {
      setStep(step + 1);
    }
  }, [game.handId, lastStep, onFinish, step]);
  useEffect(() => {
    const timer = setTimeout(advance, RUNOUT_STEP_DELAY_MS);
    return () => clearTimeout(timer);
  }, [advance]);
  const events = runoutEvents(game.events, boardSize);
  const frame = replayAt(events, events.length - 1);
  const visibility = Object.fromEntries(frame.dealt.map(id => [id, frame.visibility[id] || 'HIDDEN']));
  const publicGame = { ...game, board: game.board.slice(0, boardSize), phase, result: '',
    shown: frame.shown, visibility, events, actor: -1 };
  return (
    <>
      <TableSurface game={publicGame} copy={copy} lang={lang} />
      <section className="rr-actions">
        <div className="rr-turn-copy">
          <small>{copy.ALL_IN}</small>
          <h2>{copy.runout}</h2>
        </div>
        <button className="rr-secondary" type="button" onClick={advance}>{copy.nextEvent}</button>
      </section>
    </>
  );
}

function TableHeading({ game, copy, lang, busy, runout, solo, settled, openOptions }) {
  return (
    <header className="rr-table-heading">
      <div>
        <div className="rr-kicker"><i />{copy.stakes} · {game.players.length} {copy.seats}</div>
        <h1>{copy.title}</h1>
      </div>
      <div className="rr-header-facts">
        <div><small>{copy.hand}</small><strong>{String(game.handNumber).padStart(2, '0')}</strong></div>
        <div>
          <small>{copy.blinds}</small>
          <strong>{chipText(game.smallBlind, lang)} / {chipText(game.bigBlind, lang)}</strong>
        </div>
        <div className="rr-table-tools">
          {!runout && !solo && (
            <button type="button" className="rr-secondary" disabled={busy}
              onClick={() => openOptions('cards')}>{copy.sharedCards}</button>
          )}
          {settled && (
            <button type="button" className="rr-secondary" disabled={busy}
              onClick={() => openOptions('history')}>{copy.history}</button>
          )}
        </div>
        <button type="button" className="rr-options"
          disabled={busy || Boolean(game.showActor) || Boolean(runout)}
          onClick={() => openOptions('seats')} aria-label={copy.manage} title={copy.manage}>
          <i /><i /><i />
        </button>
      </div>
    </header>
  );
}

function humanTurn(game) {
  return game.players.some(player => player.type === 'HUMAN' && player.seat === game.actor);
}

export default function Table({ lang, request, initialGame, onEnd }) {
  const [finishedRunout, setFinishedRunout] = useState('');
  const [options, setOptions] = useState('');
  const [details, setDetails] = useState(false);
  const [soloAccount] = useState(() => {
    const humans = initialGame.players.filter(player => player.type === 'HUMAN');
    return humans.length === 1 ? humans[0].accountId : '';
  });
  const [screen, setScreen] = useState(() =>
    !soloAccount && humanTurn(initialGame) ? 'curtain' : 'board');

  const onTurnChanged = useCallback((turn, newHand) => {
    setScreen(!soloAccount && humanTurn(turn) ? 'transition' : 'board');
    if (newHand) setDetails(false);
  }, [soloAccount]);
  const onConcealed = useCallback(turn => {
    setOptions('');
    if (!soloAccount && humanTurn(turn)) setScreen('curtain');
  }, [soloAccount]);
  const onRevealed = useCallback(() => setScreen('play'), []);
  const {
    snapshot, hole, busy, failure, failedSaveHand, execute, conceal, refresh, dismissFailure,
    attemptSettlement,
  } = useTableRequests({
    initialGame, request, onEnd, soloAccount, onTurnChanged, onConcealed, onRevealed,
  });

  const game = useMemo(() => seatedNames(snapshot, lang), [snapshot, lang]);
  const runout = game.runout && !game.paid && finishedRunout !== game.handId;
  const solo = game.players.find(player => player.accountId && player.accountId === soloAccount);
  const actor = game.players.find(player => player.seat === game.actor);
  const copy = localeCatalog.get(lang).table;
  const error = failure && (copy.errors[failure] ?? copy.requestError);

  useEffect(() => {
    if (!runout && !busy) attemptSettlement(game);
  }, [busy, attemptSettlement, game, runout]);

  useEffect(() => {
    if (actor?.type !== 'BOT' || busy || (options && options !== 'cards') || error || game.result) return;
    const timer = setTimeout(() => execute('bot'), 680);
    return () => clearTimeout(timer);
  }, [actor, busy, error, execute, game, options]);

  useEffect(() => {
    if (screen !== 'transition' || options) return;
    const timer = setTimeout(() => setScreen('curtain'), 750);
    return () => clearTimeout(timer);
  }, [screen, game.handId, game.revision, options]);

  function openOptions(page) {
    conceal(false);
    setScreen('board');
    setOptions(page);
  }

  const settled = game.paid && Boolean(game.result);
  const showDetails = settled && details;
  const sharedHumanTurn = !solo && actor?.type === 'HUMAN';
  const showCurtain = sharedHumanTurn && screen === 'curtain' && !options;
  const waitingForHandoff = sharedHumanTurn && screen === 'transition';
  const cardOptions = options === 'cards' || options === 'showdown';
  const closeOptions = () => setOptions('');

  return (
    <main className="rr-game" data-testid="poker-table">
      <TableHeading game={game} copy={copy} lang={lang} busy={busy} runout={runout}
        solo={solo} settled={settled} openOptions={openOptions} />
      {showDetails && (
        <>
          <button className="rr-secondary" type="button" onClick={() => setDetails(false)}>
            <span aria-hidden="true">← </span>{copy.backToTable}
          </button>
          <h2>
            {game.players.filter(p => game.result.winners.includes(p.id))
              .map(player => player.name).join(' / ')} · {copy.won}
          </h2>
          <SettlementSummary game={game} copy={copy} lang={lang} />
          <Results key={game.handId} game={game} copy={copy} lang={lang} />
        </>
      )}
      <div hidden={showDetails}>
        {runout ? (
          <Runout key={game.handId} game={game} copy={copy} lang={lang}
            onFinish={setFinishedRunout} />
        ) : (
          <TableSurface game={game} copy={copy} lang={lang}
            ownerSeat={solo?.seat ?? (hole.length ? game.actor : undefined)} />
        )}
        {error && !options && (
          <div className="rr-alert" role="alert">
            {error}
            <button type="button" onClick={dismissFailure}>
              {failure === 'bot' ? copy.retry : copy.dismiss}
            </button>
          </div>
        )}
        {!settled && <LastAction game={game} copy={copy} lang={lang} />}
        {!runout && (
          <div className={solo ? 'rr-solo-controls' : undefined}>
            {solo && (
              <OwnerHand key={game.handId} player={solo} game={game} copy={copy}
                request={request} execute={execute} busy={busy} solo />
            )}
            {showCurtain ? (
              <HandoffCurtain game={game} actor={actor} copy={copy}
                busy={busy} error={error} viewTable={() => setScreen('board')}
                reveal={() => execute('reveal', { id: actor.accountId })} />
            ) : waitingForHandoff ? (
              <section className="rr-actions">
                <div className="rr-turn-copy"><small>{copy.ready}</small>
                  <h2>{copy.pass(actor.name)}</h2>
                </div>
              </section>
            ) : (
              <TurnStrip game={game} actor={actor} hole={hole} copy={copy} lang={lang}
                busy={busy} execute={execute} conceal={conceal} openOptions={openOptions}
                solo={solo} showDetails={() => setDetails(true)}
                saveFailed={failedSaveHand === game.handId} />
            )}
          </div>
        )}
        {settled && <TableHistory key={game.handId} game={game} copy={copy} lang={lang} />}
      </div>
      {options === 'history' && settled && (
        <HandReview game={game} copy={copy} lang={lang} request={request} close={closeOptions} />
      )}
      {cardOptions && (
        <CardDecision game={game} copy={copy} initialId={options === 'showdown' ? game.showActor : ''}
          execute={execute} request={request} busy={busy} close={closeOptions} />
      )}
      {options && options !== 'history' && !cardOptions && (
        <TableOptions game={game} copy={copy} lang={lang} request={request} initialPage={options}
          execute={execute} refresh={refresh} busy={busy} error={error} close={closeOptions} />
      )}
    </main>
  );
}
