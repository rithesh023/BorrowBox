import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { ErrorNotice } from '../components/Notice.jsx';

export default function AuthPage({ mode, adminOnly = false }) {
  const registering = mode === 'register';
  const { user, adminSession, login, adminLogin, register, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ name: '', email: '', password: '', phone: '' });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (adminOnly && user?.role === 'ADMIN' && adminSession) return <Navigate to="/admin" replace />;
  if (user && !adminOnly) return <Navigate to={location.state?.from || '/dashboard'} replace />;

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const authenticatedUser = registering
        ? await register(form)
        : adminOnly
          ? await adminLogin(form.email, form.password)
          : await login(form.email, form.password);
      if (adminOnly && authenticatedUser.role !== 'ADMIN') {
        logout();
        setError({ message: 'This account does not have administrator access. Use an administrator account.' });
        return;
      }
      navigate(adminOnly ? (location.state?.from || '/admin') : (location.state?.from || '/dashboard'), { replace: true });
    } catch (caught) {
      setError(caught);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link to="/" className="brand"><span className="brand-mark">b</span>borrowbox<span className="brand-period">.</span></Link>
        <p className="eyebrow">{adminOnly ? 'BORROWBOX ADMINISTRATION' : 'CAMPUS RENTALS, MADE EASY'}</p>
        <h1>{adminOnly ? 'Administrator sign in.' : registering ? 'Join your campus community.' : 'Welcome back.'}</h1>
        <p className="auth-subtitle">
          {adminOnly
            ? 'Sign in with an administrator account to manage listings and platform activity.'
            : registering
              ? 'Create an account to borrow and share useful gear.'
              : 'Log in to manage your rentals and listings.'}
        </p>
        <ErrorNotice error={error} />
        {adminOnly && user && (
          <div className="admin-session-note">
            <span>Currently signed in as {user.email}. Enter administrator credentials to open the control room.</span>
            <button type="button" className="text-button" onClick={logout}>Sign out this account</button>
          </div>
        )}
        <form onSubmit={submit}>
          {registering && <label>Your name<input required maxLength="100" autoComplete="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>}
          <label>Email<input required type="email" maxLength="190" autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
          {registering && <label>Phone <span className="optional-label">(optional)</span><input maxLength="30" autoComplete="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>}
          <label>Password<input required type="password" minLength={registering ? 8 : undefined} maxLength="72" autoComplete={registering ? 'new-password' : 'current-password'} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />{registering && <small>Use at least 8 characters.</small>}</label>
          <button className="primary-button auth-submit" disabled={busy}>
            {busy ? 'Please wait…' : registering ? 'Create account' : adminOnly ? 'Sign in as administrator' : 'Log in'}
          </button>
        </form>
        {adminOnly ? (
          <>
            <p className="auth-switch">Student account? <Link to="/login">Student sign in</Link></p>
            <p className="auth-switch">First admin setup: set <code>ADMIN_EMAIL</code> and <code>ADMIN_PASSWORD</code> in <code>backend/.env</code>, save the file, and restart the backend.</p>
          </>
        ) : (
          <>
            <p className="auth-switch">
              {registering ? 'Already have an account?' : 'New to BorrowBox?'}{' '}
              <Link to={registering ? '/login' : '/register'}>{registering ? 'Log in' : 'Create an account'}</Link>
            </p>
            {!registering && <p className="auth-switch"><Link to="/admin/login">Administrator sign in</Link></p>}
          </>
        )}
      </section>
      <aside className="auth-aside"><span>✳</span><p>Borrow local.<br />Do more.</p><small>A little sharing goes a long way on campus.</small></aside>
    </main>
  );
}
