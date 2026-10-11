import { useEffect, useState } from 'react';
import { resolveImageUrl } from '../services/api.js';

export default function ItemImage({ src, alt = '', className = '', onLoadError }) {
  const [failed, setFailed] = useState(false);
  const imageUrl = resolveImageUrl(src);
  useEffect(() => setFailed(false), [src]);
  if (!imageUrl || failed) return <span className="image-placeholder" role="img" aria-label={alt || 'No image available'}>✳</span>;
  return <img className={className} src={imageUrl} alt={alt} loading="lazy" onError={() => { setFailed(true); onLoadError?.(); }} />;
}
