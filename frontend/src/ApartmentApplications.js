import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function ApartmentApplications() {
  const { t } = useTranslation();
  const { id } = useParams();
  const { authHeader } = useAuth();
  const [applications, setApplications] = useState([]);
  const [apartment, setApartment] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetch(`${API}/api/apartments/${id}`, { headers: authHeader() }).then(r => r.ok ? r.json() : null),
      fetch(`${API}/api/applications?apartmentId=${id}`, { headers: authHeader() }).then(r => r.ok ? r.json() : [])
    ]).then(([apt, apps]) => {
      setApartment(apt);
      setApplications(apps);
      setLoading(false);
    });
  }, [id]);

  if (loading) return <div className="app"><p>{t('loading')}</p></div>;

  return (
    <div className="app">
      <header>
        <Link to="/" className="btn-back">{t('detail.back')}</Link>
        <h1>{apartment ? apartment.title : t('loading')} {t('detail.applications')}</h1>
      </header>

      <div className="user-management">
        {applications.length === 0 ? (
          <p className="empty-protocols">{t('apartmentList.noApplications')}</p>
        ) : (
          <div className="application-list">
            {applications.map(app => (
              <div key={app.id} className="application-card">
                <div className="application-card-header">
                  <span className="application-name">{app.applicantName}</span>
                  <span className="application-date">{new Date(app.submittedAt).toLocaleDateString()}</span>
                </div>
                <div className="application-card-body">
                  <div className="application-detail-row">
                    <span className="application-detail-label">{t('presentationList.applicantEmail')}</span>
                    <span className="application-detail-value">{app.applicantEmail}</span>
                  </div>
                  {app.applicantPhone && (
                    <div className="application-detail-row">
                      <span className="application-detail-label">{t('presentationList.applicantPhone')}</span>
                      <span className="application-detail-value">{app.applicantPhone}</span>
                    </div>
                  )}
                </div>
                <div className="application-card-footer">
                  <a href={`${API}/api/applications/files/${app.storedFileName}`} target="_blank" rel="noreferrer" className="btn-sm">
                    {app.originalFileName}
                  </a>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default ApartmentApplications;
