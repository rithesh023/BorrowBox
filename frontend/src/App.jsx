import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext.jsx';
import Navbar from './components/Navbar.jsx';
import Chatbot from './components/Chatbot.jsx';
import Home from './pages/Home.jsx';
import ItemsPage from './pages/ItemsPage.jsx';
import ItemDetail from './pages/ItemDetail.jsx';
import ListingForm from './pages/ListingForm.jsx';
import AuthPage from './pages/AuthPage.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Profile from './pages/Profile.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';

function RequireAuth({ children, admin = false }) {
  const { user, ready } = useAuth();
  if (!ready) return <main className="page-wrap"><p className="loading">Loading your account…</p></main>;
  if (!user) return <Navigate to="/login" replace state={{ from: window.location.pathname }} />;
  if (admin && user.role !== 'ADMIN') return <main className="page-wrap"><div className="empty-results"><h2>Admin access required</h2><p>To enable this account, open <code>backend/.env</code> and set <code>ADMIN_EMAIL</code> to the exact email used to sign in. Set <code>ADMIN_PASSWORD</code> to at least 12 characters, save the file, restart the backend, then log out and back in.</p><p>If this account already exists, continue signing in with its current password; <code>ADMIN_PASSWORD</code> only sets the password when the bootstrap creates a new account.</p><Link to="/" className="primary-button inline-button">Back home</Link></div></main>;
  return children;
}

function RequireAdmin({ children }) {
  const { user, ready, adminSession } = useAuth();
  if (!ready) return <main className="page-wrap"><p className="loading">Checking administrator access…</p></main>;
  if (!user || !adminSession || user.role !== 'ADMIN') return <Navigate to="/admin/login" replace state={{ from: '/admin' }} />;
  return children;
}

function Shell() {
  const { user } = useAuth();
  return <div className="shell"><Navbar /><Routes>
    <Route path="/" element={<Home />} />
    <Route path="/items" element={<ItemsPage />} />
    <Route path="/items/new" element={<RequireAuth><ListingForm /></RequireAuth>} />
    <Route path="/items/:id/edit" element={<RequireAuth><ListingForm /></RequireAuth>} />
    <Route path="/items/:id" element={<ItemDetail />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/admin/login" element={<AuthPage mode="login" adminOnly />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route path="/dashboard" element={<RequireAuth><Dashboard /></RequireAuth>} />
    <Route path="/profile" element={<RequireAuth><Profile /></RequireAuth>} />
    <Route path="/admin" element={<RequireAdmin><AdminDashboard /></RequireAdmin>} />
    <Route path="/admin/items/:id/edit" element={<RequireAdmin><ListingForm adminMode /></RequireAdmin>} />
    <Route path="*" element={<main className="page-wrap empty-results"><h1>Page not found</h1><Link className="primary-button inline-button" to="/">Go home</Link></main>} />
  </Routes><footer><span>© 2026 BorrowBox</span><span>Made for campus, shared with care.</span><span>Rent less. Do more. <b>✳</b></span></footer><Chatbot /></div>;
}

export default function App() {
  return <AuthProvider><Shell /></AuthProvider>;
}
