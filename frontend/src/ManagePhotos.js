import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function ManagePhotos() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { authHeader } = useAuth();
  const [apartment, setApartment] = useState(null);
  const [uploadError, setUploadError] = useState(null);

  useEffect(() => {
    fetchApartment();
  }, [id]);

  const fetchApartment = async () => {
    const res = await fetch(`${API}/api/apartments/${id}`, { headers: authHeader() });
    if (res.ok) {
      setApartment(await res.json());
    } else if (res.status === 403 || res.status === 404) {
      navigate('/');
    }
  };

  const handlePhotoUpload = async (event) => {
    const file = event.target.files[0];
    event.target.value = '';
    setUploadError(null);
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API}/api/apartments/${id}/photos`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    if (!res.ok) {
      setUploadError(t('apartmentList.photoUploadError'));
    } else {
      fetchApartment();
    }
  };

  const handleDeletePhoto = async (fileName) => {
    if (window.confirm(t('apartmentList.deletePhotoConfirm'))) {
      const res = await fetch(`${API}/api/apartments/${id}/photos/${encodeURIComponent(fileName)}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      if (res.ok) fetchApartment();
    }
  };

  const handleSetMainPhoto = async (fileName) => {
    const res = await fetch(`${API}/api/apartments/${id}/photos/main`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify({ fileName })
    });
    if (res.ok) fetchApartment();
  };

  if (!apartment) return <div className="app"><p>{t('loading')}</p></div>;

  return (
    <div className="app">
      <header>
        <button className="btn-back" onClick={() => navigate('/')}>{t('detail.back')}</button>
        <h1>{apartment.title}</h1>
      </header>

      <div className="manage-photos-page">
        <div className="protocols-header">
          <h2>{t('apartmentList.managePhotos')}</h2>
          <label className="btn-upload">
            {t('apartmentList.addPhoto')}
            <input type="file" accept="image/*" hidden onChange={handlePhotoUpload} />
          </label>
        </div>

        {uploadError && <p className="login-error">{uploadError}</p>}

        {apartment.photoPaths && apartment.photoPaths.length > 0 ? (
          <div className="photo-manager-list">
            {apartment.photoPaths.map((path, i) => (
              <div key={i} className={`photo-manager-item${i === 0 ? ' main' : ''}`}>
                <img src={`${API}/api/apartments/photos/${path}`} alt={`Photo ${i + 1}`} />
                {i === 0 && <span className="photo-main-badge">{t('apartmentList.mainPhoto')}</span>}
                <div className="photo-manager-actions">
                  {i !== 0 && (
                    <button className="btn-upload small" onClick={() => handleSetMainPhoto(path)}>{t('apartmentList.setMainPhoto')}</button>
                  )}
                  <button className="btn-delete small" onClick={() => handleDeletePhoto(path)}>{t('apartmentList.deletePhoto')}</button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="metadata-empty">{t('apartmentList.noPhotos')}</p>
        )}
      </div>
    </div>
  );
}

export default ManagePhotos;
