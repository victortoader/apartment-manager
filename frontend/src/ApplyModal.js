import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';

const API = process.env.REACT_APP_API_URL || '';

function ApplyModal({ apartment, onClose, onSuccess }) {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !file) {
      setError(t('presentationList.applyErrorRequired'));
      return;
    }
    setUploading(true);
    setError('');
    const formData = new FormData();
    formData.append('apartmentId', apartment.id);
    formData.append('applicantName', name.trim());
    formData.append('applicantEmail', email.trim());
    formData.append('applicantPhone', phone.trim());
    formData.append('file', file);
    try {
      const res = await fetch(`${API}/api/applications`, {
        method: 'POST',
        body: formData
      });
      if (res.ok) {
        setSuccess(true);
        setTimeout(() => { onSuccess(); }, 2000);
      } else {
        const data = await res.json();
        setError(data.error || t('presentationList.applyError'));
      }
    } catch {
      setError(t('presentationList.applyError'));
    }
    setUploading(false);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box apply-modal" onClick={e => e.stopPropagation()}>
        {success ? (
          <>
            <h3>{t('presentationList.applySuccess')}</h3>
            <p>{t('presentationList.applySuccessMsg')}</p>
          </>
        ) : (
          <>
            <h3>{t('presentationList.applyFor')} {apartment.title}</h3>
            <form className="apply-form" onSubmit={handleSubmit}>
              {error && <div className="login-error">{error}</div>}
              <input
                type="text"
                placeholder={t('presentationList.applicantName')}
                value={name}
                onChange={e => setName(e.target.value)}
                required
              />
              <input
                type="email"
                placeholder={t('presentationList.applicantEmail')}
                value={email}
                onChange={e => setEmail(e.target.value)}
                required
              />
              <input
                type="tel"
                placeholder={t('presentationList.applicantPhone')}
                value={phone}
                onChange={e => setPhone(e.target.value)}
              />
              <div className="apply-file-row">
                <label className="btn-upload">
                  {file ? file.name : t('presentationList.chooseFile')}
                  <input type="file" hidden onChange={e => setFile(e.target.files[0])} />
                </label>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-cancel small" onClick={onClose}>{t('cancel')}</button>
                <button type="submit" className="btn-primary small" disabled={uploading}>
                  {uploading ? t('presentationList.submitting') : t('presentationList.submit')}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}

export default ApplyModal;
