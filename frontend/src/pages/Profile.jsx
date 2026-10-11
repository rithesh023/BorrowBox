import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import { ErrorNotice, SuccessNotice } from '../components/Notice.jsx';

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [name, setName] = useState(user.name || '');
  const [phone, setPhone] = useState(user.phone || '');
  const [error, setError] = useState(null);
  const [saved, setSaved] = useState(false);
  async function submit(event) {
    event.preventDefault(); setError(null); setSaved(false);
    try { const { data } = await api.put('/auth/me', { name, phone }); updateUser(data); setSaved(true); }
    catch (caught) { setError(caught); }
  }
  return <main className="page-wrap form-page"><Link to="/dashboard" className="back-link">← Dashboard</Link><div className="page-heading"><p className="eyebrow">YOUR ACCOUNT</p><h1>Profile</h1><p>Update the name and phone shown to other students.</p></div>
    <form className="listing-form" onSubmit={submit}><ErrorNotice error={error} /><SuccessNotice>{saved ? 'Profile saved.' : ''}</SuccessNotice><label>Full name<input required maxLength="100" value={name} onChange={(e) => setName(e.target.value)} /></label><label>Email<input value={user.email} disabled /></label><label>Phone <span className="optional-label">(optional)</span><input maxLength="30" value={phone} onChange={(e) => setPhone(e.target.value)} /></label><p className="form-help">Account role: {user.role === 'ADMIN' ? 'Administrator' : 'Student'}.</p><button className="primary-button">Save profile</button></form>
  </main>;
}
