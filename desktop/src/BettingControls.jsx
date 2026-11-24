import React, { useState } from 'react';
import { PlayingCard } from './TableSurface.jsx';
import { chipText, wagerPresets, wagerPreview, wagerScale } from './table-state.mjs';

// The Java engine accepts chip targets up to Long.MAX_VALUE.
const MAX_CHIP_DIGITS = 19;

function SharedHoleCards({ hole, name, conceal, copy }) {
  return (
    <>
      <div className="rr-pocket" data-testid="private-hand" aria-label={copy.cards}>
        {hole.map((card, index) => <PlayingCard key={index} card={card} copy={copy} />)}
      </div>
      <div className="rr-private-title">
        <small>{copy.personal}</small>
        <h2>{name}</h2>
        <button type="button" className="rr-link" onClick={conceal}>{copy.hide}</button>
      </div>
    </>
  );
}

function WagerCost({ preview, min, max, copy, lang }) {
  return (
    <div className="rr-wager-preview" id="rr-wager-preview" aria-live="polite">
      {preview ? (
        <>
          <span>{copy.addNow} <b>{chipText(preview.cost, lang)}</b></span>
          <span>{copy.leftBehind} <b>{chipText(preview.remaining, lang)}</b></span>
        </>
      ) : copy.sizingRange(chipText(min, lang), chipText(max, lang))}
    </div>
  );
}

function WagerSizing({
  legal, pot, committed, action, target, setTarget, preview, copy, lang, busy,
}) {
  const { min, max } = legal;
  const scale = wagerScale(min, max);
  const presets = wagerPresets(legal, pot, committed);
  const selectedTarget = preview && BigInt(target).toString();
  return (
    <div className="rr-sizing">
      <div className="rr-amount">
        <label htmlFor="rr-wager">{action === 'RAISE' ? copy.raiseTo : copy.betTo}</label>
        <div className="rr-amount-field">
          <button type="button" disabled={busy} onClick={() => setTarget(min)}>{copy.min}</button>
          <input id="rr-wager" inputMode="numeric" type="text" maxLength={MAX_CHIP_DIGITS}
            value={target} disabled={busy} onChange={event => setTarget(event.target.value)}
            spellCheck="false" aria-describedby="rr-wager-preview" aria-invalid={!preview} />
          <button type="button" disabled={busy} onClick={() => setTarget(max)}>{copy.max}</button>
        </div>
      </div>
      <div className="rr-sizing-tools">
        <input className="rr-wager-slider" type="range" aria-label={copy.wagerSlider}
          min={0} max={scale.steps} step={1} value={scale.positionOf(target)}
          disabled={busy || scale.steps === 0}
          aria-valuetext={chipText(selectedTarget ?? min, lang) + ' ' + copy.chips}
          onChange={event => setTarget(scale.targetAt(event.target.valueAsNumber))} />
        <div className="rr-presets">
          {presets.map(preset => (
            <button key={preset.label} type="button" disabled={busy}
              aria-pressed={selectedTarget === preset.target}
              title={chipText(preset.target, lang) + ' ' + copy.chips}
              onClick={() => setTarget(preset.target)}>{copy[preset.label]}</button>
          ))}
        </div>
      </div>
      <WagerCost preview={preview} min={min} max={max} copy={copy} lang={lang} />
    </div>
  );
}

export default function BettingControls({
  game, player, hole, copy, lang, submit, conceal, solo, busy,
}) {
  const { legal } = game;
  const [showFold, setShowFold] = useState(false);
  const [target, setTarget] = useState(legal.min);
  const canFold = legal.actions.includes('FOLD');
  const wagerAction = legal.actions.find(action => action === 'BET' || action === 'RAISE');
  const preview = wagerAction && wagerPreview(target, legal, player.round, player.stack);
  const allInCall = legal.call === player.stack;

  return (
    <section className="rr-actions rr-private" aria-label={copy.actions}>
      {!solo && <SharedHoleCards hole={hole} name={player.name} conceal={conceal} copy={copy} />}
      <div className="rr-bet-panel">
        {wagerAction && (
          <WagerSizing legal={legal} pot={game.pot} committed={player.round} action={wagerAction}
            target={target} setTarget={setTarget} preview={preview} copy={copy} lang={lang}
            busy={busy} />
        )}
        <div className="rr-action-buttons">
          {canFold && (
            <button type="button" className="rr-secondary fold" disabled={busy}
              onClick={() => submit({ action: 'FOLD', ...(showFold && { showFold }) })}>
              {copy.FOLD}
            </button>
          )}
          {legal.actions.includes('CHECK') && (
            <button type="button" className="rr-secondary" disabled={busy}
              onClick={() => submit({ action: 'CHECK' })}>{copy.CHECK}</button>
          )}
          {legal.actions.includes('CALL') && (
            <button type="button" className="rr-secondary" disabled={busy}
              onClick={() => submit({ action: 'CALL' })}>
              {copy.CALL} {chipText(legal.call, lang)}{allInCall && ' · ' + copy.ALL_IN}
            </button>
          )}
          {wagerAction && (
            <button type="button" className="rr-primary" disabled={busy || !preview}
              onClick={() => submit({ action: wagerAction, target: BigInt(target).toString() })}>
              {copy[wagerAction]}
            </button>
          )}
        </div>
        {canFold && (
          <label className="rr-disclosure-option">
            <input type="checkbox" checked={showFold} disabled={busy}
              onChange={event => setShowFold(event.target.checked)} />
            {game.immediateFold ? copy.foldShow : copy.foldDefer}
          </label>
        )}
      </div>
    </section>
  );
}
