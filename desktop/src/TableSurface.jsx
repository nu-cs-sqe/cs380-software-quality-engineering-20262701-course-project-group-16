import React, { useState } from 'react';
import { chipText, validTarget } from './table-state.mjs';
import { metrics } from './table-copy.mjs';

const ranks = { TWO: '2', THREE: '3', FOUR: '4', FIVE: '5', SIX: '6', SEVEN: '7', EIGHT: '8',
  NINE: '9', TEN: '10', JACK: 'J', QUEEN: 'Q', KING: 'K', ACE: 'A' };
const suits = { SPADES: '♠', HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣' };

export function PlayingCard({ card, copy }) {
  const rank = ranks[card.rank] ?? card.rank;
  const red = ['HEARTS', 'DIAMONDS'].includes(card.suit);
  return <span className={'rr-card' + (red ? ' is-red' : '')}
    role="img" aria-label={copy.card(rank, copy[card.suit])}>
    <span className="rr-corner" aria-hidden="true">{rank}<small>{suits[card.suit]}</small></span>
    <span className="rr-pip" aria-hidden="true">{suits[card.suit]}</span>
    <span className="rr-corner reverse" aria-hidden="true">{rank}</span>
  </span>;
}

export function CardBacks() {
  return <span aria-hidden="true" className="rr-cardbacks"><i>♠</i><i>♠</i></span>;
}

function Chips() {
  return <span className="rr-chips" aria-hidden="true">
    {[2, 4, 3].map((count, i) => <span key={count} className={'rr-stack stack-' + i}>
      {Array.from({ length: count }, (_, j) => <i key={j} style={{ bottom: j * 3 }} />)}
    </span>)}
  </span>;
}

function Seat({ player, index, position, active, winner, lang, copy }) {
  if (!player) return <div className={'rr-seat vacant pos-' + position}>{copy.vacant}</div>;
  const live = ['ACTIVE', 'ALL_IN'].includes(player.status);
  const stateLabel = winner ? copy.winner : active ? copy.acting : copy[player.status];
  return <div className={['rr-seat', 'pos-' + position, active && 'is-active', winner && 'is-winner',
    !live && 'is-folded'].filter(Boolean).join(' ')} data-testid={'seat-' + index}>
    {live && <CardBacks />}
    <div className="rr-seatplate">
      <span className="rr-seat-number" title={copy.seat + ' ' + (index + 1)}>
        {String(index + 1).padStart(2, '0')}
      </span>
      <div className="rr-seat-text"><strong title={player.name}>{player.name}</strong>
        <span className="rr-stack-value">{chipText(player.stack, lang)}</span></div>
      {player.role && <span className="rr-role" title={player.role}>{player.role.replaceAll(',', '·')}</span>}
    </div>
    <span className="rr-seat-status"><i />{active || !live || winner || player.status === 'ALL_IN'
      ? stateLabel : copy[player.type]}{BigInt(player.round) > 0n &&
      <span title={copy.contributed}>{' · ' + chipText(player.round, lang)}</span>}</span>
  </div>;
}

export function TableSurface({ game, lang, copy, privateView }) {
  return <section className="rr-arena" aria-label={copy.table}>
    <div className={'rr-table-layout' + (privateView ? ' from-actor' : '')}>
      <div className="rr-rail" aria-hidden="true"><div className="rr-felt" /></div>
      <div className="rr-center">
        <div className="rr-pot" key={game.pot} aria-label={copy.pot + ' ' + chipText(game.pot, lang)}>
          <Chips /><div><small>{copy.pot}</small><strong>{chipText(game.pot, lang)}</strong></div>
        </div>
        <div className="rr-board" aria-label={copy.board} data-testid="community-cards">
          {Array.from({ length: 5 }, (_, index) => game.board[index]
            ? <PlayingCard key={game.handId + ':' + index} card={game.board[index]} copy={copy} />
            : <span className="rr-slot" key={index} aria-hidden="true">◇</span>)}
        </div>
        <div className="rr-street"><span />{copy[game.phase]}<span /></div>
      </div>
      {Array.from({ length: 6 }, (_, index) => {
        const player = game.players.find(p => p.seat === index);
        const position = privateView ? (index - game.actor + 6) % 6 : index;
        return <Seat key={index} index={index} position={position} player={player} active={game.actor === index}
          winner={game.result && game.result.winners.includes(player?.id)} copy={copy} lang={lang} />;
      })}
    </div>
  </section>;
}

export function BettingControls({ game, player, hole, copy, lang, submit, conceal }) {
  const { legal } = game;
  const [target, setTarget] = useState(legal.min);
  const wager = legal.actions.find(action => action === 'BET' || action === 'RAISE');
  return <section className="rr-actions rr-private" aria-label={copy.actions}>
    <div className="rr-pocket" data-testid="private-hand" aria-label={copy.cards}>
      {hole.map((card, index) => <PlayingCard key={index} card={card} copy={copy} />)}
    </div>
    <div className="rr-private-title"><small>{copy.personal}</small><h2>{player.name}</h2>
      <button type="button" className="rr-link" onClick={conceal}>{copy.hide}</button></div>
    <div className="rr-bet-panel">
      {wager && <div className="rr-target">
        <label>{wager === 'RAISE' ? copy.raiseTo : copy.betTo}
          <input inputMode="numeric" type="text" maxLength={19} value={target}
            onChange={e => setTarget(e.target.value)} spellCheck="false"
            aria-invalid={!validTarget(target, legal.min, legal.max)} /></label>
        <button type="button" onClick={() => setTarget(legal.min)}>{copy.min}</button>
        <button type="button" onClick={() => setTarget(legal.max)}>{copy.max}</button>
        <small>{copy.limits(chipText(legal.min, lang), chipText(legal.max, lang))}</small>
      </div>}
      <div className="rr-action-buttons">
        {legal.actions.filter(action => action !== wager).map(action => <button type="button"
          key={action} className={action === 'FOLD' ? 'rr-secondary fold' : 'rr-secondary'}
          onClick={() => submit(action)}>{copy[action]}{action === 'CALL' && ' ' + chipText(legal.call, lang)}</button>)}
        {wager && <button type="button" className="rr-primary" disabled={!validTarget(target, legal.min, legal.max)}
          onClick={() => submit(wager, BigInt(target).toString())}>{copy[wager]}</button>}
      </div>
    </div>
  </section>;
}

export function Results({ game, copy, lang }) {
  const [tab, setTab] = useState('result');
  return <section className="rr-results">
    <div className="rr-tabs">
      {['result', 'stats'].map(item => <button type="button" key={item} aria-pressed={item === tab}
        onClick={() => setTab(item)}>{copy[item]}</button>)}
    </div>
    <div className="rr-scroll"><table>
      <thead><tr><th scope="col">{copy.player}</th>{tab === 'result'
        ? ['before', 'payout', 'refund', 'after', 'net'].map(key => <th key={key} scope="col">{copy[key]}</th>)
        : <><th scope="col">{copy.hands}</th>{Object.entries(metrics).map(([key, name]) =>
          <th key={key} scope="col" title={copy.metric[key]}>{name}</th>)}</>}</tr></thead>
      <tbody>{game.players.map(player => {
        const id = player.id;
        const result = game.result;
        const delta = BigInt(result.after[id]) - BigInt(result.before[id]);
        const stats = game.metrics[id] || { handsPlayed: 0, rates: {} };
        return <tr key={id}><th scope="row">{player.name}</th>{tab === 'result' ? <>
          <td>{chipText(result.before[id], lang)}</td>
          <td>{chipText(result.payouts[id] || '0', lang)}</td>
          <td>{chipText(result.refunds[id] || '0', lang)}</td>
          <td>{chipText(result.after[id], lang)}</td>
          <td className={delta > 0n ? 'positive' : delta < 0n ? 'negative' : ''}>
            {delta > 0n && '+'}{chipText(delta, lang)}</td></> : <>
          <td>{stats.handsPlayed}</td>{Object.keys(metrics).map(key => <td key={key}>
            {key in stats.rates ? stats.rates[key].toLocaleString(lang, { maximumFractionDigits: 1 })
              + (key === 'af' || key === 'gap' ? '' : '%') : '—'}</td>)}</>}</tr>;
      })}</tbody>
    </table></div>
  </section>;
}
