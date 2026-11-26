import React, {useEffect, useRef, useState} from 'react';
import {CardBacks, PlayingCard} from './TableSurface.jsx';

// The engine checks the active account for every private request. Private cards
// live only in this component, never in a public snapshot or the action journal.
export default function OwnerHand({game, player, copy, request, execute, busy, solo, close}) {
  const [visible, setVisible] = useState(solo && document.hasFocus() && !document.hidden);
  const [holding, setHolding] = useState();
  const [needsLogin, setNeedsLogin] = useState(false);
  const [error, setError] = useState('');
  const credential = useRef();
  const generation = useRef(0);
  const account = player.accountId;
  const authorized = account === game.privateOwner && !needsLogin;
  const fields = {handId:game.handId, revision:game.revision, id:account};
  const visibility = game.visibility[player.id];
  const folded = game.folded.includes(player.id);
  const optional = folded || (game.result?.uncontested && game.result.winners.includes(player.id));
  const showdown = game.showActor === player.id;
  const canChoose = showdown || (optional && ['HIDDEN','MUCKED'].includes(visibility));

  function hide() {
    generation.current++;
    setHolding(undefined);
    setVisible(false);
    if (credential.current) credential.current.value = '';
  }
  useEffect(() => {
    const visibilityChange = () => { if (document.hidden) hide(); };
    window.addEventListener('blur', hide);
    window.addEventListener('table-conceal', hide);
    document.addEventListener('visibilitychange', visibilityChange);
    return () => {
      generation.current++;
      window.removeEventListener('blur', hide);
      window.removeEventListener('table-conceal', hide);
      document.removeEventListener('visibilitychange', visibilityChange);
    };
  }, []);
  useEffect(() => {
    if (!visible || !authorized || !visibility) return;
    const ticket = ++generation.current;
    let timer;
    request('inspect', fields).then(data => {
      if (ticket !== generation.current || document.hidden || !document.hasFocus()) return;
      setHolding(data);
      timer = setTimeout(hide, data.validForMillis);
    }).catch(err => {
      if (ticket !== generation.current) return;
      setHolding(undefined);
      if (err.message.includes('Sign in')) setNeedsLogin(true);
      else setError(copy.cardAccessError);
    });
    return () => { generation.current++; clearTimeout(timer); };
  }, [visible, authorized, visibility, game.handId, game.revision, account, request]);

  async function unlock(event) {
    event.preventDefault();
    const fields = {id:account, password:credential.current.value};
    credential.current.value = '';
    if (await execute('switchOwner', fields)) {
      setNeedsLogin(false);
      setVisible(true);
      setError('');
    } else setError(copy.cardAccessError);
  }
  async function choose(choice) {
    if (await execute('disclose', {id:account, choice})) {
      setError('');
      close?.();
    } else setError(copy.cardAccessError);
  }
  if (!visibility) return null;
  return <section className="rr-owner-hand" aria-label={copy.personal}>
    <div><small>{copy.personal}</small><strong>{player.name}</strong></div>
    {!authorized ? <form onSubmit={unlock}>
      <label>{copy.password}<input type="password" required ref={credential} autoComplete="off"/></label>
      <button className="rr-secondary" disabled={busy}>{copy.signInSeat}</button>
    </form> : <>
      <div className="rr-pocket" data-testid={visible && holding ? 'private-hand' : undefined}>
        {visible && holding ? holding.cards.map((card,i) =>
          <PlayingCard key={i} card={card} copy={copy}/>) : <CardBacks/>}
      </div>
      <button type="button" className="rr-link" onClick={() => visible ? hide() : setVisible(true)}>
        {visible ? copy.hide : copy.inspect}</button>
      {canChoose && <div className="rr-owner-choices">
        <small>{showdown ? copy.claimNote : holding?.deferred ? copy.queued : copy.privateDefault}</small>
        <button className="rr-secondary" type="button" disabled={busy}
          onClick={() => choose('SHOW')}>{folded && !game.immediateFold && !game.result
            ? copy.deferShow : copy.showCards}</button>
        <button className="rr-link" type="button" disabled={busy}
          onClick={() => choose('MUCK')}>{copy.muck}</button>
      </div>}
    </>}
    {error && <small role="alert">{error}</small>}
  </section>;
}
