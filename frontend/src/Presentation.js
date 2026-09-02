import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import './App.css';

function Presentation() {
  const { t } = useTranslation();
  const { id } = useParams();
  const { user } = useAuth();
  const isOwner = user && (user.role === 'OWNER' || user.role === 'ADMIN');
  const [apt, setApt] = useState(null);
  const [content, setContent] = useState('');
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [draftPrice, setDraftPrice] = useState('');
  const [draftDescription, setDraftDescription] = useState('');
  const [draftStatus, setDraftStatus] = useState('');
  const [draftAvailableFrom, setDraftAvailableFrom] = useState('');
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
        description: draftDescription,
        status: draftStatus,
        availableFrom: draftAvailableFrom || null
      })
    });

    setContent(draft);
    setApt({ ...apt, presentation: draft, price: draftPrice !== '' ? parseFloat(draftPrice) : apt.price, description: draftDescription, status: draftStatus, availableFrom: draftAvailableFrom || null });
    setEditing(false);
    setSaving(false);
  };

  if (loading) return <div className="pres"><p className="loading">{t('loading')}</p></div>;
  if (!apt) return <div className="pres"><p className="error">{t('presentation.notFound')}</p></div>;

  const photos = apt.photoPaths || [];

  return (
    <div className="pres">
      <header className="pres-header">
        <Link to="/presentations" className="pres-logo">{t('presentation.brand')}</Link>
        <nav className="pres-nav">
          <Link to="/presentations" className="pres-nav-link">{t('presentationList.title')}</Link>
          {user && <Link to="/" className="pres-nav-link">{t('presentation.dashboard')}</Link>}
          {isOwner && <Link to={`/apartments/${id}`} className="pres-nav-link">{t('presentation.management')}</Link>}
        </nav>
      </header>

      <main className="pres-main">
        <section className="pres-hero">
          <div className="pres-hero-inner">
            <span className={`pl-status ${apt.status === 'RENTED' ? 'status-rented' : apt.status === 'AVAILABLE_FROM' ? 'status-available-from' : 'status-available'}`}>
              {apt.status === 'RENTED' ? t('presentationList.rented') : apt.status === 'AVAILABLE_FROM' ? t('presentationList.availableFrom') + (apt.availableFrom ? ' ' + apt.availableFrom : '') : t('presentationList.availableNow')}
            </span>
            <h1>{apt.title}</h1>
            <p className="pres-location">{apt.location}</p>
            <div className="pres-stats">
              {apt.price && <div className="pres-stat"><span className="pres-stat-value">&euro;{apt.price}</span><span className="pres-stat-label">{t('presentation.perMonth')}</span></div>}
              {apt.rooms && <div className="pres-stat"><span className="pres-stat-value">{apt.rooms}</span><span className="pres-stat-label">{t('presentation.rooms')}</span></div>}
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
          <h2>{t('presentation.aboutApartment')}</h2>
          {editing ? (
            <div className="pres-editor">
              <label className="pres-field-label">{t('presentation.description')}</label>
              <textarea value={draftDescription} onChange={e => setDraftDescription(e.target.value)} rows={4} disabled={saving} placeholder={t('presentation.descriptionPlaceholder')} />
              <label className="pres-field-label">{t('presentation.priceLabel')}</label>
              <input type="number" step="0.01" value={draftPrice} onChange={e => setDraftPrice(e.target.value)} disabled={saving} placeholder={t('presentation.pricePlaceholder')} className="pres-price-input" />
              <label className="pres-field-label">{t('presentationList.status')}</label>
              <select value={draftStatus} onChange={e => setDraftStatus(e.target.value)} className="pres-select" disabled={saving}>
                <option value="AVAILABLE_IMMEDIATELY">{t('presentationList.availableNow')}</option>
                <option value="AVAILABLE_FROM">{t('presentationList.availableFromDate')}</option>
                <option value="RENTED">{t('presentationList.rented')}</option>
              </select>
              {draftStatus === 'AVAILABLE_FROM' && (
                <>
                  <label className="pres-field-label">{t('presentationList.availableFromDate')}</label>
                  <input type="date" value={draftAvailableFrom} onChange={e => setDraftAvailableFrom(e.target.value)} className="pres-price-input" disabled={saving} />
                </>
              )}
            </div>
          ) : (
            <>
              {apt.description ? (
                <p className="pres-body-text">{apt.description}</p>
              ) : (
                <p className="pres-empty">{t('presentation.noDescription')}</p>
              )}
            </>
          )}
        </section>

        <section className="pres-section">
          <h2>{t('presentation.detailsHighlights')}</h2>
          {editing ? (
            <div className="pres-editor">
              <textarea value={draft} onChange={e => setDraft(e.target.value)} rows={14} disabled={saving} placeholder={t('presentation.detailsPlaceholder')} />
            </div>
          ) : content ? (
            <div className="pres-rich-text">
              {content.split('\n').map((line, i) => <React.Fragment key={i}>{line}<br /></React.Fragment>)}
            </div>
          ) : (
            <p className="pres-empty">{t('presentation.noDetails')}</p>
          )}
          {isOwner && !editing && (
            <button className="pres-btn outline" onClick={() => { setDraft(content); setDraftPrice(apt.price || ''); setDraftDescription(apt.description || ''); setDraftStatus(apt.status || 'AVAILABLE_IMMEDIATELY'); setDraftAvailableFrom(apt.availableFrom || ''); setEditing(true); }}>{t('presentation.editPresentation')}</button>
          )}
          {editing && (
            <div className="pres-editor-btns">
              <button className="pres-btn primary" onClick={handleSave} disabled={saving}>{saving ? t('presentation.saving') : t('save')}</button>
              <button className="pres-btn" onClick={() => { setDraft(content); setEditing(false); }} disabled={saving}>{t('cancel')}</button>
            </div>
          )}
        </section>
      </main>

      <footer className="pres-footer">
        <p>{t('presentation.brand')} &mdash; {t('presentation.footerText')}</p>
      </footer>
    </div>
  );
}

export default Presentation;
