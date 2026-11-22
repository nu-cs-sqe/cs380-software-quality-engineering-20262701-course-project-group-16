import React, { useEffect, useRef, useState } from 'react';
import Recovery from './Recovery.jsx';
import { chipText, remainingHumans, validTarget } from './table-state.mjs';

export default function TableOptions({ game, copy, lang, request, execute, refresh, close, busy, error, initialPage }) {
  const dialog = useRef();
  const [page, setPage] = useState(initialPage);
  const [id, setId] = useState('');
  const [amount, setAmount] = useState('');
  const humans = remainingHumans(game.players);
  const person = humans.find(p => p.accountId === id);
  const funds = game.funds[id];

  useEffect(() => {
    const element = dialog.current;
    element.showModal();
    return () => element.close();
  }, []);

  function select(p) {
    setId(p.accountId);
    setAmount(game.funds[p.accountId]?.min || '');
    setPage('confirm');
  }

  async function changeSeat(op, fields) {
    const ok = await execute(op, { id, ...fields });
    if (ok) close();
  }

  const back = <button type="button" className="rr-secondary" disabled={busy}
    onClick={() => setPage('seats')}>{copy.back}</button>;
  return <dialog ref={dialog} className="rr-dialog" aria-label={copy.manage}
    onCancel={event => { event.preventDefault(); close(); }}>
    <div className="rr-dialog-heading"><span className="rr-kicker">{copy.manage}</span>
      <button type="button" className="rr-x" aria-label={copy.close} onClick={close}>×</button></div>
    {page === 'seats' && <>
      <h2>{copy.choose}</h2><p>{copy.manageNote}</p>
      <div className="rr-seat-list">{humans.map(p => <button type="button" key={p.id} onClick={() => select(p)}>
        <span>{p.name}</span><small>{copy.seat} {p.seat + 1}　›</small></button>)}</div>
      {game.paid && <button type="button" className="rr-secondary" onClick={() => setPage('end')}>{copy.end}</button>}
    </>}
    {page === 'end' && <>
      <h2>{copy.endConfirm}</h2><p>{copy.endNote}</p>
      <div className="rr-row">{back}<button className="rr-primary" type="button" disabled={busy}
        onClick={() => execute('end')}>{copy.paidReturn}</button></div>
    </>}
    {page === 'confirm' && person && <>
      <h2>{copy.pass(person.name)}</h2><p>{copy.privacy}</p>
      <div className="rr-row">{back}<button type="button" className="rr-primary"
        onClick={() => setPage('funds')}>{copy.authorize(person.name)}</button></div>
    </>}
    {page === 'funds' && person && <>
      <h2>{person.name}</h2>
      {funds && <dl className="rr-funds">{['tableChips', 'wallet', 'balance', 'sessionNet'].map((label, i) =>
        <div key={label}><dt>{copy[label]}</dt><dd>{chipText(funds[['tableChips', 'available', 'balance', 'net'][i]], lang)}</dd></div>)}</dl>}
      {game.paid && funds && <>
        {funds.needsRecovery && <p>{copy.needsRecovery}</p>}
        {funds.needsTopUp && <p>{copy.needsTopUp}</p>}
        {BigInt(funds.max) >= BigInt(funds.min) && <form className="rr-topup" onSubmit={event => {
          event.preventDefault(); if (validTarget(amount, funds.min, funds.max)) changeSeat('topup', { amount: BigInt(amount).toString() });
        }}>
          <label>{copy.amount}<input inputMode="numeric" maxLength={19} value={amount}
            onChange={event => setAmount(event.target.value)} /></label>
          <small>{copy.limits(chipText(funds.min, lang), chipText(funds.max, lang))}</small>
          <button type="submit" className="rr-primary" disabled={busy || !validTarget(amount, funds.min, funds.max)}>{copy.topup}</button>
        </form>}
        {funds.needsRecovery && <button type="button" className="rr-primary"
          onClick={() => setPage('recover')}>{copy.recovery}</button>}
      </>}
      <div className="rr-row">{back}<button type="button" className="rr-secondary fold"
        onClick={() => setPage('leave')}>{copy.leave}</button></div>
    </>}
    {page === 'leave' && person && <>
      <h2>{copy.leaveConfirm(person.name)}</h2><p>{copy.leaveNote}</p>
      <div className="rr-row">{back}<button className="rr-secondary fold" type="button"
        disabled={busy} onClick={() => changeSeat('quit', {})}>{copy.leave}</button></div>
    </>}
    {page === 'recover' && person && <Recovery key={id} lang={lang} id={id} request={request}
      onAccount={refresh} onClose={() => setPage('funds')} />}
    {error && <p role="alert" className="rr-error">{error}</p>}
  </dialog>;
}
