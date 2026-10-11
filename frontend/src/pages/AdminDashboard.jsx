import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import api from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import { ErrorNotice, SuccessNotice } from '../components/Notice.jsx';

const tabs = ['Overview', 'Users', 'Listings', 'Rentals', 'Payments'];
export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [tab, setTab] = useState(location.state?.adminTab || 'Overview');
  const [summary, setSummary] = useState(null);
  const [users, setUsers] = useState([]);
  const [items, setItems] = useState([]);
  const [rentals, setRentals] = useState([]);
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [deletingItemId, setDeletingItemId] = useState(null);
  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [s, u, i, r, p] = await Promise.all([api.get('/admin/summary'), api.get('/admin/users'), api.get('/admin/items'), api.get('/admin/rentals'), api.get('/admin/payments')]);
      setSummary(s.data); setUsers(u.data); setItems(i.data); setRentals(r.data); setPayments(p.data);
    } catch (caught) { setError(caught); }
  }, []);
  useEffect(() => { refresh(); }, [refresh]);
  useEffect(() => { if (location.state?.adminTab) setTab(location.state.adminTab); }, [location.key, location.state]);
  async function update(url, params, message) {
    setError(null); setNotice('');
    try { await api.put(url, null, { params }); setNotice(message); await refresh(); }
    catch (caught) { setError(caught); }
  }
  async function removeListing(item) {
    if (!window.confirm(`Remove “${item.name}” from the marketplace? Rental and payment history will be retained.`)) return;
    setError(null); setNotice(''); setDeletingItemId(item.id);
    try {
      await api.delete(`/admin/items/${item.id}`);
      setNotice('Listing removed from the marketplace. Its history was retained.');
      await refresh();
    } catch (caught) { setError(caught); }
    finally { setDeletingItemId(null); }
  }

  function signOut() {
    logout();
    navigate('/admin/login', { replace: true });
  }

  return <main className="page-wrap admin-page"><div className="admin-heading"><div className="page-heading"><p className="eyebrow">BORROWBOX CONTROL ROOM</p><h1>Admin dashboard</h1><p>Review platform activity and keep listings safe for students.</p></div><div className="admin-session"><span>Signed in as <strong>{user?.email}</strong></span><button type="button" className="secondary-button" onClick={signOut}>Admin log out</button></div></div><ErrorNotice error={error} /><SuccessNotice>{notice}</SuccessNotice>
    <div className="dashboard-tabs">{tabs.map((label) => <button key={label} className={tab === label ? 'active' : ''} onClick={() => setTab(label)}>{label}</button>)}</div>
    {tab === 'Overview' && summary && <><div className="stat-grid">{[['Total users', summary.totalUsers], ['Listed items', summary.totalItems], ['Active rentals', summary.activeRentals], ['Completed rentals', summary.completedRentals], ['Pending requests', summary.pendingRequests], ['Transactions', summary.totalPayments]].map(([label, value]) => <article className="stat-card" key={label}><span>{label}</span><strong>{value}</strong></article>)}</div><section className="dashboard-content"><p className="eyebrow">SUCCESSFUL SIMULATED PAYMENTS</p><h2>₹{Number(summary.successfulPaymentAmount || 0).toLocaleString('en-IN')}</h2><p className="form-help">This total reflects demo payment records only.</p></section></>}
    {tab === 'Users' && <section className="dashboard-content"><h2>Users</h2><div className="table-scroll"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>Account</th><th>Action</th></tr></thead><tbody>{users.map((user) => <tr key={user.id}><td>{user.name}</td><td>{user.email}</td><td>{user.role}</td><td>{new Date(user.createdAt).toLocaleDateString()}</td><td>{user.enabled ? 'Enabled' : 'Disabled'}</td><td><button className="text-button" onClick={() => update(`/admin/users/${user.id}/enabled`, { enabled: !user.enabled }, `Account ${user.enabled ? 'disabled' : 'enabled'}.`)}>{user.enabled ? 'Disable' : 'Enable'}</button></td></tr>)}</tbody></table></div></section>}
    {tab === 'Listings' && <section className="dashboard-content"><h2>Listings</h2><div className="table-scroll"><table><thead><tr><th>Item</th><th>Owner</th><th>Category</th><th>Price/day</th><th>Availability</th><th>Actions</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td>{item.name}</td><td>{item.ownerName}</td><td>{item.category}</td><td>₹{Number(item.pricePerDay).toLocaleString('en-IN')}</td><td>{item.available ? 'Available' : 'Paused'}</td><td><div className="admin-table-actions"><Link className="text-button" to={`/admin/items/${item.id}/edit`}>Edit</Link><button type="button" className="text-button" onClick={() => update(`/admin/items/${item.id}/availability`, { available: !item.available }, `Listing ${item.available ? 'paused' : 'restored'}.`)}>{item.available ? 'Pause' : 'Restore'}</button><button type="button" className="danger-button small-button" disabled={deletingItemId === item.id} onClick={() => removeListing(item)}>{deletingItemId === item.id ? 'Removing…' : 'Delete'}</button></div></td></tr>)}</tbody></table></div></section>}
    {tab === 'Rentals' && <section className="dashboard-content"><h2>Rental activity</h2><div className="table-scroll"><table><thead><tr><th>Item</th><th>Borrower</th><th>Owner</th><th>Dates</th><th>Status</th><th>Total</th></tr></thead><tbody>{rentals.map((rental) => <tr key={rental.id}><td>{rental.itemName}</td><td>{rental.borrowerName}</td><td>{rental.ownerName}</td><td>{rental.startDate}–{rental.endDate}</td><td>{rental.status}</td><td>₹{Number(rental.totalAmount).toLocaleString('en-IN')}</td></tr>)}</tbody></table></div></section>}
    {tab === 'Payments' && <section className="dashboard-content"><h2>Payments</h2><div className="table-scroll"><table><thead><tr><th>Reference</th><th>Rental</th><th>Method</th><th>Status</th><th>Amount</th><th>Date</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>{payment.reference.slice(0, 10)}…</td><td>#{payment.rentalId}</td><td>{payment.method}</td><td>{payment.status}</td><td>₹{Number(payment.amount).toLocaleString('en-IN')}</td><td>{new Date(payment.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div></section>}
  </main>;
}
