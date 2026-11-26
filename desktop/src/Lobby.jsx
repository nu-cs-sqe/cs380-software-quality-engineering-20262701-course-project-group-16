import React, {useCallback, useEffect, useRef, useState} from 'react';
import {AccessForm, PrivateAccount} from './LobbyAccount.jsx';
import {availableProfiles, choosePosition, emptySeats, minimumPlayerCount, nextUnreadySeat, occupy, readyPlayers, SEAT_COUNT} from './lobby-model.mjs';
import { DEFAULT_LOCALE, localeCatalog } from './locales.mjs';
import { TableFrame } from './TableSurface.jsx';
import './lobby.css';

function SeatSelection({t, seats, positions, slot, choose, busy}) {
  return <section className="rr-game rr-lobby-table" aria-label={t.pickSeat}>
    <TableFrame><div className="rr-center"><h1 id="lobby-title">{t.title}</h1>
      <p>{t.pickSeat}</p></div>
      {positions.map((position, index) => {
        const occupant = seats[index];
        const selected = index === slot;
        return <button type="button" key={position} data-position={position}
          className={'rr-seat rr-chair pos-' + position} aria-pressed={selected}
          disabled={busy || (!selected && Boolean(occupant.id))} onClick={() => choose(position)}>
          <small>{t.physicalSeat(position + 1)}</small>
          <strong>{occupant.name || (selected ? t.yourSeat : t.availableSeat)}</strong>
          {occupant.id && selected && <small>{t.yourSeat}</small>}
        </button>;
      })}
    </TableFrame>
  </section>;
}

function PlayerSlots({t, seats, count, slot, busy, onSelect}) {
  return <div className="rr-slots">{seats.map((seat, index) => {
    const human = index < count;
    const status = !human ? t.automatic : seat.buyIn ? t.ready : seat.id ? t.signed : t.waiting;
    const name = human ? seat.name || t.slot(index + 1) : t.computer;
    const content = <>
      <span className={'rr-slot-icon' + (human ? '' : ' rr-bot')} aria-hidden="true">
        {human ? String(index + 1).padStart(2, '0') : <span className="rr-bot-face"/>}
      </span>
      <span className="rr-slot-copy"><strong>{name}</strong><small>
        <i className={seat.buyIn ? 'rr-ready-dot' : ''}></i>{status}</small></span>
      {human && <span className="rr-select-arrow" aria-hidden="true">›</span>}
    </>;
    return human ? <button key={index} type="button" className={'rr-slot rr-human' + (seat.buyIn ? ' is-ready' : '')} disabled={busy}
      aria-pressed={slot === index} onClick={() => onSelect(index)}>{content}</button>
      : <div key={index} className="rr-slot rr-computer">{content}</div>;
  })}</div>;
}

export default function Lobby({lang = DEFAULT_LOCALE, request, onStart}) {
  const t = localeCatalog.get(lang).lobby;
  const [seats, setSeats] = useState(emptySeats);
  const [immediateFold, setImmediateFold] = useState(false);
  const [positions, setPositions] = useState(() => seats.map((_, i) => i));
  const [manualShowdown, setManualShowdown] = useState(false);
  const [showBots, setShowBots] = useState(false);
  const [count, setCount] = useState(1);
  const [slot, setSlot] = useState(0);
  const [profiles, setProfiles] = useState([]);
  const [loadState, setLoadState] = useState('pending');
  const [reload, setReload] = useState(0);
  const [unlocking, setUnlocking] = useState(false);
  const roster = useRef();
  const [account, setAccount] = useState();
  const [epoch, setEpoch] = useState(0);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const generation = useRef(0);
  const running = useRef(false);
  const mounted = useRef(false);
  const ready = readyPlayers(seats, positions);
  const seat = seats[slot];

  const conceal = useCallback(() => {
    generation.current++;
    setEpoch(generation.current);
    setAccount(undefined);
    setUnlocking(false);
    setError('');
  }, []);
  useEffect(() => {
    mounted.current = true;
    function visibility() { if (document.hidden) conceal(); }
    window.addEventListener('blur', conceal);
    window.addEventListener('table-conceal', conceal);
    document.addEventListener('visibilitychange', visibility);
    return () => {
      mounted.current = false;
      generation.current++;
      window.removeEventListener('blur', conceal);
      window.removeEventListener('table-conceal', conceal);
      document.removeEventListener('visibilitychange', visibility);
    };
  }, [conceal]);

  useEffect(() => {
    let current = true;
    setLoadState('pending');
    request('profiles').then(data => {
      if (current) { setProfiles(data); setLoadState('loaded'); }
    }).catch(() => { if (current) setLoadState('failed'); });
    return () => { current = false; };
  }, [request, reload]);

  function select(next) {
    if (next === slot) return;
    conceal();
    setSlot(next);
  }

  function replaceSeat(index, updated) {
    setSeats(old => old.map((value, at) => at === index ? updated : value));
  }

  async function perform(operation, action) {
    if (running.current) return;
    running.current = true;
    setBusy(true);
    setError('');
    const ticket = generation.current;
    try {
      await action(ticket);
    } catch (failure) {
      if (mounted.current && ticket === generation.current) {
        setError(failure.message === 'duplicate' ? 'duplicate' : operation);
      }
    } finally {
      running.current = false;
      if (mounted.current) setBusy(false);
    }
  }

  function authenticate(operation, fields) {
    return perform(operation, async ticket => {
      const authenticated = await request(operation, fields);
      if (!mounted.current) return;
      const occupant = occupy(seats, slot, authenticated);
      replaceSeat(slot, occupant);
      setProfiles(old => old.some(profile => profile.id === occupant.id) ? old :
        [...old, {id:occupant.id, name:occupant.name}]);
      if (ticket === generation.current) setAccount(authenticated);
    });
  }

  function confirmBuyIn(buyIn) {
    const updated = seats.map((entry, index) => index === slot ? {...entry, buyIn} : entry);
    setSeats(updated);
    if (!buyIn) return;
    conceal();
    const next = nextUnreadySeat(updated, count, slot);
    setSlot(next);
    roster.current.querySelectorAll('.rr-human')[next].focus();
  }

  function signOut() {
    return perform('logout', async () => {
      await request('logout', {id:seat.id});
      if (mounted.current) { replaceSeat(slot, emptySeats()[slot]); conceal(); }
    });
  }

  function start() {
    return perform('start', async () => {
      const game = await request('start', {seats:ready, immediateFold, showBots, manualShowdown});
      if (mounted.current) onStart(game);
    });
  }

  const available = availableProfiles(profiles, seats);
  return <main className="rr-lobby"><SeatSelection t={t} seats={seats} slot={slot}
    positions={positions} busy={busy} choose={pos =>
      setPositions(old => choosePosition(old, seats, slot, pos))}/>
    <div className="rr-lobby-grid">
      <section ref={roster} className="rr-panel rr-roster" aria-label={t.roster}>
        <div className="rr-roster-title"><h2>{t.roster}</h2>
          <div className="rr-ready-track" aria-hidden="true">{seats.map((entry, index) =>
            <i key={index} className={entry.buyIn ? 'filled' : index < count ? 'open' : ''}/>)}</div>
        </div>
        <div className="rr-count">
          <span id="rr-count-label">{t.local}</span>
          <div className="rr-stepper" role="group" aria-labelledby="rr-count-label">
            {seats.map((_, at) => <button type="button" key={at} aria-pressed={count === at + 1}
              disabled={busy || at + 1 < minimumPlayerCount(seats)} onClick={() => {
                setCount(at + 1); if (slot > at) select(at);
              }}>{at + 1}</button>)}
          </div>
        </div>
        <PlayerSlots t={t} seats={seats} count={count} slot={slot} busy={busy} onSelect={select}/>
        <details className="rr-house-rules"><summary>{t.disclosureRules}</summary>
          <label><input type="checkbox" checked={immediateFold}
            onChange={e => setImmediateFold(e.target.checked)}/>{t.earlyCards}</label>
          <label><input type="checkbox" checked={manualShowdown}
            onChange={e => setManualShowdown(e.target.checked)}/>{t.manualShowdown}</label>
          <label><input type="checkbox" checked={showBots}
            onChange={e => setShowBots(e.target.checked)}/>{t.botCards}</label>
        </details>
        <div className="rr-start"><div><strong>{t.readyCount(ready.length, SEAT_COUNT - ready.length)}</strong>
          <p>{t.startHelp}</p></div>
          <button type="button" className="rr-primary" onClick={start} disabled={busy || ready.length !== count}>
            {t.start}<span aria-hidden="true"> →</span></button>
        </div>
        {error === 'start' && <p className="rr-error" role="alert">{t.errors.start}</p>}
      </section>
      <section className="rr-panel rr-account" aria-label={t.access}>
        {loadState === 'pending' ? <p role="status">{t.loading}</p> :
          loadState === 'failed' ? <><p className="rr-error" role="alert">{t.errors.profiles}</p>
            <button type="button" className="rr-secondary" onClick={() => setReload(value => value + 1)}>{t.retry}</button></> :
          account ? <PrivateAccount key={account.id} lang={lang} account={account} seat={seat} slot={slot}
            request={request} onAccount={updated => {
              if (epoch === generation.current) setAccount(updated);
            }} onReady={confirmBuyIn}
            onLock={conceal} onLeave={signOut} busy={busy}/> :
          seat.buyIn && !unlocking ? <div className="rr-ready-receipt" role="status">
            <span className="rr-ready-seal" aria-hidden="true">✓</span>
            <h2>{t.seatReady(seat.name)}</h2><p>{t.readyPrivate}</p>
            <button className="rr-secondary" type="button" onClick={() => setUnlocking(true)}>{t.manageSeat}</button>
          </div> : <AccessForm key={slot + '-' + epoch} t={t} slot={slot} seat={seat} profiles={available}
            busy={busy} onAuth={authenticate}/>}
        {error && error !== 'start' && <p className="rr-error" role="alert">{t.errors[error]}</p>}
      </section>
    </div>
  </main>;
}
