import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/cormorant-garamond/latin-600.css';
import './room.css';
import Lobby from './Lobby.jsx';
import Table from './Table.jsx';

const request = (op, fields = {}) => window.poker.request(op, fields);

function Room() {
  const [game, setGame] = useState(undefined);
  const [lang, setLang] = useState('en');
  function changeLanguage(value) {
    if (value === lang) return;
    window.dispatchEvent(new Event('table-conceal'));
    document.documentElement.lang = value === 'zh' ? 'zh-CN' : 'en';
    setLang(value);
  }
  return <div className="room">
    <header className="room-header">
      <div className="room-brand"><span aria-hidden="true">♠</span> River Room</div>
      <div className="room-utilities"><span className="room-local">
        <i />{lang === 'en' ? 'Local table' : '本地牌桌'}</span>
        <div className="room-languages" role="group" aria-label={lang === 'en' ? 'Language' : '语言'}>
          {[['en','English'],['zh','中文']].map(([value,label]) =>
            <button key={value} lang={value} aria-pressed={value === lang}
              onClick={() => changeLanguage(value)}>{label}</button>)}
        </div>
      </div>
    </header>
    {game ? <Table initialGame={game} request={request} lang={lang}
        onEnd={() => setGame(undefined)} />
      : <Lobby lang={lang} request={request} onStart={setGame} />}
  </div>;
}

createRoot(document.getElementById('root')).render(<Room />);
