import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';
import PaidBills from './PaidBills';

const API = process.env.REACT_APP_API_URL || '';

function ApartmentDetail() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, authHeader } = useAuth();
  const [apartment, setApartment] = useState(null);
  const [protocols, setProtocols] = useState([]);
  const [contacts, setContacts] = useState([]);
  const [notes, setNotes] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [selectedDocType, setSelectedDocType] = useState('HANDOVER_PROTOCOL');
  const [ticketForm, setTicketForm] = useState({ title: '', description: '' });
  const [showTicketForm, setShowTicketForm] = useState(false);
  const [showContactForm, setShowContactForm] = useState(false);
  const [contactForm, setContactForm] = useState({ name: '', value: '' });
  const [editingContact, setEditingContact] = useState(null);
  const [showNoteForm, setShowNoteForm] = useState(false);
  const [noteForm, setNoteForm] = useState('');
  const [editingNote, setEditingNote] = useState(null);

  const canDelete = user?.role === 'OWNER';
  const canUpload = user?.role === 'OWNER' || user?.role === 'ADMIN';
  const canSeeNotes = user?.role === 'OWNER' || user?.role === 'ADMIN';

  useEffect(() => {
    fetchApartment();
    fetchProtocols();
    fetchContacts();
    if (canSeeNotes) fetchNotes();
  }, [id]);

  const fetchApartment = async () => {
    const res = await fetch(`${API}/api/apartments/${id}`, { headers: authHeader() });
    if (res.ok) {
      setApartment(await res.json());
    } else if (res.status === 403 || res.status === 404) {
      navigate('/');
    }
  };

  const fetchProtocols = async () => {
    const res = await fetch(`${API}/api/apartments/${id}/protocols`, { headers: authHeader() });
    if (res.ok) {
      setProtocols(await res.json());
    }
  };

  const fetchContacts = async () => {
    const res = await fetch(`${API}/api/apartments/${id}/contacts`, { headers: authHeader() });
    if (res.ok) {
      setContacts(await res.json());
    }
  };

  const handleContactSubmit = async (e) => {
    e.preventDefault();
    if (editingContact) {
      await fetch(`${API}/api/contacts/${editingContact.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...authHeader() },
        body: JSON.stringify(contactForm)
      });
    } else {
      await fetch(`${API}/api/apartments/${id}/contacts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeader() },
        body: JSON.stringify(contactForm)
      });
    }
    setContactForm({ name: '', value: '' });
    setEditingContact(null);
    setShowContactForm(false);
    fetchContacts();
  };

  const handleEditContact = (contact) => {
    setContactForm({ name: contact.name, value: contact.value });
    setEditingContact(contact);
    setShowContactForm(true);
  };

  const handleDeleteContact = async (contactId) => {
    if (window.confirm(t('detail.deleteContactConfirm'))) {
      await fetch(`${API}/api/contacts/${contactId}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchContacts();
    }
  };

  const fetchNotes = async () => {
    const res = await fetch(`${API}/api/apartments/${id}/notes`, { headers: authHeader() });
    if (res.ok) {
      setNotes(await res.json());
    }
  };

  const handleNoteSubmit = async (e) => {
    e.preventDefault();
    if (editingNote) {
      await fetch(`${API}/api/notes/${editingNote.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', ...authHeader() },
        body: JSON.stringify({ content: noteForm })
      });
    } else {
      await fetch(`${API}/api/apartments/${id}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeader() },
        body: JSON.stringify({ content: noteForm })
      });
    }
    setNoteForm('');
    setEditingNote(null);
    setShowNoteForm(false);
    fetchNotes();
  };

  const handleDeleteNote = async (noteId) => {
    if (window.confirm(t('detail.deleteNoteConfirm'))) {
      await fetch(`${API}/api/notes/${noteId}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchNotes();
    }
  };

  const handlePhotoUpload = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    await fetch(`${API}/api/apartments/${id}/photos`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    fetchApartment();
  };

  const handleProtocolUpload = async (file) => {
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', selectedDocType);
    await fetch(`${API}/api/apartments/${id}/protocols`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    setUploading(false);
    fetchProtocols();
  };

  const handleDeleteProtocol = async (protocolId) => {
    if (window.confirm(t('detail.deleteProtocolConfirm'))) {
      await fetch(`${API}/api/apartments/protocols/${protocolId}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchProtocols();
    }
  };

  const handleTicketSubmit = async (e) => {
    e.preventDefault();
    await fetch(`${API}/api/apartments/${id}/tickets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(ticketForm)
    });
    setTicketForm({ title: '', description: '' });
    setShowTicketForm(false);
  };

  if (!apartment) return <div className="app"><p>{t('loading')}</p></div>;

  const getProtocolIcon = (contentType) => {
    if (contentType?.includes('pdf')) return 'PDF';
    if (contentType?.includes('word') || contentType?.includes('doc')) return 'DOC';
    if (contentType?.includes('image')) return 'IMG';
    return 'FILE';
  };

  const formatDocType = (docType) => {
    const types = {
      'HANDOVER_PROTOCOL': t('detail.handoverProtocol'),
      'BILLS': t('detail.bills'),
      'PHOTOS': t('detail.photos'),
      'OTHER': t('detail.other')
    };
    return types[docType] || docType;
  };

  return (
    <div className="app">
      <header>
        <button className="btn-back" onClick={() => navigate('/')}>{t('detail.back')}</button>
        <h1>{apartment.title}</h1>
        {canDelete && (
          <a href={`/presentations/apartments/${apartment.id}`} className="btn-back" target="_blank" rel="noreferrer">{t('apartmentList.presentation')}</a>
        )}
      </header>

      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-photos">
            {apartment.photoPaths && apartment.photoPaths.length > 0 ? (
              <img src={`${API}/api/apartments/photos/${apartment.photoPaths[0]}`} alt="Photo" />
            ) : (
              <img src="/placeholder.svg" alt="No photos" className="placeholder-img" />
            )}
            {canUpload && (
              <label className="btn-upload large">
                {t('detail.addPhoto')}
                <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(e.target.files[0])} />
              </label>
            )}
          </div>

          <div className="detail-info">
            <div className="info-row">
              <span className="label">{t('detail.location')}</span>
              <span>{apartment.location}</span>
            </div>
            <div className="info-row">
              <span className="label">{t('detail.rooms')}</span>
              <span>{apartment.rooms}</span>
            </div>
            <div className="info-row">
              <span className="label">{t('detail.area')}</span>
              <span>{apartment.area} {t('detail.sqm')}</span>
            </div>
            {apartment.tenant && (
              <div className="info-row">
                <span className="label">{t('detail.tenant')}</span>
                <span>{apartment.tenant}</span>
              </div>
            )}
          </div>

          <PaidBills apartmentId={id} />
        </div>

        <div className="detail-protocols">
          <div className="protocols-header">
            <h2>{t('detail.documents')}</h2>
            {canUpload && (
              <div className="upload-controls">
                <select
                  value={selectedDocType}
                  onChange={(e) => setSelectedDocType(e.target.value)}
                  className="doc-type-select"
                >
                  <option value="HANDOVER_PROTOCOL">{t('detail.handoverProtocol')}</option>
                  <option value="BILLS">{t('detail.bills')}</option>
                  <option value="PHOTOS">{t('detail.photos')}</option>
                  <option value="OTHER">{t('detail.other')}</option>
                </select>
                <label className="btn-upload">
                  {uploading ? t('detail.uploading') : t('detail.upload')}
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx,.jpeg,.jpg,.png"
                    hidden
                    onChange={(e) => handleProtocolUpload(e.target.files[0])}
                    disabled={uploading}
                  />
                </label>
              </div>
            )}
          </div>

          {protocols.length === 0 ? (
            <p className="empty-protocols">{t('detail.noDocuments')}</p>
          ) : (
            <div className="protocol-list">
              {protocols.map(proto => (
                <div key={proto.id} className="protocol-item">
                  <a
                    href={`${API}/api/apartments/protocols/${proto.fileName}`}
                    target="_blank"
                    rel="noreferrer"
                    className="protocol-link"
                  >
                    <span className={`protocol-icon ${getProtocolIcon(proto.contentType).toLowerCase()}`}>
                      {getProtocolIcon(proto.contentType)}
                    </span>
                    <div className="protocol-info">
                      <span className="protocol-name">{proto.originalName}</span>
                      <div className="protocol-meta">
                        <span className="protocol-type">{formatDocType(proto.documentType)}</span>
                        <span className="protocol-date">
                          {new Date(proto.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  </a>
                  {canDelete && (
                    <button className="btn-delete-small" onClick={() => handleDeleteProtocol(proto.id)}>×</button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="detail-protocols">
          <div className="protocols-header">
            <h2>{t('detail.contacts')}</h2>
            {canDelete && (
              <button className="btn-upload" onClick={() => { setShowContactForm(!showContactForm); setEditingContact(null); setContactForm({ name: '', value: '' }); }}>
                {showContactForm ? t('cancel') : t('detail.add')}
              </button>
            )}
          </div>

          {showContactForm && (
            <form className="contact-form" onSubmit={handleContactSubmit}>
              <input
                placeholder={t('detail.contactNamePlaceholder')}
                value={contactForm.name}
                onChange={(e) => setContactForm({ ...contactForm, name: e.target.value })}
                required
              />
              <input
                placeholder={t('detail.contactValuePlaceholder')}
                value={contactForm.value}
                onChange={(e) => setContactForm({ ...contactForm, value: e.target.value })}
                required
              />
              <button type="submit" className="btn-primary">{editingContact ? t('update') : t('save')}</button>
            </form>
          )}

          {contacts.length === 0 ? (
            <p className="empty-protocols">{t('detail.noContacts')}</p>
          ) : (
            <div className="contact-list">
              {contacts.map(contact => (
                <div key={contact.id} className="contact-item">
                  <div className="contact-info">
                    <span className="contact-name">{contact.name}</span>
                    <span className="contact-value">{contact.value}</span>
                  </div>
                  {canDelete && (
                    <div className="contact-actions">
                      <button className="btn-sm" onClick={() => handleEditContact(contact)}>{t('edit')}</button>
                      <button className="btn-delete-small" onClick={() => handleDeleteContact(contact.id)}>×</button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {canSeeNotes && (
          <div className="detail-protocols">
            <div className="protocols-header">
              <h2>{t('detail.notes')}</h2>
              <button className="btn-upload" onClick={() => { setShowNoteForm(!showNoteForm); setEditingNote(null); setNoteForm(''); }}>
                {showNoteForm ? t('cancel') : t('detail.add')}
              </button>
            </div>

            {showNoteForm && (
              <form className="contact-form" onSubmit={handleNoteSubmit}>
                <textarea
                  placeholder={t('detail.writeNote')}
                  value={noteForm}
                  onChange={(e) => setNoteForm(e.target.value)}
                  rows={3}
                  required
                  style={{ resize: 'vertical', padding: '8px 12px', border: '1px solid #ddd', borderRadius: '6px', fontSize: '13px', fontFamily: 'inherit' }}
                />
                <button type="submit" className="btn-primary">{editingNote ? t('update') : t('save')}</button>
              </form>
            )}

            {notes.length === 0 ? (
              <p className="empty-protocols">{t('detail.noNotes')}</p>
            ) : (
              <div className="note-list">
                {notes.map(note => (
                  <div key={note.id} className="note-item">
                    <div className="note-content">{note.content}</div>
                    <div className="note-footer">
                      <span className="note-date">{new Date(note.createdAt).toLocaleString()}</span>
                      <div className="contact-actions">
                        <button className="btn-sm" onClick={() => { setNoteForm(note.content); setEditingNote(note); setShowNoteForm(true); }}>{t('edit')}</button>
                        <button className="btn-delete-small" onClick={() => handleDeleteNote(note.id)}>×</button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div className="detail-tickets">
          <div className="protocols-header">
            <h2>{t('detail.tickets')}</h2>
            <div className="upload-controls">
              <button className="btn-primary" onClick={() => setShowTicketForm(!showTicketForm)}>
                {showTicketForm ? t('cancel') : t('detail.newTicket')}
              </button>
              <a href={`/tickets?apartmentId=${id}`} className="btn-back">{t('detail.viewAll')}</a>
            </div>
          </div>

          {showTicketForm && (
            <form className="apartment-form" onSubmit={handleTicketSubmit}>
              <input
                name="title"
                placeholder={t('detail.ticketTitle')}
                value={ticketForm.title}
                onChange={(e) => setTicketForm({ ...ticketForm, title: e.target.value })}
                required
              />
              <textarea
                name="description"
                placeholder={t('detail.describeIssue')}
                value={ticketForm.description}
                onChange={(e) => setTicketForm({ ...ticketForm, description: e.target.value })}
              />
              <button type="submit" className="btn-primary">{t('detail.submitTicket')}</button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

export default ApartmentDetail;
