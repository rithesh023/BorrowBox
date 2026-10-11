import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../services/api.js';
import ItemCard from '../components/ItemCard.jsx';
import { ErrorNotice } from '../components/Notice.jsx';

export default function ItemsPage() {
  const [params, setParams] = useSearchParams();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({ q: params.get('q') || '', category: params.get('category') || '', maxPrice: '', condition: '', minRating: '' });
  const [page, setPage] = useState({ content: [], totalPages: 0, number: 0 });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => { api.get('/categories').then(({ data }) => setCategories(data)).catch(setError); }, []);
  useEffect(() => {
    const nextForm = { q: params.get('q') || '', category: params.get('category') || '', maxPrice: params.get('maxPrice') || '', condition: params.get('condition') || '', minRating: params.get('minRating') || '' };
    setForm(nextForm); setLoading(true); setError(null);
    api.get('/items', { params: { ...Object.fromEntries(Object.entries(nextForm).filter(([, value]) => value)), available: true, page: params.get('page') || 0, size: 12 } })
      .then(({ data }) => setPage(data)).catch(setError).finally(() => setLoading(false));
  }, [params]);
  function submit(event) {
    event.preventDefault();
    const next = Object.fromEntries(Object.entries(form).filter(([, value]) => value));
    setParams(next);
  }
  return <main className="page-wrap"><div className="page-heading"><p className="eyebrow">CAMPUS MARKETPLACE</p><h1>Find your next useful thing.</h1><p>Every result comes from an actual BorrowBox listing.</p></div>
    <form className="filter-bar" onSubmit={submit}><label className="filter-search">Search<input value={form.q} onChange={(event) => setForm({ ...form, q: event.target.value })} placeholder="Camera, calculator, books…" /></label>
      <label>Category<select value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}><option value="">All categories</option>{categories.map((category) => <option key={category.id}>{category.name}</option>)}</select></label>
      <label>Max ₹ / day<input type="number" min="0" value={form.maxPrice} onChange={(event) => setForm({ ...form, maxPrice: event.target.value })} placeholder="Any" /></label>
      <label>Condition<select value={form.condition} onChange={(event) => setForm({ ...form, condition: event.target.value })}><option value="">Any condition</option>{['New', 'Like new', 'Good', 'Fair'].map((value) => <option key={value}>{value}</option>)}</select></label>
      <label>Rating<select value={form.minRating} onChange={(event) => setForm({ ...form, minRating: event.target.value })}><option value="">Any rating</option><option value="4">4+ stars</option><option value="3">3+ stars</option></select></label>
      <button className="primary-button">Apply filters</button>
    </form>
    <ErrorNotice error={error} />
    {loading ? <p className="loading">Loading listings…</p> : page.content?.length ? <><p className="result-count">{page.totalElements} available listing{page.totalElements === 1 ? '' : 's'}</p><div className="item-grid">{page.content.map((item) => <ItemCard item={item} key={item.id} />)}</div>
      {page.totalPages > 1 && <div className="pagination"><button disabled={page.number <= 0} onClick={() => setParams({ ...Object.fromEntries(params), page: String(page.number - 1) })}>← Previous</button><span>Page {page.number + 1} of {page.totalPages}</span><button disabled={page.number + 1 >= page.totalPages} onClick={() => setParams({ ...Object.fromEntries(params), page: String(page.number + 1) })}>Next →</button></div>}</>
      : <div className="empty-results"><span>⌕</span><h2>No matching listings</h2><p>Try another search or clear a filter.</p><button className="text-button" onClick={() => { setForm({ q: '', category: '', maxPrice: '', condition: '', minRating: '' }); setParams({}); }}>Clear filters</button></div>}
  </main>;
}
