import { useEffect, useRef, useState } from 'react';
import './ImageCropDialog.css';

const OUTPUT_WIDTH = 1200;
const OUTPUT_HEIGHT = 900;

function clamp(value) { return Math.max(0, Math.min(1, value)); }

function canvasBlob(canvas) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error('Could not crop this image. Please choose another photo.')), 'image/jpeg', 0.88);
  });
}

export default function ImageCropDialog({ file, onCancel, onCrop }) {
  const [sourceUrl, setSourceUrl] = useState('');
  const stageRef = useRef(null);
  const imageRef = useRef(null);
  const dragRef = useRef(null);
  const [naturalSize, setNaturalSize] = useState(null);
  const [previewReady, setPreviewReady] = useState(false);
  const [stageWidth, setStageWidth] = useState(0);
  const [zoom, setZoom] = useState(1);
  const [pan, setPan] = useState({ x: 0.5, y: 0.5 });
  const [dragging, setDragging] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const url = URL.createObjectURL(file);
    setNaturalSize(null);
    setPreviewReady(false);
    setZoom(1);
    setPan({ x: 0.5, y: 0.5 });
    setError('');
    setSourceUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);

  useEffect(() => {
    if (!stageRef.current || typeof ResizeObserver === 'undefined') return undefined;
    const observer = new ResizeObserver(([entry]) => setStageWidth(entry.contentRect.width));
    observer.observe(stageRef.current);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    function onKeyDown(event) { if (event.key === 'Escape' && !saving) onCancel(); }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onCancel, saving]);

  const stageHeight = stageWidth * 3 / 4;
  const baseScale = naturalSize && stageWidth ? Math.max(stageWidth / naturalSize.width, stageHeight / naturalSize.height) : 0;
  const renderedWidth = naturalSize ? naturalSize.width * baseScale * zoom : 0;
  const renderedHeight = naturalSize ? naturalSize.height * baseScale * zoom : 0;
  const overflowX = Math.max(0, renderedWidth - stageWidth);
  const overflowY = Math.max(0, renderedHeight - stageHeight);

  function updatePan(axis, value) {
    setPan((current) => ({ ...current, [axis]: clamp(value) }));
  }

  function startDrag(event) {
    if (!naturalSize || saving || event.button !== 0) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    dragRef.current = { x: event.clientX, y: event.clientY };
    setDragging(true);
  }

  function moveDrag(event) {
    if (!dragRef.current) return;
    const deltaX = event.clientX - dragRef.current.x;
    const deltaY = event.clientY - dragRef.current.y;
    dragRef.current = { x: event.clientX, y: event.clientY };
    setPan((current) => ({
      x: overflowX ? clamp(current.x - deltaX / overflowX) : current.x,
      y: overflowY ? clamp(current.y - deltaY / overflowY) : current.y,
    }));
  }

  function stopDrag() { dragRef.current = null; setDragging(false); }

  async function saveCrop() {
    if (!imageRef.current || !previewReady || !naturalSize || !stageWidth) return;
    setError(''); setSaving(true);
    try {
      const scale = baseScale * zoom;
      const cropX = overflowX * pan.x / scale;
      const cropY = overflowY * pan.y / scale;
      const cropWidth = stageWidth / scale;
      const cropHeight = stageHeight / scale;
      const canvas = document.createElement('canvas');
      canvas.width = OUTPUT_WIDTH;
      canvas.height = OUTPUT_HEIGHT;
      const context = canvas.getContext('2d');
      if (!context) throw new Error('Your browser could not crop this image. Please try another browser.');
      context.imageSmoothingQuality = 'high';
      context.fillStyle = '#fff';
      context.fillRect(0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT);
      context.drawImage(imageRef.current, cropX, cropY, cropWidth, cropHeight, 0, 0, OUTPUT_WIDTH, OUTPUT_HEIGHT);
      const blob = await canvasBlob(canvas);
      if (blob.size > 5 * 1024 * 1024) throw new Error('The cropped photo is still over 5 MB. Please choose another image.');
      const baseName = file.name.replace(/\.[^.]+$/, '') || 'listing-photo';
      onCrop(new File([blob], `${baseName}-cropped.jpg`, { type: 'image/jpeg' }));
    } catch (caught) {
      setError(caught.message || 'Could not crop this image. Please try again.');
      setSaving(false);
    }
  }

  return <div className="crop-dialog-backdrop">
    <section className="crop-dialog" role="dialog" aria-modal="true" aria-labelledby="crop-title">
      <header className="crop-dialog-header"><div><p className="eyebrow">LISTING PHOTO</p><h2 id="crop-title">Choose what renters will see</h2><p>Drag the photo inside the frame, then adjust zoom and position.</p></div><button type="button" className="crop-close" onClick={onCancel} disabled={saving} aria-label="Cancel crop">×</button></header>
      <div
        className={`crop-stage${dragging ? ' is-dragging' : ''}`}
        ref={stageRef}
        role="group"
        onPointerDown={startDrag}
        onPointerMove={moveDrag}
        onPointerUp={stopDrag}
        onPointerCancel={stopDrag}
        onLostPointerCapture={stopDrag}
        aria-label="Photo crop area. Drag to reposition."
      >
        {naturalSize && stageWidth > 0 && <img
          ref={imageRef}
          src={sourceUrl}
          alt="Crop preview"
          draggable="false"
          onLoad={() => setPreviewReady(true)}
          onError={() => setError('This image could not be opened. Choose a different photo.')}
          style={{ width: renderedWidth, height: renderedHeight, left: -overflowX * pan.x, top: -overflowY * pan.y }}
        />}
        <span className="crop-frame-label">4:3 LISTING PHOTO</span>
        <span className="crop-drag-hint">↔ Drag to reposition</span>
      </div>
      <div className="crop-controls">
        <label>Zoom <span>{zoom.toFixed(1)}×</span><input type="range" min="1" max="3" step="0.05" value={zoom} onChange={(event) => setZoom(Number(event.target.value))} /></label>
        <label>Horizontal position <input type="range" min="0" max="100" value={Math.round(pan.x * 100)} onChange={(event) => updatePan('x', Number(event.target.value) / 100)} /></label>
        <label>Vertical position <input type="range" min="0" max="100" value={Math.round(pan.y * 100)} onChange={(event) => updatePan('y', Number(event.target.value) / 100)} /></label>
      </div>
      <p className="crop-note">The selected area is saved as a 4:3 JPG photo. Transparent parts become white.</p>
      {file.type === 'image/gif' && <p className="crop-note">Cropping an animated GIF saves the currently displayed frame as a photo.</p>}
      {error && <p className="crop-error" role="alert">{error}</p>}
      <footer className="crop-dialog-actions"><button type="button" className="secondary-button" onClick={onCancel} disabled={saving}>Cancel</button><button type="button" className="primary-button" onClick={saveCrop} disabled={!naturalSize || !previewReady || saving}>{saving ? 'Preparing photo…' : 'Use this crop'}</button></footer>
      {sourceUrl && <img className="crop-source-loader" src={sourceUrl} alt="" onLoad={(event) => setNaturalSize({ width: event.currentTarget.naturalWidth, height: event.currentTarget.naturalHeight })} onError={() => setError('This image could not be opened. Choose a different photo.')} />}
    </section>
  </div>;
}
