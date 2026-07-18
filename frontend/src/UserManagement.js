import React, { useState, useEffect } from 'react';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function UserManagement() {
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
      setError(data.error || 'Failed to create user');
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
    if (window.confirm('Delete this user?')) {
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
        <h1>User Administration</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          <a href="/" className="btn-back">Apartments</a>
          <button className="btn-back" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="user-management">
        <form className="user-create-form" onSubmit={handleCreate}>
          {error && <div className="login-error">{error}</div>}
          <input
            placeholder="Username"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          <input
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option value="OWNER">Owner</option>
            <option value="ADMIN">Admin</option>
            <option value="TENANT">Tenant</option>
          </select>
          <button type="submit" className="btn-primary">Create User</button>
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
                  <option value="">No apartment</option>
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
