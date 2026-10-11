import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api.js';
import { ErrorNotice } from './Notice.jsx';
import './Chatbot.css';

export default function Chatbot() {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([{ from: 'bot', text: 'Hi! Tell me what you need for campus, or ask how BorrowBox works.' }]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  async function ask(message) {
    message = message.trim();
    if (!message || busy) return;
    setMessages((current) => [...current, { from: 'you', text: message }]);
    setInput(''); setBusy(true); setError(null);
    try {
      const { data } = await api.post('/chat', { message });
      setMessages((current) => [...current, { from: 'bot', text: data.message, items: data.items || [], aiStatus: data.aiStatus }]);
    } catch (caught) { setError(caught); }
    finally { setBusy(false); }
  }

  function send(event) {
    event.preventDefault();
    ask(input);
  }

  return <div className="chat-widget">
    {open && <section className="chat-panel" aria-label="BorrowBox assistant">
      <header><div><small>BORROWBOX AI</small><strong>Campus helper</strong></div><button onClick={() => setOpen(false)} aria-label="Close assistant">×</button></header>
      <div className="chat-messages">{messages.map((message, index) => <div className={`chat-message ${message.from}`} key={index}><p>{message.text}</p>{message.aiStatus === 'unavailable' && <small className="chat-ai-status">AI replies are unavailable right now. Item matches still come from live listings.</small>}{message.aiStatus === 'not_configured' && <small className="chat-ai-status">AI replies are not configured. Live listing search still works.</small>}{message.items?.map((item) => <Link className="chat-result" to={`/items/${item.id}`} key={item.id}><span>{item.name}<small>{item.category} · deposit ₹{Number(item.securityDeposit).toLocaleString('en-IN')}</small></span><b>₹{Number(item.pricePerDay).toLocaleString('en-IN')}/day</b></Link>)}</div>)}{messages.length === 1 && <div className="chat-suggestions" aria-label="Example questions">{['Suggest badminton gear', 'Camera under ₹400', 'How does borrowing work?'].map((prompt) => <button type="button" key={prompt} disabled={busy} onClick={() => ask(prompt)}>{prompt}</button>)}</div>}{busy && <div className="chat-message bot"><p>Checking live listings…</p></div>}<ErrorNotice error={error} /></div>
      <form onSubmit={send} className="chat-input"><input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Ask for an item…" maxLength={500} /><button disabled={busy || !input.trim()}>Send</button></form>
      <p className="chat-footnote">Item suggestions come from current listings. Ask about prices, deposits, or renting.</p>
    </section>}
    <button className="chat-toggle" onClick={() => setOpen((value) => !value)} aria-label={open ? 'Close BorrowBox AI' : 'Open BorrowBox AI'}>{open ? '×' : '✳'}<span>{open ? '' : 'Ask BorrowBox'}</span></button>
  </div>;
}
