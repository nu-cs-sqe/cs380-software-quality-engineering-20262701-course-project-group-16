import { useCallback, useEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { handoffGate, needsSettlement, publicSnapshot, sameTurn } from './table-state.mjs';

const PRIVATE_REQUESTS = new Set(['reveal', 'handoff', 'switchOwner']);

export function useTableRequests({
  initialGame, request, onEnd, soloAccount, onTurnChanged, onConcealed, onRevealed,
}) {
  const [snapshot, setSnapshot] = useState(() => publicSnapshot(initialGame));
  const [hole, setHole] = useState([]);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState();
  const [failedSaveHand, setFailedSaveHand] = useState('');
  const lastAutoSave = useRef('');
  const latest = useRef(snapshot);
  const gate = useRef(handoffGate());
  const privatePending = useRef(false);
  const locked = useRef(false);
  const hiding = useRef(Promise.resolve());
  const mounted = useRef(false);
  const api = useRef({ request, onEnd });
  api.current = { request, onEnd };

  const adopt = useCallback(response => {
    if (!mounted.current) return;
    if (!sameTurn(latest.current, response)) {
      onTurnChanged(response, latest.current.handId !== response.handId);
    }
    latest.current = publicSnapshot(response);
    setSnapshot(latest.current);
  }, [onTurnChanged]);

  const clearPrivate = useCallback(() => {
    gate.current.conceal();
    privatePending.current = false;
    setHole([]);
  }, []);

  const conceal = useCallback((lockOwner = true) => {
    const shouldHide = lockOwner && (!soloAccount || privatePending.current);
    flushSync(() => {
      clearPrivate();
      onConcealed(latest.current);
    });
    if (shouldHide) {
      const hidden = api.current.request('hide');
      const pending = hidden.then(response => {
        if (hiding.current === pending && sameTurn(latest.current, response)) adopt(response);
      }).catch(() => {
        if (mounted.current && hiding.current === pending) {
          setFailure('hide');
        }
      });
      hiding.current = pending;
    }
  }, [adopt, clearPrivate, onConcealed, soloAccount]);

  useEffect(() => {
    mounted.current = true;
    const focusLost = () => conceal();
    const visibility = () => { if (document.hidden) conceal(); };
    window.addEventListener('table-conceal', focusLost);
    window.addEventListener('blur', focusLost);
    document.addEventListener('visibilitychange', visibility);
    return () => {
      mounted.current = false;
      gate.current.conceal();
      window.removeEventListener('table-conceal', focusLost);
      window.removeEventListener('blur', focusLost);
      document.removeEventListener('visibilitychange', visibility);
      if (privatePending.current) void api.current.request('hide').catch(() => {});
    };
  }, [conceal]);

  const refresh = useCallback(async () => {
    const observed = latest.current;
    try {
      const response = await api.current.request('state');
      if (latest.current === observed) adopt(response);
    } catch {
      if (mounted.current && latest.current === observed) {
        setFailure('state');
      }
    }
  }, [adopt]);

  const applyResponse = useCallback((op, turn, response) => {
    if (op === 'end') {
      api.current.onEnd();
    } else if (op === 'reveal') {
      if (!sameTurn(turn, response)) return false;
      adopt(response);
      setHole(response.hole);
      onRevealed();
    } else {
      adopt(response);
    }
    return true;
  }, [adopt, onRevealed]);

  const execute = useCallback(async (op, fields = {}) => {
    if (locked.current) return false;
    locked.current = true;
    const turn = latest.current;
    if (op === 'act') {
      flushSync(() => {
        setBusy(true);
        clearPrivate();
      });
    } else if (op !== 'reveal') {
      clearPrivate();
    }
    const privateRequest = PRIVATE_REQUESTS.has(op);
    const ticket = gate.current.begin(turn);
    privatePending.current = privateRequest;
    const stillPrivate = () => gate.current.accepts(ticket, latest.current) && !document.hidden;

    setBusy(true);
    setFailure(undefined);
    if (op === 'settle') setFailedSaveHand('');
    try {
      await hiding.current;
      if (!mounted.current || (privateRequest && !stillPrivate())) return false;
      const response = await api.current.request(op, {
        ...fields, handId: turn.handId, revision: turn.revision,
      });
      if (!mounted.current || (privateRequest && !stillPrivate())) return false;
      return applyResponse(op, turn, response);
    } catch {
      if (!mounted.current) return false;
      clearPrivate();
      await refresh();
      if (mounted.current) {
        if (op === 'settle') setFailedSaveHand(turn.handId);
        else setFailure(op);
      }
      return false;
    } finally {
      if (op !== 'reveal') privatePending.current = false;
      locked.current = false;
      if (mounted.current) setBusy(false);
    }
  }, [applyResponse, clearPrivate, refresh]);

  const attemptSettlement = useCallback(game => {
    if (locked.current || !needsSettlement(game, lastAutoSave.current)) return;
    lastAutoSave.current = game.handId;
    void execute('settle');
  }, [execute]);

  return {
    snapshot, hole, busy, failure, failedSaveHand, conceal, execute, refresh, attemptSettlement,
    dismissFailure: () => setFailure(undefined),
  };
}
