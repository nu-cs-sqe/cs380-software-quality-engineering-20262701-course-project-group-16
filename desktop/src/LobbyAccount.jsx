import React, {useState} from 'react';
import {formatChips, openingBuyIn, validBuyIn} from './lobby-model.mjs';
import { localeCatalog } from './locales.mjs';
import Recovery from './Recovery.jsx';

export function AccessForm({t, slot, seat, profiles, busy, onAuth}) {
  const [mode, setMode] = useState(profiles.length || seat.id ? 'login' : 'register');
  const [choice, setChoice] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const login = mode === 'login' || Boolean(seat.id);
  const id = seat.id || choice || profiles[0]?.id || '';
  const allow = password.length && (login ? id : name.trim());

  function submit(event) {
    event.preventDefault();
    if (!allow || busy) return;
    const fields = login ? {id, password} : {name:name.trim(), password};
    setPassword('');
    onAuth(login ? 'login' : 'register', fields);
  }
  return <>
    <div className="rr-eyebrow">{t.slot(slot + 1)}</div>
    <h2>{seat.id ? seat.name : t.access}</h2>
    {seat.id ? <p className="rr-muted">{t.locked}</p> :
      <div className="rr-segments" role="group" aria-label={t.access}>
        {[['login', t.signIn], ['register', t.register]].map(([value, label]) =>
          <button key={value} type="button" aria-pressed={mode === value}
            onClick={() => { setMode(value); setPassword(''); }}>{label}</button>)}
      </div>}
    <form onSubmit={submit} className="rr-auth">
      {login && !seat.id && <label>{t.account}
        <select value={id} onChange={event => setChoice(event.target.value)} disabled={busy || !profiles.length}>
          {!profiles.length && <option value="">{t.choose}</option>}
          {profiles.map(profile => <option key={profile.id} value={profile.id}>{profile.label}</option>)}
        </select>
      </label>}
      {!login && <label>{t.displayName}
        <input value={name} required disabled={busy} autoComplete="off" placeholder={t.namePlaceholder}
          onChange={event => setName(event.target.value)}/>
      </label>}
      <label>{t.password}
        <input type="password" value={password} required autoComplete="off" disabled={busy}
          placeholder={t.passwordPlaceholder} onChange={event => setPassword(event.target.value)}/>
      </label>
      {!login && <small>{t.policy}</small>}
      {login && !seat.id && !profiles.length && <small>{t.noProfiles}</small>}
      <button type="submit" className="rr-primary" disabled={busy || !allow}>
        {seat.id ? t.unlock : login ? t.signIn : t.register}<span aria-hidden="true"> →</span>
      </button>
    </form>
    <p className="rr-private-note"><span aria-hidden="true">◇</span>{t.shared}</p>
  </>;
}

function Statistics({account, lang, t}) {
  const {handsPlayed, rates} = account.metrics;
  const { tag } = localeCatalog.get(lang);
  return <div className="rr-metrics">
    <dl><div><dt>{t.hands}</dt><dd>{handsPlayed.toLocaleString(tag)}</dd></div>
      {Object.entries(t.metrics).map(([key, label]) => {
        const suffix = key === 'af' ? '' : key === 'gap' ? t.gapUnit : '%';
        const rate = rates[key];
        return <div key={key}><dt>{label}</dt>
          <dd>{rate === undefined ? <abbr title={t.noRate}>—</abbr>
            : rate.toLocaleString(tag, {maximumFractionDigits:2}) + suffix}</dd>
        </div>;
      })}
    </dl>
  </div>;
}

function History({account, lang, t}) {
  if (!account.history.length) return <p className="rr-muted">{t.noHistory}</p>;
  const dateFormat = new Intl.DateTimeFormat(localeCatalog.get(lang).tag, {
    dateStyle: 'short', timeStyle: 'short',
  });
  const signed = value => (BigInt(value) > 0n ? '+' : '') + formatChips(value, lang);
  return <div className="rr-history">{account.history.map((row, index) =>
    <article key={row.handId || row.timestamp + '-' + index}>
      <div><strong>{row.type === 'RECOVERY_REWARD' ? t.reward : t.hand}</strong>
        <time dateTime={row.timestamp}>{dateFormat.format(new Date(row.timestamp))}</time></div>
      <b className={BigInt(row.change) > 0n ? 'rr-positive' : ''}>{signed(row.change)}</b>
      <small>{t.after} · {formatChips(row.balance, lang)} {t.chips}
        {row.sessionNet !== undefined && ' · ' + t.net + ' ' + signed(row.sessionNet)}</small>
    </article>)}</div>;
}

export function PrivateAccount({lang, account, seat, slot, request, onAccount, onReady,
  onLock, onLeave, busy}) {
  const t = localeCatalog.get(lang).lobby;
  const [tab, setTab] = useState('dashboard');
  const [amount, setAmount] = useState(seat.buyIn || openingBuyIn(account.balance));
  const [error, setError] = useState(false);
  const ready = Boolean(seat.buyIn);
  function confirm(event) {
    event.preventDefault();
    if (ready) {
      onReady('');
      return;
    }
    const buyIn = amount || openingBuyIn(account.balance);
    if (!validBuyIn(buyIn, account.balance)) {
      setError(true);
      return;
    }
    onReady(BigInt(buyIn).toString());
    setError(false);
  }

  return <>
    <div className="rr-panel-heading"><div className="rr-eyebrow">{t.slot(slot + 1)} · {t.private}</div>
      <button type="button" className="rr-text" onClick={onLock}>{t.lock}</button></div>
    <h2>{account.name}</h2>
    {tab === 'recovery' ?
      <Recovery id={account.id} lang={lang} request={request} onAccount={onAccount}
        onClose={() => setTab('dashboard')}/> : <>
        <div className="rr-tabs" role="group" aria-label={t.private}>
          {['dashboard', 'statistics', 'history'].map(key => <button key={key} type="button"
            aria-pressed={tab === key} onClick={() => setTab(key)}>{t[key]}</button>)}
        </div>
        {tab === 'dashboard' && <>
          <div className="rr-balance"><span className="rr-muted">{t.balance}</span>
            <strong>{formatChips(account.balance, lang)} <small>{t.chips}</small></strong></div>
          <form className="rr-auth" onSubmit={confirm}>
            <label>{t.buyIn}<input type="text" inputMode="numeric" value={amount} disabled={ready}
              onChange={event => { setAmount(event.target.value); setError(false); }}/></label>
            <small>{ready ? t.confirmReady : t.buyHelp}</small>
            {error && <p role="alert" className="rr-error">{t.errors.buyIn}</p>}
            <button type="submit" className={ready ? 'rr-secondary' : 'rr-primary'} disabled={busy}>
              {ready ? t.unready : t.markReady}</button>
          </form>
          <button className="rr-recover-link rr-text" type="button" onClick={() => setTab('recovery')}>
            {t.recoverLink}<span aria-hidden="true"> ↗</span></button>
        </>}
        {tab === 'statistics' && <Statistics account={account} lang={lang} t={t}/>}
        {tab === 'history' && <History account={account} lang={lang} t={t}/>}
      </>}
    <div className="rr-signout"><small>{t.accountId} · {account.id}</small><button type="button" className="rr-text" onClick={onLeave}
      disabled={busy}>{t.leave}</button></div>
  </>;
}
