import React, {useEffect, useRef, useState} from 'react';
import {lobbyLanguage} from './lobby-language.mjs';
import './lobby.css';

export default function Recovery({lang = 'en', id, request, onAccount, onClose}) {
  const t = lobbyLanguage[lang];
  const [attempt, setAttempt] = useState(0);
  const [challenge, setChallenge] = useState();
  const [answers, setAnswers] = useState({});
  const [outcome, setOutcome] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const active = useRef(false);
  const sending = useRef(false);
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);

  useEffect(() => {
    if (outcome) return;
    let current = true;
    setChallenge(undefined);
    setError('');
    request('recovery', {id, locale: lang}).then(data => {
      if (current) setChallenge(data);
    }).catch(() => { if (current) setError('recovery'); });
    return () => { current = false; };
  }, [id, lang, request, attempt, outcome]);

  async function submit(event) {
    event.preventDefault();
    if (sending.current) return;
    sending.current = true;
    setSubmitting(true);
    setError('');
    try {
      const ordered = challenge.questions.map(question => answers[question.id]);
      const result = await request('claim', {id, challengeId:challenge.challengeId, answers:ordered});
      if (active.current) {
        setOutcome(result.outcome);
        onAccount(result.account);
      }
    } catch {
      if (active.current) setError('claim');
    } finally {
      sending.current = false;
      if (active.current) setSubmitting(false);
    }
  }

  function retry() {
    setOutcome('');
    setAnswers({});
    setAttempt(value => value + 1);
  }

  return <section className="rr-quiz" aria-label={t.recovery}>
    <div className="rr-panel-heading"><h3>{t.recovery}</h3>
      <button type="button" className="rr-text" onClick={onClose}>{t.back}</button>
    </div>
    <p className="rr-muted">{t.quizIntro}</p>
    {error && <p className="rr-error" role="alert">{t.errors[error]}</p>}
    {error === 'recovery' && <button className="rr-secondary" type="button" onClick={retry}>{t.retry}</button>}
    {outcome ? <div className="rr-result">
      <p role="status">{t.results[outcome]}</p>
      <button className="rr-secondary" type="button" onClick={retry}>{outcome === 'WRONG_ANSWERS' ? t.retry : t.newQuiz}</button>
    </div> : challenge ? <form onSubmit={submit}>
      {challenge.questions.map((question, index) => <fieldset key={question.id} disabled={submitting}>
        <legend>{index + 1}. {question.prompt}</legend>
        {question.choices.map((choice, option) => <label key={option}>
          <input type="radio" name={question.id} checked={answers[question.id] === option}
            onChange={() => setAnswers(old => ({...old, [question.id]:option}))}/>{choice}
        </label>)}
      </fieldset>)}
      <p className="rr-muted">{t.quizProgress(Object.keys(answers).length, challenge.questions.length)}</p>
      <button className="rr-primary" disabled={submitting || !challenge.questions.every(q => q.id in answers)}>
        {t.submit}
      </button>
    </form> : !error && <p role="status">{t.loading}</p>}
  </section>;
}
