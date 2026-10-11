import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import ItemCard from '../components/ItemCard.jsx';
import ItemImage from '../components/ItemImage.jsx';
import { ErrorNotice, SuccessNotice } from '../components/Notice.jsx';

function dateString(date) { return date.toISOString().slice(0, 10); }
const today = dateString(new Date());

export default function ItemDetail() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [item, setItem] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [recommended, setRecommended] = useState([]);
  const [startDate, setStartDate] = useState(today);
  const [endDate, setEndDate] = useState(today);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(null); setItem(null); setReviews([]); setRecommended([]);

    // The listing is essential; reviews and recommendations are optional extras.
    api.get(`/items/${id}`)
      .then(({ data }) => { if (active) setItem(data); })
      .catch((caught) => { if (active) setError(caught); })
      .finally(() => { if (active) setLoading(false); });
    api.get(`/reviews/item/${id}`)
      .then(({ data }) => { if (active) setReviews(data); })
      .catch(() => { if (active) setReviews([]); });
    api.get(`/recommendations/item/${id}`)
      .then(({ data }) => { if (active) setRecommended(data); })
      .catch(() => { if (active) setRecommended([]); });

    return () => { active = false; };
  }, [id]);

  async function requestRental(event) {
    event.preventDefault(); setError(null); setSuccess('');
    try {
      const { data } = await api.post('/rentals', { itemId: item.id, startDate, endDate });
      setSuccess(`Request #${data.id} sent to ${item.ownerName}. You can follow it in My dashboard.`);
    } catch (caught) { setError(caught); }
  }

  async function deleteListing() {
    if (!window.confirm(`Remove “${item.name}” from listings? Rental history will be kept.`)) return;
    setError(null); setDeleting(true);
    try { await api.delete(`/items/${item.id}`); navigate('/dashboard', { replace: true }); }
    catch (caught) { setError(caught); }
    finally { setDeleting(false); }
  }

  if (loading) return <main className="page-wrap"><p className="loading">Loading item…</p></main>;
  if (!item) return <main className="page-wrap"><ErrorNotice error={error} /><Link to="/items">Back to listings</Link></main>;
  return <main className="page-wrap detail-page"><Link className="back-link" to="/items">← Back to listings</Link><ErrorNotice error={error} /><SuccessNotice>{success}</SuccessNotice>
    <section className="detail-layout"><div className="detail-image"><ItemImage src={item.imageUrl} alt={item.name} /></div>
      <div className="detail-copy"><p className="eyebrow">{item.category} · {item.itemCondition}</p><h1>{item.name}</h1><p className="detail-owner">Shared by {item.ownerName} · <span className={item.available ? 'available-text' : 'unavailable-text'}>{item.available ? 'Available' : 'Unavailable'}</span></p>
        <p className="detail-rating">★ {Number(item.averageRating || 0).toFixed(1)} <span>({item.reviewCount || 0} reviews)</span></p><p className="detail-description">{item.description}</p>
        <div className="price-summary"><div><strong>₹{Number(item.pricePerDay).toLocaleString('en-IN')}</strong><span> / day</span></div><p>Security deposit: ₹{Number(item.securityDeposit).toLocaleString('en-IN')}</p></div>
        {user && user.id !== item.ownerId && item.available ? <form className="rental-request" onSubmit={requestRental}><h2>Request a rental</h2><div className="date-fields"><label>From<input type="date" min={today} value={startDate} onChange={(event) => setStartDate(event.target.value)} required /></label><label>To<input type="date" min={startDate || today} value={endDate} onChange={(event) => setEndDate(event.target.value)} required /></label></div><button className="primary-button">Send request</button><small>The owner confirms first. The final amount is calculated by BorrowBox.</small></form>
          : user?.id === item.ownerId ? <div className="listing-owner-actions"><Link className="primary-button inline-button" to={`/items/${item.id}/edit`}>Edit your listing</Link><button type="button" className="danger-button" disabled={deleting} onClick={deleteListing}>{deleting ? 'Deleting…' : 'Delete listing'}</button></div>
            : !user ? <p className="signin-prompt"><Link to="/login">Log in</Link> to request this item.</p> : <p className="unavailable-text">This listing is not currently available.</p>}
      </div>
    </section>
    {reviews.length > 0 && <section className="listing-section"><div className="section-heading"><div><p className="eyebrow">COMMUNITY FEEDBACK</p><h2>Reviews</h2></div></div><div className="review-grid">{reviews.map((review) => <article className="review-card" key={review.id}><div>⭐ {review.rating}/5 <span>{new Date(review.createdAt).toLocaleDateString()}</span></div><p>{review.comment || 'Shared experience'}</p><small>{review.reviewerName}</small></article>)}</div></section>}
    {recommended.length > 0 && <section className="listing-section"><div className="section-heading"><div><p className="eyebrow">GOOD TOGETHER</p><h2>You may also need</h2></div></div><div className="item-grid">{recommended.slice(0, 4).map((candidate) => <ItemCard item={candidate} key={candidate.id} />)}</div></section>}
  </main>;
}
