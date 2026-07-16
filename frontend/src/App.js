import React, { useState, useEffect } from 'react';
import './App.css';

const API = process.env.REACT_APP_API_URL || 'http://localhost:8080';

function App() {
  const [apartments, setApartments] = useState([]);
  const [form, setForm] = useState({
    title: '', description: '', location: '', price: '', rooms: '', area: ''
  });
  const [selectedFiles, setSelectedFiles] = useState({});
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    fetchApartments();
  }, []);

  const fetchApartments = async () => {
    const res = await fetch(`${API}/api/apartments`);
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
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(apartment)
    });

    if (res.ok) {
      const saved = await res.json();

      if (selectedFiles[saved.id]) {
        const formData = new FormData();
        formData.append('file', selectedFiles[saved.id]);
        await fetch(`${API}/api/apartments/${saved.id}/photos`, {
          method: 'POST',
          body: formData
        });
      }

      setForm({ title: '', description: '', location: '', price: '', rooms: '', area: '' });
      setSelectedFiles({});
      setShowForm(false);
      fetchApartments();
    }
  };

  const handlePhotoUpload = async (apartmentId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    await fetch(`${API}/api/apartments/${apartmentId}/photos`, {
      method: 'POST',
      body: formData
    });
    fetchApartments();
  };

  const handleDelete = async (id) => {
    if (window.confirm('Delete this apartment?')) {
      await fetch(`${API}/api/apartments/${id}`, { method: 'DELETE' });
      fetchApartments();
    }
  };

  return (
    <div className="app">
      <header>
        <h1>Apartment Manager</h1>
        <button className="btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : '+ Add Apartment'}
        </button>
      </header>

      {showForm && (
        <form className="apartment-form" onSubmit={handleSubmit}>
          <input name="title" placeholder="Title" value={form.title} onChange={handleChange} required />
          <textarea name="description" placeholder="Description" value={form.description} onChange={handleChange} />
          <input name="location" placeholder="Location" value={form.location} onChange={handleChange} required />
          <input name="price" type="number" step="0.01" placeholder="Price (RON/month)" value={form.price} onChange={handleChange} required />
          <input name="rooms" type="number" placeholder="Rooms" value={form.rooms} onChange={handleChange} />
          <input name="area" type="number" step="0.1" placeholder="Area (sqm)" value={form.area} onChange={handleChange} />
          <button type="submit" className="btn-primary">Save</button>
        </form>
      )}

      <div className="apartment-grid">
        {apartments.length === 0 && <p className="empty">No apartments yet. Add one!</p>}
        {apartments.map(apt => (
          <div key={apt.id} className="apartment-card">
            {apt.photoPaths && apt.photoPaths.length > 0 ? (
              <div className="photos">
                {apt.photoPaths.map((photo, i) => (
                  <img key={i} src={`${API}/api/apartments/photos/${photo}`} alt="apartment" />
                ))}
              </div>
            ) : (
              <div className="no-photo">No photos</div>
            )}
            <div className="card-body">
              <h3>{apt.title}</h3>
              <p className="location">{apt.location}</p>
              <p className="price">{apt.price} RON/luna</p>
              <p className="details">{apt.rooms} rooms | {apt.area} sqm</p>
              {apt.description && <p className="desc">{apt.description}</p>}
              <div className="card-actions">
                <label className="btn-upload">
                  + Photo
                  <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(apt.id, e.target.files[0])} />
                </label>
                <button className="btn-delete" onClick={() => handleDelete(apt.id)}>Delete</button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
