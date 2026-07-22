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

const DOCUMENT_TYPES = [
  { key: 'bill', apiValue: 'bill' },
  { key: 'proof', apiValue: 'proof' }
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
  const [selectedDocType, setSelectedDocType] = useState('bill');
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [editAmount, setEditAmount] = useState('');
  const [editCurrency, setEditCurrency] = useState('');

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
    formData.append('documentType', selectedDocType);
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

  const handleUpdateAmount = async (billId) => {
    const amount = editAmount === '' ? null : parseFloat(editAmount);
    const currency = editCurrency || null;
    try {
      const res = await fetch(`${API}/api/bills/${billId}/amount`, {
        method: 'PUT',
        headers: { ...authHeader(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount, currency })
      });
      if (res.ok) {
        const updated = await res.json();
        setBills(prev => prev.map(b => b.id === billId ? { ...b, extractedAmount: updated.extractedAmount, extractedCurrency: updated.extractedCurrency } : b));
      }
    } catch (err) {
      console.error('Update amount failed:', err);
    }
    setEditingId(null);
    setEditAmount('');
    setEditCurrency('');
  };

  const startEditAmount = (bill) => {
    setEditingId(bill.id);
    setEditAmount(bill.extractedAmount != null ? bill.extractedAmount.toString() : '');
    setEditCurrency(bill.extractedCurrency || 'EUR');
  };

  const getConfidenceColor = (confidence) => {
    if (confidence >= 0.85) return 'confidence-high';
    if (confidence >= 0.60) return 'confidence-medium';
    return 'confidence-low';
  };

  const grouped = groupByMonth(bills, t);
  const getFileExtension = (name) => {
    if (!name) return '';
    const parts = name.split('.');
    return parts.length > 1 ? parts.pop().toUpperCase() : '';
  };

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
          <div className="paid-bills-form-row">
            <select
              className="paid-bills-select"
              value={selectedType}
              onChange={e => setSelectedType(e.target.value)}
            >
              {BILL_TYPES.map(bt => (
                <option key={bt.key} value={bt.apiValue}>{t(`paidBills.billTypes.${bt.key}`)}</option>
              ))}
            </select>
            <select
              className="paid-bills-select"
              value={selectedDocType}
              onChange={e => setSelectedDocType(e.target.value)}
            >
              {DOCUMENT_TYPES.map(dt => (
                <option key={dt.key} value={dt.apiValue}>{t(`paidBills.documentTypes.${dt.key}`)}</option>
              ))}
            </select>
          </div>
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
                    <span className={`paid-bills-doc-label ${bill.documentType === 'proof' ? 'proof' : 'bill'}`}>
                      {bill.documentType === 'proof' ? t('paidBills.documentTypes.proof') : t('paidBills.documentTypes.bill')}
                    </span>
                    <span className="paid-bills-format">{getFileExtension(bill.originalFileName)}</span>
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
                    <span className="paid-bills-ocr">
                      {editingId === bill.id ? (
                        <span className="ocr-edit">
                          <select
                            className="ocr-currency-select"
                            value={editCurrency}
                            onChange={e => setEditCurrency(e.target.value)}
                          >
                            <option value="RON">RON</option>
                            <option value="EUR">EUR</option>
                            <option value="CHF">CHF</option>
                            <option value="USD">USD</option>
                          </select>
                          <input
                            type="number"
                            step="0.01"
                            className="ocr-amount-input"
                            value={editAmount}
                            onChange={e => setEditAmount(e.target.value)}
                            placeholder="0.00"
                          />
                          <button className="btn-save tiny" onClick={() => handleUpdateAmount(bill.id)}>✓</button>
                          <button className="btn-cancel tiny" onClick={() => setEditingId(null)}>×</button>
                        </span>
                      ) : bill.extractedAmount != null ? (
                        <span className="ocr-result">
                          <span className={`ocr-amount ${getConfidenceColor(bill.ocrConfidence)}`}>
                            {bill.extractedAmount.toFixed(2)} {bill.extractedCurrency || 'EUR'}
                          </span>
                          {bill.ocrConfidence != null && (
                            <span className="ocr-confidence" title={t('paidBills.ocr.confidence')}>
                              {Math.round(bill.ocrConfidence * 100)}%
                            </span>
                          )}
                          <button className="btn-edit tiny" onClick={() => startEditAmount(bill)} title={t('paidBills.ocr.editAmount')}>✎</button>
                        </span>
                      ) : bill.ocrFailed ? (
                        <span className="ocr-failed">
                          <span className="ocr-no-amount">{t('paidBills.ocr.notRecognized')}</span>
                          <button className="btn-edit tiny" onClick={() => startEditAmount(bill)} title={t('paidBills.ocr.editAmount')}>✎</button>
                        </span>
                      ) : (
                        <span className="ocr-waiting">{t('paidBills.ocr.processing')}</span>
                      )}
                    </span>
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
