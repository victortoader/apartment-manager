import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from './AuthContext';
import './App.css';

function Presentation() {
  const { id } = useParams();
  const { user } = useAuth();
  const isOwner = user && user.role === 'OWNER';
  const [apt, setApt] = useState(null);
  const [content, setContent] = useState('');
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [draftPrice, setDraftPrice] = useState('');
  const [draftDescription, setDraftDescription] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [activePhoto, setActivePhoto] = useState(0);

  useEffect(() => {
    fetch(`/api/apartments/${id}/presentation`)
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => {
        setApt(data);
        setContent(data.presentation || '');
        setDraft(data.presentation || '');
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, [id]);

  const handleSave = async () => {
    setSaving(true);
    const token = localStorage.getItem('token');
    const headers = { 'Authorization': 'Bearer ' + token };

    await fetch(`/api/apartments/${id}/presentation`, {
      method: 'PUT',
      headers: { 'Content-Type': 'text/plain', ...headers },
      body: draft
    });

    await fetch(`/api/apartments/${id}/details`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...headers },
      body: JSON.stringify({
        price: draftPrice !== '' ? parseFloat(draftPrice) : null,
        description: draftDescription
      })
    });

    setContent(draft);
    setApt({ ...apt, presentation: draft, price: draftPrice !== '' ? parseFloat(draftPrice) : apt.price, description: draftDescription });
    setEditing(false);
    setSaving(false);
  };

  if (loading) return <div className="pres"><p className="loading">Loading...</p></div>;
  if (!apt) return <div className="pres"><p className="error">Apartment not found.</p></div>;

  const photos = apt.photoPaths || [];

  return (
    <div className="pres">
      <header className="pres-header">
        <Link to="/" className="pres-logo">Berlin Housing</Link>
        <nav className="pres-nav">
          <Link to="/" className="pres-nav-link">Dashboard</Link>
          {isOwner && <Link to={`/apartments/${id}`} className="pres-nav-link">Management</Link>}
        </nav>
      </header>

      <main className="pres-main">
        <section className="pres-hero">
          <div className="pres-hero-inner">
            <span className="pres-badge">For Rent</span>
            <h1>{apt.title}</h1>
            <p className="pres-location">{apt.location}</p>
            <div className="pres-stats">
              {apt.price && <div className="pres-stat"><span className="pres-stat-value">&euro;{apt.price}</span><span className="pres-stat-label">/month</span></div>}
              {apt.rooms && <div className="pres-stat"><span className="pres-stat-value">{apt.rooms}</span><span className="pres-stat-label">Rooms</span></div>}
              {apt.area && <div className="pres-stat"><span className="pres-stat-value">{apt.area}</span><span className="pres-stat-label">m&sup2;</span></div>}
            </div>
          </div>
        </section>

        {photos.length > 0 && (
          <section className="pres-gallery">
            <div className="pres-gallery-main">
              <img src={`/api/apartments/photos/${photos[activePhoto]}`} alt={`${apt.title} ${activePhoto + 1}`} />
              {photos.length > 1 && (
                <>
                  <button className="pres-gallery-arrow left" onClick={() => setActivePhoto((activePhoto - 1 + photos.length) % photos.length)}>&lsaquo;</button>
                  <button className="pres-gallery-arrow right" onClick={() => setActivePhoto((activePhoto + 1) % photos.length)}>&rsaquo;</button>
                </>
              )}
              <span className="pres-gallery-counter">{activePhoto + 1} / {photos.length}</span>
            </div>
            {photos.length > 1 && (
              <div className="pres-gallery-thumbs">
                {photos.map((path, i) => (
                  <button key={i} className={`pres-thumb ${i === activePhoto ? 'active' : ''}`} onClick={() => setActivePhoto(i)}>
                    <img src={`/api/apartments/photos/${path}`} alt={`Thumbnail ${i + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </section>
        )}

        <section className="pres-section">
          <h2>About this apartment</h2>
          {editing ? (
            <div className="pres-editor">
              <label className="pres-field-label">Description</label>
              <textarea value={draftDescription} onChange={e => setDraftDescription(e.target.value)} rows={4} disabled={saving} placeholder="Short description of the apartment..." />
              <label className="pres-field-label">Price (EUR/month)</label>
              <input type="number" step="0.01" value={draftPrice} onChange={e => setDraftPrice(e.target.value)} disabled={saving} placeholder="e.g. 850" className="pres-price-input" />
            </div>
          ) : (
            <>
              {apt.description ? (
                <p className="pres-body-text">{apt.description}</p>
              ) : (
                <p className="pres-empty">No description added yet.</p>
              )}
            </>
          )}
        </section>

        <section className="pres-section">
          <h2>Details &amp; Highlights</h2>
          {editing ? (
            <div className="pres-editor">
              <textarea value={draft} onChange={e => setDraft(e.target.value)} rows={14} disabled={saving} placeholder="Write the presentation content here..." />
            </div>
          ) : content ? (
            <div className="pres-rich-text">
              {content.split('\n').map((line, i) => <React.Fragment key={i}>{line}<br /></React.Fragment>)}
            </div>
          ) : (
            <p className="pres-empty">No details added yet.</p>
          )}
          {isOwner && !editing && (
            <button className="pres-btn outline" onClick={() => { setDraft(content); setDraftPrice(apt.price || ''); setDraftDescription(apt.description || ''); setEditing(true); }}>Edit Presentation</button>
          )}
          {editing && (
            <div className="pres-editor-btns">
              <button className="pres-btn primary" onClick={handleSave} disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
              <button className="pres-btn" onClick={() => { setDraft(content); setEditing(false); }} disabled={saving}>Cancel</button>
            </div>
          )}
        </section>
      </main>

      <footer className="pres-footer">
        <p>Berlin Housing &mdash; Quality apartments for discerning tenants</p>
      </footer>
    </div>
  );
}

export default Presentation;
