import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';

const API = process.env.REACT_APP_API_URL || '';

const SECTIONS = [
  { key: 'property', labelKey: 'application.section.property' },
  { key: 'mainApplicant', labelKey: 'application.section.mainApplicant' },
  { key: 'coApplicant', labelKey: 'application.section.coApplicant' },
  { key: 'employmentMain', labelKey: 'application.section.employmentMain' },
  { key: 'employmentCo', labelKey: 'application.section.employmentCo' },
  { key: 'housingMain', labelKey: 'application.section.housingMain' },
  { key: 'housingCo', labelKey: 'application.section.housingCo' },
  { key: 'referencesMain', labelKey: 'application.section.referencesMain' },
  { key: 'referencesCo', labelKey: 'application.section.referencesCo' },
  { key: 'additional', labelKey: 'application.section.additional' },
  { key: 'documents', labelKey: 'application.section.documents' },
];

const FIELDS = [
  { section: 'property', name: 'Liegenschaft/Ort', labelKey: 'application.property.location' },
  { section: 'property', name: 'abaRef.-Nr.', labelKey: 'application.property.abaRef' },
  { section: 'property', name: 'Wohnung', labelKey: 'application.property.apartment' },
  { section: 'property', name: 'MZ Wohnung inkl. NK', labelKey: 'application.property.rent' },
  { section: 'property', name: 'Anz. Einstellplätze', labelKey: 'application.property.indoorParking', type: 'number' },
  { section: 'property', name: 'Anz. Aussen-Parkplätze', labelKey: 'application.property.outdoorParking', type: 'number' },
  { section: 'property', name: 'Gewünschter Mietbeginn', labelKey: 'application.property.desiredStart', type: 'date' },
  { section: 'property', name: 'Kontrollschild-Nr.', labelKey: 'application.property.licensePlate' },
  { section: 'property', name: 'Ex-Mieter', labelKey: 'application.property.prevTenant' },
  { section: 'property', name: 'Nutzung als (Familienwohnung / Neben-/Zweitwohnung / Wohnung von Unverheirateten / Wohngemeinschaft)', labelKey: 'application.property.usage' },

  { section: 'mainApplicant', name: 'Mietinteressent/in Anrede (Herr / Frau)', labelKey: 'application.main.salutation' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Rolle (Mietinteressent/in / Ehepartner/in / Partner/in / Solidarhafter/in)', labelKey: 'application.main.role' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Familienname', labelKey: 'application.main.lastName' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Vorname', labelKey: 'application.main.firstName' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Strasse / Nr.', labelKey: 'application.main.street' },
  { section: 'mainApplicant', name: 'Mietinteressent/in PLZ/Ort', labelKey: 'application.main.city' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Telefon Privat', labelKey: 'application.main.phonePrivate', type: 'tel' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Telefon Geschäft', labelKey: 'application.main.phoneBusiness', type: 'tel' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Mobiltelefon', labelKey: 'application.main.phoneMobile', type: 'tel' },
  { section: 'mainApplicant', name: 'Mietinteressent/in E-Mail', labelKey: 'application.main.email', type: 'email' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Geburtsdatum', labelKey: 'application.main.birthDate', type: 'date' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Zivilstand', labelKey: 'application.main.maritalStatus' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Heimatort/Land', labelKey: 'application.main.hometown' },
  { section: 'mainApplicant', name: 'Mietinteressent/in Aufenthaltsbewilligung (A / B / C / F)', labelKey: 'application.main.residencePermit' },

  { section: 'coApplicant', name: 'Co-Mietinteressent/in Anrede (Herr / Frau)', labelKey: 'application.co.salutation' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Rolle (Ehepartner/in / Partner/in / Solidarhafter/in)', labelKey: 'application.co.role' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Familienname', labelKey: 'application.co.lastName' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Vorname', labelKey: 'application.co.firstName' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Strasse / Nr.', labelKey: 'application.co.street' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in PLZ/Ort', labelKey: 'application.co.city' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Telefon Privat', labelKey: 'application.co.phonePrivate', type: 'tel' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Telefon Geschäft', labelKey: 'application.co.phoneBusiness', type: 'tel' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Mobiltelefon', labelKey: 'application.co.phoneMobile', type: 'tel' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in E-Mail', labelKey: 'application.co.email', type: 'email' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Geburtsdatum', labelKey: 'application.co.birthDate', type: 'date' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Zivilstand', labelKey: 'application.co.maritalStatus' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Heimatort/Land', labelKey: 'application.co.hometown' },
  { section: 'coApplicant', name: 'Co-Mietinteressent/in Aufenthaltsbewilligung (A / B / C / F)', labelKey: 'application.co.residencePermit' },

  { section: 'employmentMain', name: 'Mietinteressent/in Beruf', labelKey: 'application.empMain.occupation' },
  { section: 'employmentMain', name: 'Mietinteressent/in Arbeitgeber', labelKey: 'application.empMain.employer' },
  { section: 'employmentMain', name: 'Mietinteressent/in Dort beschäftigt seit', labelKey: 'application.empMain.employedSince', type: 'date' },
  { section: 'employmentMain', name: 'Mietinteressent/in Einkommen pro Monat', labelKey: 'application.empMain.income' },

  { section: 'employmentCo', name: 'Co-Mietinteressent/in Beruf', labelKey: 'application.empCo.occupation' },
  { section: 'employmentCo', name: 'Co-Mietinteressent/in Arbeitgeber', labelKey: 'application.empCo.employer' },
  { section: 'employmentCo', name: 'Co-Mietinteressent/in Dort beschäftigt seit', labelKey: 'application.empCo.employedSince', type: 'date' },
  { section: 'employmentCo', name: 'Co-Mietinteressent/in Einkommen pro Monat', labelKey: 'application.empCo.income' },

  { section: 'housingMain', name: 'Mietinteressent/in Heutiger Vermieter', labelKey: 'application.housingMain.currentLandlord' },
  { section: 'housingMain', name: 'Mietinteressent/in Dort wohnhaft seit', labelKey: 'application.housingMain.livingSince', type: 'date' },
  { section: 'housingMain', name: 'Mietinteressent/in Bisheriger Mietzins', labelKey: 'application.housingMain.prevRent' },
  { section: 'housingMain', name: 'Mietinteressent/in Grund für Wechsel', labelKey: 'application.housingMain.moveReason' },

  { section: 'housingCo', name: 'Co-Mietinteressent/in Heutiger Vermieter', labelKey: 'application.housingCo.currentLandlord' },
  { section: 'housingCo', name: 'Co-Mietinteressent/in Dort wohnhaft seit', labelKey: 'application.housingCo.livingSince', type: 'date' },
  { section: 'housingCo', name: 'Co-Mietinteressent/in Bisheriger Mietzins', labelKey: 'application.housingCo.prevRent' },
  { section: 'housingCo', name: 'Co-Mietinteressent/in Grund für Wechsel', labelKey: 'application.housingCo.moveReason' },

  { section: 'referencesMain', name: 'Mietinteressent/in Referenz Name Arbeitgeber', labelKey: 'application.refMain.employerName' },
  { section: 'referencesMain', name: 'Mietinteressent/in Referenz Arbeitgeber Telefon', labelKey: 'application.refMain.employerPhone', type: 'tel' },
  { section: 'referencesMain', name: 'Mietinteressent/in Referenz Arbeitgeber E-Mail', labelKey: 'application.refMain.employerEmail', type: 'email' },
  { section: 'referencesMain', name: 'Mietinteressent/in Referenz Name Vermieter', labelKey: 'application.refMain.landlordName' },
  { section: 'referencesMain', name: 'Mietinteressent/in Referenz Vermieter Telefon', labelKey: 'application.refMain.landlordPhone', type: 'tel' },

  { section: 'referencesCo', name: 'Co-Mietinteressent/in Referenz Name Arbeitgeber', labelKey: 'application.refCo.employerName' },
  { section: 'referencesCo', name: 'Co-Mietinteressent/in Referenz Arbeitgeber Telefon', labelKey: 'application.refCo.employerPhone', type: 'tel' },
  { section: 'referencesCo', name: 'Co-Mietinteressent/in Referenz Arbeitgeber E-Mail', labelKey: 'application.refCo.employerEmail', type: 'email' },
  { section: 'referencesCo', name: 'Co-Mietinteressent/in Referenz Name Vermieter', labelKey: 'application.refCo.landlordName' },
  { section: 'referencesCo', name: 'Co-Mietinteressent/in Referenz Vermieter Telefon', labelKey: 'application.refCo.landlordPhone', type: 'tel' },

  { section: 'additional', name: 'Haustiere (Art und Anzahl)', labelKey: 'application.add.pets' },
  { section: 'additional', name: 'Musikinstrumente (Art)', labelKey: 'application.add.instruments' },
  { section: 'additional', name: 'Bankverbindung lautend auf', labelKey: 'application.add.bankAccount' },
  { section: 'additional', name: 'IBAN-Nr.', labelKey: 'application.add.iban' },
  { section: 'additional', name: 'Bemerkungen', labelKey: 'application.add.remarks', type: 'textarea' },
  { section: 'additional', name: 'Gewünschte Beschriftung der Namensschilder', labelKey: 'application.add.nameplate' },
  { section: 'additional', name: 'Personenzahl Erwachsene', labelKey: 'application.add.adultCount', type: 'number' },
  { section: 'additional', name: 'Personenzahl Kinder', labelKey: 'application.add.childCount', type: 'number' },
  { section: 'additional', name: 'Geburtsjahre der Kinder', labelKey: 'application.add.childYears' },

  { section: 'documents', name: 'Kopie des Ausländerausweises / ID', labelKey: 'application.doc.idCopy', type: 'checkbox' },
  { section: 'documents', name: 'Betreibungsauszug (max. 3 Monate alt)', labelKey: 'application.doc.debtExtract', type: 'checkbox' },
  { section: 'documents', name: 'Datum', labelKey: 'application.doc.date', type: 'date' },
];

function ApplyModal({ apartment, onClose, onSuccess }) {
  const { t } = useTranslation();
  const [formValues, setFormValues] = useState({});
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [openSections, setOpenSections] = useState({});

  const toggleSection = (key) => {
    setOpenSections(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const handleChange = (name, value) => {
    setFormValues(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (files.length === 0) {
      setError(t('application.requiredFile'));
      return;
    }
    const nonPdf = files.find(f => f.type !== 'application/pdf');
    if (nonPdf) {
      setError(t('application.pdfOnly'));
      return;
    }
    setUploading(true);
    setError('');
    const formData = new FormData();
    formData.append('apartmentId', apartment.id);
    formData.append('applicantName', formValues['Mietinteressent/in Vorname'] || '' + ' ' + (formValues['Mietinteressent/in Familienname'] || ''));
    formData.append('applicantEmail', formValues['Mietinteressent/in E-Mail'] || '');
    formData.append('applicantPhone', formValues['Mietinteressent/in Mobiltelefon'] || formValues['Mietinteressent/in Telefon Privat'] || '');
    formData.append('formData', JSON.stringify(formValues));
    files.forEach(f => formData.append('files', f));
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

  const fieldsBySection = {};
  FIELDS.forEach(f => {
    if (!fieldsBySection[f.section]) fieldsBySection[f.section] = [];
    fieldsBySection[f.section].push(f);
  });

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box apply-modal-wide" onClick={e => e.stopPropagation()}>
        {success ? (
          <>
            <h3>{t('presentationList.applySuccess')}</h3>
            <p>{t('presentationList.applySuccessMsg')}</p>
          </>
        ) : (
          <>
            <h3>{t('presentationList.applyFor')} {apartment.title}</h3>
            <form className="apply-form-full" onSubmit={handleSubmit}>
              {error && <div className="login-error">{error}</div>}
              <div className="apply-sections">
                {SECTIONS.map(section => {
                  const fields = fieldsBySection[section.key] || [];
                  if (fields.length === 0) return null;
                  const isOpen = openSections[section.key] !== false;
                  return (
                    <div key={section.key} className="apply-section">
                      <div className="apply-section-header" onClick={() => toggleSection(section.key)}>
                        <span>{t(section.labelKey)}</span>
                        <span className="apply-section-arrow">{isOpen ? '▾' : '▸'}</span>
                      </div>
                      {isOpen && (
                        <div className="apply-section-body">
                          {fields.map(field => (
                            <div key={field.name} className="apply-field">
                              <label>{t(field.labelKey)}</label>
                              {field.type === 'textarea' ? (
                                <textarea value={formValues[field.name] || ''} onChange={e => handleChange(field.name, e.target.value)} rows={3} />
                              ) : field.type === 'checkbox' ? (
                                <div className="apply-checkbox-row">
                                  <input type="checkbox" checked={!!formValues[field.name]} onChange={e => handleChange(field.name, e.target.checked ? 'Ja' : '')} />
                                  <span>{t('application.yes')}</span>
                                </div>
                              ) : field.type === 'date' ? (
                                <input type="date" value={formValues[field.name] || ''} onChange={e => handleChange(field.name, e.target.value)} />
                              ) : (
                                <input type={field.type || 'text'} value={formValues[field.name] || ''} onChange={e => handleChange(field.name, e.target.value)} />
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>

              <div className="apply-file-section">
                {files.length > 0 && (
                  <ul className="apply-file-list">
                    {files.map((f, i) => <li key={i}>{f.name}</li>)}
                  </ul>
                )}
                <label className="btn-upload large">
                  {files.length > 0 ? t('presentationList.addMoreFiles') : t('presentationList.chooseFile')}
                  <input type="file" hidden accept=".pdf" onChange={e => { const pdfs = Array.from(e.target.files).filter(f => f.type === 'application/pdf'); if (pdfs.length < e.target.files.length) { setError(t('application.pdfOnly')); } else { setError(''); } setFiles(prev => [...prev, ...pdfs]); }} />
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
