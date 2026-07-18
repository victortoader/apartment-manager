import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function Tickets() {
  const { user, authHeader, fetchUnreadCount } = useAuth();
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ title: '', description: '' });
  const [selectedFiles, setSelectedFiles] = useState([]);
  const fileInputRef = useRef(null);
  const isAdminOrOwner = user?.role === 'OWNER' || user?.role === 'ADMIN';
  const isTenant = user?.role === 'TENANT';

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    setLoading(true);
    const url = isAdminOrOwner
      ? `${API}/api/tickets`
      : `${API}/api/apartments/${user.apartmentId}/tickets`;

    const res = await fetch(url, { headers: authHeader() });
    if (res.ok) {
      const data = await res.json();
      setTickets(data);
    }
    setLoading(false);
    if (isAdminOrOwner) fetchUnreadCount();
  };

  const handleCreateTicket = async (e) => {
    e.preventDefault();

    const res = await fetch(`${API}/api/apartments/${user.apartmentId}/tickets`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(form)
    });

    if (res.ok) {
      const ticket = await res.json();

      for (const file of selectedFiles) {
        const formData = new FormData();
        formData.append('file', file);
        await fetch(`${API}/api/tickets/${ticket.id}/photos`, {
          method: 'POST',
          headers: authHeader(),
          body: formData
        });
      }
    }

    setForm({ title: '', description: '' });
    setSelectedFiles([]);
    setShowForm(false);
    fetchTickets();
  };

  const handlePhotoUpload = async (ticketId, file) => {
    if (!file) return;
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API}/api/tickets/${ticketId}/photos`, {
      method: 'POST',
      headers: authHeader(),
      body: formData
    });
    if (res.ok) {
      fetchTickets();
    }
  };

  const handleStatusChange = async (ticketId, newStatus) => {
    await fetch(`${API}/api/tickets/${ticketId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify({ status: newStatus })
    });
    fetchTickets();
  };

  const canAddPhoto = (ticket) => {
    if (isAdminOrOwner) return true;
    if (isTenant && ticket.createdBy && ticket.createdBy.id === user.id) return true;
    return false;
  };

  const statusColor = (status) => {
    switch (status) {
      case 'NEW': return '#3b82f6';
      case 'IN_PROGRESS': return '#f59e0b';
      case 'DONE': return '#22c55e';
      case 'REJECTED': return '#ef4444';
      default: return '#6b7280';
    }
  };

  const statusLabel = (status) => {
    switch (status) {
      case 'NEW': return 'New';
      case 'IN_PROGRESS': return 'In Progress';
      case 'DONE': return 'Done';
      case 'REJECTED': return 'Rejected';
      default: return status;
    }
  };

  if (loading) {
    return (
      <div className="app">
        <header>
          <h1>Tickets</h1>
          <div className="header-right">
            <a href="/" className="btn-back">Back</a>
          </div>
        </header>
        <p className="empty">Loading...</p>
      </div>
    );
  }

  return (
    <div className="app">
      <header>
        <h1>Tickets</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          {isTenant && user.apartmentId && (
            <button className="btn-primary" onClick={() => setShowForm(!showForm)}>
              {showForm ? 'Cancel' : '+ New Ticket'}
            </button>
          )}
          <a href="/" className="btn-back">Back</a>
        </div>
      </header>

      {showForm && isTenant && (
        <form className="apartment-form" onSubmit={handleCreateTicket}>
          <input
            name="title"
            placeholder="Ticket title"
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            required
          />
          <textarea
            name="description"
            placeholder="Describe the issue..."
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
          />
          <div className="ticket-photo-upload">
            <label className="btn-upload large">
              {selectedFiles.length > 0 ? `${selectedFiles.length} photo(s) selected (max 5)` : '+ Add Photos'}
              <input
                type="file"
                accept="image/*"
                multiple
                hidden
                ref={fileInputRef}
                onChange={(e) => {
                  const remaining = 5 - selectedFiles.length;
                  const newFiles = [...e.target.files].slice(0, remaining);
                  setSelectedFiles([...selectedFiles, ...newFiles]);
                }}
              />
            </label>
            {selectedFiles.length > 0 && (
              <div className="selected-previews">
                {selectedFiles.map((f, i) => (
                  <div key={i} className="selected-preview-item">
                    <img src={URL.createObjectURL(f)} alt={f.name} className="selected-preview-thumb" />
                    <button type="button" className="btn-remove-photo" onClick={() => {
                      setSelectedFiles(selectedFiles.filter((_, idx) => idx !== i));
                    }}>×</button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <button type="submit" className="btn-primary">Submit Ticket</button>
        </form>
      )}

      {tickets.length === 0 ? (
        <p className="empty">No tickets found.</p>
      ) : (
        <div className="ticket-list">
          {tickets.map(ticket => (
            <div key={ticket.id} className="ticket-card">
              <div className="ticket-header">
                <h3>{isAdminOrOwner && ticket.apartment ? `[${ticket.apartment.title}] ${ticket.title}` : ticket.title}</h3>
                <span
                  className="ticket-status"
                  style={{ backgroundColor: statusColor(ticket.status) }}
                >
                  {statusLabel(ticket.status)}
                </span>
              </div>
              {ticket.description && (
                <p className="ticket-description">{ticket.description}</p>
              )}
              {ticket.photoPaths && ticket.photoPaths.length > 0 && (
                <div className="ticket-photos">
                  {ticket.photoPaths.map((photo, i) => (
                    <a key={i} href={`${API}/api/tickets/photos/${photo}`} target="_blank" rel="noreferrer">
                      <img src={`${API}/api/tickets/photos/${photo}`} alt={`Photo ${i + 1}`} className="ticket-photo-thumb" />
                    </a>
                  ))}
                </div>
              )}
              <div className="ticket-meta">
                {ticket.apartment && (
                  <span className="ticket-apartment">
                    Apt: {ticket.apartment.title}
                  </span>
                )}
                {ticket.createdBy && (
                  <span className="ticket-author">
                    By: {ticket.createdBy.username}
                  </span>
                )}
                <span className="ticket-date">
                  {new Date(ticket.createdAt).toLocaleDateString()}
                </span>
              </div>
              <div className="ticket-actions-row">
                {canAddPhoto(ticket) && (!ticket.photoPaths || ticket.photoPaths.length < 5) && (
                  <label className="btn-upload btn-sm">
                    + Photo
                    <input
                      type="file"
                      accept="image/*"
                      hidden
                      onChange={(e) => handlePhotoUpload(ticket.id, e.target.files[0])}
                    />
                  </label>
                )}
                {isAdminOrOwner && (
                  <div className="ticket-actions">
                    {ticket.status !== 'NEW' && (
                      <button
                        className="btn-sm"
                        onClick={() => handleStatusChange(ticket.id, 'NEW')}
                      >
                        New
                      </button>
                    )}
                    {ticket.status !== 'IN_PROGRESS' && (
                      <button
                        className="btn-sm btn-warning"
                        onClick={() => handleStatusChange(ticket.id, 'IN_PROGRESS')}
                      >
                        In Progress
                      </button>
                    )}
                    {ticket.status !== 'DONE' && (
                      <button
                        className="btn-sm btn-success"
                        onClick={() => handleStatusChange(ticket.id, 'DONE')}
                      >
                        Done
                      </button>
                    )}
                    {ticket.status !== 'REJECTED' && (
                      <button
                        className="btn-sm btn-danger"
                        onClick={() => handleStatusChange(ticket.id, 'REJECTED')}
                      >
                        Reject
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default Tickets;
