import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

const API = process.env.REACT_APP_API_URL || 'http://localhost:8080';

function ApartmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [apartment, setApartment] = useState(null);
  const [protocols, setProtocols] = useState([]);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetchApartment();
    fetchProtocols();
  }, [id]);

  const fetchApartment = async () => {
    const res = await fetch(`${API}/api/apartments/${id}`);
    if (res.ok) {
      setApartment(await res.json());
    }
  };

  const fetchProtocols = async () => {
    const res = await fetch(`${API}/api/apartments/${id}/protocols`);
    if (res.ok) {
      setProtocols(await res.json());
    }
  };

  const handlePhotoUpload = async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    await fetch(`${API}/api/apartments/${id}/photos`, {
      method: 'POST',
      body: formData
    });
    fetchApartment();
  };

  const handleProtocolUpload = async (file) => {
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    await fetch(`${API}/api/apartments/${id}/protocols`, {
      method: 'POST',
      body: formData
    });
    setUploading(false);
    fetchProtocols();
  };

  const handleDeleteProtocol = async (protocolId) => {
    if (window.confirm('Delete this protocol?')) {
      await fetch(`${API}/api/apartments/protocols/${protocolId}`, { method: 'DELETE' });
      fetchProtocols();
    }
  };

  if (!apartment) return <div className="app"><p>Loading...</p></div>;

  const getProtocolIcon = (contentType) => {
    if (contentType?.includes('pdf')) return 'PDF';
    if (contentType?.includes('word') || contentType?.includes('doc')) return 'DOC';
    if (contentType?.includes('image')) return 'IMG';
    return 'FILE';
  };

  return (
    <div className="app">
      <header>
        <button className="btn-back" onClick={() => navigate('/')}>← Back</button>
        <h1>{apartment.title}</h1>
      </header>

      <div className="detail-layout">
        <div className="detail-main">
          <div className="detail-photos">
            {apartment.photoPaths && apartment.photoPaths.length > 0 ? (
              apartment.photoPaths.map((photo, i) => (
                <img key={i} src={`${API}/api/apartments/photos/${photo}`} alt={`Photo ${i + 1}`} />
              ))
            ) : (
              <img src="/placeholder.svg" alt="No photos" className="placeholder-img" />
            )}
            <label className="btn-upload large">
              + Add Photo
              <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(e.target.files[0])} />
            </label>
          </div>

          <div className="detail-info">
            <div className="info-row">
              <span className="label">Location</span>
              <span>{apartment.location}</span>
            </div>
            <div className="info-row">
              <span className="label">Price</span>
              <span className="price">{apartment.price} RON/luna</span>
            </div>
            <div className="info-row">
              <span className="label">Rooms</span>
              <span>{apartment.rooms}</span>
            </div>
            <div className="info-row">
              <span className="label">Area</span>
              <span>{apartment.area} sqm</span>
            </div>
            {apartment.description && (
              <div className="info-row full">
                <span className="label">Description</span>
                <p>{apartment.description}</p>
              </div>
            )}
          </div>
        </div>

        <div className="detail-protocols">
          <div className="protocols-header">
            <h2>Handover Protocols</h2>
            <label className="btn-upload">
              {uploading ? 'Uploading...' : '+ Upload Document'}
              <input
                type="file"
                accept=".pdf,.doc,.docx,.jpeg,.jpg,.png"
                hidden
                onChange={(e) => handleProtocolUpload(e.target.files[0])}
                disabled={uploading}
              />
            </label>
          </div>

          {protocols.length === 0 ? (
            <p className="empty-protocols">No protocols uploaded yet.</p>
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
                      <span className="protocol-date">
                        {new Date(proto.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                  </a>
                  <button className="btn-delete-small" onClick={() => handleDeleteProtocol(proto.id)}>×</button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ApartmentDetail;
