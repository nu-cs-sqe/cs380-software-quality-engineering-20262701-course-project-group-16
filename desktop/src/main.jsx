import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/cormorant-garamond/latin-600.css';
import './room.css';
import Lobby from './Lobby.jsx';
import Table from './Table.jsx';
import { DEFAULT_LOCALE, localeCatalog } from './locales.mjs';

const request = (op, fields = {}) => window.poker.request(op, fields);
document.documentElement.lang = localeCatalog.get(DEFAULT_LOCALE).tag;

function Room() {
  const [game, setGame] = useState(undefined);
  const [lang, setLang] = useState(DEFAULT_LOCALE);
  const { room } = localeCatalog.get(lang);
  function changeLanguage(value) {
    if (value === lang) return;
    window.dispatchEvent(new Event('table-conceal'));
    document.documentElement.lang = localeCatalog.get(value).tag;
    setLang(value);
  }
  return <div className="room">
    <header className="room-header">
      <div className="room-brand"><span aria-hidden="true">♠</span> River Room</div>
      <div className="room-utilities"><span className="room-local">
        <i />{room.local}</span>
        <div className="room-languages" role="group" aria-label={room.language}>
          {localeCatalog.available.map(({ id, tag, label }) =>
            <button key={id} lang={tag} aria-pressed={id === lang}
              onClick={() => changeLanguage(id)}>{label}</button>)}
        </div>
      </div>
    </header>
    {game ? <Table initialGame={game} request={request} lang={lang}
        onEnd={() => setGame(undefined)} />
      : <Lobby lang={lang} request={request} onStart={setGame} />}
  </div>;
}

createRoot(document.getElementById('root')).render(<Room />);
