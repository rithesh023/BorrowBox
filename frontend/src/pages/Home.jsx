import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api.js';
import { useAuth } from '../context/AuthContext.jsx';
import ItemCard from '../components/ItemCard.jsx';
import { ErrorNotice } from '../components/Notice.jsx';

const icons = ['▤', '▰', '◎', '▱', '⌗', '◉', '✳', '⚗', '↗', '＋'];

export default function Home() {
  const { user, ready } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [categories, setCategories] = useState([]);
  const [items, setItems] = useState([]);
  const [recommended, setRecommended] = useState([]);
  const [error, setError] = useState(null);
  useEffect(() => {
    if (!ready) return;
    setError(null);
    api.get('/categories').then(({ data }) => setCategories(data)).catch(setError);
    api.get('/items', { params: { available: true, size: 8 } }).then(({ data }) => setItems(data.content || [])).catch(setError);
  }, [ready]);
  useEffect(() => {
    if (user) api.get(`/recommendations/user/${user.id}`).then(({ data }) => setRecommended(data)).catch(() => setRecommended([]));
    else setRecommended([]);
  }, [user]);

  function search(event) {
    event.preventDefault();
    navigate(`/items${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''}`);
  }

  return <main>
    <section className="hero">
      <div className="hero-copy"><p className="eyebrow">BORROW LOCAL. DO MORE.</p><h1>Good gear for<br />great campus ideas.</h1>
        <p className="lede">Find the things you need from students around you. Rent for a few days, return with ease.</p>
        <form className="searchbox" onSubmit={search}><span aria-hidden="true">⌕</span><input value={query} onChange={(event) => setQuery(event.target.value)} aria-label="Search items" placeholder="What do you need for your next big idea?" /><button>Search</button></form>
        <div className="trustline"><span className="status-dot" /> Built for campus communities <span className="api-status">Aiven-powered marketplace</span></div>
        <div className="hero-links"><Link to="/items">Browse all items ↗</Link><Link to={user ? '/items/new' : '/register'}>Share an item ↗</Link></div>
      </div>
      <div className="hero-art" aria-label="Illustration of campus rental equipment"><div className="sun" /><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="book book-one" /><div className="book book-two" /><div className="camera"><div className="lens"><div /></div><span className="camera-flash" /></div><div className="plant"><i /><i /><i /><b /></div><div className="art-tag">Your next project starts here <span>↗</span></div></div>
    </section>
    <section className="categories"><div className="section-heading"><div><p className="eyebrow">THE CAMPUS TOOLKIT</p><h2>What are you looking for?</h2></div><Link to="/items">Browse everything <span>↗</span></Link></div>
      {error && <ErrorNotice error={error} />}
      <div className="category-grid">{categories.slice(0, 10).map((category, index) => <Link className="category-card" key={category.id} to={`/items?category=${encodeURIComponent(category.name)}`}><span className={`category-icon icon-${index % 6}`}>{icons[index % icons.length]}</span><span>{category.name}</span><span className="arrow">↗</span></Link>)}</div>
    </section>
    {user && recommended.length > 0 && <section className="listing-section"><div className="section-heading"><div><p className="eyebrow">BASED ON YOUR ACTIVITY</p><h2>Recommended for you</h2></div></div><div className="item-grid">{recommended.slice(0, 4).map((item) => <ItemCard item={item} key={item.id} />)}</div></section>}
    <section className="listing-section" id="browse"><div className="section-heading"><div><p className="eyebrow">FRESH FROM CAMPUS</p><h2>Recently added</h2></div><Link to="/items">See all listings <span>↗</span></Link></div>
      {items.length ? <div className="item-grid">{items.slice(0, 8).map((item) => <ItemCard item={item} key={item.id} />)}</div> : <div className="empty-state"><div className="empty-icon">✳</div><div><p className="eyebrow">A COMMUNITY MARKETPLACE</p><h2>No listings yet</h2><p>Be the first to share something useful with your campus.</p></div><Link to={user ? '/items/new' : '/register'} className="outline-button">List your first item ↗</Link></div>}
    </section>
    <section className="how-it-works" id="how-it-works"><p className="eyebrow">EASY AS ONE, TWO, THREE</p><h2>Good things are better shared.</h2><div className="steps"><article><span>01</span><h3>Find what you need</h3><p>Search real student listings by category, budget, and availability.</p></article><article><span>02</span><h3>Request your dates</h3><p>The owner confirms your rental before you make a simulated payment.</p></article><article><span>03</span><h3>Use it, return it</h3><p>Coordinate with the owner, complete the rental, and leave a review.</p></article></div></section>
  </main>;
}
