import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import ItemImage from '../components/ItemImage.jsx';
import { ErrorNotice, SuccessNotice } from '../components/Notice.jsx';

function RentalCard({ rental, isOwner, onAction, onReview, reviewed }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [method, setMethod] = useState('UPI');
  const [busy, setBusy] = useState(false);
  const pickupContact = rental.pickupContacts ? (isOwner ? rental.pickupContacts.borrower : rental.pickupContacts.owner) : null;
  async function act(action, extra) { setBusy(true); await onAction(rental.id, action, extra); setBusy(false); }
  return <article className="rental-card"><div className="rental-card-top"><div><p className="eyebrow">{isOwner ? 'OWNER REQUEST' : 'YOUR RENTAL'} · #{rental.id}</p><h3>{rental.itemDeleted ? rental.itemName : <Link to={`/items/${rental.itemId}`}>{rental.itemName}</Link>}{rental.itemDeleted && <small className="removed-listing-note"> · listing removed</small>}</h3></div><span className={`status-pill status-${rental.status.toLowerCase()}`}>{rental.status}</span></div>
    <div className="rental-meta"><span>{rental.startDate} → {rental.endDate}</span><span>{isOwner ? `Borrower: ${rental.borrowerName}` : `Owner: ${rental.ownerName}`}</span><strong>₹{Number(rental.totalAmount).toLocaleString('en-IN')} total</strong></div>
    <div className="rental-actions">
      {isOwner && rental.status === 'PENDING' && <><button className="primary-button small-button" disabled={busy} onClick={() => act('accept')}>Accept</button><button className="danger-button" disabled={busy} onClick={() => act('reject')}>Decline</button></>}
      {!isOwner && rental.status === 'ACCEPTED' && <><select value={method} onChange={(e) => setMethod(e.target.value)}><option value="UPI">UPI (simulated)</option><option value="CARD">Card (simulated)</option><option value="OFFLINE">Offline</option></select><button className="primary-button small-button" disabled={busy} onClick={() => act('pay', method)}>Pay ₹{Number(rental.totalAmount).toLocaleString('en-IN')}</button></>}
      {isOwner && rental.status === 'PAID' && <button className="primary-button small-button" disabled={busy} onClick={() => act('activate')}>Mark handed over</button>}
      {rental.status === 'ACTIVE' && <button className="primary-button small-button" disabled={busy} onClick={() => act('complete')}>Mark returned</button>}
      {!isOwner && ['PENDING', 'ACCEPTED'].includes(rental.status) && <button className="text-button" disabled={busy} onClick={() => act('cancel')}>Cancel request</button>}
    </div>
    {pickupContact && <section className="pickup-contact"><div><p className="eyebrow">PICKUP CONTACT</p><h4>Coordinate with {pickupContact.name}</h4><p>Contact each other to agree on a pickup time and location.</p><div className="pickup-contact-links"><a href={`mailto:${pickupContact.email}`}>Email {pickupContact.name}</a>{pickupContact.phone && <a href={`tel:${pickupContact.phone}`}>Call {pickupContact.name}</a>}</div><small>{pickupContact.email}{pickupContact.phone ? ` · ${pickupContact.phone}` : ' · No phone number added to profile'} · Shared with rental participants only</small></div></section>}
    {rental.status === 'PENDING' && <p className="pickup-contact-pending">Pickup contact details will appear here after the owner accepts.</p>}
    {!isOwner && rental.status === 'COMPLETED' && !reviewed && <form className="review-form" onSubmit={async (event) => { event.preventDefault(); setBusy(true); await onReview(rental.id, rating, comment); setBusy(false); }}><strong>How was the item?</strong><div className="review-inputs"><select value={rating} onChange={(e) => setRating(Number(e.target.value))}>{[5, 4, 3, 2, 1].map((n) => <option key={n} value={n}>{n} star{n === 1 ? '' : 's'}</option>)}</select><input maxLength="1500" value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Write a short review (optional)" /><button className="secondary-button" disabled={busy}>Submit review</button></div></form>}
    {!isOwner && reviewed && rental.status === 'COMPLETED' && <p className="reviewed-note">✓ Review submitted</p>}
  </article>;
}

export default function Dashboard() {
  const { user } = useAuth();
  const [tab, setTab] = useState('rentals');
  const [items, setItems] = useState([]);
  const [borrowings, setBorrowings] = useState([]);
  const [ownerRentals, setOwnerRentals] = useState([]);
  const [payments, setPayments] = useState([]);
  const [reviewed, setReviewed] = useState({});
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [deletingItemId, setDeletingItemId] = useState(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const [itemResponse, borrowResponse, ownerResponse, paymentResponse] = await Promise.all([
        api.get('/items/mine'), api.get('/rentals/my'), api.get('/rentals/owner'), api.get('/payments/my'),
      ]);
      setItems(itemResponse.data.content || []); setBorrowings(borrowResponse.data); setOwnerRentals(ownerResponse.data); setPayments(paymentResponse.data);
      const checkable = borrowResponse.data.filter((rental) => rental.status === 'COMPLETED');
      const checks = await Promise.all(checkable.map(async (rental) => [rental.id, (await api.get(`/reviews/rental/${rental.id}/mine`)).data]));
      setReviewed(Object.fromEntries(checks));
    } catch (caught) { setError(caught); }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);
  const allRentals = [...borrowings.map((rental) => ({ ...rental, isOwner: false })), ...ownerRentals.filter((rental) => !borrowings.some((b) => b.id === rental.id)).map((rental) => ({ ...rental, isOwner: true }))].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  async function action(id, name, extra) {
    setError(null); setNotice('');
    try {
      if (name === 'pay') await api.post(`/rentals/${id}/pay`, { method: extra });
      else await api.put(`/rentals/${id}/${name}`);
      setNotice(name === 'pay' ? 'Simulated payment recorded.' : 'Rental updated.'); await refresh();
    } catch (caught) { setError(caught); }
  }
  async function review(rentalId, rating, comment) {
    setError(null); setNotice('');
    try { await api.post('/reviews', { rentalId, rating, comment }); setNotice('Thanks for sharing your review.'); await refresh(); }
    catch (caught) { setError(caught); }
  }

  async function deleteItem(item) {
    if (!window.confirm(`Remove “${item.name}” from listings? Rental history will be kept.`)) return;
    setError(null); setNotice(''); setDeletingItemId(item.id);
    try {
      await api.delete(`/items/${item.id}`);
      setNotice('Listing removed. Past rental records were kept.');
      await refresh();
    } catch (caught) { setError(caught); }
    finally { setDeletingItemId(null); }
  }

  return <main className="page-wrap dashboard-page"><div className="page-heading"><p className="eyebrow">YOUR BORROWBOX</p><h1>Hi, {user.name}.</h1><p>Manage your listings, rental requests, and payments in one place.</p><div className="heading-actions"><Link className="primary-button inline-button" to="/items/new">+ List an item</Link><Link className="secondary-button inline-button" to="/profile">Edit profile</Link></div></div>
    <ErrorNotice error={error} /><SuccessNotice>{notice}</SuccessNotice>
    <div className="dashboard-tabs">{[['rentals', 'Rentals & requests'], ['items', 'My listings'], ['payments', 'Payment history']].map(([key, label]) => <button className={tab === key ? 'active' : ''} key={key} onClick={() => setTab(key)}>{label}</button>)}</div>
    {tab === 'rentals' && <section className="dashboard-content"><div className="section-heading"><div><p className="eyebrow">BORROWER + OWNER</p><h2>Your rental activity</h2></div><button type="button" className="secondary-button small-button" onClick={refresh}>Refresh activity</button></div>{allRentals.length ? <div className="rental-list">{allRentals.map((rental) => <RentalCard key={`${rental.id}-${rental.isOwner}`} rental={rental} isOwner={rental.isOwner} reviewed={reviewed[rental.id]} onAction={action} onReview={review} />)}</div> : <div className="empty-results"><span>↔</span><h2>No rentals yet</h2><p>When you request or receive an item request, it’ll show up here.</p><Link to="/items" className="primary-button inline-button">Explore listings</Link></div>}</section>}
    {tab === 'items' && <section className="dashboard-content"><div className="section-heading"><div><p className="eyebrow">WHAT YOU SHARE</p><h2>My listings</h2></div></div>{items.length ? <div className="management-list">{items.map((item) => <article className="management-row" key={item.id}><div className="management-thumb"><ItemImage src={item.imageUrl} alt={item.name} /></div><div className="management-main"><Link to={`/items/${item.id}`}><strong>{item.name}</strong></Link><span>{item.category} · ₹{Number(item.pricePerDay).toLocaleString('en-IN')}/day</span></div><span className={`status-pill ${item.available ? 'status-active' : 'status-cancelled'}`}>{item.available ? 'Available' : 'Paused'}</span><div className="listing-row-actions"><Link className="text-button" to={`/items/${item.id}/edit`}>Edit</Link><button type="button" className="danger-button small-button" disabled={deletingItemId === item.id} onClick={() => deleteItem(item)}>{deletingItemId === item.id ? 'Deleting…' : 'Delete'}</button></div></article>)}</div> : <p>You haven’t listed anything yet. <Link to="/items/new">Create your first listing</Link>.</p>}</section>}
    {tab === 'payments' && <section className="dashboard-content"><div className="section-heading"><div><p className="eyebrow">SIMULATED CHECKOUTS</p><h2>Payment history</h2></div></div>{payments.length ? <div className="table-scroll"><table><thead><tr><th>Reference</th><th>Rental</th><th>Method</th><th>Status</th><th>Amount</th><th>Date</th></tr></thead><tbody>{payments.map((payment) => <tr key={payment.id}><td>{payment.reference.slice(0, 10)}…</td><td>#{payment.rentalId}</td><td>{payment.method}</td><td>{payment.status}</td><td>₹{Number(payment.amount).toLocaleString('en-IN')}</td><td>{new Date(payment.createdAt).toLocaleDateString()}</td></tr>)}</tbody></table></div> : <p>No payments recorded.</p>}<p className="form-help">BorrowBox records demo payment status only; it does not collect card numbers or process real payments.</p></section>}
  </main>;
}
