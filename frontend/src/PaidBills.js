import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

const BILL_TYPES = [
  { key: 'maintenance', apiValue: 'Monthly Maintenance Fee' },
  { key: 'electricity', apiValue: 'Electricity Bill' },
  { key: 'internet', apiValue: 'Internet Subscription' },
  { key: 'other', apiValue: 'Other Payments' }
];

const BILL_TYPE_TO_KEY = Object.fromEntries(BILL_TYPES.map(bt => [bt.apiValue, bt.key]));

function groupByMonth(bills, t) {
  const groups = {};
  bills.forEach(bill => {
    const date = new Date(bill.uploadDate);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    const monthName = t(`paidBills.months.${date.getMonth()}`);
    const label = `${monthName} ${date.getFullYear()}`;
    if (!groups[key]) groups[key] = { label, items: [] };
    groups[key].items.push(bill);
  });
  return Object.values(groups);
}

function PaidBills({ apartmentId }) {
  const { t } = useTranslation();
  const { user, authHeader } = useAuth();
  const isTenant = user?.role === 'TENANT';
  const isOwner = user?.role === 'OWNER';
  const canUpload = isTenant;
  const canDelete = isOwner;
  const [bills, setBills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [selectedType, setSelectedType] = useState(BILL_TYPES[0].apiValue);
  const [showForm, setShowForm] = useState(false);

  useEffect(() => { fetchBills(); }, [apartmentId]);

  const fetchBills = async () => {
    setLoading(true);
    const res = await fetch(`${API}/api/apartments/${apartmentId}/bills`, { headers: authHeader() });
    if (res.ok) {
      const data = await res.json();
      setBills(data);
    }
    setLoading(false);
  };

  const handleUpload = async (file) => {
    if (!file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('billType', selectedType);
    await fetch(`${API}/api/apartments/${apartmentId}/bills`, {
      method: 'POST',
      headers: { 'Authorization': authHeader().Authorization },
      body: formData
    });
    setUploading(false);
    setShowForm(false);
    fetchBills();
  };

  const handleDelete = async (billId) => {
    if (!window.confirm(t('paidBills.deleteConfirm'))) return;
    await fetch(`${API}/api/bills/${billId}`, {
      method: 'DELETE',
      headers: authHeader()
    });
    fetchBills();
  };

  const grouped = groupByMonth(bills, t);
  const isPdf = (name) => /\.pdf$/i.test(name);

  return (
    <div className="paid-bills-section">
      <div className="paid-bills-header">
        <h3>{t('paidBills.title')}</h3>
        {canUpload && !showForm && (
          <button className="btn-upload small" onClick={() => setShowForm(true)}>
            {t('paidBills.uploadProof')}
          </button>
        )}
      </div>

      {canUpload && showForm && (
        <div className="paid-bills-form">
          <select
            className="paid-bills-select"
            value={selectedType}
            onChange={e => setSelectedType(e.target.value)}
          >
            {BILL_TYPES.map(bt => (
              <option key={bt.key} value={bt.apiValue}>{t(`paidBills.billTypes.${bt.key}`)}</option>
            ))}
          </select>
          <div className="paid-bills-form-row">
            <label className="btn-upload small">
              {uploading ? t('paidBills.uploading') : t('paidBills.chooseFile')}
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png,.gif,.webp"
                hidden
                disabled={uploading}
                onChange={(e) => handleUpload(e.target.files[0])}
              />
            </label>
            <button className="btn-cancel small" onClick={() => setShowForm(false)} disabled={uploading}>{t('cancel')}</button>
          </div>
        </div>
      )}

      {loading ? (
        <p className="paid-bills-empty">{t('loading')}</p>
      ) : bills.length === 0 ? (
        <p className="paid-bills-empty">{t('paidBills.noPayments')}</p>
      ) : (
        <div className="paid-bills-groups">
          {grouped.map(group => (
            <div key={group.label} className="paid-bills-month">
              <h4 className="paid-bills-month-label">{group.label}</h4>
              <ul className="paid-bills-list">
                {group.items.map(bill => (
                  <li key={bill.id} className="paid-bills-item">
                    <span className={`paid-bills-icon ${isPdf(bill.originalFileName) ? 'pdf' : 'img'}`}>
                      {isPdf(bill.originalFileName) ? 'PDF' : 'IMG'}
                    </span>
                    <span className="paid-bills-type">{t(`paidBills.billTypes.${BILL_TYPE_TO_KEY[bill.billType] || 'other'}`)}</span>
                    <a
                      href={`${API}/api/bills/${bill.storedFileName}`}
                      target="_blank"
                      rel="noreferrer"
                      className="paid-bills-link"
                    >
                      {bill.originalFileName}
                    </a>
                    <span className="paid-bills-date">
                      {new Date(bill.uploadDate).toLocaleDateString()}
                    </span>
                    {bill.uploadedBy && (
                      <span className="paid-bills-uploader">{bill.uploadedBy.username}</span>
                    )}
                    {canDelete && (
                      <button className="btn-delete tiny" onClick={() => handleDelete(bill.id)}>×</button>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PaidBills;
