import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from './AuthContext';

const API = process.env.REACT_APP_API_URL || '';

function AuditLog() {
  const { user, logout, authHeader } = useAuth();
  const [logs, setLogs] = useState([]);
  const [filter, setFilter] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const fetchLogs = useCallback(async () => {
    const url = filter
      ? `${API}/api/audit?username=${encodeURIComponent(filter)}`
      : `${API}/api/audit`;
    const res = await fetch(url, { headers: authHeader() });
    if (res.ok) {
      setLogs(await res.json());
    }
  }, [filter, authHeader]);

  const fetchUsers = async () => {
    const res = await fetch(`${API}/api/users`, { headers: authHeader() });
    if (res.ok) {
      setAllUsers(await res.json());
    }
  };

  useEffect(() => {
    fetchLogs();
    fetchUsers();
  }, []);

  useEffect(() => {
    if (!autoRefresh) return;
    const interval = setInterval(fetchLogs, 5000);
    return () => clearInterval(interval);
  }, [autoRefresh, fetchLogs]);

  const formatTimestamp = (ts) => {
    if (!ts) return '';
    try {
      const d = Array.isArray(ts)
        ? new Date(ts[0], ts[1] - 1, ts[2], ts[3], ts[4], ts[5])
        : new Date(ts);
      return d.toLocaleString();
    } catch {
      return String(ts);
    }
  };

  const roleBadge = (role) => {
    if (!role) return <span className="role-badge role-tenant">UNKNOWN</span>;
    const cls = role === 'OWNER' ? 'role-owner' : role === 'ADMIN' ? 'role-admin' : 'role-tenant';
    return <span className={`role-badge ${cls}`}>{role}</span>;
  };

  const actionBadge = (action) => {
    if (!action) return <span className="action-badge">{action}</span>;
    let cls = 'action-other';
    if (action.startsWith('LOGIN')) cls = 'action-login';
    else if (action.includes('CREATED')) cls = 'action-create';
    else if (action.includes('UPDATED')) cls = 'action-update';
    else if (action.includes('DELETED')) cls = 'action-delete';
    else if (action.includes('UPLOADED')) cls = 'action-upload';
    else if (action.includes('ASSIGNED')) cls = 'action-assign';
    return <span className={`action-badge ${cls}`}>{action.replace(/_/g, ' ')}</span>;
  };

  const uniqueUsernames = [...new Set(logs.map(l => l.username).filter(Boolean))];

  return (
    <div className="app">
      <header>
        <h1>Audit Log</h1>
        <div className="header-right">
          <span className="header-user">{user.username} ({user.role})</span>
          <a href="/" className="btn-back">Apartments</a>
          <button className="btn-back" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="audit-controls">
        <div className="audit-filter">
          <label>Filter by user:</label>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">All users</option>
            {uniqueUsernames.map(u => (
              <option key={u} value={u}>{u}</option>
            ))}
          </select>
          {filter && (
            <button className="btn-back" onClick={() => setFilter('')} style={{ marginLeft: '8px' }}>
              Clear
            </button>
          )}
        </div>
        <div className="audit-refresh">
          <label>
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
            />
            {' '}Auto-refresh (5s)
          </label>
          <button className="btn-back" onClick={fetchLogs} style={{ marginLeft: '12px' }}>
            Refresh Now
          </button>
        </div>
      </div>

      <div className="audit-table-wrapper">
        <table className="audit-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>User</th>
              <th>Role</th>
              <th>Action</th>
              <th>Details</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && (
              <tr>
                <td colSpan="5" className="audit-empty">No audit logs found</td>
              </tr>
            )}
            {logs.map(log => (
              <tr key={log.id}>
                <td className="audit-time">{formatTimestamp(log.timestamp)}</td>
                <td className="audit-username">{log.username}</td>
                <td>{roleBadge(log.role)}</td>
                <td>{actionBadge(log.action)}</td>
                <td className="audit-details">{log.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="audit-count">
        {logs.length} log{logs.length !== 1 ? 's' : ''} total
      </div>
    </div>
  );
}

export default AuditLog;
