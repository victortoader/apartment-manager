import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function InspectionForm() {
  const { t } = useTranslation();
  const { id, inspectionId } = useParams();
  const navigate = useNavigate();
  const { user, authHeader } = useAuth();

  const isOwner = user?.role === 'OWNER';
  const isAdmin = user?.role === 'ADMIN';
  if (!isOwner && !isAdmin) { navigate('/'); return null; }

  const today = new Date().toISOString().slice(0, 10);
  const now = new Date().toTimeString().slice(0, 5);

  const [inspection, setInspection] = useState({
    documentType: 'Übergabeprotokoll',
    date: today,
    time: now,
    companyName: '',
    companyAddress: '',
    companyPostalCode: '',
    companyCity: '',
    companyPhone: '',
    companyEmail: '',
    property: '',
    objectNumber: '',
    rentalObject: '',
    incomingParty: '',
    confirmationsJson: '["Der Zustand der Wohnung wurde gemeinsam besichtigt und dokumentiert.","Spätere Mängel sind innerhalb von 7 Tagen schriftlich anzuzeigen."]',
    signaturesJson: '[{"role":"Vermieter","city":"","date":"","printedName":""},{"role":"Mieter","city":"","date":"","printedName":""}]',
    sections: []
  });

  const [generatedProtocol, setGeneratedProtocol] = useState(null);
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [downloadingPhotos, setDownloadingPhotos] = useState(false);
  const [editId, setEditId] = useState(null);
  const [confText, setConfText] = useState('');
  const mountedRef = useRef(true);
  const doSaveRef = useRef(null);

  useEffect(() => { return () => { mountedRef.current = false; }; }, []);

  const doSave = useCallback(async () => {
    if (!id) return null;
    const body = { ...inspection };
    body.confirmationsJson = JSON.stringify(
      (confText || '').split('\n').filter(l => l.trim())
    );
    const url = editId
      ? `${API}/api/inspections/${editId}`
      : `${API}/api/apartments/${id}/inspections`;
    const method = editId ? 'PUT' : 'POST';
    const res = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(body)
    });
    if (res.ok) {
      const saved = await res.json();
      if (!editId) setEditId(saved.id);
      setInspection(prev => ({
        ...prev,
        sections: (prev.sections || []).map((s, si) => {
          const ss = saved.sections?.[si];
          if (!ss) return s;
          return {
            ...s,
            id: ss.id ?? s.id,
            rows: (s.rows || []).map((r, ri) => {
              const sr = ss.rows?.[ri];
              return sr ? { ...r, id: sr.id ?? r.id } : r;
            })
          };
        })
      }));
      return saved;
    }
    return null;
  }, [id, editId, inspection, confText, authHeader]);

  doSaveRef.current = doSave;

  useEffect(() => {
    if (inspectionId) {
      loadInspection(inspectionId);
    }
    if (id) {
      fetch(`${API}/api/apartments/${id}`, { headers: authHeader() })
        .then(r => r.json())
        .then(apt => {
          setInspection(prev => ({ ...prev, property: apt.title || '' }));
        })
        .catch(() => {});
    }
  }, [id, inspectionId]);

  const loadInspection = async (iid) => {
    const res = await fetch(`${API}/api/inspections/${iid}`, { headers: authHeader() });
    if (res.ok) {
      const data = await res.json();
      setInspection(data);
      setEditId(data.id);
      if (data.generatedProtocolId) {
        fetch(`${API}/api/apartments/protocols/find/${data.generatedProtocolId}`, { headers: authHeader() })
          .then(r => r.ok ? r.json() : null)
          .then(p => { if (p) setGeneratedProtocol({ id: p.id, fileName: p.fileName, originalName: p.originalName }); })
          .catch(() => {});
      }
    }
  };

  const handleChange = (field, value) => {
    setInspection(prev => ({ ...prev, [field]: value }));
  };

  const addSection = () => {
    setInspection(prev => ({
      ...prev,
      sections: [...prev.sections, { id: null, title: '', rows: [] }]
    }));
  };

  const updateSection = (idx, field, value) => {
    setInspection(prev => {
      const sections = [...prev.sections];
      sections[idx] = { ...sections[idx], [field]: value };
      return { ...prev, sections };
    });
  };

  const removeSection = (idx) => {
    setInspection(prev => ({
      ...prev,
      sections: prev.sections.filter((_, i) => i !== idx)
    }));
  };

  const addRow = (secIdx) => {
    setInspection(prev => {
      const sections = [...prev.sections];
      sections[secIdx] = { ...sections[secIdx], rows: [...sections[secIdx].rows, { id: null, detail: '', text: '', status: '', costShare: '', photos: [] }] };
      return { ...prev, sections };
    });
  };

  const updateRow = (secIdx, rowIdx, field, value) => {
    setInspection(prev => {
      const sections = [...prev.sections];
      const rows = [...sections[secIdx].rows];
      rows[rowIdx] = { ...rows[rowIdx], [field]: value };
      sections[secIdx] = { ...sections[secIdx], rows };
      return { ...prev, sections };
    });
  };

  const removeRow = (secIdx, rowIdx) => {
    setInspection(prev => {
      const sections = [...prev.sections];
      sections[secIdx] = { ...sections[secIdx], rows: sections[secIdx].rows.filter((_, i) => i !== rowIdx) };
      return { ...prev, sections };
    });
  };

  const uploadPhoto = async (secIdx, rowIdx, file) => {
    const row = inspection.sections?.[secIdx]?.rows?.[rowIdx];
    if (!row) return;
    let rowId = row.id;
    if (!rowId) {
      const saved = await doSaveRef.current();
      if (!saved) { alert(t('inspection.saveFailed')); return; }
      rowId = saved?.sections?.[secIdx]?.rows?.[rowIdx]?.id;
      if (!rowId) { alert(t('inspection.saveFailed')); return; }
    }
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API}/api/inspections/rows/${rowId}/photos`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    if (res.ok) {
      const photo = await res.json();
      setInspection(prev => {
        const sections = [...prev.sections];
        if (sections[secIdx]?.rows?.[rowIdx]) {
          const rows = [...sections[secIdx].rows];
          rows[rowIdx] = { ...rows[rowIdx], photos: [...(rows[rowIdx].photos || []), photo] };
          sections[secIdx] = { ...sections[secIdx], rows };
        }
        return { ...prev, sections };
      });
    }
  };

  const removePhoto = async (secIdx, rowIdx, photo) => {
    if (photo.id) {
      await fetch(`${API}/api/inspections/photos/${photo.id}`, {
        method: 'DELETE',
        headers: authHeader()
      });
    }
    const row = inspection.sections[secIdx].rows[rowIdx];
    updateRow(secIdx, rowIdx, 'photos', (row.photos || []).filter(p => p !== photo));
  };

  const handleSave = async () => {
    setSaving(true);
    const saved = await doSaveRef.current();
    if (saved) {
      alert(t('inspection.saved'));
    } else {
      alert(t('inspection.saveFailed'));
    }
    setSaving(false);
  };

  const handleGenerate = async () => {
    if (!editId) { alert(t('inspection.saveFirst')); return; }
    setGenerating(true);
    const res = await fetch(`${API}/api/inspections/${editId}/generate`, {
      method: 'POST',
      headers: authHeader()
    });
    if (res.ok) {
      const protocol = await res.json();
      setGeneratedProtocol(protocol);
      alert(t('inspection.generated'));
    } else {
      alert(t('inspection.generateFailed'));
    }
    setGenerating(false);
  };

  const handleDownloadPhotos = async () => {
    if (!editId) return;
    setDownloadingPhotos(true);
    try {
      const res = await fetch(`${API}/api/inspections/${editId}/photos`, { headers: authHeader() });
      if (!res.ok) { alert(t('inspection.downloadPhotosFailed')); return; }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Fotos_${editId}.zip`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } finally {
      setDownloadingPhotos(false);
    }
  };

  const statusOptions = [
    { value: '', label: '-' },
    { value: 'new', label: t('inspection.new') },
    { value: 'normal', label: t('inspection.normal') },
    { value: 'defect', label: t('inspection.defect') },
    { value: 'missing', label: t('inspection.missing') }
  ];

  const hasPhotos = (inspection.sections || []).some(s => (s.rows || []).some(r => (r.photos || []).length > 0));

  return (
    <div className="app">
      <header>
        <button className="btn-back" onClick={() => navigate(`/apartments/${id}`)}>
          {t('back')}
        </button>
        <h1>{t('inspection.title')}</h1>
      </header>

      <div className="inspection-form">
        <section className="insp-section">
          <h2>{t('inspection.companyInfo')}</h2>
          <div className="insp-grid-2">
            <label>{t('inspection.companyName')}<input value={inspection.companyName} onChange={e => handleChange('companyName', e.target.value)} /></label>
            <label>{t('inspection.companyAddress')}<input value={inspection.companyAddress} onChange={e => handleChange('companyAddress', e.target.value)} /></label>
            <label>{t('inspection.postalCode')}<input value={inspection.companyPostalCode} onChange={e => handleChange('companyPostalCode', e.target.value)} /></label>
            <label>{t('inspection.city')}<input value={inspection.companyCity} onChange={e => handleChange('companyCity', e.target.value)} /></label>
            <label>{t('inspection.phone')}<input value={inspection.companyPhone} onChange={e => handleChange('companyPhone', e.target.value)} /></label>
            <label>{t('inspection.email')}<input value={inspection.companyEmail} onChange={e => handleChange('companyEmail', e.target.value)} /></label>
          </div>
        </section>

        <section className="insp-section">
          <h2>{t('inspection.propertyInfo')}</h2>
          <div className="insp-grid-2">
            <label>{t('inspection.property')}<input value={inspection.property} onChange={e => handleChange('property', e.target.value)} /></label>
            <label>{t('inspection.objectNumber')}<input value={inspection.objectNumber} onChange={e => handleChange('objectNumber', e.target.value)} /></label>
            <label>{t('inspection.rentalObject')}<input value={inspection.rentalObject} onChange={e => handleChange('rentalObject', e.target.value)} /></label>
            <label>{t('inspection.incomingParty')}<input value={inspection.incomingParty} onChange={e => handleChange('incomingParty', e.target.value)} /></label>
            <label>{t('inspection.documentType')}<input value={inspection.documentType} onChange={e => handleChange('documentType', e.target.value)} /></label>
            <label>{t('inspection.date')}<input type="date" value={inspection.date} onChange={e => handleChange('date', e.target.value)} /></label>
          </div>
        </section>

        <section className="insp-section">
          <div className="insp-section-header">
            <h2>{t('inspection.sections')}</h2>
            <button className="btn-primary small" onClick={addSection}>{t('inspection.addSection')}</button>
          </div>

          {inspection.sections.map((section, si) => (
            <div key={si} className="insp-card">
              <div className="insp-card-header">
                <input
                  className="insp-section-title-input"
                  placeholder={t('inspection.sectionPlaceholder')}
                  value={section.title}
                  onChange={e => updateSection(si, 'title', e.target.value)}
                />
                <button className="btn-delete small" onClick={() => removeSection(si)}>{t('delete')}</button>
              </div>

              {section.rows.map((row, ri) => (
                <div key={ri} className="insp-row">
                  <div className="insp-row-fields">
                    <input
                      placeholder={t('inspection.detailPlaceholder')}
                      value={row.detail}
                      onChange={e => updateRow(si, ri, 'detail', e.target.value)}
                    />
                    <textarea
                      placeholder={t('inspection.textPlaceholder')}
                      value={row.text}
                      onChange={e => updateRow(si, ri, 'text', e.target.value)}
                      rows={2}
                    />
                    <div className="insp-row-controls">
                      <select value={row.status} onChange={e => updateRow(si, ri, 'status', e.target.value)}>
                        {statusOptions.map(o => (
                          <option key={o.value} value={o.value}>{o.label}</option>
                        ))}
                      </select>
                      <input
                        placeholder={t('inspection.costShare')}
                        value={row.costShare}
                        onChange={e => updateRow(si, ri, 'costShare', e.target.value)}
                        className="insp-cost-input"
                      />
                      <label className="btn-upload small" style={{ fontSize: 11 }}>
                        {t('inspection.photo')}
                        <input type="file" accept="image/*" hidden onChange={e => { if (e.target.files[0]) uploadPhoto(si, ri, e.target.files[0]); }} />
                      </label>
                      <button className="btn-delete small" onClick={() => removeRow(si, ri)}>×</button>
                    </div>
                    {row.photos && row.photos.length > 0 && (
                      <div className="insp-photo-list">
                        {row.photos.map((ph, pi) => (
                          <span key={pi} className="insp-photo-chip">
                            {ph.originalFileName || t('inspection.photo')}
                            <button onClick={() => removePhoto(si, ri, ph)}>×</button>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}
              <button className="btn-upload small" onClick={() => addRow(si)}>{t('inspection.addRow')}</button>
            </div>
          ))}
        </section>

        <section className="insp-section">
          <h2>{t('inspection.confirmations')}</h2>
          <textarea
            className="insp-conf-textarea"
            rows={4}
            placeholder={t('inspection.confirmationsPlaceholder')}
            value={confText || (
              (() => { try { return JSON.parse(inspection.confirmationsJson || '[]').join('\n'); } catch { return ''; } })()
            )}
            onChange={e => setConfText(e.target.value)}
          />
        </section>

        <section className="insp-section">
          <h2>{t('inspection.signatures')}</h2>
          {(() => {
            let sigs;
            try { sigs = JSON.parse(inspection.signaturesJson || '[]'); } catch { sigs = []; }
            if (!sigs.length) sigs = [{ role: '', city: '', date: '', printedName: '' }];
            return sigs.map((sig, si) => (
              <div key={si} className="insp-signature-row">
                <input placeholder={t('inspection.signatureRole')} value={sig.role} onChange={e => {
                  const copy = [...sigs]; copy[si] = { ...copy[si], role: e.target.value };
                  handleChange('signaturesJson', JSON.stringify(copy));
                }} />
                <input placeholder={t('inspection.city')} value={sig.city} onChange={e => {
                  const copy = [...sigs]; copy[si] = { ...copy[si], city: e.target.value };
                  handleChange('signaturesJson', JSON.stringify(copy));
                }} />
                <input type="date" value={sig.date} onChange={e => {
                  const copy = [...sigs]; copy[si] = { ...copy[si], date: e.target.value };
                  handleChange('signaturesJson', JSON.stringify(copy));
                }} />
                <input placeholder={t('inspection.printedName')} value={sig.printedName} onChange={e => {
                  const copy = [...sigs]; copy[si] = { ...copy[si], printedName: e.target.value };
                  handleChange('signaturesJson', JSON.stringify(copy));
                }} />
                <button className="btn-delete small" onClick={() => {
                  const copy = sigs.filter((_, i) => i !== si);
                  handleChange('signaturesJson', JSON.stringify(copy.length ? copy : [{}]));
                }}>×</button>
              </div>
            ));
          })()}
          <button className="btn-upload small" onClick={() => {
            let sigs;
            try { sigs = JSON.parse(inspection.signaturesJson || '[]'); } catch { sigs = []; }
            sigs.push({ role: '', city: '', date: '', printedName: '' });
            handleChange('signaturesJson', JSON.stringify(sigs));
          }}>{t('inspection.addSignature')}</button>
        </section>

        <div className="insp-actions">
          <button className="btn-primary" onClick={handleSave} disabled={saving}>
            {saving ? t('inspection.saving') : t('inspection.save')}
          </button>
          <button className="btn-primary" onClick={handleGenerate} disabled={!editId || generating}>
            {generating ? t('inspection.generating') : t('inspection.generatePdf')}
          </button>
          {generatedProtocol && (
            <button className="btn-primary" onClick={async () => {
              const res = await fetch(`${API}/api/apartments/protocols/${generatedProtocol.fileName}`, { headers: authHeader() });
              if (!res.ok) { alert(t('inspection.generateFailed')); return; }
              const blob = await res.blob();
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url;
              a.download = generatedProtocol.originalName || 'protocol.pdf';
              document.body.appendChild(a);
              a.click();
              document.body.removeChild(a);
              URL.revokeObjectURL(url);
            }}>
              {t('inspection.downloadPdf')}
            </button>
          )}
          {editId && hasPhotos && (
            <button className="btn-primary" onClick={handleDownloadPhotos} disabled={downloadingPhotos}>
              {downloadingPhotos ? t('inspection.downloadingPhotos') : t('inspection.downloadPhotos')}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default InspectionForm;
