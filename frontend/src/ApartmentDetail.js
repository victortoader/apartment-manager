import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function ApartmentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user, authHeader } = useAuth();
  const [apartment, setApartment] = useState(null);
  const [protocols, setProtocols] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [selectedDocType, setSelectedDocType] = useState('HANDOVER_PROTOCOL');

  const canDelete = user?.role === 'OWNER';
  const canUpload = user?.role === 'OWNER' || user?.role === 'ADMIN';

  useEffect(() => {
    fetchApartment();
    fetchProtocols();
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
    if (window.confirm('Delete this protocol?')) {
      await fetch(`${API}/api/apartments/protocols/${protocolId}`, {
        method: 'DELETE',
        headers: authHeader()
      });
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

  const formatDocType = (docType) => {
    const types = {
      'HANDOVER_PROTOCOL': 'Handover Protocol',
      'BILLS': 'Bills',
      'PHOTOS': 'Photos',
      'OTHER': 'Other'
    };
    return types[docType] || docType;
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
            {canUpload && (
              <label className="btn-upload large">
                + Add Photo
                <input type="file" accept="image/*" hidden onChange={(e) => handlePhotoUpload(e.target.files[0])} />
              </label>
            )}
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
            {apartment.tenant && (
              <div className="info-row">
                <span className="label">Tenant</span>
                <span>{apartment.tenant}</span>
              </div>
            )}
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
            <h2>Documents</h2>
            {canUpload && (
              <div className="upload-controls">
                <select
                  value={selectedDocType}
                  onChange={(e) => setSelectedDocType(e.target.value)}
                  className="doc-type-select"
                >
                  <option value="HANDOVER_PROTOCOL">Handover Protocol</option>
                  <option value="BILLS">Bills</option>
                  <option value="PHOTOS">Photos</option>
                  <option value="OTHER">Other</option>
                </select>
                <label className="btn-upload">
                  {uploading ? 'Uploading...' : '+ Upload'}
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
            <p className="empty-protocols">No documents uploaded yet.</p>
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
      </div>
    </div>
  );
}

export default ApartmentDetail;
