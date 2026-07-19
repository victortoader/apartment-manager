import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

const LANGUAGES = [
  { code: 'en', label: 'EN' },
  { code: 'de', label: 'DE' },
  { code: 'it', label: 'IT' },
  { code: 'fr', label: 'FR' },
  { code: 'ro', label: 'RO' }
];

function LanguageSwitcher() {
  const { i18n } = useTranslation();
  return (
    <div className="lang-switcher">
      {LANGUAGES.map(l => (
        <button
          key={l.code}
          className={`lang-btn${i18n.language === l.code ? ' active' : ''}`}
          onClick={() => i18n.changeLanguage(l.code)}
        >
          {l.label}
        </button>
      ))}
    </div>
  );
}

function UserManagement() {
  const { t } = useTranslation();
  const { user, logout, authHeader } = useAuth();
  const [users, setUsers] = useState([]);
  const [apartments, setApartments] = useState([]);
  const [form, setForm] = useState({ username: '', password: '', email: '', role: 'TENANT' });
  const [error, setError] = useState('');

  useEffect(() => {
    fetchUsers();
    fetchApartments();
  }, []);

  const fetchUsers = async () => {
    const res = await fetch(`${API}/api/users`, { headers: authHeader() });
    if (res.ok) {
      setUsers(await res.json());
    }
  };

  const fetchApartments = async () => {
    const res = await fetch(`${API}/api/apartments`, { headers: authHeader() });
    if (res.ok) {
      setApartments(await res.json());
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setError('');
    const res = await fetch(`${API}/api/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify(form)
    });
    if (res.ok) {
      setForm({ username: '', password: '', email: '', role: 'TENANT' });
      fetchUsers();
    } else {
      const data = await res.json();
      setError(data.error || t('userManagement.createFailed'));
    }
  };

  const handleAssign = async (userId, apartmentId) => {
    if (!apartmentId) return;
    await fetch(`${API}/api/users/${userId}/apartment`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeader() },
      body: JSON.stringify({ apartmentId: parseInt(apartmentId) })
    });
    fetchUsers();
  };

  const handleDelete = async (userId) => {
    if (window.confirm(t('userManagement.deleteConfirm'))) {
      await fetch(`${API}/api/users/${userId}`, {
        method: 'DELETE',
        headers: authHeader()
      });
      fetchUsers();
    }
  };

  const roleBadge = (role) => {
    const cls = role === 'OWNER' ? 'role-owner' : role === 'ADMIN' ? 'role-admin' : 'role-tenant';
    return <span className={`role-badge ${cls}`}>{role}</span>;
  };

  return (
    <div className="app">
      <header>
        <h1>{t('userManagement.title')}</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          <LanguageSwitcher />
          <a href="/" className="btn-back">{t('header.apartments')}</a>
          <button className="btn-back" onClick={logout}>{t('logout')}</button>
        </div>
      </header>

      <div className="user-management">
        <form className="user-create-form" onSubmit={handleCreate}>
          {error && <div className="login-error">{error}</div>}
          <input
            placeholder={t('userManagement.username')}
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
          <input
            type="password"
            placeholder={t('userManagement.password')}
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          <input
            type="email"
            placeholder={t('userManagement.email')}
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="OWNER">{t('userManagement.owner')}</option>
            <option value="ADMIN">{t('userManagement.admin')}</option>
            <option value="TENANT">{t('userManagement.tenant')}</option>
          </select>
          <button type="submit" className="btn-primary">{t('userManagement.createUser')}</button>
        </form>

        <div className="user-list">
          {users.map(u => (
            <div key={u.id} className="user-item">
              <div className="user-info">
                <span className="user-name">{u.username}</span>
                {u.email && <span className="user-email">{u.email}</span>}
                {roleBadge(u.role)}
              </div>
              {u.role === 'TENANT' && (
                <select
                  className="apt-select"
                  value={u.apartment ? u.apartment.id : ''}
                  onChange={(e) => handleAssign(u.id, e.target.value)}
                >
                  <option value="">{t('userManagement.noApartment')}</option>
                  {apartments.map(apt => (
                    <option key={apt.id} value={apt.id}>{apt.title}</option>
                  ))}
                </select>
              )}
              {u.role !== 'OWNER' && (
                <button className="btn-delete-small" onClick={() => handleDelete(u.id)}>×</button>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default UserManagement;
