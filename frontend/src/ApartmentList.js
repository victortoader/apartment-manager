import React, { useState, useEffect } from 'react';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function ApartmentList() {
  const { user, logout, authHeader } = useAuth();
  const [apartments, setApartments] = useState([]);
  const [form, setForm] = useState({
    title: '', description: '', location: '', price: '', rooms: '', area: ''
  });
  const [showForm, setShowForm] = useState(false);

  const canCreate = user?.role === 'OWNER' || user?.role === 'ADMIN';
  const canDelete = user?.role === 'OWNER';

  useEffect(() => {
    fetchApartments();
  }, []);

  const fetchApartments = async () => {
    const res = await fetch(`${API}/api/apartments`, { headers: authHeader() });
    const data = await res.json();
    setApartments(data);
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const apartment = {
      ...form,
      price: parseFloat(form.price),
      rooms: parseInt(form.rooms),
      area: parseFloat(form.area)
    };

    const res = await fetch(`${API}/api/apartments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(apartment)
    });

    if (res.ok) {
      setForm({ title: '', description: '', location: '', price: '', rooms: '', area: '' });
      setShowForm(false);
      fetchApartments();
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
  };

  const handleDelete = async (id) => {
    if (window.confirm('Delete this apartment?')) {
      await fetch(`${API}/api/apartments/${id}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchApartments();
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
          {user?.role === 'OWNER' && (
            <a href="/users" className="btn-primary">Users</a>
          )}
          <button className="btn-back" onClick={logout}>Logout</button>
        </div>
      </header>

      {showForm && (
        <form className="apartment-form" onSubmit={handleSubmit}>
          <input name="title" placeholder="Title" value={form.title} onChange={handleChange} required />
          <textarea name="description" placeholder="Description" value={form.description} onChange={handleChange} />
          <input name="location" placeholder="Location" value={form.location} onChange={handleChange} />
          <input name="price" type="number" step="0.01" placeholder="Price (RON/month)" value={form.price} onChange={handleChange} />
          <input name="rooms" type="number" placeholder="Rooms" value={form.rooms} onChange={handleChange} />
          <input name="area" type="number" step="0.1" placeholder="Area (sqm)" value={form.area} onChange={handleChange} />
          <button type="submit" className="btn-primary">Save</button>
        </form>
      )}

      <div className="apartment-grid">
        {apartments.length === 0 && <p className="empty">No apartments yet. Add one!</p>}
        {apartments.map(apt => (
          <a key={apt.id} href={`/apartments/${apt.id}`} className="apartment-card">
            <div className="photos">
              <img
                src={apt.photoPaths && apt.photoPaths.length > 0
                  ? `${API}/api/apartments/photos/${apt.photoPaths[0]}`
                  : '/placeholder.svg'}
                alt="apartment"
              />
            </div>
            <div className="card-body">
              <h3>{apt.title}</h3>
              <p className="location">{apt.location}</p>
              <p className="price">{apt.price} RON/luna</p>
              <p className="details">{apt.rooms} rooms | {apt.area} sqm</p>
              {apt.tenant && <p className="tenant">Tenant: {apt.tenant}</p>}
              <div className="card-actions" onClick={(e) => e.preventDefault()}>
                {canCreate && (
                  <label className="btn-upload">
                    + Photo
                    <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(apt.id, e.target.files[0])} />
                  </label>
                )}
                {canDelete && (
                  <button className="btn-delete" onClick={() => handleDelete(apt.id)}>Delete</button>
                )}
              </div>
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}

export default ApartmentList;
