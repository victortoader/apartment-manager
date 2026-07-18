import React, { useState, useEffect } from 'react';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function ApartmentList() {
  const { user, logout, authHeader, unreadTickets, fetchUnreadCount } = useAuth();
  const [apartments, setApartments] = useState([]);
  const [summaries, setSummaries] = useState([]);
  const [form, setForm] = useState({
    title: '', location: '', rooms: '', area: ''
  });
  const [showForm, setShowForm] = useState(false);
  const [unreadList, setUnreadList] = useState([]);
  const [manageMode, setManageMode] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [passwordInput, setPasswordInput] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [verifying, setVerifying] = useState(false);

  const isOwner = user?.role === 'OWNER';
  const isAdmin = user?.role === 'ADMIN';
  const canCreate = isOwner || isAdmin;
  const canDelete = isOwner;

  useEffect(() => {
    fetchApartments();
    fetchSummaries();
    fetchUnreadCount();
    if (canCreate) fetchUnreadTickets();
  }, []);

  const fetchApartments = async () => {
    const res = await fetch(`${API}/api/apartments`, { headers: authHeader() });
    const data = await res.json();
    setApartments(data);
  };

  const fetchSummaries = async () => {
    const res = await fetch(`${API}/api/apartments/summary`, { headers: authHeader() });
    if (res.ok) {
      const data = await res.json();
      setSummaries(data);
    }
  };

  const fetchUnreadTickets = async () => {
    const res = await fetch(`${API}/api/tickets/unread`, { headers: authHeader() });
    if (res.ok) {
      const data = await res.json();
      setUnreadList(data);
    }
  };

  const handleVerifyPassword = async () => {
    setVerifying(true);
    setPasswordError('');
    try {
      const res = await fetch(`${API}/api/auth/verify-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeader() },
        body: JSON.stringify({ password: passwordInput })
      });
      if (res.ok) {
        setManageMode(true);
        setShowPasswordModal(false);
        setPasswordInput('');
      } else {
        setPasswordError('Incorrect password');
      }
    } catch (e) {
      setPasswordError('Verification failed');
    }
    setVerifying(false);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const apartment = {
      ...form,
      rooms: parseInt(form.rooms),
      area: parseFloat(form.area)
    };

    const res = await fetch(`${API}/api/apartments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(apartment)
    });

    if (res.ok) {
      setForm({ title: '', location: '', rooms: '', area: '' });
      setShowForm(false);
      fetchApartments();
      fetchSummaries();
    }
  };

  const handlePhotoUpload = async (apartmentId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    await fetch(`${API}/api/apartments/${apartmentId}/photos`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    fetchApartments();
    fetchSummaries();
  };

  const handleDelete = async (id) => {
    if (window.confirm('Delete this apartment?')) {
      await fetch(`${API}/api/apartments/${id}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchApartments();
      fetchSummaries();
    }
  };

  if (user?.role === 'TENANT' && apartments.length === 0) {
    return (
      <div className="app">
        <header>
          <h1>Apartment Manager</h1>
          <div className="header-right">
            <span className="header-user">{user.username} ({user.role})</span>
            <button className="btn-back" onClick={logout}>Logout</button>
          </div>
        </header>
        <div className="no-apartment-msg">
          <p>No apartment assigned to your account. Please contact the administrator.</p>
        </div>
      </div>
    );
  }

  const summaryMap = {};
  summaries.forEach(s => { summaryMap[s.id] = s; });

  return (
    <div className="app">
      <header>
        <h1>Apartment Manager</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          {canCreate && (
            <button className="btn-primary" onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Cancel' : '+ Add Apartment'}
            </button>
          )}
          {canDelete && (
            manageMode
              ? <button className="btn-manage active" onClick={() => setManageMode(false)}>Done</button>
              : <button className="btn-manage" onClick={() => setShowPasswordModal(true)}>Manage</button>
          )}
          {isOwner && (
            <a href="/users" className="btn-primary">Users</a>
          )}
          <a href="/tickets" className="btn-primary tickets-btn">
            Tickets
            {unreadTickets > 0 && <span className="unread-badge">{unreadTickets}</span>}
          </a>
          <button className="btn-back" onClick={logout}>Logout</button>
        </div>
      </header>

      {isAdmin && unreadList.length > 0 && (
        <div className="unread-tickets-section">
          <h2>New Tickets</h2>
          <div className="unread-tickets-list">
            {unreadList.map(ticket => (
              <div key={ticket.id} className="unread-ticket-item">
                <div className="unread-ticket-info">
                  <span className="unread-ticket-title">
                    {ticket.apartment ? `[${ticket.apartment.title}] ` : ''}{ticket.title}
                  </span>
                  <span className="unread-ticket-desc">{ticket.description}</span>
                </div>
                <span className="unread-ticket-date">
                  {new Date(ticket.createdAt).toLocaleDateString()}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {showForm && (
        <form className="apartment-form" onSubmit={handleSubmit}>
          <input name="title" placeholder="Title" value={form.title} onChange={handleChange} required />
          <input name="location" placeholder="Location" value={form.location} onChange={handleChange} />
          <input name="rooms" type="number" placeholder="Rooms" value={form.rooms} onChange={handleChange} />
          <input name="area" type="number" step="0.1" placeholder="Area (sqm)" value={form.area} onChange={handleChange} />
          <button type="submit" className="btn-primary">Save</button>
        </form>
      )}

      {showPasswordModal && (
        <div className="modal-overlay" onClick={() => { setShowPasswordModal(false); setPasswordInput(''); setPasswordError(''); }}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <h3>Confirm Password</h3>
            <p>Enter your password to enable management mode.</p>
            <input
              type="password"
              placeholder="Password"
              value={passwordInput}
              onChange={e => setPasswordInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleVerifyPassword()}
              autoFocus
            />
            {passwordError && <p className="modal-error">{passwordError}</p>}
            <div className="modal-actions">
              <button className="btn-cancel small" onClick={() => { setShowPasswordModal(false); setPasswordInput(''); setPasswordError(''); }}>Cancel</button>
              <button className="btn-primary small" onClick={handleVerifyPassword} disabled={verifying}>
                {verifying ? 'Verifying...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="apartment-stack">
        {apartments.length === 0 && <p className="empty">No apartments yet. Add one!</p>}
        {apartments.map(apt => {
          const s = summaryMap[apt.id] || {};
          const bills = s.recentBills || [];
          const openTickets = s.openTickets || 0;

          return (
            <a key={apt.id} href={`/apartments/${apt.id}`} className="apartment-row">
              <div className="row-photo">
                <img
                  src={apt.photoPaths && apt.photoPaths.length > 0
                    ? `${API}/api/apartments/photos/${apt.photoPaths[0]}`
                    : '/placeholder.svg'}
                  alt="apartment"
                />
              </div>

              <div className="row-info">
                <div className="row-main">
                  <h3>{apt.title}</h3>
                  <p className="row-location">{apt.location}</p>
                  <p className="row-price">{apt.rooms} rooms &middot; {apt.area} m&sup2;</p>
                  {apt.tenant && <p className="row-tenant">Tenant: {apt.tenant}</p>}
                </div>

                <div className="row-bills">
                  <h4>Bills</h4>
                  {bills.length > 0 ? (
                    bills.map(bill => (
                      <div key={bill.id} className="row-bill-item">
                        <span className="row-bill-type">{bill.billType || 'Other'}</span>
                        <span className="row-bill-name">{bill.originalFileName}</span>
                        <span className="row-bill-date">{new Date(bill.uploadDate).toLocaleDateString()}</span>
                      </div>
                    ))
                  ) : (
                    <p className="row-bills-empty">No bills uploaded</p>
                  )}
                </div>

                {openTickets > 0 && (
                  <div className="row-tickets-inline">
                    <h4>Open Tickets</h4>
                    <span className="row-ticket-badge">{openTickets} open</span>
                  </div>
                )}
              </div>

              <div className="row-actions">
                <button className="btn-presentation" onClick={(e) => { e.stopPropagation(); e.preventDefault(); window.open(`/presentations/apartments/${apt.id}`, '_blank'); }}>Presentation</button>
                {manageMode && canCreate && (
                  <label className="btn-upload small">
                    + Photo
                    <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(apt.id, e.target.files[0])} />
                  </label>
                )}
                {manageMode && canDelete && (
                  <button className="btn-delete small" onClick={() => handleDelete(apt.id)}>Delete</button>
                )}
              </div>
            </a>
          );
        })}
      </div>
    </div>
  );
}

export default ApartmentList;
