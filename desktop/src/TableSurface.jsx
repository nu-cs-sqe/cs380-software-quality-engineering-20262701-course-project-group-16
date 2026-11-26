import React, { useState } from 'react';
import { chipText, seatLabel } from './table-state.mjs';
import { metrics } from './table-copy.mjs';
import { localeCatalog } from './locales.mjs';

const ranks = { TWO: '2', THREE: '3', FOUR: '4', FIVE: '5', SIX: '6', SEVEN: '7', EIGHT: '8',
  NINE: '9', TEN: '10', JACK: 'J', QUEEN: 'Q', KING: 'K', ACE: 'A' };
const suits = {
  SPADES: { symbol: '♠', ink: '#1F2937' },
  HEARTS: { symbol: '♥', ink: '#DC2626' },
  DIAMONDS: { symbol: '♦', ink: '#2563EB' },
  CLUBS: { symbol: '♣', ink: '#15803D' },
};

const pipRows = {
  TWO: [[1], [], [1]], THREE: [[1], [1], [1]], FOUR: [[0, 2], [], [0, 2]],
  FIVE: [[0, 2], [1], [0, 2]], SIX: [[0, 2], [0, 2], [0, 2]],
  SEVEN: [[0, 2], [1], [0, 2], [], [0, 2]], EIGHT: [[0, 2], [1], [0, 2], [1], [0, 2]],
  NINE: [[0, 2], [0, 2], [1], [0, 2], [0, 2]], TEN: [[0, 2], [1], [0, 2], [0, 2], [1], [0, 2]],
};

function CardFace({ rank, symbol }) {
  const rows = pipRows[rank];
  if (!rows) return <span aria-hidden="true" className="rr-pip">{symbol}</span>;
  return <span aria-hidden="true" className="rr-pip-field" style={{gridTemplateRows:'repeat(' + rows.length + ', 1fr)'}}>
    {rows.flatMap((columns, row) => columns.map(col => <i key={row + '-' + col}
      style={{gridColumn: col + 1, gridRow: row + 1}}>{symbol}</i>))}
  </span>;
}

export function PlayingCard({ card, copy, highlighted = false }) {
  const rank = ranks[card.rank] ?? card.rank;
  const { symbol, ink } = suits[card.suit];
  const corner = <>{rank}<span>{symbol}</span></>;
  return <span className="rr-card" data-winning={highlighted || undefined} style={{ color: ink }}
    role="img" aria-label={copy.card(rank, copy[card.suit])}>
    <span className="rr-corner" aria-hidden="true">{corner}</span>
    <CardFace rank={card.rank} symbol={symbol} />
    <span className="rr-corner reverse" aria-hidden="true">{corner}</span>
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

function marked(card, best) {
  return best.some(other => other.rank === card.rank && other.suit === card.suit);
}

function Seat({ player, index, personal, active, winner, paid, streetAction, shown, visibility, best, award, category, lang, copy }) {
  if (!player) return <div className={'rr-seat vacant pos-' + index}>{copy.vacant}</div>;
  const live = ['ACTIVE', 'ALL_IN'].includes(player.status);
  const status = live && visibility === 'MUCKED' ? 'MUCKED' : player.status;
  const label = paid && live && !winner && visibility
    ? visibility : seatLabel(status, streetAction, active, winner);
  const note = [category && copy[category], visibility && visibility !== label
    && visibility !== 'HIDDEN' && copy.shortVisibility[visibility]].filter(Boolean).join(' · ');
  const stack = chipText(player.stack, lang);
  const contribution = chipText(player.round, lang);
  const roundLabel = copy.contributed + ' ' + contribution;
  return <div className={['rr-seat', 'pos-' + index, personal && 'is-personal', active && 'is-active', winner && 'is-winner',
    !live && 'is-folded'].filter(Boolean).join(' ')} data-seat={index} data-testid={'seat-' + index}>
    {visibility && !shown && <CardBacks />}
    <div className="rr-seatplate">
      {shown ? <div className="rr-exposed" role="group" aria-label={copy.seat + ' ' + (index + 1)}>
        {shown.map((card, i) => <PlayingCard key={i} card={card} copy={copy} highlighted={marked(card, best)}/>)}</div> :
      <span className="rr-seat-number" title={copy.seat + ' ' + (index + 1)}>
        {String(index + 1).padStart(2, '0')}
      </span>}
      <div className="rr-seat-text"><strong title={player.name}>{player.name}</strong>
        <span className="rr-seat-funds"><span className="rr-stack-value" title={stack}>{stack}</span>
          {player.role && <abbr className="rr-role"
            title={player.role.split(',').map(role => copy.roles[role]).join(' · ')}>
            {player.role.replaceAll(',', '·')}</abbr>}
        </span></div>
    </div>
    <span className="rr-seat-status" data-status={label}>{copy[label]}
      {award && <b className="rr-award">+{chipText(award, lang)}</b>}{BigInt(player.round) > 0n &&
      <span className="rr-round-chips" title={roundLabel} aria-label={roundLabel}>
        {contribution}</span>}</span>
    {note && <small className="rr-hand-category">{note}</small>}
  </div>;
}

const streets = ['PREFLOP', 'FLOP', 'TURN', 'RIVER'];

function StreetProgress({ game, copy }) {
  const reached = game.board.length === 0 ? 0 : game.board.length - 2;
  return <ol className="rr-street-progress" aria-label={copy.streets}>
    {streets.map((street, index) => <li key={street} data-reached={index <= reached}
      aria-current={street === game.phase ? 'step' : undefined}>
      <i aria-hidden="true"/>{copy[street]}
    </li>)}
  </ol>;
}

function sumChips(amounts) {
  return Object.values(amounts).reduce((total, amount) => total + BigInt(amount), 0n);
}

export function PotBreakdown({ pots, players, copy, lang }) {
  return <dl className="rr-pot-breakdown">{pots.map((shares, i) =>
    <div key={i}><dt>{i === 0 ? copy.mainPot : copy.sidePot(i)} · {chipText(sumChips(shares), lang)}</dt>
      <dd>{players.filter(p => p.id in shares)
        .map(p => p.name + ' +' + chipText(shares[p.id], lang)).join(' · ')}</dd></div>)}</dl>;
}

export function SettlementSummary({ game, copy, lang }) {
  const returned = sumChips(game.result.refunds);
  return <section className="rr-settlement-summary" aria-label={copy.paid}>
    <div className="rr-settlement-value"><Chips /><div><small>{copy.awarded}</small>
      <strong data-testid="awarded-chips">{chipText(sumChips(game.result.payouts), lang)}</strong></div></div>
    {returned > 0n && <div className="rr-settlement-return"><small>{copy.refund}</small>
      <strong>{chipText(returned, lang)}</strong></div>}
    {game.board.length > 0 && <div className="rr-final-board"><small>{copy.board}</small>
      <div className="rr-board">{game.board.map((card, index) =>
        <PlayingCard key={index} card={card} copy={copy} />)}</div></div>}
  </section>;
}

export function TableFrame({ children }) {
  return <div className="rr-arena"><div className="rr-table-layout">
    <div className="rr-rail" aria-hidden="true"><div className="rr-felt"/></div>
    {children}
  </div></div>;
}

export function TableSurface({ game, lang, copy, ownerSeat }) {
  const winners = game.paid ? game.result.winners : [];
  const publicWinners = winners.filter(id => game.winningCards[id]);
  const [preferred, setPreferred] = useState('');
  const selected = publicWinners.includes(preferred) ? preferred : publicWinners[0];
  const winning = game.winningCards?.[selected] || [];

  const potLabel = game.paid ? copy.awarded : copy.pot;
  const pot = chipText(game.paid ? sumChips(game.result.payouts) : game.pot, lang);
  return <section aria-label={copy.table}>
    <TableFrame>
      <div className="rr-center">
        <div className="rr-pot" key={game.pot} aria-label={potLabel + ' ' + pot}>
          <Chips /><div><small>{potLabel}</small><strong>{pot}</strong></div>
        </div>
        <div className="rr-board" aria-label={copy.board} data-testid="community-cards">
          {Array.from({ length: 5 }, (_, index) => game.board[index]
            ? <PlayingCard key={game.handId + ':' + index} card={game.board[index]} copy={copy}
                highlighted={marked(game.board[index], winning)} />
            : <span className="rr-slot" key={index} aria-hidden="true" />)}
        </div>
        <StreetProgress game={game} copy={copy} />
        {publicWinners.length > 1 && <label className="rr-winning-focus">{copy.highlight}
          <select value={selected} onChange={e => setPreferred(e.target.value)}>
            {game.players.filter(p => publicWinners.includes(p.id)).map(p =>
              <option key={p.id} value={p.id}>{p.name}</option>)}
          </select></label>}
      </div>
      {Array.from({ length: 6 }, (_, index) => {
        const player = game.players.find(p => p.seat === index);
        const id = player?.id;
        const folded = game.paid && game.folded.includes(id);
        return <Seat key={index} index={index} personal={index === ownerSeat}
          player={folded ? { ...player, status: 'FOLDED' } : player}
          active={game.actor === index || (id && game.showActor === id)}
          best={id === selected ? winning : []} category={game.categories?.[id]}
          award={game.paid && game.result.winners.includes(id) && game.result.payouts[id]}
          streetAction={game.streetActions?.[id]} shown={game.shown[id]}
          paid={game.paid} visibility={game.visibility[id]} winner={game.result && game.result.winners.includes(id)}
          copy={copy} lang={lang} />;
      })}
    </TableFrame>
  </section>;
}

export function Results({ game, copy, lang }) {
  const [tab, setTab] = useState('result');
  const { tag } = localeCatalog.get(lang);
  return <section className="rr-results">
    <div className="rr-tabs">
      {['result', 'stats'].map(item => <button type="button" key={item} aria-pressed={item === tab}
        onClick={() => setTab(item)}>{copy[item]}</button>)}
    </div>
    <div className="rr-scroll"><table>
      <thead><tr><th scope="col">{copy.player}</th>{tab === 'result'
        ? ['net', 'before', 'payout', 'refund', 'after'].map(key => <th key={key} scope="col">{copy[key]}</th>)
        : <><th scope="col">{copy.hands}</th>{Object.entries(metrics).map(([key, name]) =>
          <th key={key} scope="col" title={copy.metric[key]}>{name}</th>)}</>}</tr></thead>
      <tbody>{game.players.map(player => {
        const id = player.id;
        const result = game.result;
        const delta = BigInt(result.after[id]) - BigInt(result.before[id]);
        const stats = game.metrics[id] || { handsPlayed: 0, rates: {} };
        return <tr key={id}><th scope="row">{player.name}{game.categories?.[id] &&
          <small className="rr-public-result">{copy[game.categories[id]]}</small>}</th>{tab === 'result' ? <>
          <td><span className={'rr-net-pill ' + (delta > 0n ? 'gain' : delta < 0n ? 'loss' : 'even')}>
            {delta > 0n && '+'}{chipText(delta, lang)}</span></td>
          <td>{chipText(result.before[id], lang)}</td>
          <td>{chipText(result.payouts[id] || '0', lang)}</td>
          <td>{chipText(result.refunds[id] || '0', lang)}</td>
          <td>{chipText(result.after[id], lang)}</td>
          </> : <>
          <td>{stats.handsPlayed}</td>{Object.keys(metrics).map(key => <td key={key}>
            {key in stats.rates ? stats.rates[key].toLocaleString(tag, { maximumFractionDigits: 1 })
              + (key === 'af' || key === 'gap' ? '' : '%') : '—'}</td>)}</>}</tr>;
      })}</tbody>
    </table></div>
    {tab === 'result' && <PotBreakdown pots={game.result.pots} players={game.players} copy={copy} lang={lang}/>}
    {tab === 'stats' && <details className="rr-stats-guide"><summary>{copy.statsGuide}</summary>
      <dl>{Object.entries(metrics).map(([key, short]) =>
        <div key={key}><dt>{short}</dt><dd>{copy.metric[key]}</dd></div>)}</dl>
    </details>}
  </section>;
}
