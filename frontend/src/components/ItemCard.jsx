import { Link } from 'react-router-dom';
import ItemImage from './ItemImage.jsx';

export default function ItemCard({ item }) {
  return <article className="item-card">
    <Link className="item-card-image" to={`/items/${item.id}`}>
      <ItemImage src={item.imageUrl} alt={item.name} />
      <span className="availability-pill">{item.available ? 'Available' : 'Unavailable'}</span>
    </Link>
    <div className="item-card-body"><div className="item-card-meta"><span>{item.category}</span><span>★ {Number(item.averageRating || 0).toFixed(1)}{item.reviewCount ? ` · ${item.reviewCount}` : ''}</span></div>
      <Link className="item-card-title" to={`/items/${item.id}`}>{item.name}</Link>
      <p className="item-card-condition">{item.itemCondition} · By {item.ownerName || 'campus owner'}</p>
      <div className="item-card-price"><strong>₹{Number(item.pricePerDay).toLocaleString('en-IN')}</strong><span>/ day</span><Link to={`/items/${item.id}`} aria-label={`View ${item.name}`}>↗</Link></div>
    </div>
  </article>;
}
