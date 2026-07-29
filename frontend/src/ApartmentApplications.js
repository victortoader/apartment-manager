import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

const FORM_LABELS = {
  'Liegenschaft/Ort': 'application.property.location',
  'abaRef.-Nr.': 'application.property.abaRef',
  'Wohnung': 'application.property.apartment',
  'MZ Wohnung inkl. NK': 'application.property.rent',
  'Anz. Einstellplätze': 'application.property.indoorParking',
  'Anz. Aussen-Parkplätze': 'application.property.outdoorParking',
  'Gewünschter Mietbeginn': 'application.property.desiredStart',
  'Kontrollschild-Nr.': 'application.property.licensePlate',
  'Ex-Mieter': 'application.property.prevTenant',
  'Nutzung als (Familienwohnung / Neben-/Zweitwohnung / Wohnung von Unverheirateten / Wohngemeinschaft)': 'application.property.usage',

  'Mietinteressent/in Anrede (Herr / Frau)': 'application.main.salutation',
  'Mietinteressent/in Rolle (Mietinteressent/in / Ehepartner/in / Partner/in / Solidarhafter/in)': 'application.main.role',
  'Mietinteressent/in Familienname': 'application.main.lastName',
  'Mietinteressent/in Vorname': 'application.main.firstName',
  'Mietinteressent/in Strasse / Nr.': 'application.main.street',
  'Mietinteressent/in PLZ/Ort': 'application.main.city',
  'Mietinteressent/in Telefon Privat': 'application.main.phonePrivate',
  'Mietinteressent/in Telefon Geschäft': 'application.main.phoneBusiness',
  'Mietinteressent/in Mobiltelefon': 'application.main.phoneMobile',
  'Mietinteressent/in E-Mail': 'application.main.email',
  'Mietinteressent/in Geburtsdatum': 'application.main.birthDate',
  'Mietinteressent/in Zivilstand': 'application.main.maritalStatus',
  'Mietinteressent/in Heimatort/Land': 'application.main.hometown',
  'Mietinteressent/in Aufenthaltsbewilligung (A / B / C / F)': 'application.main.residencePermit',

  'Co-Mietinteressent/in Anrede (Herr / Frau)': 'application.co.salutation',
  'Co-Mietinteressent/in Rolle (Ehepartner/in / Partner/in / Solidarhafter/in)': 'application.co.role',
  'Co-Mietinteressent/in Familienname': 'application.co.lastName',
  'Co-Mietinteressent/in Vorname': 'application.co.firstName',
  'Co-Mietinteressent/in Strasse / Nr.': 'application.co.street',
  'Co-Mietinteressent/in PLZ/Ort': 'application.co.city',
  'Co-Mietinteressent/in Telefon Privat': 'application.co.phonePrivate',
  'Co-Mietinteressent/in Telefon Geschäft': 'application.co.phoneBusiness',
  'Co-Mietinteressent/in Mobiltelefon': 'application.co.phoneMobile',
  'Co-Mietinteressent/in E-Mail': 'application.co.email',
  'Co-Mietinteressent/in Geburtsdatum': 'application.co.birthDate',
  'Co-Mietinteressent/in Zivilstand': 'application.co.maritalStatus',
  'Co-Mietinteressent/in Heimatort/Land': 'application.co.hometown',
  'Co-Mietinteressent/in Aufenthaltsbewilligung (A / B / C / F)': 'application.co.residencePermit',

  'Mietinteressent/in Beruf': 'application.empMain.occupation',
  'Mietinteressent/in Arbeitgeber': 'application.empMain.employer',
  'Mietinteressent/in Dort beschäftigt seit': 'application.empMain.employedSince',
  'Mietinteressent/in Einkommen pro Monat': 'application.empMain.income',

  'Co-Mietinteressent/in Beruf': 'application.empCo.occupation',
  'Co-Mietinteressent/in Arbeitgeber': 'application.empCo.employer',
  'Co-Mietinteressent/in Dort beschäftigt seit': 'application.empCo.employedSince',
  'Co-Mietinteressent/in Einkommen pro Monat': 'application.empCo.income',

  'Mietinteressent/in Heutiger Vermieter': 'application.housingMain.currentLandlord',
  'Mietinteressent/in Dort wohnhaft seit': 'application.housingMain.livingSince',
  'Mietinteressent/in Bisheriger Mietzins': 'application.housingMain.prevRent',
  'Mietinteressent/in Grund für Wechsel': 'application.housingMain.moveReason',

  'Co-Mietinteressent/in Heutiger Vermieter': 'application.housingCo.currentLandlord',
  'Co-Mietinteressent/in Dort wohnhaft seit': 'application.housingCo.livingSince',
  'Co-Mietinteressent/in Bisheriger Mietzins': 'application.housingCo.prevRent',
  'Co-Mietinteressent/in Grund für Wechsel': 'application.housingCo.moveReason',

  'Mietinteressent/in Referenz Name Arbeitgeber': 'application.refMain.employerName',
  'Mietinteressent/in Referenz Arbeitgeber Telefon': 'application.refMain.employerPhone',
  'Mietinteressent/in Referenz Arbeitgeber E-Mail': 'application.refMain.employerEmail',
  'Mietinteressent/in Referenz Name Vermieter': 'application.refMain.landlordName',
  'Mietinteressent/in Referenz Vermieter Telefon': 'application.refMain.landlordPhone',

  'Co-Mietinteressent/in Referenz Name Arbeitgeber': 'application.refCo.employerName',
  'Co-Mietinteressent/in Referenz Arbeitgeber Telefon': 'application.refCo.employerPhone',
  'Co-Mietinteressent/in Referenz Arbeitgeber E-Mail': 'application.refCo.employerEmail',
  'Co-Mietinteressent/in Referenz Name Vermieter': 'application.refCo.landlordName',
  'Co-Mietinteressent/in Referenz Vermieter Telefon': 'application.refCo.landlordPhone',

  'Haustiere (Art und Anzahl)': 'application.add.pets',
  'Musikinstrumente (Art)': 'application.add.instruments',
  'Bankverbindung lautend auf': 'application.add.bankAccount',
  'IBAN-Nr.': 'application.add.iban',
  'Bemerkungen': 'application.add.remarks',
  'Gewünschte Beschriftung der Namensschilder': 'application.add.nameplate',
  'Personenzahl Erwachsene': 'application.add.adultCount',
  'Personenzahl Kinder': 'application.add.childCount',
  'Geburtsjahre der Kinder': 'application.add.childYears',

  'Kopie des Ausländerausweises / ID': 'application.doc.idCopy',
  'Betreibungsauszug (max. 3 Monate alt)': 'application.doc.debtExtract',
  'Datum': 'application.doc.date',
  'Unterschrift Mietinteressent/in': 'application.doc.signatureMain',
  'Unterschrift Co-Mietinteressent/in': 'application.doc.signatureCo',
};

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

const FIELD_NAMES_BY_SECTION = {};
Object.keys(FORM_LABELS).forEach(key => {
  const section = (() => {
    if (key.startsWith('Co-Mietinteressent/in Referenz')) return 'referencesCo';
    if (key.startsWith('Mietinteressent/in Referenz')) return 'referencesMain';
    if (key.startsWith('Co-Mietinteressent/in Einkommen') || key.startsWith('Co-Mietinteressent/in Beruf') || key.startsWith('Co-Mietinteressent/in Arbeitgeber') || key.startsWith('Co-Mietinteressent/in Dort beschäftigt')) return 'employmentCo';
    if (key.startsWith('Mietinteressent/in Einkommen') || key.startsWith('Mietinteressent/in Beruf') || key.startsWith('Mietinteressent/in Arbeitgeber') || key.startsWith('Mietinteressent/in Dort beschäftigt')) return 'employmentMain';
    if (key.startsWith('Co-Mietinteressent/in Heutiger') || key.startsWith('Co-Mietinteressent/in Dort wohnhaft') || key.startsWith('Co-Mietinteressent/in Bisheriger') || key.startsWith('Co-Mietinteressent/in Grund')) return 'housingCo';
    if (key.startsWith('Mietinteressent/in Heutiger') || key.startsWith('Mietinteressent/in Dort wohnhaft') || key.startsWith('Mietinteressent/in Bisheriger') || key.startsWith('Mietinteressent/in Grund')) return 'housingMain';
    if (key.startsWith('Co-Mietinteressent/in')) return 'coApplicant';
    if (key.startsWith('Mietinteressent/in')) return 'mainApplicant';
    if (key.startsWith('Kopie') || key.startsWith('Betreibungsauszug') || key.startsWith('Datum') || key.startsWith('Unterschrift')) return 'documents';
    if (key.startsWith('Haustiere') || key.startsWith('Musikinstrumente') || key.startsWith('Bankverbindung') || key.startsWith('IBAN') || key.startsWith('Bemerkungen') || key.startsWith('Gewünschte') || key.startsWith('Personenzahl') || key.startsWith('Geburtsjahre')) return 'additional';
    return 'property';
  })();
  if (!FIELD_NAMES_BY_SECTION[section]) FIELD_NAMES_BY_SECTION[section] = [];
  FIELD_NAMES_BY_SECTION[section].push(key);
});

function ApartmentApplications() {
  const { t } = useTranslation();
  const { id } = useParams();
  const { authHeader } = useAuth();
  const [applications, setApplications] = useState([]);
  const [apartment, setApartment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [expandedApp, setExpandedApp] = useState(null);

  const downloadFile = async (fileName, originalName) => {
    const res = await fetch(`${API}/api/applications/files/${fileName}`, { headers: authHeader() });
    if (!res.ok) return;
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = originalName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

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

  const parseFormData = (app) => {
    if (!app.formData) return null;
    try { return JSON.parse(app.formData); } catch { return null; }
  };

  if (loading) return <div className="app"><p>{t('loading')}</p></div>;

  return (
    <div className="app">
      <header>
        <Link to="/" className="btn-back">{t('detail.back')}</Link>
        <h1>{apartment ? apartment.title : t('loading')} — {t('detail.applications')}</h1>
      </header>

      <div className="application-page-list">
        {applications.length === 0 ? (
          <p className="empty-protocols">{t('apartmentList.noApplications')}</p>
        ) : (
          applications.map(app => {
            const formData = parseFormData(app);
            const isExpanded = expandedApp === app.id;
            return (
              <div key={app.id} className="application-page-card">
                <div className="application-page-header" onClick={() => setExpandedApp(isExpanded ? null : app.id)}>
                  <div className="application-page-summary">
                    <span className="application-page-name">{app.applicantName}</span>
                    <span className="application-page-date">{new Date(app.submittedAt).toLocaleDateString()}</span>
                  </div>
                  <div className="application-page-actions">
                    {app.storedFileName.split(', ').map((fn, i) => (
                      <button key={i} className="btn-sm" onClick={e => { e.stopPropagation(); downloadFile(fn, (app.originalFileName.split(', ')[i]) || fn); }}>
                        {(app.originalFileName.split(', ')[i]) || fn}
                      </button>
                    ))}
                    <span className="apply-section-arrow">{isExpanded ? '▾' : '▸'}</span>
                  </div>
                </div>
                {isExpanded && formData && (
                  <div className="application-page-body">
                    {SECTIONS.map(section => {
                      const fieldNames = FIELD_NAMES_BY_SECTION[section.key] || [];
                      const hasData = fieldNames.some(n => formData[n] && formData[n].trim());
                      if (!hasData) return null;
                      return (
                        <div key={section.key} className="application-page-section">
                          <h4>{t(section.labelKey)}</h4>
                          <table className="application-page-table">
                            <tbody>
                              {fieldNames.map(name => {
                                const val = formData[name];
                                if (!val || !val.trim()) return null;
                                return (
                                  <tr key={name}>
                                    <td className="app-table-label">{t(FORM_LABELS[name] || name)}</td>
                                    <td className="app-table-value">{val}</td>
                                  </tr>
                                );
                              })}
                            </tbody>
                          </table>
                        </div>
                      );
                    })}
                  </div>
                )}
                {isExpanded && !formData && (
                  <div className="application-page-body">
                    <p className="empty-protocols">{t('application.noFormData')}</p>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

export default ApartmentApplications;
