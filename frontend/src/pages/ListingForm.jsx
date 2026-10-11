import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../services/api.js';
import ItemImage from '../components/ItemImage.jsx';
import ImageCropDialog from '../components/ImageCropDialog.jsx';
import { ErrorNotice, SuccessNotice } from '../components/Notice.jsx';

const blank = { name: '', category: '', description: '', pricePerDay: '', securityDeposit: '0', itemCondition: 'Good', imageUrl: '', available: true };

export default function ListingForm({ adminMode = false }) {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [form, setForm] = useState(blank);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState('');
  const [aiBusy, setAiBusy] = useState(false);
  const [busy, setBusy] = useState(false);
  const [imageFile, setImageFile] = useState(null);
  const [cropFile, setCropFile] = useState(null);
  const [imagePreview, setImagePreview] = useState('');
  const [previewFailed, setPreviewFailed] = useState(false);

  useEffect(() => {
    if (!imageFile) { setImagePreview(''); return undefined; }
    const preview = URL.createObjectURL(imageFile);
    setImagePreview(preview);
    return () => URL.revokeObjectURL(preview);
  }, [imageFile]);

  useEffect(() => {
    api.get('/categories').then(({ data }) => { setCategories(data); if (!editing && data.length) setForm((value) => ({ ...value, category: data[0].name })); }).catch(setError);
    if (editing) api.get(`/items/${id}`).then(({ data }) => setForm({ ...data, pricePerDay: String(data.pricePerDay), securityDeposit: String(data.securityDeposit) })).catch(setError);
  }, [id, editing]);
  function update(key, value) { setForm((current) => ({ ...current, [key]: value })); }

  function chooseImage(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const supported = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
    if (!supported.includes(file.type)) {
      setError({ response: { data: { message: 'Choose a JPG, PNG, WEBP, or GIF image.' } } });
      event.target.value = '';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError({ response: { data: { message: 'The image must be 5 MB or smaller.' } } });
      event.target.value = '';
      return;
    }
    setError(null); setPreviewFailed(false); setCropFile(file);
    event.target.value = '';
  }

  function acceptCrop(file) {
    setImageFile(file);
    setCropFile(null);
    setPreviewFailed(false);
    setError(null);
  }

  async function generateDescription() {
    setError(null); setSuccess(''); setAiBusy(true);
    try {
      const { data } = await api.post('/ai/generate-description', { name: form.name, condition: form.itemCondition, details: form.description });
      update('description', data.description); setSuccess(data.message || (data.aiEnhanced ? 'AI draft added. Review it before publishing.' : 'A basic draft was added. Review and edit it before publishing.'));
    } catch (caught) { setError(caught); }
    finally { setAiBusy(false); }
  }

  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null);
    const payload = { ...form, pricePerDay: Number(form.pricePerDay), securityDeposit: Number(form.securityDeposit) };
    try {
      if (imageFile) {
        const imageData = new FormData();
        imageData.append('file', imageFile);
        const { data: uploaded } = await api.post('/uploads', imageData);
        payload.imageUrl = uploaded.url;
      }
      const { data } = editing
        ? await api.put(adminMode ? `/admin/items/${id}` : `/items/${id}`, payload)
        : await api.post('/items', payload);
      navigate(adminMode ? '/admin' : `/items/${data.id}`, adminMode ? { state: { adminTab: 'Listings' } } : undefined);
    } catch (caught) { setError(caught); }
    finally { setBusy(false); }
  }

  return <main className="page-wrap form-page"><Link className="back-link" to={adminMode ? '/admin' : editing ? `/items/${id}` : '/dashboard'}>← Cancel</Link><div className="page-heading"><p className="eyebrow">{adminMode ? 'ADMIN LISTING MANAGEMENT' : 'SHARE WITH YOUR CAMPUS'}</p><h1>{editing ? 'Edit listing' : 'List an item'}</h1><p>{adminMode ? 'Update the listing details and availability. Rental records remain attached to this item.' : 'Keep details clear so another student knows what they’ll be renting.'}</p></div>
    <form className="listing-form" onSubmit={submit}><ErrorNotice error={error} /><SuccessNotice>{success}</SuccessNotice>
      <div className="form-grid"><label>Item name<input required maxLength="140" value={form.name} onChange={(e) => update('name', e.target.value)} placeholder="Canon EOS camera" /></label>
        <label>Category<select required value={form.category} onChange={(e) => update('category', e.target.value)}>{categories.map((category) => <option key={category.id} value={category.name}>{category.name}</option>)}</select></label>
        <label>Price per day (₹)<input required type="number" min="0.01" step="0.01" value={form.pricePerDay} onChange={(e) => update('pricePerDay', e.target.value)} /></label>
        <label>Security deposit (₹)<input required type="number" min="0" step="0.01" value={form.securityDeposit} onChange={(e) => update('securityDeposit', e.target.value)} /></label>
        <label>Condition<select value={form.itemCondition} onChange={(e) => update('itemCondition', e.target.value)}>{['New', 'Like new', 'Good', 'Fair'].map((value) => <option key={value}>{value}</option>)}</select></label>
        <label>Image URL <span className="optional-label">(optional)</span><input type="url" maxLength="1000" value={form.imageUrl || ''} onChange={(e) => { setImageFile(null); setCropFile(null); setPreviewFailed(false); update('imageUrl', e.target.value); }} placeholder="https://…" /><span className="form-help">Or upload a JPG, PNG, WEBP, or GIF photo (up to 5 MB). A direct image link also works; Google search page links are not image links.</span></label>
      </div>
      <label>Upload photo<input type="file" accept="image/jpeg,image/png,image/webp,image/gif" onChange={chooseImage} /><span className="form-help">{imageFile ? `${imageFile.name} selected · You can choose another photo to crop it again.` : 'Select a photo from this computer. You can crop it before it uploads.'}</span></label>
      {(imagePreview || form.imageUrl) && <div className="listing-image-preview"><ItemImage src={imagePreview || form.imageUrl} alt="Listing photo preview" onLoadError={() => setPreviewFailed(true)} />{previewFailed && <p className="form-help">This link or file could not be displayed. Upload a photo from your computer or use a direct image link.</p>}</div>}
      <label>Description<textarea required maxLength="3000" rows="6" value={form.description} onChange={(e) => update('description', e.target.value)} placeholder="Describe its condition, included parts, and anything the renter should know." /></label>
      <button type="button" className="secondary-button" disabled={aiBusy || busy || !form.name || !form.itemCondition} onClick={generateDescription}>{aiBusy ? 'Writing…' : '✳ Draft description'}</button><p className="form-help">Uses AI when an API key is configured. Otherwise it creates a basic editable draft.</p>
      {editing && <label className="checkbox-label"><input type="checkbox" checked={Boolean(form.available)} onChange={(e) => update('available', e.target.checked)} /> Available for requests</label>}
      <button className="primary-button" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Publish listing'}</button>
    </form>
    {cropFile && <ImageCropDialog file={cropFile} onCancel={() => setCropFile(null)} onCrop={acceptCrop} />}
  </main>;
}
