import React, { useEffect, useRef, useState } from 'react';
import { PlayingCard, PotBreakdown } from './TableSurface.jsx';
import { chipText, seatedNames } from './table-state.mjs';
import { handPositions, replayAt, streetHistory } from './hand-history.mjs';
import OwnerHand from './OwnerHand.jsx';

export function ReviewDialog({ title, close, children, copy, wide }) {
  const ref = useRef();
  useEffect(() => {
    const dialog = ref.current;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog ref={ref} className={'rr-dialog' + (wide ? ' rr-history-dialog' : '')}
    aria-label={title} onCancel={event => { event.preventDefault(); close(); }}>
    <div className="rr-dialog-heading"><h2>{title}</h2>
      <button className="rr-x" type="button" aria-label={copy.close} onClick={close}>×</button></div>
    {children}
  </dialog>;
}

export function CardDecision({ game, initialId, request, execute, copy, busy, close }) {
  const [id, setId] = useState(initialId || '');
  const owners = game.players.filter(p => p.type === 'HUMAN' && game.visibility[p.id]);
  const owner = owners.find(p => p.id === id);
  return <ReviewDialog title={copy.sharedCards} close={close} copy={copy}>
    {owner ? <OwnerHand key={owner.id} game={game} player={owner} copy={copy} request={request}
      execute={execute} busy={busy} close={close}/> :
      <div className="rr-seat-list">{owners.map(p => <button type="button" key={p.id}
        onClick={() => setId(p.id)}>{p.name}</button>)}</div>}
  </ReviewDialog>;
}

function eventText(event, names, copy, lang) {
  const name = names[event.id];
  const kind = copy[event.kind] || event.kind;
  const amount = event.target && ['BET','RAISE'].includes(event.kind) ? event.target : event.amount;
  return [name, kind, amount && BigInt(amount) > 0n ? chipText(amount, lang) : '',
    event.allIn ? copy.ALL_IN : ''].filter(Boolean).join(' · ');
}

export function TableHistory({game, copy, lang}) {
  const journal = useRef();
  const following = useRef(true);
  useEffect(() => {
    if (following.current) journal.current.scrollTop = journal.current.scrollHeight;
  }, [game.handId, game.events.length]);
  const positions = handPositions(game.players, game.events[0]);
  const names = Object.fromEntries(game.players.map(p => [p.id,
    [p.name, copy.seat + ' ' + (p.seat + 1), positions[p.id]].filter(Boolean).join(' · ')]));
  const streets = streetHistory(game.events);
  return <details className="rr-table-history" open>
    <summary>{copy.actionHistory}</summary>
    <div className="rr-history-streets" ref={journal} onScroll={({currentTarget: list}) => {
      following.current = list.scrollHeight - list.scrollTop <= list.clientHeight + 4;
    }}>{streets.map(({street, events, pot}, index) =>
      <details key={index} open={index === streets.length - 1}>
        <summary>{copy[street]} <span>{copy.pot} · {chipText(pot, lang)}</span></summary>
        <ol>{events.map((event, i) => <li key={i}>
          <span>{eventText(event, names, copy, lang)}</span>
          {(event.cards || event.board) && <span className="rr-review-cards">
            {(event.cards || event.board).map((card,j) => <PlayingCard key={j} card={card} copy={copy}/>)}
          </span>}
          {event.pots && <PotBreakdown pots={event.pots} players={game.players} copy={copy} lang={lang}/>}
          {event.refunds && game.players.filter(p => BigInt(event.refunds[p.id]) > 0n)
            .map(p => <small key={p.id}>{p.name} · {copy.refund} +{chipText(event.refunds[p.id], lang)}</small>)}
        </li>)}</ol>
      </details>)}</div>
  </details>;
}

function Replay({ hand, copy, lang }) {
  const [index, setIndex] = useState(0);
  const journal = useRef();
  useEffect(() => {
    const list = journal.current;
    list.scrollTop = list.scrollHeight;
  }, [index]);
  const { events } = hand;
  const frame = replayAt(events, index);
  const { players } = seatedNames(hand, lang);
  const names = Object.fromEntries(players.map(p => [p.id, p.name]));
  const streets = [...new Set(events.map(event => event.street))];
  return <>
    <div className="rr-replay-heading"><strong>{copy[frame.street]}</strong>
      <span>{copy.pot}　{chipText(frame.pot, lang)}</span></div>
    <nav className="rr-review-streets" aria-label={copy.streets}>{streets.map(street =>
      <button type="button" key={street} aria-current={frame.street === street ? 'step' : undefined}
        onClick={() => setIndex(events.findIndex(event => event.street === street))}>
        {copy[street]}</button>)}</nav>
    <div className="rr-board rr-review-board" aria-label={copy.board}>
      {Array.from({length:5}, (_, i) => frame.board[i]
        ? <PlayingCard key={i} card={frame.board[i]} copy={copy}/>
        : <span key={i} className="rr-slot" aria-hidden="true"/>)}</div>
    <div className="rr-replay-controls">
      <button type="button" className="rr-secondary" disabled={index === 0}
        onClick={() => setIndex(index - 1)}>{copy.prevEvent}</button>
      <label>{copy.timeline} · {index + 1} / {events.length}
        <input type="range" min="0" max={events.length - 1} value={index}
          aria-label={copy.timeline} onChange={e => setIndex(Number(e.target.value))}/></label>
      <button type="button" className="rr-secondary" disabled={index === events.length - 1}
        onClick={() => setIndex(index + 1)}>{copy.nextEvent}</button>
    </div>
    <div className="rr-scroll"><table><thead><tr>
      {[copy.player, copy.tableChips, copy.cards, copy.payout, copy.refund].map(label =>
        <th key={label} scope="col">{label}</th>)}</tr></thead>
      <tbody>{players.map(p => <tr key={p.id}>
        <th scope="row">{p.name}<small>{[p.seat === frame.dealer && copy.roles.BTN,
          copy[frame.blinds[p.id]], frame.blinds[p.id] !== frame.statuses[p.id] && copy[frame.statuses[p.id]]]
          .filter(Boolean).join(' · ')}</small></th>
        <td>{chipText(frame.stacks[p.id], lang)}</td>
        <td>{frame.shown[p.id] ? <div className="rr-review-cards">
          {frame.shown[p.id].map((card, i) => <PlayingCard key={i} card={card} copy={copy}/>)}
        </div> : frame.dealt.includes(p.id) ? copy[frame.visibility[p.id] || 'HIDDEN'] : copy.notDealt}
          {frame.shown[p.id] && <small>{copy[frame.visibility[p.id]]}</small>}
          {frame.categories[p.id] && <small>{copy[frame.categories[p.id]]}</small>}</td>
        <td>{frame.payouts[p.id] === undefined ? '—' : chipText(frame.payouts[p.id], lang)}</td>
        <td>{frame.refunds[p.id] === undefined ? '—' : chipText(frame.refunds[p.id], lang)}</td>
      </tr>)}</tbody></table></div>
    {frame.pots.length > 0 && <PotBreakdown pots={frame.pots} players={players} copy={copy} lang={lang}/>}
    <ol ref={journal} className="rr-event-list" aria-label={copy.timeline}>
      {events.slice(0, index + 1).map((event, i) =>
        <li key={i} aria-current={i === index ? 'step' : undefined}>
          <span>{copy[event.street]}</span> {eventText(event, names, copy, lang)}</li>)}</ol>
  </>;
}

export function HandReview({ game, request, copy, lang, close }) {
  const [hands, setHands] = useState([]);
  const [handIndex, setHandIndex] = useState(0);
  const [error, setError] = useState(false);
  useEffect(() => {
    let mounted = true;
    request('history', { handId: game.handId, revision: game.revision }).then(({ hands }) => {
      if (mounted) { setHands(hands); setHandIndex(hands.length - 1); }
    }).catch(() => { if (mounted) setError(true); });
    return () => { mounted = false; };
  }, [game.handId, game.revision, request]);
  return <ReviewDialog title={copy.history} copy={copy} close={close} wide>
    {error && <p role="alert">{copy.historyError}</p>}
    {hands.length > 0 && <>
      <label>{copy.hand} <select value={handIndex} onChange={e => setHandIndex(Number(e.target.value))}>
        {hands.map((hand, i) => <option key={hand.handId} value={i}>{hand.number}</option>)}</select></label>
      <Replay key={hands[handIndex].handId} hand={hands[handIndex]} lang={lang} copy={copy}/>
    </>}
  </ReviewDialog>;
}
