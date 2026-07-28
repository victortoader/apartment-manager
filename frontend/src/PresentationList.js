import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import ApplyModal from './ApplyModal';

const API = process.env.REACT_APP_API_URL || '';

const LANGUAGES = [
  { code: 'en', label: 'EN' },
  { code: 'de', label: 'DE' },
  { code: 'it', label: 'IT' },
  { code: 'fr', label: 'FR' },
  { code: 'ro', label: 'RO' }
];

function PresentationList() {
  const { t, i18n } = useTranslation();
  const { user, logout } = useAuth();
  const [apartments, setApartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [applyApt, setApplyApt] = useState(null);

  useEffect(() => {
    fetch(`${API}/api/apartments/presentation-list`)
      .then(r => r.json())
      .then(data => {
        setApartments(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  const isAvailable = (apt) => apt.status === 'AVAILABLE_IMMEDIATELY' || apt.status === 'AVAILABLE_FROM';

  const statusLabel = (apt) => {
    if (apt.status === 'RENTED') return t('presentationList.rented');
    if (apt.status === 'AVAILABLE_IMMEDIATELY') return t('presentationList.availableNow');
    if (apt.status === 'AVAILABLE_FROM' && apt.availableFrom) return t('presentationList.availableFrom') + ' ' + apt.availableFrom;
    return t('presentationList.availableNow');
  };

  const statusClass = (apt) => {
    if (apt.status === 'RENTED') return 'status-rented';
    if (apt.status === 'AVAILABLE_IMMEDIATELY') return 'status-available';
    return 'status-available-from';
  };

  if (loading) return <div className="pres"><p className="loading">{t('loading')}</p></div>;

  return (
    <div className="pres">
      <header className="pres-header">
        <Link to="/presentations" className="pres-logo">{t('presentation.brand')}</Link>
        <nav className="pres-nav">
          {user ? (
            <>
              <Link to="/" className="pres-nav-link">{t('presentation.dashboard')}</Link>
              <span className="header-user">{user.username} ({user.role})</span>
              <button className="btn-back" onClick={logout}>{t('logout')}</button>
            </>
          ) : (
            <>
              <Link to="/login" className="pres-nav-link">{t('login.signIn')}</Link>
            </>
          )}
          <div className="lang-switcher">
            {LANGUAGES.map(l => (
              <button
                key={l.code}
                className={`lang-btn${i18n.language === l.code ? ' active' : ''}`}
                onClick={() => i18n.changeLanguage(l.code)}
              >
                {l.label}
              </button>
            ))}
          </div>
        </nav>
      </header>

      <main className="pres-main">
        <section className="pres-hero">
          <div className="pres-hero-inner">
            <h1>{t('presentationList.title')}</h1>
            <p className="pres-location">{t('presentationList.subtitle')}</p>
          </div>
        </section>

        <div className="pl-grid">
          {apartments.map(apt => (
            <div key={apt.id} className="pl-card">
              <div className="pl-card-photo">
                <img
                  src={apt.photoPaths && apt.photoPaths.length > 0
                    ? `${API}/api/apartments/photos/${apt.photoPaths[0]}`
                    : '/placeholder.svg'}
                  alt={apt.title}
                />
              </div>
              <div className="pl-card-body">
                <div className="pl-card-header">
                  <h3>{apt.title}</h3>
                  <span className={`pl-status ${statusClass(apt)}`}>{statusLabel(apt)}</span>
                </div>
                <p className="pl-location">{apt.location}</p>
                <div className="pl-details">
                  {apt.price && <span className="pl-price">&euro;{apt.price} {t('presentation.perMonth')}</span>}
                  <span className="pl-specs">{apt.rooms} {t('detail.rooms')} &middot; {apt.area} m&sup2;</span>
                </div>
                {apt.description && <p className="pl-desc">{apt.description}</p>}
                <div className="pl-actions">
                  <Link to={`/presentations/apartments/${apt.id}`} className="pres-btn outline small">{t('presentationList.viewDetails')}</Link>
                  {isAvailable(apt) && (
                    <button className="pres-btn primary small" onClick={() => setApplyApt(apt)}>{t('presentationList.apply')}</button>
                  )}
                </div>
              </div>
            </div>
          ))}
          {apartments.length === 0 && <p className="empty">{t('presentationList.noApartments')}</p>}
        </div>
      </main>

      <footer className="pres-footer">
        <p>{t('presentation.brand')} &mdash; {t('presentation.footerText')}</p>
      </footer>

      {applyApt && (
        <ApplyModal
          apartment={applyApt}
          onClose={() => setApplyApt(null)}
          onSuccess={() => setApplyApt(null)}
        />
      )}
    </div>
  );
}

export default PresentationList;
